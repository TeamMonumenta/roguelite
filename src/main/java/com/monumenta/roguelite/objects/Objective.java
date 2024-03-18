package com.monumenta.roguelite.objects;

import com.monumenta.roguelite.enums.Biome;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.util.Vector;

import java.util.Objects;

public class Objective extends RoomObject {

    // basic constructor with default values
    public Objective() {
        super();
    }

    // basic constructor with given RoomObject values
    public Objective(BlockFace direction, Biome biome, Vector relPos) {
        super(direction, biome, relPos);
    }

    // copy constructor
    public Objective(Objective old) {
        super(old);
    }

    public synchronized void spawnObjective() {
        Location loc = this.getLocation().clone().add(0.5, 1, 0.5);
        this.getLocation().getBlock().setType(Material.AIR);
        Objects.requireNonNull(loc.getWorld()).spawn(loc, EnderCrystal.class);
    }

    public void spawnChest() {
        Location loc = this.getLocation();
        String table = "epic:r2/dungeons/fred/";
        if (this.getBiome() == Biome.VAULT) {
            table += "challenge";
        } else {
            table += "objective_" + this.getBiome().name().toLowerCase();
        }
        boolean waterlogged = this.getBiome() == Biome.WATER;
        LootChest.spawnLootChest(loc, table, this.getDirection(), waterlogged);
    }
}
