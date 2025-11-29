package ru.mjkey.storykee.systems.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Handles persistence of quest progress data.
 * Saves and loads quest state to/from JSON files.
 * Requirements: 8.5
 */
public class QuestPersistence {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestPersistence.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path dataDirectory;
    private final QuestManager questManager;
    
    public QuestPersistence(Path dataDirectory, QuestManager questManager) {
        this.dataDirectory = dataDirectory;
        this.questManager = questManager;
    }
    
    /**
     * Saves quest progress for a player.
     */
    public void savePlayerProgress(UUID playerId) throws IOException {
        if (playerId == null) {
            return;
        }
        
        // Get progress data
        List<QuestProgress.QuestProgressData> progressData = questManager.getPlayerProgressData(playerId);
        
        if (progressData.isEmpty()) {
            LOGGER.debug("No quest progress to save for player {}", playerId);
            return;
        }
        
        // Create save data structure
        PlayerQuestData saveData = new PlayerQuestData();
        saveData.playerId = playerId.toString();
        saveData.questProgress = progressData;
        saveData.timestamp = Instant.now().toString();
        
        // Ensure directory exists
        Path questDataDir = dataDirectory.resolve("quest_progress");
        Files.createDirectories(questDataDir);
        
        // Write to file
        Path saveFile = questDataDir.resolve(playerId.toString() + ".json");
        String json = GSON.toJson(saveData);
        Files.writeString(saveFile, json);
        
        LOGGER.debug("Saved quest progress for player {}: {} quests", playerId, progressData.size());
    }
    
    /**
     * Loads quest progress for a player.
     */
    public void loadPlayerProgress(UUID playerId) throws IOException {
        if (playerId == null) {
            return;
        }
        
        Path questDataDir = dataDirectory.resolve("quest_progress");
        Path saveFile = questDataDir.resolve(playerId.toString() + ".json");
        
        if (!Files.exists(saveFile)) {
            LOGGER.debug("No saved quest progress found for player {}", playerId);
            return;
        }
        
        try {
            String json = Files.readString(saveFile);
            PlayerQuestData saveData = GSON.fromJson(json, PlayerQuestData.class);
            
            if (saveData == null || saveData.questProgress == null) {
                LOGGER.warn("Invalid quest data file for player {}", playerId);
                return;
            }
            
            // Validate player ID matches
            if (!playerId.toString().equals(saveData.playerId)) {
                LOGGER.warn("Player ID mismatch in quest data file for {}", playerId);
            }
            
            // Load progress into manager
            questManager.loadPlayerProgressData(playerId, saveData.questProgress);
            
            LOGGER.debug("Loaded quest progress for player {}: {} quests", 
                playerId, saveData.questProgress.size());
            
        } catch (JsonSyntaxException e) {
            LOGGER.error("Failed to parse quest data file for player {}: {}", playerId, e.getMessage());
            throw new IOException("Corrupted quest data file", e);
        }
    }
    
    /**
     * Saves all player progress.
     */
    public void saveAllProgress() {
        Set<UUID> playerIds = getAllPlayerIds();
        
        for (UUID playerId : playerIds) {
            try {
                savePlayerProgress(playerId);
            } catch (IOException e) {
                LOGGER.error("Failed to save quest progress for player {}: {}", playerId, e.getMessage());
            }
        }
        
        LOGGER.info("Saved quest progress for {} players", playerIds.size());
    }
    
    /**
     * Gets all player IDs that have quest progress.
     */
    private Set<UUID> getAllPlayerIds() {
        Set<UUID> playerIds = new HashSet<>();
        
        // Get from quest manager
        for (String questId : questManager.getQuestIds()) {
            // This is a simplified approach - in practice, we'd track player IDs separately
        }
        
        // Also check existing save files
        Path questDataDir = dataDirectory.resolve("quest_progress");
        if (Files.exists(questDataDir)) {
            try {
                Files.list(questDataDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        String filename = p.getFileName().toString();
                        String uuidStr = filename.substring(0, filename.length() - 5);
                        try {
                            playerIds.add(UUID.fromString(uuidStr));
                        } catch (IllegalArgumentException e) {
                            // Invalid UUID filename, skip
                        }
                    });
            } catch (IOException e) {
                LOGGER.error("Failed to list quest data directory", e);
            }
        }
        
        return playerIds;
    }
    
    /**
     * Deletes quest progress for a player.
     */
    public void deletePlayerProgress(UUID playerId) throws IOException {
        if (playerId == null) {
            return;
        }
        
        Path questDataDir = dataDirectory.resolve("quest_progress");
        Path saveFile = questDataDir.resolve(playerId.toString() + ".json");
        
        if (Files.exists(saveFile)) {
            Files.delete(saveFile);
            LOGGER.info("Deleted quest progress for player {}", playerId);
        }
        
        questManager.clearPlayerProgress(playerId);
    }
    
    /**
     * Creates a backup of quest progress.
     */
    public void createBackup() throws IOException {
        Path questDataDir = dataDirectory.resolve("quest_progress");
        if (!Files.exists(questDataDir)) {
            return;
        }
        
        Path backupDir = dataDirectory.resolve("quest_backups");
        Files.createDirectories(backupDir);
        
        String timestamp = Instant.now().toString().replace(":", "-");
        Path backupFile = backupDir.resolve("backup_" + timestamp + ".json");
        
        // Collect all progress data
        Map<String, List<QuestProgress.QuestProgressData>> allProgress = new HashMap<>();
        
        try {
            Files.list(questDataDir)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> {
                    try {
                        String json = Files.readString(p);
                        PlayerQuestData data = GSON.fromJson(json, PlayerQuestData.class);
                        if (data != null && data.questProgress != null) {
                            allProgress.put(data.playerId, data.questProgress);
                        }
                    } catch (IOException | JsonSyntaxException e) {
                        LOGGER.warn("Failed to read quest data file {}: {}", p, e.getMessage());
                    }
                });
        } catch (IOException e) {
            LOGGER.error("Failed to list quest data directory for backup", e);
            throw e;
        }
        
        // Write backup
        BackupData backup = new BackupData();
        backup.timestamp = timestamp;
        backup.playerProgress = allProgress;
        
        String json = GSON.toJson(backup);
        Files.writeString(backupFile, json);
        
        LOGGER.info("Created quest progress backup: {}", backupFile);
    }
    
    /**
     * Restores quest progress from a backup.
     */
    public void restoreFromBackup(Path backupFile) throws IOException {
        if (!Files.exists(backupFile)) {
            throw new IOException("Backup file not found: " + backupFile);
        }
        
        String json = Files.readString(backupFile);
        BackupData backup = GSON.fromJson(json, BackupData.class);
        
        if (backup == null || backup.playerProgress == null) {
            throw new IOException("Invalid backup file");
        }
        
        // Restore each player's progress
        for (Map.Entry<String, List<QuestProgress.QuestProgressData>> entry : backup.playerProgress.entrySet()) {
            try {
                UUID playerId = UUID.fromString(entry.getKey());
                questManager.loadPlayerProgressData(playerId, entry.getValue());
                savePlayerProgress(playerId);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid player ID in backup: {}", entry.getKey());
            }
        }
        
        LOGGER.info("Restored quest progress from backup: {}", backupFile);
    }
    
    /**
     * Data structure for player quest save files.
     */
    private static class PlayerQuestData {
        String playerId;
        List<QuestProgress.QuestProgressData> questProgress;
        String timestamp;
    }
    
    /**
     * Data structure for backup files.
     */
    private static class BackupData {
        String timestamp;
        Map<String, List<QuestProgress.QuestProgressData>> playerProgress;
    }
}
