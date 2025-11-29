package ru.mjkey.storykee.systems.dialogue;

import java.util.UUID;

/**
 * Represents the current state of a dialogue for a specific player.
 * Tracks which dialogue is active and which node the player is currently viewing.
 */
public class DialogueState {
    
    private final UUID playerId;
    private final String dialogueId;
    private String currentNodeId;
    private long startTime;
    private boolean completed;
    
    public DialogueState(UUID playerId, String dialogueId, String startNodeId) {
        this.playerId = playerId;
        this.dialogueId = dialogueId;
        this.currentNodeId = startNodeId;
        this.startTime = System.currentTimeMillis();
        this.completed = false;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getDialogueId() {
        return dialogueId;
    }
    
    public String getCurrentNodeId() {
        return currentNodeId;
    }
    
    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Advances to the next node.
     */
    public void advanceTo(String nextNodeId) {
        this.currentNodeId = nextNodeId;
        if (nextNodeId == null) {
            this.completed = true;
        }
    }
}
