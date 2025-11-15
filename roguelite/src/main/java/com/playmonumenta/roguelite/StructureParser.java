package com.playmonumenta.roguelite;

import com.google.gson.GsonBuilder;
import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.RoomType;
import com.playmonumenta.roguelite.objects.Door;
import com.playmonumenta.roguelite.objects.LootChest;
import com.playmonumenta.roguelite.objects.Objective;
import com.playmonumenta.roguelite.objects.Room;
import com.playmonumenta.structures.StructuresAPI;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class StructureParser {

	private final Plugin mPlugin;

	private CommandSender mSender;
	private final String mRoomId;
	private String mFullRoomName = "Not Set";
	private final Location mLowLoc;
	private final Location mHighLoc;

	private final Room mRoom;

	StructureParser(Plugin plug, Location senderLoc, CommandSender sender, String roomId, Location corner1, Location corner2) {
		this.mPlugin = plug;
		this.mSender = sender;
		for (Player p : senderLoc.getWorld().getPlayers()) {
			if (p.getLocation().distanceSquared(senderLoc) < 100) {
				this.mSender = p;
			}
		}
		this.mRoomId = roomId;
		this.mLowLoc = getActualCorner(corner1, corner2, false);
		this.mHighLoc = getActualCorner(corner1, corner2, true);
		this.mRoom = new Room();
	}

	void startParser() {
		this.parseAndSetBaseCoords();
		this.parser();
		this.mFullRoomName = this.mRoom.getType().name() + "/" + this.mRoomId;

		// Save the room
		String path = "roguelite/" + this.mFullRoomName;
		// Start saving, and then run actions when complete
		StructuresAPI.copyAreaAndSaveStructure(path, this.mLowLoc, this.mHighLoc).whenComplete((unused, ex) -> {
			// Saving complete
			if (ex != null) {
				// Completed with an error
				this.mSender.sendMessage(Component.text("Failed to save: " + ex.getMessage(), NamedTextColor.RED));
				Main.getInstance().getLogger().log(Level.WARNING, ex, () -> "Failed to save " + this.mRoomId + ": ");
			} else {
				// Completed successfully
				this.mRoom.setPath(path);
				String filePath = this.mPlugin.getDataFolder().getPath() + "/rooms/" + this.mFullRoomName + ".json";
				File f = new File(filePath);
				f.getParentFile().mkdirs();
				try (FileWriter file = new FileWriter(f, StandardCharsets.UTF_8)) {
					String str = new GsonBuilder().setPrettyPrinting().create().toJson(this.mRoom.toJsonObject());
					file.write(str);
					file.flush();
					this.mSender.sendMessage(filePath + " Written.\nContent:" + str);
				} catch (IOException e) {
					this.mSender.sendMessage(Component.text("Failed to save JSON: " + e.getMessage(), NamedTextColor.RED));
					Main.getInstance().getLogger().log(Level.WARNING, e, () -> "Failed to save JSON " + this.mRoomId + ": ");
				}
			}
		});
	}

	private void parser() {
		//go through every block of the structure
		Vector rs = this.mRoom.getSize();
		for (int x = 0; x <= rs.getBlockX(); x++) {
			for (int y = 0; y <= rs.getBlockY(); y++) {
				for (int z = 0; z <= rs.getBlockZ(); z++) {
					this.parseBlockAtRel(x, y, z);
				}
			}
		}
	}

	private void parseBlockAtRel(int x, int y, int z) {
		Block block = this.mLowLoc.clone().add(x, y, z).getBlock();

		switch (block.getType()) {
			// Door blocks
			case WHITE_STAINED_GLASS:
			case BLUE_STAINED_GLASS:
			case GREEN_STAINED_GLASS:
			case GRAY_STAINED_GLASS:
				this.parseDoor(block);
				break;
			//Objectives blocks
			case WHITE_BANNER:
			case BLUE_BANNER:
			case GREEN_BANNER:
			case GRAY_BANNER:
				this.parseObjective(block);
				break;
			//Loot Chest Blocks
			case MAGENTA_GLAZED_TERRACOTTA:
				this.parseChest(block);
				break;
			//Room Type Blocks
			case WHITE_CONCRETE:
			case YELLOW_CONCRETE:
			case RED_CONCRETE:
			case LIME_CONCRETE:
			case BLUE_CONCRETE:
			case WHITE_GLAZED_TERRACOTTA:
			case LIGHT_BLUE_GLAZED_TERRACOTTA:
			case BLUE_GLAZED_TERRACOTTA:
			case GRAY_GLAZED_TERRACOTTA:
			case BLACK_GLAZED_TERRACOTTA:
				this.parseRoomType(block);
				break;
			default:
				// Other blocks have no significance
				break;
		}
	}

	private void parseRoomType(Block block) {
		if (getObjectDirectionOnOuterShell(block) == BlockFace.SELF) {
			return;
		}
		switch (block.getType()) {
			case WHITE_CONCRETE:
				this.mRoom.setType(RoomType.NORMAL);
				break;
			case YELLOW_CONCRETE:
				this.mRoom.setType(RoomType.UTIL);
				break;
			case RED_CONCRETE:
				this.mRoom.setType(RoomType.DEADEND);
				break;
			case LIME_CONCRETE:
				this.mRoom.setType(RoomType.END);
				break;
			case BLUE_CONCRETE:
				this.mRoom.setType(RoomType.CORRIDOR);
				break;
			//Weight blocks
			case WHITE_GLAZED_TERRACOTTA:
				this.mRoom.setWeight(this.mRoom.getWeight() + 1);
				break;
			case LIGHT_BLUE_GLAZED_TERRACOTTA:
				this.mRoom.setWeight(this.mRoom.getWeight() + 10);
				break;
			case BLUE_GLAZED_TERRACOTTA:
				this.mRoom.setWeight(this.mRoom.getWeight() + 100);
				break;
			case GRAY_GLAZED_TERRACOTTA:
				this.mRoom.setWeight(this.mRoom.getWeight() + 1000);
				break;
			case BLACK_GLAZED_TERRACOTTA:
				this.mRoom.setWeight(this.mRoom.getWeight() + 10000);
				break;
			default:
				// Other blocks have no significance
				break;
		}
	}

	private void parseChest(Block block) {
		//if the block above it is not a biome glass, then it is not a loot chest
		// in the meantime, get that biome
		Biome biome = this.getBiomeFromGlassOrBanner(block.getRelative(BlockFace.UP));
		if (biome == Biome.NONE) {
			return;
		}

		// get the direction
		BlockFace direction = Utils.rotateClockwise(((Directional)block.getBlockData()).getFacing());

		// get the relative position
		Vector relPos = block.getLocation().clone().subtract(this.mLowLoc).toVector();

		this.mRoom.getLootChestList().add(new LootChest(direction, biome, relPos));
	}

	private void parseObjective(Block block) {

		Banner banner = (Banner)block.getState();
		// if the banner has patterns, then it is not an objective marker
		if (banner.numberOfPatterns() > 0) {
			return;
		}

		// get the direction
		BlockFace direction = ((Rotatable)banner.getBlockData()).getRotation();

		// get the biome
		Biome biome = getBiomeFromGlassOrBanner(block);
		if (biome == Biome.NONE) {
			return;
		}

		// get the relative position
		Vector relPos = block.getLocation().clone().subtract(this.mLowLoc).toVector();

		this.mRoom.getObjectiveList().add(new Objective(direction, biome, relPos));

	}

	private void parseDoor(Block block) {
		// make sure it is a door, by checking if the block above is a doorframe block
		if (block.getRelative(BlockFace.UP).getType() != Material.POLISHED_ANDESITE) {
			return;
		}
		// and by checking it is on the outer shell (get the direction at the same time)
		BlockFace direction = this.getObjectDirectionOnOuterShell(block);
		if (direction == BlockFace.SELF) {
			//no direction found, the block is not on outer shell, hence it's not a door
			return;
		}

		// get the biome
		Biome biome = getBiomeFromGlassOrBanner(block);
		if (biome == Biome.NONE) {
			return;
		}

		// get the relative position
		Vector relPos = block.getLocation().clone().subtract(this.mLowLoc).toVector();

		// create a door and add it to the doorList of the room
		Door d = new Door();
		d.setDirection(direction);
		d.setBiome(biome);
		d.setRelPos(relPos);
		this.mRoom.getDoorList().add(d);

	}

	private void parseAndSetBaseCoords() {
		// calculate room size
		this.mRoom.setSize(new Vector(
			this.mHighLoc.getBlockX() - this.mLowLoc.getBlockX(),
			this.mHighLoc.getBlockY() - this.mLowLoc.getBlockY(),
			this.mHighLoc.getBlockZ() - this.mLowLoc.getBlockZ()));
	}

	private Location getActualCorner(Location loc1, Location loc2, boolean wantMax) {
		BiFunction<Integer, Integer, Integer> cornerFunction;
		if (wantMax) {
			cornerFunction = Integer::max;
		} else {
			cornerFunction = Integer::min;
		}

		World world = loc1.getWorld();
		int x = cornerFunction.apply(loc1.getBlockX(), loc2.getBlockX());
		int y = cornerFunction.apply(loc1.getBlockY(), loc2.getBlockY());
		int z = cornerFunction.apply(loc1.getBlockZ(), loc2.getBlockZ());

		return new Location(world, x, y, z);
	}

	private BlockFace getObjectDirectionOnOuterShell(Block block) {
		if (block.getLocation().getBlockX() == mLowLoc.getBlockX()) {
			return BlockFace.WEST;
		} else if (block.getLocation().getBlockX() == mHighLoc.getBlockX()) {
			return BlockFace.EAST;
		} else if (block.getLocation().getBlockZ() == mLowLoc.getBlockZ()) {
			return BlockFace.NORTH;
		} else if (block.getLocation().getBlockZ() == mHighLoc.getBlockZ()) {
			return BlockFace.SOUTH;
		} else {
			return BlockFace.SELF;
		}
	}

	private Biome getBiomeFromGlassOrBanner(Block block) {
		switch (block.getType()) {
			case WHITE_STAINED_GLASS:
			case WHITE_BANNER:
				return Biome.ANY;
			case BLUE_STAINED_GLASS:
			case BLUE_BANNER:
				return Biome.WATER;
			case GREEN_STAINED_GLASS:
			case GREEN_BANNER:
				return Biome.LUSH;
			case GRAY_STAINED_GLASS:
			case GRAY_BANNER:
				return Biome.CITY;
			case YELLOW_STAINED_GLASS:
				return Biome.VAULT;
			default:
				// Other blocks have no significance
				break;
		}
		return Biome.NONE;
	}
}
