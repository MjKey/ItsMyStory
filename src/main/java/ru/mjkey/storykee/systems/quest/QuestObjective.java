package ru.mjkey.storykee.systems.quest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single objective within a quest.
 * Objectives track specific goals that players must complete.
 * Requirements: 8.2
 */
public class QuestObjective {
    
    private final String id;
    private String description;
    private ObjectiveType type;
    private int targetCount;
    private final Map<String, Object> criteria;
    private boolean optional;
    private boolean hidden;
    
    public QuestObjective(String id) {
        this.id = id;
        this.targetCount = 1;
        this.criteria = new HashMap<>();
        this.optional = false;
        this.hidden = false;
    }
    
    public QuestObjective(String id, String description, ObjectiveType type, int targetCount) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.targetCount = targetCount;
        this.criteria = new HashMap<>();
        this.optional = false;
        this.hidden = false;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public ObjectiveType getType() {
        return type;
    }
    
    public void setType(ObjectiveType type) {
        this.type = type;
    }
    
    public int getTargetCount() {
        return targetCount;
    }
    
    public void setTargetCount(int targetCount) {
        this.targetCount = Math.max(1, targetCount);
    }
    
    public Map<String, Object> getCriteria() {
        return Collections.unmodifiableMap(criteria);
    }
    
    public void setCriterion(String key, Object value) {
        criteria.put(key, value);
    }
    
    public Object getCriterion(String key) {
        return criteria.get(key);
    }
    
    public boolean hasCriterion(String key) {
        return criteria.containsKey(key);
    }
    
    public boolean isOptional() {
        return optional;
    }
    
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    /**
     * Builder for creating QuestObjective instances.
     */
    public static class Builder {
        private String id;
        private String description;
        private ObjectiveType type = ObjectiveType.CUSTOM;
        private int targetCount = 1;
        private final Map<String, Object> criteria = new HashMap<>();
        private boolean optional = false;
        private boolean hidden = false;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder type(ObjectiveType type) {
            this.type = type;
            return this;
        }
        
        public Builder targetCount(int count) {
            this.targetCount = count;
            return this;
        }
        
        public Builder criterion(String key, Object value) {
            this.criteria.put(key, value);
            return this;
        }
        
        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }
        
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }
        
        public QuestObjective build() {
            QuestObjective objective = new QuestObjective(id, description, type, targetCount);
            criteria.forEach(objective::setCriterion);
            objective.setOptional(optional);
            objective.setHidden(hidden);
            return objective;
        }
    }
}
