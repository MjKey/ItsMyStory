package ru.mjkey.storykee.runtime.backup;

/**
 * Configuration for the backup system.
 */
public class BackupConfig {
    
    private boolean autoBackupEnabled = true;
    private long autoBackupIntervalMinutes = 30;
    private int maxBackupCount = 10;
    private boolean compressionEnabled = true;
    
    public BackupConfig() {
    }
    
    public BackupConfig(boolean autoBackupEnabled, long autoBackupIntervalMinutes, 
                        int maxBackupCount, boolean compressionEnabled) {
        this.autoBackupEnabled = autoBackupEnabled;
        this.autoBackupIntervalMinutes = autoBackupIntervalMinutes;
        this.maxBackupCount = maxBackupCount;
        this.compressionEnabled = compressionEnabled;
    }
    
    public boolean isAutoBackupEnabled() {
        return autoBackupEnabled;
    }
    
    public void setAutoBackupEnabled(boolean autoBackupEnabled) {
        this.autoBackupEnabled = autoBackupEnabled;
    }
    
    public long getAutoBackupIntervalMinutes() {
        return autoBackupIntervalMinutes;
    }
    
    public void setAutoBackupIntervalMinutes(long autoBackupIntervalMinutes) {
        this.autoBackupIntervalMinutes = autoBackupIntervalMinutes;
    }
    
    public int getMaxBackupCount() {
        return maxBackupCount;
    }
    
    public void setMaxBackupCount(int maxBackupCount) {
        this.maxBackupCount = maxBackupCount;
    }
    
    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }
    
    public void setCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
    }
    
    /**
     * Creates a builder for BackupConfig.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean autoBackupEnabled = true;
        private long autoBackupIntervalMinutes = 30;
        private int maxBackupCount = 10;
        private boolean compressionEnabled = true;
        
        public Builder autoBackupEnabled(boolean enabled) {
            this.autoBackupEnabled = enabled;
            return this;
        }
        
        public Builder autoBackupIntervalMinutes(long minutes) {
            this.autoBackupIntervalMinutes = minutes;
            return this;
        }
        
        public Builder maxBackupCount(int count) {
            this.maxBackupCount = count;
            return this;
        }
        
        public Builder compressionEnabled(boolean enabled) {
            this.compressionEnabled = enabled;
            return this;
        }
        
        public BackupConfig build() {
            return new BackupConfig(autoBackupEnabled, autoBackupIntervalMinutes, 
                                   maxBackupCount, compressionEnabled);
        }
    }
}
