package ru.mjkey.storykee.systems.quest;

import java.util.*;

/**
 * Represents a complete quest with objectives and rewards.
 * Quests are registered with the QuestManager and can be started by scripts.
 * Requirements: 8.1, 8.2
 */
public class Quest {
    
    private final String id;
    private String title;
    private String description;
    private final List<QuestObjective> objectives;
    private final Map<String, Object> rewards;
    private final List<String> prerequisites;
    private String scriptId;
    private boolean repeatable;
    private int cooldownTicks;
    private String onCompleteCallback;
    
    public Quest(String id) {
        this.id = id;
        this.objectives = new ArrayList<>();
        this.rewards = new LinkedHashMap<>();
        this.prerequisites = new ArrayList<>();
        this.repeatable = false;
        this.cooldownTicks = 0;
    }
    
    public Quest(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.objectives = new ArrayList<>();
        this.rewards = new LinkedHashMap<>();
        this.prerequisites = new ArrayList<>();
        this.repeatable = false;
        this.cooldownTicks = 0;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }
    
    public boolean isRepeatable() {
        return repeatable;
    }
    
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }
    
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    public void setCooldownTicks(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }
    
    public String getOnCompleteCallback() {
        return onCompleteCallback;
    }
    
    public void setOnCompleteCallback(String onCompleteCallback) {
        this.onCompleteCallback = onCompleteCallback;
    }
    
    // ===== Objectives =====
    
    public void addObjective(QuestObjective objective) {
        objectives.add(objective);
    }
    
    public void removeObjective(String objectiveId) {
        objectives.removeIf(obj -> obj.getId().equals(objectiveId));
    }
    
    public QuestObjective getObjective(String objectiveId) {
        return objectives.stream()
            .filter(obj -> obj.getId().equals(objectiveId))
            .findFirst()
            .orElse(null);
    }
    
    public List<QuestObjective> getObjectives() {
        return Collections.unmodifiableList(objectives);
    }
    
    public int getObjectiveCount() {
        return objectives.size();
    }
    
    public int getRequiredObjectiveCount() {
        return (int) objectives.stream()
            .filter(obj -> !obj.isOptional())
            .count();
    }
    
    // ===== Rewards =====
    
    public void setReward(String key, Object value) {
        rewards.put(key, value);
    }
    
    public Object getReward(String key) {
        return rewards.get(key);
    }
    
    public Map<String, Object> getRewards() {
        return Collections.unmodifiableMap(rewards);
    }
    
    public boolean hasRewards() {
        return !rewards.isEmpty();
    }
    
    // ===== Prerequisites =====
    
    public void addPrerequisite(String questId) {
        if (!prerequisites.contains(questId)) {
            prerequisites.add(questId);
        }
    }
    
    public void removePrerequisite(String questId) {
        prerequisites.remove(questId);
    }
    
    public List<String> getPrerequisites() {
        return Collections.unmodifiableList(prerequisites);
    }
    
    public boolean hasPrerequisites() {
        return !prerequisites.isEmpty();
    }
    
    // ===== Validation =====
    
    /**
     * Validates the quest structure.
     * @return List of validation errors, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (id == null || id.isEmpty()) {
            errors.add("Quest has no ID");
        }
        
        if (title == null || title.isEmpty()) {
            errors.add("Quest has no title");
        }
        
        if (objectives.isEmpty()) {
            errors.add("Quest has no objectives");
        }
        
        // Check for duplicate objective IDs
        Set<String> objectiveIds = new HashSet<>();
        for (QuestObjective objective : objectives) {
            if (objective.getId() == null) {
                errors.add("Objective has no ID");
            } else if (!objectiveIds.add(objective.getId())) {
                errors.add("Duplicate objective ID: " + objective.getId());
            }
        }
        
        // Check that at least one objective is required
        if (getRequiredObjectiveCount() == 0 && !objectives.isEmpty()) {
            errors.add("Quest has no required objectives");
        }
        
        return errors;
    }
    
    /**
     * Builder for creating Quest instances.
     */
    public static class Builder {
        private String id;
        private String title;
        private String description;
        private String scriptId;
        private boolean repeatable = false;
        private int cooldownTicks = 0;
        private String onCompleteCallback;
        private final List<QuestObjective> objectives = new ArrayList<>();
        private final Map<String, Object> rewards = new LinkedHashMap<>();
        private final List<String> prerequisites = new ArrayList<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder scriptId(String scriptId) {
            this.scriptId = scriptId;
            return this;
        }
        
        public Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }
        
        public Builder cooldownTicks(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }
        
        public Builder onCompleteCallback(String callback) {
            this.onCompleteCallback = callback;
            return this;
        }
        
        public Builder addObjective(QuestObjective objective) {
            this.objectives.add(objective);
            return this;
        }
        
        public Builder reward(String key, Object value) {
            this.rewards.put(key, value);
            return this;
        }
        
        public Builder prerequisite(String questId) {
            this.prerequisites.add(questId);
            return this;
        }
        
        public Quest build() {
            Quest quest = new Quest(id, title, description);
            quest.setScriptId(scriptId);
            quest.setRepeatable(repeatable);
            quest.setCooldownTicks(cooldownTicks);
            quest.setOnCompleteCallback(onCompleteCallback);
            objectives.forEach(quest::addObjective);
            rewards.forEach(quest::setReward);
            prerequisites.forEach(quest::addPrerequisite);
            return quest;
        }
    }
}
