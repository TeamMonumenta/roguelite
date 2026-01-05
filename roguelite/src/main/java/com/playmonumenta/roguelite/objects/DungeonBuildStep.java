package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.Utils;
import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.Config;
import com.playmonumenta.roguelite.enums.RoomType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.Nullable;

public final class DungeonBuildStep {
	// Doesn't change per-step
	DungeonBuilder mDungeonBuilder;

	// Doors/rooms available for future steps
	Set<Room> mRemainingRooms;
	Set<Door> mRemainingDoors;

	// Room doors that haven't yet been tried on this door location
	List<Door> mRemainingSrcDoorsThisStep = new ArrayList<>();
	List<Room> mRemainingDestRoomsThisStep = new ArrayList<>();
	Map<Room, List<Door>> mValidDestDoorsThisStep = new HashMap<>();

	// Used to determine when to stop generating certain aspects
	int mCurrentRoomNumber;
	Map<RoomType, Integer> mRoomTypeCounts = new HashMap<>();
	@Nullable Door mSrcDoor = null;
	@Nullable Door mDstDoor = null;

	/**
	 * Create initial step
	 * @param dungeonBuilder The builder generating this step
	 * @param remainingDoors The doors that do not have a room or dead end attached yet
	 * @param roomPoolMaster The collection of all valid rooms
	 */
	public DungeonBuildStep(DungeonBuilder dungeonBuilder, Collection<Door> remainingDoors, Collection<Room> roomPoolMaster) {
		mDungeonBuilder = dungeonBuilder;

		mRemainingRooms = new HashSet<>(roomPoolMaster);
		mRemainingDoors = new HashSet<>(remainingDoors);

		mCurrentRoomNumber = 1;

		shuffleSrcDoors();
	}

	/**
	 * Creates a new step off of an existing step
	 * @param other The previous step in the build stack
	 * @throws NoSuchElementException Occurs if the current step is invalid, requiring backtracking
	 */
	public DungeonBuildStep(DungeonBuildStep other) throws NoSuchElementException {
		mDungeonBuilder = other.mDungeonBuilder;
		mRemainingRooms = new HashSet<>(other.mRemainingRooms);
		mRemainingDoors = new HashSet<>(other.mRemainingDoors);

		Room previousRoom = other.selectedRoom();
		if (previousRoom == null) {
			throw new NoSuchElementException("Previous room somehow not selected?");
		}
		if (!RoomType.DEADEND.equals(previousRoom.getType())) {
			mRemainingRooms.remove(previousRoom);
		}

		mCurrentRoomNumber = other.mCurrentRoomNumber + 1;
		mRoomTypeCounts = new HashMap<>(other.mRoomTypeCounts);

		mRemainingSrcDoorsThisStep = new ArrayList<>(other.mRemainingSrcDoorsThisStep);
		mRemainingSrcDoorsThisStep.remove(other.mSrcDoor);
		mRemainingDoors.remove(other.mSrcDoor);
		for (Door d : Objects.requireNonNull(other.selectedRoom()).getDoorList()) {
			if (d.equals(other.mDstDoor)) {
				continue;
			}
			mRemainingSrcDoorsThisStep.add(d);
		}

		mRemainingDestRoomsThisStep = new ArrayList<>();
		mValidDestDoorsThisStep = new HashMap<>();
		mSrcDoor = null;
		mDstDoor = null;

		shuffleSrcDoors();
	}

	/**
	 * Attempts to add another room to the planned instance
	 *
	 * @return true for a valid instance, false if more steps are required
	 * @throws NoSuchElementException if there are no possible ways to make a valid instance from this step;
	 * if this occurs, backtracking is required.
	 */
	public boolean tryBuildStep() throws NoSuchElementException {
		if (mRemainingDoors.isEmpty()) {
			return true;
		}

		// If we're re-entering this, we had to backtrack - unmark the selected door/room
		mDstDoor = null;

		if (mRemainingDestRoomsThisStep.isEmpty()) {
			shuffleDstRooms();
		}
		if (mSrcDoor == null) {
			throw new NoSuchElementException("No valid rooms?");
		}

		Location pos = mSrcDoor.getLocation();

		Room dstRoom = mRemainingDestRoomsThisStep.getFirst();
		Door dstDoor = mValidDestDoorsThisStep.getOrDefault(dstRoom, new ArrayList<>()).removeFirst();
		dstRoom.setLocation(pos.clone().subtract(dstDoor.getRelPos()));
		Hitbox dstHitbox = dstRoom.getHitbox();

		if (mDungeonBuilder.selectedHitboxes().stream().noneMatch(dstHitbox::collidesWith)) {
			// Valid location; attempt to create a next step
			mDstDoor = dstDoor;
			DungeonBuildStep nextStep = new DungeonBuildStep(this);
			mDungeonBuilder.mBuildStack.add(nextStep);

			// Since the room's position appears valid so far, adjust the positions of the contents of the room to match
			dstRoom.setContentLocations();
		}

		return false;
	}

	/**
	 * Gets the currently selected room, if one has been selected
	 * @return The selected room, or null if none is selected at this time
	 */
	public @Nullable Room selectedRoom() {
		if (mDstDoor == null) {
			return null;
		}
		return mDstDoor.getParentRoom();
	}

	/**
	 * Randomizes the order in which source (starting) doors are selected
	 */
	public void shuffleSrcDoors() {
		mRemainingSrcDoorsThisStep = new ArrayList<>(mRemainingDoors);
		if (mCurrentRoomNumber <= Config.ROOMS_TO_SPAWN) {
			Collections.shuffle(mRemainingSrcDoorsThisStep);
		}
	}

	/**
	 * Creates a randomized list of destination rooms for the currently selected door
	 * @throws NoSuchElementException if there are no possible ways to make a valid instance from this step;
	 * if this occurs, backtracking is required.
	 */
	public void shuffleDstRooms() throws NoSuchElementException {
		EnumSet<RoomType> validRoomTypes = EnumSet.noneOf(RoomType.class);
		if (mCurrentRoomNumber > Config.ROOMS_TO_SPAWN) {
			validRoomTypes.add(RoomType.DEADEND);
		} else if (mCurrentRoomNumber == 25) {
			validRoomTypes.add(RoomType.END);
		} else {
			validRoomTypes.add(RoomType.NORMAL);
			if (mCurrentRoomNumber < 25 && mCurrentRoomNumber % 5 == 0) {
				validRoomTypes.add(RoomType.UTIL);
			}
		}

		mSrcDoor = mRemainingSrcDoorsThisStep.removeFirst();
		Biome biome = mSrcDoor.getBiome();
		BlockFace direction = mSrcDoor.getDirection();
		mRemainingDestRoomsThisStep.clear();
		mValidDestDoorsThisStep.clear();

		for (Room destRoom : mRemainingRooms) {
			if (!validRoomTypes.contains(destRoom.getType())) {
				continue;
			}

			boolean roomInvalid = true;

			for (Door destDoor : destRoom.getDoorList()) {
				if (destDoor.correspondsTo(direction, biome)) {
					mValidDestDoorsThisStep
						.computeIfAbsent(destRoom, k -> new ArrayList<>())
						.add(destDoor);
					if (roomInvalid) {
						mRemainingDestRoomsThisStep.add(destRoom);
						roomInvalid = false;
					}
				}
			}
		}

		Utils.shuffleRoomsByWeight(mRemainingDestRoomsThisStep);
	}
}
