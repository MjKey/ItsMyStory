package ru.mjkey.storykee.events;

import ru.mjkey.storykee.runtime.context.ExecutionContext;

/**
 * Interface for event handlers.
 * Handlers are executed when their registered event type is fired.
 */
@FunctionalInterface
public interface EventHandler {
    
    /**
     * Handle an event.
     * 
     * @param data The event data containing properties and cancellation state
     * @param context The execution context for this handler
     */
    void handle(EventData data, ExecutionContext context);
}
