package ru.mjkey.storykee.runtime.backup;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackupManager.
 * Tests backup creation, retention, corruption detection, and recovery.
 * Requirements: 60.1, 60.2, 60.3, 60.4, 60.5
 */
class BackupManagerTest {
    
    @TempDir
    Path tempDir;
    
    private BackupManager backupManager;
    private Path dataDirectory;
    
    @BeforeEach
    void setUp() throws IOException {
        dataDirectory = tempDir.resolve("data");
        Files.createDirectories(dataDirectory);
        
        // Create backup manager with auto-backup disabled for testing
        BackupConfig config = BackupConfig.builder()
            .autoBackupEnabled(false)
            .maxBackupCount(3)
            .build();
        
        backupManager = new BackupManager(dataDirectory, config);
        backupManager.initialize();
    }
    
    @AfterEach
    void tearDown() {
        if (backupManager != null) {
            backupManager.shutdown();
        }
    }
    
    @Test
    @DisplayName("Should create backup directory on initialization")
    void shouldCreateBackupDirectory() {
        Path backupDir = backupManager.getBackupDirectory();
        assertTrue(Files.exists(backupDir), "Backup directory should exist");
    }
    
    @Test
    @DisplayName("Should create manual backup successfully")
    void shouldCreateManualBackup() throws IOException {
        // Create some test data
        createTestData();
        
        // Create backup
        BackupResult result = backupManager.createManualBackup();
        
        assertTrue(result.isSuccess(), "Backup should succeed");
        assertNotNull(result.getBackupPath(), "Backup path should not be null");
        assertTrue(Files.exists(result.getBackupPath()), "Backup file should exist");
        assertTrue(result.getFileSize() > 0, "Backup file should have content");
    }
    
    @Test
    @DisplayName("Should create compressed backup")
    void shouldCreateCompressedBackup() throws IOException {
        // Create some test data
        createTestData();
        
        // Create backup
        BackupResult result = backupManager.createBackup("test");
        
        assertTrue(result.isSuccess());
        String filename = result.getBackupPath().getFileName().toString();
        assertTrue(filename.endsWith(".backup.gz"), "Backup should be compressed");
    }

    
    @Test
    @DisplayName("Should detect valid backup integrity")
    void shouldDetectValidBackupIntegrity() throws IOException {
        createTestData();
        BackupResult result = backupManager.createBackup("test");
        
        assertTrue(result.isSuccess());
        
        CorruptionCheckResult check = backupManager.checkBackupIntegrity(result.getBackupPath());
        assertTrue(check.isValid(), "Backup should be valid");
    }
    
    @Test
    @DisplayName("Should detect corrupted backup")
    void shouldDetectCorruptedBackup() throws IOException {
        createTestData();
        BackupResult result = backupManager.createBackup("test");
        
        assertTrue(result.isSuccess());
        
        // Corrupt the backup file
        Files.writeString(result.getBackupPath(), "corrupted data");
        
        CorruptionCheckResult check = backupManager.checkBackupIntegrity(result.getBackupPath());
        assertTrue(check.isCorrupted(), "Backup should be detected as corrupted");
        assertNotNull(check.getReason(), "Corruption reason should be provided");
    }
    
    @Test
    @DisplayName("Should restore from backup successfully")
    void shouldRestoreFromBackup() throws IOException {
        // Create test data
        createTestData();
        
        // Create backup
        BackupResult backupResult = backupManager.createBackup("test");
        assertTrue(backupResult.isSuccess());
        
        // Delete original data
        deleteTestData();
        
        // Restore from backup
        RestoreResult restoreResult = backupManager.restoreFromBackup(backupResult.getBackupPath());
        
        assertTrue(restoreResult.isSuccess(), "Restore should succeed");
        assertTrue(restoreResult.getRestoredFiles() > 0, "Should restore files");
        
        // Verify data was restored
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        assertTrue(Files.exists(playerVarsDir), "Player variables directory should exist");
    }
    
