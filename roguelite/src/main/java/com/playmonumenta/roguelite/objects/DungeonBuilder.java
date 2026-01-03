package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.enums.Biome;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.plugin.Plugin;

public class DungeonBuilder {
	Plugin mPlugin;
	boolean mDirectLog;
	Location mCenterLoc;
	public final List<DungeonBuildStep> mBuildStack = new ArrayList<>();
	public final List<Hitbox> mDungeonWalls;

	public DungeonBuilder(Plugin plugin, boolean directLog, Location cl, List<Room> roomPoolMaster) {
		mPlugin = plugin;
		mDirectLog = directLog;
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

		Set<Room> weightedRooms = new HashSet<>(roomPoolMaster);
		weightedRooms.removeIf(r -> r.getWeight() <= 0.0);

		DungeonBuildStep firstStep = new DungeonBuildStep(this, remainingDoors, weightedRooms);
		mBuildStack.add(firstStep);
	}

	public boolean tryBuildStep() throws NoSuchElementException {
		try {
			return mBuildStack.getLast().tryBuildStep();
		} catch (NoSuchElementException ex) {
			mBuildStack.removeLast();
			return false;
		}
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
}
