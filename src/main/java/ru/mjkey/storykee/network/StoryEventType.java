package ru.mjkey.storykee.network;

/**
 * Enumeration of story event types that can be synchronized across the network.
 * 
 * Requirements: 31.1, 31.2
 */
public enum StoryEventType {
    // NPC events
    NPC_SPAWN,
    NPC_DESPAWN,
    NPC_UPDATE,
    NPC_INTERACT,
    
    // Quest events
    QUEST_START,
    QUEST_UPDATE,
    QUEST_COMPLETE,
    QUEST_OBJECTIVE_UPDATE,
    
    // Dialogue events
    DIALOGUE_START,
    DIALOGUE_UPDATE,
    DIALOGUE_END,
    
    // Variable events
    VARIABLE_UPDATE,
    
    // Region events
    REGION_ENTER,
    REGION_EXIT,
    
    // Boss events
    BOSS_SPAWN,
    BOSS_PHASE_CHANGE,
    BOSS_DEFEAT,
    
    // Cutscene events
    CUTSCENE_START,
    CUTSCENE_END,
    
    // HUD events
    HUD_SHOW,
    HUD_HIDE,
    HUD_UPDATE,
    
    // Audio events
    SOUND_PLAY,
    MUSIC_PLAY,
    MUSIC_STOP,
    
    // Particle events
    PARTICLE_SPAWN,
    
    // Hologram events
    HOLOGRAM_CREATE,
    HOLOGRAM_UPDATE,
    HOLOGRAM_REMOVE,
    
    // Generic story event
    CUSTOM_EVENT
}
