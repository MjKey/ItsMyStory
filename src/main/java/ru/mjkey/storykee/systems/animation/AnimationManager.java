package ru.mjkey.storykee.systems.animation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages animations in the Storykee system.
 * Handles animation registration, playback control, and lifecycle management.
 * 
 * Requirements: 10.1, 10.2
 */
public class AnimationManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationManager.class);
    
    private static AnimationManager instance;
    
    // Registered animations by ID
    private final Map<String, Animation> animations;
    
    // Active animation players by target entity ID
    private final Map<String, AnimationPlayer> activePlayers;
    
    // Animation queue per entity (for sequential playback)
    private final Map<String, Queue<QueuedAnimation>> animationQueues;
    
    // Listeners for animation events
    private final List<AnimationEventListener> eventListeners;
    
    private AnimationManager() {
        this.animations = new ConcurrentHashMap<>();
        this.activePlayers = new ConcurrentHashMap<>();
        this.animationQueues = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
    }
    
    /**
     * Gets the singleton instance of AnimationManager.
     * 
     * @return The AnimationManager instance
     */
    public static AnimationManager getInstance() {
        if (instance == null) {
            instance = new AnimationManager();
        }
        return instance;
    }
    
    // ==================== Animation Registration ====================
    
    /**
     * Registers an animation.
     * 
     * @param animation Animation to register
     * @return true if registered successfully, false if ID already exists
     */
    public boolean registerAnimation(Animation animation) {
        if (animation == null || animation.getId() == null) {
            LOGGER.warn("Cannot register null animation or animation with null ID");
            return false;
        }
        
        if (animations.containsKey(animation.getId())) {
            LOGGER.warn("Animation with ID '{}' already exists", animation.getId());
            return false;
        }
        
        animations.put(animation.getId(), animation);
        LOGGER.debug("Registered animation: {}", animation.getId());
        return true;
    }
    
    /**
     * Unregisters an animation by ID.
     * 
     * @param animationId Animation ID to unregister
     * @return The removed animation, or null if not found
     */
    public Animation unregisterAnimation(String animationId) {
        Animation removed = animations.remove(animationId);
        if (removed != null) {
            LOGGER.debug("Unregistered animation: {}", animationId);
        }
        return removed;
    }
    
    /**
     * Gets an animation by ID.
     * 
     * @param animationId Animation ID
     * @return The animation, or null if not found
     */
    public Animation getAnimation(String animationId) {
        return animations.get(animationId);
    }
    
    /**
     * Checks if an animation with the given ID exists.
     * 
     * @param animationId Animation ID
     * @return true if the animation exists
     */
    public boolean hasAnimation(String animationId) {
        return animations.containsKey(animationId);
    }
    
    /**
     * Gets all registered animation IDs.
     * 
     * @return Set of animation IDs
     */
    public Set<String> getAnimationIds() {
        return Collections.unmodifiableSet(animations.keySet());
    }
    
    /**
     * Gets all registered animations.
     * 
     * @return Unmodifiable collection of animations
     */
    public Collection<Animation> getAllAnimations() {
        return Collections.unmodifiableCollection(animations.values());
    }
    
    /**
     * Loads animations from a directory.
     * 
     * @param directory Directory containing animation JSON files
     * @return Number of animations loaded
     */
    public int loadAnimationsFromDirectory(Path directory) {
        Map<String, Animation> loaded = AnimationLoader.loadFromDirectory(directory);
        int count = 0;
        for (Animation anim : loaded.values()) {
            if (registerAnimation(anim)) {
                count++;
            }
        }
        LOGGER.info("Loaded {} animations from {}", count, directory);
        return count;
    }
    
    // ==================== Playback Control ====================
    
    /**
     * Plays an animation on a target entity.
     * 
     * @param animationId Animation ID to play
     * @param targetId Target entity ID
     * @return AnimationPlayer for controlling playback, or null if animation not found
     */
    public AnimationPlayer playAnimation(String animationId, String targetId) {
        return playAnimation(animationId, targetId, null);
    }
    
    /**
     * Plays an animation on a target entity with a callback.
     * 
     * @param animationId Animation ID to play
     * @param targetId Target entity ID
     * @param onComplete Callback when animation completes
     * @return AnimationPlayer for controlling playback, or null if animation not found
     */
    public AnimationPlayer playAnimation(String animationId, String targetId, Runnable onComplete) {
        Animation animation = animations.get(animationId);
        if (animation == null) {
            LOGGER.warn("Animation '{}' not found", animationId);
            return null;
        }
        
        // Stop any currently playing animation on this target
        stopAnimation(targetId);
        
        AnimationPlayer player = new AnimationPlayer(animation, targetId);
        if (onComplete != null) {
            player.setOnComplete(onComplete);
        }
        
        // Add internal completion handler for queue processing
        player.addCompletionListener(() -> {
            activePlayers.remove(targetId);
            notifyAnimationComplete(targetId, animationId);
            processQueue(targetId);
        });
        
        activePlayers.put(targetId, player);
        player.play();
        
        notifyAnimationStart(targetId, animationId);
        LOGGER.debug("Playing animation '{}' on target '{}'", animationId, targetId);
        
        return player;
    }
    
    /**
     * Queues an animation to play after current animations complete.
     * 
     * @param animationId Animation ID to queue
     * @param targetId Target entity ID
     * @param onComplete Callback when this animation completes
     */
    public void queueAnimation(String animationId, String targetId, Runnable onComplete) {
        if (!animations.containsKey(animationId)) {
            LOGGER.warn("Cannot queue unknown animation: {}", animationId);
            return;
        }
        
        Queue<QueuedAnimation> queue = animationQueues.computeIfAbsent(
                targetId, k -> new LinkedList<>());
        queue.add(new QueuedAnimation(animationId, onComplete));
        
        LOGGER.debug("Queued animation '{}' for target '{}'", animationId, targetId);
        
        // If no animation is currently playing, start the queue
        if (!activePlayers.containsKey(targetId)) {
            processQueue(targetId);
        }
    }
    
    /**
     * Processes the animation queue for a target.
     * 
     * @param targetId Target entity ID
     */
    private void processQueue(String targetId) {
        Queue<QueuedAnimation> queue = animationQueues.get(targetId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        
        QueuedAnimation next = queue.poll();
        playAnimation(next.animationId, targetId, next.onComplete);
    }
    
    /**
     * Stops the animation playing on a target.
     * 
     * @param targetId Target entity ID
     * @return true if an animation was stopped
     */
    public boolean stopAnimation(String targetId) {
        AnimationPlayer player = activePlayers.remove(targetId);
        if (player != null) {
            player.stop();
            notifyAnimationInterrupt(targetId, player.getAnimation().getId());
            LOGGER.debug("Stopped animation on target '{}'", targetId);
            return true;
        }
        return false;
    }
    
    /**
     * Stops all animations and clears queues for a target.
     * 
     * @param targetId Target entity ID
     */
    public void stopAllAnimations(String targetId) {
        stopAnimation(targetId);
        animationQueues.remove(targetId);
    }
    
    /**
     * Pauses the animation on a target.
     * 
     * @param targetId Target entity ID
     * @return true if animation was paused
     */
    public boolean pauseAnimation(String targetId) {
        AnimationPlayer player = activePlayers.get(targetId);
        if (player != null) {
            player.pause();
            return true;
        }
        return false;
    }
    
    /**
     * Resumes a paused animation on a target.
     * 
     * @param targetId Target entity ID
     * @return true if animation was resumed
     */
    public boolean resumeAnimation(String targetId) {
        AnimationPlayer player = activePlayers.get(targetId);
        if (player != null) {
            player.resume();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the animation player for a target.
     * 
     * @param targetId Target entity ID
     * @return AnimationPlayer, or null if no animation is playing
     */
    public AnimationPlayer getPlayer(String targetId) {
        return activePlayers.get(targetId);
    }
    
    /**
     * Checks if an animation is playing on a target.
     * 
     * @param targetId Target entity ID
     * @return true if an animation is playing
     */
    public boolean isPlaying(String targetId) {
        AnimationPlayer player = activePlayers.get(targetId);
        return player != null && player.isPlaying();
    }
    
    /**
     * Gets the current animation frame for a target.
     * 
     * @param targetId Target entity ID
     * @return Current frame, or null if no animation is playing
     */
    public AnimationFrame getCurrentFrame(String targetId) {
        AnimationPlayer player = activePlayers.get(targetId);
        return player != null ? player.getCurrentFrame() : null;
    }
    
    // ==================== Update ====================
    
    /**
     * Updates all active animation players.
     * Should be called every tick.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        for (AnimationPlayer player : activePlayers.values()) {
            player.update(deltaTime);
        }
    }
    
    /**
     * Updates all active animation players using tick delta.
     * 
     * @param tickDelta Partial tick (0.0 to 1.0)
     */
    public void updateTick(float tickDelta) {
        // Convert tick delta to seconds (assuming 20 TPS)
        float deltaTime = tickDelta / 20.0f;
        update(deltaTime);
    }
    
    // ==================== Event Listeners ====================
    
    /**
     * Adds an animation event listener.
     * 
     * @param listener Listener to add
     */
    public void addEventListener(AnimationEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Removes an animation event listener.
     * 
     * @param listener Listener to remove
     */
    public void removeEventListener(AnimationEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void notifyAnimationStart(String targetId, String animationId) {
        for (AnimationEventListener listener : eventListeners) {
            try {
                listener.onAnimationStart(targetId, animationId);
            } catch (Exception e) {
                LOGGER.error("Error in animation start listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyAnimationComplete(String targetId, String animationId) {
        for (AnimationEventListener listener : eventListeners) {
            try {
                listener.onAnimationComplete(targetId, animationId);
            } catch (Exception e) {
                LOGGER.error("Error in animation complete listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyAnimationInterrupt(String targetId, String animationId) {
        for (AnimationEventListener listener : eventListeners) {
            try {
                listener.onAnimationInterrupt(targetId, animationId);
            } catch (Exception e) {
                LOGGER.error("Error in animation interrupt listener: {}", e.getMessage());
            }
        }
    }
    
    // ==================== Cleanup ====================
    
    /**
     * Clears all animations and stops all playback.
     */
    public void clear() {
        for (AnimationPlayer player : activePlayers.values()) {
            player.stop();
        }
        activePlayers.clear();
        animationQueues.clear();
        animations.clear();
        LOGGER.debug("Cleared all animations");
    }
    
    /**
     * Gets the number of registered animations.
     * 
     * @return Animation count
     */
    public int getAnimationCount() {
        return animations.size();
    }
    
    /**
     * Gets the number of active animation players.
     * 
     * @return Active player count
     */
    public int getActivePlayerCount() {
        return activePlayers.size();
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents a queued animation.
     */
    private static class QueuedAnimation {
        final String animationId;
        final Runnable onComplete;
        
        QueuedAnimation(String animationId, Runnable onComplete) {
            this.animationId = animationId;
            this.onComplete = onComplete;
        }
    }
    
    /**
     * Listener interface for animation events.
     */
    public interface AnimationEventListener {
        /**
         * Called when an animation starts playing.
         * 
         * @param targetId Target entity ID
         * @param animationId Animation ID
         */
        void onAnimationStart(String targetId, String animationId);
        
        /**
         * Called when an animation completes normally.
         * 
         * @param targetId Target entity ID
         * @param animationId Animation ID
         */
        void onAnimationComplete(String targetId, String animationId);
        
        /**
         * Called when an animation is interrupted.
         * 
         * @param targetId Target entity ID
         * @param animationId Animation ID
         */
        void onAnimationInterrupt(String targetId, String animationId);
    }
    
    /**
     * Adapter class for AnimationEventListener with empty default implementations.
     */
    public static class AnimationEventAdapter implements AnimationEventListener {
        @Override
        public void onAnimationStart(String targetId, String animationId) {}
        
        @Override
        public void onAnimationComplete(String targetId, String animationId) {}
        
        @Override
        public void onAnimationInterrupt(String targetId, String animationId) {}
    }
}
