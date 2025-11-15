package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.Main;
import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.DungeonStatus;
import com.playmonumenta.roguelite.enums.RoomType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class DungeonReader {

	private final List<Room> mRooms;
	private final Plugin mPlugin;
	private final CommandSender mSender;
	private float mProgress;
	private final Location mLoc;

	private final Stats mStats;

	public DungeonReader(List<Room> r, Plugin p, CommandSender s, Location l) {
		this.mPlugin = p;
		this.mRooms = r;
		this.mSender = s;
		this.mProgress = 0;
		this.mLoc = l.clone();
		this.mLoc.setY(89);
		this.mStats = new Stats();
	}

	public void read(int amount) {
		BukkitTask progressMeterTask = Bukkit.getServer().getScheduler().runTaskTimer(this.mPlugin, () -> this.mSender.sendMessage(Component.text(String.format("%.2f%%", this.mProgress))), 20L, 20L);
		this.mStats.addToTargetDungeonCount(amount);
		for (int i = 0; i < amount; i++) {
			this.mProgress = 100 * (float)i / amount;
			Dungeon dungeon = new Dungeon(this.mRooms, this.mLoc, this.mPlugin, false);
			dungeon.calculateWithRetries(1);
			this.readDungeon(dungeon);
		}
		progressMeterTask.cancel();
	}

	private void readDungeon(Dungeon dungeon) {
		if (dungeon.mStatus == DungeonStatus.CALCULATED) {
			this.mStats.addToSuccessfulDungeonCount(1);
			this.readRooms(dungeon.mUsedRooms);
			this.mStats.addToUnusedChestsTotal(dungeon.mLootChestPotentialSpawnPoints.size());
			this.readSpawnedChests(dungeon);
		} else {
			this.mStats.addToUnsuccessfulDungeonCount(1);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			if (dungeon.mCalculationException != null) {
				dungeon.mCalculationException.printStackTrace(pw);
			}
			this.mStats.addToDungeonCalculationFailures(sw.toString(), 1);
		}
	}

	private void readRooms(List<Room> rooms) {
		for (Room r : rooms) {
			this.mStats.addToRoomDistribution(r, 1);
		}
	}

	private void readSpawnedChests(Dungeon dungeon) {
		for (Objective o : dungeon.mObjectivePotentialSpawnPoints) {
			this.mStats.addToSpawnedChests(o, 1);
		}
		for (LootChest c : dungeon.mSelectedLootChests) {
			this.mStats.addToSpawnedChests(c, 1);
		}
	}

	public void output() {
		String str = this.getOutputString();

		// normal file
		String fileName = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss"));
		String filePath = this.mPlugin.getDataFolder().getPath() + "/stats/" + fileName + ".txt";
		File f = new File(filePath);
		f.getParentFile().mkdirs();
		try (FileWriter file = new FileWriter(f, StandardCharsets.UTF_8)) {
			file.write(str);
			file.flush();
			this.mSender.sendMessage(Component.text(filePath + " Written."));
		} catch (IOException e) {
			Main.getInstance().getLogger().log(Level.WARNING, "Failed to save normal dungeon stats file", e);
		}

		// latest file
		filePath = this.mPlugin.getDataFolder().getPath() + "/stats/latest.txt";
		f = new File(filePath);
		try (FileWriter file = new FileWriter(f, StandardCharsets.UTF_8)) {
			file.write(str);
			file.flush();
		} catch (IOException e) {
			Main.getInstance().getLogger().log(Level.WARNING, "Failed to save latest dungeon stats file", e);
		}
	}

    /*

    STRING BUILDER

     */

	private String getOutputString() {
		StringBuilder str = new StringBuilder();
		this.addHeader(str);
		this.addDungeons(str);
		this.addUnusedChests(str);
		this.addChests(str);
		this.addRoomDistribution(str);
		return str.toString();
	}

	private void addHeader(StringBuilder str) {
		str.append("Monumenta - Friendly Roguelite Experience Dungeon - Stats for ").append(this.mStats.getTargetDungeonCount()).append(" dungeons\n");
		long msElapsed = System.currentTimeMillis() - this.mStats.getStartTime();
		str.append(String.format("generated over %.2f seconds, with an average of %d nanoseconds per dungeon.\n\n", (float)msElapsed / 1000, msElapsed*1000 / this.mStats.getTargetDungeonCount()));
	}

	private void addDungeons(StringBuilder str) {
		str.append("Dungeons: ").append(this.mStats.getDungeonCount()).append("\n");
		str.append(String.format("\t┣╾ Successful calculations: %d (%.1f%%)\n", this.mStats.getSuccessfulDungeonCount(), 100 * ((float)this.mStats.getSuccessfulDungeonCount() / (float)this.mStats.getDungeonCount())));
		str.append(String.format("\t┗╾ Unsuccessful calculations: %d (%.1f%%)\n", this.mStats.getUnsuccessfulDungeonCount(), 100 * ((float)this.mStats.getUnsuccessfulDungeonCount() / (float)this.mStats.getDungeonCount())));
		if (this.mStats.getUnsuccessfulDungeonCount() > 0) {
			Iterator<Map.Entry<String, Integer>> i = this.mStats.getDungeonCalculationFailures().entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String, Integer> e = i.next();
				String lineSymbol = "┣╾";
				if (!i.hasNext()) {
					lineSymbol = "┗╾";
				}
				str.append(String.format("\t   \t%s %d : %s\n",
					lineSymbol, e.getValue(), e.getKey()));
			}
		}
		str.append("\n\n");
	}

	private void addUnusedChests(StringBuilder str) {
		str.append(String.format("Unused Chests Markers: %d (%.1f/D)\n", this.mStats.getUnusedChestsTotal(), (float)this.mStats.getUnusedChestsTotal() / this.mStats.getSuccessfulDungeonCount()));
	}

	private void addChests(StringBuilder str) {

		int total = this.mStats.getSpawnedChestsTotal();
		int dc = this.mStats.getSuccessfulDungeonCount();
		str.append(String.format("Spawned Chests: %d (%.1f/D)\n", total, (float)total / dc));

		int objectiveTotal = this.mStats.getSpawnedChestsObjectiveTotal();
		str.append(String.format("\t┣╾ Objective Chests: %d (%.1f/D) (%.1f%%)\n", objectiveTotal, (float)objectiveTotal / dc, 100 * (float)objectiveTotal / total));
		Iterator<Map.Entry<Biome, Integer>> m = this.mStats.getSpawnedChestsObjective().entrySet().iterator();
		while (m.hasNext()) {
			Map.Entry<Biome, Integer> e = m.next();
			String lineSymbol = "┣╾";
			if (!m.hasNext()) {
				lineSymbol = "┗╾";
			}
			str.append(String.format("\t┃  \t%s %s : %d (%.1f/D) (%.1f%%) (%.1f%% of objective chests)\n",
				lineSymbol, e.getKey().name(), e.getValue(), (float)e.getValue()/dc, 100 * (float)e.getValue() / total, 100 * (float)e.getValue() / objectiveTotal));
		}
		int normalTotal = this.mStats.getSpawnedChestsNormalTotal();
		str.append(String.format("\t┗╾ Normal Chests: %d (%.1f/D) (%.1f%%)\n", normalTotal, (float)normalTotal / dc, 100 * (float)normalTotal / total));
		m = this.mStats.getSpawnedChestsNormal().entrySet().iterator();
		while (m.hasNext()) {
			Map.Entry<Biome, Integer> e = m.next();
			String lineSymbol = "┣╾";
			if (!m.hasNext()) {
				lineSymbol = "┗╾";
			}
			str.append(String.format("\t   \t%s %s : %d (%.1f/D) (%.1f%%) (%.1f%% of normal chests)\n",
				lineSymbol, e.getKey().name(), e.getValue(), (float)e.getValue()/dc, 100 * (float)e.getValue() / total, 100 * (float)e.getValue() / normalTotal));
		}
		str.append("\n\n");
	}

	private void addRoomDistribution(StringBuilder str) {
		int total = this.mStats.getRoomTotal();
		int dc = this.mStats.getSuccessfulDungeonCount();
		str.append(String.format("Rooms: %d (%.1f/D)\n", total, (float)total / dc));
		Iterator<Map.Entry<RoomType, Integer>> typeIterator = this.mStats.getRoomTypeDistribution().entrySet().iterator();
		String typeLineSymbol = "┣╾";
		String typeIntermediateSymbol = "┃";
		while (typeIterator.hasNext()) {
			Map.Entry<RoomType, Integer> typeEntry = typeIterator.next();
			if (!typeIterator.hasNext()) {
				typeLineSymbol = "┗╾";
				typeIntermediateSymbol = " ";
			}
			Map<String, Integer> roomDistribution = this.mStats.getRoomDistribution(typeEntry.getKey());
			float goalValue = 0.0f;
			String goalPresenceStr = "";
			if (typeEntry.getKey() == RoomType.NORMAL) {
				goalValue = (float)typeEntry.getValue() / dc / roomDistribution.size() * 100.0f;
				goalPresenceStr = String.format(" (Goal Presence: %.1f%%)", goalValue);
			}
			str.append(String.format("\t%s %s: %d (%.1f/D)%s\n", typeLineSymbol, typeEntry.getKey().name(), typeEntry.getValue(), (float)typeEntry.getValue() / dc, goalPresenceStr));
			Iterator<Map.Entry<String, Integer>> idIterator = roomDistribution.entrySet().iterator();
			int iteratorLength = roomDistribution.size();
			String idLineSymbol = "┣╾";
			while (idIterator.hasNext()) {
				Map.Entry<String, Integer> idEntry = idIterator.next();
				if (!idIterator.hasNext()) {
					idLineSymbol = "┗╾";
				}
				float presence = 100 * (float)idEntry.getValue() / dc;
				String presenceError = "";
				Integer roomWeight = this.mStats.getRoomWeightMap().get(idEntry.getKey());
				if (roomWeight == null) {
					continue;
				}
				if (typeEntry.getKey() == RoomType.NORMAL) {
					double maxDiffFromGoal = Math.cbrt(iteratorLength);
					double calculatedValue = roomWeight * (1 - (presence - goalValue) / 100);
					if (presence < goalValue - maxDiffFromGoal) {
						presenceError = String.format(" !!! Too Low %+.1f !!!", presence - goalValue);
						this.mSender.sendMessage(Component.text(String.format("%s: %d -> %d", idEntry.getKey(), roomWeight, 1 + (int) calculatedValue)));
					} else if (presence > goalValue + maxDiffFromGoal) {
						presenceError = String.format(" !!! Too High %+.1f !!!", presence - goalValue);
						this.mSender.sendMessage(Component.text(String.format("%s: %d -> %d", idEntry.getKey(), roomWeight, (int) calculatedValue)));
					}
				}
				str.append(String.format("\t%s \t%s %s: %d (%.1f%% Total) (%.1f%% %s) (%.1f%% Presence%s) (Weight: %d)\n",
					typeIntermediateSymbol, idLineSymbol, idEntry.getKey(), idEntry.getValue(),
					100 * (float)idEntry.getValue() / total, 100 * (float)idEntry.getValue() / typeEntry.getValue(), typeEntry.getKey().name(),
					presence, presenceError, roomWeight));
			}
		}
		str.append("\n\n");
	}
}
