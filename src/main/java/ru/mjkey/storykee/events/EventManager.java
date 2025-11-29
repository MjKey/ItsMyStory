package ru.mjkey.storykee.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.context.ExecutionContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central event manager for the story system.
 * Handles event registration and dispatching.
 */
public class EventManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EventManager.class);
    private static EventManager instance;
    
    // Map of eventType -> List of (scriptId, handler) pairs
    private final Map<String, List<HandlerEntry>> handlers;
    
    private EventManager() {
        this.handlers = new ConcurrentHashMap<>();
    }
    
    public static EventManager getInstance() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }
    
    /**
     * Register an event handler for a specific event type.
     * 
     * @param eventType The type of event to listen for
     * @param scriptId The ID of the script registering the handler
     * @param handler The handler to execute when the event fires
     */
    public void registerHandler(String eventType, String scriptId, EventHandler handler) {
        if (eventType == null || scriptId == null || handler == null) {
            throw new IllegalArgumentException("eventType, scriptId, and handler must not be null");
        }
        
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(new HandlerEntry(scriptId, handler));
        
        LOGGER.debug("Registered handler for event '{}' from script '{}'", eventType, scriptId);
    }
    
    /**
     * Unregister all handlers for a specific script and event type.
     * 
     * @param eventType The type of event
     * @param scriptId The ID of the script to unregister
     */
    public void unregisterHandler(String eventType, String scriptId) {
        if (eventType == null || scriptId == null) {
            return;
        }
        
        List<HandlerEntry> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.removeIf(entry -> entry.scriptId.equals(scriptId));
            LOGGER.debug("Unregistered handlers for event '{}' from script '{}'", eventType, scriptId);
        }
    }
    
    /**
     * Unregister all handlers for a specific script across all event types.
     * 
     * @param scriptId The ID of the script to unregister
     */
    public void unregisterAllHandlers(String scriptId) {
        if (scriptId == null) {
            return;
        }
        
        for (Map.Entry<String, List<HandlerEntry>> entry : handlers.entrySet()) {
            entry.getValue().removeIf(handlerEntry -> handlerEntry.scriptId.equals(scriptId));
        }
        
        LOGGER.debug("Unregistered all handlers for script '{}'", scriptId);
    }
    
    /**
     * Fire an event and execute all registered handlers.
     * Handlers are executed in registration order.
     * If a handler throws an exception, it is logged and other handlers continue.
     * 
     * @param eventType The type of event to fire
     * @param data The event data
     */
    public void fireEvent(String eventType, EventData data) {
        if (eventType == null || data == null) {
            return;
        }
        
        List<HandlerEntry> eventHandlers = handlers.get(eventType);
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return;
        }
        
        LOGGER.debug("Firing event '{}' with {} handlers", eventType, eventHandlers.size());
        
        for (HandlerEntry entry : eventHandlers) {
            try {
                // Create a context for this handler execution
                // In a real implementation, this would be retrieved from the script's context
                ExecutionContext context = new ExecutionContext(entry.scriptId, null);
                entry.handler.handle(data, context);
                
                // If the event was cancelled, stop processing further handlers
                if (data.isCancelled()) {
                    LOGGER.debug("Event '{}' was cancelled by handler from script '{}'", 
                            eventType, entry.scriptId);
                    break;
                }
            } catch (Exception e) {
                // Handler isolation: errors in one handler don't affect others
                LOGGER.error("Error executing event handler for '{}' in script '{}': {}", 
                        eventType, entry.scriptId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Get all handlers registered for a specific event type.
     * 
     * @param eventType The type of event
     * @return List of handlers (defensive copy)
     */
    public List<EventHandler> getHandlers(String eventType) {
        List<HandlerEntry> eventHandlers = handlers.get(eventType);
        if (eventHandlers == null) {
            return Collections.emptyList();
        }
        
        List<EventHandler> result = new ArrayList<>();
        for (HandlerEntry entry : eventHandlers) {
            result.add(entry.handler);
        }
        return result;
    }
    
    /**
     * Get the number of handlers registered for a specific event type.
     * 
     * @param eventType The type of event
     * @return The number of handlers
     */
    public int getHandlerCount(String eventType) {
        List<HandlerEntry> eventHandlers = handlers.get(eventType);
        return eventHandlers != null ? eventHandlers.size() : 0;
    }
    
    /**
     * Clear all registered handlers.
     */
    public void clearAllHandlers() {
        handlers.clear();
        LOGGER.debug("Cleared all event handlers");
    }
    
    /**
     * Internal class to store handler entries with their script IDs.
     */
    private static class HandlerEntry {
        final String scriptId;
        final EventHandler handler;
        
        HandlerEntry(String scriptId, EventHandler handler) {
            this.scriptId = scriptId;
            this.handler = handler;
        }
    }
}
