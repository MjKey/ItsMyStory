package ru.mjkey.storykee.runtime.story;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages story configurations.
 * Handles loading, caching, and hot-reloading of config.json files.
 * 
 * Requirements: 42.1, 42.2, 42.3, 42.4, 42.5
 */
public class ConfigManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    
    private static ConfigManager instance;
    
    // Cached configurations: storyId -> config
    private final Map<String, StoryConfig> configCache;
    
    // Listeners for config change events
    private final List<ConfigChangeListener> changeListeners;
    
    private ConfigManager() {
        this.configCache = new ConcurrentHashMap<>();
        this.changeListeners = new ArrayList<>();
    }
    
    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public static void resetInstance() {
        instance = null;
    }
    
    // ===== Configuration Loading (Requirement 42.1) =====
    
    /**
     * Loads configuration for a story.
     * 
     * @param storyId The story identifier
     * @return The loaded configuration
     */
    public StoryConfig loadConfig(String storyId) {
        if (storyId == null || storyId.isEmpty()) {
            LOGGER.warn("Cannot load config for null or empty story ID");
            return new StoryConfig();
        }
        
        // Check cache first
        StoryConfig cached = configCache.get(storyId);
        if (cached != null && !cached.hasBeenModified()) {
            return cached;
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
            
            StoryConfig config = StoryConfig.loadFromDirectory(storyDir);
            configCache.put(storyId, config);
            
            // Log validation warnings (Requirement 42.3)
            if (!config.isValid()) {
                LOGGER.warn("Config for story {} has validation issues: {}", 
                    storyId, config.getValidationErrors());
            }
            
            return config;
            
        } catch (Exception e) {
            LOGGER.error("Error loading config for story {}: {}", storyId, e.getMessage());
            return new StoryConfig();
        }
    }
    
    /**
     * Gets configuration for a story, loading it if necessary.
     * 
     * @param storyId The story identifier
     * @return The configuration
     */
    public StoryConfig getConfig(String storyId) {
        StoryConfig config = configCache.get(storyId);
        if (config == null) {
            config = loadConfig(storyId);
        }
        return config;
    }
    
    /**
     * Checks if a configuration is loaded for a story.
     * 
     * @param storyId The story identifier
     * @return true if configuration is loaded
     */
    public boolean hasConfig(String storyId) {
        return configCache.containsKey(storyId);
    }
    
    // ===== Configuration Access (Requirement 42.2) =====
    
    /**
     * Gets a configuration value for a story.
     * 
     * @param storyId The story identifier
     * @param key The configuration key
     * @return The configuration value, or null if not found
     */
    public Object getConfigValue(String storyId, String key) {
        StoryConfig config = getConfig(storyId);
        return config.getCustomValue(key);
    }
    
    /**
     * Gets a configuration value with a default.
     * 
     * @param storyId The story identifier
     * @param key The configuration key
     * @param defaultValue The default value
     * @return The configuration value, or the default if not found
     */
    public Object getConfigValue(String storyId, String key, Object defaultValue) {
        StoryConfig config = getConfig(storyId);
        return config.getCustomValue(key, defaultValue);
    }
    
    /**
     * Gets a string configuration value.
     */
    public String getStringValue(String storyId, String key, String defaultValue) {
        StoryConfig config = getConfig(storyId);
        return config.getCustomString(key, defaultValue);
    }
    
    /**
     * Gets an integer configuration value.
     */
    public int getIntValue(String storyId, String key, int defaultValue) {
        StoryConfig config = getConfig(storyId);
        return config.getCustomInt(key, defaultValue);
    }
    
    /**
     * Gets a boolean configuration value.
     */
    public boolean getBooleanValue(String storyId, String key, boolean defaultValue) {
        StoryConfig config = getConfig(storyId);
        return config.getCustomBoolean(key, defaultValue);
    }
    
    /**
     * Gets a double configuration value.
     */
    public double getDoubleValue(String storyId, String key, double defaultValue) {
        StoryConfig config = getConfig(storyId);
        return config.getCustomDouble(key, defaultValue);
    }

    
    // ===== Hot Reload Support (Requirement 42.4) =====
    
    /**
     * Checks for modified configurations and reloads them.
     * 
     * @return List of story IDs that were reloaded
     */
    public List<String> checkAndReloadModified() {
        List<String> reloaded = new ArrayList<>();
        
        for (Map.Entry<String, StoryConfig> entry : configCache.entrySet()) {
            String storyId = entry.getKey();
            StoryConfig config = entry.getValue();
            
            if (config.hasBeenModified()) {
                StoryConfig oldConfig = config;
                if (config.reload()) {
                    reloaded.add(storyId);
                    fireConfigChanged(storyId, oldConfig, config);
                }
            }
        }
        
        if (!reloaded.isEmpty()) {
            LOGGER.info("Reloaded {} configurations: {}", reloaded.size(), reloaded);
        }
        
        return reloaded;
    }
    
    /**
     * Reloads configuration for a specific story.
     * 
     * @param storyId The story identifier
     * @return true if reload was successful
     */
    public boolean reloadConfig(String storyId) {
        StoryConfig config = configCache.get(storyId);
        if (config != null) {
            StoryConfig oldConfig = config;
            if (config.reload()) {
                fireConfigChanged(storyId, oldConfig, config);
                return true;
            }
        } else {
            // Load fresh
            loadConfig(storyId);
            return true;
        }
        return false;
    }
    
    /**
     * Unloads configuration for a story.
     * 
     * @param storyId The story identifier
     */
    public void unloadConfig(String storyId) {
        configCache.remove(storyId);
        LOGGER.debug("Unloaded config for story: {}", storyId);
    }
    
    /**
     * Unloads all configurations.
     */
    public void unloadAll() {
        configCache.clear();
        LOGGER.info("Unloaded all configurations");
    }
    
    // ===== Validation (Requirement 42.5) =====
    
    /**
     * Validates configuration for a story.
     * 
     * @param storyId The story identifier
     * @return List of validation errors, empty if valid
     */
    public List<String> validateConfig(String storyId) {
        StoryConfig config = getConfig(storyId);
        config.validate();
        return config.getValidationErrors();
    }
    
    /**
     * Checks if configuration for a story is valid.
     * 
     * @param storyId The story identifier
     * @return true if configuration is valid
     */
    public boolean isConfigValid(String storyId) {
        StoryConfig config = getConfig(storyId);
        return config.isValid();
    }
    
    // ===== Permission Checks =====
    
    /**
     * Checks if a story is allowed to modify the world.
     */
    public boolean canModifyWorld(String storyId) {
        return getConfig(storyId).isWorldModificationAllowed();
    }
    
    /**
     * Checks if a story is allowed to modify players.
     */
    public boolean canModifyPlayer(String storyId) {
        return getConfig(storyId).isPlayerModificationAllowed();
    }
    
    /**
     * Checks if a story is allowed to register commands.
     */
    public boolean canRegisterCommands(String storyId) {
        return getConfig(storyId).isCommandRegistrationAllowed();
    }
    
    /**
     * Checks if a story has Java sections enabled.
     */
    public boolean areJavaSectionsEnabled(String storyId) {
        return getConfig(storyId).isJavaSectionsEnabled();
    }
    
    /**
     * Checks if a story has hot reload enabled.
     */
    public boolean isHotReloadEnabled(String storyId) {
        return getConfig(storyId).isHotReloadEnabled();
    }
    
    /**
     * Checks if a story is in debug mode.
     */
    public boolean isDebugMode(String storyId) {
        return getConfig(storyId).isDebugMode();
    }
    
    /**
     * Gets the max execution time for a story.
     */
    public long getMaxExecutionTime(String storyId) {
        return getConfig(storyId).getMaxExecutionTimeMs();
    }
    
    // ===== Change Listeners =====
    
    /**
     * Adds a configuration change listener.
     */
    public void addChangeListener(ConfigChangeListener listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }
    
    /**
     * Removes a configuration change listener.
     */
    public void removeChangeListener(ConfigChangeListener listener) {
        changeListeners.remove(listener);
    }
    
    private void fireConfigChanged(String storyId, StoryConfig oldConfig, StoryConfig newConfig) {
        for (ConfigChangeListener listener : changeListeners) {
            try {
                listener.onConfigChanged(storyId, oldConfig, newConfig);
            } catch (Exception e) {
                LOGGER.error("Error in config change listener", e);
            }
        }
    }
    
    // ===== Utility Methods =====
    
    /**
     * Gets all loaded story IDs.
     */
    public Set<String> getLoadedStoryIds() {
        return Collections.unmodifiableSet(configCache.keySet());
    }
    
    /**
     * Gets the number of loaded configurations.
     */
    public int getLoadedConfigCount() {
        return configCache.size();
    }
    
    /**
     * Gets a summary of all loaded configurations.
     */
    public Map<String, String> getConfigSummary() {
        Map<String, String> summary = new LinkedHashMap<>();
        for (Map.Entry<String, StoryConfig> entry : configCache.entrySet()) {
            StoryConfig config = entry.getValue();
            summary.put(entry.getKey(), config.getName() + " v" + config.getVersion());
        }
        return summary;
    }
    
    /**
     * Listener interface for configuration change events.
     */
    public interface ConfigChangeListener {
        /**
         * Called when a configuration is changed.
         */
        void onConfigChanged(String storyId, StoryConfig oldConfig, StoryConfig newConfig);
    }
}
