package ru.mjkey.storykee.systems.camera;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Tracks the state of an active cutscene for a player.
 * 
 * Requirements: 19.1, 19.3, 19.4
 */
public class CutsceneState {
    
    private final UUID playerId;
    private final Cutscene cutscene;
    private final Vec3 originalPosition;
    private final float originalPitch;
    private final float originalYaw;
    private final long startTick;
    
    private long currentTick;
    private boolean completed;
    private boolean skipped;
    
    public CutsceneState(UUID playerId, Cutscene cutscene, Vec3 originalPosition, 
                         float originalPitch, float originalYaw, long startTick) {
        this.playerId = playerId;
        this.cutscene = cutscene;
        this.originalPosition = originalPosition;
        this.originalPitch = originalPitch;
        this.originalYaw = originalYaw;
        this.startTick = startTick;
        this.currentTick = startTick;
        this.completed = false;
        this.skipped = false;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Cutscene getCutscene() {
        return cutscene;
    }
    
    public Vec3 getOriginalPosition() {
        return originalPosition;
    }
    
    public float getOriginalPitch() {
        return originalPitch;
    }
    
    public float getOriginalYaw() {
        return originalYaw;
    }
    
    public long getStartTick() {
        return startTick;
    }
    
    public long getCurrentTick() {
        return currentTick;
    }
    
    public void setCurrentTick(long currentTick) {
        this.currentTick = currentTick;
    }
    
    public long getElapsedTicks() {
        return currentTick - startTick;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public boolean isSkipped() {
        return skipped;
    }
    
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }
    
    /**
     * Gets the current interpolated camera position and rotation.
     * 
     * @return CameraPosition with interpolated values, or null if cutscene is complete
     */
    public CameraPosition getCurrentCameraPosition() {
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(getElapsedTicks());
        
        if (progress == null) {
            return null;
        }
        
        CameraWaypoint from = progress.from();
        CameraWaypoint to = progress.to();
        float t = to.applyInterpolation(progress.progress());
        
        // Interpolate position
        Vec3 pos = from.getPosition().lerp(to.getPosition(), t);
        
        // Interpolate rotation
        float pitch = lerp(from.getPitch(), to.getPitch(), t);
        float yaw = lerpAngle(from.getYaw(), to.getYaw(), t);
        
        return new CameraPosition(pos, pitch, yaw);
    }
    
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
    
    private float lerpAngle(float a, float b, float t) {
        // Handle angle wrapping for smooth rotation
        float diff = b - a;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return a + diff * t;
    }
    
    /**
     * Represents an interpolated camera position.
     */
    public record CameraPosition(Vec3 position, float pitch, float yaw) {
    }
}
