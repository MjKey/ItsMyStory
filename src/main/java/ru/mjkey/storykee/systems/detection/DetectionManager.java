package ru.mjkey.storykee.systems.detection;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Manages block and entity detection in the Storykee system.
 * 
 * Requirements: 47.1, 47.2, 47.3, 47.4, 47.5
 */
public class DetectionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectionManager.class);
    
    private static DetectionManager instance;
    
    // Active block change monitors
    private final Map<String, BlockMonitor> blockMonitors;
    
    // Cached block states for change detection
    private final Map<BlockPos, BlockState> cachedBlockStates;
    
    private MinecraftServer server;
    
    private DetectionManager() {
        this.blockMonitors = new ConcurrentHashMap<>();
        this.cachedBlockStates = new ConcurrentHashMap<>();
    }
    
    public static DetectionManager getInstance() {
        if (instance == null) {
            instance = new DetectionManager();
        }
        return instance;
    }
    
    /**
     * Initializes the detection manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("DetectionManager initialized");
    }
    
    /**
     * Shuts down the detection manager.
     */
    public void shutdown() {
        blockMonitors.clear();
        cachedBlockStates.clear();
        LOGGER.info("DetectionManager shutdown");
    }
    
    // ==================== Block Querying ====================
    
    /**
     * Gets the block at a position.
     * Requirement 47.1: Create block querying
     * 
     * @param world Server world
     * @param pos Block position
     * @return Block at position
     */
    public Block getBlock(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        return world.getBlockState(pos).getBlock();
    }
    
    /**
     * Gets the block state at a position.
     * 
     * @param world Server world
     * @param pos Block position
     * @return BlockState at position
     */
    public BlockState getBlockState(ServerLevel world, BlockPos pos) {
        if (world == null || pos == null) {
            return null;
        }
        return world.getBlockState(pos);
    }
    
    /**
     * Checks if a block at position matches a type.
     * 
     * @param world Server world
     * @param pos Block position
     * @param blockId Block ResourceLocation
     * @return true if block matches
     */
    public boolean isBlock(ServerLevel world, BlockPos pos, String blockId) {
        if (world == null || pos == null || blockId == null) {
            return false;
        }
        
        Block block = getBlock(world, pos);
        if (block == null) {
            return false;
        }
        
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation targetId = ResourceLocation.tryParse(blockId);
        if (targetId == null) {
            targetId = ResourceLocation.fromNamespaceAndPath("minecraft", blockId);
        }
        
        return id.equals(targetId);
    }
    
    /**
     * Finds all blocks of a type in an area.
     * Requirement 47.3: Implement area detection
     * 
     * @param world Server world
     * @param from Start position
     * @param to End position
     * @param blockId Block ResourceLocation to find
     * @return List of positions with matching blocks
     */
    public List<BlockPos> findBlocks(ServerLevel world, BlockPos from, BlockPos to, String blockId) {
        if (world == null || from == null || to == null) {
            return Collections.emptyList();
        }
        
        List<BlockPos> found = new ArrayList<>();
        
        int minX = Math.min(from.getX(), to.getX());
        int maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxZ = Math.max(from.getZ(), to.getZ());
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isBlock(world, pos, blockId)) {
                        found.add(pos);
                    }
                }
            }
        }
        
        return found;
    }
    
    /**
     * Counts blocks of a type in an area.
     * 
     * @param world Server world
     * @param from Start position
     * @param to End position
     * @param blockId Block ResourceLocation to count
     * @return Count of matching blocks
     */
    public int countBlocks(ServerLevel world, BlockPos from, BlockPos to, String blockId) {
        return findBlocks(world, from, to, blockId).size();
    }
    
    // ==================== Entity Searching ====================
    
    /**
     * Finds entities in a radius.
     * Requirement 47.2: Add entity searching
     * 
     * @param world Server world
     * @param center Center position
     * @param radius Search radius
     * @return List of entities found
     */
    public List<Entity> findEntitiesInRadius(ServerLevel world, Vec3 center, double radius) {
        return findEntitiesInRadius(world, center, radius, entity -> true);
    }
    
    /**
     * Finds entities in a radius with a filter.
     * 
     * @param world Server world
     * @param center Center position
     * @param radius Search radius
     * @param filter Entity filter
     * @return List of entities found
     */
    public List<Entity> findEntitiesInRadius(ServerLevel world, Vec3 center, double radius, 
                                              Predicate<Entity> filter) {
        if (world == null || center == null || radius <= 0) {
            return Collections.emptyList();
        }
        
        AABB AABB = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        
        return world.getEntitiesOfClass(Entity.class, AABB, 
                entity -> filter.test(entity) && entity.distanceToSqr(center) <= radius * radius);
    }
    
    /**
     * Finds entities of a specific type in a radius.
     * 
     * @param world Server world
     * @param center Center position
     * @param radius Search radius
     * @param entityType Entity type ResourceLocation
     * @return List of entities found
     */
    public List<Entity> findEntitiesByType(ServerLevel world, Vec3 center, double radius, String entityType) {
        ResourceLocation typeId = ResourceLocation.tryParse(entityType);
        if (typeId == null) {
            typeId = ResourceLocation.fromNamespaceAndPath("minecraft", entityType);
        }
        
        final ResourceLocation finalTypeId = typeId;
        return findEntitiesInRadius(world, center, radius, entity -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            return id.equals(finalTypeId);
        });
    }
    
    /**
     * Finds players in a radius.
     * 
     * @param world Server world
     * @param center Center position
     * @param radius Search radius
     * @return List of players found
     */
    public List<Player> findPlayersInRadius(ServerLevel world, Vec3 center, double radius) {
        if (world == null || center == null || radius <= 0) {
            return Collections.emptyList();
        }
        
        AABB AABB = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        
        return world.getEntitiesOfClass(Player.class, AABB,
                player -> player.distanceToSqr(center) <= radius * radius);
    }
    
    /**
     * Finds living entities in a radius.
     * 
     * @param world Server world
     * @param center Center position
     * @param radius Search radius
     * @return List of living entities found
     */
    public List<LivingEntity> findLivingEntitiesInRadius(ServerLevel world, Vec3 center, double radius) {
        if (world == null || center == null || radius <= 0) {
            return Collections.emptyList();
        }
        
        AABB AABB = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );
        
        return world.getEntitiesOfClass(LivingEntity.class, AABB,
                entity -> entity.distanceToSqr(center) <= radius * radius);
    }
    
    /**
     * Finds the nearest entity to a position.
     * 
     * @param world Server world
     * @param center Center position
     * @param radius Search radius
     * @return Nearest entity, or null if none found
     */
    public Entity findNearestEntity(ServerLevel world, Vec3 center, double radius) {
        List<Entity> entities = findEntitiesInRadius(world, center, radius);
        
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            double dist = entity.distanceToSqr(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entity;
            }
        }
        
        return nearest;
    }
    
    /**
     * Finds entities in a AABB area.
     * Requirement 47.3: Implement area detection
     * 
     * @param world Server world
     * @param from Start corner
     * @param to End corner
     * @return List of entities in the AABB
     */
    public List<Entity> findEntitiesInAABB(ServerLevel world, Vec3 from, Vec3 to) {
        if (world == null || from == null || to == null) {
            return Collections.emptyList();
        }
        
        AABB AABB = new AABB(from, to);
        return world.getEntitiesOfClass(Entity.class, AABB, EntitySelector.ENTITY_STILL_ALIVE);
    }
    
    // ==================== Change Monitoring ====================
    
    /**
     * Starts monitoring a block position for changes.
     * Requirement 47.4: Add change monitoring
     * 
     * @param monitorId Unique monitor ID
     * @param world Server world
     * @param pos Block position
     * @param callback Callback when block changes
     * @return true if monitor was created
     */
    public boolean startBlockMonitor(String monitorId, ServerLevel world, BlockPos pos, 
                                      BlockChangeCallback callback) {
        if (monitorId == null || world == null || pos == null || callback == null) {
            return false;
        }
        
        if (blockMonitors.containsKey(monitorId)) {
            LOGGER.warn("Block monitor '{}' already exists", monitorId);
            return false;
        }
        
        BlockState currentState = world.getBlockState(pos);
        cachedBlockStates.put(pos, currentState);
        
        BlockMonitor monitor = new BlockMonitor(monitorId, world, pos, currentState, callback);
        blockMonitors.put(monitorId, monitor);
        
        LOGGER.debug("Started block monitor '{}' at {}", monitorId, pos);
        return true;
    }
    
    /**
     * Stops a block monitor.
     * 
     * @param monitorId Monitor ID
     * @return true if stopped
     */
    public boolean stopBlockMonitor(String monitorId) {
        BlockMonitor monitor = blockMonitors.remove(monitorId);
        if (monitor != null) {
            cachedBlockStates.remove(monitor.pos);
            LOGGER.debug("Stopped block monitor '{}'", monitorId);
            return true;
        }
        return false;
    }
    
    /**
     * Updates all block monitors.
     * Should be called periodically.
     */
    public void tick() {
        for (BlockMonitor monitor : blockMonitors.values()) {
            BlockState currentState = monitor.world.getBlockState(monitor.pos);
            BlockState cachedState = cachedBlockStates.get(monitor.pos);
            
            if (!currentState.equals(cachedState)) {
                cachedBlockStates.put(monitor.pos, currentState);
                
                try {
                    monitor.callback.onBlockChange(monitor.pos, cachedState, currentState);
                } catch (Exception e) {
                    LOGGER.error("Error in block change callback: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Gets all active monitor IDs.
     */
    public Set<String> getActiveMonitors() {
        return Collections.unmodifiableSet(blockMonitors.keySet());
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents a block change monitor.
     */
    private static class BlockMonitor {
        final String id;
        final ServerLevel world;
        final BlockPos pos;
        final BlockState initialState;
        final BlockChangeCallback callback;
        
        BlockMonitor(String id, ServerLevel world, BlockPos pos, 
                    BlockState initialState, BlockChangeCallback callback) {
            this.id = id;
            this.world = world;
            this.pos = pos;
            this.initialState = initialState;
            this.callback = callback;
        }
    }
    
    /**
     * Callback interface for block changes.
     */
    public interface BlockChangeCallback {
        /**
         * Called when a monitored block changes.
         * 
         * @param pos Block position
         * @param oldState Previous block state
         * @param newState New block state
         */
        void onBlockChange(BlockPos pos, BlockState oldState, BlockState newState);
    }
}
