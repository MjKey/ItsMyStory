package ru.mjkey.storykee.runtime.packaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;
import ru.mjkey.storykee.runtime.story.StoryConfig;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Creates story packages from story directories.
 * Bundles all scripts, assets, and metadata into a single archive.
 * 
 * Requirements: 29.1, 29.3
 */
public class StoryPackager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryPackager.class);
    
    private static final String SCRIPT_EXTENSION = ".skee";
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * Result of a packaging operation.
     */
    public static class PackageResult {
        private final boolean success;
        private final Path packagePath;
        private final StoryPackageMetadata metadata;
        private final List<String> errors;
        private final List<String> warnings;
        
        public PackageResult(boolean success, Path packagePath, StoryPackageMetadata metadata,
                           List<String> errors, List<String> warnings) {
            this.success = success;
            this.packagePath = packagePath;
            this.metadata = metadata;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Path getPackagePath() {
            return packagePath;
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
     * Packages a story into a distributable archive.
     * 
     * Requirement 29.1: Bundle all scripts, assets, and metadata into a single archive.
     * 
     * @param storyId The story identifier
     * @param outputDirectory Directory to write the package to
     * @return Result of the packaging operation
     */
    public PackageResult packageStory(String storyId, Path outputDirectory) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        if (storyId == null || storyId.isEmpty()) {
            errors.add("Story ID is required");
            return new PackageResult(false, null, null, errors, warnings);
        }
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path scriptsDir = runtime.getScriptsDirectory();
            Path assetsDir = runtime.getAssetsDirectory();
            
            // Determine story directory
            Path storyScriptsDir;
            Path storyAssetsDir;
            
            if ("default".equals(storyId)) {
                storyScriptsDir = scriptsDir;
                storyAssetsDir = assetsDir;
            } else {
                storyScriptsDir = scriptsDir.resolve(storyId);
                storyAssetsDir = assetsDir.resolve(storyId);
            }
            
            if (!Files.exists(storyScriptsDir)) {
                errors.add("Story scripts directory does not exist: " + storyScriptsDir);
                return new PackageResult(false, null, null, errors, warnings);
            }
            
            // Load story config
            StoryConfig config = StoryConfig.loadFromDirectory(storyScriptsDir);
            
            // Build metadata
            StoryPackageMetadata.Builder metadataBuilder = StoryPackageMetadata.builder()
                .storyId(config.getStoryId())
                .version(config.getVersion())
                .name(config.getName())
                .author(config.getAuthor())
                .description(config.getDescription())
                .minecraftVersion(config.getMinecraftVersion())
                .createdAt(System.currentTimeMillis())
                .packagedBy(System.getProperty("user.name", "unknown"));
            
            // Add dependencies (Requirement 29.3)
            for (String dep : config.getDependencies()) {
                metadataBuilder.addDependency(parseDependency(dep));
            }
            
            // Collect files to package
            List<FileEntry> filesToPackage = new ArrayList<>();
            
            // Collect scripts
            collectScripts(storyScriptsDir, storyScriptsDir, filesToPackage, metadataBuilder, warnings);
            
            // Collect assets if they exist
            if (Files.exists(storyAssetsDir)) {
                collectAssets(storyAssetsDir, storyAssetsDir, filesToPackage, metadataBuilder, warnings);
            }
            
            // Collect config.json if it exists
            Path configPath = storyScriptsDir.resolve(StoryConfig.CONFIG_FILE_NAME);
            if (Files.exists(configPath)) {
                filesToPackage.add(new FileEntry(configPath, StoryConfig.CONFIG_FILE_NAME, FileType.CONFIG));
            }
            
            StoryPackageMetadata metadata = metadataBuilder.build();
            
            // Validate metadata
            List<String> validationErrors = metadata.validate();
            if (!validationErrors.isEmpty()) {
                errors.addAll(validationErrors);
                return new PackageResult(false, null, metadata, errors, warnings);
            }
            
            // Create output directory if needed
            if (!Files.exists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
            }
            
            // Generate package filename
            String packageFileName = storyId + "-" + config.getVersion() + 
                StoryPackageMetadata.PACKAGE_EXTENSION;
            Path packagePath = outputDirectory.resolve(packageFileName);
            
            // Create the archive
            createArchive(packagePath, filesToPackage, metadata);
            
            LOGGER.info("Successfully packaged story {} to {}", storyId, packagePath);
            return new PackageResult(true, packagePath, metadata, errors, warnings);
            
        } catch (Exception e) {
            LOGGER.error("Error packaging story {}", storyId, e);
            errors.add("Packaging failed: " + e.getMessage());
            return new PackageResult(false, null, null, errors, warnings);
        }
    }
    
    /**
     * Collects script files from a directory.
     */
    private void collectScripts(Path baseDir, Path currentDir, List<FileEntry> files,
                               StoryPackageMetadata.Builder metadataBuilder, List<String> warnings) 
            throws IOException {
        
        if (!Files.exists(currentDir)) {
            return;
        }
        
        Files.walkFileTree(currentDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString().toLowerCase();
                
                if (fileName.endsWith(SCRIPT_EXTENSION)) {
                    String relativePath = baseDir.relativize(file).toString().replace('\\', '/');
                    files.add(new FileEntry(file, "scripts/" + relativePath, FileType.SCRIPT));
                    metadataBuilder.addScript(relativePath);
                    
                    // Calculate checksum
                    try {
                        String checksum = calculateChecksum(file);
                        metadataBuilder.addChecksum("scripts/" + relativePath, checksum);
                    } catch (Exception e) {
                        warnings.add("Could not calculate checksum for: " + relativePath);
                    }
                }
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                warnings.add("Could not access file: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Collects asset files from a directory.
     */
    private void collectAssets(Path baseDir, Path currentDir, List<FileEntry> files,
                              StoryPackageMetadata.Builder metadataBuilder, List<String> warnings) 
            throws IOException {
        
        if (!Files.exists(currentDir)) {
            return;
        }
        
        Files.walkFileTree(currentDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relativePath = baseDir.relativize(file).toString().replace('\\', '/');
                files.add(new FileEntry(file, "assets/" + relativePath, FileType.ASSET));
                metadataBuilder.addAsset(relativePath);
                
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                warnings.add("Could not access asset: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * Creates the ZIP archive containing all story files.
     */
    private void createArchive(Path packagePath, List<FileEntry> files, 
                              StoryPackageMetadata metadata) throws IOException {
        
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(packagePath)))) {
            
            // Write metadata first
            ZipEntry metadataEntry = new ZipEntry(StoryPackageMetadata.METADATA_FILE_NAME);
            zos.putNextEntry(metadataEntry);
            zos.write(metadata.toJson().getBytes("UTF-8"));
            zos.closeEntry();
            
            // Write all files
            byte[] buffer = new byte[BUFFER_SIZE];
            
            for (FileEntry entry : files) {
                ZipEntry zipEntry = new ZipEntry(entry.archivePath);
                zos.putNextEntry(zipEntry);
                
                try (InputStream is = new BufferedInputStream(Files.newInputStream(entry.sourcePath))) {
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                
                zos.closeEntry();
            }
        }
        
        LOGGER.debug("Created archive with {} files", files.size() + 1);
    }
    
    /**
     * Calculates SHA-256 checksum for a file.
     */
    private String calculateChecksum(Path file) throws IOException, NoSuchAlgorithmException {
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
    
    /**
     * Parses a dependency string into a StoryDependency object.
     * Format: "storyId@version" or "storyId@version?" for optional
     */
    private StoryPackageMetadata.StoryDependency parseDependency(String depString) {
        boolean optional = depString.endsWith("?");
        if (optional) {
            depString = depString.substring(0, depString.length() - 1);
        }
        
        String[] parts = depString.split("@");
        String storyId = parts[0];
        String minVersion = parts.length > 1 ? parts[1] : "1.0.0";
        
        return new StoryPackageMetadata.StoryDependency(storyId, minVersion, optional);
    }
    
    /**
     * Validates that all dependencies are available.
     * 
     * Requirement 29.3: Validate that required resources are available.
     * 
     * @param metadata The package metadata to validate
     * @return List of missing dependencies
     */
    public List<String> validateDependencies(StoryPackageMetadata metadata) {
        List<String> missing = new ArrayList<>();
        
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path scriptsDir = runtime.getScriptsDirectory();
            
            for (StoryPackageMetadata.StoryDependency dep : metadata.getDependencies()) {
                if (dep.isOptional()) {
                    continue;
                }
                
                Path depDir = scriptsDir.resolve(dep.getStoryId());
                if (!Files.exists(depDir)) {
                    missing.add(dep.toString());
                } else {
                    // Check version if config exists
                    StoryConfig depConfig = StoryConfig.loadFromDirectory(depDir);
                    if (!isVersionCompatible(depConfig.getVersion(), dep.getMinVersion())) {
                        missing.add(dep.getStoryId() + " (requires " + dep.getMinVersion() + 
                            ", found " + depConfig.getVersion() + ")");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error validating dependencies", e);
            missing.add("Error checking dependencies: " + e.getMessage());
        }
        
        return missing;
    }
    
    /**
     * Checks if an installed version is compatible with a required minimum version.
     * Uses simple semver comparison.
     */
    private boolean isVersionCompatible(String installed, String required) {
        if (installed == null || required == null) {
            return true;
        }
        
        try {
            int[] installedParts = parseVersion(installed);
            int[] requiredParts = parseVersion(required);
            
            for (int i = 0; i < 3; i++) {
                if (installedParts[i] > requiredParts[i]) {
                    return true;
                }
                if (installedParts[i] < requiredParts[i]) {
                    return false;
                }
            }
            return true; // Equal versions
            
        } catch (Exception e) {
            return true; // If parsing fails, assume compatible
        }
    }
    
    private int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] result = new int[3];
        for (int i = 0; i < Math.min(parts.length, 3); i++) {
            try {
                // Remove any suffix like "-beta"
                String numPart = parts[i].split("-")[0];
                result[i] = Integer.parseInt(numPart);
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
    
    /**
     * Represents a file to be included in the package.
     */
    private static class FileEntry {
        final Path sourcePath;
        final String archivePath;
        final FileType type;
        
        FileEntry(Path sourcePath, String archivePath, FileType type) {
            this.sourcePath = sourcePath;
            this.archivePath = archivePath;
            this.type = type;
        }
    }
    
    private enum FileType {
        SCRIPT,
        ASSET,
        CONFIG,
        DATA
    }
}
