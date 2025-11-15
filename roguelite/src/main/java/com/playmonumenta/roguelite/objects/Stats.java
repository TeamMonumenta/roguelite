package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.enums.Biome;
import com.playmonumenta.roguelite.enums.RoomType;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Stats {

	private final long mStartTime;
	private int mTargetDungeonCount;
	private int mDungeonCount;
	private int mSuccessfulDungeonCount;
	private int mUnsuccessfulDungeonCount;
	private final Map<String, Integer> mDungeonCalculationFailures;
	private int mUnusedChestsTotal;
	private int mSpawnedChestsTotal;
	private int mSpawnedChestsObjectiveTotal;
	private int mSpawnedChestsNormalTotal;
	private final Map<Biome, Integer> mSpawnedChestsObjective;
	private final Map<Biome, Integer> mSpawnedChestsNormal;
	private int mRoomTotal;
	private final Map<RoomType, Integer> mRoomTypeDistribution;
	private final Map<RoomType, Map<String, Integer>> mRoomDistribution;
	private final Map<String, Integer> mRoomWeightMap;

	public Stats() {
		this.mDungeonCalculationFailures = new HashMap<>();
		this.mSpawnedChestsObjective = new HashMap<>();
		this.mSpawnedChestsNormal = new HashMap<>();
		this.mRoomDistribution = new HashMap<>();
		this.mRoomTypeDistribution = new HashMap<>();
		this.mRoomWeightMap = new HashMap<>();
		this.mStartTime = System.currentTimeMillis();
	}

	public void addToTargetDungeonCount(int amount) {
		this.mTargetDungeonCount += amount;
	}

	public void addToDungeonCount(int amount) {
		this.mDungeonCount += amount;
	}

	public void addToSuccessfulDungeonCount(int amount) {
		this.mSuccessfulDungeonCount += amount;
		this.addToDungeonCount(amount);
	}

	public void addToUnsuccessfulDungeonCount(int amount) {
		this.addToDungeonCount(amount);
		this.mUnsuccessfulDungeonCount += amount;
	}

	public void addToDungeonCalculationFailures(String failure, int amount) {
		this.mDungeonCalculationFailures.put(failure, amount +
			this.mDungeonCalculationFailures.getOrDefault(failure, 0));
	}

	public void addToUnusedChestsTotal(int amount) {
		this.mUnusedChestsTotal += amount;
	}

	public void addToSpawnedChestsTotal(int amount) {
		this.mSpawnedChestsTotal += amount;
	}

	public void addToSpawnedChestsObjectiveTotal(int amount) {
		this.addToSpawnedChestsTotal(amount);
		this.mSpawnedChestsObjectiveTotal += amount;
	}

	public void addToSpawnedChestsNormalTotal(int amount) {
		this.addToSpawnedChestsTotal(amount);
		this.mSpawnedChestsNormalTotal += amount;
	}

	public void addToSpawnedChests(Objective o, int amount) {
		this.addToSpawnedChestsObjectiveTotal(amount);
		this.mSpawnedChestsObjective.put(o.getBiome(), amount +
			this.mSpawnedChestsObjective.getOrDefault(o.getBiome(), 0));
	}

	public void addToSpawnedChests(LootChest c, int amount) {
		this.addToSpawnedChestsNormalTotal(amount);
		this.mSpawnedChestsNormal.put(c.getBiome(), amount +
			this.mSpawnedChestsNormal.getOrDefault(c.getBiome(), 0));
	}

	public void addToRoomTotal(int amount) {
		this.mRoomTotal += amount;
	}

	public void addToRoomTypeDistribution(RoomType type, int amount) {
		this.addToRoomTotal(amount);
		this.mRoomTypeDistribution.put(type, this.mRoomTypeDistribution.getOrDefault(type, 0) + amount);
	}

	public void addToRoomDistribution(Room r, int amount) {
		this.addToRoomTypeDistribution(r.getType(), amount);
		Map<String, Integer> m = this.mRoomDistribution.computeIfAbsent(r.getType(), l -> new HashMap<>());
		String id = r.getPath().substring(r.getPath().lastIndexOf("/"));
		this.addToRoomWeightMap(id, r.getWeight());
		m.put(id, m.getOrDefault(id, 0) + amount);
	}

	public void addToRoomWeightMap(String id, int value) {
		this.mRoomWeightMap.putIfAbsent(id, value);
	}

	public long getStartTime() {
		return this.mStartTime;
	}

	public int getDungeonCount() {
		return this.mDungeonCount;
	}

	public int getTargetDungeonCount() {
		return this.mTargetDungeonCount;
	}

	public int getSuccessfulDungeonCount() {
		return this.mSuccessfulDungeonCount;
	}

	public int getUnsuccessfulDungeonCount() {
		return this.mUnsuccessfulDungeonCount;
	}

	public int getUnusedChestsTotal() {
		return this.mUnusedChestsTotal;
	}

	public int getSpawnedChestsTotal() {
		return this.mSpawnedChestsTotal;
	}

	public int getSpawnedChestsNormalTotal() {
		return this.mSpawnedChestsNormalTotal;
	}

	public int getSpawnedChestsObjectiveTotal() {
		return this.mSpawnedChestsObjectiveTotal;
	}

	public Map<Biome, Integer> getSpawnedChestsNormal() {
		return this.mSpawnedChestsNormal.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	public Map<Biome, Integer> getSpawnedChestsObjective() {
		return this.mSpawnedChestsObjective.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	public Map<String, Integer> getDungeonCalculationFailures() {
		return this.mDungeonCalculationFailures.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	public int getRoomTotal() {
		return mRoomTotal;
	}

	public Map<RoomType, Integer> getRoomTypeDistribution() {
		return this.mRoomTypeDistribution.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	public Map<String, Integer> getRoomDistribution(RoomType type) {
		Map<String, Integer> roomDistributionOfType = this.mRoomDistribution.get(type);
		if (roomDistributionOfType == null) {
			return Map.of();
		}
		return roomDistributionOfType.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
				(oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	public Map<String, Integer> getRoomWeightMap() {
		return this.mRoomWeightMap;
	}
}
