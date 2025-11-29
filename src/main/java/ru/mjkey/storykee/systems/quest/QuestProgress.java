package ru.mjkey.storykee.systems.quest;

import java.util.*;

/**
 * Tracks a player's progress on a specific quest.
 * Requirements: 8.2, 8.3, 8.5
 */
public class QuestProgress {
    
    private final String questId;
    private final UUID playerId;
    private QuestStatus status;
    private final Map<String, Integer> objectiveProgress;
    private final Set<String> completedObjectives;
    private long startTime;
    private long completeTime;
    private int completionCount;
    
    public QuestProgress(String questId, UUID playerId) {
        this.questId = questId;
        this.playerId = playerId;
        this.status = QuestStatus.NOT_STARTED;
        this.objectiveProgress = new HashMap<>();
        this.completedObjectives = new HashSet<>();
        this.startTime = 0;
        this.completeTime = 0;
        this.completionCount = 0;
    }
    
    public String getQuestId() {
        return questId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public QuestStatus getStatus() {
        return status;
    }
    
    public void setStatus(QuestStatus status) {
        this.status = status;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getCompleteTime() {
        return completeTime;
    }
    
    public void setCompleteTime(long completeTime) {
        this.completeTime = completeTime;
    }
    
    public int getCompletionCount() {
        return completionCount;
    }
    
    public void incrementCompletionCount() {
        this.completionCount++;
    }
    
    // ===== Objective Progress =====
    
    /**
     * Gets the current progress for an objective.
     */
    public int getObjectiveProgress(String objectiveId) {
        return objectiveProgress.getOrDefault(objectiveId, 0);
    }
    
    /**
     * Sets the progress for an objective.
     */
    public void setObjectiveProgress(String objectiveId, int progress) {
        objectiveProgress.put(objectiveId, Math.max(0, progress));
    }
    
    /**
     * Increments the progress for an objective by 1.
     * @return The new progress value
     */
    public int incrementObjectiveProgress(String objectiveId) {
        int newProgress = getObjectiveProgress(objectiveId) + 1;
        objectiveProgress.put(objectiveId, newProgress);
        return newProgress;
    }
    
    /**
     * Increments the progress for an objective by a specified amount.
     * @return The new progress value
     */
    public int incrementObjectiveProgress(String objectiveId, int amount) {
        int newProgress = getObjectiveProgress(objectiveId) + amount;
        objectiveProgress.put(objectiveId, Math.max(0, newProgress));
        return newProgress;
    }
    
    /**
     * Gets all objective progress as a map.
     */
    public Map<String, Integer> getAllObjectiveProgress() {
        return Collections.unmodifiableMap(objectiveProgress);
    }
    
    // ===== Completed Objectives =====
    
    /**
     * Marks an objective as completed.
     */
    public void markObjectiveCompleted(String objectiveId) {
        completedObjectives.add(objectiveId);
    }
    
    /**
     * Checks if an objective is completed.
     */
    public boolean isObjectiveCompleted(String objectiveId) {
        return completedObjectives.contains(objectiveId);
    }
    
    /**
     * Gets all completed objective IDs.
     */
    public Set<String> getCompletedObjectives() {
        return Collections.unmodifiableSet(completedObjectives);
    }
    
    /**
     * Gets the number of completed objectives.
     */
    public int getCompletedObjectiveCount() {
        return completedObjectives.size();
    }
    
    // ===== Status Helpers =====
    
    /**
     * Checks if the quest is active (in progress).
     */
    public boolean isActive() {
        return status == QuestStatus.IN_PROGRESS;
    }
    
    /**
     * Checks if the quest is completed.
     */
    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }
    
    /**
     * Checks if the quest is failed.
     */
    public boolean isFailed() {
        return status == QuestStatus.FAILED;
    }
    
    /**
     * Checks if the quest can be started (not started or repeatable and completed).
     */
    public boolean canStart(Quest quest) {
        if (status == QuestStatus.NOT_STARTED) {
            return true;
        }
        if (quest.isRepeatable() && status == QuestStatus.COMPLETED) {
            // Check cooldown
            if (quest.getCooldownTicks() > 0) {
                long currentTime = System.currentTimeMillis();
                long cooldownMs = quest.getCooldownTicks() * 50L; // 50ms per tick
                return (currentTime - completeTime) >= cooldownMs;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Resets progress for a repeatable quest.
     */
    public void reset() {
        objectiveProgress.clear();
        completedObjectives.clear();
        status = QuestStatus.NOT_STARTED;
        startTime = 0;
        completeTime = 0;
    }
    
    /**
     * Calculates the completion percentage based on a quest's objectives.
     */
    public float getCompletionPercentage(Quest quest) {
        if (quest == null || quest.getObjectives().isEmpty()) {
            return 0f;
        }
        
        int totalRequired = 0;
        int totalProgress = 0;
        
        for (QuestObjective objective : quest.getObjectives()) {
            if (!objective.isOptional()) {
                totalRequired += objective.getTargetCount();
                totalProgress += Math.min(
                    getObjectiveProgress(objective.getId()),
                    objective.getTargetCount()
                );
            }
        }
        
        if (totalRequired == 0) {
            return isCompleted() ? 100f : 0f;
        }
        
        return (totalProgress * 100f) / totalRequired;
    }
    
    /**
     * Creates a serializable data object for persistence.
     */
    public QuestProgressData toData() {
        QuestProgressData data = new QuestProgressData();
        data.questId = questId;
        data.playerId = playerId.toString();
        data.status = status.name();
        data.objectiveProgress = new HashMap<>(objectiveProgress);
        data.completedObjectives = new ArrayList<>(completedObjectives);
        data.startTime = startTime;
        data.completeTime = completeTime;
        data.completionCount = completionCount;
        return data;
    }
    
    /**
     * Creates a QuestProgress from serialized data.
     */
    public static QuestProgress fromData(QuestProgressData data) {
        QuestProgress progress = new QuestProgress(data.questId, UUID.fromString(data.playerId));
        progress.status = QuestStatus.valueOf(data.status);
        progress.objectiveProgress.putAll(data.objectiveProgress);
        progress.completedObjectives.addAll(data.completedObjectives);
        progress.startTime = data.startTime;
        progress.completeTime = data.completeTime;
        progress.completionCount = data.completionCount;
        return progress;
    }
    
    /**
     * Data class for JSON serialization.
     */
    public static class QuestProgressData {
        public String questId;
        public String playerId;
        public String status;
        public Map<String, Integer> objectiveProgress;
        public List<String> completedObjectives;
        public long startTime;
        public long completeTime;
        public int completionCount;
    }
}
