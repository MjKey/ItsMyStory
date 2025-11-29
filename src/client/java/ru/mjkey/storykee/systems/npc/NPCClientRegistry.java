package ru.mjkey.storykee.systems.npc;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry for NPC rendering components.
 * Registers entity renderers and model layers for both wide (Steve)
 * and slim (Alex) arm variants.
 * Also tracks client-side animation state for NPCs.
 * 
 * Requirement 6.2: Custom skin rendering and model support
 * Requirement 10.2: Animation playback support
 */
public class NPCClientRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NPCClientRegistry.class);
    
    private static NPCClientRegistry instance;
    
    // Client-side animation state tracking
    private final Map<String, NPCAnimationState> animationStates;
    
    private NPCClientRegistry() {
        this.animationStates = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance.
     */
    public static NPCClientRegistry getInstance() {
        if (instance == null) {
            instance = new NPCClientRegistry();
        }
        return instance;
    }
    
    /**
     * Registers all client-side NPC components.
     * Should be called during client mod initialization.
     */
    @SuppressWarnings("deprecation")
    public static void register() {
        LOGGER.info("Registering NPC client components...");
        
        // Ensure instance is created
        getInstance();
        
        // Register standard model layer (Steve-style with wide arms)
        EntityModelLayerRegistry.registerModelLayer(
                NPCModelLayers.STORY_NPC, 
                StoryNPCModel::createBodyLayer
        );
        
        // Register slim model layer (Alex-style with slim arms)
        EntityModelLayerRegistry.registerModelLayer(
                NPCModelLayers.STORY_NPC_SLIM, 
                StoryNPCModel::createSlimBodyLayer
        );
        
        // Register entity renderer
        // Note: EntityRendererRegistry is deprecated but still functional in Fabric 1.21+
        EntityRendererRegistry.register(
                NPCEntityRegistry.STORY_NPC, 
                StoryNPCRenderer::new
        );
        
        LOGGER.info("NPC client components registered successfully");
    }
    
    /**
     * Clears the texture cache used by the NPC renderer.
     * Useful when reloading resources.
     */
    public static void clearTextureCache() {
        StoryNPCRenderer.clearTextureCache();
    }
    
    // ===== Animation State Management (Requirements: 10.2) =====
    
    /**
     * Sets the animation state for an NPC.
     * Called when receiving animation packets from the server.
     * 
     * @param npcId The NPC ID
     * @param animationId The animation to play
     * @param loop Whether to loop the animation
     */
    public void setNPCAnimation(String npcId, String animationId, boolean loop) {
        NPCAnimationState state = new NPCAnimationState(animationId, loop, System.currentTimeMillis());
        animationStates.put(npcId, state);
        LOGGER.debug("Set client animation for NPC {}: {} (loop={})", npcId, animationId, loop);
    }
    
    /**
     * Stops the animation for an NPC.
     * 
     * @param npcId The NPC ID
     */
    public void stopNPCAnimation(String npcId) {
        animationStates.remove(npcId);
        LOGGER.debug("Stopped client animation for NPC {}", npcId);
    }
    
    /**
     * Gets the animation state for an NPC.
     * 
     * @param npcId The NPC ID
     * @return The animation state, or null if not animating
     */
    public NPCAnimationState getNPCAnimationState(String npcId) {
        return animationStates.get(npcId);
    }
    
    /**
     * Checks if an NPC has an active animation.
     * 
     * @param npcId The NPC ID
     * @return true if the NPC is animating
     */
    public boolean isNPCAnimating(String npcId) {
        return animationStates.containsKey(npcId);
    }
    
    /**
     * Clears all animation states.
     */
    public void clearAnimationStates() {
        animationStates.clear();
    }
    
    /**
     * Represents the animation state for an NPC on the client.
     */
    public static class NPCAnimationState {
        public final String animationId;
        public final boolean loop;
        public final long startTime;
        
        public NPCAnimationState(String animationId, boolean loop, long startTime) {
            this.animationId = animationId;
            this.loop = loop;
            this.startTime = startTime;
        }
        
        /**
         * Gets the elapsed time since the animation started in seconds.
         */
        public float getElapsedTime() {
            return (System.currentTimeMillis() - startTime) / 1000.0f;
        }
    }
}
