package ru.mjkey.storykee.systems.camera;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cinematic camera cutscenes for players.
 * Handles camera control, waypoint interpolation, and player input disabling.
 * 
 * Requirements: 19.1, 19.2, 19.3, 19.4, 19.5
 */
public class CinematicCameraManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CinematicCameraManager.class);
    
    private static CinematicCameraManager instance;
    
    // Registered cutscenes by ID
    private final Map<String, Cutscene> cutscenes;
    
    // Active cutscene states per player
    private final Map<UUID, CutsceneState> activeStates;
    
    // Event listeners
    private final List<CutsceneEventListener> eventListeners;
    
    private MinecraftServer server;
    
    private CinematicCameraManager() {
        this.cutscenes = new ConcurrentHashMap<>();
        this.activeStates = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
    }
    
    public static CinematicCameraManager getInstance() {
        if (instance == null) {
            instance = new CinematicCameraManager();
        }
        return instance;
    }
    
    /**
     * Initializes the camera manager with the server instance.
     * 
     * @param server Minecraft server
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("CinematicCameraManager initialized");
    }
    
    /**
     * Shuts down the camera manager.
     */
    public void shutdown() {
        // End all active cutscenes
        for (UUID playerId : new ArrayList<>(activeStates.keySet())) {
            endCutscene(playerId, false);
        }
        cutscenes.clear();
        LOGGER.info("CinematicCameraManager shutdown");
    }
    
    // ==================== Cutscene Registration ====================
    
    /**
     * Registers a cutscene.
     * 
     * @param cutscene Cutscene to register
     * @return true if registered successfully
     */
    public boolean registerCutscene(Cutscene cutscene) {
        if (cutscene == null || cutscene.getId() == null) {
            LOGGER.warn("Cannot register null cutscene or cutscene with null ID");
            return false;
        }
        
        if (cutscenes.containsKey(cutscene.getId())) {
            LOGGER.warn("Cutscene with ID '{}' already exists", cutscene.getId());
            return false;
        }
        
        cutscenes.put(cutscene.getId(), cutscene);
        LOGGER.debug("Registered cutscene: {}", cutscene.getId());
        return true;
    }
    
    /**
     * Unregisters a cutscene.
     * 
     * @param cutsceneId Cutscene ID
     * @return The removed cutscene, or null if not found
     */
    public Cutscene unregisterCutscene(String cutsceneId) {
        return cutscenes.remove(cutsceneId);
    }
    
    /**
     * Gets a cutscene by ID.
     * 
     * @param cutsceneId Cutscene ID
     * @return The cutscene, or null if not found
     */
    public Cutscene getCutscene(String cutsceneId) {
        return cutscenes.get(cutsceneId);
    }
    
    // ==================== Cutscene Playback ====================
    
    /**
     * Starts a cutscene for a player.
     * Requirement 19.1: Takes control of player's camera
     * Requirement 19.3: Disables player movement and input
     * 
     * @param player Player to start cutscene for
     * @param cutsceneId Cutscene ID
     * @return true if cutscene started successfully
     */
    public boolean startCutscene(ServerPlayer player, String cutsceneId) {
        if (player == null || cutsceneId == null) {
            return false;
        }
        
        Cutscene cutscene = cutscenes.get(cutsceneId);
        if (cutscene == null) {
            LOGGER.warn("Cutscene '{}' not found", cutsceneId);
            return false;
        }
        
        return startCutscene(player, cutscene);
    }
    
    /**
     * Starts a cutscene for a player.
     * 
     * @param player Player to start cutscene for
     * @param cutscene Cutscene to play
     * @return true if cutscene started successfully
     */
    public boolean startCutscene(ServerPlayer player, Cutscene cutscene) {
        if (player == null || cutscene == null) {
            return false;
        }
        
        UUID playerId = player.getUUID();
        
        // End any existing cutscene
        if (activeStates.containsKey(playerId)) {
            endCutscene(playerId, false);
        }
        
        // Store original position and rotation
        Vec3 originalPos = player.position();
        float originalPitch = player.getXRot();
        float originalYaw = player.getYRot();
        
        // Create cutscene state
        long currentTick = server != null ? server.getTickCount() : 0;
        CutsceneState state = new CutsceneState(
                playerId, cutscene, originalPos, originalPitch, originalYaw, currentTick);
        
        activeStates.put(playerId, state);
        
        // Notify listeners
        notifyCutsceneStart(playerId, cutscene.getId());
        
        LOGGER.debug("Started cutscene '{}' for player {}", cutscene.getId(), playerId);
        return true;
    }
    
    /**
     * Skips the current cutscene for a player.
     * Requirement 19.5: Jump to end state and restore player control
     * 
     * @param playerId Player UUID
     * @return true if cutscene was skipped
     */
    public boolean skipCutscene(UUID playerId) {
        CutsceneState state = activeStates.get(playerId);
        if (state == null) {
            return false;
        }
        
        if (!state.getCutscene().isSkippable()) {
            LOGGER.debug("Cutscene '{}' is not skippable", state.getCutscene().getId());
            return false;
        }
        
        state.setSkipped(true);
        endCutscene(playerId, true);
        return true;
    }
    
    /**
     * Ends a cutscene for a player.
     * Requirement 19.4: Returns camera control to player
     * 
     * @param playerId Player UUID
     * @param wasSkipped Whether the cutscene was skipped
     */
    public void endCutscene(UUID playerId, boolean wasSkipped) {
        CutsceneState state = activeStates.remove(playerId);
        if (state == null) {
            return;
        }
        
        Cutscene cutscene = state.getCutscene();
        
        // Restore player position and rotation
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                restorePlayerState(player, state);
            }
        }
        
        // Execute callbacks
        if (wasSkipped && cutscene.getOnSkip() != null) {
            try {
                cutscene.getOnSkip().run();
            } catch (Exception e) {
                LOGGER.error("Error in cutscene skip callback: {}", e.getMessage());
            }
        } else if (!wasSkipped && cutscene.getOnComplete() != null) {
            try {
                cutscene.getOnComplete().run();
            } catch (Exception e) {
                LOGGER.error("Error in cutscene complete callback: {}", e.getMessage());
            }
        }
        
        // Notify listeners
        if (wasSkipped) {
            notifyCutsceneSkip(playerId, cutscene.getId());
        } else {
            notifyCutsceneComplete(playerId, cutscene.getId());
        }
        
        LOGGER.debug("Ended cutscene '{}' for player {} (skipped: {})", 
                cutscene.getId(), playerId, wasSkipped);
    }
    
    /**
     * Restores player state after cutscene ends.
     */
    private void restorePlayerState(ServerPlayer player, CutsceneState state) {
        // Teleport player back to original position with original rotation
        player.teleportTo(
                (ServerLevel)player.level(),
                state.getOriginalPosition().x,
                state.getOriginalPosition().y,
                state.getOriginalPosition().z,
                Set.of(),
                state.getOriginalYaw(),
                state.getOriginalPitch(),
                false
        );
    }
    
    // ==================== Tick Update ====================
    
    /**
     * Updates all active cutscenes.
     * Should be called every server tick.
     */
    public void tick() {
        if (server == null) {
            return;
        }
        
        long currentTick = server.getTickCount();
        
        for (Map.Entry<UUID, CutsceneState> entry : new HashMap<>(activeStates).entrySet()) {
            UUID playerId = entry.getKey();
            CutsceneState state = entry.getValue();
            
            state.setCurrentTick(currentTick);
            
            // Get current camera position
            CutsceneState.CameraPosition camPos = state.getCurrentCameraPosition();
            
            if (camPos == null) {
                // Cutscene complete
                state.setCompleted(true);
                endCutscene(playerId, false);
                continue;
            }
            
            // Update player camera position
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                updatePlayerCamera(player, camPos);
            }
        }
    }
    
    /**
     * Updates player camera to the specified position.
     * Requirement 19.2: Smooth interpolation between waypoints
     */
    private void updatePlayerCamera(ServerPlayer player, CutsceneState.CameraPosition camPos) {
        // Teleport player to camera position with rotation
        player.teleportTo(
                (ServerLevel)player.level(),
                camPos.position().x,
                camPos.position().y,
                camPos.position().z,
                Set.of(),
                camPos.yaw(),
                camPos.pitch(),
                false
        );
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Checks if a player is in a cutscene.
     * 
     * @param playerId Player UUID
     * @return true if player is in a cutscene
     */
    public boolean isInCutscene(UUID playerId) {
        return activeStates.containsKey(playerId);
    }
    
    /**
     * Gets the active cutscene state for a player.
     * 
     * @param playerId Player UUID
     * @return CutsceneState, or null if not in cutscene
     */
    public CutsceneState getCutsceneState(UUID playerId) {
        return activeStates.get(playerId);
    }
    
    /**
     * Gets all players currently in cutscenes.
     * 
     * @return Set of player UUIDs
     */
    public Set<UUID> getPlayersInCutscenes() {
        return Collections.unmodifiableSet(activeStates.keySet());
    }
    
    // ==================== Event Listeners ====================
    
    public void addEventListener(CutsceneEventListener listener) {
        eventListeners.add(listener);
    }
    
    public void removeEventListener(CutsceneEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void notifyCutsceneStart(UUID playerId, String cutsceneId) {
        for (CutsceneEventListener listener : eventListeners) {
            try {
                listener.onCutsceneStart(playerId, cutsceneId);
            } catch (Exception e) {
                LOGGER.error("Error in cutscene start listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyCutsceneComplete(UUID playerId, String cutsceneId) {
        for (CutsceneEventListener listener : eventListeners) {
            try {
                listener.onCutsceneComplete(playerId, cutsceneId);
            } catch (Exception e) {
                LOGGER.error("Error in cutscene complete listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyCutsceneSkip(UUID playerId, String cutsceneId) {
        for (CutsceneEventListener listener : eventListeners) {
            try {
                listener.onCutsceneSkip(playerId, cutsceneId);
            } catch (Exception e) {
                LOGGER.error("Error in cutscene skip listener: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Listener interface for cutscene events.
     */
    public interface CutsceneEventListener {
        void onCutsceneStart(UUID playerId, String cutsceneId);
        void onCutsceneComplete(UUID playerId, String cutsceneId);
        void onCutsceneSkip(UUID playerId, String cutsceneId);
    }
    
    /**
     * Adapter class with empty default implementations.
     */
    public static class CutsceneEventAdapter implements CutsceneEventListener {
        @Override
        public void onCutsceneStart(UUID playerId, String cutsceneId) {}
        
        @Override
        public void onCutsceneComplete(UUID playerId, String cutsceneId) {}
        
        @Override
        public void onCutsceneSkip(UUID playerId, String cutsceneId) {}
    }
}
