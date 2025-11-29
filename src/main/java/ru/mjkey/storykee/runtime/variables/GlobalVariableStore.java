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

/**
 * Stores and persists global variables shared across all scripts.
 * Global variables are accessible to all stories and persist across sessions.
 */
public class GlobalVariableStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalVariableStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path storageFile;
    private final Map<String, Object> variables;
    private boolean dirty;
    
    public GlobalVariableStore(Path storageFile) {
        this.storageFile = storageFile;
        this.variables = new HashMap<>();
        this.dirty = false;
    }
    
    /**
     * Sets a global variable value.
     */
    public void set(String key, Object value) {
        variables.put(key, value);
        dirty = true;
    }
    
    /**
     * Gets a global variable value, or null if not found.
     */
    public Object get(String key) {
        return variables.get(key);
    }
    
    /**
     * Gets a global variable value with a default if not found.
     */
    public Object get(String key, Object defaultValue) {
        return variables.getOrDefault(key, defaultValue);
    }
    
    /**
     * Checks if a global variable exists.
     */
    public boolean has(String key) {
        return variables.containsKey(key);
    }
    
    /**
     * Removes a global variable.
     */
    public void remove(String key) {
        if (variables.remove(key) != null) {
            dirty = true;
        }
    }
    
    /**
     * Clears all global variables.
     */
    public void clear() {
        if (!variables.isEmpty()) {
            variables.clear();
            dirty = true;
        }
    }
    
    /**
     * Gets all global variables (read-only copy).
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
     * Saves global variables to disk in JSON format.
     */
    public void save() throws IOException {
        // Create parent directories if they don't exist
        Files.createDirectories(storageFile.getParent());
        
        // Create data structure for serialization
        GlobalData data = new GlobalData();
        data.variables = variables;
        data.timestamp = Instant.now().toString();
        
        // Serialize to JSON
        String json = GSON.toJson(data);
        
        // Write to file
        Files.writeString(storageFile, json);
        
        dirty = false;
        LOGGER.debug("Saved global variables: {} variables", variables.size());
    }
    
    /**
     * Loads global variables from disk.
     */
    public void load() throws IOException {
        if (!Files.exists(storageFile)) {
            LOGGER.debug("No saved global variables found");
            return;
        }
        
        try {
            // Read JSON from file
            String json = Files.readString(storageFile);
            
            // Deserialize
            GlobalData data = GSON.fromJson(json, GlobalData.class);
            
            if (data == null || data.variables == null) {
                LOGGER.warn("Invalid global data file, starting fresh");
                return;
            }
            
            // Load variables
            variables.clear();
            variables.putAll(data.variables);
            dirty = false;
            
            LOGGER.debug("Loaded global variables: {} variables", variables.size());
            
        } catch (JsonSyntaxException e) {
            LOGGER.error("Failed to parse global data file: {}", e.getMessage());
            throw new IOException("Corrupted global data file", e);
        }
    }
    
    /**
     * Data structure for JSON serialization.
     */
    private static class GlobalData {
        Map<String, Object> variables;
        String timestamp;
    }
}
