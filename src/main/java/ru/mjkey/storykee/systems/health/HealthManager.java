package ru.mjkey.storykee.systems.health;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player health and damage in the Storykee system.
 * 
 * Requirements: 45.1, 45.2, 45.3, 45.4, 45.5
 */
public class HealthManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HealthManager.class);
    
    private static HealthManager instance;
    
    // Players with invulnerability
    private final Set<UUID> invulnerablePlayers;
    
    // Custom max health modifiers per player
    private final Map<UUID, Float> maxHealthModifiers;
    
    private MinecraftServer server;
    
    private HealthManager() {
        this.invulnerablePlayers = ConcurrentHashMap.newKeySet();
        this.maxHealthModifiers = new ConcurrentHashMap<>();
    }
    
    public static HealthManager getInstance() {
        if (instance == null) {
            instance = new HealthManager();
        }
        return instance;
    }
    
    /**
     * Initializes the health manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("HealthManager initialized");
    }
    
    /**
     * Shuts down the health manager.
     */
    public void shutdown() {
        invulnerablePlayers.clear();
        maxHealthModifiers.clear();
        LOGGER.info("HealthManager shutdown");
    }
    
    // ==================== Damage ====================
    
    /**
     * Deals damage to a player.
     * Requirement 45.1: Create damage dealing
     * 
     * @param player Player to damage
     * @param amount Damage amount
     * @return true if damage was dealt
     */
    public boolean dealDamage(ServerPlayer player, float amount) {
        return dealDamage(player, amount, "generic");
    }
    
    /**
     * Deals damage to a player with a specific type.
     * 
     * @param player Player to damage
     * @param amount Damage amount
     * @param damageType Damage type (generic, fire, magic, etc.)
     * @return true if damage was dealt
     */
    public boolean dealDamage(ServerPlayer player, float amount, String damageType) {
        if (player == null || amount <= 0) {
            return false;
        }
        
        // Check invulnerability
        if (isInvulnerable(player.getUUID())) {
            LOGGER.debug("Player {} is invulnerable, damage blocked", player.getName().getString());
            return false;
        }
        
        DamageSource source = createDamageSource(player, damageType);
        float healthBefore = player.getHealth();
        ServerLevel level = (ServerLevel) player.level();
        player.hurtServer(level, source, amount);
        boolean result = player.getHealth() < healthBefore;
        
        if (result) {
            LOGGER.debug("Dealt {} {} damage to {}", amount, damageType, player.getName().getString());
        }
        
        return result;
    }
    
    /**
     * Kills a player instantly.
     * 
     * @param player Player to kill
     */
    public void kill(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        // Bypass invulnerability for script-triggered kills
        player.setHealth(0);
        LOGGER.debug("Killed player {}", player.getName().getString());
    }
    
    // ==================== Healing ====================
    
    /**
     * Heals a player.
     * Requirement 45.2: Add healing
     * 
     * @param player Player to heal
     * @param amount Heal amount
     * @return Actual amount healed
     */
    public float heal(ServerPlayer player, float amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float newHealth = Math.min(currentHealth + amount, maxHealth);
        float actualHeal = newHealth - currentHealth;
        
        if (actualHeal > 0) {
            player.setHealth(newHealth);
            LOGGER.debug("Healed {} for {} HP (now at {})", 
                    player.getName().getString(), actualHeal, newHealth);
        }
        
        return actualHeal;
    }
    
    /**
     * Fully heals a player.
     * 
     * @param player Player to heal
     * @return Amount healed
     */
    public float fullHeal(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        
        player.setHealth(maxHealth);
        
        float healed = maxHealth - currentHealth;
        LOGGER.debug("Fully healed {} (healed {} HP)", player.getName().getString(), healed);
        
        return healed;
    }
    
    /**
     * Sets a player's health to a specific value.
     * 
     * @param player Player
     * @param health Health value
     */
    public void setHealth(ServerPlayer player, float health) {
        if (player == null) {
            return;
        }
        
        float maxHealth = player.getMaxHealth();
        float clampedHealth = Math.max(0, Math.min(health, maxHealth));
        
        player.setHealth(clampedHealth);
        LOGGER.debug("Set {} health to {}", player.getName().getString(), clampedHealth);
    }
    
    /**
     * Gets a player's current health.
     * 
     * @param player Player
     * @return Current health
     */
    public float getHealth(ServerPlayer player) {
        return player != null ? player.getHealth() : 0;
    }
    
    /**
     * Gets a player's health as a percentage.
     * 
     * @param player Player
     * @return Health percentage (0.0 to 1.0)
     */
    public float getHealthPercent(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return player.getHealth() / player.getMaxHealth();
    }
    
    // ==================== Max Health ====================
    
    /**
     * Sets a player's max health.
     * Requirement 45.3: Implement max health modification
     * 
     * @param player Player
     * @param maxHealth New max health
     */
    public void setMaxHealth(ServerPlayer player, float maxHealth) {
        if (player == null || maxHealth <= 0) {
            return;
        }
        
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(maxHealth);
            
            // Clamp current health to new max
            if (player.getHealth() > maxHealth) {
                player.setHealth(maxHealth);
            }
            
            maxHealthModifiers.put(player.getUUID(), maxHealth);
            LOGGER.debug("Set {} max health to {}", player.getName().getString(), maxHealth);
        }
    }
    
    /**
     * Gets a player's max health.
     * 
     * @param player Player
     * @return Max health
     */
    public float getMaxHealth(ServerPlayer player) {
        return player != null ? player.getMaxHealth() : 0;
    }
    
    /**
     * Resets a player's max health to default.
     * 
     * @param player Player
     */
    public void resetMaxHealth(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(20.0); // Default max health
            maxHealthModifiers.remove(player.getUUID());
            LOGGER.debug("Reset {} max health to default", player.getName().getString());
        }
    }
    
    /**
     * Adds to a player's max health.
     * 
     * @param player Player
     * @param amount Amount to add
     */
    public void addMaxHealth(ServerPlayer player, float amount) {
        if (player == null) {
            return;
        }
        
        float currentMax = player.getMaxHealth();
        setMaxHealth(player, currentMax + amount);
    }
    
    // ==================== Invulnerability ====================
    
    /**
     * Sets a player's invulnerability.
     * Requirement 45.4: Add invulnerability
     * 
     * @param player Player
     * @param invulnerable Whether player should be invulnerable
     */
    public void setInvulnerable(ServerPlayer player, boolean invulnerable) {
        if (player == null) {
            return;
        }
        
        if (invulnerable) {
            invulnerablePlayers.add(player.getUUID());
            LOGGER.debug("Made {} invulnerable", player.getName().getString());
        } else {
            invulnerablePlayers.remove(player.getUUID());
            LOGGER.debug("Removed invulnerability from {}", player.getName().getString());
        }
    }
    
    /**
     * Checks if a player is invulnerable.
     * 
     * @param playerId Player UUID
     * @return true if invulnerable
     */
    public boolean isInvulnerable(UUID playerId) {
        return invulnerablePlayers.contains(playerId);
    }
    
    /**
     * Sets temporary invulnerability.
     * 
     * @param player Player
     * @param durationTicks Duration in ticks
     */
    public void setTemporaryInvulnerability(ServerPlayer player, int durationTicks) {
        if (player == null || durationTicks <= 0) {
            return;
        }
        
        setInvulnerable(player, true);
        
        // Schedule removal (using Minecraft's built-in invulnerability timer)
        player.invulnerableTime = durationTicks;
        
        LOGGER.debug("Set {} invulnerable for {} ticks", player.getName().getString(), durationTicks);
    }
    
    // ==================== Hunger ====================
    
    /**
     * Sets a player's food level.
     * Requirement 45.5: Additional health-related functions
     * 
     * @param player Player
     * @param foodLevel Food level (0-20)
     */
    public void setFoodLevel(ServerPlayer player, int foodLevel) {
        if (player == null) {
            return;
        }
        
        player.getFoodData().setFoodLevel(Math.max(0, Math.min(20, foodLevel)));
        LOGGER.debug("Set {} food level to {}", player.getName().getString(), foodLevel);
    }
    
    /**
     * Gets a player's food level.
     * 
     * @param player Player
     * @return Food level
     */
    public int getFoodLevel(ServerPlayer player) {
        return player != null ? player.getFoodData().getFoodLevel() : 0;
    }
    
    /**
     * Sets a player's saturation level.
     * 
     * @param player Player
     * @param saturation Saturation level
     */
    public void setSaturation(ServerPlayer player, float saturation) {
        if (player == null) {
            return;
        }
        
        player.getFoodData().setSaturation(Math.max(0, saturation));
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Creates a damage source for the given type.
     */
    private DamageSource createDamageSource(ServerPlayer player, String damageType) {
        var world = (ServerLevel)player.level();
        var registry = world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        
        return switch (damageType.toLowerCase()) {
            case "fire" -> new DamageSource(registry.getOrThrow(DamageTypes.ON_FIRE));
            case "lava" -> new DamageSource(registry.getOrThrow(DamageTypes.LAVA));
            case "magic" -> new DamageSource(registry.getOrThrow(DamageTypes.MAGIC));
            case "wither" -> new DamageSource(registry.getOrThrow(DamageTypes.WITHER));
            case "drown" -> new DamageSource(registry.getOrThrow(DamageTypes.DROWN));
            case "starve" -> new DamageSource(registry.getOrThrow(DamageTypes.STARVE));
            case "fall" -> new DamageSource(registry.getOrThrow(DamageTypes.FALL));
            case "void" -> new DamageSource(registry.getOrThrow(DamageTypes.FELL_OUT_OF_WORLD));
            case "lightning" -> new DamageSource(registry.getOrThrow(DamageTypes.LIGHTNING_BOLT));
            case "freeze" -> new DamageSource(registry.getOrThrow(DamageTypes.FREEZE));
            default -> new DamageSource(registry.getOrThrow(DamageTypes.GENERIC));
        };
    }
}
