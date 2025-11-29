package ru.mjkey.storykee.systems.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents an animation consisting of multiple frames.
 * Animations can be played on entities to create visual effects.
 * 
 * Requirements: 10.1
 */
public class Animation {
    
    private final String id;
    private final List<AnimationFrame> frames;
    private float duration; // Total duration in seconds
    private boolean loop;
    private float frameRate; // Frames per second
    
    /**
     * Creates a new animation.
     * 
     * @param id Unique ResourceLocation for the animation
     */
    public Animation(String id) {
        this.id = id;
        this.frames = new ArrayList<>();
        this.duration = 0;
        this.loop = false;
        this.frameRate = 20.0f; // Default to Minecraft tick rate
    }
    
    /**
     * Creates a new animation with specified properties.
     * 
     * @param id Unique ResourceLocation
     * @param duration Duration in seconds
     * @param loop Whether to loop
     */
    public Animation(String id, float duration, boolean loop) {
        this.id = id;
        this.frames = new ArrayList<>();
        this.duration = duration;
        this.loop = loop;
        this.frameRate = 20.0f;
    }
    
    /**
     * Gets the animation ID.
     * 
     * @return Animation ResourceLocation
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets all frames in the animation.
     * 
     * @return Unmodifiable list of frames
     */
    public List<AnimationFrame> getFrames() {
        return Collections.unmodifiableList(frames);
    }
    
    /**
     * Gets the total duration in seconds.
     * 
     * @return Duration in seconds
     */
    public float getDuration() {
        return duration;
    }
    
    /**
     * Sets the total duration.
     * 
     * @param duration Duration in seconds
     * @return this for chaining
     */
    public Animation setDuration(float duration) {
        this.duration = Math.max(0, duration);
        return this;
    }
    
    /**
     * Gets whether the animation loops.
     * 
     * @return true if looping
     */
    public boolean isLoop() {
        return loop;
    }
    
    /**
     * Sets whether the animation loops.
     * 
     * @param loop true to enable looping
     * @return this for chaining
     */
    public Animation setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }
    
    /**
     * Gets the frame rate.
     * 
     * @return Frames per second
     */
    public float getFrameRate() {
        return frameRate;
    }
    
    /**
     * Sets the frame rate.
     * 
     * @param frameRate Frames per second
     * @return this for chaining
     */
    public Animation setFrameRate(float frameRate) {
        this.frameRate = Math.max(1, frameRate);
        return this;
    }
    
    /**
     * Gets the number of frames.
     * 
     * @return Frame count
     */
    public int getFrameCount() {
        return frames.size();
    }
    
    /**
     * Adds a frame to the animation.
     * Frames are automatically sorted by timestamp.
     * 
     * @param frame Frame to add
     * @return this for chaining
     */
    public Animation addFrame(AnimationFrame frame) {
        frames.add(frame);
        frames.sort(Comparator.comparingDouble(AnimationFrame::getTimestamp));
        
        // Update duration if this frame extends beyond current duration
        if (frame.getTimestamp() > duration) {
            duration = frame.getTimestamp();
        }
        
        return this;
    }
    
    /**
     * Removes a frame by index.
     * 
     * @param index Frame index
     * @return The removed frame
     */
    public AnimationFrame removeFrame(int index) {
        return frames.remove(index);
    }
    
    /**
     * Gets a frame by index.
     * 
     * @param index Frame index
     * @return The frame at the index
     */
    public AnimationFrame getFrame(int index) {
        return frames.get(index);
    }
    
    /**
     * Gets the frame at a specific time, with interpolation.
     * 
     * @param time Time in seconds from animation start
     * @return Interpolated frame at the given time
     */
    public AnimationFrame getFrameAtTime(float time) {
        if (frames.isEmpty()) {
            return new AnimationFrame(0, 0);
        }
        
        // Handle looping
        if (loop && duration > 0) {
            time = time % duration;
        } else {
            time = Math.max(0, Math.min(time, duration));
        }
        
        // Find surrounding frames
        AnimationFrame prevFrame = null;
        AnimationFrame nextFrame = null;
        
        for (int i = 0; i < frames.size(); i++) {
            AnimationFrame frame = frames.get(i);
            if (frame.getTimestamp() <= time) {
                prevFrame = frame;
            }
            if (frame.getTimestamp() >= time && nextFrame == null) {
                nextFrame = frame;
            }
        }
        
        // Handle edge cases
        if (prevFrame == null) {
            return frames.get(0).copy();
        }
        if (nextFrame == null || prevFrame == nextFrame) {
            return prevFrame.copy();
        }
        
        // Interpolate between frames
        float frameDuration = nextFrame.getTimestamp() - prevFrame.getTimestamp();
        if (frameDuration <= 0) {
            return prevFrame.copy();
        }
        
        float t = (time - prevFrame.getTimestamp()) / frameDuration;
        return prevFrame.lerp(nextFrame, t);
    }
    
    /**
     * Gets the duration in ticks (assuming 20 TPS).
     * 
     * @return Duration in game ticks
     */
    public int getDurationTicks() {
        return (int) (duration * 20);
    }
    
    /**
     * Sets the duration from ticks.
     * 
     * @param ticks Duration in game ticks
     * @return this for chaining
     */
    public Animation setDurationTicks(int ticks) {
        this.duration = ticks / 20.0f;
        return this;
    }
    
    /**
     * Clears all frames from the animation.
     */
    public void clearFrames() {
        frames.clear();
    }
    
    /**
     * Creates a copy of this animation.
     * 
     * @return New animation with same data
     */
    public Animation copy() {
        Animation copy = new Animation(id, duration, loop);
        copy.frameRate = this.frameRate;
        for (AnimationFrame frame : frames) {
            copy.frames.add(frame.copy());
        }
        return copy;
    }
    
    /**
     * Creates a simple animation with start and end transforms.
     * 
     * @param id Animation ID
     * @param boneName Bone to animate
     * @param startTransform Starting transform
     * @param endTransform Ending transform
     * @param duration Duration in seconds
     * @param loop Whether to loop
     * @return New animation
     */
    public static Animation createSimple(String id, String boneName, 
            Transform startTransform, Transform endTransform, 
            float duration, boolean loop) {
        Animation anim = new Animation(id, duration, loop);
        
        AnimationFrame startFrame = new AnimationFrame(0, 0);
        startFrame.setBoneTransform(boneName, startTransform);
        anim.addFrame(startFrame);
        
        AnimationFrame endFrame = new AnimationFrame(1, duration);
        endFrame.setBoneTransform(boneName, endTransform);
        anim.addFrame(endFrame);
        
        return anim;
    }
    
    @Override
    public String toString() {
        return String.format("Animation[id=%s, frames=%d, duration=%.2fs, loop=%b]",
                id, frames.size(), duration, loop);
    }
}
