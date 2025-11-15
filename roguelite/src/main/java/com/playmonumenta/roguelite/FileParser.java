package com.playmonumenta.roguelite;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.RoomType;
import com.playmonumenta.roguelite.objects.Door;
import com.playmonumenta.roguelite.objects.LootChest;
import com.playmonumenta.roguelite.objects.Objective;
import com.playmonumenta.roguelite.objects.Room;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FileParser {

	static List<Room> loadRooms(Plugin plugin, @Nullable CommandSender sender) {
		List<Room> out = new ArrayList<>();
		String roomsPath = plugin.getDataFolder().getPath() + "/rooms";
		Gson gson = new Gson();

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
						try (FileReader reader = new FileReader(fileName, StandardCharsets.UTF_8)) {
							outLog.append(file.getName());
							out.add(parseFile(gson.fromJson(reader, JsonObject.class)));
							outLog.append(" | ");
						} catch (IOException e) {
							Main.getInstance().getLogger().log(Level.WARNING, "Failed to parse room file at " + fileName, e);
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

	static List<Door> parseDoorList(Room parentRoom, JsonArray array) {
		List<Door> out = new ArrayList<>();

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

	static List<Objective> parseObjectiveList(JsonArray array) {
		List<Objective> out = new ArrayList<>();

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

	static List<LootChest> parseChestList(JsonArray array) {
		List<LootChest> out = new ArrayList<>();

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
