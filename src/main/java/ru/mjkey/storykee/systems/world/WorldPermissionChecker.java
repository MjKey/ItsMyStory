package ru.mjkey.storykee.systems.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Handles permission checking for world modifications.
 * Requirement 21.5: WHEN world modifications are made THEN the Runtime SHALL respect world protection and permission systems
 */
public class WorldPermissionChecker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldPermissionChecker.class);
    
    // Protected regions that cannot be modified
    private final Map<String, ProtectedRegion> protectedRegions;
    
    // Global permission settings
    private boolean worldModificationEnabled = true;
    private boolean requireOperatorPermission = false;
    private int minimumPermissionLevel = 0;
    
    // Dimension-specific permissions
    private final Set<String> allowedDimensions;
    private final Set<String> blockedDimensions;
    
    public WorldPermissionChecker() {
        this.protectedRegions = new HashMap<>();
        this.allowedDimensions = new HashSet<>();
        this.blockedDimensions = new HashSet<>();
    }
    
    /**
     * Checks if a block at the given position can be modified.
     */
    public boolean canModifyBlock(Level world, BlockPos pos) {
        if (!worldModificationEnabled) {
            return false;
        }
        
        if (world == null || pos == null) {
            return false;
        }
        
        // Check dimension permissions
        String dimensionId = world.dimension().location().toString();
        
        if (!blockedDimensions.isEmpty() && blockedDimensions.contains(dimensionId)) {
            LOGGER.debug("canModifyBlock: Dimension {} is blocked", dimensionId);
            return false;
        }
        
        if (!allowedDimensions.isEmpty() && !allowedDimensions.contains(dimensionId)) {
            LOGGER.debug("canModifyBlock: Dimension {} is not in allowed list", dimensionId);
            return false;
        }
        
        // Check protected regions
        for (ProtectedRegion region : protectedRegions.values()) {
            if (region.contains(world, pos)) {
                LOGGER.debug("canModifyBlock: Position {} is in protected region {}", pos, region.getName());
                return false;
            }
        }
        
        // Check world bounds
        if (!world.isInWorldBounds(pos)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a player has permission to modify blocks.
     */
    public boolean canPlayerModify(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        
        if (!worldModificationEnabled) {
            return false;
        }
        
        if (requireOperatorPermission && !player.hasPermissions(2)) {
            return false;
        }
        
        if (player.getPermissionLevel() < minimumPermissionLevel) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a player can modify a specific block.
     */
    public boolean canPlayerModifyBlock(ServerPlayer player, Level world, BlockPos pos) {
        return canPlayerModify(player) && canModifyBlock(world, pos);
    }

    
    // ===== Protected Region Management =====
    
    /**
     * Adds a protected region.
     */
    public void addProtectedRegion(String name, String dimensionId, BlockPos from, BlockPos to) {
        ProtectedRegion region = new ProtectedRegion(name, dimensionId, from, to);
        protectedRegions.put(name, region);
        LOGGER.info("Added protected region: {} in {} from {} to {}", name, dimensionId, from, to);
    }
    
    /**
     * Removes a protected region.
     */
    public boolean removeProtectedRegion(String name) {
        ProtectedRegion removed = protectedRegions.remove(name);
        if (removed != null) {
            LOGGER.info("Removed protected region: {}", name);
            return true;
        }
        return false;
    }
    
    /**
     * Gets a protected region by name.
     */
    public ProtectedRegion getProtectedRegion(String name) {
        return protectedRegions.get(name);
    }
    
    /**
     * Gets all protected regions.
     */
    public Collection<ProtectedRegion> getAllProtectedRegions() {
        return Collections.unmodifiableCollection(protectedRegions.values());
    }
    
    /**
     * Clears all protected regions.
     */
    public void clearProtectedRegions() {
        protectedRegions.clear();
        LOGGER.info("Cleared all protected regions");
    }
    
    // ===== Dimension Permissions =====
    
    public void allowDimension(String dimensionId) {
        allowedDimensions.add(dimensionId);
    }
    
    public void disallowDimension(String dimensionId) {
        allowedDimensions.remove(dimensionId);
    }
    
    public void blockDimension(String dimensionId) {
        blockedDimensions.add(dimensionId);
    }
    
    public void unblockDimension(String dimensionId) {
        blockedDimensions.remove(dimensionId);
    }
    
    public void clearDimensionRestrictions() {
        allowedDimensions.clear();
        blockedDimensions.clear();
    }
    
    // ===== Global Settings =====
    
    public void setWorldModificationEnabled(boolean enabled) {
        this.worldModificationEnabled = enabled;
        LOGGER.info("World modification {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isWorldModificationEnabled() {
        return worldModificationEnabled;
    }
    
    public void setRequireOperatorPermission(boolean required) {
        this.requireOperatorPermission = required;
    }
    
    public boolean isRequireOperatorPermission() {
        return requireOperatorPermission;
    }
    
    public void setMinimumPermissionLevel(int level) {
        this.minimumPermissionLevel = Math.max(0, Math.min(4, level));
    }
    
    public int getMinimumPermissionLevel() {
        return minimumPermissionLevel;
    }
    
    // ===== Protected Region Class =====
    
    public static class ProtectedRegion {
        private final String name;
        private final String dimensionId;
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        
        public ProtectedRegion(String name, String dimensionId, BlockPos from, BlockPos to) {
            this.name = name;
            this.dimensionId = dimensionId;
            this.minX = Math.min(from.getX(), to.getX());
            this.minY = Math.min(from.getY(), to.getY());
            this.minZ = Math.min(from.getZ(), to.getZ());
            this.maxX = Math.max(from.getX(), to.getX());
            this.maxY = Math.max(from.getY(), to.getY());
            this.maxZ = Math.max(from.getZ(), to.getZ());
        }
        
        public String getName() {
            return name;
        }
        
        public String getDimensionId() {
            return dimensionId;
        }
        
        public boolean contains(Level world, BlockPos pos) {
            if (world == null || pos == null) {
                return false;
            }
            
            String worldDimension = world.dimension().location().toString();
            if (!worldDimension.equals(dimensionId)) {
                return false;
            }
            
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            
            return x >= minX && x <= maxX 
                && y >= minY && y <= maxY 
                && z >= minZ && z <= maxZ;
        }
        
        public BlockPos getMin() {
            return new BlockPos(minX, minY, minZ);
        }
        
        public BlockPos getMax() {
            return new BlockPos(maxX, maxY, maxZ);
        }
    }
}
