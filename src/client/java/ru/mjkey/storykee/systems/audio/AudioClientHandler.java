package ru.mjkey.storykee.systems.audio;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.ItsMyStory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side audio handler for advanced sound features.
 * Handles music playback with fading and client-side sound effects.
 * 
 * Requirements: 13.1, 13.2, 13.3, 13.4
 */
@Environment(EnvType.CLIENT)
public class AudioClientHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioClientHandler.class);
    
    // Current music instance being played
    private static SoundInstance currentMusic = null;
    private static String currentMusicId = null;
    
    // Music fade state
    private static boolean isFading = false;
    private static float fadeTargetVolume = 0.0f;
    private static float fadeCurrentVolume = 1.0f;
    private static float fadeSpeed = 0.0f;
    private static Runnable onFadeComplete = null;
    
    // Pending music to play after fade out
    private static String pendingMusicId = null;
    private static boolean pendingMusicLoop = false;
    private static float pendingMusicFadeIn = 0.0f;
    
    // Volume settings (synced from server)
    private static float masterVolume = 1.0f;
    private static float soundVolume = 1.0f;
    private static float musicVolume = 1.0f;
    
    // Active sound instances for tracking
    private static final Map<String, SoundInstance> activeSounds = new ConcurrentHashMap<>();
    
    /**
     * Registers client-side audio handlers.
     */
    public static void register() {
        // Register tick event for fade processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null) {
                tick();
            }
        });
        
        LOGGER.info("AudioClientHandler registered");
    }

    
    /**
     * Called every client tick to process fading.
     */
    private static void tick() {
        if (!isFading) {
            return;
        }
        
        // Update fade volume
        if (fadeTargetVolume > fadeCurrentVolume) {
            fadeCurrentVolume = Math.min(fadeTargetVolume, fadeCurrentVolume + fadeSpeed);
        } else if (fadeTargetVolume < fadeCurrentVolume) {
            fadeCurrentVolume = Math.max(fadeTargetVolume, fadeCurrentVolume - fadeSpeed);
        }
        
        // Check if fade is complete
        if (Math.abs(fadeCurrentVolume - fadeTargetVolume) < 0.01f) {
            fadeCurrentVolume = fadeTargetVolume;
            isFading = false;
            
            if (onFadeComplete != null) {
                onFadeComplete.run();
                onFadeComplete = null;
            }
            
            // If we faded out and have pending music, start it
            if (fadeTargetVolume == 0.0f && pendingMusicId != null) {
                playMusicInternal(pendingMusicId, pendingMusicLoop, pendingMusicFadeIn);
                pendingMusicId = null;
            }
        }
    }
    
    /**
     * Plays a sound at the player's position.
     * 
     * @param soundId The sound ResourceLocation
     * @param volume The volume (0.0 to 1.0)
     * @param pitch The pitch (0.5 to 2.0)
     */
    public static void playSound(String soundId, float volume, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        
        SoundEvent soundEvent = getSoundEvent(soundId);
        if (soundEvent == null) {
            LOGGER.warn("Sound not found: {}", soundId);
            return;
        }
        
        float finalVolume = volume * soundVolume * masterVolume;
        
        SoundInstance instance = SimpleSoundInstance.forUI(soundEvent, pitch, finalVolume);
        client.getSoundManager().play(instance);
        
        LOGGER.debug("Playing sound: {} (volume: {}, pitch: {})", soundId, finalVolume, pitch);
    }
    
    /**
     * Plays music with optional fade-in.
     * If music is already playing, it will fade out first.
     * 
     * @param musicId The music ResourceLocation
     * @param loop Whether to loop the music
     * @param fadeInSeconds Fade-in duration in seconds
     */
    public static void playMusic(String musicId, boolean loop, float fadeInSeconds) {
        // If music is currently playing, fade it out first
        if (currentMusic != null && currentMusicId != null) {
            pendingMusicId = musicId;
            pendingMusicLoop = loop;
            pendingMusicFadeIn = fadeInSeconds;
            stopMusic(0.5f); // Quick fade out
            return;
        }
        
        playMusicInternal(musicId, loop, fadeInSeconds);
    }
    
    /**
     * Internal method to play music.
     */
    private static void playMusicInternal(String musicId, boolean loop, float fadeInSeconds) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        
        SoundEvent soundEvent = getSoundEvent(musicId);
        if (soundEvent == null) {
            LOGGER.warn("Music not found: {}", musicId);
            return;
        }
        
        // Create music instance
        float initialVolume = fadeInSeconds > 0 ? 0.0f : musicVolume * masterVolume;
        
        currentMusic = new MusicSoundInstance(soundEvent, loop, initialVolume);
        currentMusicId = musicId;
        
        client.getSoundManager().play(currentMusic);
        
        // Start fade in if needed
        if (fadeInSeconds > 0) {
            fadeCurrentVolume = 0.0f;
            fadeTargetVolume = musicVolume * masterVolume;
            fadeSpeed = (fadeTargetVolume - fadeCurrentVolume) / (fadeInSeconds * 20); // 20 ticks per second
            isFading = true;
        } else {
            fadeCurrentVolume = musicVolume * masterVolume;
        }
        
        LOGGER.info("Playing music: {} (loop: {}, fadeIn: {}s)", musicId, loop, fadeInSeconds);
    }

    
    /**
     * Stops the current music with optional fade-out.
     * 
     * @param fadeOutSeconds Fade-out duration in seconds (0 for immediate stop)
     */
    public static void stopMusic(float fadeOutSeconds) {
        if (currentMusic == null) {
            return;
        }
        
        if (fadeOutSeconds > 0) {
            // Start fade out
            fadeTargetVolume = 0.0f;
            fadeSpeed = fadeCurrentVolume / (fadeOutSeconds * 20);
            isFading = true;
            onFadeComplete = () -> {
                stopMusicImmediate();
            };
            
            LOGGER.info("Fading out music over {}s", fadeOutSeconds);
        } else {
            stopMusicImmediate();
        }
    }
    
    /**
     * Immediately stops the current music.
     */
    private static void stopMusicImmediate() {
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() != null && currentMusic != null) {
            client.getSoundManager().stop(currentMusic);
        }
        
        currentMusic = null;
        currentMusicId = null;
        isFading = false;
        fadeCurrentVolume = 1.0f;
        
        LOGGER.info("Music stopped");
    }
    
    /**
     * Stops all audio playback.
     */
    public static void stopAll() {
        stopMusicImmediate();
        
        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() != null) {
            for (SoundInstance sound : activeSounds.values()) {
                client.getSoundManager().stop(sound);
            }
        }
        activeSounds.clear();
        
        pendingMusicId = null;
        
        LOGGER.info("All audio stopped");
    }
    
    // ===== Volume Control =====
    
    /**
     * Sets the master volume.
     */
    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * Sets the sound effects volume.
     */
    public static void setSoundVolume(float volume) {
        soundVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * Sets the music volume.
     */
    public static void setMusicVolume(float volume) {
        musicVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    // ===== Utility Methods =====
    
    /**
     * Gets a SoundEvent by ID.
     */
    private static SoundEvent getSoundEvent(String soundId) {
        // Try to get from StorySounds first
        SoundEvent event = StorySounds.get(soundId);
        if (event != null) {
            return event;
        }
        
        // Try Minecraft registry
        ResourceLocation id;
        if (soundId.contains(":")) {
            id = ResourceLocation.parse(soundId);
        } else {
            id = ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, soundId);
        }
        
        return BuiltInRegistries.SOUND_EVENT.get(id)
            .map(ref -> ref.value())
            .orElse(null);
    }
    
    /**
     * Checks if music is currently playing.
     */
    public static boolean isMusicPlaying() {
        return currentMusic != null && currentMusicId != null;
    }
    
    /**
     * Gets the current music ID.
     */
    public static String getCurrentMusicId() {
        return currentMusicId;
    }
    
    /**
     * Gets the current fade volume.
     */
    public static float getCurrentFadeVolume() {
        return fadeCurrentVolume;
    }

    
    // ===== Inner Classes =====
    
    /**
     * Custom SoundInstance for music playback with volume control.
     */
    @Environment(EnvType.CLIENT)
    private static class MusicSoundInstance implements SoundInstance {
        private final SoundEvent soundEvent;
        private final boolean loop;
        private float volume;
        
        public MusicSoundInstance(SoundEvent soundEvent, boolean loop, float volume) {
            this.soundEvent = soundEvent;
            this.loop = loop;
            this.volume = volume;
        }
        
        @Override
        public ResourceLocation getLocation() {
            return soundEvent.location();
        }
        
        @Override
        public WeighedSoundEvents resolve(SoundManager soundManager) {
            return soundManager.getSoundEvent(soundEvent.location());
        }
        
        @Override
        public net.minecraft.client.resources.sounds.Sound getSound() {
            return null; // Let the sound manager resolve this
        }
        
        @Override
        public SoundSource getSource() {
            return SoundSource.MUSIC;
        }
        
        @Override
        public boolean isLooping() {
            return loop;
        }
        
        @Override
        public boolean isRelative() {
            return true; // Music is not positional
        }
        
        @Override
        public int getDelay() {
            return 0;
        }
        
        @Override
        public float getVolume() {
            // Return the current fade volume
            return fadeCurrentVolume;
        }
        
        @Override
        public float getPitch() {
            return 1.0f;
        }
        
        @Override
        public double getX() {
            return 0;
        }
        
        @Override
        public double getY() {
            return 0;
        }
        
        @Override
        public double getZ() {
            return 0;
        }
        
        @Override
        public Attenuation getAttenuation() {
            return Attenuation.NONE; // Music doesn't attenuate
        }
    }
}
