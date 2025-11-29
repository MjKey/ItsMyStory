package ru.mjkey.storykee.runtime.java;

import ru.mjkey.storykee.runtime.context.ExecutionContext;

import java.util.UUID;

/**
 * API available to Java sections for accessing Storykee variables and Minecraft functionality.
 * This class provides a bridge between Java code sections and the Storykee runtime.
 * 
 * Note: Minecraft-specific functionality will be added as the corresponding systems are implemented.
 */
public class StorykeeAPI {
    
    private final ExecutionContext context;
    
    public StorykeeAPI(ExecutionContext context) {
        this.context = context;
    }
    
    // ==================== Variable Access ====================
    
    /**
     * Gets a variable from the current execution context.
     * 
     * @param name The variable name
     * @return The variable value, or null if not found
     */
    public Object getVariable(String name) {
        if (context.hasVariable(name)) {
            return context.getVariable(name);
        }
        return null;
    }
    
    /**
     * Sets a variable in the current execution context.
     * 
     * @param name The variable name
     * @param value The variable value
     */
    public void setVariable(String name, Object value) {
        if (context.hasVariable(name)) {
            context.setVariable(name, value);
        } else {
            context.defineVariable(name, value);
        }
    }
    
    /**
     * Checks if a variable exists in the current context.
     * 
     * @param name The variable name
     * @return true if the variable exists
     */
    public boolean hasVariable(String name) {
        return context.hasVariable(name);
    }
    
    /**
     * Defines a new variable in the current scope.
     * 
     * @param name The variable name
     * @param value The initial value
     */
    public void defineVariable(String name, Object value) {
        context.defineVariable(name, value);
    }
    
    // ==================== Minecraft Server Access ====================
    // Note: Minecraft server access will be implemented when the corresponding systems are ready
    // For now, these methods provide placeholders that can be used in Java sections
    
    // ==================== Helper Methods ====================
    
    /**
     * Logs a message to the console.
     * 
     * @param message The message to log
     */
    public void log(String message) {
        System.out.println("[Storykee Java Section] " + message);
    }
    
    /**
     * Logs a formatted message to the console.
     * 
     * @param format The format string
     * @param args The format arguments
     */
    public void log(String format, Object... args) {
        System.out.println("[Storykee Java Section] " + String.format(format, args));
    }
    

    
    /**
     * Gets the execution context.
     * 
     * @return The execution context
     */
    public ExecutionContext getContext() {
        return context;
    }
    
    /**
     * Gets the script ID.
     * 
     * @return The script ID
     */
    public String getScriptId() {
        return context.getScriptId();
    }
    
    /**
     * Checks if this execution has a player context.
     * 
     * @return true if there is a player context
     */
    public boolean hasPlayer() {
        return context.hasPlayer();
    }
    
    /**
     * Gets the player ID from the context.
     * 
     * @return The player UUID, or null if no player context
     */
    public UUID getPlayerId() {
        return context.getPlayerId();
    }
}
