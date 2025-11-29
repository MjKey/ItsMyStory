package ru.mjkey.storykee.systems.teleport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages teleportation in the Storykee system.
 * 
 * Requirements: 43.1, 43.2, 43.3, 43.4, 43.5
 */
public class TeleportManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportManager.class);
    
    private static TeleportManager instance;
    
    // Saved locations by ID
    private final Map<String, SavedLocation> savedLocations;
    
    // Player-specific saved locations
    private final Map<UUID, Map<String, SavedLocation>> playerLocations;
    
    private MinecraftServer server;
    
    private TeleportManager() {
        this.savedLocations = new ConcurrentHashMap<>();
        this.playerLocations = new ConcurrentHashMap<>();
    }
    
    public static TeleportManager getInstance() {
        if (instance == null) {
            instance = new TeleportManager();
        }
        return instance;
    }
    
    /**
     * Initializes the teleport manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("TeleportManager initialized");
    }
    
    /**
     * Shuts down the teleport manager.
     */
    public void shutdown() {
        savedLocations.clear();
        playerLocations.clear();
        LOGGER.info("TeleportManager shutdown");
    }
    
    // ==================== Teleportation ====================
    
    /**
     * Teleports a player to coordinates.
     * Requirement 43.1: Create teleportation function
     * 
     * @param player Player to teleport
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return true if teleported successfully
     */
    public boolean teleport(ServerPlayer player, double x, double y, double z) {
        return teleport(player, x, y, z, player.getYRot(), player.getXRot());
    }
    
    /**
     * Teleports a player to coordinates with rotation.
     * 
     * @param player Player to teleport
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param yaw Yaw rotation
     * @param pitch Pitch rotation
     * @return true if teleported successfully
     */
    public boolean teleport(ServerPlayer player, double x, double y, double z, float yaw, float pitch) {
        if (player == null) {
            return false;
        }
        
        // Safety check
        // Requirement 43.4: Add safety checks
        if (!isSafeLocation((ServerLevel)player.level(), x, y, z)) {
            Vec3 safePos = findSafeLocation((ServerLevel)player.level(), x, y, z);
            if (safePos != null) {
                x = safePos.x;
                y = safePos.y;
                z = safePos.z;
            } else {
                LOGGER.warn("Cannot find safe teleport location near ({}, {}, {})", x, y, z);
                return false;
            }
        }
        
        player.teleportTo((ServerLevel)player.level(), x, y, z, Set.of(), yaw, pitch, false);
        LOGGER.debug("Teleported {} to ({}, {}, {})", player.getName().getString(), x, y, z);
        return true;
    }
    
    /**
     * Teleports a player to a saved location.
     * 
     * @param player Player to teleport
     * @param locationId Saved location ID
     * @return true if teleported successfully
     */
    public boolean teleportToLocation(ServerPlayer player, String locationId) {
        SavedLocation location = savedLocations.get(locationId);
        if (location == null) {
            LOGGER.warn("Saved location '{}' not found", locationId);
            return false;
        }
        
        return teleportToLocation(player, location);
    }
    
    /**
     * Teleports a player to a saved location.
     * 
     * @param player Player to teleport
     * @param location Saved location
     * @return true if teleported successfully
     */
    public boolean teleportToLocation(ServerPlayer player, SavedLocation location) {
        if (player == null || location == null) {
            return false;
        }
        
        // Handle cross-dimension teleport
        // Requirement 43.3: Cross-dimension support
        ServerLevel targetWorld = getWorld(location.getDimension());
        if (targetWorld == null) {
            LOGGER.warn("Dimension '{}' not found", location.getDimension());
            return false;
        }
        
        Vec3 pos = location.getPosition();
        
        // Safety check
        if (!isSafeLocation(targetWorld, pos.x, pos.y, pos.z)) {
            Vec3 safePos = findSafeLocation(targetWorld, pos.x, pos.y, pos.z);
            if (safePos != null) {
                pos = safePos;
            } else {
                LOGGER.warn("Cannot find safe teleport location at {}", location);
                return false;
            }
        }
        
        player.teleportTo(targetWorld, pos.x, pos.y, pos.z, Set.of(), 
                location.getYaw(), location.getPitch(), false);
        
        LOGGER.debug("Teleported {} to location '{}'", player.getName().getString(), location.getId());
        return true;
    }
    
    /**
     * Teleports a player to another dimension at their current coordinates.
     * Requirement 43.3: Cross-dimension support
     * 
     * @param player Player to teleport
     * @param dimension Target dimension
     * @return true if teleported successfully
     */
    public boolean teleportToDimension(ServerPlayer player, ResourceLocation dimension) {
        if (player == null || dimension == null) {
            return false;
        }
        
        ServerLevel targetWorld = getWorld(dimension);
        if (targetWorld == null) {
            LOGGER.warn("Dimension '{}' not found", dimension);
            return false;
        }
        
        Vec3 pos = player.position();
        
        // Find safe location in target dimension
        Vec3 safePos = findSafeLocation(targetWorld, pos.x, pos.y, pos.z);
        if (safePos == null) {
            // Fallback to world origin at sea level if no safe location found
            safePos = new Vec3(0.5, targetWorld.getSeaLevel(), 0.5);
        }
        
        player.teleportTo(targetWorld, safePos.x, safePos.y, safePos.z, Set.of(),
                player.getYRot(), player.getXRot(), false);
        
        LOGGER.debug("Teleported {} to dimension '{}'", player.getName().getString(), dimension);
        return true;
    }
    
    // ==================== Location Saving ====================
    
    /**
     * Saves a location.
     * Requirement 43.2: Location saving/loading
     * 
     * @param location Location to save
     * @return true if saved
     */
    public boolean saveLocation(SavedLocation location) {
        if (location == null || location.getId() == null) {
            return false;
        }
        
        savedLocations.put(location.getId(), location);
        LOGGER.debug("Saved location: {}", location.getId());
        return true;
    }
    
    /**
     * Saves a player's current location.
     * 
     * @param locationId Location ID
     * @param player Player whose location to save
     * @return The saved location
     */
    public SavedLocation savePlayerLocation(String locationId, ServerPlayer player) {
        if (player == null || locationId == null) {
            return null;
        }
        
        SavedLocation location = new SavedLocation(
                locationId,
                player.position(),
                player.getYRot(),
                player.getXRot(),
                player.level().dimension().location()
        );
        
        saveLocation(location);
        return location;
    }
    
    /**
     * Gets a saved location.
     * 
     * @param locationId Location ID
     * @return The location, or null if not found
     */
    public SavedLocation getLocation(String locationId) {
        return savedLocations.get(locationId);
    }
    
    /**
     * Removes a saved location.
     * 
     * @param locationId Location ID
     * @return true if removed
     */
    public boolean removeLocation(String locationId) {
        return savedLocations.remove(locationId) != null;
    }
    
    /**
     * Gets all saved location IDs.
     */
    public Set<String> getLocationIds() {
        return Collections.unmodifiableSet(savedLocations.keySet());
    }
    
    // ==================== Player-Specific Locations ====================
    
    /**
     * Saves a location for a specific player.
     * 
     * @param playerId Player UUID
     * @param location Location to save
     */
    public void savePlayerSpecificLocation(UUID playerId, SavedLocation location) {
        playerLocations.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(location.getId(), location);
    }
    
    /**
     * Gets a player-specific saved location.
     * 
     * @param playerId Player UUID
     * @param locationId Location ID
     * @return The location, or null if not found
     */
    public SavedLocation getPlayerSpecificLocation(UUID playerId, String locationId) {
        Map<String, SavedLocation> locations = playerLocations.get(playerId);
        return locations != null ? locations.get(locationId) : null;
    }
    
    /**
     * Removes a player-specific saved location.
     * 
     * @param playerId Player UUID
     * @param locationId Location ID
     * @return true if removed
     */
    public boolean removePlayerSpecificLocation(UUID playerId, String locationId) {
        Map<String, SavedLocation> locations = playerLocations.get(playerId);
        return locations != null && locations.remove(locationId) != null;
    }
    
    // ==================== Safety Checks ====================
    
    /**
     * Checks if a location is safe for teleportation.
     * Requirement 43.4: Safety checks
     */
    private boolean isSafeLocation(ServerLevel world, double x, double y, double z) {
        BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
        BlockPos below = pos.below();
        BlockPos above = pos.above();
        
        // Check if there's solid ground below
        boolean hasGround = !world.getBlockState(below).isAir();
        
        // Check if there's space for the player
        boolean hasSpace = world.getBlockState(pos).isAir() && world.getBlockState(above).isAir();
        
        // Check if not in void
        boolean notInVoid = y > world.getMinY() && y < world.getMaxY();
        
        return hasGround && hasSpace && notInVoid;
    }
    
    /**
     * Finds a safe location near the target.
     * Requirement 43.4: Safety checks
     */
    private Vec3 findSafeLocation(ServerLevel world, double x, double y, double z) {
        // Search in expanding radius
        for (int radius = 0; radius <= 5; radius++) {
            for (int dy = 0; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        double testX = x + dx;
                        double testY = y + dy;
                        double testZ = z + dz;
                        
                        if (isSafeLocation(world, testX, testY, testZ)) {
                            return new Vec3(testX + 0.5, testY, testZ + 0.5);
                        }
                        
                        // Also check below
                        testY = y - dy;
                        if (dy > 0 && isSafeLocation(world, testX, testY, testZ)) {
                            return new Vec3(testX + 0.5, testY, testZ + 0.5);
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Gets a world by dimension ResourceLocation.
     */
    private ServerLevel getWorld(ResourceLocation dimension) {
        if (server == null || dimension == null) {
            return null;
        }
        
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, dimension);
        return server.getLevel(worldKey);
    }
}
