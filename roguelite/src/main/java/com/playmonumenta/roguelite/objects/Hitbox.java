package com.playmonumenta.roguelite.objects;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Hitbox {
	private final Vector mPos1;
	private final Vector mPos2;

	public Hitbox(Location p1, Location p2) {
		this.mPos1 = new Vector(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
		this.mPos2 = new Vector(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
	}

	public Hitbox(Hitbox old) {
		this.mPos1 = old.mPos1.clone();
		this.mPos2 = old.mPos2.clone();
	}

	public Hitbox(Room r) {
		this.mPos1 = new Vector(r.getLocation().getBlockX(), r.getLocation().getBlockY(), r.getLocation().getBlockZ());
		Location tmp = r.getLocation().clone().add(r.getSize());
		this.mPos2 = new Vector(tmp.getBlockX(), tmp.getBlockY(), tmp.getBlockZ());
	}

	public boolean collidesWith(Hitbox other) {
		//test collision between the two hitboxes
		boolean collision = (this.mPos1.getBlockX() + 1 <= other.mPos2.getBlockX() && this.mPos2.getBlockX() - 1 >= other.mPos1.getBlockX()) &&
			(this.mPos1.getBlockY() + 1 <= other.mPos2.getBlockY() && this.mPos2.getBlockY() - 1 >= other.mPos1.getBlockY()) &&
			(this.mPos1.getBlockZ() + 1 <= other.mPos2.getBlockZ() && this.mPos2.getBlockZ() - 1 >= other.mPos1.getBlockZ());
		if (!collision) {
			//test collision with this hitbox and the world limits
			collision = this.mPos1.getBlockY() < 0 || this.mPos2.getBlockY() > 255;
		}
		return collision;
	}

	public Hitbox incToPostPlacing() {
		Vector v = new Vector(2, 0, 2);
		this.mPos1.subtract(v);
		this.mPos2.add(v);
		return this;
	}
}
