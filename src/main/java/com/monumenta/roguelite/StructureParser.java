package com.monumenta.roguelite;

import com.google.gson.GsonBuilder;
import com.monumenta.roguelite.enums.Biome;
import com.monumenta.roguelite.enums.RoomType;
import com.monumenta.roguelite.objects.Door;
import com.monumenta.roguelite.objects.LootChest;
import com.monumenta.roguelite.objects.Objective;
import com.monumenta.roguelite.objects.Room;
import com.playmonumenta.structures.StructuresAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class StructureParser {

    private final Plugin plugin;

    private CommandSender sender;
    private final String[] commandArgs;
    private String fullRoomName;
    private final Location lowLoc;
    private final Location highLoc;

    private final Room room;

    StructureParser(Plugin plug, Location senderLoc, CommandSender sender, String[] args) {
        this.plugin = plug;
        this.sender = sender;
        for (Player p : senderLoc.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(senderLoc) < 100) {
                this.sender = p;
            }
        }
        this.commandArgs = args;
        this.lowLoc = senderLoc.clone().getBlock().getLocation();
        this.highLoc = this.lowLoc.clone();
        this.room = new Room();
    }

    void startParser() {
        if (this.commandArgs.length < 8) {
            this.sender.sendMessage(ChatColor.RED + "At least 7 arguments are required\n/roguelite savestructure <id> <x1> <y1> <z1> <x2> <y2> <z2>");
            return;
        }

        this.parseAndSetBaseCoords();
        this.parser();
        this.fullRoomName = this.room.getType().name() + "/" + this.commandArgs[1];

        // Save the room
        String path =  "roguelite/" + this.fullRoomName;
        // Start saving, and then run actions when complete
        StructuresAPI.copyAreaAndSaveStructure(path, this.lowLoc, this.highLoc).whenComplete((unused, ex) -> {
            // Saving complete
            if (ex != null) {
                // Completed with an error
                this.sender.sendMessage(ChatColor.RED + "Failed to save: " + ex.getMessage());
                ex.printStackTrace();
            } else {
                // Completed successfully
                this.room.setPath(path);
                String filePath = this.plugin.getDataFolder().getPath() + "/rooms/" + this.fullRoomName + ".json";
                File f = new File(filePath);
                f.getParentFile().mkdirs();
                try (FileWriter file = new FileWriter(f)) {
                    String str = new GsonBuilder().setPrettyPrinting().create().toJson(this.room.toJsonObject());
                    file.write(str);
                    file.flush();
                    this.sender.sendMessage(filePath + " Writen.\nContent:" + str);
                } catch (IOException e) {
                    this.sender.sendMessage(ChatColor.RED + "Failed to save JSON: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void parser() {
        //go through every block of the structure
        Vector rs = this.room.getSize();
        for (int x = 0; x <= rs.getBlockX(); x++) {
            for (int y = 0; y <= rs.getBlockY(); y++) {
                for (int z = 0; z <= rs.getBlockZ(); z++) {
                    this.parseBlockAtRel(x,y,z);
                }
            }
        }
    }

    private void parseBlockAtRel(int x, int y, int z) {
        Block block = this.lowLoc.clone().add(x,y,z).getBlock();

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
                this.room.setType(RoomType.NORMAL);
                break;
            case YELLOW_CONCRETE:
                this.room.setType(RoomType.UTIL);
                break;
            case RED_CONCRETE:
                this.room.setType(RoomType.DEADEND);
                break;
            case LIME_CONCRETE:
                this.room.setType(RoomType.END);
                break;
            case BLUE_CONCRETE:
                this.room.setType(RoomType.CORRIDOR);
                break;
            //Weight blocks
            case WHITE_GLAZED_TERRACOTTA:
                this.room.setWeight(this.room.getWeight() + 1);
                break;
            case LIGHT_BLUE_GLAZED_TERRACOTTA:
                this.room.setWeight(this.room.getWeight() + 10);
                break;
            case BLUE_GLAZED_TERRACOTTA:
                this.room.setWeight(this.room.getWeight() + 100);
                break;
            case GRAY_GLAZED_TERRACOTTA:
                this.room.setWeight(this.room.getWeight() + 1000);
                break;
            case BLACK_GLAZED_TERRACOTTA:
                this.room.setWeight(this.room.getWeight() + 10000);
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
        Vector relPos = block.getLocation().clone().subtract(this.lowLoc).toVector();

        this.room.getLootChestList().add(new LootChest(direction, biome, relPos));
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
        Vector relPos = block.getLocation().clone().subtract(this.lowLoc).toVector();

        this.room.getObjectiveList().add(new Objective(direction, biome, relPos));

    }

    private void parseDoor(Block block) {
        // make sure it is a door, by checking if the block above is a doorframe block
        if (block.getRelative(BlockFace.UP).getType() != Material.POLISHED_ANDESITE) {
            return;
        }
        // and by checking it is on the outer shell (get the direction at the same time)
        BlockFace direction = this.getObjectDirectionOnOuterShell(block);
        if (direction == BlockFace.SELF) {
            //no direction found, the block is not on outer shell, hence its not a door
            return;
        }

        // get the biome
        Biome biome = getBiomeFromGlassOrBanner(block);
        if (biome == Biome.NONE) {
            return;
        }

        // get the relative position
        Vector relPos = block.getLocation().clone().subtract(this.lowLoc).toVector();

        // create a door and add it to the doorlist of the room
        Door d = new Door();
        d.setDirection(direction);
        d.setBiome(biome);
        d.setRelPos(relPos);
        this.room.getDoorList().add(d);

    }

    private void parseAndSetBaseCoords() {
        // read the commands argument
        this.lowLoc.setX(Integer.parseInt(this.commandArgs[2]));
        this.lowLoc.setY(Integer.parseInt(this.commandArgs[3]));
        this.lowLoc.setZ(Integer.parseInt(this.commandArgs[4]));
        this.highLoc.setX(Integer.parseInt(this.commandArgs[5]));
        this.highLoc.setY(Integer.parseInt(this.commandArgs[6]));
        this.highLoc.setZ(Integer.parseInt(this.commandArgs[7]));
        // find the lowest coord for each axis, and invert them if they are in the wrong order
        if (this.lowLoc.getBlockX() > this.highLoc.getBlockX()) {
            int tmp = this.highLoc.getBlockX();
            this.highLoc.setX(this.lowLoc.getBlockX());
            this.lowLoc.setX(tmp);
        }
        if (this.lowLoc.getBlockY() > this.highLoc.getBlockY()) {
            int tmp = this.highLoc.getBlockY();
            this.highLoc.setY(this.lowLoc.getBlockY());
            this.lowLoc.setY(tmp);
        }
        if (this.lowLoc.getBlockZ() > this.highLoc.getBlockZ()) {
            int tmp = this.highLoc.getBlockZ();
            this.highLoc.setZ(this.lowLoc.getBlockZ());
            this.lowLoc.setZ(tmp);
        }
        // calculate room size
        this.room.setSize(new Vector(
                this.highLoc.getBlockX() - this.lowLoc.getBlockX(),
                this.highLoc.getBlockY() - this.lowLoc.getBlockY(),
                this.highLoc.getBlockZ() - this.lowLoc.getBlockZ()));
    }

    private BlockFace getObjectDirectionOnOuterShell(Block block) {
        if (block.getLocation().getBlockX() == lowLoc.getBlockX()) {
            return BlockFace.WEST;
        } else if (block.getLocation().getBlockX() == highLoc.getBlockX()) {
            return BlockFace.EAST;
        } else if (block.getLocation().getBlockZ() == lowLoc.getBlockZ()) {
            return BlockFace.NORTH;
        } else if (block.getLocation().getBlockZ() == highLoc.getBlockZ()) {
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
