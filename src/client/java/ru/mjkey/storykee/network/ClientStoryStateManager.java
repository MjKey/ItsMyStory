package ru.mjkey.storykee.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Client-side manager for story state synchronization.
 * Maintains local cache of story states and provides access to synchronized data.
 * 
 * Requirements: 31.3, 31.4
 */
public class ClientStoryStateManager implements StoryNetworkClientHandler.StateUpdateListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientStoryStateManager.class);
    
    private static ClientStoryStateManager instance;
    
    // Local cache of story states
    private final Map<String, Map<String, String>> storyStates;
    
    // Active story contexts the player is participating in
    private final Set<String> activeContexts;
    
    // Listeners for state changes
    private final Set<StateChangeListener> listeners;
    
    // Flag indicating if initial sync has been received
    private volatile boolean initialSyncReceived;
    
    private ClientStoryStateManager() {
        this.storyStates = new ConcurrentHashMap<>();
        this.activeContexts = new CopyOnWriteArraySet<>();
        this.listeners = new CopyOnWriteArraySet<>();
        this.initialSyncReceived = false;
    }
    
    public static ClientStoryStateManager getInstance() {
        if (instance == null) {
            instance = new ClientStoryStateManager();
        }
        return instance;
    }
    
    /**
     * Initializes the state manager and registers with the network handler.
     */
    public static void initialize() {
        ClientStoryStateManager manager = getInstance();
        StoryNetworkClientHandler.getInstance().addStateListener(manager);
        LOGGER.info("Client story state manager initialized");
    }

    // ===== StateUpdateListener Implementation =====
    
    @Override
    public void onStateUpdate(String storyId, Map<String, String> updates) {
        LOGGER.debug("State update for story {}: {} keys", storyId, updates.size());
        
        Map<String, String> storyState = storyStates.computeIfAbsent(
            storyId, k -> new ConcurrentHashMap<>()
        );
        
        // Track which keys changed
        Map<String, String> changedKeys = new ConcurrentHashMap<>();
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String oldValue = storyState.get(entry.getKey());
            if (oldValue == null || !oldValue.equals(entry.getValue())) {
                changedKeys.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Apply updates
        storyState.putAll(updates);
        
        // Notify listeners of changes
        if (!changedKeys.isEmpty()) {
            for (StateChangeListener listener : listeners) {
                try {
                    listener.onStateChanged(storyId, changedKeys);
                } catch (Exception e) {
                    LOGGER.error("Error in state change listener: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    @Override
    public void onFullStateSync(Map<String, Map<String, String>> allStates) {
        LOGGER.info("Full state sync received: {} stories", allStates.size());
        
        // Clear existing state
        storyStates.clear();
        
        // Apply all states
        for (Map.Entry<String, Map<String, String>> entry : allStates.entrySet()) {
            storyStates.put(entry.getKey(), new ConcurrentHashMap<>(entry.getValue()));
        }
        
        initialSyncReceived = true;
        
        // Notify listeners of full sync
        for (StateChangeListener listener : listeners) {
            try {
                listener.onFullSync(allStates);
            } catch (Exception e) {
                LOGGER.error("Error in full sync listener: {}", e.getMessage(), e);
            }
        }
    }
    
    // ===== State Access =====
    
    /**
     * Gets a state value.
     * 
     * @param storyId The story identifier
     * @param key The state key
     * @return The value, or null if not found
     */
    public String getState(String storyId, String key) {
        Map<String, String> storyState = storyStates.get(storyId);
        return storyState != null ? storyState.get(key) : null;
    }
    
    /**
     * Gets a state value with a default.
     * 
     * @param storyId The story identifier
     * @param key The state key
     * @param defaultValue Default value if not found
     * @return The value, or defaultValue if not found
     */
    public String getState(String storyId, String key, String defaultValue) {
        String value = getState(storyId, key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Gets a state value as an integer.
     * 
     * @param storyId The story identifier
     * @param key The state key
     * @param defaultValue Default value if not found or not parseable
     * @return The integer value
     */
    public int getStateInt(String storyId, String key, int defaultValue) {
        String value = getState(storyId, key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Gets a state value as a boolean.
     * 
     * @param storyId The story identifier
     * @param key The state key
     * @param defaultValue Default value if not found
     * @return The boolean value
     */
    public boolean getStateBool(String storyId, String key, boolean defaultValue) {
        String value = getState(storyId, key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Gets all state for a story.
     * 
     * @param storyId The story identifier
     * @return Copy of the state map
     */
    public Map<String, String> getAllState(String storyId) {
        Map<String, String> storyState = storyStates.get(storyId);
        return storyState != null ? new ConcurrentHashMap<>(storyState) : new ConcurrentHashMap<>();
    }
    
    /**
     * Checks if a state key exists.
     * 
     * @param storyId The story identifier
     * @param key The state key
     * @return true if the key exists
     */
    public boolean hasState(String storyId, String key) {
        Map<String, String> storyState = storyStates.get(storyId);
        return storyState != null && storyState.containsKey(key);
    }
    
    /**
     * Checks if initial sync has been received.
     * 
     * @return true if initial sync has been received
     */
    public boolean isInitialSyncReceived() {
        return initialSyncReceived;
    }
    
    // ===== Context Management =====
    
    /**
     * Marks a story context as active for this client.
     * 
     * @param storyId The story identifier
     */
    public void enterContext(String storyId) {
        activeContexts.add(storyId);
        LOGGER.debug("Entered story context: {}", storyId);
    }
    
    /**
     * Marks a story context as inactive for this client.
     * 
     * @param storyId The story identifier
     */
    public void exitContext(String storyId) {
        activeContexts.remove(storyId);
        LOGGER.debug("Exited story context: {}", storyId);
    }
    
    /**
     * Checks if a story context is active.
     * 
     * @param storyId The story identifier
     * @return true if the context is active
     */
    public boolean isInContext(String storyId) {
        return activeContexts.contains(storyId);
    }
    
    /**
     * Gets all active story contexts.
     * 
     * @return Set of active story IDs
     */
    public Set<String> getActiveContexts() {
        return new CopyOnWriteArraySet<>(activeContexts);
    }
    
    // ===== Listener Management =====
    
    /**
     * Adds a state change listener.
     * 
     * @param listener The listener to add
     */
    public void addListener(StateChangeListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a state change listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }
    
    // ===== Cleanup =====
    
    /**
     * Clears all state (called on disconnect).
     */
    public void clear() {
        storyStates.clear();
        activeContexts.clear();
        initialSyncReceived = false;
        LOGGER.debug("Client story state cleared");
    }
    
    // ===== Listener Interface =====
    
    /**
     * Listener for state changes.
     */
    public interface StateChangeListener {
        /**
         * Called when state values change.
         * 
         * @param storyId The story identifier
         * @param changedKeys Map of changed keys and their new values
         */
        void onStateChanged(String storyId, Map<String, String> changedKeys);
        
        /**
         * Called when full state sync is received.
         * 
         * @param allStates All story states
         */
        default void onFullSync(Map<String, Map<String, String>> allStates) {
            // Default: process each story as a change
            for (Map.Entry<String, Map<String, String>> entry : allStates.entrySet()) {
                onStateChanged(entry.getKey(), entry.getValue());
            }
        }
    }
}
