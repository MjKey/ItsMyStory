package ru.mjkey.storykee.systems.particle;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages particle effects in the Storykee system.
 * Handles particle spawning with custom properties.
 * 
 * Requirements: 20.1, 20.2, 20.3, 20.4, 20.5
 */
public class ParticleManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleManager.class);
    
    private static ParticleManager instance;
    
    // Active particle effects (for tracking and cleanup)
    private final Map<String, ActiveParticleOptions> activeEffects;
    
    // Custom particle type mappings
    private final Map<String, ParticleOptions> customParticles;
    
    private MinecraftServer server;
    
    private ParticleManager() {
        this.activeEffects = new ConcurrentHashMap<>();
        this.customParticles = new ConcurrentHashMap<>();
    }
    
    public static ParticleManager getInstance() {
        if (instance == null) {
            instance = new ParticleManager();
        }
        return instance;
    }
    
    /**
     * Initializes the particle manager.
     * 
     * @param server Minecraft server
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("ParticleManager initialized");
    }
    
    /**
     * Shuts down the particle manager.
     */
    public void shutdown() {
        activeEffects.clear();
        customParticles.clear();
        LOGGER.info("ParticleManager shutdown");
    }
    
    // ==================== Particle Spawning ====================
    
    /**
     * Spawns particles at a location.
     * Requirement 20.1: Create particles at specified location
     * 
     * @param world Server world
     * @param particleType Particle type ResourceLocation
     * @param position World position
     * @param properties Particle properties
     */
    public void spawnParticles(ServerLevel world, String particleType, Vec3 position, ParticleProperties properties) {
        if (world == null || position == null) {
            return;
        }
        
        ParticleOptions effect = getParticleOptions(particleType, properties);
        if (effect == null) {
            LOGGER.warn("Unknown particle type: {}", particleType);
            return;
        }
        
        int count = properties != null ? properties.getCount() : 1;
        Vec3 spread = properties != null ? properties.getSpread() : Vec3.ZERO;
        Vec3 velocity = properties != null ? properties.getVelocity() : Vec3.ZERO;
        
        // Spawn particles for all nearby players
        // Requirement 20.3: Render for all nearby players
        world.sendParticles(
                effect,
                position.x, position.y, position.z,
                count,
                spread.x, spread.y, spread.z,
                velocity.length()
        );
        
        LOGGER.debug("Spawned {} particles of type '{}' at {}", count, particleType, position);
    }
    
    /**
     * Spawns particles at a location for a specific player only.
     * 
     * @param player Target player
     * @param particleType Particle type ResourceLocation
     * @param position World position
     * @param properties Particle properties
     */
    public void spawnParticlesForPlayer(ServerPlayer player, String particleType, 
                                         Vec3 position, ParticleProperties properties) {
        if (player == null || position == null) {
            return;
        }
        
        ParticleOptions effect = getParticleOptions(particleType, properties);
        if (effect == null) {
            LOGGER.warn("Unknown particle type: {}", particleType);
            return;
        }
        
        int count = properties != null ? properties.getCount() : 1;
        Vec3 spread = properties != null ? properties.getSpread() : Vec3.ZERO;
        Vec3 velocity = properties != null ? properties.getVelocity() : Vec3.ZERO;
        
        ((ServerLevel)player.level()).sendParticles(
                effect,
                position.x, position.y, position.z,
                count,
                spread.x, spread.y, spread.z,
                velocity.length()
        );
    }
    
    /**
     * Creates a repeating particle effect.
     * 
     * @param effectId Unique effect ID
     * @param world Server world
     * @param particleType Particle type
     * @param position Position
     * @param properties Properties
     * @param intervalTicks Spawn interval in ticks
     * @return true if effect was created
     */
    public boolean createRepeatingEffect(String effectId, ServerLevel world, String particleType,
                                          Vec3 position, ParticleProperties properties, int intervalTicks) {
        if (activeEffects.containsKey(effectId)) {
            LOGGER.warn("Particle effect '{}' already exists", effectId);
            return false;
        }
        
        ActiveParticleOptions effect = new ActiveParticleOptions(
                effectId, world, particleType, position, properties, intervalTicks);
        activeEffects.put(effectId, effect);
        
        LOGGER.debug("Created repeating particle effect: {}", effectId);
        return true;
    }
    
    /**
     * Stops and removes a repeating particle effect.
     * Requirement 20.4: Clean up resources automatically
     * 
     * @param effectId Effect ID
     * @return true if effect was removed
     */
    public boolean stopEffect(String effectId) {
        ActiveParticleOptions removed = activeEffects.remove(effectId);
        if (removed != null) {
            LOGGER.debug("Stopped particle effect: {}", effectId);
            return true;
        }
        return false;
    }
    
    /**
     * Updates all active particle effects.
     * Should be called every tick.
     */
    public void tick() {
        for (ActiveParticleOptions effect : activeEffects.values()) {
            effect.tick(this);
        }
    }
    
    // ==================== Particle Type Resolution ====================
    
    /**
     * Gets a particle effect for the given type and properties.
     * Requirement 20.2: Apply color, size, velocity, and lifetime settings
     */
    private ParticleOptions getParticleOptions(String particleType, ParticleProperties properties) {
        // Check custom particles first
        if (customParticles.containsKey(particleType)) {
            return customParticles.get(particleType);
        }
        
        // Handle dust particles with custom color
        if (particleType.equals("dust") || particleType.equals("minecraft:dust")) {
            if (properties != null) {
                float r = properties.getRed() / 255.0f;
                float g = properties.getGreen() / 255.0f;
                float b = properties.getBlue() / 255.0f;
                float size = properties.getSize();
                return new DustParticleOptions(((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255), size);
            }
            return new DustParticleOptions(0xFFFFFF, 1.0f);
        }
        
        // Try to resolve from registry
        ResourceLocation id = ResourceLocation.tryParse(particleType);
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath("minecraft", particleType);
        }
        
        var particleTypeObj = BuiltInRegistries.PARTICLE_TYPE.getValue(id);
        if (particleTypeObj != null && particleTypeObj instanceof ParticleOptions effect) {
            return effect;
        }
        
        // Fallback to common particle types
        return switch (particleType.toLowerCase()) {
            case "flame", "minecraft:flame" -> ParticleTypes.FLAME;
            case "smoke", "minecraft:smoke" -> ParticleTypes.SMOKE;
            case "heart", "minecraft:heart" -> ParticleTypes.HEART;
            case "crit", "minecraft:crit" -> ParticleTypes.CRIT;
            case "enchant", "minecraft:enchant" -> ParticleTypes.ENCHANT;
            case "portal", "minecraft:portal" -> ParticleTypes.PORTAL;
            case "explosion", "minecraft:explosion" -> ParticleTypes.EXPLOSION;
            case "cloud", "minecraft:cloud" -> ParticleTypes.CLOUD;
            case "spark", "minecraft:electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "bubble", "minecraft:bubble" -> ParticleTypes.BUBBLE;
            case "splash", "minecraft:splash" -> ParticleTypes.SPLASH;
            case "note", "minecraft:note" -> ParticleTypes.NOTE;
            case "happy_villager", "minecraft:happy_villager" -> ParticleTypes.HAPPY_VILLAGER;
            case "angry_villager", "minecraft:angry_villager" -> ParticleTypes.ANGRY_VILLAGER;
            case "witch", "minecraft:witch" -> ParticleTypes.WITCH;
            case "drip_water", "minecraft:dripping_water" -> ParticleTypes.DRIPPING_WATER;
            case "drip_lava", "minecraft:dripping_lava" -> ParticleTypes.DRIPPING_LAVA;
            case "snowflake", "minecraft:snowflake" -> ParticleTypes.SNOWFLAKE;
            case "soul_fire_flame", "minecraft:soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "end_rod", "minecraft:end_rod" -> ParticleTypes.END_ROD;
            case "totem", "minecraft:totem_of_undying" -> ParticleTypes.TOTEM_OF_UNDYING;
            default -> null;
        };
    }
    
    /**
     * Registers a custom particle type.
     * Requirement 20.5: Load custom textures from assets
     * 
     * @param name Custom particle name
     * @param effect Particle effect
     */
    public void registerCustomParticle(String name, ParticleOptions effect) {
        customParticles.put(name, effect);
        LOGGER.debug("Registered custom particle: {}", name);
    }
    
    /**
     * Unregisters a custom particle type.
     * 
     * @param name Custom particle name
     */
    public void unregisterCustomParticle(String name) {
        customParticles.remove(name);
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Gets all active effect IDs.
     */
    public Set<String> getActiveEffectIds() {
        return Collections.unmodifiableSet(activeEffects.keySet());
    }
    
    /**
     * Checks if an effect is active.
     */
    public boolean isEffectActive(String effectId) {
        return activeEffects.containsKey(effectId);
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents an active repeating particle effect.
     */
    private static class ActiveParticleOptions {
        final String id;
        final ServerLevel world;
        final String particleType;
        final Vec3 position;
        final ParticleProperties properties;
        final int intervalTicks;
        int tickCounter;
        
        ActiveParticleOptions(String id, ServerLevel world, String particleType,
                            Vec3 position, ParticleProperties properties, int intervalTicks) {
            this.id = id;
            this.world = world;
            this.particleType = particleType;
            this.position = position;
            this.properties = properties;
            this.intervalTicks = intervalTicks;
            this.tickCounter = 0;
        }
        
        void tick(ParticleManager manager) {
            tickCounter++;
            if (tickCounter >= intervalTicks) {
                tickCounter = 0;
                manager.spawnParticles(world, particleType, position, properties);
            }
        }
    }
}
