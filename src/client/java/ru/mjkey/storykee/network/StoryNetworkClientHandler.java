package ru.mjkey.storykee.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Client-side handler for story network packets.
 * Receives events and state updates from the server.
 * 
 * Requirements: 31.1, 31.3, 31.4
 */
public class StoryNetworkClientHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryNetworkClientHandler.class);
    
    private static StoryNetworkClientHandler instance;
    
    // Local cache of story states
    private final Map<String, Map<String, String>> storyStates;
    
    // Event listeners
    private final List<StoryEventListener> eventListeners;
    private final List<StateUpdateListener> stateListeners;
    
    private StoryNetworkClientHandler() {
        this.storyStates = new ConcurrentHashMap<>();
        this.eventListeners = new CopyOnWriteArrayList<>();
        this.stateListeners = new CopyOnWriteArrayList<>();
    }
    
    public static StoryNetworkClientHandler getInstance() {
        if (instance == null) {
            instance = new StoryNetworkClientHandler();
        }
        return instance;
    }
    
    /**
     * Registers client-side packet receivers.
     */
    public static void register() {
        LOGGER.info("Registering story network client handlers");
        
        StoryNetworkClientHandler handler = getInstance();
        
        // Register receiver for story events
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.StoryEventPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handler.handleStoryEvent(payload));
            }
        );
        
        // Register receiver for state sync
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.StoryStateSyncPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handler.handleStateSync(payload));
            }
        );
        
        // Register receiver for full state sync
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.StoryFullStatePayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handler.handleFullStateSync(payload));
            }
        );
        
        // Register receiver for NPC animation (Requirements: 10.2)
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.NPCAnimationPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handler.handleNPCAnimation(payload));
            }
        );
        
        // Register receiver for NPC animation stop (Requirements: 10.2)
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.NPCAnimationStopPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handler.handleNPCAnimationStop(payload));
            }
        );
    }

    // ===== Event Handling =====
    
    /**
     * Handles a story event received from the server.
     */
    private void handleStoryEvent(StoryNetworkManager.StoryEventPayload payload) {
        LOGGER.debug("Received story event: type={}, id={}", payload.eventType(), payload.eventId());
        
        // Notify all listeners
        for (StoryEventListener listener : eventListeners) {
            try {
                listener.onStoryEvent(payload.eventType(), payload.eventId(), payload.data());
            } catch (Exception e) {
                LOGGER.error("Error in story event listener: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Handles a state sync update from the server.
     */
    private void handleStateSync(StoryNetworkManager.StoryStateSyncPayload payload) {
        LOGGER.debug("Received state sync for story: {}", payload.storyId());
        
        // Update local state cache
        Map<String, String> storyState = storyStates.computeIfAbsent(
            payload.storyId(), k -> new ConcurrentHashMap<>()
        );
        storyState.putAll(payload.stateUpdates());
        
        // Notify listeners
        for (StateUpdateListener listener : stateListeners) {
            try {
                listener.onStateUpdate(payload.storyId(), payload.stateUpdates());
            } catch (Exception e) {
                LOGGER.error("Error in state update listener: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Handles full state synchronization on join.
     * 
     * Requirements: 31.4
     */
    private void handleFullStateSync(StoryNetworkManager.StoryFullStatePayload payload) {
        LOGGER.info("Received full story state sync ({} stories)", payload.allStates().size());
        
        // Replace all local state with server state
        storyStates.clear();
        for (Map.Entry<String, Map<String, String>> entry : payload.allStates().entrySet()) {
            storyStates.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        
        // Notify listeners of full sync
        for (StateUpdateListener listener : stateListeners) {
            try {
                listener.onFullStateSync(payload.allStates());
            } catch (Exception e) {
                LOGGER.error("Error in full state sync listener: {}", e.getMessage(), e);
            }
        }
    }
    
    // ===== State Access =====
    
    /**
     * Gets a state value from the local cache.
     * 
     * @param storyId The story identifier
     * @param key The state key
     * @return The state value, or null if not found
     */
    public String getState(String storyId, String key) {
        Map<String, String> storyState = storyStates.get(storyId);
        return storyState != null ? storyState.get(key) : null;
    }
    
    /**
     * Gets all state for a story from the local cache.
     * 
     * @param storyId The story identifier
     * @return Copy of the state map
     */
    public Map<String, String> getAllState(String storyId) {
        Map<String, String> storyState = storyStates.get(storyId);
        return storyState != null ? new ConcurrentHashMap<>(storyState) : new ConcurrentHashMap<>();
    }
    
    // ===== Interaction Sending =====
    
    /**
     * Sends a request to start the story for the current player.
     * Called when the player presses the start story keybinding.
     */
    public static void sendStartStoryRequest() {
        Map<String, String> data = new ConcurrentHashMap<>();
        data.put("action", "start");
        getInstance().sendInteraction("start_story", "main", data);
        LOGGER.info("Sent start story request to server");
    }
    
    /**
     * Sends an interaction to the server.
     * 
     * Requirements: 31.2
     * 
     * @param interactionType Type of interaction
     * @param targetId ID of the target
     * @param data Additional data
     */
    public void sendInteraction(String interactionType, String targetId, Map<String, String> data) {
        StoryNetworkManager.StoryInteractionPayload payload = 
            new StoryNetworkManager.StoryInteractionPayload(interactionType, targetId, data);
        ClientPlayNetworking.send(payload);
        LOGGER.debug("Sent interaction: type={}, target={}", interactionType, targetId);
    }
    
    // ===== Listener Management =====
    
    /**
     * Adds a story event listener.
     */
    public void addEventListener(StoryEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Removes a story event listener.
     */
    public void removeEventListener(StoryEventListener listener) {
        eventListeners.remove(listener);
    }
    
    /**
     * Adds a state update listener.
     */
    public void addStateListener(StateUpdateListener listener) {
        stateListeners.add(listener);
    }
    
    /**
     * Removes a state update listener.
     */
    public void removeStateListener(StateUpdateListener listener) {
        stateListeners.remove(listener);
    }
    
    // ===== Listener Interfaces =====
    
    /**
     * Listener for story events.
     */
    public interface StoryEventListener {
        void onStoryEvent(String eventType, String eventId, Map<String, String> data);
    }
    
    /**
     * Listener for state updates.
     */
    public interface StateUpdateListener {
        void onStateUpdate(String storyId, Map<String, String> updates);
        
        default void onFullStateSync(Map<String, Map<String, String>> allStates) {
            // Default implementation: process each story's state as an update
            for (Map.Entry<String, Map<String, String>> entry : allStates.entrySet()) {
                onStateUpdate(entry.getKey(), entry.getValue());
            }
        }
    }
    
    // ===== NPC Animation Handling (Requirements: 10.2) =====
    
    /**
     * Handles an NPC animation packet from the server.
     */
    private void handleNPCAnimation(StoryNetworkManager.NPCAnimationPayload payload) {
        LOGGER.debug("Received NPC animation: npc={}, animation={}, loop={}", 
            payload.npcId(), payload.animationId(), payload.loop());
        
        // Store animation state for client-side rendering
        ru.mjkey.storykee.systems.npc.NPCClientRegistry.getInstance()
            .setNPCAnimation(payload.npcId(), payload.animationId(), payload.loop());
    }
    
    /**
     * Handles an NPC animation stop packet from the server.
     */
    private void handleNPCAnimationStop(StoryNetworkManager.NPCAnimationStopPayload payload) {
        LOGGER.debug("Received NPC animation stop: npc={}", payload.npcId());
        
        // Clear animation state for client-side rendering
        ru.mjkey.storykee.systems.npc.NPCClientRegistry.getInstance()
            .stopNPCAnimation(payload.npcId());
    }
}
