package com.playmonumenta.roguelite.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.roguelite.enums.RoomType;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Room {
	private String mPath;
	private RoomType mType;
	private Vector mSize;
	private Location mLocation;
	private Hitbox mHitbox;
	private int mWeight;

	List<Objective> mObjectiveList;
	List<LootChest> mLootChestList;
	List<Door> mDoorList;

	// Copy constructor
	public Room(Room old) {
		this();
		this.mPath = old.mPath;
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
	}

	// Basic constructor
	public Room() {
		this.mPath = "undefined";
		this.mType = RoomType.NONE;
		this.mSize = new Vector(0, 0, 0);
		this.mLocation = new Location(null, 0, 0, 0);
		this.mHitbox = new Hitbox(this);
		this.mWeight = 0;
		this.mObjectiveList = new ArrayList<>();
		this.mLootChestList = new ArrayList<>();
		this.mDoorList = new ArrayList<>();
	}

	// Getters

	public String getPath() {
		return mPath;
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

	public CompletableFuture<Void> loadStructureAsync() {
		return StructuresAPI.loadAndPasteStructure(this.mPath, this.mLocation, true, false);
	}

	// Setters

	public void setPath(String path) {
		this.mPath = path;
	}

	public void setType(RoomType type) {
		this.mType = type;
	}

	public void setSize(Vector size) {
		this.mSize = size;
	}

	public void setLocation(Location location) {
		this.mLocation = location;
	}

	public void setHitbox(Hitbox hitbox) {
		this.mHitbox = hitbox;
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

	// Methods

	public JsonObject toJsonObject() {
		JsonObject room = new JsonObject();
		JsonObject size = new JsonObject();
		JsonArray doors = new JsonArray();
		JsonArray objectives = new JsonArray();
		JsonArray chests = new JsonArray();

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
		room.add("size", size);
		room.addProperty("type", this.mType.name());
		room.addProperty("weight", this.mWeight);
		room.add("doors", doors);
		room.add("objectives", objectives);
		room.add("chests", chests);
		return room;
	}
}
