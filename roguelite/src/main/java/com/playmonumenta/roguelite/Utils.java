package com.playmonumenta.roguelite;

import com.playmonumenta.roguelite.objects.Door;
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
		// compute total weight
		double totalWeight = 0.0d;
		for (Door d : list) {
			totalWeight += d.getParentRoom().getWeight();
		}
		// get a random
		int randomIndex = -1;
		double random = Math.random() * totalWeight;
		// select
		for (int i = 0; i < list.size(); i++) {
			random -= list.get(i).getParentRoom().getWeight();
			if (random <= 0.0d) {
				randomIndex = i;
				break;
			}
		}

		return list.get(randomIndex);
	}
}
