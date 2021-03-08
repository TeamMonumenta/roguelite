package com.monumenta.roguelite.objects;

import com.monumenta.roguelite.enums.Biome;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class LootChest extends RoomObject {

    // basic constructor with default values
    public LootChest() {
        super();
    }

    // basic constructor with given RoomObject values
    public LootChest(BlockFace direction, Biome biome, Vector relPos) {
        super(direction, biome, relPos);
    }

    // copy constructor
    public LootChest(LootChest old) {
        super(old);
    }

    public void spawnChest() {
        Location loc = this.getLocation();
        String table = "epic:r2/dungeons/fred/normal_" + this.getBiome().name().toLowerCase();
        if (this.getBiome() == Biome.VAULT) {
            table = "epic:r2/dungeons/fred/challenge";
        }
        String waterlogged = "false";
        Material blockAbove = Material.AIR;
        if (this.getBiome() == Biome.WATER) {
            waterlogged = "true";
            blockAbove = Material.WATER;
        }
        loc.getBlock().getRelative(0, 1, 0).setType(blockAbove);
        String cmd = "setblock " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() +
                " minecraft:chest[facing=" + this.getDirection().name().toLowerCase() +
                ",waterlogged=" + waterlogged + "]{LootTable:\"" + table + "\"}";
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    public void spawnAir() {
        Block block = this.getLocation().getBlock();
        block.setType(Material.AIR);
        block.getRelative(0, 1, 0).setType(Material.AIR);
    }
}
