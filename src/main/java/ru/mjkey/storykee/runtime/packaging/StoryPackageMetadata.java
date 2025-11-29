package ru.mjkey.storykee.runtime.packaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

/**
 * Metadata for a story package.
 * Contains information about the story, its version, author, and dependencies.
 * 
 * Requirements: 29.1, 29.3
 */
public class StoryPackageMetadata {
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    public static final String METADATA_FILE_NAME = "package.json";
    public static final String PACKAGE_EXTENSION = ".storypack";
    
    // Core metadata
    private String storyId;
    private String version;
    private String name;
    private String author;
    private String description;
    private String minecraftVersion;
    
    // Package info
    private long createdAt;
    private String packagedBy;
    private String packageVersion;
    
    // Dependencies (Requirement 29.3)
    private List<StoryDependency> dependencies;
    
    // File manifest
    private List<String> scripts;
    private List<String> assets;
    private List<String> dataFiles;
    
    // Checksums for validation
    private Map<String, String> checksums;
    
    public StoryPackageMetadata() {
        this.dependencies = new ArrayList<>();
        this.scripts = new ArrayList<>();
        this.assets = new ArrayList<>();
        this.dataFiles = new ArrayList<>();
        this.checksums = new HashMap<>();
        this.packageVersion = "1.0";
    }
    
    // ===== Builder Pattern =====
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final StoryPackageMetadata metadata;
        
        public Builder() {
            this.metadata = new StoryPackageMetadata();
        }
        
        public Builder storyId(String storyId) {
            metadata.storyId = storyId;
            return this;
        }
        
        public Builder version(String version) {
            metadata.version = version;
            return this;
        }
        
        public Builder name(String name) {
            metadata.name = name;
            return this;
        }
        
        public Builder author(String author) {
            metadata.author = author;
            return this;
        }
        
        public Builder description(String description) {
            metadata.description = description;
            return this;
        }
        
        public Builder minecraftVersion(String minecraftVersion) {
            metadata.minecraftVersion = minecraftVersion;
            return this;
        }
        
        public Builder createdAt(long createdAt) {
            metadata.createdAt = createdAt;
            return this;
        }
        
        public Builder packagedBy(String packagedBy) {
            metadata.packagedBy = packagedBy;
            return this;
        }
        
        public Builder addDependency(StoryDependency dependency) {
            metadata.dependencies.add(dependency);
            return this;
        }
        
        public Builder addScript(String scriptPath) {
            metadata.scripts.add(scriptPath);
            return this;
        }
        
        public Builder addAsset(String assetPath) {
            metadata.assets.add(assetPath);
            return this;
        }
        
        public Builder addDataFile(String dataPath) {
            metadata.dataFiles.add(dataPath);
            return this;
        }
        
        public Builder addChecksum(String filePath, String checksum) {
            metadata.checksums.put(filePath, checksum);
            return this;
        }
        
        public StoryPackageMetadata build() {
            return metadata;
        }
    }
    
    // ===== Serialization =====
    
    public String toJson() {
        return GSON.toJson(this);
    }
    
    public static StoryPackageMetadata fromJson(String json) {
        return GSON.fromJson(json, StoryPackageMetadata.class);
    }
    
    // ===== Validation =====
    
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (storyId == null || storyId.isEmpty()) {
            errors.add("storyId is required");
        }
        
        if (version == null || version.isEmpty()) {
            errors.add("version is required");
        }
        
        if (name == null || name.isEmpty()) {
            errors.add("name is required");
        }
        
        if (scripts.isEmpty()) {
            errors.add("Package must contain at least one script");
        }
        
        return errors;
    }
    
    public boolean isValid() {
        return validate().isEmpty();
    }
    
    // ===== Getters and Setters =====
    
    public String getStoryId() {
        return storyId;
    }
    
    public void setStoryId(String storyId) {
        this.storyId = storyId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getMinecraftVersion() {
        return minecraftVersion;
    }
    
    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getPackagedBy() {
        return packagedBy;
    }
    
    public void setPackagedBy(String packagedBy) {
        this.packagedBy = packagedBy;
    }
    
    public String getPackageVersion() {
        return packageVersion;
    }
    
    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }
    
    public List<StoryDependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }
    
    public void setDependencies(List<StoryDependency> dependencies) {
        this.dependencies = new ArrayList<>(dependencies);
    }
    
    public List<String> getScripts() {
        return Collections.unmodifiableList(scripts);
    }
    
    public void setScripts(List<String> scripts) {
        this.scripts = new ArrayList<>(scripts);
    }
    
    public List<String> getAssets() {
        return Collections.unmodifiableList(assets);
    }
    
    public void setAssets(List<String> assets) {
        this.assets = new ArrayList<>(assets);
    }
    
    public List<String> getDataFiles() {
        return Collections.unmodifiableList(dataFiles);
    }
    
    public void setDataFiles(List<String> dataFiles) {
        this.dataFiles = new ArrayList<>(dataFiles);
    }
    
    public Map<String, String> getChecksums() {
        return Collections.unmodifiableMap(checksums);
    }
    
    public void setChecksums(Map<String, String> checksums) {
        this.checksums = new HashMap<>(checksums);
    }
    
    public int getTotalFileCount() {
        return scripts.size() + assets.size() + dataFiles.size();
    }
    
    @Override
    public String toString() {
        return "StoryPackageMetadata{" +
            "storyId='" + storyId + '\'' +
            ", version='" + version + '\'' +
            ", name='" + name + '\'' +
            ", author='" + author + '\'' +
            ", files=" + getTotalFileCount() +
            ", dependencies=" + dependencies.size() +
            '}';
    }
    
    /**
     * Represents a story dependency.
     */
    public static class StoryDependency {
        private String storyId;
        private String minVersion;
        private boolean optional;
        
        public StoryDependency() {}
        
        public StoryDependency(String storyId, String minVersion, boolean optional) {
            this.storyId = storyId;
            this.minVersion = minVersion;
            this.optional = optional;
        }
        
        public String getStoryId() {
            return storyId;
        }
        
        public void setStoryId(String storyId) {
            this.storyId = storyId;
        }
        
        public String getMinVersion() {
            return minVersion;
        }
        
        public void setMinVersion(String minVersion) {
            this.minVersion = minVersion;
        }
        
        public boolean isOptional() {
            return optional;
        }
        
        public void setOptional(boolean optional) {
            this.optional = optional;
        }
        
        @Override
        public String toString() {
            return storyId + "@" + minVersion + (optional ? " (optional)" : "");
        }
    }
}
