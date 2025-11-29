package ru.mjkey.storykee.runtime.story;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents the configuration for a single story.
 * Loaded from config.json in the story directory.
 * 
 * Requirements: 42.1, 42.2, 42.3, 42.4, 42.5
 */
public class StoryConfig {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryConfig.class);
    
    public static final String CONFIG_FILE_NAME = "config.json";
    
    // Core metadata
    private String storyId;
    private String version;
    private String name;
    private String author;
    private String description;
    private String minecraftVersion;
    
    // Dependencies
    private List<String> dependencies;
    
    // Settings
    private boolean hotReload;
    private boolean debugMode;
    private long maxExecutionTimeMs;
    private boolean enableJavaSections;
    
    // Permissions
    private boolean worldModification;
    private boolean playerModification;
    private boolean commandRegistration;
    
    // Custom configuration values
    private Map<String, Object> customConfig;
    
    // Validation errors
    private List<String> validationErrors;
    
    // Source path
    private Path configPath;
    private long lastModified;
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    /**
     * Creates a new StoryConfig with default values.
     */
    public StoryConfig() {
        // Set defaults (Requirement 42.3)
        this.storyId = "unknown";
        this.version = "1.0.0";
        this.name = "Unnamed Story";
        this.author = "Unknown";
        this.description = "";
        this.minecraftVersion = "1.21.10";
        this.dependencies = new ArrayList<>();
        this.hotReload = true;
        this.debugMode = false;
        this.maxExecutionTimeMs = 5000;
        this.enableJavaSections = true;
        this.worldModification = true;
        this.playerModification = true;
        this.commandRegistration = true;
        this.customConfig = new HashMap<>();
        this.validationErrors = new ArrayList<>();
    }

    
    // ===== Loading (Requirement 42.1) =====
    
    /**
     * Loads configuration from a config.json file.
     * 
     * @param configPath Path to the config.json file
     * @return The loaded configuration, or a default config if loading fails
     */
    public static StoryConfig load(Path configPath) {
        StoryConfig config = new StoryConfig();
        config.configPath = configPath;
        
        if (configPath == null || !Files.exists(configPath)) {
            LOGGER.debug("Config file not found, using defaults: {}", configPath);
            return config;
        }
        
        try {
            String content = Files.readString(configPath);
            config.lastModified = Files.getLastModifiedTime(configPath).toMillis();
            
            JsonObject json = GSON.fromJson(content, JsonObject.class);
            if (json == null) {
                config.validationErrors.add("Config file is empty or invalid JSON");
                return config;
            }
            
            // Parse core metadata
            config.storyId = getStringOrDefault(json, "storyId", config.storyId);
            config.version = getStringOrDefault(json, "version", config.version);
            config.name = getStringOrDefault(json, "name", config.name);
            config.author = getStringOrDefault(json, "author", config.author);
            config.description = getStringOrDefault(json, "description", config.description);
            config.minecraftVersion = getStringOrDefault(json, "minecraftVersion", config.minecraftVersion);
            
            // Parse dependencies
            if (json.has("dependencies") && json.get("dependencies").isJsonArray()) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                config.dependencies = GSON.fromJson(json.get("dependencies"), listType);
            }
            
            // Parse settings
            if (json.has("settings") && json.get("settings").isJsonObject()) {
                JsonObject settings = json.getAsJsonObject("settings");
                config.hotReload = getBooleanOrDefault(settings, "hotReload", config.hotReload);
                config.debugMode = getBooleanOrDefault(settings, "debugMode", config.debugMode);
                config.maxExecutionTimeMs = getLongOrDefault(settings, "maxExecutionTimeMs", config.maxExecutionTimeMs);
                config.enableJavaSections = getBooleanOrDefault(settings, "enableJavaSections", config.enableJavaSections);
            }
            
            // Parse permissions
            if (json.has("permissions") && json.get("permissions").isJsonObject()) {
                JsonObject permissions = json.getAsJsonObject("permissions");
                config.worldModification = getBooleanOrDefault(permissions, "worldModification", config.worldModification);
                config.playerModification = getBooleanOrDefault(permissions, "playerModification", config.playerModification);
                config.commandRegistration = getBooleanOrDefault(permissions, "commandRegistration", config.commandRegistration);
            }
            
            // Parse custom config
            if (json.has("custom") && json.get("custom").isJsonObject()) {
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                config.customConfig = GSON.fromJson(json.get("custom"), mapType);
            }
            
            // Validate the configuration (Requirement 42.5)
            config.validate();
            
            LOGGER.info("Loaded story config from {}: {} v{}", configPath, config.name, config.version);
            
        } catch (Exception e) {
            LOGGER.error("Error loading config from {}: {}", configPath, e.getMessage());
            config.validationErrors.add("Failed to parse config: " + e.getMessage());
        }
        
        return config;
    }
    
    /**
     * Loads configuration from a story directory.
     * 
     * @param storyDirectory Path to the story directory
     * @return The loaded configuration
     */
    public static StoryConfig loadFromDirectory(Path storyDirectory) {
        Path configPath = storyDirectory.resolve(CONFIG_FILE_NAME);
        StoryConfig config = load(configPath);
        
        // If storyId wasn't set in config, derive it from directory name
        if ("unknown".equals(config.storyId)) {
            config.storyId = storyDirectory.getFileName().toString();
        }
        
        return config;
    }
    
    // ===== Validation (Requirement 42.5) =====
    
    /**
     * Validates the configuration against the schema.
     */
    public void validate() {
        validationErrors.clear();
        
        // Validate required fields
        if (storyId == null || storyId.isEmpty() || "unknown".equals(storyId)) {
            validationErrors.add("storyId is required");
        }
        
        if (version == null || version.isEmpty()) {
            validationErrors.add("version is required");
        } else if (!isValidVersion(version)) {
            validationErrors.add("version must be in semver format (e.g., 1.0.0)");
        }
        
        if (name == null || name.isEmpty()) {
            validationErrors.add("name is required");
        }
        
        // Validate settings
        if (maxExecutionTimeMs < 0) {
            validationErrors.add("maxExecutionTimeMs must be non-negative");
            maxExecutionTimeMs = 5000; // Reset to default
        }
        
        // Log validation errors (Requirement 42.3)
        if (!validationErrors.isEmpty()) {
            LOGGER.warn("Config validation warnings for {}: {}", storyId, validationErrors);
        }
    }
    
    private boolean isValidVersion(String version) {
        // Simple semver validation
        return version.matches("\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?");
    }
    
    /**
     * Checks if the configuration is valid.
     */
    public boolean isValid() {
        return validationErrors.isEmpty();
    }
    
    /**
     * Gets validation errors.
     */
    public List<String> getValidationErrors() {
        return Collections.unmodifiableList(validationErrors);
    }

    
    // ===== Hot Reload Support (Requirement 42.4) =====
    
    /**
     * Checks if the config file has been modified since last load.
     */
    public boolean hasBeenModified() {
        if (configPath == null || !Files.exists(configPath)) {
            return false;
        }
        try {
            long currentModified = Files.getLastModifiedTime(configPath).toMillis();
            return currentModified > lastModified;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Reloads the configuration from disk.
     * 
     * @return true if reload was successful
     */
    public boolean reload() {
        if (configPath == null) {
            return false;
        }
        
        StoryConfig reloaded = load(configPath);
        
        // Copy values from reloaded config
        this.storyId = reloaded.storyId;
        this.version = reloaded.version;
        this.name = reloaded.name;
        this.author = reloaded.author;
        this.description = reloaded.description;
        this.minecraftVersion = reloaded.minecraftVersion;
        this.dependencies = reloaded.dependencies;
        this.hotReload = reloaded.hotReload;
        this.debugMode = reloaded.debugMode;
        this.maxExecutionTimeMs = reloaded.maxExecutionTimeMs;
        this.enableJavaSections = reloaded.enableJavaSections;
        this.worldModification = reloaded.worldModification;
        this.playerModification = reloaded.playerModification;
        this.commandRegistration = reloaded.commandRegistration;
        this.customConfig = reloaded.customConfig;
        this.validationErrors = reloaded.validationErrors;
        this.lastModified = reloaded.lastModified;
        
        LOGGER.info("Reloaded config for story: {}", storyId);
        return true;
    }
    
    // ===== Custom Config Access (Requirement 42.2) =====
    
    /**
     * Gets a custom configuration value.
     */
    public Object getCustomValue(String key) {
        return customConfig.get(key);
    }
    
    /**
     * Gets a custom configuration value with a default.
     */
    public Object getCustomValue(String key, Object defaultValue) {
        return customConfig.getOrDefault(key, defaultValue);
    }
    
    /**
     * Gets a custom string value.
     */
    public String getCustomString(String key, String defaultValue) {
        Object value = customConfig.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }
    
    /**
     * Gets a custom integer value.
     */
    public int getCustomInt(String key, int defaultValue) {
        Object value = customConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    /**
     * Gets a custom boolean value.
     */
    public boolean getCustomBoolean(String key, boolean defaultValue) {
        Object value = customConfig.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * Gets a custom double value.
     */
    public double getCustomDouble(String key, double defaultValue) {
        Object value = customConfig.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    /**
     * Gets all custom configuration values.
     */
    public Map<String, Object> getAllCustomValues() {
        return Collections.unmodifiableMap(customConfig);
    }
    
    /**
     * Checks if a custom configuration key exists.
     */
    public boolean hasCustomValue(String key) {
        return customConfig.containsKey(key);
    }
    
    // ===== Getters =====
    
    public String getStoryId() {
        return storyId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getName() {
        return name;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    public List<String> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }
    
    public boolean isHotReloadEnabled() {
        return hotReload;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs;
    }
    
    public boolean isJavaSectionsEnabled() {
        return enableJavaSections;
    }
    
    public boolean isWorldModificationAllowed() {
        return worldModification;
    }
    
    public boolean isPlayerModificationAllowed() {
        return playerModification;
    }
    
    public boolean isCommandRegistrationAllowed() {
        return commandRegistration;
    }
    
    public Path getConfigPath() {
        return configPath;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    // ===== Helper Methods =====
    
    private static String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return defaultValue;
    }
    
    private static boolean getBooleanOrDefault(JsonObject json, String key, boolean defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsBoolean();
        }
        return defaultValue;
    }
    
    private static long getLongOrDefault(JsonObject json, String key, long defaultValue) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsLong();
        }
        return defaultValue;
    }
    
    /**
     * Saves the configuration to disk.
     */
    public void save() throws IOException {
        if (configPath == null) {
            throw new IOException("Config path is not set");
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("storyId", storyId);
        json.addProperty("version", version);
        json.addProperty("name", name);
        json.addProperty("author", author);
        json.addProperty("description", description);
        json.addProperty("minecraftVersion", minecraftVersion);
        json.add("dependencies", GSON.toJsonTree(dependencies));
        
        JsonObject settings = new JsonObject();
        settings.addProperty("hotReload", hotReload);
        settings.addProperty("debugMode", debugMode);
        settings.addProperty("maxExecutionTimeMs", maxExecutionTimeMs);
        settings.addProperty("enableJavaSections", enableJavaSections);
        json.add("settings", settings);
        
        JsonObject permissions = new JsonObject();
        permissions.addProperty("worldModification", worldModification);
        permissions.addProperty("playerModification", playerModification);
        permissions.addProperty("commandRegistration", commandRegistration);
        json.add("permissions", permissions);
        
        if (!customConfig.isEmpty()) {
            json.add("custom", GSON.toJsonTree(customConfig));
        }
        
        String content = GSON.toJson(json);
        Files.writeString(configPath, content);
        lastModified = Files.getLastModifiedTime(configPath).toMillis();
        
        LOGGER.info("Saved config for story: {}", storyId);
    }
    
    @Override
    public String toString() {
        return "StoryConfig{" +
            "storyId='" + storyId + '\'' +
            ", version='" + version + '\'' +
            ", name='" + name + '\'' +
            ", author='" + author + '\'' +
            ", valid=" + isValid() +
            '}';
    }
}
