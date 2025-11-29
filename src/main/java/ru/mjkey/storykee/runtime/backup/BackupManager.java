package ru.mjkey.storykee.runtime.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages backup and recovery of story data.
 * Provides automatic backups, retention management, corruption detection, and compression.
 * 
 * Requirements: 60.1, 60.2, 60.3, 60.4, 60.5
 */
public class BackupManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private static final String BACKUP_EXTENSION = ".backup.gz";
    private static final String BACKUP_PREFIX = "backup_";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final Path dataDirectory;
    private final Path backupDirectory;
    private final BackupConfig config;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> autoBackupTask;
    
    private volatile boolean running = false;
    
    public BackupManager(Path dataDirectory) {
        this(dataDirectory, new BackupConfig());
    }
    
    public BackupManager(Path dataDirectory, BackupConfig config) {
        this.dataDirectory = dataDirectory;
        this.backupDirectory = dataDirectory.resolve("backups");
        this.config = config;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Storykee-Backup-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    
    /**
     * Initializes the backup manager and starts automatic backups if enabled.
     */
    public void initialize() throws IOException {
        if (running) {
            LOGGER.warn("BackupManager already initialized");
            return;
        }
        
        // Create backup directory
        Files.createDirectories(backupDirectory);
        
        // Start automatic backups if enabled
        if (config.isAutoBackupEnabled()) {
            startAutoBackup();
        }
        
        running = true;
        LOGGER.info("BackupManager initialized. Backup directory: {}", backupDirectory);
    }
    
    /**
     * Shuts down the backup manager.
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        
        running = false;
        
        if (autoBackupTask != null) {
            autoBackupTask.cancel(false);
            autoBackupTask = null;
        }
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOGGER.info("BackupManager shut down");
    }
    
    /**
     * Starts automatic backup scheduling.
     */
    private void startAutoBackup() {
        long intervalMinutes = config.getAutoBackupIntervalMinutes();
        
        autoBackupTask = scheduler.scheduleAtFixedRate(
            this::performAutoBackup,
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );
        
        LOGGER.info("Automatic backups enabled. Interval: {} minutes", intervalMinutes);
    }
    
    /**
     * Performs an automatic backup.
     */
    private void performAutoBackup() {
        try {
            LOGGER.debug("Performing automatic backup...");
            BackupResult result = createBackup("auto");
            
            if (result.isSuccess()) {
                LOGGER.info("Automatic backup created: {}", result.getBackupPath());
                
                // Clean up old backups
                cleanupOldBackups();
            } else {
                LOGGER.error("Automatic backup failed: {}", result.getErrorMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Error during automatic backup", e);
        }
    }
    
    /**
     * Creates a manual backup immediately.
     * Requirement 60.4: Manual backup on request.
     * 
     * @return BackupResult containing the result of the backup operation
     */
    public BackupResult createManualBackup() {
        return createBackup("manual");
    }
    
    /**
     * Creates a backup with the specified type prefix.
     * Requirement 60.1: Creates timestamped backup copy.
     * Requirement 60.5: Compresses backups to save disk space.
     * 
     * @param type The type of backup (auto, manual, etc.)
     * @return BackupResult containing the result of the backup operation
     */
    public BackupResult createBackup(String type) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupName = BACKUP_PREFIX + type + "_" + timestamp + BACKUP_EXTENSION;
        Path backupPath = backupDirectory.resolve(backupName);
        
        try {
            // Collect all data to backup
            BackupData backupData = collectBackupData();
            backupData.setTimestamp(Instant.now().toString());
            backupData.setType(type);
            backupData.setVersion(1);
            
            // Calculate checksum before compression
            String json = GSON.toJson(backupData);
            String checksum = calculateChecksum(json);
            backupData.setChecksum(checksum);
            
            // Re-serialize with checksum
            json = GSON.toJson(backupData);
            
            // Write compressed backup (Requirement 60.5)
            writeCompressedBackup(backupPath, json);
            
            long fileSize = Files.size(backupPath);
            LOGGER.info("Backup created: {} ({} bytes)", backupPath, fileSize);
            
            return BackupResult.success(backupPath, fileSize);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create backup: {}", e.getMessage(), e);
            return BackupResult.failure(e.getMessage());
        }
    }

    
    /**
     * Collects all data that needs to be backed up.
     */
    private BackupData collectBackupData() throws IOException {
        BackupData data = new BackupData();
        
        // Collect player variables
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        if (Files.exists(playerVarsDir)) {
            data.setPlayerVariables(collectJsonFiles(playerVarsDir));
        }
        
        // Collect global variables
        Path globalVarsFile = dataDirectory.resolve("global_variables.json");
        if (Files.exists(globalVarsFile)) {
            data.setGlobalVariables(Files.readString(globalVarsFile));
        }
        
        // Collect quest progress
        Path questProgressDir = dataDirectory.resolve("quest_progress");
        if (Files.exists(questProgressDir)) {
            data.setQuestProgress(collectJsonFiles(questProgressDir));
        }
        
        // Collect story configs
        Path configsDir = dataDirectory.resolve("configs");
        if (Files.exists(configsDir)) {
            data.setStoryConfigs(collectJsonFiles(configsDir));
        }
        
        return data;
    }
    
    /**
     * Collects all JSON files from a directory into a map.
     */
    private Map<String, String> collectJsonFiles(Path directory) throws IOException {
        Map<String, String> files = new HashMap<>();
        
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                  .forEach(p -> {
                      try {
                          String filename = p.getFileName().toString();
                          String content = Files.readString(p);
                          files.put(filename, content);
                      } catch (IOException e) {
                          LOGGER.warn("Failed to read file {}: {}", p, e.getMessage());
                      }
                  });
        }
        
        return files;
    }
    
    /**
     * Writes compressed backup data to a file.
     */
    private void writeCompressedBackup(Path backupPath, String json) throws IOException {
        try (OutputStream fos = Files.newOutputStream(backupPath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter writer = new OutputStreamWriter(gzos)) {
            writer.write(json);
        }
    }
    
    /**
     * Reads and decompresses backup data from a file.
     */
    private String readCompressedBackup(Path backupPath) throws IOException {
        try (InputStream fis = Files.newInputStream(backupPath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             InputStreamReader reader = new InputStreamReader(gzis);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * Calculates a checksum for data integrity verification.
     */
    private String calculateChecksum(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            LOGGER.warn("SHA-256 not available, using simple hash");
            return String.valueOf(data.hashCode());
        }
    }
    
    /**
     * Cleans up old backups beyond the retention limit.
     * Requirement 60.2: Automatically deletes old backups beyond retention limit.
     */
    public void cleanupOldBackups() {
        try {
            List<Path> backups = listBackups();
            int maxBackups = config.getMaxBackupCount();
            
            if (backups.size() <= maxBackups) {
                return;
            }
            
            // Sort by modification time (oldest first)
            backups.sort(Comparator.comparingLong(p -> {
                try {
                    return Files.getLastModifiedTime(p).toMillis();
                } catch (IOException e) {
                    return 0L;
                }
            }));
            
            // Delete oldest backups
            int toDelete = backups.size() - maxBackups;
            for (int i = 0; i < toDelete; i++) {
                Path backup = backups.get(i);
                try {
                    Files.delete(backup);
                    LOGGER.info("Deleted old backup: {}", backup);
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete old backup {}: {}", backup, e.getMessage());
                }
            }
            
            LOGGER.info("Cleaned up {} old backups", toDelete);
            
        } catch (IOException e) {
            LOGGER.error("Failed to cleanup old backups", e);
        }
    }

    
    /**
     * Lists all backup files sorted by date (newest first).
     */
    public List<Path> listBackups() throws IOException {
        if (!Files.exists(backupDirectory)) {
            return Collections.emptyList();
        }
        
        try (Stream<Path> stream = Files.list(backupDirectory)) {
            return stream
                .filter(p -> p.getFileName().toString().endsWith(BACKUP_EXTENSION))
                .sorted((a, b) -> {
                    try {
                        return Long.compare(
                            Files.getLastModifiedTime(b).toMillis(),
                            Files.getLastModifiedTime(a).toMillis()
                        );
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Detects corruption in a backup file.
     * Requirement 60.3: Detects data corruption.
     * 
     * @param backupPath Path to the backup file
     * @return CorruptionCheckResult containing the result of the check
     */
    public CorruptionCheckResult checkBackupIntegrity(Path backupPath) {
        try {
            // Try to read and decompress
            String json = readCompressedBackup(backupPath);
            
            // Try to parse JSON
            BackupData data = GSON.fromJson(json, BackupData.class);
            
            if (data == null) {
                return CorruptionCheckResult.corrupted("Backup data is null");
            }
            
            // Verify checksum if present
            if (data.getChecksum() != null) {
                // Remove checksum from data for verification
                String storedChecksum = data.getChecksum();
                data.setChecksum(null);
                String jsonWithoutChecksum = GSON.toJson(data);
                String calculatedChecksum = calculateChecksum(jsonWithoutChecksum);
                
                if (!storedChecksum.equals(calculatedChecksum)) {
                    return CorruptionCheckResult.corrupted("Checksum mismatch");
                }
            }
            
            return CorruptionCheckResult.valid();
            
        } catch (IOException e) {
            return CorruptionCheckResult.corrupted("Failed to read backup: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            return CorruptionCheckResult.corrupted("Invalid JSON: " + e.getMessage());
        }
    }
    
    /**
     * Detects corruption in the current data files.
     * Requirement 60.3: Detects data corruption.
     * 
     * @return List of corrupted files
     */
    public List<CorruptedFile> detectDataCorruption() {
        List<CorruptedFile> corrupted = new ArrayList<>();
        
        // Check player variables
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        if (Files.exists(playerVarsDir)) {
            corrupted.addAll(checkJsonFilesInDirectory(playerVarsDir));
        }
        
        // Check global variables
        Path globalVarsFile = dataDirectory.resolve("global_variables.json");
        if (Files.exists(globalVarsFile)) {
            CorruptedFile result = checkJsonFile(globalVarsFile);
            if (result != null) {
                corrupted.add(result);
            }
        }
        
        // Check quest progress
        Path questProgressDir = dataDirectory.resolve("quest_progress");
        if (Files.exists(questProgressDir)) {
            corrupted.addAll(checkJsonFilesInDirectory(questProgressDir));
        }
        
        return corrupted;
    }
    
    /**
     * Checks all JSON files in a directory for corruption.
     */
    private List<CorruptedFile> checkJsonFilesInDirectory(Path directory) {
        List<CorruptedFile> corrupted = new ArrayList<>();
        
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                  .forEach(p -> {
                      CorruptedFile result = checkJsonFile(p);
                      if (result != null) {
                          corrupted.add(result);
                      }
                  });
        } catch (IOException e) {
            LOGGER.warn("Failed to list directory {}: {}", directory, e.getMessage());
        }
        
        return corrupted;
    }
    
    /**
     * Checks a single JSON file for corruption.
     */
    private CorruptedFile checkJsonFile(Path file) {
        try {
            String content = Files.readString(file);
            
            // Try to parse as JSON
            GSON.fromJson(content, Object.class);
            
            return null; // File is valid
            
        } catch (IOException e) {
            return new CorruptedFile(file, "Failed to read: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            return new CorruptedFile(file, "Invalid JSON: " + e.getMessage());
        }
    }
    
    /**
     * Restores data from the most recent valid backup.
     * Requirement 60.3: Attempts to restore from most recent backup.
     * 
     * @return RestoreResult containing the result of the restore operation
     */
    public RestoreResult restoreFromLatestBackup() {
        try {
            List<Path> backups = listBackups();
            
            if (backups.isEmpty()) {
                return RestoreResult.failure("No backups available");
            }
            
            // Find the first valid backup
            for (Path backup : backups) {
                CorruptionCheckResult check = checkBackupIntegrity(backup);
                if (check.isValid()) {
                    return restoreFromBackup(backup);
                } else {
                    LOGGER.warn("Backup {} is corrupted: {}", backup, check.getReason());
                }
            }
            
            return RestoreResult.failure("All backups are corrupted");
            
        } catch (IOException e) {
            return RestoreResult.failure("Failed to list backups: " + e.getMessage());
        }
    }

    
    /**
     * Restores data from a specific backup file.
     * 
     * @param backupPath Path to the backup file
     * @return RestoreResult containing the result of the restore operation
     */
    public RestoreResult restoreFromBackup(Path backupPath) {
        try {
            // Verify backup integrity first
            CorruptionCheckResult check = checkBackupIntegrity(backupPath);
            if (!check.isValid()) {
                return RestoreResult.failure("Backup is corrupted: " + check.getReason());
            }
            
            // Read backup data
            String json = readCompressedBackup(backupPath);
            BackupData data = GSON.fromJson(json, BackupData.class);
            
            // Create a backup of current data before restoring
            createBackup("pre-restore");
            
            int restoredFiles = 0;
            
            // Restore player variables
            if (data.getPlayerVariables() != null) {
                Path playerVarsDir = dataDirectory.resolve("player_variables");
                Files.createDirectories(playerVarsDir);
                
                for (Map.Entry<String, String> entry : data.getPlayerVariables().entrySet()) {
                    Path file = playerVarsDir.resolve(entry.getKey());
                    Files.writeString(file, entry.getValue());
                    restoredFiles++;
                }
            }
            
            // Restore global variables
            if (data.getGlobalVariables() != null) {
                Path globalVarsFile = dataDirectory.resolve("global_variables.json");
                Files.writeString(globalVarsFile, data.getGlobalVariables());
                restoredFiles++;
            }
            
            // Restore quest progress
            if (data.getQuestProgress() != null) {
                Path questProgressDir = dataDirectory.resolve("quest_progress");
                Files.createDirectories(questProgressDir);
                
                for (Map.Entry<String, String> entry : data.getQuestProgress().entrySet()) {
                    Path file = questProgressDir.resolve(entry.getKey());
                    Files.writeString(file, entry.getValue());
                    restoredFiles++;
                }
            }
            
            // Restore story configs
            if (data.getStoryConfigs() != null) {
                Path configsDir = dataDirectory.resolve("configs");
                Files.createDirectories(configsDir);
                
                for (Map.Entry<String, String> entry : data.getStoryConfigs().entrySet()) {
                    Path file = configsDir.resolve(entry.getKey());
                    Files.writeString(file, entry.getValue());
                    restoredFiles++;
                }
            }
            
            LOGGER.info("Restored {} files from backup: {}", restoredFiles, backupPath);
            return RestoreResult.success(backupPath, restoredFiles);
            
        } catch (Exception e) {
            LOGGER.error("Failed to restore from backup: {}", e.getMessage(), e);
            return RestoreResult.failure("Restore failed: " + e.getMessage());
        }
    }
    
    /**
     * Gets backup information for a specific backup file.
     */
    public BackupInfo getBackupInfo(Path backupPath) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(backupPath, BasicFileAttributes.class);
            CorruptionCheckResult integrity = checkBackupIntegrity(backupPath);
            
            String json = readCompressedBackup(backupPath);
            BackupData data = GSON.fromJson(json, BackupData.class);
            
            return new BackupInfo(
                backupPath,
                attrs.size(),
                attrs.creationTime().toInstant(),
                data.getType(),
                integrity.isValid(),
                data.getPlayerVariables() != null ? data.getPlayerVariables().size() : 0,
                data.getQuestProgress() != null ? data.getQuestProgress().size() : 0
            );
            
        } catch (Exception e) {
            LOGGER.warn("Failed to get backup info for {}: {}", backupPath, e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets information about all backups.
     */
    public List<BackupInfo> getAllBackupInfo() {
        try {
            List<Path> backups = listBackups();
            List<BackupInfo> infos = new ArrayList<>();
            
            for (Path backup : backups) {
                BackupInfo info = getBackupInfo(backup);
                if (info != null) {
                    infos.add(info);
                }
            }
            
            return infos;
            
        } catch (IOException e) {
            LOGGER.error("Failed to get backup info", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets the backup directory path.
     */
    public Path getBackupDirectory() {
        return backupDirectory;
    }
    
    /**
     * Gets the current configuration.
     */
    public BackupConfig getConfig() {
        return config;
    }
    
    /**
     * Checks if the backup manager is running.
     */
    public boolean isRunning() {
        return running;
    }
}
