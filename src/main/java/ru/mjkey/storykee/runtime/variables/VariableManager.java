package ru.mjkey.storykee.runtime.variables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for player and global variables.
 * Handles persistence and retrieval of story state.
 * Provides caching layer for performance.
 */
public class VariableManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VariableManager.class);
    
    private final Path dataDirectory;
    private final GlobalVariableStore globalStore;
    private final Map<UUID, PlayerVariableStore> playerStoreCache;
    
    public VariableManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.globalStore = new GlobalVariableStore(dataDirectory.resolve("global_variables.json"));
        this.playerStoreCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Sets a player-specific variable.
     */
    public void setPlayerVariable(UUID playerId, String key, Object value) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        store.set(key, value);
    }
    
    /**
     * Gets a player-specific variable.
     */
    public Object getPlayerVariable(UUID playerId, String key) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        return store.get(key);
    }
    
    /**
     * Gets a player-specific variable with a default value.
     */
    public Object getPlayerVariable(UUID playerId, String key, Object defaultValue) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        return store.get(key, defaultValue);
    }
    
    /**
     * Checks if a player has a specific variable.
     */
    public boolean hasPlayerVariable(UUID playerId, String key) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        return store.has(key);
    }
    
    /**
     * Removes a player-specific variable.
     */
    public void removePlayerVariable(UUID playerId, String key) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        store.remove(key);
    }
    
    /**
     * Gets all variables for a player.
     */
    public Map<String, Object> getAllPlayerVariables(UUID playerId) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        return store.getAll();
    }
    
    /**
     * Sets a global variable.
     */
    public void setGlobalVariable(String key, Object value) {
        globalStore.set(key, value);
    }
    
    /**
     * Gets a global variable.
     */
    public Object getGlobalVariable(String key) {
        return globalStore.get(key);
    }
    
    /**
     * Gets a global variable with a default value.
     */
    public Object getGlobalVariable(String key, Object defaultValue) {
        return globalStore.get(key, defaultValue);
    }
    
    /**
     * Checks if a global variable exists.
     */
    public boolean hasGlobalVariable(String key) {
        return globalStore.has(key);
    }
    
    /**
     * Removes a global variable.
     */
    public void removeGlobalVariable(String key) {
        globalStore.remove(key);
    }
    
    /**
     * Gets all global variables.
     */
    public Map<String, Object> getAllGlobalVariables() {
        return globalStore.getAll();
    }
    
    /**
     * Saves player variables to disk.
     */
    public void savePlayerVariables(UUID playerId) {
        PlayerVariableStore store = playerStoreCache.get(playerId);
        if (store != null && store.isDirty()) {
            try {
                store.save();
            } catch (IOException e) {
                LOGGER.error("Failed to save player variables for {}: {}", playerId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Loads player variables from disk.
     */
    public void loadPlayerVariables(UUID playerId) {
        PlayerVariableStore store = getOrCreatePlayerStore(playerId);
        try {
            store.load();
        } catch (IOException e) {
            LOGGER.error("Failed to load player variables for {}: {}", playerId, e.getMessage(), e);
        }
    }
    
    /**
     * Saves all player and global variables.
     */
    public void saveAll() {
        // Save global variables
        if (globalStore.isDirty()) {
            try {
                globalStore.save();
            } catch (IOException e) {
                LOGGER.error("Failed to save global variables: {}", e.getMessage(), e);
            }
        }
        
        // Save all cached player variables
        for (Map.Entry<UUID, PlayerVariableStore> entry : playerStoreCache.entrySet()) {
            if (entry.getValue().isDirty()) {
                try {
                    entry.getValue().save();
                } catch (IOException e) {
                    LOGGER.error("Failed to save player variables for {}: {}", 
                        entry.getKey(), e.getMessage(), e);
                }
            }
        }
        
        LOGGER.info("Saved all variables: {} player stores, 1 global store", playerStoreCache.size());
    }
    
    /**
     * Loads all global variables.
     */
    public void loadAll() {
        try {
            globalStore.load();
        } catch (IOException e) {
            LOGGER.error("Failed to load global variables: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clears the player store cache (useful for testing or memory management).
     * Does not affect persisted data.
     */
    public void clearCache() {
        playerStoreCache.clear();
    }
    
    /**
     * Clears a specific player from the cache.
     * Saves their data first if dirty.
     */
    public void clearCache(UUID playerId) {
        PlayerVariableStore store = playerStoreCache.remove(playerId);
        if (store != null && store.isDirty()) {
            try {
                store.save();
            } catch (IOException e) {
                LOGGER.error("Failed to save player variables during cache clear for {}: {}", 
                    playerId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Gets or creates a player variable store.
     */
    private PlayerVariableStore getOrCreatePlayerStore(UUID playerId) {
        return playerStoreCache.computeIfAbsent(playerId, id -> {
            Path playerFile = dataDirectory.resolve("player_variables").resolve(id.toString() + ".json");
            return new PlayerVariableStore(id, playerFile);
        });
    }
    
    /**
     * Gets the number of cached player stores.
     */
    public int getCacheSize() {
        return playerStoreCache.size();
    }
}
