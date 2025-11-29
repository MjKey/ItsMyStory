package ru.mjkey.storykee.systems.audio;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.ItsMyStory;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles registration of custom sound events for the story system.
 * Integrates with Minecraft's sound registry.
 * 
 * Requirement 13.1: Load sounds from assets/sounds directory
 */
public class StorySounds {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StorySounds.class);
    
    // Registry of all custom sounds
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
    
    // Pre-defined story sounds that can be used by scripts
    // These are registered at mod initialization
    
    /**
     * Registers all predefined story sounds.
     * Called during mod initialization.
     */
    public static void registerAll() {
        LOGGER.info("Registering story sounds...");
        
        // Note: Custom sounds from story packages are registered dynamically
        // through AudioManager.registerSound() when stories are loaded
        
        LOGGER.info("Story sounds registration complete");
    }
    
    /**
     * Registers a sound event with the given ID.
     * 
     * @param id The sound ID (without namespace)
     * @return The registered SoundEvent
     */
    public static SoundEvent register(String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, id);
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(location);
        
        SoundEvent registered = Registry.register(BuiltInRegistries.SOUND_EVENT, location, soundEvent);
        SOUNDS.put(id, registered);
        
        LOGGER.debug("Registered sound: {}", id);
        return registered;
    }
    
    /**
     * Registers a sound event with a custom namespace.
     * Used for story-specific sounds.
     * 
     * @param namespace The namespace (story ID)
     * @param id The sound ID
     * @return The registered SoundEvent
     */
    public static SoundEvent register(String namespace, String id) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, id);
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(location);
        
        SoundEvent registered = Registry.register(BuiltInRegistries.SOUND_EVENT, location, soundEvent);
        SOUNDS.put(namespace + ":" + id, registered);
        
        LOGGER.debug("Registered sound: {}:{}", namespace, id);
        return registered;
    }
    
    /**
     * Gets a registered sound event by ID.
     * 
     * @param id The sound ID (with or without namespace)
     * @return The SoundEvent, or null if not found
     */
    public static SoundEvent get(String id) {
        // Check our registry first
        if (SOUNDS.containsKey(id)) {
            return SOUNDS.get(id);
        }
        
        // Try to get from Minecraft's registry
        ResourceLocation location;
        if (id.contains(":")) {
            location = ResourceLocation.parse(id);
        } else {
            // Try mod namespace first
            location = ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, id);
            SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(location)
                .map(ref -> ref.value())
                .orElse(null);
            if (event != null) {
                return event;
            }
            // Fall back to vanilla
            location = ResourceLocation.withDefaultNamespace(id);
        }
        
        return BuiltInRegistries.SOUND_EVENT.get(location)
            .map(ref -> ref.value())
            .orElse(null);
    }
    
    /**
     * Checks if a sound is registered.
     * 
     * @param id The sound ID
     * @return true if the sound is registered
     */
    public static boolean isRegistered(String id) {
        return SOUNDS.containsKey(id) || get(id) != null;
    }
    
    /**
     * Gets all registered custom sounds.
     * 
     * @return Map of sound IDs to SoundEvents
     */
    public static Map<String, SoundEvent> getAll() {
        return new HashMap<>(SOUNDS);
    }
    
    /**
     * Clears all custom sounds.
     * Used during hot reload.
     */
    public static void clear() {
        SOUNDS.clear();
        LOGGER.debug("Cleared all custom sounds");
    }
}
