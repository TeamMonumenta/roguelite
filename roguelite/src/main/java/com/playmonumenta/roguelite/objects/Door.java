package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.enums.Biome;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Door extends RoomObject {
	private Room mParentRoom;

	// Basic constructor without arguments
	public Door() {
		super();
		this.mParentRoom = new Room();
	}

	// Constructor with location and biome
	public Door(Location loc, Biome biome, BlockFace dir) {
		super();
		this.setLocation(loc);
		this.setBiome(biome);
		this.setDirection(dir);
		this.mParentRoom = new Room();
	}

	// Copy constructor
	public Door(Door old) {
		this.setRelPos(old.getRelPos().clone());
		this.setBiome(old.getBiome());
		this.setDirection(old.getDirection());
		this.setLocation(old.getLocation().clone());
		this.mParentRoom = old.mParentRoom;
	}

	// Getters

	public Room getParentRoom() {
		return mParentRoom;
	}

	// Setters

	public void setParentRoom(Room parentRoom) {
		this.mParentRoom = parentRoom;
	}

	public boolean correspondsTo(BlockFace direction, Biome biome) {
		boolean directionMatch = this.getDirection().getOppositeFace() == direction;
		boolean biomeMatch = biome == Biome.ANY || this.getBiome() == Biome.ANY || biome == this.getBiome();
		return directionMatch && biomeMatch;
	}
}
