package ru.mjkey.storykee.systems.boss;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.events.EventData;
import ru.mjkey.storykee.events.EventManager;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages boss encounters in the story system.
 * Handles boss spawning, tracking, and defeat handling.
 * 
 * Requirements: 18.1, 18.5
 */
public class BossManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BossManager.class);
    
    private static BossManager instance;
    
    // Boss registry by ID
    private final Map<String, StoryBoss> activeBosses = new ConcurrentHashMap<>();
    
    // Defeat callbacks
    private final Map<String, Consumer<StoryBoss>> defeatCallbacks = new ConcurrentHashMap<>();
    
    private MinecraftServer server;
    
    private BossManager() {
    }
    
    public static BossManager getInstance() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }
    
    /**
     * Initializes the BossManager with the server instance.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("BossManager initialized");
    }
    
    /**
     * Shuts down the BossManager and cleans up all bosses.
     */
    public void shutdown() {
        LOGGER.info("Shutting down BossManager, removing {} active bosses", activeBosses.size());
        
        // Remove all active bosses
        for (StoryBoss boss : activeBosses.values()) {
            if (boss.isAlive()) {
                boss.remove(StoryBoss.RemovalReason.DISCARDED);
            }
        }
        
        activeBosses.clear();
        defeatCallbacks.clear();
        this.server = null;
    }
    
    /**
     * Spawns a new boss with the given properties.
     * Requirements: 18.1 - Spawn boss with specified health, damage, and abilities
     * 
     * @param bossId Unique ResourceLocation for the boss
     * @param properties Boss configuration properties
     * @param level The server level to spawn in
     * @return The spawned boss entity, or null if spawning failed
     */
    public StoryBoss spawnBoss(String bossId, BossProperties properties, ServerLevel level) {
        if (level == null) {
            LOGGER.error("Cannot spawn boss {}: level is null", bossId);
            return null;
        }
        
        // Check if boss with this ID already exists
        if (activeBosses.containsKey(bossId)) {
            LOGGER.warn("Boss with ID {} already exists, removing old boss", bossId);
            removeBoss(bossId);
        }
        
        try {
            // Create the boss entity
            StoryBoss boss = BossEntityRegistry.STORY_BOSS.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (boss == null) {
                LOGGER.error("Failed to create boss entity for {}", bossId);
                return null;
            }
            
            // Set boss ID
            boss.setBossId(bossId);
            
            // Apply attributes before applying properties
            applyAttributes(boss, properties);
            
            // Apply properties
            boss.applyProperties(properties);
            
            // Spawn the entity in the world
            level.addFreshEntity(boss);
            
            // Register in active bosses
            activeBosses.put(bossId, boss);
            
            // Fire boss spawn event
            EventData eventData = new EventData("onBossSpawn");
            eventData.set("boss", bossId);
            eventData.set("bossName", properties.getName());
            eventData.set("x", properties.getX());
            eventData.set("y", properties.getY());
            eventData.set("z", properties.getZ());
            eventData.set("maxHealth", properties.getMaxHealth());
            
            EventManager.getInstance().fireEvent("onBossSpawn", eventData);
            
            LOGGER.info("Spawned boss {} at ({}, {}, {})", bossId, 
                    properties.getX(), properties.getY(), properties.getZ());
            
            return boss;
            
        } catch (Exception e) {
            LOGGER.error("Failed to spawn boss {}: {}", bossId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Applies attribute values to the boss entity.
     */
    private void applyAttributes(StoryBoss boss, BossProperties properties) {
        // Set max health
        var maxHealthAttr = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr != null) {
            maxHealthAttr.setBaseValue(properties.getMaxHealth());
        }
        boss.setHealth((float) properties.getMaxHealth());
        
        // Set attack damage
        var attackAttr = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(properties.getAttackDamage());
        }
        
        // Set movement speed
        var speedAttr = boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(properties.getMovementSpeed());
        }
        
        // Set follow range
        var followAttr = boss.getAttribute(Attributes.FOLLOW_RANGE);
        if (followAttr != null) {
            followAttr.setBaseValue(properties.getFollowRange());
        }
        
        // Set knockback resistance
        var knockbackAttr = boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.setBaseValue(properties.getKnockbackResistance());
        }
    }
    
    /**
     * Removes a boss by ID.
     * 
     * @param bossId The boss ID to remove
     * @return true if the boss was removed, false if not found
     */
    public boolean removeBoss(String bossId) {
        StoryBoss boss = activeBosses.remove(bossId);
        if (boss != null) {
            if (boss.isAlive()) {
                boss.remove(StoryBoss.RemovalReason.DISCARDED);
            }
            defeatCallbacks.remove(bossId);
            LOGGER.info("Removed boss {}", bossId);
            return true;
        }
        return false;
    }
    
    /**
     * Gets a boss by ID.
     * 
     * @param bossId The boss ID
     * @return Optional containing the boss if found
     */
    public Optional<StoryBoss> getBoss(String bossId) {
        StoryBoss boss = activeBosses.get(bossId);
        // Clean up dead bosses
        if (boss != null && !boss.isAlive()) {
            activeBosses.remove(bossId);
            defeatCallbacks.remove(bossId);
            return Optional.empty();
        }
        return Optional.ofNullable(boss);
    }
    
    /**
     * Gets all active bosses.
     */
    public Collection<StoryBoss> getAllBosses() {
        // Clean up dead bosses
        activeBosses.entrySet().removeIf(entry -> !entry.getValue().isAlive());
        return activeBosses.values();
    }
    
    /**
     * Checks if a boss with the given ID exists and is alive.
     */
    public boolean isBossActive(String bossId) {
        return getBoss(bossId).isPresent();
    }
    
    /**
     * Registers a callback to be executed when a boss is defeated.
     * Requirements: 18.5 - Execute reward scripts on defeat
     * 
     * @param bossId The boss ID
     * @param callback The callback to execute
     */
    public void onBossDefeated(String bossId, Consumer<StoryBoss> callback) {
        defeatCallbacks.put(bossId, callback);
    }
    
    /**
     * Called when a boss is defeated.
     * Requirements: 18.5 - Execute reward scripts and despawn entity
     */
    public void handleBossDefeated(StoryBoss boss) {
        String bossId = boss.getBossId();
        
        LOGGER.info("Handling defeat of boss {}", bossId);
        
        // Execute defeat callback if registered
        Consumer<StoryBoss> callback = defeatCallbacks.remove(bossId);
        if (callback != null) {
            try {
                callback.accept(boss);
            } catch (Exception e) {
                LOGGER.error("Error executing defeat callback for boss {}: {}", bossId, e.getMessage(), e);
            }
        }
        
        // Remove from active bosses
        activeBosses.remove(bossId);
    }
    
    /**
     * Updates boss properties.
     * 
     * @param bossId The boss ID
     * @param properties New properties to apply
     * @return true if the boss was updated, false if not found
     */
    public boolean updateBoss(String bossId, BossProperties properties) {
        return getBoss(bossId).map(boss -> {
            applyAttributes(boss, properties);
            boss.applyProperties(properties);
            LOGGER.debug("Updated boss {} properties", bossId);
            return true;
        }).orElse(false);
    }
    
    /**
     * Sets the combat area for a boss.
     * Requirements: 18.6 - Prevent boss from leaving designated area
     */
    public boolean setCombatArea(String bossId, double centerX, double centerY, double centerZ, double radius) {
        return getBoss(bossId).map(boss -> {
            boss.setCombatArea(centerX, centerY, centerZ, radius);
            LOGGER.debug("Set combat area for boss {} at ({}, {}, {}) radius {}", 
                    bossId, centerX, centerY, centerZ, radius);
            return true;
        }).orElse(false);
    }
    
    /**
     * Clears the combat area restriction for a boss.
     */
    public boolean clearCombatArea(String bossId) {
        return getBoss(bossId).map(boss -> {
            boss.clearCombatArea();
            LOGGER.debug("Cleared combat area for boss {}", bossId);
            return true;
        }).orElse(false);
    }
    
    /**
     * Adds a phase to an existing boss.
     */
    public boolean addPhase(String bossId, BossPhase phase) {
        return getBoss(bossId).map(boss -> {
            boss.addPhase(phase);
            LOGGER.debug("Added phase {} to boss {}", phase.getId(), bossId);
            return true;
        }).orElse(false);
    }
    
    /**
     * Gets the number of active bosses.
     */
    public int getActiveBossCount() {
        // Clean up dead bosses
        activeBosses.entrySet().removeIf(entry -> !entry.getValue().isAlive());
        return activeBosses.size();
    }
    
    /**
     * Removes all active bosses.
     */
    public void removeAllBosses() {
        LOGGER.info("Removing all {} active bosses", activeBosses.size());
        
        for (StoryBoss boss : activeBosses.values()) {
            if (boss.isAlive()) {
                boss.remove(StoryBoss.RemovalReason.DISCARDED);
            }
        }
        
        activeBosses.clear();
        defeatCallbacks.clear();
    }
    
    /**
     * Gets the server instance.
     */
    public MinecraftServer getServer() {
        return server;
    }
}
