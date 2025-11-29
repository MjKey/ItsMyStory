package ru.mjkey.storykee.systems.lighting;

import net.minecraft.core.BlockPos;

/**
 * Represents a dynamic light source that can be moved and updated.
 * 
 * Requirements: 53.5
 */
public class DynamicLight {
    
    private final String id;
    private BlockPos position;
    private final String dimension;
    private int lightLevel;
    private boolean enabled;
    
    public DynamicLight(String id, BlockPos position, String dimension, int lightLevel) {
        this.id = id;
        this.position = position;
        this.dimension = dimension;
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
        this.enabled = true;
    }
    
    public String getId() {
        return id;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setPosition(BlockPos position) {
        this.position = position;
    }
    
    public String getDimension() {
        return dimension;
    }
    
    public int getLightLevel() {
        return lightLevel;
    }
    
    public void setLightLevel(int lightLevel) {
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        return "DynamicLight{id='" + id + "', position=" + position + ", level=" + lightLevel + "}";
    }
}
