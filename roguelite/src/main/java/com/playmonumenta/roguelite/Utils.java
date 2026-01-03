package com.playmonumenta.roguelite;

import com.playmonumenta.roguelite.objects.Door;
import com.playmonumenta.roguelite.objects.Room;
import java.util.List;
import org.bukkit.block.BlockFace;

public class Utils {
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
}
