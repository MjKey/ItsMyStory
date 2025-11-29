package ru.mjkey.storykee.systems.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.async.MinecraftThreadBridge;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Handles world modifications in the story system.
 * Provides methods for block placement, removal, region filling, and chunk updates.
 * 
 * Requirements: 21.1, 21.2, 21.3, 21.5
 */
public class WorldModifier {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldModifier.class);
    
    private static WorldModifier instance;
    
    // Reference to the Minecraft server for world access
    private MinecraftServer server;
    
    // Permission checker for world modifications
    private WorldPermissionChecker permissionChecker;
    
    // Block update flags
    public static final int UPDATE_NEIGHBORS = Block.UPDATE_NEIGHBORS;
    public static final int UPDATE_CLIENTS = Block.UPDATE_CLIENTS;
    public static final int UPDATE_ALL = Block.UPDATE_ALL;
    
    private WorldModifier() {
        this.permissionChecker = new WorldPermissionChecker();
    }
    
    public static WorldModifier getInstance() {
        if (instance == null) {
            instance = new WorldModifier();
        }
        return instance;
    }
    
    /**
     * Sets the Minecraft server reference.
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Gets the Minecraft server reference.
     */
    public MinecraftServer getServer() {
        return server;
    }
    
    /**
     * Gets the permission checker.
     */
    public WorldPermissionChecker getPermissionChecker() {
        return permissionChecker;
    }

    
    // ===== Block Operations =====
    
    /**
     * Sets a block at the specified position.
     * Requirement 21.1: WHEN a script places a block THEN the Runtime SHALL set the block at the specified coordinates
     */
    public boolean setBlock(Level world, BlockPos pos, BlockState state) {
        return setBlock(world, pos, state, UPDATE_ALL);
    }
    
    /**
     * Sets a block at the specified position with custom update flags.
     */
    public boolean setBlock(Level world, BlockPos pos, BlockState state, int flags) {
        if (world == null || pos == null || state == null) {
            LOGGER.warn("setBlock: Invalid arguments - world={}, pos={}, state={}", world, pos, state);
            return false;
        }
        
        // Check permissions
        if (!permissionChecker.canModifyBlock(world, pos)) {
            LOGGER.warn("setBlock: Permission denied for position {}", pos);
            return false;
        }
        
        try {
            if (world instanceof ServerLevel serverLevel) {
                if (serverLevel.getServer().isSameThread()) {
                    return world.setBlock(pos, state, flags);
                } else {
                    CompletableFuture<Boolean> future = new CompletableFuture<>();
                    MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
                        boolean result = world.setBlock(pos, state, flags);
                        future.complete(result);
                    });
                    return future.join();
                }
            } else {
                return world.setBlock(pos, state, flags);
            }
        } catch (Exception e) {
            LOGGER.error("setBlock: Error setting block at {}: {}", pos, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Sets a block by block ID string.
     */
    public boolean setBlock(Level world, int x, int y, int z, String blockId) {
        BlockPos pos = new BlockPos(x, y, z);
        Optional<BlockState> state = parseBlockState(blockId);
        
        if (state.isEmpty()) {
            LOGGER.warn("setBlock: Unknown block type: {}", blockId);
            return false;
        }
        
        return setBlock(world, pos, state.get());
    }
    
    /**
     * Gets the block state at the specified position.
     */
    public BlockState getBlock(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return Blocks.AIR.defaultBlockState();
        }
        
        return world.getBlockState(pos);
    }
    
    /**
     * Gets the block ID string at the specified position.
     */
    public String getBlockId(Level world, int x, int y, int z) {
        BlockState state = getBlock(world, new BlockPos(x, y, z));
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }
    
    /**
     * Removes a block at the specified position (sets to air).
     * Requirement 21.2: WHEN a script removes a block THEN the Runtime SHALL break the block
     */
    public boolean removeBlock(Level world, BlockPos pos, boolean dropItems) {
        if (world == null || pos == null) {
            return false;
        }
        
        // Check permissions
        if (!permissionChecker.canModifyBlock(world, pos)) {
            LOGGER.warn("removeBlock: Permission denied for position {}", pos);
            return false;
        }
        
        try {
            if (world instanceof ServerLevel serverLevel) {
                if (dropItems) {
                    return serverLevel.destroyBlock(pos, true);
                } else {
                    return setBlock(world, pos, Blocks.AIR.defaultBlockState());
                }
            } else {
                return setBlock(world, pos, Blocks.AIR.defaultBlockState());
            }
        } catch (Exception e) {
            LOGGER.error("removeBlock: Error removing block at {}: {}", pos, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Removes a block at the specified coordinates.
     */
    public boolean removeBlock(Level world, int x, int y, int z, boolean dropItems) {
        return removeBlock(world, new BlockPos(x, y, z), dropItems);
    }

    
    // ===== Region Operations =====
    
    /**
     * Fills a region with a specific block state.
     * Requirement 21.3: WHEN a script modifies terrain THEN the Runtime SHALL update chunk data and notify clients
     */
    public int fillRegion(Level world, BlockPos from, BlockPos to, BlockState state) {
        return fillRegion(world, from, to, state, null);
    }
    
    /**
     * Fills a region with a specific block state, optionally filtering by existing block.
     */
    public int fillRegion(Level world, BlockPos from, BlockPos to, BlockState state, BlockState filter) {
        if (world == null || from == null || to == null || state == null) {
            return 0;
        }
        
        // Calculate bounds
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());
        
        // Check region size limit (prevent server lag)
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        int maxVolume = 32768; // 32^3 blocks max
        
        if (volume > maxVolume) {
            LOGGER.warn("fillRegion: Region too large ({} blocks, max {})", volume, maxVolume);
            return -1;
        }
        
        int changedCount = 0;
        
        try {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        
                        // Check permissions for each block
                        if (!permissionChecker.canModifyBlock(world, pos)) {
                            continue;
                        }
                        
                        // Check filter if specified
                        if (filter != null) {
                            BlockState current = world.getBlockState(pos);
                            if (!current.equals(filter)) {
                                continue;
                            }
                        }
                        
                        if (world.setBlock(pos, state, UPDATE_ALL)) {
                            changedCount++;
                        }
                    }
                }
            }
            
            LOGGER.info("fillRegion: Changed {} blocks from {} to {}", changedCount, from, to);
            return changedCount;
            
        } catch (Exception e) {
            LOGGER.error("fillRegion: Error filling region: {}", e.getMessage(), e);
            return changedCount;
        }
    }
    
    /**
     * Fills a region with a specific block by ID.
     */
    public int fillRegion(Level world, int x1, int y1, int z1, int x2, int y2, int z2, String blockId) {
        Optional<BlockState> state = parseBlockState(blockId);
        if (state.isEmpty()) {
            LOGGER.warn("fillRegion: Unknown block type: {}", blockId);
            return 0;
        }
        
        return fillRegion(world, new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2), state.get());
    }
    
    /**
     * Replaces all blocks of one type with another in a region.
     */
    public int replaceRegion(Level world, BlockPos from, BlockPos to, String oldBlockId, String newBlockId) {
        Optional<BlockState> oldState = parseBlockState(oldBlockId);
        Optional<BlockState> newState = parseBlockState(newBlockId);
        
        if (oldState.isEmpty() || newState.isEmpty()) {
            LOGGER.warn("replaceRegion: Unknown block type: {} or {}", oldBlockId, newBlockId);
            return 0;
        }
        
        return fillRegion(world, from, to, newState.get(), oldState.get());
    }
    
    /**
     * Clears a region (fills with air).
     */
    public int clearRegion(Level world, BlockPos from, BlockPos to) {
        return fillRegion(world, from, to, Blocks.AIR.defaultBlockState());
    }

    
    // ===== Async Operations =====
    
    /**
     * Sets a block asynchronously.
     */
    public CompletableFuture<Boolean> setBlockAsync(Level world, BlockPos pos, BlockState state) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            boolean result = setBlock(world, pos, state);
            future.complete(result);
        });
        
        return future;
    }
    
    /**
     * Fills a region asynchronously with progress callback.
     */
    public CompletableFuture<Integer> fillRegionAsync(Level world, BlockPos from, BlockPos to, 
                                                       BlockState state, Consumer<Integer> progressCallback) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        
        // Calculate bounds
        int minX = Math.min(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxY = Math.max(from.getY(), to.getY());
        int maxZ = Math.max(from.getZ(), to.getZ());
        
        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        
        // Process in batches to avoid blocking the main thread
        int batchSize = 1000;
        int[] changedCount = {0};
        int[] processedCount = {0};
        
        processRegionBatch(world, minX, minY, minZ, maxX, maxY, maxZ, 
                          state, batchSize, changedCount, processedCount, 
                          totalBlocks, progressCallback, future);
        
        return future;
    }
    
    private void processRegionBatch(Level world, int minX, int minY, int minZ, 
                                    int maxX, int maxY, int maxZ, BlockState state,
                                    int batchSize, int[] changedCount, int[] processedCount,
                                    int totalBlocks, Consumer<Integer> progressCallback,
                                    CompletableFuture<Integer> future) {
        
        MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            int processed = 0;
            
            outerLoop:
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // Skip already processed blocks
                        int linearIndex = (x - minX) * (maxY - minY + 1) * (maxZ - minZ + 1) 
                                        + (y - minY) * (maxZ - minZ + 1) 
                                        + (z - minZ);
                        
                        if (linearIndex < processedCount[0]) {
                            continue;
                        }
                        
                        BlockPos pos = new BlockPos(x, y, z);
                        
                        if (permissionChecker.canModifyBlock(world, pos)) {
                            if (world.setBlock(pos, state, UPDATE_ALL)) {
                                changedCount[0]++;
                            }
                        }
                        
                        processedCount[0]++;
                        processed++;
                        
                        if (processed >= batchSize) {
                            break outerLoop;
                        }
                    }
                }
            }
            
            // Report progress
            if (progressCallback != null) {
                int progress = (int) ((processedCount[0] * 100.0) / totalBlocks);
                progressCallback.accept(progress);
            }
            
            // Check if done
            if (processedCount[0] >= totalBlocks) {
                future.complete(changedCount[0]);
            } else {
                // Schedule next batch
                processRegionBatch(world, minX, minY, minZ, maxX, maxY, maxZ,
                                  state, batchSize, changedCount, processedCount,
                                  totalBlocks, progressCallback, future);
            }
        });
    }

    
    // ===== Utility Methods =====
    
    /**
     * Parses a block ID string into a BlockState.
     */
    public Optional<BlockState> parseBlockState(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return Optional.empty();
        }
        
        // Add minecraft namespace if not present
        String fullId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
        
        try {
            ResourceLocation location = ResourceLocation.parse(fullId);
            Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(location);
            
            if (blockOpt.isPresent()) {
                return Optional.of(blockOpt.get().defaultBlockState());
            }
            
            // Try without namespace
            location = ResourceLocation.withDefaultNamespace(blockId);
            blockOpt = BuiltInRegistries.BLOCK.getOptional(location);
            
            if (blockOpt.isPresent()) {
                return Optional.of(blockOpt.get().defaultBlockState());
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            LOGGER.warn("parseBlockState: Invalid block ID: {}", blockId);
            return Optional.empty();
        }
    }
    
    /**
     * Gets the world by dimension ResourceLocation.
     */
    public ServerLevel getWorld(String dimensionId) {
        if (server == null) {
            LOGGER.warn("getWorld: Server not initialized");
            return null;
        }
        
        String fullId = dimensionId.contains(":") ? dimensionId : "minecraft:" + dimensionId;
        
        for (ServerLevel world : server.getAllLevels()) {
            String worldId = world.dimension().location().toString();
            if (worldId.equals(fullId)) {
                return world;
            }
        }
        
        // Try common aliases
        switch (dimensionId.toLowerCase()) {
            case "overworld":
                return server.overworld();
            case "nether":
            case "the_nether":
                return getWorld("minecraft:the_nether");
            case "end":
            case "the_end":
                return getWorld("minecraft:the_end");
            default:
                LOGGER.warn("getWorld: Unknown dimension: {}", dimensionId);
                return null;
        }
    }
    
    /**
     * Gets the overworld.
     */
    public ServerLevel getOverworld() {
        return server != null ? server.overworld() : null;
    }
    
    /**
     * Checks if a position is within world bounds.
     */
    public boolean isValidPosition(Level world, BlockPos pos) {
        if (world == null || pos == null) {
            return false;
        }
        
        return world.isInWorldBounds(pos);
    }
    
    /**
     * Forces a chunk update at the specified position.
     * Requirement 21.3: update chunk data and notify clients
     */
    public void updateChunk(Level world, BlockPos pos) {
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
        }
    }
    
    /**
     * Forces chunk updates for a region.
     */
    public void updateChunksInRegion(Level world, BlockPos from, BlockPos to) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        
        int minChunkX = from.getX() >> 4;
        int minChunkZ = from.getZ() >> 4;
        int maxChunkX = to.getX() >> 4;
        int maxChunkZ = to.getZ() >> 4;
        
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                BlockPos chunkPos = new BlockPos(cx << 4, from.getY(), cz << 4);
                serverLevel.getChunkSource().blockChanged(chunkPos);
            }
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void shutdown() {
        this.server = null;
        LOGGER.info("WorldModifier shutdown complete");
    }
}
