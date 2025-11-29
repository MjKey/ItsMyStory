package ru.mjkey.storykee.systems.quest;

/**
 * Types of quest objectives that can be tracked.
 * Requirements: 8.2
 */
public enum ObjectiveType {
    /**
     * Kill a specific entity type or named entity.
     * Criteria: entityType, entityName (optional), count
     */
    KILL_ENTITY,
    
    /**
     * Collect a specific item.
     * Criteria: itemId, count
     */
    COLLECT_ITEM,
    
    /**
     * Reach a specific location or region.
     * Criteria: x, y, z, radius (optional), dimension (optional)
     */
    REACH_LOCATION,
    
    /**
     * Interact with a specific NPC.
     * Criteria: npcId
     */
    INTERACT_WITH_NPC,
    
    /**
     * Interact with a specific block.
     * Criteria: blockType, x, y, z (optional for specific block)
     */
    INTERACT_WITH_BLOCK,
    
    /**
     * Complete a dialogue.
     * Criteria: dialogueId
     */
    COMPLETE_DIALOGUE,
    
    /**
     * Custom objective tracked by script.
     * Criteria: defined by script
     */
    CUSTOM
}
