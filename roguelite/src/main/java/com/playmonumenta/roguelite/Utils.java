package com.playmonumenta.roguelite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.roguelite.objects.Room;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	public static <T> T removeRandomElement(List<T> list) throws IndexOutOfBoundsException {
		Random random = new Random();
		if (list.isEmpty()) {
			throw new IndexOutOfBoundsException("That list is empty!");
		}
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

	private static void writeJson(String fileName, JsonElement json) throws IOException {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		Path file = Path.of(fileName);
		if (!Files.exists(file)) {
			Files.createDirectories(file.getParent());
		}
		Gson gson;
		try (var writer = Files.newBufferedWriter(file)) {
			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.disableHtmlEscaping();
			gson = gsonBuilder.create();
			gson.toJson(json, writer);
		}
	}

	public static void writeJsonSafely(String fileName, JsonObject json) throws IOException {
		String tempFileName = fileName + ".tmp";
		writeJson(tempFileName, json);

		File file = new File(fileName);
		File tempFile = new File(tempFileName);

		if (file.isFile()) {
			if (!file.delete()) {
				Main.getInstance().getLogger().warning("Failed to delete " + fileName + " before replacing it");
			}
		}

		if (!tempFile.renameTo(file)) {
			Main.getInstance().getLogger().warning("Failed to rename " + tempFileName + " to " + fileName);
		}
	}

	private static JsonObject readJson(String fileName) throws Exception {
		// Do not attempt to catch exceptions here - let them propagate to the caller
		Gson gson = new Gson();
		Reader reader = Files.newBufferedReader(Paths.get(fileName));
		return gson.fromJson(reader, JsonObject.class);
	}

	public static JsonObject readJsonSafely(String fileName) throws Exception {
		try {
			return readJson(fileName);
		} catch (IOException ignored) {
			String tempFileName = fileName + ".tmp";
			File file = new File(fileName);
			File tempFile = new File(tempFileName);

			// If this fails to find valid data, the temp file (if it exists) will not be moved
			JsonObject data = readJson(tempFileName);

			if (file.isFile() && !file.delete()) {
				Main.getInstance().getLogger().warning("Failed to delete " + fileName + " before replacing it");
			}
			if (!tempFile.renameTo(file)) {
				Main.getInstance().getLogger().warning("Failed to rename " + tempFileName + " to " + fileName);
			}

			return data;
		}
	}
}
