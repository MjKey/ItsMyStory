package ru.mjkey.storykee.runtime.backup;

import java.util.Map;

/**
 * Data structure for backup contents.
 * Contains all story data that needs to be backed up.
 */
public class BackupData {
    
    private int version;
    private String timestamp;
    private String type;
    private String checksum;
    
    private Map<String, String> playerVariables;
    private String globalVariables;
    private Map<String, String> questProgress;
    private Map<String, String> storyConfigs;
    
    public BackupData() {
    }
    
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    public Map<String, String> getPlayerVariables() {
        return playerVariables;
    }
    
    public void setPlayerVariables(Map<String, String> playerVariables) {
        this.playerVariables = playerVariables;
    }
    
    public String getGlobalVariables() {
        return globalVariables;
    }
    
    public void setGlobalVariables(String globalVariables) {
        this.globalVariables = globalVariables;
    }
    
    public Map<String, String> getQuestProgress() {
        return questProgress;
    }
    
    public void setQuestProgress(Map<String, String> questProgress) {
        this.questProgress = questProgress;
    }
    
    public Map<String, String> getStoryConfigs() {
        return storyConfigs;
    }
    
    public void setStoryConfigs(Map<String, String> storyConfigs) {
        this.storyConfigs = storyConfigs;
    }
}
