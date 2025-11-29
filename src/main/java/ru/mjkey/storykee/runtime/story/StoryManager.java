package ru.mjkey.storykee.runtime.story;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages story namespaces and provides isolation between stories.
 * Ensures that variables, commands, and resources from one story
 * cannot interfere with another story.
 * 
 * Requirements: 41.1, 41.2, 41.3, 41.4, 41.5
 */
public class StoryManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryManager.class);
    
    private static StoryManager instance;
    
    // All loaded story namespaces: storyId -> namespace
    private final Map<String, StoryNamespace> namespaces;
    
    // Listeners for story lifecycle events
    private final List<StoryLifecycleListener> lifecycleListeners;
    
    private StoryManager() {
        this.namespaces = new ConcurrentHashMap<>();
        this.lifecycleListeners = new ArrayList<>();
    }
    
    public static StoryManager getInstance() {
        if (instance == null) {
            instance = new StoryManager();
        }
        return instance;
    }
    
    public static void resetInstance() {
        if (instance != null) {
            instance.unloadAllStories();
        }
        instance = null;
    }
    
    // ===== Story Discovery and Loading =====
    
    /**
     * Discovers all stories in the scripts directory.
     * A story is identified by a subdirectory containing .skee files.
     */
    public List<String> discoverStories() {
        List<String> storyIds = new ArrayList<>();
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path scriptsDir = runtime.getScriptsDirectory();
            
            if (!Files.exists(scriptsDir)) {
                return storyIds;
            }
            
            // Each subdirectory in scripts is a potential story
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        String storyId = entry.getFileName().toString();
                        // Check if directory contains any .skee files
                        if (containsScriptFiles(entry)) {
                            storyIds.add(storyId);
                        }
                    }
                }
            }
            
            // Also check for scripts directly in the scripts directory (default story)
            if (containsScriptFiles(scriptsDir)) {
                storyIds.add("default");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error discovering stories", e);
        }
        
        LOGGER.info("Discovered {} stories: {}", storyIds.size(), storyIds);
        return storyIds;
    }
    
    private boolean containsScriptFiles(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.skee")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }

    
    /**
     * Loads a story and creates its namespace.
     * 
     * @param storyId The story identifier
     * @return The created namespace, or null if loading failed
     */
    public StoryNamespace loadStory(String storyId) {
        if (storyId == null || storyId.isEmpty()) {
            LOGGER.warn("Cannot load story with null or empty ID");
            return null;
        }
        
        // Check if already loaded
        if (namespaces.containsKey(storyId)) {
            LOGGER.debug("Story {} is already loaded", storyId);
            return namespaces.get(storyId);
        }
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path scriptsDir = runtime.getScriptsDirectory();
            
            Path storyDir;
            if ("default".equals(storyId)) {
                storyDir = scriptsDir;
            } else {
                storyDir = scriptsDir.resolve(storyId);
            }
            
            if (!Files.exists(storyDir)) {
                LOGGER.warn("Story directory does not exist: {}", storyDir);
                return null;
            }
            
            StoryNamespace namespace = new StoryNamespace(storyId, storyDir);
            namespaces.put(storyId, namespace);
            namespace.markLoaded();
            
            // Notify listeners
            fireStoryLoaded(namespace);
            
            LOGGER.info("Loaded story: {}", storyId);
            return namespace;
            
        } catch (Exception e) {
            LOGGER.error("Error loading story: {}", storyId, e);
            return null;
        }
    }
    
    /**
     * Unloads a story and cleans up its resources.
     * 
     * Requirement 41.5: Clean up all resources without affecting other stories.
     * 
     * @param storyId The story identifier
     */
    public void unloadStory(String storyId) {
        if (storyId == null || storyId.isEmpty()) {
            return;
        }
        
        StoryNamespace namespace = namespaces.remove(storyId);
        if (namespace != null) {
            // Notify listeners before cleanup
            fireStoryUnloading(namespace);
            
            // Perform cleanup
            namespace.cleanup();
            
            LOGGER.info("Unloaded story: {}", storyId);
        }
    }
    
    /**
     * Unloads all stories.
     */
    public void unloadAllStories() {
        List<String> storyIds = new ArrayList<>(namespaces.keySet());
        for (String storyId : storyIds) {
            unloadStory(storyId);
        }
        LOGGER.info("Unloaded all stories");
    }
    
    /**
     * Reloads a story by unloading and loading it again.
     * 
     * @param storyId The story identifier
     * @return The reloaded namespace, or null if reloading failed
     */
    public StoryNamespace reloadStory(String storyId) {
        unloadStory(storyId);
        return loadStory(storyId);
    }
    
    // ===== Namespace Access =====
    
    /**
     * Gets a story namespace by ID.
     * 
     * @param storyId The story identifier
     * @return The namespace, or null if not loaded
     */
    public StoryNamespace getNamespace(String storyId) {
        return namespaces.get(storyId);
    }
    
    /**
     * Gets a story namespace, loading it if necessary.
     * 
     * @param storyId The story identifier
     * @return The namespace, or null if loading failed
     */
    public StoryNamespace getOrLoadNamespace(String storyId) {
        StoryNamespace namespace = namespaces.get(storyId);
        if (namespace == null) {
            namespace = loadStory(storyId);
        }
        return namespace;
    }
    
    /**
     * Checks if a story is loaded.
     * 
     * @param storyId The story identifier
     * @return true if the story is loaded
     */
    public boolean isStoryLoaded(String storyId) {
        return namespaces.containsKey(storyId);
    }
    
    /**
     * Gets all loaded story IDs.
     * 
     * @return Set of loaded story IDs
     */
    public Set<String> getLoadedStoryIds() {
        return Collections.unmodifiableSet(namespaces.keySet());
    }
    
    /**
     * Gets all loaded namespaces.
     * 
     * @return Collection of loaded namespaces
     */
    public Collection<StoryNamespace> getAllNamespaces() {
        return Collections.unmodifiableCollection(namespaces.values());
    }
    
    /**
     * Gets the number of loaded stories.
     * 
     * @return Number of loaded stories
     */
    public int getLoadedStoryCount() {
        return namespaces.size();
    }
    
    // ===== Cross-Story Isolation (Requirement 41.4) =====
    
    /**
     * Validates that a story can access a resource.
     * Prevents cross-story interference.
     * 
     * @param storyId The story requesting access
     * @param resourceStoryId The story that owns the resource
     * @return true if access is allowed
     */
    public boolean canAccessResource(String storyId, String resourceStoryId) {
        // Stories can only access their own resources
        return storyId != null && storyId.equals(resourceStoryId);
    }
    
    /**
     * Gets the story ID from a prefixed resource name.
     * 
     * @param prefixedName The prefixed resource name (e.g., "mystory:mycommand")
     * @return The story ID, or null if not prefixed
     */
    public String getStoryIdFromPrefix(String prefixedName) {
        if (prefixedName == null || !prefixedName.contains(":")) {
            return null;
        }
        int colonIndex = prefixedName.indexOf(':');
        return prefixedName.substring(0, colonIndex);
    }
    
    /**
     * Gets the resource name from a prefixed resource name.
     * 
     * @param prefixedName The prefixed resource name (e.g., "mystory:mycommand")
     * @return The resource name without prefix
     */
    public String getResourceNameFromPrefix(String prefixedName) {
        if (prefixedName == null) {
            return null;
        }
        if (!prefixedName.contains(":")) {
            return prefixedName;
        }
        int colonIndex = prefixedName.indexOf(':');
        return prefixedName.substring(colonIndex + 1);
    }

    
    // ===== Lifecycle Listeners =====
    
    /**
     * Adds a story lifecycle listener.
     */
    public void addLifecycleListener(StoryLifecycleListener listener) {
        if (listener != null && !lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener);
        }
    }
    
    /**
     * Removes a story lifecycle listener.
     */
    public void removeLifecycleListener(StoryLifecycleListener listener) {
        lifecycleListeners.remove(listener);
    }
    
    private void fireStoryLoaded(StoryNamespace namespace) {
        for (StoryLifecycleListener listener : lifecycleListeners) {
            try {
                listener.onStoryLoaded(namespace);
            } catch (Exception e) {
                LOGGER.error("Error in story lifecycle listener", e);
            }
        }
    }
    
    private void fireStoryUnloading(StoryNamespace namespace) {
        for (StoryLifecycleListener listener : lifecycleListeners) {
            try {
                listener.onStoryUnloading(namespace);
            } catch (Exception e) {
                LOGGER.error("Error in story lifecycle listener", e);
            }
        }
    }
    
    // ===== Utility Methods =====
    
    /**
     * Gets a summary of all loaded stories.
     */
    public Map<String, Map<String, Integer>> getStorySummary() {
        Map<String, Map<String, Integer>> summary = new LinkedHashMap<>();
        for (Map.Entry<String, StoryNamespace> entry : namespaces.entrySet()) {
            summary.put(entry.getKey(), entry.getValue().getResourceSummary());
        }
        return summary;
    }
    
    /**
     * Listener interface for story lifecycle events.
     */
    public interface StoryLifecycleListener {
        /**
         * Called when a story is loaded.
         */
        void onStoryLoaded(StoryNamespace namespace);
        
        /**
         * Called before a story is unloaded.
         */
        void onStoryUnloading(StoryNamespace namespace);
    }
}
