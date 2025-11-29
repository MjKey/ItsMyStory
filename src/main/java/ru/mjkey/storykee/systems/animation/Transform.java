package ru.mjkey.storykee.systems.animation;

import org.joml.Vector3f;

/**
 * Represents a 3D transformation with position, rotation, and scale.
 * Used for animation keyframes to define bone/entity transformations.
 * 
 * Requirements: 10.1
 */
public class Transform {
    
    private final Vector3f position;
    private final Vector3f rotation;
    private final Vector3f scale;
    
    /**
     * Creates a default identity transform.
     */
    public Transform() {
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
    }
    
    /**
     * Creates a transform with specified values.
     * 
     * @param position Position offset (x, y, z)
     * @param rotation Rotation in degrees (pitch, yaw, roll)
     * @param scale Scale factors (x, y, z)
     */
    public Transform(Vector3f position, Vector3f rotation, Vector3f scale) {
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(rotation);
        this.scale = new Vector3f(scale);
    }
    
    /**
     * Creates a transform with position only.
     * 
     * @param x X position
     * @param y Y position
     * @param z Z position
     */
    public Transform(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.rotation = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
    }
    
    /**
     * Copy constructor.
     * 
     * @param other Transform to copy
     */
    public Transform(Transform other) {
        this.position = new Vector3f(other.position);
        this.rotation = new Vector3f(other.rotation);
        this.scale = new Vector3f(other.scale);
    }
    
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public Vector3f getRotation() {
        return new Vector3f(rotation);
    }
    
    public Vector3f getScale() {
        return new Vector3f(scale);
    }
    
    public float getX() {
        return position.x;
    }
    
    public float getY() {
        return position.y;
    }
    
    public float getZ() {
        return position.z;
    }
    
    public float getPitch() {
        return rotation.x;
    }
    
    public float getYaw() {
        return rotation.y;
    }
    
    public float getRoll() {
        return rotation.z;
    }
    
    public float getScaleX() {
        return scale.x;
    }
    
    public float getScaleY() {
        return scale.y;
    }
    
    public float getScaleZ() {
        return scale.z;
    }
    
    /**
     * Sets the position.
     * 
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @return this for chaining
     */
    public Transform setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        return this;
    }
    
    /**
     * Sets the rotation.
     * 
     * @param pitch Pitch in degrees
     * @param yaw Yaw in degrees
     * @param roll Roll in degrees
     * @return this for chaining
     */
    public Transform setRotation(float pitch, float yaw, float roll) {
        this.rotation.set(pitch, yaw, roll);
        return this;
    }
    
    /**
     * Sets the scale.
     * 
     * @param x X scale
     * @param y Y scale
     * @param z Z scale
     * @return this for chaining
     */
    public Transform setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        return this;
    }
    
    /**
     * Sets uniform scale.
     * 
     * @param scale Scale factor for all axes
     * @return this for chaining
     */
    public Transform setScale(float scale) {
        this.scale.set(scale, scale, scale);
        return this;
    }
    
    /**
     * Linearly interpolates between this transform and another.
     * 
     * @param other Target transform
     * @param t Interpolation factor (0.0 to 1.0)
     * @return New interpolated transform
     */
    public Transform lerp(Transform other, float t) {
        t = Math.max(0, Math.min(1, t));
        
        Vector3f newPos = new Vector3f(position).lerp(other.position, t);
        Vector3f newRot = new Vector3f(rotation).lerp(other.rotation, t);
        Vector3f newScale = new Vector3f(scale).lerp(other.scale, t);
        
        return new Transform(newPos, newRot, newScale);
    }
    
    /**
     * Creates an identity transform (no transformation).
     * 
     * @return Identity transform
     */
    public static Transform identity() {
        return new Transform();
    }
    
    /**
     * Creates a transform from position only.
     * 
     * @param x X position
     * @param y Y position
     * @param z Z position
     * @return New transform
     */
    public static Transform fromPosition(float x, float y, float z) {
        return new Transform(x, y, z);
    }
    
    /**
     * Creates a transform from rotation only.
     * 
     * @param pitch Pitch in degrees
     * @param yaw Yaw in degrees
     * @param roll Roll in degrees
     * @return New transform
     */
    public static Transform fromRotation(float pitch, float yaw, float roll) {
        Transform t = new Transform();
        t.setRotation(pitch, yaw, roll);
        return t;
    }
    
    @Override
    public String toString() {
        return String.format("Transform[pos=(%.2f, %.2f, %.2f), rot=(%.2f, %.2f, %.2f), scale=(%.2f, %.2f, %.2f)]",
                position.x, position.y, position.z,
                rotation.x, rotation.y, rotation.z,
                scale.x, scale.y, scale.z);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transform other = (Transform) obj;
        return position.equals(other.position) && 
               rotation.equals(other.rotation) && 
               scale.equals(other.scale);
    }
    
    @Override
    public int hashCode() {
        int result = position.hashCode();
        result = 31 * result + rotation.hashCode();
        result = 31 * result + scale.hashCode();
        return result;
    }
}
