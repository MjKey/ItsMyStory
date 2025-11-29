package ru.mjkey.storykee.systems.dialogue;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a choice option in a dialogue.
 * Choices can have conditions that determine visibility and actions that execute when selected.
 */
public class DialogueChoice {
    
    private final String id;
    private final String text;
    private final String nextNodeId;
    private Predicate<UUID> condition;
    private Consumer<UUID> action;
    
    public DialogueChoice(String id, String text, String nextNodeId) {
        this.id = id;
        this.text = text;
        this.nextNodeId = nextNodeId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getText() {
        return text;
    }
    
    public String getNextNodeId() {
        return nextNodeId;
    }
    
    public Predicate<UUID> getCondition() {
        return condition;
    }
    
    public void setCondition(Predicate<UUID> condition) {
        this.condition = condition;
    }
    
    public Consumer<UUID> getAction() {
        return action;
    }
    
    public void setAction(Consumer<UUID> action) {
        this.action = action;
    }
    
    /**
     * Checks if this choice is available for the given player.
     */
    public boolean isAvailable(UUID playerId) {
        return condition == null || condition.test(playerId);
    }
    
    /**
     * Executes the action associated with this choice.
     */
    public void executeAction(UUID playerId) {
        if (action != null) {
            action.accept(playerId);
        }
    }
    
    /**
     * Builder for creating DialogueChoice instances.
     */
    public static class Builder {
        private String id;
        private String text;
        private String nextNodeId;
        private Predicate<UUID> condition;
        private Consumer<UUID> action;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder nextNodeId(String nextNodeId) {
            this.nextNodeId = nextNodeId;
            return this;
        }
        
        public Builder condition(Predicate<UUID> condition) {
            this.condition = condition;
            return this;
        }
        
        public Builder action(Consumer<UUID> action) {
            this.action = action;
            return this;
        }
        
        public DialogueChoice build() {
            DialogueChoice choice = new DialogueChoice(id, text, nextNodeId);
            choice.setCondition(condition);
            choice.setAction(action);
            return choice;
        }
    }
}
