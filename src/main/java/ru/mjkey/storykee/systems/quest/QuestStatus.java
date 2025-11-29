package ru.mjkey.storykee.systems.quest;

/**
 * Status of a quest for a player.
 * Requirements: 8.1, 8.4
 */
public enum QuestStatus {
    /**
     * Quest is not yet started.
     */
    NOT_STARTED,
    
    /**
     * Quest is active and in progress.
     */
    IN_PROGRESS,
    
    /**
     * Quest is completed successfully.
     */
    COMPLETED,
    
    /**
     * Quest was failed.
     */
    FAILED,
    
    /**
     * Quest was abandoned by the player.
     */
    ABANDONED
}
