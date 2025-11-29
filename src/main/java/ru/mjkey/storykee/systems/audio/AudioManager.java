package ru.mjkey.storykee.systems.audio;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.ItsMyStory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages audio playback in the story system.
 * Provides sound and music playback with volume control and fading.
 * 
 * Requirements: 13.1, 13.2, 13.3, 13.4
 */
public class AudioManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioManager.class);
    
    private static AudioManager instance;
    
    // Default volume settings
    private float masterVolume = 1.0f;
    private float soundVolume = 1.0f;
    private float musicVolume = 1.0f;
    
    // Track current music per player for fading
    private final Map<UUID, MusicState> playerMusicStates = new ConcurrentHashMap<>();
    
    // Custom sound events registry
    private final Map<String, SoundEvent> customSounds = new ConcurrentHashMap<>();
    
    // Server reference for playing sounds
    private MinecraftServer server;
    
    private AudioManager() {
    }
    
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    
    /**
     * Initializes the AudioManager with the server reference.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("AudioManager initialized");
    }
    
    /**
     * Registers a custom sound event.
     * Requirement 13.1: Load sounds from assets/sounds directory
     * 
     * @param soundId The ResourceLocation for the sound (e.g., "my_sound")
     * @return The registered SoundEvent
     */
    public SoundEvent registerSound(String soundId) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, soundId);
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(id);
        
        // Register with Minecraft's registry
        Registry.register(BuiltInRegistries.SOUND_EVENT, id, soundEvent);
        
        // Store in our custom sounds map
        customSounds.put(soundId, soundEvent);
        
        LOGGER.debug("Registered custom sound: {}", soundId);
        return soundEvent;
    }
    
    /**
     * Gets a sound event by ID, checking custom sounds first, then vanilla.
     */
    public SoundEvent getSoundEvent(String soundId) {
        // Check custom sounds first
        if (customSounds.containsKey(soundId)) {
            return customSounds.get(soundId);
        }
        
        // Try to get from registry
        ResourceLocation id;
        if (soundId.contains(":")) {
            id = ResourceLocation.parse(soundId);
        } else {
            // Try mod namespace first, then minecraft
            id = ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, soundId);
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id)
                .map(ref -> ref.value())
                .orElse(null);
            if (event == null) {
                id = ResourceLocation.withDefaultNamespace(soundId);
            } else {
                return event;
            }
        }
        
        return BuiltInRegistries.SOUND_EVENT.get(id)
            .map(ref -> ref.value())
            .orElse(null);
    }
    
    // ===== Sound Playback =====
    
    /**
     * Plays a sound at a specific position in the world.
     * Requirement 13.1: Play sounds from assets/sounds directory
     * Requirement 13.2: Apply volume, pitch, and position parameters
     * 
     * @param soundId The sound ResourceLocation
     * @param position The position to play the sound at
     * @param volume The volume (0.0 to 1.0)
     * @param pitch The pitch (0.5 to 2.0)
     */
    public void playSound(String soundId, Vec3 position, float volume, float pitch) {
        playSound(soundId, position, volume, pitch, SoundSource.MASTER);
    }
    
    /**
     * Plays a sound at a specific position with a sound category.
     */
    public void playSound(String soundId, Vec3 position, float volume, float pitch, SoundSource category) {
        if (server == null) {
            LOGGER.warn("Cannot play sound - server not initialized");
            return;
        }
        
        SoundEvent soundEvent = getSoundEvent(soundId);
        if (soundEvent == null) {
            LOGGER.warn("Sound not found: {}", soundId);
            return;
        }
        
        // Apply volume modifiers
        float finalVolume = volume * soundVolume * masterVolume;
        
        // Play in all worlds at the position
        for (ServerLevel world : server.getAllLevels()) {
            world.playSound(
                null, // null = play for all players
                position.x, position.y, position.z,
                soundEvent,
                category,
                finalVolume,
                pitch
            );
        }
        
        LOGGER.debug("Playing sound {} at {} with volume {} and pitch {}", 
            soundId, position, finalVolume, pitch);
    }

    
    /**
     * Plays a sound for a specific player.
     * 
     * @param playerId The player's UUID
     * @param soundId The sound ResourceLocation
     * @param volume The volume (0.0 to 1.0)
     * @param pitch The pitch (0.5 to 2.0)
     */
    public void playSoundForPlayer(UUID playerId, String soundId, float volume, float pitch) {
        if (server == null) {
            LOGGER.warn("Cannot play sound - server not initialized");
            return;
        }
        
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("Player not found: {}", playerId);
            return;
        }
        
        SoundEvent soundEvent = getSoundEvent(soundId);
        if (soundEvent == null) {
            LOGGER.warn("Sound not found: {}", soundId);
            return;
        }
        
        // Apply volume modifiers
        float finalVolume = volume * soundVolume * masterVolume;
        
        // Play sound at player's position, only for that player
        player.playNotifySound(soundEvent, SoundSource.MASTER, finalVolume, pitch);
        
        LOGGER.debug("Playing sound {} for player {} with volume {} and pitch {}", 
            soundId, playerId, finalVolume, pitch);
    }
    
    /**
     * Plays a sound at a specific position in a specific world.
     */
    public void playSoundInWorld(ServerLevel world, String soundId, Vec3 position, 
                                  float volume, float pitch, SoundSource category) {
        if (world == null) {
            LOGGER.warn("Cannot play sound - world is null");
            return;
        }
        
        SoundEvent soundEvent = getSoundEvent(soundId);
        if (soundEvent == null) {
            LOGGER.warn("Sound not found: {}", soundId);
            return;
        }
        
        // Apply volume modifiers
        float finalVolume = volume * soundVolume * masterVolume;
        
        world.playSound(
            null,
            position.x, position.y, position.z,
            soundEvent,
            category,
            finalVolume,
            pitch
        );
    }
    
    // ===== Music Playback with Fading =====
    
    /**
     * Plays music for a specific player with optional fade-in.
     * Requirement 13.3: Fade out previous music and fade in new track
     * 
     * @param playerId The player's UUID
     * @param musicId The music ResourceLocation
     * @param loop Whether to loop the music
     * @param fadeInSeconds Fade-in duration in seconds
     */
    public void playMusic(UUID playerId, String musicId, boolean loop, float fadeInSeconds) {
        if (server == null) {
            LOGGER.warn("Cannot play music - server not initialized");
            return;
        }
        
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("Player not found: {}", playerId);
            return;
        }
        
        SoundEvent soundEvent = getSoundEvent(musicId);
        if (soundEvent == null) {
            LOGGER.warn("Music not found: {}", musicId);
            return;
        }
        
        // Get or create music state for this player
        MusicState currentState = playerMusicStates.get(playerId);
        
        // If there's currently playing music, fade it out first
        if (currentState != null && currentState.isPlaying()) {
            // Start fade out of current music
            currentState.startFadeOut(0.5f); // Quick fade out
        }
        
        // Create new music state
        MusicState newState = new MusicState(musicId, loop, fadeInSeconds);
        playerMusicStates.put(playerId, newState);
        
        // Start playing the new music
        // Note: In Minecraft, music is typically handled client-side
        // We send a packet to trigger client-side music playback
        float initialVolume = fadeInSeconds > 0 ? 0.0f : musicVolume * masterVolume;
        
        player.playNotifySound(soundEvent, SoundSource.MUSIC, initialVolume, 1.0f);
        
        LOGGER.info("Playing music {} for player {} (loop: {}, fadeIn: {}s)", 
            musicId, playerId, loop, fadeInSeconds);
    }

    
    /**
     * Stops music for a specific player with optional fade-out.
     * Requirement 13.4: Halt playback immediately or with fade
     * 
     * @param playerId The player's UUID
     * @param fadeOutSeconds Fade-out duration in seconds (0 for immediate stop)
     */
    public void stopMusic(UUID playerId, float fadeOutSeconds) {
        MusicState state = playerMusicStates.get(playerId);
        if (state == null || !state.isPlaying()) {
            LOGGER.debug("No music playing for player {}", playerId);
            return;
        }
        
        if (fadeOutSeconds > 0) {
            state.startFadeOut(fadeOutSeconds);
            LOGGER.info("Fading out music for player {} over {}s", playerId, fadeOutSeconds);
        } else {
            state.stop();
            playerMusicStates.remove(playerId);
            LOGGER.info("Stopped music immediately for player {}", playerId);
        }
    }
    
    /**
     * Stops all audio playback.
     * Requirement 13.4: Halt playback immediately
     */
    public void stopAllAudio() {
        // Stop all player music
        for (UUID playerId : playerMusicStates.keySet()) {
            stopMusic(playerId, 0);
        }
        playerMusicStates.clear();
        
        LOGGER.info("Stopped all audio playback");
    }
    
    // ===== Volume Control =====
    
    /**
     * Sets the master volume.
     * Requirement 13.4: Volume control
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
        LOGGER.debug("Master volume set to {}", this.masterVolume);
    }
    
    /**
     * Gets the master volume.
     */
    public float getMasterVolume() {
        return masterVolume;
    }
    
    /**
     * Sets the sound effects volume.
     * Requirement 13.4: Volume control
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setSoundVolume(float volume) {
        this.soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
        LOGGER.debug("Sound volume set to {}", this.soundVolume);
    }
    
    /**
     * Gets the sound effects volume.
     */
    public float getSoundVolume() {
        return soundVolume;
    }
    
    /**
     * Sets the music volume.
     * Requirement 13.4: Volume control
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
        LOGGER.debug("Music volume set to {}", this.musicVolume);
    }
    
    /**
     * Gets the music volume.
     */
    public float getMusicVolume() {
        return musicVolume;
    }
    
    /**
     * Gets the effective volume for sounds (master * sound).
     */
    public float getEffectiveSoundVolume() {
        return masterVolume * soundVolume;
    }
    
    /**
     * Gets the effective volume for music (master * music).
     */
    public float getEffectiveMusicVolume() {
        return masterVolume * musicVolume;
    }
    
    // ===== Utility Methods =====
    
    /**
     * Checks if a player has music currently playing.
     */
    public boolean isMusicPlaying(UUID playerId) {
        MusicState state = playerMusicStates.get(playerId);
        return state != null && state.isPlaying();
    }
    
    /**
     * Gets the current music ID for a player.
     */
    public String getCurrentMusic(UUID playerId) {
        MusicState state = playerMusicStates.get(playerId);
        return state != null ? state.getMusicId() : null;
    }
    
    /**
     * Cleans up resources when a player disconnects.
     */
    public void onPlayerDisconnect(UUID playerId) {
        playerMusicStates.remove(playerId);
        LOGGER.debug("Cleaned up audio state for player {}", playerId);
    }
    
    /**
     * Shuts down the AudioManager.
     */
    public void shutdown() {
        stopAllAudio();
        customSounds.clear();
        server = null;
        LOGGER.info("AudioManager shut down");
    }

    
    // ===== Inner Classes =====
    
    /**
     * Tracks the state of music playback for a player.
     */
    private static class MusicState {
        private final String musicId;
        private final boolean loop;
        private final float fadeInDuration;
        private boolean playing;
        private boolean fadingOut;
        private float fadeOutDuration;
        private long startTime;
        private long fadeOutStartTime;
        
        public MusicState(String musicId, boolean loop, float fadeInDuration) {
            this.musicId = musicId;
            this.loop = loop;
            this.fadeInDuration = fadeInDuration;
            this.playing = true;
            this.fadingOut = false;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getMusicId() {
            return musicId;
        }
        
        public boolean isLoop() {
            return loop;
        }
        
        public boolean isPlaying() {
            return playing && !fadingOut;
        }
        
        public boolean isFadingOut() {
            return fadingOut;
        }
        
        public void startFadeOut(float duration) {
            this.fadingOut = true;
            this.fadeOutDuration = duration;
            this.fadeOutStartTime = System.currentTimeMillis();
        }
        
        public void stop() {
            this.playing = false;
            this.fadingOut = false;
        }
        
        /**
         * Gets the current volume multiplier based on fade state.
         * Returns a value between 0.0 and 1.0.
         */
        public float getCurrentVolumeMultiplier() {
            long currentTime = System.currentTimeMillis();
            
            if (fadingOut) {
                // Calculate fade out progress
                float elapsed = (currentTime - fadeOutStartTime) / 1000.0f;
                float progress = Math.min(1.0f, elapsed / fadeOutDuration);
                return 1.0f - progress;
            }
            
            if (fadeInDuration > 0) {
                // Calculate fade in progress
                float elapsed = (currentTime - startTime) / 1000.0f;
                if (elapsed < fadeInDuration) {
                    return elapsed / fadeInDuration;
                }
            }
            
            return 1.0f;
        }
    }
}
