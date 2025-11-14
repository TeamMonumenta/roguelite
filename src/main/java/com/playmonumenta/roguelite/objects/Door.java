package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.enums.Biome;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Door extends RoomObject {
    private Room parentRoom;

    // basic constructor without arguments
    public Door() {
        super();
        this.parentRoom = new Room();
    }

    // constructor with location and biome
    public Door(Location loc, Biome biome, BlockFace dir) {
        super();
        this.setLocation(loc);
        this.setBiome(biome);
        this.setDirection(dir);
    }

    // copy constructor
    public Door(Door old) {
        this.setRelPos(old.getRelPos().clone());
        this.setBiome(old.getBiome());
        this.setDirection(old.getDirection());
        this.setLocation(old.getLocation().clone());
        this.parentRoom = old.parentRoom;
    }

    //getters

    public Room getParentRoom() {
        return parentRoom;
    }

    //setters

    public void setParentRoom(Room parentRoom) {
        this.parentRoom = parentRoom;
    }

    public boolean correspondsTo(BlockFace direction, Biome biome) {
        boolean directionMatch = this.getDirection().getOppositeFace() == direction;
        boolean biomeMatch = biome == Biome.ANY || this.getBiome() == Biome.ANY || biome == this.getBiome();
        return directionMatch && biomeMatch;
    }
}
