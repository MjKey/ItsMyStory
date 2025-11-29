package ru.mjkey.storykee.systems.animation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controls playback of a single animation on a target entity.
 * Handles frame interpolation, looping, callbacks, and playback state.
 * 
 * Requirements: 10.2, 10.3, 10.4
 */
public class AnimationPlayer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationPlayer.class);
    
    /**
     * Animation playback state.
     */
    public enum PlaybackState {
        STOPPED,
        PLAYING,
        PAUSED,
        COMPLETED
    }
    
    private final Animation animation;
    private final String targetId;
    
    private PlaybackState state;
    private float currentTime;
    private float playbackSpeed;
    private int loopCount;
    private int maxLoops; // -1 for infinite (uses animation.isLoop())
    
    // Callbacks
    private Runnable onComplete;
    private Runnable onLoop;
    private final List<Runnable> completionListeners;
    
    // Frame caching
    private AnimationFrame currentFrame;
    private AnimationFrame previousFrame;
    private boolean frameDirty;
    
    /**
     * Creates an animation player.
     * 
     * @param animation Animation to play
     * @param targetId Target entity ID
     */
    public AnimationPlayer(Animation animation, String targetId) {
        this.animation = animation;
        this.targetId = targetId;
        this.state = PlaybackState.STOPPED;
        this.currentTime = 0;
        this.playbackSpeed = 1.0f;
        this.loopCount = 0;
        this.maxLoops = animation.isLoop() ? -1 : 0;
        this.completionListeners = new ArrayList<>();
        this.frameDirty = true;
    }
    
    /**
     * Gets the animation being played.
     * 
     * @return The animation
     */
    public Animation getAnimation() {
        return animation;
    }
    
    /**
     * Gets the target entity ID.
     * 
     * @return Target ID
     */
    public String getTargetId() {
        return targetId;
    }
    
    /**
     * Gets the current playback state.
     * 
     * @return Playback state
     */
    public PlaybackState getState() {
        return state;
    }
    
    /**
     * Checks if the animation is currently playing.
     * 
     * @return true if playing
     */
    public boolean isPlaying() {
        return state == PlaybackState.PLAYING;
    }
    
    /**
     * Checks if the animation is paused.
     * 
     * @return true if paused
     */
    public boolean isPaused() {
        return state == PlaybackState.PAUSED;
    }
    
    /**
     * Checks if the animation has completed.
     * 
     * @return true if completed
     */
    public boolean isCompleted() {
        return state == PlaybackState.COMPLETED;
    }
    
    /**
     * Gets the current playback time in seconds.
     * 
     * @return Current time
     */
    public float getCurrentTime() {
        return currentTime;
    }
    
    /**
     * Gets the playback progress (0.0 to 1.0).
     * 
     * @return Progress percentage
     */
    public float getProgress() {
        if (animation.getDuration() <= 0) {
            return 1.0f;
        }
        return Math.min(1.0f, currentTime / animation.getDuration());
    }
    
    /**
     * Gets the playback speed multiplier.
     * 
     * @return Speed multiplier
     */
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }
    
    /**
     * Sets the playback speed multiplier.
     * 
     * @param speed Speed multiplier (1.0 = normal, 2.0 = double speed)
     * @return this for chaining
     */
    public AnimationPlayer setPlaybackSpeed(float speed) {
        this.playbackSpeed = Math.max(0.01f, speed);
        return this;
    }
    
    /**
     * Gets the number of times the animation has looped.
     * 
     * @return Loop count
     */
    public int getLoopCount() {
        return loopCount;
    }
    
    /**
     * Sets the maximum number of loops.
     * 
     * @param maxLoops Maximum loops (-1 for infinite, 0 for no looping)
     * @return this for chaining
     */
    public AnimationPlayer setMaxLoops(int maxLoops) {
        this.maxLoops = maxLoops;
        return this;
    }
    
    /**
     * Sets the completion callback.
     * 
     * @param onComplete Callback to run when animation completes
     * @return this for chaining
     */
    public AnimationPlayer setOnComplete(Runnable onComplete) {
        this.onComplete = onComplete;
        return this;
    }
    
    /**
     * Sets the loop callback.
     * 
     * @param onLoop Callback to run each time animation loops
     * @return this for chaining
     */
    public AnimationPlayer setOnLoop(Runnable onLoop) {
        this.onLoop = onLoop;
        return this;
    }
    
    /**
     * Adds a completion listener (internal use).
     * 
     * @param listener Listener to add
     */
    void addCompletionListener(Runnable listener) {
        completionListeners.add(listener);
    }
    
    // ==================== Playback Control ====================
    
    /**
     * Starts or resumes playback.
     */
    public void play() {
        if (state == PlaybackState.COMPLETED) {
            // Restart from beginning
            currentTime = 0;
            loopCount = 0;
        }
        state = PlaybackState.PLAYING;
        frameDirty = true;
        LOGGER.debug("Playing animation '{}' on '{}'", animation.getId(), targetId);
    }
    
    /**
     * Pauses playback.
     */
    public void pause() {
        if (state == PlaybackState.PLAYING) {
            state = PlaybackState.PAUSED;
            LOGGER.debug("Paused animation '{}' on '{}'", animation.getId(), targetId);
        }
    }
    
    /**
     * Resumes playback from pause.
     */
    public void resume() {
        if (state == PlaybackState.PAUSED) {
            state = PlaybackState.PLAYING;
            LOGGER.debug("Resumed animation '{}' on '{}'", animation.getId(), targetId);
        }
    }
    
    /**
     * Stops playback and resets to beginning.
     */
    public void stop() {
        state = PlaybackState.STOPPED;
        currentTime = 0;
        loopCount = 0;
        frameDirty = true;
        LOGGER.debug("Stopped animation '{}' on '{}'", animation.getId(), targetId);
    }
    
    /**
     * Seeks to a specific time.
     * 
     * @param time Time in seconds
     */
    public void seekTo(float time) {
        this.currentTime = Math.max(0, Math.min(time, animation.getDuration()));
        frameDirty = true;
    }
    
    /**
     * Seeks to a specific progress percentage.
     * 
     * @param progress Progress (0.0 to 1.0)
     */
    public void seekToProgress(float progress) {
        seekTo(progress * animation.getDuration());
    }
    
    // ==================== Update ====================
    
    /**
     * Updates the animation player.
     * Should be called every frame/tick.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        if (state != PlaybackState.PLAYING) {
            return;
        }
        
        previousFrame = currentFrame;
        currentTime += deltaTime * playbackSpeed;
        frameDirty = true;
        
        // Check for animation end
        if (currentTime >= animation.getDuration()) {
            handleAnimationEnd();
        }
    }
    
    /**
     * Handles animation reaching its end.
     */
    private void handleAnimationEnd() {
        // Check if we should loop
        boolean shouldLoop = maxLoops < 0 || loopCount < maxLoops;
        
        if (shouldLoop && animation.isLoop()) {
            // Loop the animation
            currentTime = currentTime % animation.getDuration();
            loopCount++;
            
            if (onLoop != null) {
                try {
                    onLoop.run();
                } catch (Exception e) {
                    LOGGER.error("Error in animation loop callback: {}", e.getMessage());
                }
            }
            
            LOGGER.debug("Animation '{}' looped (count: {})", animation.getId(), loopCount);
        } else {
            // Animation completed
            currentTime = animation.getDuration();
            state = PlaybackState.COMPLETED;
            
            // Fire completion callback
            if (onComplete != null) {
                try {
                    onComplete.run();
                } catch (Exception e) {
                    LOGGER.error("Error in animation completion callback: {}", e.getMessage());
                }
            }
            
            // Fire internal completion listeners
            for (Runnable listener : completionListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    LOGGER.error("Error in animation completion listener: {}", e.getMessage());
                }
            }
            
            LOGGER.debug("Animation '{}' completed on '{}'", animation.getId(), targetId);
        }
    }
    
    // ==================== Frame Access ====================
    
    /**
     * Gets the current interpolated frame.
     * 
     * @return Current animation frame
     */
    public AnimationFrame getCurrentFrame() {
        if (frameDirty || currentFrame == null) {
            currentFrame = animation.getFrameAtTime(currentTime);
            frameDirty = false;
        }
        return currentFrame;
    }
    
    /**
     * Gets the previous frame (before last update).
     * 
     * @return Previous frame, or null if not available
     */
    public AnimationFrame getPreviousFrame() {
        return previousFrame;
    }
    
    /**
     * Gets the transform for a specific bone at current time.
     * 
     * @param boneName Name of the bone
     * @return Transform for the bone
     */
    public Transform getBoneTransform(String boneName) {
        return getCurrentFrame().getBoneTransform(boneName);
    }
    
    /**
     * Gets an interpolated transform between previous and current frame.
     * Useful for smooth rendering between updates.
     * 
     * @param boneName Name of the bone
     * @param interpolation Interpolation factor (0.0 = previous, 1.0 = current)
     * @return Interpolated transform
     */
    public Transform getInterpolatedBoneTransform(String boneName, float interpolation) {
        if (previousFrame == null) {
            return getBoneTransform(boneName);
        }
        
        Transform prev = previousFrame.getBoneTransform(boneName);
        Transform curr = getCurrentFrame().getBoneTransform(boneName);
        return prev.lerp(curr, interpolation);
    }
    
    @Override
    public String toString() {
        return String.format("AnimationPlayer[anim=%s, target=%s, state=%s, time=%.2f/%.2f, loops=%d]",
                animation.getId(), targetId, state, currentTime, animation.getDuration(), loopCount);
    }
}
