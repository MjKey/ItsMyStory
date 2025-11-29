package ru.mjkey.storykee.systems.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages status effects/potions in the Storykee system.
 * 
 * Requirements: 44.1, 44.2, 44.3, 44.4, 44.5
 */
public class EffectManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EffectManager.class);
    
    private static EffectManager instance;
    
    // Custom effect mappings (name -> effect)
    private final Map<String, Holder<MobEffect>> customEffects;
    
    private MinecraftServer server;
    
    private EffectManager() {
        this.customEffects = new HashMap<>();
    }
    
    public static EffectManager getInstance() {
        if (instance == null) {
            instance = new EffectManager();
        }
        return instance;
    }
    
    /**
     * Initializes the effect manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        registerDefaultEffects();
        LOGGER.info("EffectManager initialized");
    }
    
    /**
     * Shuts down the effect manager.
     */
    public void shutdown() {
        customEffects.clear();
        LOGGER.info("EffectManager shutdown");
    }
    
    /**
     * Registers default effect name mappings.
     */
    private void registerDefaultEffects() {
        // Common effect aliases
        customEffects.put("speed", MobEffects.SPEED);
        customEffects.put("slowness", MobEffects.SLOWNESS);
        customEffects.put("haste", MobEffects.HASTE);
        customEffects.put("mining_fatigue", MobEffects.MINING_FATIGUE);
        customEffects.put("strength", MobEffects.STRENGTH);
        customEffects.put("instant_health", MobEffects.INSTANT_HEALTH);
        customEffects.put("instant_damage", MobEffects.INSTANT_DAMAGE);
        customEffects.put("jump_boost", MobEffects.JUMP_BOOST);
        customEffects.put("nausea", MobEffects.NAUSEA);
        customEffects.put("regeneration", MobEffects.REGENERATION);
        customEffects.put("resistance", MobEffects.RESISTANCE);
        customEffects.put("fire_resistance", MobEffects.FIRE_RESISTANCE);
        customEffects.put("water_breathing", MobEffects.WATER_BREATHING);
        customEffects.put("invisibility", MobEffects.INVISIBILITY);
        customEffects.put("blindness", MobEffects.BLINDNESS);
        customEffects.put("night_vision", MobEffects.NIGHT_VISION);
        customEffects.put("hunger", MobEffects.HUNGER);
        customEffects.put("weakness", MobEffects.WEAKNESS);
        customEffects.put("poison", MobEffects.POISON);
        customEffects.put("wither", MobEffects.WITHER);
        customEffects.put("health_boost", MobEffects.HEALTH_BOOST);
        customEffects.put("absorption", MobEffects.ABSORPTION);
        customEffects.put("saturation", MobEffects.SATURATION);
        customEffects.put("glowing", MobEffects.GLOWING);
        customEffects.put("levitation", MobEffects.LEVITATION);
        customEffects.put("luck", MobEffects.LUCK);
        customEffects.put("unluck", MobEffects.UNLUCK);
        customEffects.put("slow_falling", MobEffects.SLOW_FALLING);
        customEffects.put("conduit_power", MobEffects.CONDUIT_POWER);
        customEffects.put("dolphins_grace", MobEffects.DOLPHINS_GRACE);
        customEffects.put("bad_omen", MobEffects.BAD_OMEN);
        customEffects.put("hero_of_the_village", MobEffects.HERO_OF_THE_VILLAGE);
        customEffects.put("darkness", MobEffects.DARKNESS);
    }
    
    // ==================== Effect Application ====================
    
    /**
     * Applies an effect to a player.
     * Requirement 44.1: Create effect application
     * 
     * @param player Player to apply effect to
     * @param effectName Effect name or ResourceLocation
     * @param durationTicks Duration in ticks
     * @param amplifier Effect amplifier (0 = level 1)
     * @return true if applied successfully
     */
    public boolean applyEffect(ServerPlayer player, String effectName, int durationTicks, int amplifier) {
        return applyEffect(player, effectName, durationTicks, amplifier, false, true, true);
    }
    
    /**
     * Applies an effect to a player with full options.
     * 
     * @param player Player to apply effect to
     * @param effectName Effect name or ResourceLocation
     * @param durationTicks Duration in ticks
     * @param amplifier Effect amplifier (0 = level 1)
     * @param ambient Whether effect is ambient (from beacon)
     * @param showParticles Whether to show particles
     * @param showIcon Whether to show icon in HUD
     * @return true if applied successfully
     */
    public boolean applyEffect(ServerPlayer player, String effectName, int durationTicks, 
                               int amplifier, boolean ambient, boolean showParticles, boolean showIcon) {
        if (player == null || effectName == null) {
            return false;
        }
        
        Holder<MobEffect> effect = resolveEffect(effectName);
        if (effect == null) {
            LOGGER.warn("Unknown effect: {}", effectName);
            return false;
        }
        
        MobEffectInstance instance = new MobEffectInstance(
                effect, durationTicks, amplifier, ambient, showParticles, showIcon);
        
        boolean result = player.addEffect(instance);
        
        if (result) {
            LOGGER.debug("Applied effect '{}' to {} for {} ticks (amplifier: {})", 
                    effectName, player.getName().getString(), durationTicks, amplifier);
        }
        
        return result;
    }
    
    /**
     * Applies an infinite duration effect.
     * 
     * @param player Player to apply effect to
     * @param effectName Effect name
     * @param amplifier Effect amplifier
     * @return true if applied successfully
     */
    public boolean applyPermanentEffect(ServerPlayer player, String effectName, int amplifier) {
        return applyEffect(player, effectName, MobEffectInstance.INFINITE_DURATION, amplifier);
    }
    
    // ==================== Effect Removal ====================
    
    /**
     * Removes an effect from a player.
     * Requirement 44.2: Add effect removal
     * 
     * @param player Player to remove effect from
     * @param effectName Effect name
     * @return true if removed
     */
    public boolean removeEffect(ServerPlayer player, String effectName) {
        if (player == null || effectName == null) {
            return false;
        }
        
        Holder<MobEffect> effect = resolveEffect(effectName);
        if (effect == null) {
            LOGGER.warn("Unknown effect: {}", effectName);
            return false;
        }
        
        boolean result = player.removeEffect(effect);
        
        if (result) {
            LOGGER.debug("Removed effect '{}' from {}", effectName, player.getName().getString());
        }
        
        return result;
    }
    
    /**
     * Removes all effects from a player.
     * 
     * @param player Player to clear effects from
     * @return Number of effects removed
     */
    public int clearAllEffects(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        int count = player.getActiveEffects().size();
        player.removeAllEffects();
        
        LOGGER.debug("Cleared {} effects from {}", count, player.getName().getString());
        return count;
    }
    
    /**
     * Removes all negative effects from a player.
     * 
     * @param player Player to clear effects from
     * @return Number of effects removed
     */
    public int clearNegativeEffects(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        int count = 0;
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (!instance.getEffect().value().isBeneficial()) {
                toRemove.add(instance.getEffect());
            }
        }
        
        for (Holder<MobEffect> effect : toRemove) {
            if (player.removeEffect(effect)) {
                count++;
            }
        }
        
        return count;
    }
    
    // ==================== Effect Checking ====================
    
    /**
     * Checks if a player has an effect.
     * Requirement 44.3: Implement effect checking
     * 
     * @param player Player to check
     * @param effectName Effect name
     * @return true if player has the effect
     */
    public boolean hasEffect(ServerPlayer player, String effectName) {
        if (player == null || effectName == null) {
            return false;
        }
        
        Holder<MobEffect> effect = resolveEffect(effectName);
        if (effect == null) {
            return false;
        }
        
        return player.hasEffect(effect);
    }
    
    /**
     * Gets the amplifier of an effect on a player.
     * 
     * @param player Player to check
     * @param effectName Effect name
     * @return Amplifier (0 = level 1), or -1 if not present
     */
    public int getEffectAmplifier(ServerPlayer player, String effectName) {
        if (player == null || effectName == null) {
            return -1;
        }
        
        Holder<MobEffect> effect = resolveEffect(effectName);
        if (effect == null) {
            return -1;
        }
        
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null ? instance.getAmplifier() : -1;
    }
    
    /**
     * Gets the remaining duration of an effect on a player.
     * 
     * @param player Player to check
     * @param effectName Effect name
     * @return Remaining duration in ticks, or -1 if not present
     */
    public int getEffectDuration(ServerPlayer player, String effectName) {
        if (player == null || effectName == null) {
            return -1;
        }
        
        Holder<MobEffect> effect = resolveEffect(effectName);
        if (effect == null) {
            return -1;
        }
        
        MobEffectInstance instance = player.getEffect(effect);
        return instance != null ? instance.getDuration() : -1;
    }
    
    /**
     * Gets all active effects on a player.
     * 
     * @param player Player to check
     * @return Map of effect names to amplifiers
     */
    public Map<String, Integer> getActiveEffects(ServerPlayer player) {
        if (player == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Integer> effects = new HashMap<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            if (id != null) {
                effects.put(id.toString(), instance.getAmplifier());
            }
        }
        
        return effects;
    }
    
    // ==================== Custom Effects ====================
    
    /**
     * Registers a custom effect alias.
     * Requirement 44.4: Add custom effects
     * 
     * @param name Custom name
     * @param effect Status effect
     */
    public void registerCustomEffect(String name, Holder<MobEffect> effect) {
        customEffects.put(name.toLowerCase(), effect);
        LOGGER.debug("Registered custom effect alias: {}", name);
    }
    
    /**
     * Unregisters a custom effect alias.
     * 
     * @param name Custom name
     */
    public void unregisterCustomEffect(String name) {
        customEffects.remove(name.toLowerCase());
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Resolves an effect name to a MobEffect.
     */
    private Holder<MobEffect> resolveEffect(String effectName) {
        String normalized = effectName.toLowerCase().replace(" ", "_");
        
        // Check custom effects first
        if (customEffects.containsKey(normalized)) {
            return customEffects.get(normalized);
        }
        
        // Try to parse as ResourceLocation
        ResourceLocation id = ResourceLocation.tryParse(effectName);
        if (id == null) {
            id = ResourceLocation.fromNamespaceAndPath("minecraft", normalized);
        }
        
        var effectHolder = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effectHolder.isPresent()) {
            return effectHolder.get();
        }
        
        return null;
    }
    
    /**
     * Gets all available effect names.
     */
    public Set<String> getAvailableEffects() {
        Set<String> effects = new HashSet<>(customEffects.keySet());
        for (ResourceLocation id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            effects.add(id.toString());
        }
        return effects;
    }
}
