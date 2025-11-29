package ru.mjkey.storykee.runtime.story;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an isolated namespace for a single story.
 * Provides isolation for variables, commands, and assets.
 * 
 * Requirements: 41.1, 41.2, 41.3, 41.4, 41.5
 */
public class StoryNamespace {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryNamespace.class);
    
    private final String storyId;
    private final Path storyDirectory;
    private final Path scriptsDirectory;
    private final Path assetsDirectory;
    
    // Isolated variable storage for this story
    private final Map<String, Object> globalVariables;
    private final Map<UUID, Map<String, Object>> playerVariables;
    
    // Registered commands for this story (with prefix)
    private final Set<String> registeredCommands;
    
    // Registered event handlers for this story
    private final Set<String> registeredEventHandlers;
    
    // Registered timers for this story
    private final Set<UUID> registeredTimers;
    
    // Registered NPCs for this story
    private final Set<String> registeredNPCs;
    
    // Registered HUD elements for this story
    private final Set<String> registeredHUDElements;
    
    // Story state
    private boolean loaded;
    private long loadedAt;
    
    public StoryNamespace(String storyId, Path storyDirectory) {
        this.storyId = Objects.requireNonNull(storyId, "storyId cannot be null");
        this.storyDirectory = Objects.requireNonNull(storyDirectory, "storyDirectory cannot be null");
        this.scriptsDirectory = storyDirectory;
        this.assetsDirectory = storyDirectory.resolve("assets");
        
        this.globalVariables = new ConcurrentHashMap<>();
        this.playerVariables = new ConcurrentHashMap<>();
        this.registeredCommands = ConcurrentHashMap.newKeySet();
        this.registeredEventHandlers = ConcurrentHashMap.newKeySet();
        this.registeredTimers = ConcurrentHashMap.newKeySet();
        this.registeredNPCs = ConcurrentHashMap.newKeySet();
        this.registeredHUDElements = ConcurrentHashMap.newKeySet();
        
        this.loaded = false;
        this.loadedAt = 0;
    }

    
    // ===== Getters =====
    
    public String getStoryId() {
        return storyId;
    }
    
    public Path getStoryDirectory() {
        return storyDirectory;
    }
    
    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }
    
    public Path getAssetsDirectory() {
        return assetsDirectory;
    }
    
    public boolean isLoaded() {
        return loaded;
    }
    
    public long getLoadedAt() {
        return loadedAt;
    }
    
    // ===== Variable Management (Requirement 41.1) =====
    
    /**
     * Sets a global variable within this story's namespace.
     */
    public void setGlobalVariable(String key, Object value) {
        if (key == null || key.isEmpty()) {
            return;
        }
        globalVariables.put(key, value);
    }
    
    /**
     * Gets a global variable from this story's namespace.
     */
    public Object getGlobalVariable(String key) {
        return globalVariables.get(key);
    }
    
    /**
     * Gets a global variable with a default value.
     */
    public Object getGlobalVariable(String key, Object defaultValue) {
        return globalVariables.getOrDefault(key, defaultValue);
    }
    
    /**
     * Checks if a global variable exists in this story's namespace.
     */
    public boolean hasGlobalVariable(String key) {
        return globalVariables.containsKey(key);
    }
    
    /**
     * Removes a global variable from this story's namespace.
     */
    public void removeGlobalVariable(String key) {
        globalVariables.remove(key);
    }
    
    /**
     * Gets all global variables in this story's namespace.
     */
    public Map<String, Object> getAllGlobalVariables() {
        return Collections.unmodifiableMap(globalVariables);
    }
    
    /**
     * Sets a player variable within this story's namespace.
     */
    public void setPlayerVariable(UUID playerId, String key, Object value) {
        if (playerId == null || key == null || key.isEmpty()) {
            return;
        }
        playerVariables.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(key, value);
    }
    
    /**
     * Gets a player variable from this story's namespace.
     */
    public Object getPlayerVariable(UUID playerId, String key) {
        if (playerId == null || key == null) {
            return null;
        }
        Map<String, Object> vars = playerVariables.get(playerId);
        return vars != null ? vars.get(key) : null;
    }
    
    /**
     * Gets a player variable with a default value.
     */
    public Object getPlayerVariable(UUID playerId, String key, Object defaultValue) {
        Object value = getPlayerVariable(playerId, key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Checks if a player variable exists in this story's namespace.
     */
    public boolean hasPlayerVariable(UUID playerId, String key) {
        if (playerId == null || key == null) {
            return false;
        }
        Map<String, Object> vars = playerVariables.get(playerId);
        return vars != null && vars.containsKey(key);
    }
    
    /**
     * Removes a player variable from this story's namespace.
     */
    public void removePlayerVariable(UUID playerId, String key) {
        if (playerId == null || key == null) {
            return;
        }
        Map<String, Object> vars = playerVariables.get(playerId);
        if (vars != null) {
            vars.remove(key);
        }
    }
    
    /**
     * Gets all player variables for a specific player in this story's namespace.
     */
    public Map<String, Object> getAllPlayerVariables(UUID playerId) {
        if (playerId == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> vars = playerVariables.get(playerId);
        return vars != null ? Collections.unmodifiableMap(vars) : Collections.emptyMap();
    }
    
    // ===== Command Management (Requirement 41.2) =====
    
    /**
     * Gets the prefixed command name for this story.
     * Commands are prefixed with the story ID to prevent conflicts.
     */
    public String getPrefixedCommandName(String commandName) {
        if (commandName == null || commandName.isEmpty()) {
            return null;
        }
        return storyId + ":" + commandName;
    }
    
    /**
     * Registers a command for this story.
     */
    public void registerCommand(String commandName) {
        if (commandName != null && !commandName.isEmpty()) {
            registeredCommands.add(commandName);
        }
    }
    
    /**
     * Unregisters a command from this story.
     */
    public void unregisterCommand(String commandName) {
        registeredCommands.remove(commandName);
    }
    
    /**
     * Gets all registered commands for this story.
     */
    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(registeredCommands);
    }
    
    /**
     * Checks if a command is registered for this story.
     */
    public boolean hasCommand(String commandName) {
        return registeredCommands.contains(commandName);
    }

    
    // ===== Asset Path Management (Requirement 41.3) =====
    
    /**
     * Validates that a path is within this story's asset directory.
     * Prevents access to assets from other stories.
     */
    public boolean isValidAssetPath(Path path) {
        if (path == null) {
            return false;
        }
        try {
            Path normalizedPath = path.toAbsolutePath().normalize();
            Path normalizedAssetsDir = assetsDirectory.toAbsolutePath().normalize();
            return normalizedPath.startsWith(normalizedAssetsDir);
        } catch (Exception e) {
            LOGGER.warn("Error validating asset path: {}", path, e);
            return false;
        }
    }
    
    /**
     * Resolves an asset path within this story's asset directory.
     * Returns null if the path would escape the asset directory.
     */
    public Path resolveAssetPath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        
        // Prevent path traversal attacks
        if (relativePath.contains("..")) {
            LOGGER.warn("Attempted path traversal in story {}: {}", storyId, relativePath);
            return null;
        }
        
        Path resolved = assetsDirectory.resolve(relativePath);
        if (isValidAssetPath(resolved)) {
            return resolved;
        }
        return null;
    }
    
    /**
     * Gets the textures directory for this story.
     */
    public Path getTexturesDirectory() {
        return assetsDirectory.resolve("textures");
    }
    
    /**
     * Gets the sounds directory for this story.
     */
    public Path getSoundsDirectory() {
        return assetsDirectory.resolve("sounds");
    }
    
    /**
     * Gets the models directory for this story.
     */
    public Path getModelsDirectory() {
        return assetsDirectory.resolve("models");
    }
    
    /**
     * Gets the lang directory for this story.
     */
    public Path getLangDirectory() {
        return assetsDirectory.resolve("lang");
    }
    
    // ===== Resource Registration (for cleanup on unload) =====
    
    /**
     * Registers an event handler for this story.
     */
    public void registerEventHandler(String handlerId) {
        if (handlerId != null && !handlerId.isEmpty()) {
            registeredEventHandlers.add(handlerId);
        }
    }
    
    /**
     * Unregisters an event handler from this story.
     */
    public void unregisterEventHandler(String handlerId) {
        registeredEventHandlers.remove(handlerId);
    }
    
    /**
     * Gets all registered event handlers for this story.
     */
    public Set<String> getRegisteredEventHandlers() {
        return Collections.unmodifiableSet(registeredEventHandlers);
    }
    
    /**
     * Registers a timer for this story.
     */
    public void registerTimer(UUID timerId) {
        if (timerId != null) {
            registeredTimers.add(timerId);
        }
    }
    
    /**
     * Unregisters a timer from this story.
     */
    public void unregisterTimer(UUID timerId) {
        registeredTimers.remove(timerId);
    }
    
    /**
     * Gets all registered timers for this story.
     */
    public Set<UUID> getRegisteredTimers() {
        return Collections.unmodifiableSet(registeredTimers);
    }
    
    /**
     * Registers an NPC for this story.
     */
    public void registerNPC(String npcId) {
        if (npcId != null && !npcId.isEmpty()) {
            registeredNPCs.add(npcId);
        }
    }
    
    /**
     * Unregisters an NPC from this story.
     */
    public void unregisterNPC(String npcId) {
        registeredNPCs.remove(npcId);
    }
    
    /**
     * Gets all registered NPCs for this story.
     */
    public Set<String> getRegisteredNPCs() {
        return Collections.unmodifiableSet(registeredNPCs);
    }
    
    /**
     * Registers a HUD element for this story.
     */
    public void registerHUDElement(String elementId) {
        if (elementId != null && !elementId.isEmpty()) {
            registeredHUDElements.add(elementId);
        }
    }
    
    /**
     * Unregisters a HUD element from this story.
     */
    public void unregisterHUDElement(String elementId) {
        registeredHUDElements.remove(elementId);
    }
    
    /**
     * Gets all registered HUD elements for this story.
     */
    public Set<String> getRegisteredHUDElements() {
        return Collections.unmodifiableSet(registeredHUDElements);
    }
    
    // ===== Lifecycle Management (Requirement 41.5) =====
    
    /**
     * Marks this story as loaded.
     */
    public void markLoaded() {
        this.loaded = true;
        this.loadedAt = System.currentTimeMillis();
        LOGGER.info("Story namespace loaded: {}", storyId);
    }
    
    /**
     * Cleans up all resources associated with this story.
     * Called when the story is unloaded.
     * 
     * Requirement 41.5: Clean up all resources without affecting other stories.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up story namespace: {}", storyId);
        
        // Clear variables
        globalVariables.clear();
        playerVariables.clear();
        
        // Note: Actual cleanup of commands, event handlers, timers, NPCs, and HUD elements
        // should be done by the StoryManager which has access to the respective managers.
        // Here we just track what needs to be cleaned up.
        
        int commandCount = registeredCommands.size();
        int handlerCount = registeredEventHandlers.size();
        int timerCount = registeredTimers.size();
        int npcCount = registeredNPCs.size();
        int hudCount = registeredHUDElements.size();
        
        registeredCommands.clear();
        registeredEventHandlers.clear();
        registeredTimers.clear();
        registeredNPCs.clear();
        registeredHUDElements.clear();
        
        this.loaded = false;
        
        LOGGER.info("Story namespace {} cleaned up: {} commands, {} handlers, {} timers, {} NPCs, {} HUD elements",
            storyId, commandCount, handlerCount, timerCount, npcCount, hudCount);
    }
    
    /**
     * Gets a summary of resources registered to this story.
     */
    public Map<String, Integer> getResourceSummary() {
        Map<String, Integer> summary = new LinkedHashMap<>();
        summary.put("globalVariables", globalVariables.size());
        summary.put("playerVariables", playerVariables.size());
        summary.put("commands", registeredCommands.size());
        summary.put("eventHandlers", registeredEventHandlers.size());
        summary.put("timers", registeredTimers.size());
        summary.put("npcs", registeredNPCs.size());
        summary.put("hudElements", registeredHUDElements.size());
        return summary;
    }
    
    @Override
    public String toString() {
        return "StoryNamespace{" +
            "storyId='" + storyId + '\'' +
            ", loaded=" + loaded +
            ", resources=" + getResourceSummary() +
            '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoryNamespace that = (StoryNamespace) o;
        return Objects.equals(storyId, that.storyId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(storyId);
    }
}
