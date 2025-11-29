package ru.mjkey.storykee.systems.advancement;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Context for evaluating advancement criteria.
 * 
 * Requirements: 51.2
 */
public class CriterionContext {
    
    private final UUID playerId;
    private final Map<String, Object> data;
    
    public CriterionContext(UUID playerId, Map<String, Object> data) {
        this.playerId = playerId;
        this.data = data != null ? data : Collections.emptyMap();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public Object get(String key) {
        return data.get(key);
    }
    
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? String.valueOf(value) : null;
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public boolean has(String key) {
        return data.containsKey(key);
    }
}
