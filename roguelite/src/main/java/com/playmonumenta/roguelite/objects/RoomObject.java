package com.playmonumenta.roguelite.objects;

import com.google.gson.JsonObject;
import com.playmonumenta.roguelite.enums.Biome;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class RoomObject {
	// direction the object is facing
	private BlockFace mDirection;
	// the biome of the object
	private Biome mBiome;
	// the relative position inside the room
	private Vector mRelPos;
	// the ral location inside the world
	private Location mLocation;


	//basic constructor that sets fields variables to default values
	public RoomObject() {
		this.mDirection = BlockFace.SELF;
		this.mBiome = Biome.NONE;
		this.mRelPos = new Vector(0,0,0);
		this.mLocation = new Location(null, 0, 0, 0);
	}

	//constructor with given variables
	public RoomObject(BlockFace direction, Biome biome, Vector relPos) {
		this();
		this.mDirection = direction;
		this.mBiome = biome;
		this.mRelPos = relPos;
	}

	//copy constructor
	RoomObject(RoomObject old) {
		this.mDirection = old.mDirection;
		this.mBiome = old.mBiome;
		this.mRelPos = old.mRelPos.clone();
		this.mLocation = old.mLocation.clone();
	}

	// Setters

	public void setDirection(BlockFace direction) {
		this.mDirection = direction;
	}

	public void setBiome(Biome biome) {
		this.mBiome = biome;
	}

	public void setRelPos(Vector relPos) {
		this.mRelPos = relPos;
	}

	public void setLocation(Location location) {
		this.mLocation = location;
	}

	// Getters

	public BlockFace getDirection() {
		return this.mDirection;
	}

	public Biome getBiome() {
		return this.mBiome;
	}

	public Vector getRelPos() {
		return this.mRelPos;
	}

	public Location getLocation() {
		return mLocation;
	}

	// methods

	public JsonObject toJsonObject() {
		JsonObject out = new JsonObject();
		Vector rp = this.getRelPos();
		out.addProperty("x", rp.getBlockX());
		out.addProperty("y", rp.getBlockY());
		out.addProperty("z", rp.getBlockZ());
		out.addProperty("biome", this.getBiome().toString());
		out.addProperty("dir", this.getDirection().toString());
		return out;
	}

}
