package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.Main;
import com.playmonumenta.roguelite.Utils;
import com.playmonumenta.roguelite.enums.BuildStatus;
import com.playmonumenta.roguelite.enums.RoomType;
import dev.jorel.commandapi.CommandAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DungeonLayout {
	boolean mDirectLog;
	private final Location mCenterLoc;
	private final Vector mMinCorner;
	// true if visited
	private final Map<Room, Boolean> mRoomsVisited = new HashMap<>();
	// Minimize how many rooms need to be checked for a given location
	private final Map<Long, Set<Room>> mRoomBins = new HashMap<>();

	// Build finalization info
	private BuildStatus mBuildStatus;
	private final List<Objective> mObjectives;
	private final List<Objective> mChestsReplacingObjectives;
	private final List<LootChest> mSelectedChests;
	private final List<LootChest> mUnselectedChests;

	public DungeonLayout(
		boolean directLog,
		Location centerLoc,
		List<Room> rooms,
		List<Objective> objectives,
		List<Objective> chestsReplacingObjectives,
		List<LootChest> selectedChests,
		List<LootChest> unselectedChests
	) {
		mDirectLog = directLog;
		mCenterLoc = centerLoc;
		mMinCorner = centerLoc.clone().add(-131, -89, -136).toVector();

		for (Room room : rooms) {
			mRoomsVisited.put(room, false);

			for (long subChunkKey : room.getHitbox().subChunkVectors(mMinCorner)) {
				mRoomBins.computeIfAbsent(subChunkKey, k -> new HashSet<>()).add(room);
			}
		}

		// Replace duplicate sets with references to the same set to reduce memory usage
		for (Long subChunkKey : new ArrayList<>(mRoomBins.keySet())) {
			Set<Room> currentRooms = mRoomBins.get(subChunkKey);
			if (currentRooms == null) {
				continue;
			}

			for (Axis axis : Axis.values()) {
				Long neighborKey = Utils.neighboringSubChunkKey(subChunkKey, axis, false);
				if (neighborKey == null) {
					// No neighbor on this axis
					continue;
				}

				Set<Room> neighborRooms = mRoomBins.get(neighborKey);
				if (currentRooms.equals(neighborRooms)) {
					// Found a neighbor on this axis; no point checking further axes
					mRoomBins.put(subChunkKey, neighborRooms);
					break;
				}
			}
		}

		mObjectives = new ArrayList<>(objectives);
		mChestsReplacingObjectives = new ArrayList<>(chestsReplacingObjectives);
		mSelectedChests = new ArrayList<>(selectedChests);
		mUnselectedChests = new ArrayList<>(unselectedChests);

		mBuildStatus = BuildStatus.UNSTARTED;
	}

	// Assumes this is the correct world for the layout
	public void markVisited(Player player) {
		if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
			return;
		}

		Vector position = player.getLocation().toVector();
		long subChunk = Utils.subChunkKey(mMinCorner, position);
		Set<Room> possibleRooms = mRoomBins.get(subChunk);
		if (possibleRooms == null) {
			return;
		}

		for (Room room : possibleRooms) {
			if (room.getHitbox().contains(position)) {
				mRoomsVisited.put(room, true);
			}
		}
	}

	public List<Room> visitedRooms() {
		return mRoomsVisited.entrySet().stream()
			.filter(Map.Entry::getValue)
			.map(Map.Entry::getKey)
			.toList();
	}

	public CompletableFuture<Void> spawn() {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (!BuildStatus.UNSTARTED.equals(mBuildStatus)) {
			future.completeExceptionally(CommandAPI.failWithString("Generation already started!"));
			return future;
		}
		mBuildStatus = BuildStatus.BUILDING;

		Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
			Map<RoomType, List<Room>> roomMap = new HashMap<>();
			for (Room room : mRoomsVisited.keySet()) {
				roomMap.computeIfAbsent(room.getType(), k -> new ArrayList<>()).add(room);
			}

			try {
				// Spawn normal rooms
				List<CompletableFuture<Void>> futures = new ArrayList<>(beginSpawningRoomList(roomMap.getOrDefault(RoomType.NORMAL, List.of())));
				CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

				// Spawn every other room
				futures = new ArrayList<>(beginSpawningRoomList(roomMap.getOrDefault(RoomType.UTIL, List.of())));
				futures.addAll(beginSpawningRoomList(roomMap.getOrDefault(RoomType.END, List.of())));
				futures.addAll(beginSpawningRoomList(roomMap.getOrDefault(RoomType.DEADEND, List.of())));
				CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

				Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
					spawnObjectives();
					spawnChests();
					openFirstPath();
					directLog("Done.");
				});
			} catch (Exception exception) {
				mBuildStatus = BuildStatus.ERROR;
				future.completeExceptionally(exception);
				return;
			}

			mBuildStatus = BuildStatus.BUILT;
			future.complete(null);
		});
		return future;
	}

	private List<CompletableFuture<Void>> beginSpawningRoomList(List<Room> rooms) {
		if (rooms.isEmpty()) {
			return List.of();
		}
		directLog("Spawning " + rooms.size() + " rooms of type " + rooms.getFirst().getType().name());
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		for (Room room : rooms) {
			futures.add(room.loadAndPasteAsync());
		}
		return futures;
	}

	private void spawnObjectives() {
		directLog("Spawning objectives");
		for (Objective o : mObjectives) {
			o.spawnObjective();
		}
	}

	private void spawnChests() {
		directLog("Spawning chests");
		for (Objective o : mChestsReplacingObjectives) {
			o.spawnChest();
		}
		for (LootChest c : mSelectedChests) {
			c.spawnChest();
		}
		for (LootChest c : mUnselectedChests) {
			c.spawnAir();
		}
	}

	private void openFirstPath() {
		Location l = mCenterLoc.clone().add(0, 7, 2);
		l.clone().add(0, 2, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				l.clone().add(x, y, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
			}
		}
	}

	private void directLog(String text) {
		directLog(Component.text(text));
	}

	private void directLog(Component component) {
		if (!mDirectLog) {
			return;
		}
		World world = mCenterLoc.getWorld();
		Audience audience = world != null ? world : Bukkit.getConsoleSender();
		audience.sendMessage(Component.text("", NamedTextColor.AQUA).append(component));
	}
}
