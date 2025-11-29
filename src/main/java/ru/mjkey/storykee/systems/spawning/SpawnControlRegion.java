package ru.mjkey.storykee.systems.spawning;

import net.minecraft.core.BlockPos;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a region where spawn control is applied.
 * 
 * Requirements: 52.1, 52.4
 */
public class SpawnControlRegion {
    
    private final String id;
    private final BlockPos min;
    private final BlockPos max;
    private final String dimension;
    private boolean spawningAllowed;
    private Set<String> disabledTypes;
    
    public SpawnControlRegion(String id, BlockPos min, BlockPos max, String dimension, boolean spawningAllowed) {
        this.id = id;
        this.min = new BlockPos(
            Math.min(min.getX(), max.getX()),
            Math.min(min.getY(), max.getY()),
            Math.min(min.getZ(), max.getZ())
        );
        this.max = new BlockPos(
            Math.max(min.getX(), max.getX()),
            Math.max(min.getY(), max.getY()),
            Math.max(min.getZ(), max.getZ())
        );
        this.dimension = dimension;
        this.spawningAllowed = spawningAllowed;
        this.disabledTypes = new HashSet<>();
    }
    
    public String getId() {
        return id;
    }
    
    public BlockPos getMin() {
        return min;
    }
    
    public BlockPos getMax() {
        return max;
    }
    
    public String getDimension() {
        return dimension;
    }
    
    public boolean isSpawningAllowed() {
        return spawningAllowed;
    }
    
    public void setSpawningAllowed(boolean allowed) {
        this.spawningAllowed = allowed;
    }
    
    public Set<String> getDisabledTypes() {
        return Collections.unmodifiableSet(disabledTypes);
    }
    
    public void setDisabledTypes(Set<String> types) {
        this.disabledTypes = types != null ? new HashSet<>(types) : new HashSet<>();
    }
    
    public void addDisabledType(String entityType) {
        disabledTypes.add(normalizeType(entityType));
    }
    
    public void removeDisabledType(String entityType) {
        disabledTypes.remove(normalizeType(entityType));
    }
    
    public boolean isTypeDisabled(String entityType) {
        return disabledTypes.contains(normalizeType(entityType));
    }
    
    /**
     * Checks if a position is within this region.
     * Requirement 52.4: WHEN spawning is controlled THEN the Runtime SHALL respect region boundaries
     */
    public boolean contains(BlockPos pos, String dim) {
        if (!dimension.equals(dim)) {
            return false;
        }
        
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
               pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
               pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }
    
    public boolean contains(double x, double y, double z, String dim) {
        return contains(new BlockPos((int) x, (int) y, (int) z), dim);
    }
    
    private String normalizeType(String entityType) {
        if (entityType == null) return "";
        if (!entityType.contains(":")) {
            return "minecraft:" + entityType.toLowerCase();
        }
        return entityType.toLowerCase();
    }
    
    @Override
    public String toString() {
        return "SpawnControlRegion{id='" + id + "', min=" + min + ", max=" + max + ", dimension='" + dimension + "'}";
    }
}
