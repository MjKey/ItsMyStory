package ru.mjkey.storykee.systems.teleport;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * Represents a saved location for teleportation.
 * 
 * Requirements: 43.2
 */
public class SavedLocation {
    
    private final String id;
    private final Vec3 position;
    private final float yaw;
    private final float pitch;
    private final ResourceLocation dimension;
    
    public SavedLocation(String id, Vec3 position, float yaw, float pitch, ResourceLocation dimension) {
        this.id = id;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.dimension = dimension;
    }
    
    public SavedLocation(String id, Vec3 position, float yaw, float pitch, ResourceKey<Level> worldKey) {
        this(id, position, yaw, pitch, worldKey.location());
    }
    
    public String getId() {
        return id;
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public double getX() {
        return position.x;
    }
    
    public double getY() {
        return position.y;
    }
    
    public double getZ() {
        return position.z;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public ResourceLocation getDimension() {
        return dimension;
    }
    
    /**
     * Creates a copy with a new ID.
     */
    public SavedLocation withId(String newId) {
        return new SavedLocation(newId, position, yaw, pitch, dimension);
    }
    
    /**
     * Creates a copy with a new position.
     */
    public SavedLocation withPosition(Vec3 newPosition) {
        return new SavedLocation(id, newPosition, yaw, pitch, dimension);
    }
    
    /**
     * Creates a copy with new rotation.
     */
    public SavedLocation withRotation(float newYaw, float newPitch) {
        return new SavedLocation(id, position, newYaw, newPitch, dimension);
    }
    
    @Override
    public String toString() {
        return String.format("SavedLocation{id='%s', pos=(%.2f, %.2f, %.2f), yaw=%.2f, pitch=%.2f, dim=%s}",
                id, position.x, position.y, position.z, yaw, pitch, dimension);
    }
}
