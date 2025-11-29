package ru.mjkey.storykee.systems.npc;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.events.EventData;
import ru.mjkey.storykee.events.EventManager;
import ru.mjkey.storykee.runtime.async.MinecraftThreadBridge;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages NPC entities in the story system.
 * Provides creation, removal, lookup, and property update functionality.
 * 
 * Requirements: 6.1, 6.4, 6.5
 */
public class NPCManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NPCManager.class);
    private static NPCManager instance;
    
    // Registry of all NPCs by their ID
    private final Map<String, StoryNPC> npcRegistry;
    
    // Map of NPC IDs to their script IDs (for cleanup)
    private final Map<String, String> npcScriptMap;
    
    // Callbacks for NPC events
    private final Map<String, List<Consumer<StoryNPC>>> spawnCallbacks;
    private final Map<String, List<Consumer<StoryNPC>>> removeCallbacks;
    
    private NPCManager() {
        this.npcRegistry = new ConcurrentHashMap<>();
        this.npcScriptMap = new ConcurrentHashMap<>();
        this.spawnCallbacks = new ConcurrentHashMap<>();
        this.removeCallbacks = new ConcurrentHashMap<>();
    }
    
    public static NPCManager getInstance() {
        if (instance == null) {
            instance = new NPCManager();
        }
        return instance;
    }
    
    /**
     * Creates and spawns a new NPC with the given properties.
     * Requirement 6.1: Spawn entity in specified location.
     * 
     * @param npcId Unique ResourceLocation for the NPC
     * @param properties NPC properties including position, name, skin, etc.
     * @return The created StoryNPC, or null if creation failed
     */
    public StoryNPC createNPC(String npcId, NPCProperties properties) {
        return createNPC(npcId, properties, null);
    }
    
    /**
     * Creates and spawns a new NPC with the given properties.
     * 
     * @param npcId Unique ResourceLocation for the NPC
     * @param properties NPC properties
     * @param scriptId The script that created this NPC (for cleanup)
     * @return The created StoryNPC, or null if creation failed
     */
    public StoryNPC createNPC(String npcId, NPCProperties properties, String scriptId) {
        if (npcId == null || npcId.isEmpty()) {
            LOGGER.error("Cannot create NPC with null or empty ID");
            return null;
        }
        
        if (properties == null) {
            LOGGER.error("Cannot create NPC {} with null properties", npcId);
            return null;
        }
        
        // Check if NPC already exists
        if (npcRegistry.containsKey(npcId)) {
            LOGGER.warn("NPC with ID {} already exists, updating instead", npcId);
            return updateNPC(npcId, properties);
        }
        
        // Get the server level for the specified dimension
        MinecraftServer server = MinecraftThreadBridge.getInstance().getServer();
        if (server == null) {
            LOGGER.error("Cannot create NPC {}: server not available", npcId);
            return null;
        }
        
        ServerLevel level = getServerLevel(server, properties.getDimension());
        if (level == null) {
            LOGGER.error("Cannot create NPC {}: dimension {} not found", npcId, properties.getDimension());
            return null;
        }
        
        // Create the NPC entity on the main thread
        try {
            StoryNPC npc = MinecraftThreadBridge.getInstance().executeOnMainThreadAndWait(() -> {
                StoryNPC entity = new StoryNPC(NPCEntityRegistry.STORY_NPC, level);
                entity.setNpcId(npcId);
                entity.applyProperties(properties);
                
                // Spawn the entity in the world
                level.addFreshEntity(entity);
                
                return entity;
            });
            
            if (npc != null) {
                // Register the NPC
                npcRegistry.put(npcId, npc);
                
                if (scriptId != null) {
                    npcScriptMap.put(npcId, scriptId);
                }
                
                // Fire spawn event
                fireSpawnEvent(npc);
                
                // Execute spawn callbacks
                executeCallbacks(spawnCallbacks.get(npcId), npc);
                
                LOGGER.info("Created NPC {} at ({}, {}, {}) in {}", 
                        npcId, properties.getX(), properties.getY(), properties.getZ(), properties.getDimension());
            }
            
            return npc;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create NPC {}: {}", npcId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Removes an NPC by its ID.
     * Requirement 6.5: Despawn entity and clean up resources.
     * 
     * @param npcId The ID of the NPC to remove
     * @return true if the NPC was removed, false if not found
     */
    public boolean removeNPC(String npcId) {
        if (npcId == null) {
            return false;
        }
        
        StoryNPC npc = npcRegistry.remove(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot remove NPC {}: not found", npcId);
            return false;
        }
        
        // Execute remove callbacks before removal
        executeCallbacks(removeCallbacks.get(npcId), npc);
        
        // Remove from script map
        npcScriptMap.remove(npcId);
        
        // Remove the entity from the world on the main thread
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            if (!npc.isRemoved()) {
                npc.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        
        // Clear callbacks
        spawnCallbacks.remove(npcId);
        removeCallbacks.remove(npcId);
        
        LOGGER.info("Removed NPC {}", npcId);
        return true;
    }
    
    /**
     * Removes all NPCs created by a specific script.
     * Used for cleanup when a script is unloaded.
     * 
     * @param scriptId The script ID
     * @return Number of NPCs removed
     */
    public int removeNPCsByScript(String scriptId) {
        if (scriptId == null) {
            return 0;
        }
        
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, String> entry : npcScriptMap.entrySet()) {
            if (scriptId.equals(entry.getValue())) {
                toRemove.add(entry.getKey());
            }
        }
        
        int count = 0;
        for (String npcId : toRemove) {
            if (removeNPC(npcId)) {
                count++;
            }
        }
        
        LOGGER.info("Removed {} NPCs for script {}", count, scriptId);
        return count;
    }
    
    /**
     * Gets an NPC by its ID.
     * 
     * @param npcId The NPC ID
     * @return The StoryNPC, or null if not found
     */
    public StoryNPC getNPC(String npcId) {
        return npcRegistry.get(npcId);
    }
    
    /**
     * Checks if an NPC exists.
     * 
     * @param npcId The NPC ID
     * @return true if the NPC exists
     */
    public boolean hasNPC(String npcId) {
        return npcRegistry.containsKey(npcId);
    }
    
    /**
     * Gets all registered NPCs.
     * 
     * @return Unmodifiable collection of all NPCs
     */
    public Collection<StoryNPC> getAllNPCs() {
        return Collections.unmodifiableCollection(npcRegistry.values());
    }
    
    /**
     * Gets all NPC IDs.
     * 
     * @return Unmodifiable set of all NPC IDs
     */
    public Set<String> getAllNPCIds() {
        return Collections.unmodifiableSet(npcRegistry.keySet());
    }
    
    /**
     * Gets the number of registered NPCs.
     * 
     * @return NPC count
     */
    public int getNPCCount() {
        return npcRegistry.size();
    }
    
    /**
     * Updates an NPC's properties.
     * Requirement 6.4: Reflect changes immediately in the game world.
     * 
     * @param npcId The NPC ID
     * @param properties The new properties
     * @return The updated NPC, or null if not found
     */
    public StoryNPC updateNPC(String npcId, NPCProperties properties) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot update NPC {}: not found", npcId);
            return null;
        }
        
        // Update properties on the main thread
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.updateProperties(properties);
        });
        
        LOGGER.debug("Updated NPC {} properties", npcId);
        return npc;
    }
    
    /**
     * Moves an NPC to a target position.
     * 
     * @param npcId The NPC ID
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @param speed Movement speed multiplier
     * @return true if movement started, false if NPC not found
     */
    public boolean moveNPC(String npcId, double x, double y, double z, double speed) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot move NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.moveTo(x, y, z, speed);
        });
        
        return true;
    }
    
    /**
     * Sets the skin URL for an NPC.
     * 
     * @param npcId The NPC ID
     * @param skinUrl The skin URL
     * @return true if skin was set, false if NPC not found
     */
    public boolean setNPCSkin(String npcId, String skinUrl) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot set skin for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.setSkinUrl(skinUrl);
        });
        
        LOGGER.debug("Set skin for NPC {} to {}", npcId, skinUrl);
        return true;
    }
    
    /**
     * Registers a callback to be executed when an NPC is spawned.
     * 
     * @param npcId The NPC ID to watch
     * @param callback The callback to execute
     */
    public void onNPCSpawn(String npcId, Consumer<StoryNPC> callback) {
        spawnCallbacks.computeIfAbsent(npcId, k -> new ArrayList<>()).add(callback);
    }
    
    /**
     * Registers a callback to be executed when an NPC is removed.
     * 
     * @param npcId The NPC ID to watch
     * @param callback The callback to execute
     */
    public void onNPCRemove(String npcId, Consumer<StoryNPC> callback) {
        removeCallbacks.computeIfAbsent(npcId, k -> new ArrayList<>()).add(callback);
    }
    
    /**
     * Clears all NPCs and resets the manager.
     */
    public void clearAll() {
        List<String> ids = new ArrayList<>(npcRegistry.keySet());
        for (String npcId : ids) {
            removeNPC(npcId);
        }
        
        spawnCallbacks.clear();
        removeCallbacks.clear();
        
        LOGGER.info("Cleared all NPCs");
    }
    
    /**
     * Gets the ServerLevel for a dimension.
     */
    private ServerLevel getServerLevel(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            String levelDimension = level.dimension().location().toString();
            if (levelDimension.equals(dimension)) {
                return level;
            }
        }
        
        // Default to overworld if dimension not found
        return server.overworld();
    }
    
    /**
     * Fires the NPC spawn event.
     */
    private void fireSpawnEvent(StoryNPC npc) {
        EventData eventData = new EventData("onNPCSpawn");
        eventData.set("npc", npc.getNpcId());
        eventData.set("npcName", npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC");
        eventData.set("x", npc.getX());
        eventData.set("y", npc.getY());
        eventData.set("z", npc.getZ());
        
        EventManager.getInstance().fireEvent("onNPCSpawn", eventData);
    }
    
    /**
     * Executes a list of callbacks.
     */
    private void executeCallbacks(List<Consumer<StoryNPC>> callbacks, StoryNPC npc) {
        if (callbacks == null || callbacks.isEmpty()) {
            return;
        }
        
        for (Consumer<StoryNPC> callback : callbacks) {
            try {
                callback.accept(npc);
            } catch (Exception e) {
                LOGGER.error("Error executing NPC callback: {}", e.getMessage(), e);
            }
        }
    }
    
    // ===== Movement Methods (Requirements: 6.4, Task 34) =====
    
    /**
     * Sets a patrol path for an NPC.
     * 
     * @param npcId The NPC ID
     * @param waypoints List of patrol waypoints
     * @param loop Whether to loop the patrol
     * @param speed Movement speed multiplier
     * @return true if patrol was set, false if NPC not found
     */
    public boolean setPatrolPath(String npcId, java.util.List<NPCMovementController.PatrolWaypoint> waypoints, 
                                  boolean loop, double speed) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot set patrol path for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().setPatrolPath(waypoints, loop, speed);
        });
        
        LOGGER.debug("Set patrol path for NPC {} with {} waypoints", npcId, waypoints.size());
        return true;
    }
    
    /**
     * Stops an NPC's patrol.
     * 
     * @param npcId The NPC ID
     * @return true if patrol was stopped, false if NPC not found
     */
    public boolean stopPatrol(String npcId) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot stop patrol for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().stopPatrol();
        });
        
        return true;
    }
    
    /**
     * Makes an NPC follow a player.
     * 
     * @param npcId The NPC ID
     * @param playerId The UUID of the player to follow
     * @param distance The distance to maintain from the player
     * @param speed Movement speed multiplier
     * @return true if following started, false if NPC not found
     */
    public boolean followPlayer(String npcId, java.util.UUID playerId, double distance, double speed) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot make NPC {} follow player: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().followPlayer(playerId, distance, speed);
        });
        
        LOGGER.debug("NPC {} now following player {} at distance {}", npcId, playerId, distance);
        return true;
    }
    
    /**
     * Stops an NPC from following.
     * 
     * @param npcId The NPC ID
     * @return true if following was stopped, false if NPC not found
     */
    public boolean stopFollowing(String npcId) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot stop following for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().stopFollowing();
        });
        
        return true;
    }
    
    /**
     * Enables an NPC to look at the nearest player.
     * 
     * @param npcId The NPC ID
     * @param range The range to look for players
     * @return true if enabled, false if NPC not found
     */
    public boolean enableLookAtNearestPlayer(String npcId, double range) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot enable look at player for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().enableLookAtNearestPlayer(range);
        });
        
        return true;
    }
    
    /**
     * Starts an NPC wandering randomly within a radius.
     * 
     * @param npcId The NPC ID
     * @param radius The radius to wander within
     * @return true if wandering started, false if NPC not found
     */
    public boolean startWandering(String npcId, double radius) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot start wandering for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().startWandering(radius);
        });
        
        LOGGER.debug("NPC {} started wandering within {} blocks", npcId, radius);
        return true;
    }
    
    /**
     * Stops an NPC from wandering.
     * 
     * @param npcId The NPC ID
     * @return true if wandering was stopped, false if NPC not found
     */
    public boolean stopWandering(String npcId) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot stop wandering for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().stopWandering();
        });
        
        return true;
    }
    
    /**
     * Makes an NPC return to its spawn point.
     * 
     * @param npcId The NPC ID
     * @param speed Movement speed multiplier
     * @return true if returning started, false if NPC not found
     */
    public boolean returnToSpawn(String npcId, double speed) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot return NPC {} to spawn: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().returnToSpawn(speed);
        });
        
        LOGGER.debug("NPC {} returning to spawn point", npcId);
        return true;
    }
    
    /**
     * Stops all movement behaviors for an NPC.
     * 
     * @param npcId The NPC ID
     * @return true if stopped, false if NPC not found
     */
    public boolean stopAllMovement(String npcId) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot stop movement for NPC {}: not found", npcId);
            return false;
        }
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            npc.getMovementController().stopAll();
        });
        
        return true;
    }
    
    // ===== Animation Methods (Requirements: 10.2) =====
    
    /**
     * Broadcasts an NPC animation to all clients.
     * 
     * @param npcId The NPC ID
     * @param animationId The animation to play
     * @param loop Whether to loop the animation
     */
    public void broadcastNPCAnimation(String npcId, String animationId, boolean loop) {
        ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
            .broadcastNPCAnimation(npcId, animationId, loop);
    }
    
    /**
     * Broadcasts an NPC animation stop to all clients.
     * 
     * @param npcId The NPC ID
     */
    public void broadcastNPCAnimationStop(String npcId) {
        ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
            .broadcastNPCAnimationStop(npcId);
    }
    
    /**
     * Plays an animation on an NPC and broadcasts to clients.
     * 
     * @param npcId The NPC ID
     * @param animationId The animation to play
     * @param loop Whether to loop the animation
     * @param callback Optional callback when animation completes
     * @return true if animation started successfully
     */
    public boolean playNPCAnimation(String npcId, String animationId, boolean loop, Runnable callback) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot play animation on NPC {}: not found", npcId);
            return false;
        }
        
        boolean started = npc.playAnimation(animationId, loop, callback);
        
        if (started) {
            broadcastNPCAnimation(npcId, animationId, loop);
        }
        
        return started;
    }
    
    /**
     * Stops an animation on an NPC and broadcasts to clients.
     * 
     * @param npcId The NPC ID
     * @return true if animation was stopped
     */
    public boolean stopNPCAnimation(String npcId) {
        StoryNPC npc = npcRegistry.get(npcId);
        if (npc == null) {
            LOGGER.warn("Cannot stop animation on NPC {}: not found", npcId);
            return false;
        }
        
        if (npc.isAnimating()) {
            npc.stopAnimation();
            broadcastNPCAnimationStop(npcId);
            return true;
        }
        
        return false;
    }
}
