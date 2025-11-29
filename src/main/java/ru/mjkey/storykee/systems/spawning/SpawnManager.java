package ru.mjkey.storykee.systems.spawning;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages mob spawning control for Storykee scripts.
 * Allows disabling spawns in regions, custom mob spawning, and spawn rate control.
 * 
 * Requirements: 52.1, 52.2, 52.3, 52.4, 52.5
 */
public class SpawnManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnManager.class);
    
    private static SpawnManager instance;
    
    // Regions where spawning is disabled
    private final Map<String, SpawnControlRegion> disabledRegions = new ConcurrentHashMap<>();
    
    // Custom spawn rate modifiers: entityType -> multiplier
    private final Map<String, Double> spawnRateModifiers = new ConcurrentHashMap<>();
    
    // Region-based spawn rate modifiers: regionId -> (entityType -> multiplier)
    private final Map<String, Map<String, Double>> regionSpawnRates = new ConcurrentHashMap<>();
    
    // Globally disabled entity types
    private final Set<String> globallyDisabledTypes = ConcurrentHashMap.newKeySet();
    
    private MinecraftServer server;
    
    private SpawnManager() {
    }
    
    public static SpawnManager getInstance() {
        if (instance == null) {
            instance = new SpawnManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Spawn Disabling (Requirement 52.1) =====
    
    /**
     * Disables mob spawning in a region.
     * Requirement 52.1: WHEN a script disables spawning in a region THEN the Runtime SHALL prevent natural mob spawns
     */
    public void disableSpawning(String regionId, BlockPos min, BlockPos max, String dimension) {
        SpawnControlRegion region = new SpawnControlRegion(regionId, min, max, dimension, false);
        disabledRegions.put(regionId, region);
        LOGGER.info("disableSpawning: Disabled spawning in region {}", regionId);
    }
    
    /**
     * Disables spawning of specific entity types in a region.
     */
    public void disableSpawning(String regionId, BlockPos min, BlockPos max, String dimension, Set<String> entityTypes) {
        SpawnControlRegion region = new SpawnControlRegion(regionId, min, max, dimension, false);
        region.setDisabledTypes(entityTypes);
        disabledRegions.put(regionId, region);
        LOGGER.info("disableSpawning: Disabled spawning of {} types in region {}", entityTypes.size(), regionId);
    }
    
    /**
     * Enables spawning in a previously disabled region.
     * Requirement 52.5: WHEN spawn control is removed THEN the Runtime SHALL restore default spawning behavior
     */
    public void enableSpawning(String regionId) {
        if (disabledRegions.remove(regionId) != null) {
            regionSpawnRates.remove(regionId);
            LOGGER.info("enableSpawning: Enabled spawning in region {}", regionId);
        }
    }
    
    /**
     * Globally disables spawning of a specific entity type.
     */
    public void disableEntityType(String entityType) {
        globallyDisabledTypes.add(normalizeEntityType(entityType));
        LOGGER.info("disableEntityType: Globally disabled spawning of {}", entityType);
    }
    
    /**
     * Re-enables spawning of a globally disabled entity type.
     */
    public void enableEntityType(String entityType) {
        globallyDisabledTypes.remove(normalizeEntityType(entityType));
        LOGGER.info("enableEntityType: Re-enabled spawning of {}", entityType);
    }
    
    /**
     * Checks if spawning is allowed at a position.
     * Requirement 52.4: WHEN spawning is controlled THEN the Runtime SHALL respect region boundaries
     */
    public boolean isSpawningAllowed(BlockPos pos, String dimension, String entityType) {
        // Check global disable
        if (globallyDisabledTypes.contains(normalizeEntityType(entityType))) {
            return false;
        }
        
        // Check region-based disable
        for (SpawnControlRegion region : disabledRegions.values()) {
            if (region.contains(pos, dimension)) {
                if (!region.isSpawningAllowed() || region.isTypeDisabled(entityType)) {
                    return false;
                }
            }
        }
        
        return true;
    }

    // ===== Custom Mob Spawning (Requirement 52.2) =====
    
    /**
     * Spawns a custom mob at a location.
     * Requirement 52.2: WHEN a script spawns a custom mob THEN the Runtime SHALL create it with specified properties
     */
    public Entity spawnMob(String entityType, double x, double y, double z, String dimension, Map<String, Object> properties) {
        if (server == null) {
            LOGGER.warn("spawnMob: Server not available");
            return null;
        }
        
        ServerLevel level = getLevel(dimension);
        if (level == null) {
            LOGGER.warn("spawnMob: Unknown dimension - {}", dimension);
            return null;
        }
        
        EntityType<?> type = parseEntityType(entityType);
        if (type == null) {
            LOGGER.warn("spawnMob: Unknown entity type - {}", entityType);
            return null;
        }
        
        Entity entity = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (entity == null) {
            LOGGER.warn("spawnMob: Failed to create entity - {}", entityType);
            return null;
        }
        
        // Set position
        entity.setPos(x, y, z);
        
        // Apply properties
        if (properties != null) {
            applyEntityProperties(entity, properties);
        }
        
        // Spawn the entity
        if (level.addFreshEntity(entity)) {
            LOGGER.info("spawnMob: Spawned {} at ({}, {}, {}) in {}", entityType, x, y, z, dimension);
            return entity;
        }
        
        return null;
    }
    
    /**
     * Spawns a mob with default properties.
     */
    public Entity spawnMob(String entityType, double x, double y, double z, String dimension) {
        return spawnMob(entityType, x, y, z, dimension, null);
    }
    
    private void applyEntityProperties(Entity entity, Map<String, Object> properties) {
        // Apply custom name
        if (properties.containsKey("name")) {
            String name = String.valueOf(properties.get("name"));
            entity.setCustomName(net.minecraft.network.chat.Component.literal(name));
            entity.setCustomNameVisible(true);
        }
        
        // Apply no AI
        if (entity instanceof Mob mob && properties.containsKey("noAI")) {
            mob.setNoAi(toBoolean(properties.get("noAI")));
        }
        
        // Apply invulnerable
        if (properties.containsKey("invulnerable")) {
            entity.setInvulnerable(toBoolean(properties.get("invulnerable")));
        }
        
        // Apply silent
        if (properties.containsKey("silent")) {
            entity.setSilent(toBoolean(properties.get("silent")));
        }
        
        // Apply glowing
        if (properties.containsKey("glowing")) {
            entity.setGlowingTag(toBoolean(properties.get("glowing")));
        }
        
        // Apply health for mobs
        if (entity instanceof Mob mob && properties.containsKey("health")) {
            float health = toFloat(properties.get("health"));
            mob.setHealth(health);
        }
    }

    // ===== Spawn Rate Control (Requirement 52.3) =====
    
    /**
     * Sets a global spawn rate modifier for an entity type.
     * Requirement 52.3: WHEN a script sets spawn rates THEN the Runtime SHALL adjust the frequency
     */
    public void setSpawnRate(String entityType, double multiplier) {
        spawnRateModifiers.put(normalizeEntityType(entityType), multiplier);
        LOGGER.info("setSpawnRate: Set spawn rate for {} to {}x", entityType, multiplier);
    }
    
    /**
     * Sets a region-specific spawn rate modifier.
     * Requirement 52.4: WHEN spawning is controlled THEN the Runtime SHALL respect region boundaries
     */
    public void setRegionSpawnRate(String regionId, String entityType, double multiplier) {
        regionSpawnRates.computeIfAbsent(regionId, k -> new ConcurrentHashMap<>())
            .put(normalizeEntityType(entityType), multiplier);
        LOGGER.info("setRegionSpawnRate: Set spawn rate for {} in region {} to {}x", entityType, regionId, multiplier);
    }
    
    /**
     * Gets the effective spawn rate multiplier for an entity at a position.
     */
    public double getSpawnRateMultiplier(BlockPos pos, String dimension, String entityType) {
        String normalizedType = normalizeEntityType(entityType);
        double multiplier = 1.0;
        
        // Apply global modifier
        Double globalMod = spawnRateModifiers.get(normalizedType);
        if (globalMod != null) {
            multiplier *= globalMod;
        }
        
        // Apply region-specific modifiers
        for (Map.Entry<String, SpawnControlRegion> entry : disabledRegions.entrySet()) {
            SpawnControlRegion region = entry.getValue();
            if (region.contains(pos, dimension)) {
                Map<String, Double> regionRates = regionSpawnRates.get(entry.getKey());
                if (regionRates != null) {
                    Double regionMod = regionRates.get(normalizedType);
                    if (regionMod != null) {
                        multiplier *= regionMod;
                    }
                }
            }
        }
        
        return multiplier;
    }
    
    /**
     * Resets spawn rate for an entity type.
     */
    public void resetSpawnRate(String entityType) {
        spawnRateModifiers.remove(normalizeEntityType(entityType));
        LOGGER.info("resetSpawnRate: Reset spawn rate for {}", entityType);
    }
    
    /**
     * Resets all spawn rate modifiers.
     * Requirement 52.5: WHEN spawn control is removed THEN the Runtime SHALL restore default spawning behavior
     */
    public void resetAllSpawnRates() {
        spawnRateModifiers.clear();
        regionSpawnRates.clear();
        LOGGER.info("resetAllSpawnRates: Reset all spawn rates");
    }

    // ===== Entity Queries =====
    
    /**
     * Gets all entities of a type in a region.
     */
    public List<Entity> getEntitiesInRegion(String entityType, BlockPos min, BlockPos max, String dimension) {
        if (server == null) return Collections.emptyList();
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return Collections.emptyList();
        
        AABB box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        EntityType<?> type = entityType != null ? parseEntityType(entityType) : null;
        
        List<Entity> result = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (box.contains(entity.position())) {
                if (type == null || entity.getType() == type) {
                    result.add(entity);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Removes all entities of a type in a region.
     */
    public int removeEntitiesInRegion(String entityType, BlockPos min, BlockPos max, String dimension) {
        List<Entity> entities = getEntitiesInRegion(entityType, min, max, dimension);
        int count = 0;
        
        for (Entity entity : entities) {
            entity.discard();
            count++;
        }
        
        LOGGER.info("removeEntitiesInRegion: Removed {} entities", count);
        return count;
    }

    // ===== Utility Methods =====
    
    private ServerLevel getLevel(String dimension) {
        if (server == null || dimension == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        
        // Try common dimension names
        return switch (dimension.toLowerCase()) {
            case "overworld", "minecraft:overworld" -> server.overworld();
            default -> null;
        };
    }
    
    private EntityType<?> parseEntityType(String entityType) {
        if (entityType == null) return null;
        
        String normalized = normalizeEntityType(entityType);
        try {
            ResourceLocation location = ResourceLocation.parse(normalized);
            return BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private String normalizeEntityType(String entityType) {
        if (entityType == null) return "";
        if (!entityType.contains(":")) {
            return "minecraft:" + entityType.toLowerCase();
        }
        return entityType.toLowerCase();
    }
    
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(String.valueOf(value));
    }
    
    private float toFloat(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
    
    /**
     * Clears all spawn control settings.
     */
    public void clear() {
        disabledRegions.clear();
        spawnRateModifiers.clear();
        regionSpawnRates.clear();
        globallyDisabledTypes.clear();
        LOGGER.info("clear: Cleared all spawn control settings");
    }
    
    /**
     * Gets all disabled regions.
     */
    public Collection<SpawnControlRegion> getDisabledRegions() {
        return Collections.unmodifiableCollection(disabledRegions.values());
    }
    
    /**
     * Checks if a region exists.
     */
    public boolean hasRegion(String regionId) {
        return disabledRegions.containsKey(regionId);
    }
}
