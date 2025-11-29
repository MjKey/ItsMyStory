package ru.mjkey.storykee.systems.region;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a 3D region in the world.
 * 
 * Requirements: 49.1
 */
public class Region {
    
    private final String id;
    private final Vec3 min;
    private final Vec3 max;
    private final AABB boundingAABB;
    private final Map<String, Object> customData;
    private int priority; // For overlapping regions
    
    /**
     * Creates a region from two corner positions.
     * Requirement 49.1: Create region definition
     * 
     * @param id Unique region ID
     * @param corner1 First corner
     * @param corner2 Second corner
     */
    public Region(String id, Vec3 corner1, Vec3 corner2) {
        this.id = id;
        this.min = new Vec3(
                Math.min(corner1.x, corner2.x),
                Math.min(corner1.y, corner2.y),
                Math.min(corner1.z, corner2.z)
        );
        this.max = new Vec3(
                Math.max(corner1.x, corner2.x),
                Math.max(corner1.y, corner2.y),
                Math.max(corner1.z, corner2.z)
        );
        this.boundingAABB = new AABB(min, max);
        this.customData = new HashMap<>();
        this.priority = 0;
    }
    
    /**
     * Creates a region from block positions.
     */
    public Region(String id, BlockPos corner1, BlockPos corner2) {
        this(id, Vec3.atLowerCornerOf(corner1), Vec3.atLowerCornerOf(corner2).add(1, 1, 1));
    }
    
    /**
     * Creates a spherical region (approximated as a AABB).
     * 
     * @param id Unique region ID
     * @param center Center position
     * @param radius Radius
     */
    public static Region sphere(String id, Vec3 center, double radius) {
        return new Region(id,
                center.subtract(radius, radius, radius),
                center.add(radius, radius, radius));
    }
    
    public String getId() {
        return id;
    }
    
    public Vec3 getMin() {
        return min;
    }
    
    public Vec3 getMax() {
        return max;
    }
    
    public AABB getBoundingAABB() {
        return boundingAABB;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    /**
     * Gets the center of the region.
     */
    public Vec3 getCenter() {
        return new Vec3(
                (min.x + max.x) / 2,
                (min.y + max.y) / 2,
                (min.z + max.z) / 2
        );
    }
    
    /**
     * Gets the size of the region.
     */
    public Vec3 getSize() {
        return new Vec3(
                max.x - min.x,
                max.y - min.y,
                max.z - min.z
        );
    }
    
    /**
     * Gets the volume of the region.
     */
    public double getVolume() {
        Vec3 size = getSize();
        return size.x * size.y * size.z;
    }
    
    /**
     * Checks if a position is inside the region.
     * Requirement 49.3: Implement containment checking
     * 
     * @param position Position to check
     * @return true if inside
     */
    public boolean contains(Vec3 position) {
        return position.x >= min.x && position.x <= max.x &&
               position.y >= min.y && position.y <= max.y &&
               position.z >= min.z && position.z <= max.z;
    }
    
    /**
     * Checks if a block position is inside the region.
     * 
     * @param pos Block position to check
     * @return true if inside
     */
    public boolean contains(BlockPos pos) {
        return contains(Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5));
    }
    
    /**
     * Checks if this region overlaps with another.
     * Requirement 49.4: Handle overlapping regions
     * 
     * @param other Other region
     * @return true if overlapping
     */
    public boolean overlaps(Region other) {
        return boundingAABB.intersects(other.boundingAABB);
    }
    
    /**
     * Checks if this region fully contains another.
     * 
     * @param other Other region
     * @return true if this region contains the other
     */
    public boolean fullyContains(Region other) {
        return contains(other.min) && contains(other.max);
    }
    
    /**
     * Gets custom data value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) customData.get(key);
    }
    
    /**
     * Sets custom data value.
     */
    public void setData(String key, Object value) {
        customData.put(key, value);
    }
    
    /**
     * Removes custom data value.
     */
    public void removeData(String key) {
        customData.remove(key);
    }
    
    /**
     * Checks if region has custom data.
     */
    public boolean hasData(String key) {
        return customData.containsKey(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region = (Region) o;
        return Objects.equals(id, region.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("Region{id='%s', min=%s, max=%s}", id, min, max);
    }
}
