package ru.mjkey.storykee.runtime.packaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Central manager for story packaging and distribution operations.
 * Provides a unified interface for packaging, installing, and managing stories.
 * 
 * Requirements: 29.1, 29.2, 29.3, 29.4, 29.5
 */
public class PackageManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageManager.class);
    
    private static PackageManager instance;
    
    private final StoryPackager packager;
    private final StoryInstaller installer;
    
    // Listeners for package events
    private final List<PackageEventListener> eventListeners;
    
    private PackageManager() {
        this.packager = new StoryPackager();
        this.installer = new StoryInstaller();
        this.eventListeners = new ArrayList<>();
    }
    
    public static PackageManager getInstance() {
        if (instance == null) {
            instance = new PackageManager();
        }
        return instance;
    }
    
    public static void resetInstance() {
        instance = null;
    }
    
    // ===== Packaging Operations (Requirement 29.1) =====
    
    /**
     * Packages a story into a distributable archive.
     * 
     * @param storyId The story identifier
     * @return Result of the packaging operation
     */
    public StoryPackager.PackageResult packageStory(String storyId) {
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path outputDir = runtime.getStorykeeDirectory().resolve("packages");
            return packageStory(storyId, outputDir);
        } catch (Exception e) {
            LOGGER.error("Error packaging story {}", storyId, e);
            return new StoryPackager.PackageResult(false, null, null, 
                List.of("Packaging failed: " + e.getMessage()), null);
        }
    }
    
    /**
     * Packages a story into a distributable archive at a specific location.
     * 
     * @param storyId The story identifier
     * @param outputDirectory Directory to write the package to
     * @return Result of the packaging operation
     */
    public StoryPackager.PackageResult packageStory(String storyId, Path outputDirectory) {
        StoryPackager.PackageResult result = packager.packageStory(storyId, outputDirectory);
        
        if (result.isSuccess()) {
            firePackageCreated(storyId, result.getPackagePath(), result.getMetadata());
        }
        
        return result;
    }
    
    // ===== Installation Operations (Requirement 29.2) =====
    
    /**
     * Installs a story package.
     * 
     * @param packagePath Path to the .storypack file
     * @return Result of the installation
     */
    public StoryInstaller.InstallResult installPackage(Path packagePath) {
        StoryInstaller.InstallResult result = installer.installPackage(packagePath);
        
        if (result.isSuccess()) {
            fireStoryInstalled(result.getStoryId(), result.getMetadata());
        }
        
        return result;
    }
    
    /**
     * Reads metadata from a package without installing.
     * 
     * @param packagePath Path to the .storypack file
     * @return Package metadata, or null if reading failed
     */
    public StoryPackageMetadata readPackageMetadata(Path packagePath) {
        try {
            return installer.readMetadataFromPackage(packagePath);
        } catch (Exception e) {
            LOGGER.error("Error reading package metadata from {}", packagePath, e);
            return null;
        }
    }
    
    // ===== Uninstallation Operations (Requirement 29.5) =====
    
    /**
     * Uninstalls a story.
     * 
     * @param storyId The story identifier
     * @return Result of the uninstallation
     */
    public StoryInstaller.InstallResult uninstallStory(String storyId) {
        StoryInstaller.InstallResult result = installer.uninstallStory(storyId);
        
        if (result.isSuccess()) {
            fireStoryUninstalled(storyId);
        }
        
        return result;
    }
    
    // ===== Query Operations (Requirement 29.4) =====
    
    /**
     * Lists all installed stories.
     * 
     * @return List of installed story IDs
     */
    public List<String> listInstalledStories() {
        return installer.listInstalledStories();
    }
    
    /**
     * Gets information about an installed story.
     * 
     * @param storyId The story identifier
     * @return Story metadata, or null if not installed
     */
    public StoryPackageMetadata getInstalledStoryInfo(String storyId) {
        return installer.getInstalledStoryInfo(storyId);
    }
    
    /**
     * Checks if a story is installed.
     * 
     * @param storyId The story identifier
     * @return true if the story is installed
     */
    public boolean isStoryInstalled(String storyId) {
        return listInstalledStories().contains(storyId);
    }
    
    /**
     * Lists available package files in the packages directory.
     * 
     * @return List of available package paths
     */
    public List<Path> listAvailablePackages() {
        List<Path> packages = new ArrayList<>();
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path packagesDir = runtime.getStorykeeDirectory().resolve("packages");
            
            if (Files.exists(packagesDir)) {
                Files.list(packagesDir)
                    .filter(p -> p.toString().endsWith(StoryPackageMetadata.PACKAGE_EXTENSION))
                    .forEach(packages::add);
            }
        } catch (Exception e) {
            LOGGER.error("Error listing available packages", e);
        }
        
        return packages;
    }
    
    // ===== Dependency Validation (Requirement 29.3) =====
    
    /**
     * Validates dependencies for a package.
     * 
     * @param metadata The package metadata
     * @return List of missing dependencies
     */
    public List<String> validateDependencies(StoryPackageMetadata metadata) {
        return packager.validateDependencies(metadata);
    }
    
    /**
     * Validates dependencies for a package file.
     * 
     * @param packagePath Path to the package file
     * @return List of missing dependencies, or error messages if reading failed
     */
    public List<String> validatePackageDependencies(Path packagePath) {
        StoryPackageMetadata metadata = readPackageMetadata(packagePath);
        if (metadata == null) {
            return List.of("Could not read package metadata");
        }
        return validateDependencies(metadata);
    }
    
    // ===== Utility Methods =====
    
    /**
     * Gets a summary of all installed stories.
     * 
     * @return Map of storyId to story info
     */
    public Map<String, StoryPackageMetadata> getInstalledStorySummary() {
        Map<String, StoryPackageMetadata> summary = new LinkedHashMap<>();
        
        for (String storyId : listInstalledStories()) {
            StoryPackageMetadata info = getInstalledStoryInfo(storyId);
            if (info != null) {
                summary.put(storyId, info);
            }
        }
        
        return summary;
    }
    
    /**
     * Gets the packages directory path.
     * 
     * @return Path to the packages directory
     */
    public Path getPackagesDirectory() {
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path packagesDir = runtime.getStorykeeDirectory().resolve("packages");
            if (!Files.exists(packagesDir)) {
                Files.createDirectories(packagesDir);
            }
            return packagesDir;
        } catch (Exception e) {
            LOGGER.error("Error getting packages directory", e);
            return null;
        }
    }
    
    // ===== Event Listeners =====
    
    /**
     * Adds a package event listener.
     */
    public void addEventListener(PackageEventListener listener) {
        if (listener != null && !eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }
    
    /**
     * Removes a package event listener.
     */
    public void removeEventListener(PackageEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void firePackageCreated(String storyId, Path packagePath, StoryPackageMetadata metadata) {
        for (PackageEventListener listener : eventListeners) {
            try {
                listener.onPackageCreated(storyId, packagePath, metadata);
            } catch (Exception e) {
                LOGGER.error("Error in package event listener", e);
            }
        }
    }
    
    private void fireStoryInstalled(String storyId, StoryPackageMetadata metadata) {
        for (PackageEventListener listener : eventListeners) {
            try {
                listener.onStoryInstalled(storyId, metadata);
            } catch (Exception e) {
                LOGGER.error("Error in package event listener", e);
            }
        }
    }
    
    private void fireStoryUninstalled(String storyId) {
        for (PackageEventListener listener : eventListeners) {
            try {
                listener.onStoryUninstalled(storyId);
            } catch (Exception e) {
                LOGGER.error("Error in package event listener", e);
            }
        }
    }
    
    /**
     * Listener interface for package events.
     */
    public interface PackageEventListener {
        /**
         * Called when a package is created.
         */
        default void onPackageCreated(String storyId, Path packagePath, StoryPackageMetadata metadata) {}
        
        /**
         * Called when a story is installed.
         */
        default void onStoryInstalled(String storyId, StoryPackageMetadata metadata) {}
        
        /**
         * Called when a story is uninstalled.
         */
        default void onStoryUninstalled(String storyId) {}
    }
}
