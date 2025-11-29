package ru.mjkey.storykee.runtime.variables;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and persists player-specific variables.
 * Each player has their own variable namespace that persists across sessions.
 */
public class PlayerVariableStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerVariableStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final UUID playerId;
    private final Path storageFile;
    private final Map<String, Object> variables;
    private boolean dirty;
    
    public PlayerVariableStore(UUID playerId, Path storageFile) {
        this.playerId = playerId;
        this.storageFile = storageFile;
        this.variables = new HashMap<>();
        this.dirty = false;
    }
    
    /**
     * Sets a variable value.
     */
    public void set(String key, Object value) {
        variables.put(key, value);
        dirty = true;
    }
    
    /**
     * Gets a variable value, or null if not found.
     */
    public Object get(String key) {
        return variables.get(key);
    }
    
    /**
     * Gets a variable value with a default if not found.
     */
    public Object get(String key, Object defaultValue) {
        return variables.getOrDefault(key, defaultValue);
    }
    
    /**
     * Checks if a variable exists.
     */
    public boolean has(String key) {
        return variables.containsKey(key);
    }
    
    /**
     * Removes a variable.
     */
    public void remove(String key) {
        if (variables.remove(key) != null) {
            dirty = true;
        }
    }
    
    /**
     * Clears all variables.
     */
    public void clear() {
        if (!variables.isEmpty()) {
            variables.clear();
            dirty = true;
        }
    }
    
    /**
     * Gets all variables (read-only copy).
     */
    public Map<String, Object> getAll() {
        return new HashMap<>(variables);
    }
    
    /**
     * Checks if the store has unsaved changes.
     */
    public boolean isDirty() {
        return dirty;
    }
    
    /**
     * Saves variables to disk in JSON format.
     */
    public void save() throws IOException {
        // Create parent directories if they don't exist
        Files.createDirectories(storageFile.getParent());
        
        // Create data structure for serialization
        PlayerData data = new PlayerData();
        data.playerId = playerId.toString();
        data.variables = variables;
        data.timestamp = Instant.now().toString();
        
        // Serialize to JSON
        String json = GSON.toJson(data);
        
        // Write to file
        Files.writeString(storageFile, json);
        
        dirty = false;
        LOGGER.debug("Saved player variables for {}: {} variables", playerId, variables.size());
    }
    
    /**
     * Loads variables from disk.
     */
    public void load() throws IOException {
        if (!Files.exists(storageFile)) {
            LOGGER.debug("No saved variables found for player {}", playerId);
            return;
        }
        
        try {
            // Read JSON from file
            String json = Files.readString(storageFile);
            
            // Deserialize
            PlayerData data = GSON.fromJson(json, PlayerData.class);
            
            if (data == null || data.variables == null) {
                LOGGER.warn("Invalid player data file for {}, starting fresh", playerId);
                return;
            }
            
            // Validate player ID matches
            if (!playerId.toString().equals(data.playerId)) {
                LOGGER.warn("Player ID mismatch in data file for {}, expected {}, got {}", 
                    storageFile, playerId, data.playerId);
            }
            
            // Load variables
            variables.clear();
            variables.putAll(data.variables);
            dirty = false;
            
            LOGGER.debug("Loaded player variables for {}: {} variables", playerId, variables.size());
            
        } catch (JsonSyntaxException e) {
            LOGGER.error("Failed to parse player data file for {}: {}", playerId, e.getMessage());
            throw new IOException("Corrupted player data file", e);
        }
    }
    
    /**
     * Data structure for JSON serialization.
     */
    private static class PlayerData {
        String playerId;
        Map<String, Object> variables;
        String timestamp;
    }
}
