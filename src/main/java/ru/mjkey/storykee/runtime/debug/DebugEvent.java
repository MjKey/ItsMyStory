package ru.mjkey.storykee.runtime.debug;

/**
 * Represents a debug event.
 */
public class DebugEvent {
    
    public enum Type {
        PAUSED,
        RESUMED,
        STEPPING,
        BREAKPOINT_ADDED,
        BREAKPOINT_REMOVED,
        BREAKPOINT_HIT,
        WATCH_UPDATED,
        ERROR
    }
    
    private final Type type;
    private final String message;
    private final DebugState state;
    private final long timestamp;
    
    public DebugEvent(Type type, String message) {
        this(type, message, null);
    }
    
    public DebugEvent(Type type, String message, DebugState state) {
        this.type = type;
        this.message = message;
        this.state = state;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Type getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public DebugState getState() {
        return state;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("DebugEvent{type=%s, message='%s'}", type, message);
    }
}
