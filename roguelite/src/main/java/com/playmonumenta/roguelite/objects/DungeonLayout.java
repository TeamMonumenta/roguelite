package com.playmonumenta.roguelite.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
	private final Vector mSize;
	// true if visited
	private final List<Room> mRoomsVisited = new ArrayList<>();
	// Minimize how many rooms need to be checked for a given location
	private final Map<Long, Set<Room>> mRoomBins = new HashMap<>();

	// Build finalization info
	private final List<Objective> mObjectives = new ArrayList<>();
	private final List<Objective> mChestsReplacingObjectives = new ArrayList<>();
	private final List<LootChest> mSelectedChests = new ArrayList<>();
	private final List<LootChest> mUnselectedChests = new ArrayList<>();
	private BuildStatus mBuildStatus;

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
		mSize = new Vector(263, 255, 263);

		for (Room room : rooms) {
			mRoomsVisited.add(room);

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

		mObjectives.addAll(objectives);
		mChestsReplacingObjectives.addAll(chestsReplacingObjectives);
		mSelectedChests.addAll(selectedChests);
		mUnselectedChests.addAll(unselectedChests);

		mBuildStatus = BuildStatus.UNSTARTED;
	}

	public DungeonLayout(JsonObject data) throws Exception {
		if (
			data.get("directLog") instanceof JsonPrimitive directLog && directLog.isBoolean() &&
			data.get("centerLoc") instanceof JsonObject centerLoc &&
			centerLoc.get("world") instanceof JsonPrimitive worldName && worldName.isString() &&
			centerLoc.get("x") instanceof JsonPrimitive centerX && centerX.isNumber() &&
			centerLoc.get("y") instanceof JsonPrimitive centerY && centerY.isNumber() &&
			centerLoc.get("z") instanceof JsonPrimitive centerZ && centerZ.isNumber() &&
			data.get("minCorner") instanceof JsonObject minCorner &&
			minCorner.get("x") instanceof JsonPrimitive minX && minX.isNumber() &&
			minCorner.get("y") instanceof JsonPrimitive minY && minY.isNumber() &&
			minCorner.get("z") instanceof JsonPrimitive minZ && minZ.isNumber() &&
			data.get("size") instanceof JsonObject size &&
			size.get("x") instanceof JsonPrimitive sizeX && sizeX.isNumber() &&
			size.get("y") instanceof JsonPrimitive sizeY && sizeY.isNumber() &&
			size.get("z") instanceof JsonPrimitive sizeZ && sizeZ.isNumber() &&
			data.get("roomsVisited") instanceof JsonArray roomsVisited &&
			data.get("objectives") instanceof JsonArray objectives &&
			data.get("chestsReplacingObjectives") instanceof JsonArray chestsReplacingObjectives &&
			data.get("selectedChests") instanceof JsonArray selectedChests &&
			data.get("unselectedChests") instanceof JsonArray unselectedChests &&
			data.get("buildStatus") instanceof JsonPrimitive buildStatus && buildStatus.isString()
		) {
			mDirectLog = directLog.getAsBoolean();

			World world = Bukkit.getWorld(worldName.getAsString());
			mCenterLoc = new Location(world, centerX.getAsInt(), centerY.getAsInt(), centerZ.getAsInt());

			mMinCorner = new Vector(minX.getAsInt(), minY.getAsInt(), minZ.getAsInt());
			mSize = new Vector(sizeX.getAsInt(), sizeY.getAsInt(), sizeZ.getAsInt());

			for (JsonElement r : roomsVisited) {
				mRoomsVisited.add(new Room(r));
			}

			for (JsonElement o : objectives) {
				mObjectives.add(new Objective(o));
			}

			for (JsonElement o : chestsReplacingObjectives) {
				mChestsReplacingObjectives.add(new Objective(o));
			}

			for (JsonElement c : selectedChests) {
				mSelectedChests.add(new LootChest(c));
			}

			for (JsonElement c : unselectedChests) {
				mUnselectedChests.add(new LootChest(c));
			}

			mBuildStatus = BuildStatus.valueOf(buildStatus.getAsString());
		} else {
			throw new Exception("Not all required fields for a Layout were found");
		}
	}

	public JsonObject toJsonObject() {
		JsonObject layout = new JsonObject();

		layout.addProperty("directLog", mDirectLog);

		JsonObject centerLocation = new JsonObject();
		centerLocation.addProperty("world", mCenterLoc.getWorld().getName());
		centerLocation.addProperty("x", mCenterLoc.getBlockX());
		centerLocation.addProperty("y", mCenterLoc.getBlockY());
		centerLocation.addProperty("z", mCenterLoc.getBlockZ());
		layout.add("centerLoc", centerLocation);

		JsonObject minCorner = new JsonObject();
		minCorner.addProperty("x", mMinCorner.getBlockX());
		minCorner.addProperty("y", mMinCorner.getBlockY());
		minCorner.addProperty("z", mMinCorner.getBlockZ());
		layout.add("minCorner", minCorner);

		JsonObject size = new JsonObject();
		size.addProperty("x", mSize.getBlockX());
		size.addProperty("y", mSize.getBlockY());
		size.addProperty("z", mSize.getBlockZ());
		layout.add("size", size);

		JsonArray roomsVisited = new JsonArray();
		for (Room room : mRoomsVisited) {
			roomsVisited.add(room.toJsonObject());
		}
		layout.add("roomsVisited", roomsVisited);

		JsonArray objectives = new JsonArray();
		for (Objective o : mObjectives) {
			objectives.add(o.toJsonObject());
		}
		layout.add("objectives", objectives);

		JsonArray chestsReplacingObjectives = new JsonArray();
		for (Objective o : mChestsReplacingObjectives) {
			chestsReplacingObjectives.add(o.toJsonObject());
		}
		layout.add("chestsReplacingObjectives", chestsReplacingObjectives);

		JsonArray selectedChests = new JsonArray();
		for (LootChest c : mSelectedChests) {
			selectedChests.add(c.toJsonObject());
		}
		layout.add("selectedChests", selectedChests);

		JsonArray unselectedChests = new JsonArray();
		for (LootChest c : mUnselectedChests) {
			unselectedChests.add(c.toJsonObject());
		}
		layout.add("unselectedChests", unselectedChests);

		layout.addProperty("buildStatus", mBuildStatus.toString());

		return layout;
	}

	/**
	 * Checks if a player is in a room in the current layout;
	 * assumes the player is in the world for this layout
	 * @param player A player within the dungeon layout
	 */
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
				room.setVisited(true);
			}
		}
	}

	/**
	 * Gets the rooms that have been visited and can be shown to the player
	 * @return A list of rooms a player has visited
	 */
	public List<Room> visitedRooms() {
		return mRoomsVisited.stream()
			.filter(Room::getVisited)
			.toList();
	}

	/**
	 * Attempts to place the generated layout
	 * @return a future to indicate when the instance has finished being placed
	 */
	public CompletableFuture<Void> spawn() {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (!BuildStatus.UNSTARTED.equals(mBuildStatus)) {
			future.completeExceptionally(CommandAPI.failWithString("Generation already started!"));
			return future;
		}
		mBuildStatus = BuildStatus.BUILDING;

		Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
			Map<RoomType, List<Room>> roomMap = new HashMap<>();
			for (Room room : mRoomsVisited) {
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

	/**
	 * Starts to spawn rooms from a list
	 * @param rooms A list of rooms to be spawned, assumed to all be the same type
	 * @return A list of futures for each room's spawn attempt
	 */
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

	/**
	 * Spawns the objectives (end crystals); must be run sync
	 */
	private void spawnObjectives() {
		directLog("Spawning objectives");
		for (Objective o : mObjectives) {
			o.spawnObjective();
		}
	}

	/**
	 * Spawns chests, and air/water at spaces that could, but did not, have chests
	 */
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

	/**
	 * Removes the barrier blocking the player from entering the instance
	 */
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
