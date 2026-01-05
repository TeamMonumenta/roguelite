package com.playmonumenta.roguelite;

import com.playmonumenta.roguelite.objects.Door;
import com.playmonumenta.roguelite.objects.Room;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.bukkit.Axis;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Utils {
	private static final long SUB_CHUNK_KEY_BITS = 21;
	private static final long SUB_CHUNK_KEY_MASK = (1 << SUB_CHUNK_KEY_BITS) - 1;

	public static BlockFace rotateClockwise(BlockFace in) {
		return switch (in) {
			case NORTH -> BlockFace.EAST;
			case EAST -> BlockFace.SOUTH;
			case SOUTH -> BlockFace.WEST;
			case WEST -> BlockFace.NORTH;
			default -> BlockFace.SELF;
		};
	}

	public static Door getRandomDoorFromWeightedList(List<Door> list) {
		// Compute total weight
		double totalWeight = 0.0d;
		for (Door d : list) {
			totalWeight += d.getParentRoom().getWeight();
		}
		// Get a random
		int randomIndex = -1;
		double random = Math.random() * totalWeight;
		// Select
		for (int i = 0; i < list.size(); i++) {
			random -= list.get(i).getParentRoom().getWeight();
			if (random <= 0.0d) {
				randomIndex = i;
				break;
			}
		}

		return list.get(randomIndex);
	}

	public static void shuffleRoomsByWeight(List<Room> list) {
		// Compute total weight
		double totalWeight = 0.0d;
		for (Room r : list) {
			totalWeight += r.getWeight();
		}

		// Shuffle the list one element at a time
		for (int maxToShuffleIndex = list.size() - 1; maxToShuffleIndex >= 0; maxToShuffleIndex--) {
			double random = Math.random() * totalWeight;
			for (int selectedIndex = 0; selectedIndex <= maxToShuffleIndex; selectedIndex++) {
				random -= list.get(selectedIndex).getWeight();
				if (random <= 0.0d) {
					// Move the selected room to the end of the list
					list.add(list.remove(selectedIndex));
					// maxToShuffleIndex decrements when exiting this inner loop
					break;
				}
			}
		}
	}

	public static <T> T removeRandomElement(List<T> list) {
		Random random = new Random();
		return list.remove(random.nextInt(list.size()));
	}

	public static long subChunkCoordKey(Vector minCorner, Vector position, Axis axis) {
		Function<Vector, Integer> getter = switch (axis) {
			case X -> Vector::getBlockX;
			case Y -> Vector::getBlockY;
			case Z -> Vector::getBlockZ;
		};
		return (getter.apply(position) - getter.apply(minCorner)) & SUB_CHUNK_KEY_MASK;
	}

	public static long subChunkCoordKey(long subChunkKey, Axis axis) {
		long bitShiftAmount = switch (axis) {
			case X -> 2 * SUB_CHUNK_KEY_BITS;
			case Y -> SUB_CHUNK_KEY_BITS;
			case Z -> 0;
		};
		return (subChunkKey >> bitShiftAmount) & SUB_CHUNK_KEY_MASK;
	}

	public static long subChunkKey(long xKey, long yKey, long zKey) {
		return (xKey << (2 * SUB_CHUNK_KEY_BITS)) | (yKey << SUB_CHUNK_KEY_BITS) | zKey;
	}

	public static long subChunkKey(Vector minCorner, Vector position) {
		long xPart = subChunkCoordKey(minCorner, position, Axis.X);
		long yPart = subChunkCoordKey(minCorner, position, Axis.Y);
		long zPart = subChunkCoordKey(minCorner, position, Axis.Z);
		return subChunkKey(xPart, yPart, zPart);
	}

	public static @Nullable Long neighboringSubChunkKey(
		long subChunkKey,
		Axis axis,
		boolean positiveNeighbor
	) {
		int neighborDir = positiveNeighbor ? 1 : -1;
		long relevantPart;
		long xPart = subChunkCoordKey(subChunkKey, Axis.X);
		long yPart = subChunkCoordKey(subChunkKey, Axis.Y);
		long zPart = subChunkCoordKey(subChunkKey, Axis.Z);

		switch (axis) {
			case X -> {
				xPart += neighborDir;
				relevantPart = xPart;
			}
			case Y -> {
				yPart += neighborDir;
				relevantPart = yPart;
			}
			case Z -> {
				zPart += neighborDir;
				relevantPart = zPart;
			}
			default -> relevantPart = -1;
		}

		if (relevantPart < 0) {
			return null;
		}

		return xPart << (2 * SUB_CHUNK_KEY_BITS) | yPart << SUB_CHUNK_KEY_BITS | zPart;
	}
}