    @Test
    @DisplayName("Should cleanup old backups beyond retention limit")
    void shouldCleanupOldBackups() throws IOException {
        createTestData();
        
        // Create more backups than the retention limit (3)
        for (int i = 0; i < 5; i++) {
            BackupResult result = backupManager.createBackup("test" + i);
            assertTrue(result.isSuccess());
            // Small delay to ensure different timestamps
            try { Thread.sleep(10); } catch (InterruptedException e) { }
        }
        
        // Cleanup old backups
        backupManager.cleanupOldBackups();
        
        // Should only have 3 backups remaining
        List<Path> backups = backupManager.listBackups();
        assertEquals(3, backups.size(), "Should have exactly 3 backups after cleanup");
    }
    
    @Test
    @DisplayName("Should list backups sorted by date")
    void shouldListBackupsSortedByDate() throws IOException {
        createTestData();
        
        // Create multiple backups
        for (int i = 0; i < 3; i++) {
            backupManager.createBackup("test" + i);
            try { Thread.sleep(10); } catch (InterruptedException e) { }
        }
        
        List<Path> backups = backupManager.listBackups();
        
        assertEquals(3, backups.size());
        
        // Verify sorted by date (newest first)
        for (int i = 0; i < backups.size() - 1; i++) {
            long time1 = Files.getLastModifiedTime(backups.get(i)).toMillis();
            long time2 = Files.getLastModifiedTime(backups.get(i + 1)).toMillis();
            assertTrue(time1 >= time2, "Backups should be sorted newest first");
        }
    }
    
    @Test
    @DisplayName("Should detect data corruption in JSON files")
    void shouldDetectDataCorruption() throws IOException {
        // Create valid test data
        createTestData();
        
        // Corrupt one of the files
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        Path corruptedFile = playerVarsDir.resolve("corrupted.json");
        Files.writeString(corruptedFile, "{ invalid json }}}");
        
        List<CorruptedFile> corrupted = backupManager.detectDataCorruption();
        
        assertFalse(corrupted.isEmpty(), "Should detect corrupted file");
        assertTrue(corrupted.stream().anyMatch(f -> f.getPath().equals(corruptedFile)),
            "Should identify the corrupted file");
    }
    
    @Test
    @DisplayName("Should restore from latest valid backup")
    void shouldRestoreFromLatestBackup() throws IOException {
        createTestData();
        
        // Create a valid backup
        BackupResult result = backupManager.createBackup("valid");
        assertTrue(result.isSuccess());
        
        // Delete original data
        deleteTestData();
        
        // Restore from latest
        RestoreResult restoreResult = backupManager.restoreFromLatestBackup();
        
        assertTrue(restoreResult.isSuccess(), "Should restore from latest backup");
    }
    
    @Test
    @DisplayName("Should get backup info")
    void shouldGetBackupInfo() throws IOException {
        createTestData();
        
        BackupResult result = backupManager.createBackup("test");
        assertTrue(result.isSuccess());
        
        BackupInfo info = backupManager.getBackupInfo(result.getBackupPath());
        
        assertNotNull(info);
        assertEquals("test", info.getType());
        assertTrue(info.isValid());
        assertTrue(info.getFileSize() > 0);
    }
    
    // Helper methods
    
    private void createTestData() throws IOException {
        // Create player variables
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        Files.createDirectories(playerVarsDir);
        Files.writeString(playerVarsDir.resolve("player1.json"), 
            "{\"playerId\":\"uuid-1\",\"variables\":{\"score\":100}}");
        Files.writeString(playerVarsDir.resolve("player2.json"), 
            "{\"playerId\":\"uuid-2\",\"variables\":{\"score\":200}}");
        
        // Create global variables
        Files.writeString(dataDirectory.resolve("global_variables.json"),
            "{\"worldState\":\"active\",\"version\":1}");
        
        // Create quest progress
        Path questProgressDir = dataDirectory.resolve("quest_progress");
        Files.createDirectories(questProgressDir);
        Files.writeString(questProgressDir.resolve("player1.json"),
            "{\"playerId\":\"uuid-1\",\"quests\":[{\"id\":\"quest1\",\"status\":\"active\"}]}");
    }
    
    private void deleteTestData() throws IOException {
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        if (Files.exists(playerVarsDir)) {
            Files.walk(playerVarsDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException e) { }
                });
        }
        
        Files.deleteIfExists(dataDirectory.resolve("global_variables.json"));
        
        Path questProgressDir = dataDirectory.resolve("quest_progress");
        if (Files.exists(questProgressDir)) {
            Files.walk(questProgressDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException e) { }
                });
        }
    }
}
