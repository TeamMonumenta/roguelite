package com.playmonumenta.roguelite.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.roguelite.enums.RoomType;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class Room {
	private String mPath;
	private long mModifyTime;
	private RoomType mType;
	private Vector mSize;
	private Location mLocation;
	private Hitbox mHitbox;
	private int mWeight;

	List<Objective> mObjectiveList;
	List<LootChest> mLootChestList;
	List<Door> mDoorList;

	private boolean mVisited;

	// Copy constructor
	public Room(Room old) {
		this();
		this.mPath = old.mPath;
		this.mModifyTime = old.mModifyTime;
		this.mType = old.mType;
		this.mSize = old.mSize;
		this.mLocation = old.mLocation.clone();
		this.mHitbox = new Hitbox(old.mHitbox);
		this.mWeight = old.mWeight;

		for (LootChest c : old.mLootChestList) {
			this.mLootChestList.add(new LootChest(c));
		}
		for (Objective o : old.mObjectiveList) {
			this.mObjectiveList.add(new Objective(o));
		}
		for (Door d : old.mDoorList) {
			Door n = new Door(d);
			n.setParentRoom(this);
			this.mDoorList.add(n);
		}

		this.mVisited = false;
	}

	// Basic constructor
	public Room() {
		this.mPath = "undefined";
		this.mModifyTime = 0L;
		this.mType = RoomType.NONE;
		this.mSize = new Vector(0, 0, 0);
		this.mLocation = new Location(null, 0, 0, 0);
		this.mHitbox = new Hitbox(this);
		this.mWeight = 0;
		this.mObjectiveList = new ArrayList<>();
		this.mLootChestList = new ArrayList<>();
		this.mDoorList = new ArrayList<>();
		this.mVisited = false;
	}

	// Json constructor
	public Room(JsonElement data) throws Exception {
		if (
			data instanceof JsonObject roomObj &&
			roomObj.get("path") instanceof JsonPrimitive pathPrimitive && pathPrimitive.isString() &&
			roomObj.get("size") instanceof JsonObject sizeObject &&
			sizeObject.get("x") instanceof JsonPrimitive xSizePrim && xSizePrim.isNumber() &&
			sizeObject.get("y") instanceof JsonPrimitive ySizePrim && ySizePrim.isNumber() &&
			sizeObject.get("z") instanceof JsonPrimitive zSizePrim && zSizePrim.isNumber() &&
			roomObj.get("type") instanceof JsonPrimitive typePrimitive && typePrimitive.isString() &&
			roomObj.get("weight") instanceof JsonPrimitive weightPrimitive && weightPrimitive.isNumber() &&
			roomObj.get("doors") instanceof JsonArray doorsArray &&
			roomObj.get("objectives") instanceof JsonArray objectivesArray &&
			roomObj.get("chests") instanceof JsonArray chestsArray
		) {
			mPath = pathPrimitive.getAsString();
			mSize = new Vector(
				xSizePrim.getAsInt(),
				ySizePrim.getAsInt(),
				zSizePrim.getAsInt()
			);
			mType = RoomType.valueOf(typePrimitive.getAsString());
			mWeight = weightPrimitive.getAsInt();

			mDoorList = new ArrayList<>();
			for (JsonElement doorData : doorsArray) {
				mDoorList.add(new Door(doorData, this));
			}

			mObjectiveList = new ArrayList<>();
			for (JsonElement objectiveData : objectivesArray) {
				mObjectiveList.add(new Objective(objectiveData));
			}

			mLootChestList = new ArrayList<>();
			for (JsonElement chestData : chestsArray) {
				mLootChestList.add(new LootChest(chestData));
			}
		} else {
			throw new Exception("Not all required fields were present for a given Room");
		}

		if (
			roomObj.get("modifyTime") instanceof JsonPrimitive modifyTime && modifyTime.isNumber()
		) {
			mModifyTime = modifyTime.getAsLong();
		} else {
			mModifyTime = 0L;
		}

		if (
			roomObj.get("location") instanceof JsonObject locationObject &&
			locationObject.get("world") instanceof JsonPrimitive worldPrimitive && worldPrimitive.isString() &&
			locationObject.get("x") instanceof JsonPrimitive xLocPrim && xLocPrim.isNumber() &&
			locationObject.get("y") instanceof JsonPrimitive yLocPrim && yLocPrim.isNumber() &&
			locationObject.get("z") instanceof JsonPrimitive zLocPrim && zLocPrim.isNumber()
		) {
			World world = Bukkit.getWorld(worldPrimitive.getAsString());
			mLocation = new Location(
				world,
				xLocPrim.getAsInt(),
				yLocPrim.getAsInt(),
				zLocPrim.getAsInt()
			);

			setContentLocations();
		} else {
			mLocation = new Location(null, 0, 0, 0);
		}

		if (roomObj.get("visited") instanceof JsonPrimitive visited && visited.isBoolean()) {
			mVisited = visited.getAsBoolean();
		} else {
			mVisited = false;
		}

		mHitbox = new Hitbox(this);
	}

	// Getters

	public String getPath() {
		return mPath;
	}

	public long getModifyTime() {
		return mModifyTime;
	}

	public RoomType getType() {
		return this.mType;
	}

	public Vector getSize() {
		return this.mSize;
	}

	public Location getLocation() {
		return this.mLocation;
	}

	public Hitbox getHitbox() {
		return this.mHitbox;
	}

	public int getWeight() {
		return this.mWeight;
	}

	public List<Door> getDoorList() {
		return this.mDoorList;
	}

	public List<Objective> getObjectiveList() {
		return this.mObjectiveList;
	}

	public List<LootChest> getLootChestList() {
		return this.mLootChestList;
	}

	public boolean getVisited() {
		return mVisited;
	}

	public CompletableFuture<Void> loadAndPasteAsync() {
		return StructuresAPI.loadAndPasteStructure(this.mPath, this.mLocation, true, false);
	}

	// Setters

	public void setPath(String path) {
		this.mPath = path;
	}

	public void setModifyTime(long modifyTime) {
		this.mModifyTime = modifyTime;
	}

	public void setType(RoomType type) {
		this.mType = type;
	}

	public void setSize(Vector size) {
		this.mSize = size;
	}

	public void setWorld(World world) {
		this.mLocation.setWorld(world);
	}

	public void setLocation(Location location) {
		this.mLocation = location;
		this.mHitbox = new Hitbox(this);
	}

	public void setWeight(int weight) {
		this.mWeight = weight;
	}

	public void setDoorList(List<Door> doorList) {
		this.mDoorList = doorList;
	}

	public void setObjectiveList(List<Objective> objectiveList) {
		this.mObjectiveList = objectiveList;
	}

	public void setLootChestList(List<LootChest> lootChestList) {
		this.mLootChestList = lootChestList;
	}

	public void setVisited(boolean visited) {
		mVisited = visited;
	}

	// Methods

	public void setContentLocations() {
		for (Objective o : mObjectiveList) {
			o.setLocation(mLocation.clone().add(o.getRelPos()));
		}

		for (LootChest c : mLootChestList) {
			c.setLocation(mLocation.clone().add(c.getRelPos()));
		}

		for (Door d : mDoorList) {
			d.setLocation(mLocation.clone().add(d.getRelPos()));
		}
	}

	public JsonObject toJsonObject() {
		JsonObject room = new JsonObject();
		JsonObject location = null;
		JsonObject size = new JsonObject();
		JsonArray doors = new JsonArray();
		JsonArray objectives = new JsonArray();
		JsonArray chests = new JsonArray();

		Location roomLocation = this.getLocation();
		if (!roomLocation.equals(new Location(null, 0, 0, 0))) {
			location = new JsonObject();
			location.addProperty("world", roomLocation.getWorld().getName());
			location.addProperty("x", roomLocation.getBlockX());
			location.addProperty("y", roomLocation.getBlockY());
			location.addProperty("z", roomLocation.getBlockZ());
		}

		Vector roomSize = this.getSize();
		size.addProperty("x", roomSize.getBlockX());
		size.addProperty("y", roomSize.getBlockY());
		size.addProperty("z", roomSize.getBlockZ());

		for (Door d : mDoorList) {
			doors.add(d.toJsonObject());
		}
		for (Objective o : mObjectiveList) {
			objectives.add(o.toJsonObject());
		}
		for (LootChest c : mLootChestList) {
			chests.add(c.toJsonObject());
		}

		room.addProperty("path", this.mPath);
		room.addProperty("modifyTime", this.mModifyTime);
		room.add("location", location);
		room.add("size", size);
		room.addProperty("type", this.mType.name());
		room.addProperty("weight", this.mWeight);
		room.add("doors", doors);
		room.add("objectives", objectives);
		room.add("chests", chests);
		if (this.mVisited) {
			room.addProperty("visited", true);
		}
		return room;
	}
}
