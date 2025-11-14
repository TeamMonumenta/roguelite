package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.Main;
import com.playmonumenta.roguelite.Utils;
import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.Config;
import com.playmonumenta.roguelite.enums.DungeonStatus;
import com.playmonumenta.roguelite.enums.RoomType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class Dungeon {

    private final Plugin plugin;
    private final boolean doDirectLog;

    private final List<Room> masterRoomPool;
    private final Location masterLocation;

    private Location centerLoc;
    public DungeonStatus status;
    public Exception calculationException;
    private int currentIteration;

	private Audience loggingAudience;

    private List<Room> unusedRoomPool;
    private List<Door> unusedDoorPool;

    public List<Room> usedRooms;

    private List<Hitbox> hitboxCollection;

    private List<Objective> selectedObjectives;
    public List<Objective> objectivePotentialSpawnPoints;
    public List<LootChest> selectedLootChests;
    public List<LootChest> lootChestPotentialSpawnPoints;

    public Dungeon(List<Room> roomPoolMaster, Location l, Plugin p, boolean directLog) {
        this.masterRoomPool = roomPoolMaster;
        this.masterLocation = l;
		this.centerLoc = l;
        this.status = DungeonStatus.NULL;
        this.plugin = p;
        this.doDirectLog = directLog;

    }

    private Dungeon init() {
		this.loggingAudience = Audience.empty();
        this.unusedRoomPool = new ArrayList<>();
        this.usedRooms = new ArrayList<>();

        this.selectedObjectives = new ArrayList<>();
        this.selectedLootChests = new ArrayList<>();
        this.lootChestPotentialSpawnPoints = new ArrayList<>();
        this.objectivePotentialSpawnPoints = new ArrayList<>();

        this.centerLoc = this.masterLocation;
        Location cl = this.centerLoc;

        // get a copy of the room pool
        for (Room r : this.masterRoomPool) {
            if (r.getWeight() != 0) {
                unusedRoomPool.add(new Room(r));
            }
        }

        // find players that are in range for live log
	    List<Player> loggingPlayers = new ArrayList<>();
        for (Player player : this.centerLoc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(this.centerLoc) <= 20000) {
                loggingPlayers.add(player);
            }
        }
		this.loggingAudience = Audience.audience(loggingPlayers);

        // create the basic doors
        this.unusedDoorPool = new ArrayList<>();
        this.unusedDoorPool.add(new Door(this.centerLoc.clone().add(0,4,3), Biome.getRandom(), BlockFace.SOUTH));

        // create hitboxes
        this.hitboxCollection = new ArrayList<>();
        this.hitboxCollection.add(new Hitbox(cl.clone().add(-18, -88, -28), cl.clone().add(15, 166, 3))); //Central box
        this.hitboxCollection.add(new Hitbox(cl.clone().add(-127, -89, -136), cl.clone().add(128, 166, -132))); //north instance limit
        this.hitboxCollection.add(new Hitbox(cl.clone().add(128, -89, -132), cl.clone().add(132, 166, 123))); //east instance limit
        this.hitboxCollection.add(new Hitbox(cl.clone().add(-127, -89, 123), cl.clone().add(128, 166, 127))); //south instance limit
        this.hitboxCollection.add(new Hitbox(cl.clone().add(-131, -89, -132), cl.clone().add(-127, 166, 123))); //west instance limit

        this.status = DungeonStatus.INITIALIZED;
        return this;
    }

    /*
    *
    * Calculation
    *
    */

    private void calculate() throws Exception {
        // only calculate if the dungeon is initialized
        if (this.status != DungeonStatus.INITIALIZED) {
            this.directLog(Component.text("Dungeon calculation aborted: Dungeon is not initialised. \nCurrent status: " + this.status.name() + "  Should be: " + DungeonStatus.INITIALIZED.name(), NamedTextColor.DARK_RED));
            throw new Exception("Dungeon calculation aborted: Dungeon is not initialised.");
        }
        this.currentIteration = 1;

        // main generation loop
        this.mainGenerationLoop();

        this.selectObjectives();
        this.selectChests();

        if (!this.isSpawnReady()) {
            this.directLog("Calculation Failed at end: " + this.calculationException.getMessage());
            throw this.calculationException;
        } else {
            this.status = DungeonStatus.CALCULATED;
        }
    }

    private void selectObjectives() {
        // among the list of possible objective spawns, select 3 to be actual objective ender crystals
        // others will become chests
        Random rand = new Random();
        this.selectedObjectives.add(this.objectivePotentialSpawnPoints.remove(rand.nextInt(this.objectivePotentialSpawnPoints.size())));
        this.selectedObjectives.add(this.objectivePotentialSpawnPoints.remove(rand.nextInt(this.objectivePotentialSpawnPoints.size())));
        this.selectedObjectives.add(this.objectivePotentialSpawnPoints.remove(rand.nextInt(this.objectivePotentialSpawnPoints.size())));
    }

    private void selectChests() {
        // among the list of possible chests spawn, select enough, at random, to get a total chest count equal as the
        // count specified in config class. others won't spawn.
        int amountToSpawn = Config.CHESTCOUNT - this.objectivePotentialSpawnPoints.size();
        Random rand = new Random();
        for (int i = 0; i < amountToSpawn; i++) {
            this.selectedLootChests.add(this.lootChestPotentialSpawnPoints.remove(rand.nextInt(this.lootChestPotentialSpawnPoints.size())));
        }
    }

    private void mainGenerationLoop() {
        boolean generatedDeadend = false;

        int roomsToSpawnEnd = 0;
        int roomsToSpawnUtil = 0;

        while (!this.unusedDoorPool.isEmpty()) {
            // select a random door from the list of opened doors.
            Door currentDoor = this.unusedDoorPool.remove(new Random().nextInt(this.unusedDoorPool.size()));
            currentDoor.getLocation().setWorld(centerLoc.getWorld());

            boolean result = false;

            // if the door is further than the distance threshold or the amount of rooms is enough, generate a deadend
            if (this.currentIteration > Config.ROOMSTOSPAWN) {
                this.generateDeadend(currentDoor);
                continue;
            }

            // sets room type priorities
            if (this.currentIteration == 25 && !generatedDeadend) {
                roomsToSpawnEnd++;
            }
            if (this.currentIteration % 5 == 0 && this.currentIteration < 25 && !generatedDeadend) {
                roomsToSpawnUtil++;
            }


            // attempt to spawn a room following a priority queue
            if (roomsToSpawnEnd > 0) {
                result = this.attemptGenerateRoom(currentDoor, RoomType.END);
                if (result) {
                    roomsToSpawnEnd--;
                }
            }
            if (!result && roomsToSpawnUtil > 0) {
                result = this.attemptGenerateRoom(currentDoor, RoomType.UTIL);
                if (result) {
                    roomsToSpawnUtil--;
                }
            }
            if (!result) {
                result = this.attemptGenerateRoom(currentDoor, RoomType.NORMAL);
            }


            if (!result) {
                // if the room is not correctly spawned
                this.generateDeadend(currentDoor);
                generatedDeadend = true;
            } else {
                generatedDeadend = false;
            }
        }
    }

    private boolean isSpawnReady() {
        int endRoomCount = 0;
        for (Room r : this.usedRooms) {
            if (r.getType() == RoomType.END) {
                endRoomCount++;
            }
        }
        if (endRoomCount != 1) {
            this.calculationException = new Exception("End room count is wrong ( is " + endRoomCount + ", should be 1)");
            //return false;
        }
        this.directLog("Calculation done. Ready for Spawn");
        this.status = DungeonStatus.CALCULATED;
        return true;
    }

    private boolean attemptGenerateRoom(Door curDoor, RoomType roomType) {
        // go though every unused room, and list the doors that can connect with the current door.
	    List<Door> compatibleDoors = this.selectCompatibleDoors(roomType, curDoor.getDirection(), curDoor.getBiome());

        // go though all these doors, at random. until one of them yields a compatible room, or no doors is left to test
        boolean success = false;
        Room testedRoom;
        Door testedDoor = new Door();
        while (!compatibleDoors.isEmpty()) {
            // get a random door
            testedDoor = Utils.getRandomDoorFromWeightedList(compatibleDoors);
            // get this door's room
            testedRoom = testedDoor.getParentRoom();
            // find this room coordinates, and calculate its (if supposedly spawned) hitbox
            testedRoom.setLocation(curDoor.getLocation().clone().subtract(testedDoor.getRelPos()));
            testedRoom.setHitbox(new Hitbox(testedRoom));
            // test if that new room hitbox collides with the already spawned ones
            boolean isColliding = false;
            for (Hitbox h : this.hitboxCollection) {
                if (testedRoom.getHitbox().collidesWith(h)) {
                    isColliding = true;
                    break;
                }
            }
            if (!isColliding) {
                success = true;
                break;
            }
            compatibleDoors.remove(testedDoor);
        }
        if (!success) {
            // no compatible room found. abort the method, and fallback.
            return false;
        }

        this.currentIteration++;

        this.placeRoom(testedDoor.getParentRoom(), testedDoor);
        return true;
    }

    private void generateDeadend(Door curDoor) {
	    List<Door> compatibleDoors = this.selectCompatibleDoors(RoomType.DEADEND, curDoor.getDirection().getOppositeFace(), curDoor.getBiome());
        Door d = compatibleDoors.get(0); //there should be only 1 compatible deadend
        // get this door's room
        Room r = d.getParentRoom();
        // find this room coordinates, and calculate its hitbox
        r.setLocation(curDoor.getLocation().clone().subtract(d.getRelPos()));
        r.setHitbox(new Hitbox(r));
        // place a copy of the room back into the unused pool, as dead ends needs to be used multiple times
        this.unusedRoomPool.add(new Room(r));
        this.placeRoom(r, d);
    }

    private void placeRoom(Room testedRoom, Door testedDoor) {
        //get the room
        this.hitboxCollection.add(new Hitbox(testedRoom));
        Location pos = testedRoom.getLocation().clone();

        //add new room's doors to the list of active doors
	    List<Door> newDoors = testedRoom.getDoorList();
        for (Door d : newDoors) {
            d.setLocation(pos.clone().add(d.getRelPos()));
            if (d == testedDoor) {
                continue;
            }
            this.unusedDoorPool.add(d);
        }

        // add objectives and chests
        for (Objective o : testedRoom.getObjectiveList()) {
            o.setLocation(pos.clone().add(o.getRelPos()));
            this.objectivePotentialSpawnPoints.add(o);
        }
        for (LootChest c : testedRoom.getLootChestList()) {
            c.setLocation(pos.clone().add(c.getRelPos()));
            this.lootChestPotentialSpawnPoints.add(c);
        }


        // remove the room from the room pool
        this.unusedRoomPool.remove(testedRoom);
        // add it to spawned room list
        this.usedRooms.add(testedRoom);
    }

    private List<Door> selectCompatibleDoors(RoomType type, BlockFace direction, Biome biome) {
	    List<Door> out = new ArrayList<>();
        for (Room r : this.unusedRoomPool) {
            if (r.getType() == type) {
                for (Door d: r.getDoorList()) {
                    if (d.correspondsTo(direction, biome)) {
                        out.add(d);
                    }
                }
            }
        }
        return out;
    }

    public Dungeon calculateWithRetries(int maxAttempts) {
        Dungeon out = this.init();
        try {
            out.directLog("Starting Calculation");
	        out.calculate();
        } catch (Exception e) {
            if (maxAttempts > 0) {
                out.directLog("Calculation Failed... Retrying");
                out = this.calculateWithRetries(maxAttempts - 1);
            } else {
                this.calculationException = e;
                StringBuilder s = new StringBuilder();
                s.append("Calculation Failed. Please contact a moderator, as our instance cannot be generated.\n");
                s.append("Latest error:\n");
                s.append(e);
                s.append("\n");
                for (StackTraceElement elem : e.getStackTrace()) {
                    s.append(elem.toString());
                    s.append("\n");
                }
                out.directLog(s.toString());
                Main.getInstance().getLogger().log(Level.WARNING, e, s::toString);
            }
        }
        return out;
    }

    /*
    *
    * Spawn
    *
    */


    public void spawn() {
        if (this.status != DungeonStatus.CALCULATED) {
            this.directLog(Component.text("Dungeon spawn aborted: Dungeon is not calculated. \nCurrent status: " + this.status.name() + "  Should be: " + DungeonStatus.INITIALIZED.name(), NamedTextColor.DARK_RED));
            return;
        }
        // sort rooms of different kinds in different lists. so that their order of spawn can be chosen
        Map<RoomType, List<Room>> roomMap = new HashMap<>();
        for (Room r : this.usedRooms) {
            if (!roomMap.containsKey(r.getType())) {
                roomMap.put(r.getType(), new ArrayList<>());
            }
            roomMap.get(r.getType()).add(r);
        }

        // spawn things
        try {

	        // Load main rooms
	        List<CompletableFuture<Void>> futures = new ArrayList<>(this.beginSpawningRoomList(roomMap.getOrDefault(RoomType.NORMAL, null)));

            // Wait for all regular rooms to load before loading other types
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get();
            futures.clear();

            // Load other types of rooms
            futures.addAll(this.beginSpawningRoomList(roomMap.getOrDefault(RoomType.UTIL, null)));
            futures.addAll(this.beginSpawningRoomList(roomMap.getOrDefault(RoomType.END, null)));
            futures.addAll(this.beginSpawningRoomList(roomMap.getOrDefault(RoomType.DEADEND, null)));

            // Wait for all rooms to finish spawning
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get();

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                this.spawnObjectives();
                this.spawnChests();
                this.openFirstPath();
                this.directLog("Done.");
            });
        } catch (Exception ex) {
            this.directLog("Failed to generate instance: " + ex.getMessage());
            Main.getInstance().getLogger().log(Level.WARNING, "Failed to generate instance", ex);
        }
        this.status = DungeonStatus.SPAWNED;
    }

    private void openFirstPath() {
        Location l = this.centerLoc.clone().add(0, 7, 2);
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            l.getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(0, -1, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(0, 1, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(0, 2, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(-1, -1, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(-1, 0, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(-1, 1, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(1, -1, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(1, 0, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
            l.clone().add(1, 1, 0).getBlock().setType(Material.LIGHT_BLUE_STAINED_GLASS);
        });
    }

    private void spawnObjectives() {
        this.directLog("Spawning objectives");
        for (Objective o : this.selectedObjectives) {
            o.spawnObjective();
        }
    }

    private void spawnChests() {
        this.directLog("Spawning chests");
        for (Objective o : this.objectivePotentialSpawnPoints) {
            o.spawnChest();
        }
        for (LootChest c : this.selectedLootChests) {
            c.spawnChest();
        }
        for (LootChest c : this.lootChestPotentialSpawnPoints) {
            c.spawnAir();
        }
    }

    private List<CompletableFuture<Void>> beginSpawningRoomList(@Nullable List<Room> rooms) {
        if (rooms == null) {
            return List.of();
        }
        this.directLog("Spawning " + rooms.size() + " rooms of type " + rooms.get(0).getType().name());
        List<CompletableFuture<Void>> futures = new ArrayList<>(rooms.size());
        for (Room r : rooms) {
            futures.add(r.loadStructureAsync());
            try {
                Thread.sleep(25);
            } catch (Exception ignored) {
                // Failing to sleep is a non-issue
            }
        }
        return futures;
    }

    private void directLog(String text) {
		directLog(Component.text(text));
    }

	private void directLog(Component component) {
        if (!doDirectLog) {
            return;
        }
	    loggingAudience.sendMessage(Component.text("", NamedTextColor.AQUA)
		    .append(component));
    }
}
