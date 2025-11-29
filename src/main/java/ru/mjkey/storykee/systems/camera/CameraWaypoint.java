package ru.mjkey.storykee.systems.camera;

import net.minecraft.world.phys.Vec3;

/**
 * Represents a waypoint in a cinematic camera path.
 * Contains position, rotation, and timing information.
 * 
 * Requirements: 19.2
 */
public class CameraWaypoint {
    
    private final Vec3 position;
    private final float pitch;
    private final float yaw;
    private final long durationTicks;
    private final InterpolationType interpolation;
    
    /**
     * Creates a new camera waypoint.
     * 
     * @param position World position
     * @param pitch Camera pitch (vertical rotation)
     * @param yaw Camera yaw (horizontal rotation)
     * @param durationTicks Time to reach this waypoint from previous
     * @param interpolation Interpolation type for smooth movement
     */
    public CameraWaypoint(Vec3 position, float pitch, float yaw, long durationTicks, InterpolationType interpolation) {
        this.position = position;
        this.pitch = pitch;
        this.yaw = yaw;
        this.durationTicks = durationTicks;
        this.interpolation = interpolation;
    }
    
    /**
     * Creates a waypoint with linear interpolation.
     */
    public CameraWaypoint(Vec3 position, float pitch, float yaw, long durationTicks) {
        this(position, pitch, yaw, durationTicks, InterpolationType.LINEAR);
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public long getDurationTicks() {
        return durationTicks;
    }
    
    public InterpolationType getInterpolation() {
        return interpolation;
    }
    
    /**
     * Interpolation types for camera movement.
     */
    public enum InterpolationType {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        EASE_IN_OUT,
        SMOOTH
    }
    
    /**
     * Applies interpolation to a progress value (0.0 to 1.0).
     * 
     * @param progress Raw progress value
     * @return Interpolated progress value
     */
    public float applyInterpolation(float progress) {
        return switch (interpolation) {
            case LINEAR -> progress;
            case EASE_IN -> progress * progress;
            case EASE_OUT -> 1 - (1 - progress) * (1 - progress);
            case EASE_IN_OUT -> {
                if (progress < 0.5f) {
                    yield 2 * progress * progress;
                } else {
                    yield 1 - (float) Math.pow(-2 * progress + 2, 2) / 2;
                }
            }
            case SMOOTH -> {
                // Smoothstep interpolation
                float t = progress * progress * (3 - 2 * progress);
                yield t;
            }
        };
    }
}
