package ru.mjkey.storykee.runtime.packaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;
import ru.mjkey.storykee.runtime.story.StoryManager;

import ru.mjkey.storykee.runtime.story.StoryConfig;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs and uninstalls story packages.
 * Handles extraction, validation, and multi-story management.
 * 
 * Requirements: 29.2, 29.4, 29.5
 */
public class StoryInstaller {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryInstaller.class);
    
    private static final int BUFFER_SIZE = 8192;
    
    private final StoryPackager packager;
    
    public StoryInstaller() {
        this.packager = new StoryPackager();
    }
    
    /**
     * Result of an installation operation.
     */
    public static class InstallResult {
        private final boolean success;
        private final String storyId;
        private final StoryPackageMetadata metadata;
        private final List<String> errors;
        private final List<String> warnings;
        
        public InstallResult(boolean success, String storyId, StoryPackageMetadata metadata,
                           List<String> errors, List<String> warnings) {
            this.success = success;
            this.storyId = storyId;
            this.metadata = metadata;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getStoryId() {
            return storyId;
        }
        
        public StoryPackageMetadata getMetadata() {
            return metadata;
        }
        
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }
        
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }
    }
    
    /**
     * Installs a story package.
     * 
     * Requirement 29.2: Extract package to appropriate directories.
     * Requirement 29.4: Keep stories isolated in separate subdirectories.
     * 
     * @param packagePath Path to the .storypack file
     * @return Result of the installation
     */
    public InstallResult installPackage(Path packagePath) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (packagePath == null || !Files.exists(packagePath)) {
            errors.add("Package file does not exist: " + packagePath);
            return new InstallResult(false, null, null, errors, warnings);
        }
        
        try {
            // Read metadata from package
            StoryPackageMetadata metadata = readMetadataFromPackage(packagePath);
            if (metadata == null) {
                errors.add("Could not read package metadata");
                return new InstallResult(false, null, null, errors, warnings);
            }
            
            String storyId = metadata.getStoryId();
            
            // Validate metadata
            List<String> validationErrors = metadata.validate();
            if (!validationErrors.isEmpty()) {
                errors.addAll(validationErrors);
                return new InstallResult(false, storyId, metadata, errors, warnings);
            }
            
            // Check dependencies (Requirement 29.3)
            List<String> missingDeps = packager.validateDependencies(metadata);
            if (!missingDeps.isEmpty()) {
                for (String dep : missingDeps) {
                    errors.add("Missing dependency: " + dep);
                }
                return new InstallResult(false, storyId, metadata, errors, warnings);
            }
            
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            
            // Determine installation directories (Requirement 29.4)
            Path scriptsDir = runtime.getScriptsDirectory().resolve(storyId);
            Path assetsDir = runtime.getAssetsDirectory().resolve(storyId);
            
            // Check if story already exists
            if (Files.exists(scriptsDir)) {
                warnings.add("Story already exists, will be overwritten: " + storyId);
                // Unload the story first if it's loaded
                StoryManager.getInstance().unloadStory(storyId);
            }
            
            // Create directories
            Files.createDirectories(scriptsDir);
            Files.createDirectories(assetsDir);
            
            // Extract package
            extractPackage(packagePath, scriptsDir, assetsDir, metadata, warnings);
            
            // Validate extracted files
            validateExtractedFiles(scriptsDir, assetsDir, metadata, warnings);
            
            LOGGER.info("Successfully installed story {} v{}", storyId, metadata.getVersion());
            return new InstallResult(true, storyId, metadata, errors, warnings);
            
        } catch (Exception e) {
            LOGGER.error("Error installing package {}", packagePath, e);
            errors.add("Installation failed: " + e.getMessage());
            return new InstallResult(false, null, null, errors, warnings);
        }
    }
    
    /**
     * Reads metadata from a package file without extracting.
     */
    public StoryPackageMetadata readMetadataFromPackage(Path packagePath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(packagePath)))) {
            
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (StoryPackageMetadata.METADATA_FILE_NAME.equals(entry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String json = baos.toString("UTF-8");
                    return StoryPackageMetadata.fromJson(json);
                }
                zis.closeEntry();
            }
        }
        
        return null;
    }
    
    /**
     * Extracts package contents to the appropriate directories.
     */
    private void extractPackage(Path packagePath, Path scriptsDir, Path assetsDir,
                               StoryPackageMetadata metadata, List<String> warnings) 
            throws IOException {
        
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(packagePath)))) {
            
            ZipEntry entry;
            byte[] buffer = new byte[BUFFER_SIZE];
            
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                
                // Skip metadata file
                if (StoryPackageMetadata.METADATA_FILE_NAME.equals(entryName)) {
                    zis.closeEntry();
                    continue;
                }
                
                // Determine target path
                Path targetPath = null;
                
                if (entryName.startsWith("scripts/")) {
                    String relativePath = entryName.substring("scripts/".length());
                    targetPath = scriptsDir.resolve(relativePath);
                } else if (entryName.startsWith("assets/")) {
                    String relativePath = entryName.substring("assets/".length());
                    targetPath = assetsDir.resolve(relativePath);
                } else if (entryName.equals(StoryConfig.CONFIG_FILE_NAME)) {
                    targetPath = scriptsDir.resolve(entryName);
                }
                
                if (targetPath != null) {
                    // Security check: ensure path doesn't escape target directory
                    if (!isPathSafe(targetPath, scriptsDir) && !isPathSafe(targetPath, assetsDir)) {
                        warnings.add("Skipping potentially unsafe path: " + entryName);
                        zis.closeEntry();
                        continue;
                    }
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        // Create parent directories
                        Files.createDirectories(targetPath.getParent());
                        
                        // Extract file
                        try (OutputStream os = new BufferedOutputStream(
                                Files.newOutputStream(targetPath))) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                        }
                    }
                }
                
                zis.closeEntry();
            }
        }
    }
    
    /**
     * Validates that extracted files match the checksums in metadata.
     */
    private void validateExtractedFiles(Path scriptsDir, Path assetsDir,
                                       StoryPackageMetadata metadata, List<String> warnings) {
        
        Map<String, String> checksums = metadata.getChecksums();
        
        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            String filePath = entry.getKey();
            String expectedChecksum = entry.getValue();
            
            Path actualPath;
            if (filePath.startsWith("scripts/")) {
                actualPath = scriptsDir.resolve(filePath.substring("scripts/".length()));
            } else if (filePath.startsWith("assets/")) {
                actualPath = assetsDir.resolve(filePath.substring("assets/".length()));
            } else {
                continue;
            }
            
            if (!Files.exists(actualPath)) {
                warnings.add("Expected file not found: " + filePath);
                continue;
            }
            
            try {
                String actualChecksum = calculateChecksum(actualPath);
                if (!expectedChecksum.equals(actualChecksum)) {
                    warnings.add("Checksum mismatch for: " + filePath);
                }
            } catch (Exception e) {
                warnings.add("Could not verify checksum for: " + filePath);
            }
        }
    }
    
    /**
     * Checks if a path is safely within a base directory.
     */
    private boolean isPathSafe(Path path, Path baseDir) {
        try {
            Path normalizedPath = path.normalize().toAbsolutePath();
            Path normalizedBase = baseDir.normalize().toAbsolutePath();
            return normalizedPath.startsWith(normalizedBase);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Uninstalls a story.
     * 
     * Requirement 29.5: Remove all associated files and clean up data.
     * 
     * @param storyId The story identifier to uninstall
     * @return Result of the uninstallation
     */
    public InstallResult uninstallStory(String storyId) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (storyId == null || storyId.isEmpty()) {
            errors.add("Story ID is required");
            return new InstallResult(false, null, null, errors, warnings);
        }
        
        if ("default".equals(storyId)) {
            errors.add("Cannot uninstall the default story");
            return new InstallResult(false, storyId, null, errors, warnings);
        }
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            
            // Unload the story first
            StoryManager.getInstance().unloadStory(storyId);
            
            // Determine directories to remove
            Path scriptsDir = runtime.getScriptsDirectory().resolve(storyId);
            Path assetsDir = runtime.getAssetsDirectory().resolve(storyId);
            Path dataDir = runtime.getDataDirectory().resolve(storyId);
            
            int filesRemoved = 0;
            
            // Remove scripts directory
            if (Files.exists(scriptsDir)) {
                filesRemoved += deleteDirectory(scriptsDir, warnings);
            }
            
            // Remove assets directory
            if (Files.exists(assetsDir)) {
                filesRemoved += deleteDirectory(assetsDir, warnings);
            }
            
            // Remove data directory (player data for this story)
            if (Files.exists(dataDir)) {
                filesRemoved += deleteDirectory(dataDir, warnings);
            }
            
            LOGGER.info("Uninstalled story {}, removed {} files", storyId, filesRemoved);
            return new InstallResult(true, storyId, null, errors, warnings);
            
        } catch (Exception e) {
            LOGGER.error("Error uninstalling story {}", storyId, e);
            errors.add("Uninstallation failed: " + e.getMessage());
            return new InstallResult(false, storyId, null, errors, warnings);
        }
    }
    
    /**
     * Recursively deletes a directory and its contents.
     * 
     * @return Number of files deleted
     */
    private int deleteDirectory(Path directory, List<String> warnings) throws IOException {
        final int[] count = {0};
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    Files.delete(file);
                    count[0]++;
                } catch (IOException e) {
                    warnings.add("Could not delete file: " + file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                try {
                    Files.delete(dir);
                } catch (IOException e) {
                    warnings.add("Could not delete directory: " + dir);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                warnings.add("Could not access: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
        
        return count[0];
    }
    
    /**
     * Lists all installed stories.
     * 
     * Requirement 29.4: Multiple stories in separate subdirectories.
     * 
     * @return List of installed story IDs
     */
    public List<String> listInstalledStories() {
        List<String> stories = new ArrayList<>();
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path scriptsDir = runtime.getScriptsDirectory();
            
            if (!Files.exists(scriptsDir)) {
                return stories;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDir)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        // Check if it contains .skee files
                        if (containsScriptFiles(entry)) {
                            stories.add(entry.getFileName().toString());
                        }
                    }
                }
            }
            
            // Check for default story (scripts directly in scripts dir)
            if (containsScriptFiles(scriptsDir)) {
                stories.add("default");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error listing installed stories", e);
        }
        
        return stories;
    }
    
    private boolean containsScriptFiles(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.skee")) {
            return stream.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Gets information about an installed story.
     * 
     * @param storyId The story identifier
     * @return Story metadata, or null if not installed
     */
    public StoryPackageMetadata getInstalledStoryInfo(String storyId) {
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
                return null;
            }
            
            // Try to read package.json first
            Path packageJsonPath = storyDir.resolve(StoryPackageMetadata.METADATA_FILE_NAME);
            if (Files.exists(packageJsonPath)) {
                String json = Files.readString(packageJsonPath);
                return StoryPackageMetadata.fromJson(json);
            }
            
            // Fall back to config.json
            Path configPath = storyDir.resolve(StoryConfig.CONFIG_FILE_NAME);
            if (Files.exists(configPath)) {
                StoryConfig config = StoryConfig.loadFromDirectory(storyDir);
                
                return StoryPackageMetadata.builder()
                    .storyId(config.getStoryId())
                    .version(config.getVersion())
                    .name(config.getName())
                    .author(config.getAuthor())
                    .description(config.getDescription())
                    .minecraftVersion(config.getMinecraftVersion())
                    .build();
            }
            
            // Minimal metadata from directory name
            return StoryPackageMetadata.builder()
                .storyId(storyId)
                .name(storyId)
                .version("unknown")
                .build();
            
        } catch (Exception e) {
            LOGGER.error("Error getting story info for {}", storyId, e);
            return null;
        }
    }
    
    /**
     * Calculates SHA-256 checksum for a file.
     */
    private String calculateChecksum(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        
        try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = is.read(buffer)) > 0) {
                digest.update(buffer, 0, len);
            }
        }
        
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
