package ru.mjkey.storykee.systems.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a combat area that restricts boss movement.
 * Supports both spherical and AABB-shaped areas.
 * 
 * Requirements: 18.6 - Prevent boss from leaving designated combat area
 */
public class CombatArea {
    
    public enum Shape {
        SPHERE,
        AABB
    }
    
    private final Vec3 center;
    private final Shape shape;
    
    // For sphere shape
    private final double radius;
    
    // For AABB shape
    private final AABB boundingAABB;
    
    /**
     * Creates a spherical combat area.
     */
    public CombatArea(Vec3 center, double radius) {
        this.center = center;
        this.radius = radius;
        this.shape = Shape.SPHERE;
        this.boundingAABB = null;
    }
    
    /**
     * Creates a spherical combat area.
     */
    public CombatArea(double centerX, double centerY, double centerZ, double radius) {
        this(new Vec3(centerX, centerY, centerZ), radius);
    }
    
    /**
     * Creates a AABB-shaped combat area.
     */
    public CombatArea(AABB boundingAABB) {
        this.boundingAABB = boundingAABB;
        this.center = boundingAABB.getCenter();
        this.radius = 0;
        this.shape = Shape.AABB;
    }
    
    /**
     * Creates a AABB-shaped combat area from two corners.
     */
    public CombatArea(Vec3 corner1, Vec3 corner2) {
        this(new AABB(corner1, corner2));
    }
    
    /**
     * Creates a AABB-shaped combat area from block positions.
     */
    public CombatArea(BlockPos corner1, BlockPos corner2) {
        this(AABB.encapsulatingFullBlocks(corner1, corner2));
    }
    
    /**
     * Checks if a position is within the combat area.
     */
    public boolean contains(Vec3 position) {
        if (shape == Shape.SPHERE) {
            return position.distanceTo(center) <= radius;
        } else {
            return boundingAABB.contains(position);
        }
    }
    
    /**
     * Checks if an entity is within the combat area.
     */
    public boolean contains(Entity entity) {
        return contains(entity.position());
    }
    
    /**
     * Gets the distance from a position to the edge of the combat area.
     * Positive values mean inside, negative values mean outside.
     */
    public double getDistanceToEdge(Vec3 position) {
        if (shape == Shape.SPHERE) {
            return radius - position.distanceTo(center);
        } else {
            // For AABB, calculate distance to nearest edge
            double dx = Math.max(boundingAABB.minX - position.x, position.x - boundingAABB.maxX);
            double dy = Math.max(boundingAABB.minY - position.y, position.y - boundingAABB.maxY);
            double dz = Math.max(boundingAABB.minZ - position.z, position.z - boundingAABB.maxZ);
            
            dx = Math.max(0, dx);
            dy = Math.max(0, dy);
            dz = Math.max(0, dz);
            
            if (dx == 0 && dy == 0 && dz == 0) {
                // Inside the AABB, return distance to nearest wall
                double minDist = Math.min(
                    Math.min(position.x - boundingAABB.minX, boundingAABB.maxX - position.x),
                    Math.min(
                        Math.min(position.y - boundingAABB.minY, boundingAABB.maxY - position.y),
                        Math.min(position.z - boundingAABB.minZ, boundingAABB.maxZ - position.z)
                    )
                );
                return minDist;
            } else {
                // Outside the AABB
                return -Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
        }
    }
    
    /**
     * Gets the nearest point inside the combat area to the given position.
     */
    public Vec3 getNearestPointInside(Vec3 position) {
        if (contains(position)) {
            return position;
        }
        
        if (shape == Shape.SPHERE) {
            Vec3 direction = position.subtract(center).normalize();
            return center.add(direction.scale(radius * 0.95)); // Slightly inside
        } else {
            double x = Math.max(boundingAABB.minX, Math.min(boundingAABB.maxX, position.x));
            double y = Math.max(boundingAABB.minY, Math.min(boundingAABB.maxY, position.y));
            double z = Math.max(boundingAABB.minZ, Math.min(boundingAABB.maxZ, position.z));
            return new Vec3(x, y, z);
        }
    }
    
    /**
     * Gets the direction vector pointing from a position towards the center.
     */
    public Vec3 getDirectionToCenter(Vec3 position) {
        return center.subtract(position).normalize();
    }
    
    /**
     * Calculates the pull force to keep an entity inside the area.
     * Returns a velocity vector to add to the entity.
     */
    public Vec3 calculatePullForce(Vec3 position, double maxForce) {
        double distanceToEdge = getDistanceToEdge(position);
        
        if (distanceToEdge >= 0) {
            // Inside the area, no force needed
            return Vec3.ZERO;
        }
        
        // Outside the area, calculate pull force
        Vec3 direction = getDirectionToCenter(position);
        double forceMagnitude = Math.min(maxForce, Math.abs(distanceToEdge) * 0.1);
        
        return direction.scale(forceMagnitude);
    }
    
    // Getters
    
    public Vec3 getCenter() {
        return center;
    }
    
    public Shape getShape() {
        return shape;
    }
    
    public double getRadius() {
        return radius;
    }
    
    public AABB getBoundingAABB() {
        return boundingAABB;
    }
    
    /**
     * Gets the effective bounding AABB for this area.
     * For spheres, returns a AABB that contains the sphere.
     */
    public AABB getEffectiveBoundingAABB() {
        if (shape == Shape.AABB) {
            return boundingAABB;
        } else {
            return new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
            );
        }
    }
    
    @Override
    public String toString() {
        if (shape == Shape.SPHERE) {
            return "CombatArea{sphere, center=" + center + ", radius=" + radius + "}";
        } else {
            return "CombatArea{AABB, bounds=" + boundingAABB + "}";
        }
    }
}
