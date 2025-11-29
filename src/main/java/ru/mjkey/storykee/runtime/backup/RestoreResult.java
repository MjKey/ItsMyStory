package ru.mjkey.storykee.runtime.backup;

import java.nio.file.Path;

/**
 * Result of a restore operation.
 */
public class RestoreResult {
    
    private final boolean success;
    private final Path backupPath;
    private final int restoredFiles;
    private final String errorMessage;
    
    private RestoreResult(boolean success, Path backupPath, int restoredFiles, String errorMessage) {
        this.success = success;
        this.backupPath = backupPath;
        this.restoredFiles = restoredFiles;
        this.errorMessage = errorMessage;
    }
    
    public static RestoreResult success(Path backupPath, int restoredFiles) {
        return new RestoreResult(true, backupPath, restoredFiles, null);
    }
    
    public static RestoreResult failure(String errorMessage) {
        return new RestoreResult(false, null, 0, errorMessage);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Path getBackupPath() {
        return backupPath;
    }
    
    public int getRestoredFiles() {
        return restoredFiles;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    @Override
    public String toString() {
        if (success) {
            return "RestoreResult{success=true, path=" + backupPath + ", files=" + restoredFiles + "}";
        } else {
            return "RestoreResult{success=false, error=" + errorMessage + "}";
        }
    }
}
