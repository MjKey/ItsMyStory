package ru.mjkey.storykee.events;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains data for a story event.
 */
public class EventData {
    
    private final String eventType;
    private final Map<String, Object> properties;
    private boolean cancelled = false;
    
    public EventData(String eventType) {
        this.eventType = eventType;
        this.properties = new HashMap<>();
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public Object get(String key) {
        return properties.get(key);
    }
    
    public void set(String key, Object value) {
        properties.put(key, value);
    }
    
    public boolean has(String key) {
        return properties.containsKey(key);
    }
    
    public void cancel() {
        this.cancelled = true;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
}
