package com.monumenta.roguelite.objects;

import com.fastasyncworldedit.core.extent.clipboard.DiskOptimizedClipboard;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.monumenta.roguelite.enums.RoomType;
import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.StructuresPlugin;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class Room {
    private String path;
    private RoomType type;
    private Vector size;
    private Location location;
    private Hitbox hitbox;
    private int weight;

    ArrayList<Objective> objectiveList;
    ArrayList<LootChest> lootChestList;
    ArrayList<Door> doorList;

    // copy constructor
    public Room(Room old) {
        this();
        this.path = old.path;
        this.type = old.type;
        this.size = old.size;
        this.location = old.location.clone();
        this.weight = old.weight;
        this.hitbox = old.hitbox;

        for (LootChest c : old.lootChestList) {
            this.lootChestList.add(new LootChest(c));
        }
        for (Objective o : old.objectiveList) {
            this.objectiveList.add(new Objective(o));
        }
        for (Door d : old.doorList) {
            Door n = new Door(d);
            n.setParentRoom(this);
            this.doorList.add(n);
        }
    }

    //basic constructor
    public Room() {
        this.location = new Location(null, 0, 0, 0);
        this.objectiveList = new ArrayList<>();
        this.lootChestList = new ArrayList<>();
        this.doorList = new ArrayList<>();
        this.type = RoomType.NONE;
    }

    //getters

    public String getPath() {
        return path;
    }

    public RoomType getType() {
        return this.type;
    }

    public Vector getSize() {
        return this.size;
    }

    public Location getLocation() {
        return this.location;
    }

    public Hitbox getHitbox() {
        return this.hitbox;
    }

    public int getWeight() {
        return this.weight;
    }

    public ArrayList<Door> getDoorList() {
        return this.doorList;
    }

    public ArrayList<Objective> getObjectiveList() {
        return this.objectiveList;
    }

    public ArrayList<LootChest> getLootChestList() {
        return this.lootChestList;
    }


    /**
     *  An experimental version of this. works differently than StructuresAPI. Seems to be faster and more reliable.
     */
    public synchronized void experimentalLoadStructure() {


        File file = new File(Paths.get(StructuresPlugin.getInstance().getDataFolder().toString(), "structures", path + ".schematic").toString());

        ClipboardFormat format = ClipboardFormats.findByAlias("sponge");
        Clipboard newClip;
        try {
            newClip = format.load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        BlockArrayClipboard clipboard;
        if (newClip instanceof BlockArrayClipboard) {
            clipboard = (BlockArrayClipboard)newClip;
        } else {
            if (!(newClip instanceof DiskOptimizedClipboard)) {
                throw new RuntimeException();
            }

            clipboard = ((DiskOptimizedClipboard)newClip).toClipboard();
        }
        com.sk89q.worldedit.world.World adapted = BukkitAdapter.adapt(location.getWorld());
        EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(adapted, -1);
        Operation operation = new ClipboardHolder(clipboard).createPaste(session)
                .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                .copyEntities(true).copyBiomes(true).build();

        try {
            Operations.complete(operation);
            session.flushQueue();
        } catch (Exception e) {
            System.out.println("Could not paste: ");
            e.printStackTrace();
        }
    }
    public CompletableFuture<Void> loadStructureAsync() {

        return StructuresAPI.loadAndPasteStructure(this.path, this.location, true);
    }

    // setters

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public void setSize(Vector size) {
        this.size = size;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setHitbox(Hitbox hitbox) {
        this.hitbox = hitbox;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public void setDoorList(ArrayList<Door> doorList) {
        this.doorList = doorList;
    }

    public void setObjectiveList(ArrayList<Objective> objectiveList) {
        this.objectiveList = objectiveList;
    }

    public void setLootChestList(ArrayList<LootChest> lootChestList) {
        this.lootChestList = lootChestList;
    }

    // methods

    public JsonObject toJsonObject() {
        JsonObject room = new JsonObject();
        JsonObject size = new JsonObject();
        JsonArray doors = new JsonArray();
        JsonArray objectives = new JsonArray();
        JsonArray chests = new JsonArray();

        Vector roomSize = this.getSize();
        size.addProperty("x", roomSize.getBlockX());
        size.addProperty("y", roomSize.getBlockY());
        size.addProperty("z", roomSize.getBlockZ());

        for (Door d : doorList) {
            doors.add(d.toJsonObject());
        }
        for (Objective o : objectiveList) {
            objectives.add(o.toJsonObject());
        }
        for (LootChest c : lootChestList) {
            chests.add(c.toJsonObject());
        }

        room.addProperty("path", this.path);
        room.add("size", size);
        room.addProperty("type", this.type.name());
        room.addProperty("weight", this.weight);
        room.add("doors", doors);
        room.add("objectives", objectives);
        room.add("chests", chests);
        return room;
    }
}
