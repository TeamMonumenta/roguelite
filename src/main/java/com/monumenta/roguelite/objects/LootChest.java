package com.monumenta.roguelite.objects;

import com.monumenta.roguelite.Main;
import com.monumenta.roguelite.enums.Biome;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.loot.LootTable;
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
        boolean waterlogged = false;
        Material blockAbove = Material.AIR;
        if (this.getBiome() == Biome.WATER) {
            waterlogged = true;
            blockAbove = Material.WATER;
        }
        loc.getBlock().getRelative(0, 1, 0).setType(blockAbove);

        spawnLootChest(loc, table, this.getDirection(), waterlogged);
    }

    public static void spawnLootChest(Location loc, String table, BlockFace facing, boolean waterlogged) {
        // Set the block to a chest, so it can be manipulated
        loc.getBlock().setType(Material.CHEST);

        // Set the facing, waterlogged, and loot table state of the chest
        Chest blockState = (Chest) loc.getBlock().getState();
        org.bukkit.block.data.type.Chest chestData = (org.bukkit.block.data.type.Chest) blockState.getBlockData();
        chestData.setFacing(facing);
        chestData.setWaterlogged(waterlogged);
        blockState.setBlockData(chestData);
        LootTable loot = Bukkit.getLootTable(Objects.requireNonNull(NamespacedKey.fromString(table)));
        if (loot == null) {
            Main.getInstance().getLogger().severe("Could not find loot table: " + table);
        }
        blockState.setLootTable(loot);
        // Apply the state
        blockState.update();
    }

    public void spawnAir() {
        Block block = this.getLocation().getBlock();
        block.setType(Material.AIR);
        block.getRelative(0, 1, 0).setType(Material.AIR);
    }
}
