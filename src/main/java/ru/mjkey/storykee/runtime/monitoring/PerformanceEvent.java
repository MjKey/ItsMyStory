package ru.mjkey.storykee.runtime.monitoring;

/**
 * Represents a performance-related event.
 */
public class PerformanceEvent {
    
    public enum Type {
        SLOW_EXECUTION,
        SCRIPT_THROTTLED,
        SCRIPT_UNTHROTTLED,
        EXECUTION_ERROR,
        STATISTICS_RESET
    }
    
    private final Type type;
    private final String scriptId;
    private final long value;
    private final String message;
    private final long timestamp;
    
    public PerformanceEvent(Type type, String scriptId, long value, String message) {
        this.type = type;
        this.scriptId = scriptId;
        this.value = value;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Type getType() {
        return type;
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public long getValue() {
        return value;
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceEvent{type=%s, scriptId='%s', value=%d, message='%s'}",
            type, scriptId, value, message);
    }
}
