package com.monumenta.roguelite.objects;

import com.monumenta.roguelite.enums.Biome;
import com.monumenta.roguelite.enums.RoomType;

import java.util.*;
import java.util.stream.Collectors;

public class Stats {

    private long startTime;
    private int targetDungeonCount;
    private int dungeonCount;
    private int successfulDungeonCount;
    private int unsuccessfulDungeonCount;
    private Map<String, Integer> dungeonCalculationFailures;
    private int unusedChestsTotal;
    private int spawnedChestsTotal;
    private int spawnedChestsObjectiveTotal;
    private int spawnedChestsNormalTotal;
    private Map<Biome, Integer> spawnedChestsObjective;
    private Map<Biome, Integer> spawnedChestsNormal;
    private int roomTotal;
    private Map<RoomType, Integer> roomTypeDistrib;
    private Map<RoomType, Map<String, Integer>> roomDistrib;
    private Map<String, Integer> roomWeightMap;

    public Stats() {
        this.dungeonCalculationFailures = new HashMap<>();
        this.spawnedChestsObjective = new HashMap<>();
        this.spawnedChestsNormal = new HashMap<>();
        this.roomDistrib = new HashMap<>();
        this.roomTypeDistrib = new HashMap<>();
        this.roomWeightMap = new HashMap<>();
        this.startTime = System.currentTimeMillis();
    }

    public void addToTargetDungeonCount(int amount) {
        this.targetDungeonCount += amount;
    }

    public void addToDungeonCount(int amount) {
        this.dungeonCount += amount;
    }

    public void addToSuccessfulDungeonCount(int amount) {
        this.successfulDungeonCount += amount;
        this.addToDungeonCount(amount);
    }

    public void addToUnsuccessfulDungeonCount(int amount) {
        this.addToDungeonCount(amount);
        this.unsuccessfulDungeonCount += amount;
    }

    public void addTodungeonCalculationFailures(String failure, int amount) {
        this.dungeonCalculationFailures.put(failure, amount +
                this.dungeonCalculationFailures.getOrDefault(failure, 0));
    }

    public void addToUnusedChestsTotal(int amount) {
        this.unusedChestsTotal += amount;
    }

    public void addToSpawnedChestsTotal(int amount) {
        this.spawnedChestsTotal += amount;
    }

    public void addToSpawnedChestsObjectiveTotal(int amount) {
        this.addToSpawnedChestsTotal(amount);
        this.spawnedChestsObjectiveTotal += amount;
    }

    public void addToSpawnedChestsNormalTotal(int amount) {
        this.addToSpawnedChestsTotal(amount);
        this.spawnedChestsNormalTotal += amount;
    }

    public void addToSpawnedChests(Objective o, int amount) {
        this.addToSpawnedChestsObjectiveTotal(amount);
        this.spawnedChestsObjective.put(o.getBiome(), amount +
                this.spawnedChestsObjective.getOrDefault(o.getBiome(), 0));
    }

    public void addToSpawnedChests(LootChest c, int amount) {
        this.addToSpawnedChestsNormalTotal(amount);
        this.spawnedChestsNormal.put(c.getBiome(), amount +
                this.spawnedChestsNormal.getOrDefault(c.getBiome(), 0));
    }

    public void addToRoomTotal(int amount){
        this.roomTotal += amount;
    }

    public void addToRoomTypeDistrib(RoomType type, int amount) {
        this.addToRoomTotal(amount);
        this.roomTypeDistrib.put(type, this.roomTypeDistrib.getOrDefault(type, 0) + amount);
    }

    public void addToRoomDistrib(Room r, int amount) {
        this.addToRoomTypeDistrib(r.getType(), amount);
        Map<String, Integer> m = this.roomDistrib.computeIfAbsent(r.getType(), l -> new HashMap<>());
        String id = r.getPath().substring(r.getPath().lastIndexOf("/"));
        this.addToRoomWeightMap(id, r.getWeight());
        m.put(id, m.getOrDefault(id, 0) + amount);
    }

    public void addToRoomWeightMap(String id, int value) {
        this.roomWeightMap.putIfAbsent(id, value);
    }

    public long getStartTime() {
        return this.startTime;
    }

    public int getDungeonCount() {
        return this.dungeonCount;
    }

    public int getTargetDungeonCount() {
        return this.targetDungeonCount;
    }

    public int getSuccessfulDungeonCount() {
        return this.successfulDungeonCount;
    }

    public int getUnsuccessfulDungeonCount() {
        return this.unsuccessfulDungeonCount;
    }

    public int getUnusedChestsTotal() {
        return this.unusedChestsTotal;
    }

    public int getSpawnedChestsTotal() {
        return this.spawnedChestsTotal;
    }

    public int getSpawnedChestsNormalTotal() {
        return this.spawnedChestsNormalTotal;
    }

    public int getSpawnedChestsObjectiveTotal() {
        return this.spawnedChestsObjectiveTotal;
    }

    public Map<Biome, Integer> getSpawnedChestsNormal() {
        return this.spawnedChestsNormal.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Map<Biome, Integer> getSpawnedChestsObjective() {
        return this.spawnedChestsObjective.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Map<String, Integer> getDungeonCalculationFailures() {
        return this.dungeonCalculationFailures.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public int getRoomTotal() {
        return roomTotal;
    }

    public Map<RoomType, Integer> getRoomTypeDistrib() {
        return this.roomTypeDistrib.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Map<String, Integer> getRoomDistrib(RoomType type) {
        return this.roomDistrib.get(type).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Map<String, Integer> getRoomWeightMap() {
        return this.roomWeightMap;
    }
}
