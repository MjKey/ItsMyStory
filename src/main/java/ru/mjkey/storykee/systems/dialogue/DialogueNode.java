package ru.mjkey.storykee.systems.dialogue;

import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single node in a dialogue tree.
 * A node contains text to display and optionally choices for the player.
 */
public class DialogueNode {
    
    private final String id;
    private String text;
    private String speakerName;
    private String speakerTexture;
    private String nextNodeId;
    private final List<DialogueChoice> choices;
    private final List<ExpressionNode> actions;
    
    public DialogueNode(String id) {
        this.id = id;
        this.choices = new ArrayList<>();
        this.actions = new ArrayList<>();
    }
    
    public DialogueNode(String id, String text) {
        this.id = id;
        this.text = text;
        this.choices = new ArrayList<>();
        this.actions = new ArrayList<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getSpeakerName() {
        return speakerName;
    }
    
    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }
    
    public String getSpeakerTexture() {
        return speakerTexture;
    }
    
    public void setSpeakerTexture(String speakerTexture) {
        this.speakerTexture = speakerTexture;
    }
    
    public String getNextNodeId() {
        return nextNodeId;
    }
    
    public void setNextNodeId(String nextNodeId) {
        this.nextNodeId = nextNodeId;
    }
    
    public List<DialogueChoice> getChoices() {
        return Collections.unmodifiableList(choices);
    }
    
    public void addChoice(DialogueChoice choice) {
        choices.add(choice);
    }
    
    public void removeChoice(String choiceId) {
        choices.removeIf(c -> c.getId().equals(choiceId));
    }
    
    public boolean hasChoices() {
        return !choices.isEmpty();
    }
    
    // ===== Actions =====
    
    public void addAction(ExpressionNode action) {
        actions.add(action);
    }
    
    public List<ExpressionNode> getActions() {
        return Collections.unmodifiableList(actions);
    }
    
    public boolean hasActions() {
        return !actions.isEmpty();
    }
    
    /**
     * Checks if this is a terminal node (no next node and no choices).
     */
    public boolean isTerminal() {
        return nextNodeId == null && choices.isEmpty();
    }
    
    /**
     * Builder for creating DialogueNode instances.
     */
    public static class Builder {
        private String id;
        private String text;
        private String speakerName;
        private String speakerTexture;
        private String nextNodeId;
        private final List<DialogueChoice> choices = new ArrayList<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder speakerName(String speakerName) {
            this.speakerName = speakerName;
            return this;
        }
        
        public Builder speakerTexture(String speakerTexture) {
            this.speakerTexture = speakerTexture;
            return this;
        }
        
        public Builder nextNodeId(String nextNodeId) {
            this.nextNodeId = nextNodeId;
            return this;
        }
        
        public Builder addChoice(DialogueChoice choice) {
            this.choices.add(choice);
            return this;
        }
        
        public DialogueNode build() {
            DialogueNode node = new DialogueNode(id, text);
            node.setSpeakerName(speakerName);
            node.setSpeakerTexture(speakerTexture);
            node.setNextNodeId(nextNodeId);
            for (DialogueChoice choice : choices) {
                node.addChoice(choice);
            }
            return node;
        }
    }
}
