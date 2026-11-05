package com.playmonumenta.roguelite.objects;

import com.playmonumenta.roguelite.Utils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Hitbox {
	private final BoundingBox mBb;

	public Hitbox(Location p1, Location p2) {
		Vector pos1 = new Vector(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
		Vector pos2 = new Vector(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
		mBb = BoundingBox.of(pos1, pos2);
	}

	public Hitbox(Hitbox old) {
		mBb = old.mBb.clone();
	}

	public Hitbox(Room r) {
		Vector pos1 = new Vector(r.getLocation().getBlockX(), r.getLocation().getBlockY(), r.getLocation().getBlockZ());
		Location tmp = r.getLocation().clone().add(r.getSize());
		Vector pos2 = new Vector(tmp.getBlockX(), tmp.getBlockY(), tmp.getBlockZ());
		mBb = BoundingBox.of(pos1, pos2);
	}

	public boolean collidesWith(Hitbox other) {
		return mBb.contains(other.mBb);
	}

	public boolean contains(Vector vector) {
		return mBb.contains(vector);
	}

	public List<Long> subChunkVectors(Vector dungeonMinCorner) {
		Vector min = mBb.getMin();
		Vector max = mBb.getMax();

		long minX = Utils.subChunkCoordKey(dungeonMinCorner, min, Axis.X);
		long minY = Utils.subChunkCoordKey(dungeonMinCorner, min, Axis.Y);
		long minZ = Utils.subChunkCoordKey(dungeonMinCorner, min, Axis.Z);
		long maxX = Utils.subChunkCoordKey(dungeonMinCorner, max, Axis.X);
		long maxY = Utils.subChunkCoordKey(dungeonMinCorner, max, Axis.Y);
		long maxZ = Utils.subChunkCoordKey(dungeonMinCorner, max, Axis.Z);

		List<Long> result = new ArrayList<>();
		for (long x = minX; x <= maxX; x++) {
			for (long y = minY; y <= maxY; y++) {
				for (long z = minZ; z <= maxZ; z++) {
					result.add(Utils.subChunkKey(x, y, z));
				}
			}
		}
		return result;
	}
}
