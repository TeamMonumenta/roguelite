package com.monumenta.roguelite;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.monumenta.roguelite.enums.Biome;
import com.monumenta.roguelite.enums.RoomType;
import com.monumenta.roguelite.objects.Door;
import com.monumenta.roguelite.objects.LootChest;
import com.monumenta.roguelite.objects.Objective;
import com.monumenta.roguelite.objects.Room;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class FileParser {

    static ArrayList<Room> loadFiles(Plugin plugin, CommandSender sender) {
        ArrayList<Room> out = new ArrayList<>();
        String roomsPath = plugin.getDataFolder().getPath() + "/rooms";
        JsonParser jsonParser = new JsonParser();

        File folder = new File(roomsPath);
        folder.mkdirs();
        for (File subfolder : folder.listFiles()) {
            if (subfolder.isDirectory()) {
                if (sender != null) {
                    sender.sendMessage(subfolder.getName());
                }
                StringBuilder outLog = new StringBuilder();
                for (File file : subfolder.listFiles()) {
                    if (file.isFile()) {
                        String fileName = file.getPath();
                        try (FileReader reader = new FileReader(fileName))
                        {
                            outLog.append(file.getName());
                            out.add(parseFile(jsonParser.parse(reader)));
                            outLog.append(" | ");
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JsonParseException e) {
                            System.out.println("Json Parser crashed for file " + fileName);
                            throw e;
                        }
                    }
                }
                if (sender != null) {
                    sender.sendMessage(outLog.toString());
                }
            }
        }
        return out;
    }

    static Room parseFile(JsonElement root) {
        Room out = new Room();

        JsonObject rootObj = root.getAsJsonObject();
        out.setLocation(new Location(null, 0, 0, 0));
        out.setPath(rootObj.get("path").getAsString());
        out.setSize(parseVector(rootObj.get("size").getAsJsonObject()));
        out.setWeight(rootObj.get("weight").getAsInt());
        out.setType(RoomType.valueOf(rootObj.get("type").getAsString()));
        out.setDoorList(parseDoorList(out, rootObj.get("doors").getAsJsonArray()));
        out.setObjectiveList(parseObjectiveList(rootObj.get("objectives").getAsJsonArray()));
        out.setLootChestList(parseChestList(rootObj.get("chests").getAsJsonArray()));
        return out;
    }

    static ArrayList<Door> parseDoorList(Room parentRoom, JsonArray array) {
        ArrayList<Door> out = new ArrayList<>();

        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            Door current = new Door();
            current.setRelPos(parseVector(obj));
            current.setBiome(Biome.valueOf(obj.get("biome").getAsString()));
            current.setDirection(BlockFace.valueOf(obj.get("dir").getAsString()));
            current.setParentRoom(parentRoom);
            out.add(current);
        }
        return out;
    }

    static ArrayList<Objective> parseObjectiveList(JsonArray array) {
        ArrayList<Objective> out = new ArrayList<>();

        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            Objective current = new Objective();
            current.setRelPos(parseVector(obj));
            current.setBiome(Biome.valueOf(obj.get("biome").getAsString()));
            current.setDirection(BlockFace.valueOf(obj.get("dir").getAsString()));
            out.add(current);
        }
        return out;
    }

    static ArrayList<LootChest> parseChestList(JsonArray array) {
        ArrayList<LootChest> out = new ArrayList<>();

        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            LootChest current = new LootChest();
            current.setRelPos(parseVector(obj));
            current.setBiome(Biome.valueOf(obj.get("biome").getAsString()));
            current.setDirection(BlockFace.valueOf(obj.get("dir").getAsString()));
            out.add(current);
        }
        return out;
    }

    static Vector parseVector(JsonObject obj) {
        Vector out = new Vector();

        out.setX(obj.get("x").getAsInt());
        out.setY(obj.get("y").getAsInt());
        out.setZ(obj.get("z").getAsInt());
        return out;
    }
}
