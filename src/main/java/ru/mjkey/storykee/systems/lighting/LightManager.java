package ru.mjkey.storykee.systems.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages light level control for Storykee scripts.
 * Provides functions for setting light levels, creating light sources, and dynamic lighting.
 * 
 * Requirements: 53.1, 53.2, 53.3, 53.4, 53.5
 */
public class LightManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LightManager.class);
    
    private static LightManager instance;
    
    // Custom light sources placed by scripts: dimension -> position -> light level
    private final Map<String, Map<BlockPos, Integer>> customLightSources = new ConcurrentHashMap<>();
    
    // Dynamic light sources that can be updated
    private final Map<String, DynamicLight> dynamicLights = new ConcurrentHashMap<>();
    
    private MinecraftServer server;
    
    private LightManager() {
    }
    
    public static LightManager getInstance() {
        if (instance == null) {
            instance = new LightManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Light Level Queries (Requirement 53.4) =====
    
    /**
     * Gets the light level at a position.
     * Requirement 53.4: WHEN light levels are queried THEN the Runtime SHALL return the current value
     */
    public int getLightLevel(BlockPos pos, String dimension) {
        if (server == null) return 0;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return 0;
        
        return level.getMaxLocalRawBrightness(pos);
    }
    
    /**
     * Gets the block light level at a position.
     */
    public int getBlockLightLevel(BlockPos pos, String dimension) {
        if (server == null) return 0;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return 0;
        
        return level.getBrightness(LightLayer.BLOCK, pos);
    }
    
    /**
     * Gets the sky light level at a position.
     */
    public int getSkyLightLevel(BlockPos pos, String dimension) {
        if (server == null) return 0;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return 0;
        
        return level.getBrightness(LightLayer.SKY, pos);
    }

    // ===== Light Source Creation (Requirement 53.2) =====
    
    /**
     * Creates a light source at a position.
     * Requirement 53.2: WHEN a script creates a light source THEN the Runtime SHALL place it and update lighting
     * 
     * Note: Minecraft doesn't support arbitrary light levels at positions.
     * This implementation places light-emitting blocks (like glowstone or light blocks).
     */
    public boolean createLightSource(String id, BlockPos pos, String dimension, int lightLevel) {
        if (server == null) {
            LOGGER.warn("createLightSource: Server not available");
            return false;
        }
        
        ServerLevel level = getLevel(dimension);
        if (level == null) {
            LOGGER.warn("createLightSource: Unknown dimension - {}", dimension);
            return false;
        }
        
        // Clamp light level to valid range
        lightLevel = Math.max(0, Math.min(15, lightLevel));
        
        // Use light block if available (1.17+), otherwise use glowstone for max light
        BlockState lightBlock = getLightBlock(lightLevel);
        
        // Place the light block
        level.setBlock(pos, lightBlock, 3);
        
        // Track the light source
        customLightSources.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
            .put(pos, lightLevel);
        
        LOGGER.info("createLightSource: Created light source {} at {} in {} with level {}", 
            id, pos, dimension, lightLevel);
        return true;
    }
    
    /**
     * Gets the appropriate light-emitting block for a light level.
     */
    private BlockState getLightBlock(int lightLevel) {
        // In Minecraft 1.17+, we can use light blocks with specific levels
        // For now, use common light-emitting blocks based on level
        if (lightLevel >= 15) {
            return Blocks.GLOWSTONE.defaultBlockState();
        } else if (lightLevel >= 14) {
            return Blocks.SEA_LANTERN.defaultBlockState();
        } else if (lightLevel >= 12) {
            return Blocks.SHROOMLIGHT.defaultBlockState();
        } else if (lightLevel >= 10) {
            return Blocks.REDSTONE_LAMP.defaultBlockState();
        } else if (lightLevel >= 7) {
            return Blocks.TORCH.defaultBlockState();
        } else if (lightLevel >= 1) {
            return Blocks.REDSTONE_TORCH.defaultBlockState();
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }
    
    /**
     * Removes a light source.
     * Requirement 53.3: WHEN a script removes a light source THEN the Runtime SHALL update lighting calculations
     */
    public boolean removeLightSource(BlockPos pos, String dimension) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        // Remove from tracking
        Map<BlockPos, Integer> dimLights = customLightSources.get(dimension);
        if (dimLights != null) {
            dimLights.remove(pos);
        }
        
        // Replace with air
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        
        LOGGER.info("removeLightSource: Removed light source at {} in {}", pos, dimension);
        return true;
    }

    // ===== Dynamic Lighting (Requirement 53.5) =====
    
    /**
     * Creates a dynamic light that can be updated.
     * Requirement 53.5: WHEN dynamic lighting is used THEN the Runtime SHALL update it in real-time
     */
    public DynamicLight createDynamicLight(String id, BlockPos pos, String dimension, int lightLevel) {
        DynamicLight light = new DynamicLight(id, pos, dimension, lightLevel);
        dynamicLights.put(id, light);
        
        // Create the initial light source
        createLightSource(id, pos, dimension, lightLevel);
        
        LOGGER.info("createDynamicLight: Created dynamic light {} at {}", id, pos);
        return light;
    }
    
    /**
     * Updates a dynamic light's position.
     * Requirement 53.5: WHEN dynamic lighting is used THEN the Runtime SHALL update it in real-time
     */
    public boolean updateDynamicLightPosition(String id, BlockPos newPos) {
        DynamicLight light = dynamicLights.get(id);
        if (light == null) {
            LOGGER.warn("updateDynamicLightPosition: Unknown dynamic light - {}", id);
            return false;
        }
        
        // Remove old light source
        removeLightSource(light.getPosition(), light.getDimension());
        
        // Update position
        light.setPosition(newPos);
        
        // Create new light source
        createLightSource(id, newPos, light.getDimension(), light.getLightLevel());
        
        LOGGER.debug("updateDynamicLightPosition: Moved dynamic light {} to {}", id, newPos);
        return true;
    }
    
    /**
     * Updates a dynamic light's level.
     */
    public boolean updateDynamicLightLevel(String id, int newLevel) {
        DynamicLight light = dynamicLights.get(id);
        if (light == null) {
            return false;
        }
        
        // Remove old light source
        removeLightSource(light.getPosition(), light.getDimension());
        
        // Update level
        light.setLightLevel(newLevel);
        
        // Create new light source
        createLightSource(id, light.getPosition(), light.getDimension(), newLevel);
        
        LOGGER.debug("updateDynamicLightLevel: Updated dynamic light {} to level {}", id, newLevel);
        return true;
    }
    
    /**
     * Removes a dynamic light.
     */
    public boolean removeDynamicLight(String id) {
        DynamicLight light = dynamicLights.remove(id);
        if (light == null) {
            return false;
        }
        
        removeLightSource(light.getPosition(), light.getDimension());
        LOGGER.info("removeDynamicLight: Removed dynamic light {}", id);
        return true;
    }
    
    /**
     * Gets a dynamic light by ID.
     */
    public DynamicLight getDynamicLight(String id) {
        return dynamicLights.get(id);
    }

    // ===== Region Light Control =====
    
    /**
     * Fills a region with light sources.
     */
    public int fillRegionWithLight(BlockPos min, BlockPos max, String dimension, int lightLevel, int spacing) {
        if (server == null) return 0;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return 0;
        
        int count = 0;
        spacing = Math.max(1, spacing);
        
        for (int x = min.getX(); x <= max.getX(); x += spacing) {
            for (int y = min.getY(); y <= max.getY(); y += spacing) {
                for (int z = min.getZ(); z <= max.getZ(); z += spacing) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).isAir()) {
                        String id = "region_light_" + x + "_" + y + "_" + z;
                        createLightSource(id, pos, dimension, lightLevel);
                        count++;
                    }
                }
            }
        }
        
        LOGGER.info("fillRegionWithLight: Created {} light sources in region", count);
        return count;
    }
    
    /**
     * Removes all light sources in a region.
     */
    public int clearRegionLights(BlockPos min, BlockPos max, String dimension) {
        Map<BlockPos, Integer> dimLights = customLightSources.get(dimension);
        if (dimLights == null) return 0;
        
        List<BlockPos> toRemove = new ArrayList<>();
        
        for (BlockPos pos : dimLights.keySet()) {
            if (pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ()) {
                toRemove.add(pos);
            }
        }
        
        for (BlockPos pos : toRemove) {
            removeLightSource(pos, dimension);
        }
        
        LOGGER.info("clearRegionLights: Removed {} light sources from region", toRemove.size());
        return toRemove.size();
    }

    // ===== Utility Methods =====
    
    private ServerLevel getLevel(String dimension) {
        if (server == null || dimension == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        
        return switch (dimension.toLowerCase()) {
            case "overworld", "minecraft:overworld" -> server.overworld();
            default -> null;
        };
    }
    
    /**
     * Clears all custom light sources.
     */
    public void clear() {
        // Remove all dynamic lights
        for (String id : new ArrayList<>(dynamicLights.keySet())) {
            removeDynamicLight(id);
        }
        
        // Remove all static light sources
        for (Map.Entry<String, Map<BlockPos, Integer>> entry : customLightSources.entrySet()) {
            String dimension = entry.getKey();
            for (BlockPos pos : new ArrayList<>(entry.getValue().keySet())) {
                removeLightSource(pos, dimension);
            }
        }
        
        customLightSources.clear();
        dynamicLights.clear();
        
        LOGGER.info("clear: Cleared all light sources");
    }
    
    /**
     * Gets all custom light sources in a dimension.
     */
    public Map<BlockPos, Integer> getLightSources(String dimension) {
        Map<BlockPos, Integer> lights = customLightSources.get(dimension);
        return lights != null ? Collections.unmodifiableMap(lights) : Collections.emptyMap();
    }
    
    /**
     * Gets all dynamic lights.
     */
    public Collection<DynamicLight> getAllDynamicLights() {
        return Collections.unmodifiableCollection(dynamicLights.values());
    }
}
