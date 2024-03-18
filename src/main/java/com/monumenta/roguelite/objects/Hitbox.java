package com.monumenta.roguelite.objects;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class Hitbox {
    private Vector pos1;
    private Vector pos2;

    public Hitbox(Location p1, Location p2) {
        this.pos1 = new Vector(p1.getBlockX(), p1.getBlockY(), p1.getBlockZ());
        this.pos2 = new Vector(p2.getBlockX(), p2.getBlockY(), p2.getBlockZ());
    }

    public Hitbox(Room r) {
        this.pos1 = new Vector(r.getLocation().getBlockX(), r.getLocation().getBlockY(), r.getLocation().getBlockZ());
        Location tmp = r.getLocation().clone().add(r.getSize());
        this.pos2 = new Vector(tmp.getBlockX(), tmp.getBlockY(), tmp.getBlockZ());
    }

    public boolean collidesWith(Hitbox other) {
        //test collision between the two hitboxes
        boolean collision = (this.pos1.getBlockX() + 1 <= other.pos2.getBlockX() && this.pos2.getBlockX() - 1 >= other.pos1.getBlockX()) &&
                (this.pos1.getBlockY() + 1 <= other.pos2.getBlockY() && this.pos2.getBlockY() - 1 >= other.pos1.getBlockY()) &&
                (this.pos1.getBlockZ() + 1 <= other.pos2.getBlockZ() && this.pos2.getBlockZ() - 1 >= other.pos1.getBlockZ());
        if (!collision) {
            //test collision with this hitbox and the world limits
            collision = this.pos1.getBlockY() < 0 || this.pos2.getBlockY() > 255;
        }
        //test collision with instance limits
        return collision;
    }

    public Hitbox incremToPostPlacing() {
        Vector v = new Vector(2, 0, 2);
        this.pos1.subtract(v);
        this.pos2.add(v);
        return this;
    }
}
