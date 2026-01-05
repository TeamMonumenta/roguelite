package com.playmonumenta.roguelite.objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.roguelite.enums.Biome;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.util.Vector;

import java.util.Locale;

public class Objective extends RoomObject {

	// Basic constructor with default values
	public Objective() {
		super();
	}

	// Basic constructor with given RoomObject values
	public Objective(BlockFace direction, Biome biome, Vector relPos) {
		super(direction, biome, relPos);
	}

	// Copy constructor
	public Objective(Objective old) {
		super(old);
	}

	// Json constructor
	public Objective(JsonElement data) throws Exception {
		fromJsonElement(data);
	}

	public void spawnObjective() {
		Location loc = this.getLocation().clone().add(0.5, 1, 0.5);
		this.getLocation().getBlock().setType(Material.AIR);
		loc.getWorld().spawn(loc, EnderCrystal.class);
	}

	public void spawnChest() {
		Location loc = this.getLocation();
		String table = "epic:r2/dungeons/fred/";
		if (this.getBiome() == Biome.VAULT) {
			table += "challenge";
		} else {
			table += "objective_" + this.getBiome().name().toLowerCase(Locale.ROOT);
		}
		boolean waterlogged = this.getBiome() == Biome.WATER;
		LootChest.spawnLootChest(loc, table, this.getDirection(), waterlogged);
	}
}
