package ru.mjkey.storykee.systems.animation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single frame in an animation.
 * Contains transformations for different bones/parts at a specific point in time.
 * 
 * Requirements: 10.1
 */
public class AnimationFrame {
    
    private final int frameNumber;
    private final float timestamp; // Time in seconds from animation start
    private final Map<String, Transform> boneTransforms;
    
    /**
     * Creates an animation frame.
     * 
     * @param frameNumber The frame index in the animation
     * @param timestamp Time in seconds when this frame occurs
     */
    public AnimationFrame(int frameNumber, float timestamp) {
        this.frameNumber = frameNumber;
        this.timestamp = timestamp;
        this.boneTransforms = new HashMap<>();
    }
    
    /**
     * Creates an animation frame with bone transforms.
     * 
     * @param frameNumber The frame index
     * @param timestamp Time in seconds
     * @param boneTransforms Map of bone names to transforms
     */
    public AnimationFrame(int frameNumber, float timestamp, Map<String, Transform> boneTransforms) {
        this.frameNumber = frameNumber;
        this.timestamp = timestamp;
        this.boneTransforms = new HashMap<>(boneTransforms);
    }
    
    /**
     * Gets the frame number.
     * 
     * @return Frame index
     */
    public int getFrameNumber() {
        return frameNumber;
    }
    
    /**
     * Gets the timestamp in seconds.
     * 
     * @return Time in seconds from animation start
     */
    public float getTimestamp() {
        return timestamp;
    }
    
    /**
     * Gets all bone transforms.
     * 
     * @return Unmodifiable map of bone transforms
     */
    public Map<String, Transform> getBoneTransforms() {
        return Collections.unmodifiableMap(boneTransforms);
    }
    
    /**
     * Gets the transform for a specific bone.
     * 
     * @param boneName Name of the bone
     * @return Transform for the bone, or identity transform if not found
     */
    public Transform getBoneTransform(String boneName) {
        return boneTransforms.getOrDefault(boneName, Transform.identity());
    }
    
    /**
     * Checks if this frame has a transform for the specified bone.
     * 
     * @param boneName Name of the bone
     * @return true if transform exists
     */
    public boolean hasBoneTransform(String boneName) {
        return boneTransforms.containsKey(boneName);
    }
    
    /**
     * Sets the transform for a bone.
     * 
     * @param boneName Name of the bone
     * @param transform Transform to apply
     * @return this for chaining
     */
    public AnimationFrame setBoneTransform(String boneName, Transform transform) {
        boneTransforms.put(boneName, new Transform(transform));
        return this;
    }
    
    /**
     * Removes the transform for a bone.
     * 
     * @param boneName Name of the bone
     * @return The removed transform, or null if not found
     */
    public Transform removeBoneTransform(String boneName) {
        return boneTransforms.remove(boneName);
    }
    
    /**
     * Gets the number of bone transforms in this frame.
     * 
     * @return Number of bones with transforms
     */
    public int getBoneCount() {
        return boneTransforms.size();
    }
    
    /**
     * Interpolates between this frame and another frame.
     * 
     * @param other Target frame
     * @param t Interpolation factor (0.0 to 1.0)
     * @return New interpolated frame
     */
    public AnimationFrame lerp(AnimationFrame other, float t) {
        t = Math.max(0, Math.min(1, t));
        
        float newTimestamp = timestamp + (other.timestamp - timestamp) * t;
        int newFrameNumber = (int) (frameNumber + (other.frameNumber - frameNumber) * t);
        
        Map<String, Transform> newTransforms = new HashMap<>();
        
        // Interpolate transforms that exist in both frames
        for (String boneName : boneTransforms.keySet()) {
            Transform thisTransform = boneTransforms.get(boneName);
            Transform otherTransform = other.boneTransforms.getOrDefault(boneName, thisTransform);
            newTransforms.put(boneName, thisTransform.lerp(otherTransform, t));
        }
        
        // Add transforms that only exist in the other frame
        for (String boneName : other.boneTransforms.keySet()) {
            if (!boneTransforms.containsKey(boneName)) {
                Transform otherTransform = other.boneTransforms.get(boneName);
                Transform identity = Transform.identity();
                newTransforms.put(boneName, identity.lerp(otherTransform, t));
            }
        }
        
        return new AnimationFrame(newFrameNumber, newTimestamp, newTransforms);
    }
    
    /**
     * Creates a copy of this frame.
     * 
     * @return New frame with same data
     */
    public AnimationFrame copy() {
        Map<String, Transform> copiedTransforms = new HashMap<>();
        for (Map.Entry<String, Transform> entry : boneTransforms.entrySet()) {
            copiedTransforms.put(entry.getKey(), new Transform(entry.getValue()));
        }
        return new AnimationFrame(frameNumber, timestamp, copiedTransforms);
    }
    
    @Override
    public String toString() {
        return String.format("AnimationFrame[frame=%d, time=%.3fs, bones=%d]", 
                frameNumber, timestamp, boneTransforms.size());
    }
}
