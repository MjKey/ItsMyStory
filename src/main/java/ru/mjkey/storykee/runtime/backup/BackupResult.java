package ru.mjkey.storykee.runtime.backup;

import java.nio.file.Path;

/**
 * Result of a backup operation.
 */
public class BackupResult {
    
    private final boolean success;
    private final Path backupPath;
    private final long fileSize;
    private final String errorMessage;
    
    private BackupResult(boolean success, Path backupPath, long fileSize, String errorMessage) {
        this.success = success;
        this.backupPath = backupPath;
        this.fileSize = fileSize;
        this.errorMessage = errorMessage;
    }
    
    public static BackupResult success(Path backupPath, long fileSize) {
        return new BackupResult(true, backupPath, fileSize, null);
    }
    
    public static BackupResult failure(String errorMessage) {
        return new BackupResult(false, null, 0, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Path getBackupPath() {
        return backupPath;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "BackupResult{success=true, path=" + backupPath + ", size=" + fileSize + "}";
        } else {
            return "BackupResult{success=false, error=" + errorMessage + "}";
        }
    }
}
