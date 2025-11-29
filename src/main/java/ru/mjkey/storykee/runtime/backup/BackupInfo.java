package ru.mjkey.storykee.runtime.backup;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Information about a backup file.
 */
public class BackupInfo {
    
    private final Path path;
    private final long fileSize;
    private final Instant createdAt;
    private final String type;
    private final boolean valid;
    private final int playerVariablesCount;
    private final int questProgressCount;
    
    public BackupInfo(Path path, long fileSize, Instant createdAt, String type, 
                      boolean valid, int playerVariablesCount, int questProgressCount) {
        this.path = path;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.type = type;
        this.valid = valid;
        this.playerVariablesCount = playerVariablesCount;
        this.questProgressCount = questProgressCount;
    }
    
    public Path getPath() {
        return path;
    }
    
    public String getFileName() {
        return path.getFileName().toString();
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public int getPlayerVariablesCount() {
        return playerVariablesCount;
    }
    
    public int getQuestProgressCount() {
        return questProgressCount;
    }
    
    @Override
    public String toString() {
        return "BackupInfo{" +
               "path=" + path +
               ", size=" + getFormattedFileSize() +
               ", type=" + type +
               ", valid=" + valid +
               ", players=" + playerVariablesCount +
               ", quests=" + questProgressCount +
               "}";
    }
}
