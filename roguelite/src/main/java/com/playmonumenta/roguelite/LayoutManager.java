package com.playmonumenta.roguelite;

import com.playmonumenta.roguelite.enums.BuildStatus;
import com.playmonumenta.roguelite.objects.DungeonBuilder;
import com.playmonumenta.roguelite.objects.DungeonLayout;
import com.playmonumenta.roguelite.objects.Room;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class LayoutManager implements Listener {
	private static final int MAX_PREGEN_SEQUENTIAL_FAILURES = 5;
	private static final int MAX_PREGENERATED_LAYOUTS = 4;

	private static @Nullable LayoutManager INSTANCE = null;
	private final Main mPlugin;
	private final Logger mLogger;
	private final Map<String, DungeonLayout> mWorldLayouts = new HashMap<>();
	private final LinkedBlockingQueue<DungeonLayout> mPregeneratedQueue = new LinkedBlockingQueue<>(MAX_PREGENERATED_LAYOUTS);
	private final BukkitRunnable mPlayerTrackingRunnable;
	private @Nullable BukkitRunnable mGenerationRunnable = null;

	private LayoutManager(Main plugin) {
		mPlugin = plugin;
		mLogger = plugin.getLogger();
		mPlayerTrackingRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				playerTrackingTick();
			}
		};
		mPlayerTrackingRunnable.runTaskTimer(plugin, 0L, 1L);
	}

	public static LayoutManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LayoutManager(Main.getInstance());
		}
		return INSTANCE;
	}

	public void onDisable() {
		mPlayerTrackingRunnable.cancel();

		for (World world : Bukkit.getWorlds()) {
			saveWorldLayout(world);
		}
		if (mGenerationRunnable != null && !mGenerationRunnable.isCancelled()) {
			mGenerationRunnable.cancel();
		}

		INSTANCE = null;
	}

	public void applyLayout(World world) {
		String worldName = world.getName();
		DungeonLayout existingLayout = mWorldLayouts.get(worldName);
		if (existingLayout != null && !BuildStatus.ERROR.equals(existingLayout.getBuildStatus())) {
			world.sendMessage(Component.text("Your layout is being loaded into the world. Please wait.", NamedTextColor.YELLOW));
			return;
		}

		DungeonLayout layout = mPregeneratedQueue.poll();
		mLogger.log(Level.INFO, "[LayoutManager] Attempted to take a pre-generated instance, restarting generation (%d)", mPregeneratedQueue.size());
		restartGeneration(false);
		if (layout == null) {
			world.sendMessage(Component.text("No layouts are pregenerated at this time. Please try again.", NamedTextColor.RED));
			return;
		}
		mWorldLayouts.put(worldName, layout);
		layout.setWorld(world);

		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			CompletableFuture<Void> spawnFuture = layout.spawn();
			try {
				spawnFuture.join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(mPlugin, () -> {
					world.sendMessage(Component.text("Something went wrong loading your layout into the world. Please try again.", NamedTextColor.RED));
					mLogger.log(Level.WARNING, "[LayoutManager] Something went wrong loading a layout into the world " + worldName + ": " + ex.getMessage(), ex);
					mWorldLayouts.remove(worldName);
				});
			}
		});
	}

	/**
	 * Blocking test method for getting layout stats; do not run on main thread, do not run on play
	 * @return A valid layout
	 * @throws InterruptedException If interrupted while waiting for a layout to be available
	 */
	public DungeonLayout getLayoutTest() throws InterruptedException {
		return mPregeneratedQueue.poll(1, TimeUnit.MINUTES);
	}

	public void restartGeneration(boolean cancelExisting) {
		if (mGenerationRunnable != null && !mGenerationRunnable.isCancelled()) {
			if (cancelExisting) {
				mGenerationRunnable.cancel();
				mPregeneratedQueue.clear();
			} else {
				return;
			}
		}
		mGenerationRunnable = getGeneratorRunnable();
		mGenerationRunnable.runTaskAsynchronously(mPlugin);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void worldLoadEvent(WorldLoadEvent event) {
		World world = event.getWorld();
		String worldName = world.getName();
		File layoutFile = getWorldLayoutFile(world);

		try {
			DungeonLayout layout = DungeonLayout.load(layoutFile);
			mWorldLayouts.put(worldName, layout);
			layout.setWorld(world);
			mLogger.log(Level.INFO, "[LayoutManager] Loaded layout for world %s", worldName);
		} catch (Exception e) {
			mLogger.log(Level.WARNING, "[LayoutManager] No layout found for world %s", worldName);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void worldSaveEvent(WorldSaveEvent event) {
		World world = event.getWorld();
		saveWorldLayout(world);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		World world = event.getWorld();
		String worldName = world.getName();
		saveWorldLayout(world);

		mWorldLayouts.remove(worldName);
		mLogger.log(Level.INFO, "[LayoutManager] Unloaded layout for world %s", worldName);
	}

	public void saveWorldLayout(World world) {
		String worldName = world.getName();
		DungeonLayout layout = mWorldLayouts.get(worldName);
		if (layout == null) {
			return;
		}

		File layoutFile = getWorldLayoutFile(world);
		try {
			layout.save(layoutFile);
			mLogger.log(Level.INFO, "[LayoutManager] Saved layout for world %s", worldName);
		} catch (Exception ex) {
			mLogger.log(Level.WARNING, "[LayoutManager] Failed to save layout for world %s:", worldName);
			mLogger.log(Level.WARNING, ex.getMessage(), ex);
		}
	}

	public @Nullable DungeonLayout getWorldLayout(World world) {
		String worldName = world.getName();
		return mWorldLayouts.get(worldName);
	}

	private File getWorldLayoutFile(World world) {
		File worldFolder = world.getWorldFolder();
		File rogueliteFolder = new File(worldFolder, "roguelite");
		return new File(rogueliteFolder, "layout.json");
	}

	private void playerTrackingTick() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			World world = player.getWorld();
			DungeonLayout layout = getWorldLayout(world);
			if (layout == null) {
				continue;
			}
			layout.markVisited(player);
		}
	}

	private BukkitRunnable getGeneratorRunnable() {
		return new BukkitRunnable() {
			private int mRetriesRemaining = MAX_PREGEN_SEQUENTIAL_FAILURES;

			@Override
			public void run() {
				while (true) {
					try {
						CompletableFuture<List<Room>> roomsFuture = new CompletableFuture<>();
						Bukkit.getScheduler().runTask(mPlugin, () -> roomsFuture.complete(RL2Command.getRooms()));
						DungeonBuilder builder = new DungeonBuilder(
							mPlugin,
							new Location(null, -769, 91, 4868),
							roomsFuture.join()
						);

						if (!mPregeneratedQueue.offer(buildLayout(builder))) {
							mLogger.log(Level.INFO, "[LayoutManager] Pre-generated layout queue is full (%d), shutting down", mPregeneratedQueue.size());
							cancel();
							return;
						}
						mLogger.log(Level.INFO, "[LayoutManager] Pre-generated a new layout (%d)", mPregeneratedQueue.size());

						mRetriesRemaining = MAX_PREGEN_SEQUENTIAL_FAILURES;
					} catch (Exception ex) {
						mLogger.log(Level.WARNING, "[LayoutManager] Exception while generating dungeon layouts: " + ex.getMessage(), ex);
						mRetriesRemaining--;
						if (mRetriesRemaining <= 0) {
							cancel();
							break;
						}
					}
				}
			}
		};
	}

	/**
	 * Internal method that must only ever be run async
	 * @param builder The builder state to generate from
	 * @return A valid dungeon layout
	 * @throws NoSuchElementException only occurs if there are NO valid dungeon configurations; should never happen!
	 */
	private DungeonLayout buildLayout(DungeonBuilder builder) throws NoSuchElementException {
		while (true) {
			DungeonLayout layout = builder.tryBuildStep();
			if (layout != null) {
				return layout;
			}
		}
	}
}
