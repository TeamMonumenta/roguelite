package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.RoomType;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Stats {

    private final long startTime;
    private int targetDungeonCount;
    private int dungeonCount;
    private int successfulDungeonCount;
    private int unsuccessfulDungeonCount;
    private final Map<String, Integer> dungeonCalculationFailures;
    private int unusedChestsTotal;
    private int spawnedChestsTotal;
    private int spawnedChestsObjectiveTotal;
    private int spawnedChestsNormalTotal;
    private final Map<Biome, Integer> spawnedChestsObjective;
    private final Map<Biome, Integer> spawnedChestsNormal;
    private int roomTotal;
    private final Map<RoomType, Integer> roomTypeDistribution;
    private final Map<RoomType, Map<String, Integer>> roomDistribution;
    private final Map<String, Integer> roomWeightMap;

    public Stats() {
        this.dungeonCalculationFailures = new HashMap<>();
        this.spawnedChestsObjective = new HashMap<>();
        this.spawnedChestsNormal = new HashMap<>();
        this.roomDistribution = new HashMap<>();
        this.roomTypeDistribution = new HashMap<>();
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

    public void addToDungeonCalculationFailures(String failure, int amount) {
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

    public void addToRoomTotal(int amount) {
        this.roomTotal += amount;
    }

    public void addToRoomTypeDistribution(RoomType type, int amount) {
        this.addToRoomTotal(amount);
        this.roomTypeDistribution.put(type, this.roomTypeDistribution.getOrDefault(type, 0) + amount);
    }

    public void addToRoomDistribution(Room r, int amount) {
        this.addToRoomTypeDistribution(r.getType(), amount);
        Map<String, Integer> m = this.roomDistribution.computeIfAbsent(r.getType(), l -> new HashMap<>());
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

    public Map<RoomType, Integer> getRoomTypeDistribution() {
        return this.roomTypeDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Map<String, Integer> getRoomDistribution(RoomType type) {
	    Map<String, Integer> roomDistributionOfType = this.roomDistribution.get(type);
		if (roomDistributionOfType == null) {
			return Map.of();
		}
		return roomDistributionOfType.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    public Map<String, Integer> getRoomWeightMap() {
        return this.roomWeightMap;
    }
}
