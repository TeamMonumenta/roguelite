package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.Utils;
import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.Config;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class DungeonBuilder {
	Plugin mPlugin;
	long mRoomsModifyTime;
	Location mCenterLoc;
	protected final List<DungeonBuildStep> mBuildStack = new ArrayList<>();
	private final List<Hitbox> mDungeonWalls;
	private final List<Objective> mSelectedObjectives = new ArrayList<>();
	private final List<Objective> mObjectivePotentialSpawnPoints = new ArrayList<>();
	private final List<LootChest> mSelectedLootChests = new ArrayList<>();
	private final List<LootChest> mLootChestPotentialSpawnPoints = new ArrayList<>();

	public DungeonBuilder(Plugin plugin, Location cl, List<Room> roomPoolMaster) {
		mPlugin = plugin;
		mCenterLoc = cl;

		mDungeonWalls = List.of(
			new Hitbox(cl.clone().add(-18, -88, -28), cl.clone().add(15, 166, 3)), // Central box
			new Hitbox(cl.clone().add(-127, -89, -136), cl.clone().add(128, 166, -132)), // North instance limit
			new Hitbox(cl.clone().add(128, -89, -132), cl.clone().add(132, 166, 123)), // East instance limit
			new Hitbox(cl.clone().add(-127, -89, 123), cl.clone().add(128, 166, 127)), // South instance limit
			new Hitbox(cl.clone().add(-131, -89, -132), cl.clone().add(-127, 166, 123)) // West instance limit
		);

		List<Door> remainingDoors = new ArrayList<>();
		remainingDoors.add(new Door(cl.clone().add(0, 4, 3), Biome.getRandom(), BlockFace.SOUTH));

		mRoomsModifyTime = roomPoolMaster.stream()
			.map(Room::getModifyTime)
			.max(Long::compareTo)
			.orElse(0L);

		Set<Room> weightedRooms = new HashSet<>(roomPoolMaster);
		weightedRooms.removeIf(r -> r.getWeight() <= 0.0);

		DungeonBuildStep firstStep = new DungeonBuildStep(this, remainingDoors, weightedRooms);
		mBuildStack.add(firstStep);
	}

	public @Nullable DungeonLayout tryBuildStep() throws NoSuchElementException {
		try {
			boolean success = mBuildStack.getLast().tryBuildStep();
			if (success) {
				// Valid instance generated; select chests while still running async, will apply later
				selectObjectivesAndChests();
				return new DungeonLayout(
					mRoomsModifyTime,
					mCenterLoc,
					selectedRooms(),
					mSelectedObjectives,
					mObjectivePotentialSpawnPoints,
					mSelectedLootChests,
					mLootChestPotentialSpawnPoints
				);
			}
		} catch (NoSuchElementException ex) {
			mBuildStack.removeLast();
		}
		return null;
	}

	public List<Room> selectedRooms() {
		return mBuildStack.stream()
			.map(DungeonBuildStep::selectedRoom)
			.filter(Objects::nonNull)
			.toList();
	}

	public List<Hitbox> selectedHitboxes() {
		List<Hitbox> result = new ArrayList<>(mDungeonWalls);
		result.addAll(selectedRooms().stream().map(Room::getHitbox).toList());
		return result;
	}

	public void selectObjectivesAndChests() {
		// Reset in case builder is re-used
		mSelectedObjectives.clear();
		mObjectivePotentialSpawnPoints.clear();
		mSelectedLootChests.clear();
		mLootChestPotentialSpawnPoints.clear();

		for (Room room : selectedRooms()) {
			// Add objectives and chests (positions adjusted earlier)
			mObjectivePotentialSpawnPoints.addAll(room.getObjectiveList());
			mLootChestPotentialSpawnPoints.addAll(room.getLootChestList());
		}

		// Select objectives
		for (int i = 0; i < 3; i++) {
			mSelectedObjectives.add(Utils.removeRandomElement(mObjectivePotentialSpawnPoints));
		}

		// All remaining objective potential spawn locations will always be chests;
		// there are always fewer of these than the desired final chest count
		int amountToSpawn = Config.CHEST_COUNT - mObjectivePotentialSpawnPoints.size();
		for (int i = 0; i < amountToSpawn; i++) {
			mSelectedLootChests.add(Utils.removeRandomElement(mLootChestPotentialSpawnPoints));
		}
	}
}
