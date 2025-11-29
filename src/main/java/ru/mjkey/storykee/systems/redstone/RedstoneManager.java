package ru.mjkey.storykee.systems.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages redstone integration for Storykee scripts.
 * Provides functions for activating redstone, controlling power levels, and monitoring changes.
 * 
 * Requirements: 54.1, 54.2, 54.3, 54.4, 54.5
 */
public class RedstoneManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RedstoneManager.class);
    
    private static RedstoneManager instance;
    
    // Tracked redstone positions for event handling
    private final Map<String, Set<BlockPos>> trackedPositions = new ConcurrentHashMap<>();
    
    // Redstone change callbacks: dimension:pos -> callbacks
    private final Map<String, List<Consumer<RedstoneChangeEvent>>> changeCallbacks = new ConcurrentHashMap<>();
    
    // Previous power levels for change detection
    private final Map<String, Map<BlockPos, Integer>> previousPowerLevels = new ConcurrentHashMap<>();
    
    private MinecraftServer server;
    
    private RedstoneManager() {
    }
    
    public static RedstoneManager getInstance() {
        if (instance == null) {
            instance = new RedstoneManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Redstone Activation (Requirement 54.1) =====
    
    /**
     * Activates redstone at a position (powers the block).
     * Requirement 54.1: WHEN a script activates redstone THEN the Runtime SHALL power the specified block
     */
    public boolean activateRedstone(BlockPos pos, String dimension) {
        return setPowerLevel(pos, dimension, 15);
    }
    
    /**
     * Deactivates redstone at a position.
     * Requirement 54.2: WHEN a script deactivates redstone THEN the Runtime SHALL remove power from the block
     */
    public boolean deactivateRedstone(BlockPos pos, String dimension) {
        return setPowerLevel(pos, dimension, 0);
    }
    
    /**
     * Toggles a lever at a position.
     */
    public boolean toggleLever(BlockPos pos, String dimension) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LeverBlock) {
            boolean currentlyPowered = state.getValue(BlockStateProperties.POWERED);
            BlockState newState = state.setValue(BlockStateProperties.POWERED, !currentlyPowered);
            level.setBlock(pos, newState, 3);
            
            // Update neighbors
            level.updateNeighborsAt(pos, state.getBlock());
            
            LOGGER.info("toggleLever: Toggled lever at {} to {}", pos, !currentlyPowered);
            return true;
        }
        
        LOGGER.warn("toggleLever: No lever at {}", pos);
        return false;
    }
    
    /**
     * Sets a lever to a specific state.
     */
    public boolean setLever(BlockPos pos, String dimension, boolean powered) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof LeverBlock) {
            BlockState newState = state.setValue(BlockStateProperties.POWERED, powered);
            level.setBlock(pos, newState, 3);
            level.updateNeighborsAt(pos, state.getBlock());
            
            LOGGER.info("setLever: Set lever at {} to {}", pos, powered);
            return true;
        }
        
        return false;
    }
    
    /**
     * Presses a button at a position.
     */
    public boolean pressButton(BlockPos pos, String dimension) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ButtonBlock button) {
            // Activate the button
            BlockState newState = state.setValue(BlockStateProperties.POWERED, true);
            level.setBlock(pos, newState, 3);
            level.updateNeighborsAt(pos, state.getBlock());
            
            // Schedule deactivation (buttons auto-deactivate after ~20 ticks for wood, ~10 for stone)
            level.scheduleTick(pos, state.getBlock(), 20);
            
            LOGGER.info("pressButton: Pressed button at {}", pos);
            return true;
        }
        
        return false;
    }

    // ===== Power Level Control (Requirement 54.2) =====
    
    /**
     * Sets the power level at a position.
     * Requirement 54.2: WHEN a script deactivates redstone THEN the Runtime SHALL remove power from the block
     * 
     * Note: Direct power level control requires placing redstone components.
     * This implementation places/removes redstone blocks for power control.
     */
    public boolean setPowerLevel(BlockPos pos, String dimension, int powerLevel) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        powerLevel = Math.max(0, Math.min(15, powerLevel));
        
        BlockState currentState = level.getBlockState(pos);
        Block currentBlock = currentState.getBlock();
        
        // Handle different block types
        if (currentBlock instanceof LeverBlock) {
            return setLever(pos, dimension, powerLevel > 0);
        }
        
        // For other blocks, we can place/remove a redstone block
        if (powerLevel > 0) {
            // Place a redstone block to provide power
            if (currentState.isAir()) {
                level.setBlock(pos, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                LOGGER.info("setPowerLevel: Placed redstone block at {} for power level {}", pos, powerLevel);
            }
        } else {
            // Remove redstone block
            if (currentBlock == Blocks.REDSTONE_BLOCK) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                LOGGER.info("setPowerLevel: Removed redstone block at {}", pos);
            }
        }
        
        // Update neighbors
        level.updateNeighborsAt(pos, currentBlock);
        
        return true;
    }
    
    /**
     * Places a redstone torch at a position.
     */
    public boolean placeRedstoneTorch(BlockPos pos, String dimension, boolean lit) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockState state = lit ? 
            Blocks.REDSTONE_TORCH.defaultBlockState() :
            Blocks.REDSTONE_TORCH.defaultBlockState().setValue(RedstoneTorchBlock.LIT, false);
        
        level.setBlock(pos, state, 3);
        level.updateNeighborsAt(pos, Blocks.REDSTONE_TORCH);
        
        LOGGER.info("placeRedstoneTorch: Placed {} torch at {}", lit ? "lit" : "unlit", pos);
        return true;
    }

    // ===== State Checking (Requirement 54.3) =====
    
    /**
     * Gets the redstone power level at a position.
     * Requirement 54.3: WHEN a script checks redstone state THEN the Runtime SHALL return the current power level
     */
    public int getPowerLevel(BlockPos pos, String dimension) {
        if (server == null) return 0;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return 0;
        
        return level.getBestNeighborSignal(pos);
    }
    
    /**
     * Gets the direct power level at a position from a specific direction.
     */
    public int getDirectPowerLevel(BlockPos pos, String dimension, Direction direction) {
        if (server == null) return 0;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return 0;
        
        return level.getSignal(pos, direction);
    }
    
    /**
     * Checks if a position is powered.
     */
    public boolean isPowered(BlockPos pos, String dimension) {
        return getPowerLevel(pos, dimension) > 0;
    }
    
    /**
     * Checks if a block is a redstone component.
     */
    public boolean isRedstoneComponent(BlockPos pos, String dimension) {
        if (server == null) return false;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return false;
        
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        
        return block == Blocks.REDSTONE_WIRE ||
               block == Blocks.REDSTONE_TORCH ||
               block == Blocks.REDSTONE_WALL_TORCH ||
               block == Blocks.REDSTONE_BLOCK ||
               block == Blocks.REPEATER ||
               block == Blocks.COMPARATOR ||
               block instanceof LeverBlock ||
               block instanceof ButtonBlock;
    }

    // ===== Event Handlers (Requirement 54.4) =====
    
    /**
     * Registers a callback for redstone changes at a position.
     * Requirement 54.4: WHEN redstone changes occur THEN the Runtime SHALL optionally trigger event handlers
     */
    public void onRedstoneChange(BlockPos pos, String dimension, Consumer<RedstoneChangeEvent> callback) {
        String key = dimension + ":" + pos.toShortString();
        changeCallbacks.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(callback);
        
        // Track the position
        trackedPositions.computeIfAbsent(dimension, k -> ConcurrentHashMap.newKeySet())
            .add(pos);
        
        // Store initial power level
        previousPowerLevels.computeIfAbsent(dimension, k -> new ConcurrentHashMap<>())
            .put(pos, getPowerLevel(pos, dimension));
        
        LOGGER.debug("onRedstoneChange: Registered callback for {} in {}", pos, dimension);
    }
    
    /**
     * Removes redstone change callbacks for a position.
     */
    public void removeRedstoneCallback(BlockPos pos, String dimension) {
        String key = dimension + ":" + pos.toShortString();
        changeCallbacks.remove(key);
        
        Set<BlockPos> dimPositions = trackedPositions.get(dimension);
        if (dimPositions != null) {
            dimPositions.remove(pos);
        }
        
        Map<BlockPos, Integer> dimPower = previousPowerLevels.get(dimension);
        if (dimPower != null) {
            dimPower.remove(pos);
        }
    }
    
    /**
     * Checks tracked positions for changes and triggers callbacks.
     * Should be called periodically (e.g., every tick).
     */
    public void checkForChanges() {
        for (Map.Entry<String, Set<BlockPos>> entry : trackedPositions.entrySet()) {
            String dimension = entry.getKey();
            Map<BlockPos, Integer> dimPower = previousPowerLevels.get(dimension);
            if (dimPower == null) continue;
            
            for (BlockPos pos : entry.getValue()) {
                int currentPower = getPowerLevel(pos, dimension);
                Integer previousPower = dimPower.get(pos);
                
                if (previousPower != null && currentPower != previousPower) {
                    // Power changed - trigger callbacks
                    triggerChangeCallbacks(pos, dimension, previousPower, currentPower);
                    dimPower.put(pos, currentPower);
                }
            }
        }
    }
    
    private void triggerChangeCallbacks(BlockPos pos, String dimension, int oldPower, int newPower) {
        String key = dimension + ":" + pos.toShortString();
        List<Consumer<RedstoneChangeEvent>> callbacks = changeCallbacks.get(key);
        if (callbacks == null) return;
        
        RedstoneChangeEvent event = new RedstoneChangeEvent(pos, dimension, oldPower, newPower);
        
        for (Consumer<RedstoneChangeEvent> callback : callbacks) {
            try {
                callback.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in redstone change callback: {}", e.getMessage());
            }
        }
    }

    // ===== Circuit Updates (Requirement 54.5) =====
    
    /**
     * Forces a block update at a position.
     * Requirement 54.5: WHEN scripts control redstone THEN the Runtime SHALL update connected circuits correctly
     */
    public void updateCircuit(BlockPos pos, String dimension) {
        if (server == null) return;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return;
        
        BlockState state = level.getBlockState(pos);
        level.updateNeighborsAt(pos, state.getBlock());
        
        // Also update neighbors of neighbors for redstone wire propagation
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            level.updateNeighborsAt(neighborPos, neighborState.getBlock());
        }
        
        LOGGER.debug("updateCircuit: Updated circuit at {}", pos);
    }
    
    /**
     * Sends a redstone pulse (activate then deactivate after delay).
     */
    public void sendPulse(BlockPos pos, String dimension, int durationTicks) {
        activateRedstone(pos, dimension);
        
        // Schedule deactivation
        if (server != null) {
            ServerLevel level = getLevel(dimension);
            if (level != null) {
                BlockState state = level.getBlockState(pos);
                level.scheduleTick(pos, state.getBlock(), durationTicks);
            }
        }
        
        LOGGER.info("sendPulse: Sent {} tick pulse at {}", durationTicks, pos);
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
     * Clears all tracked positions and callbacks.
     */
    public void clear() {
        trackedPositions.clear();
        changeCallbacks.clear();
        previousPowerLevels.clear();
        LOGGER.info("clear: Cleared all redstone tracking");
    }
    
    /**
     * Gets all tracked positions in a dimension.
     */
    public Set<BlockPos> getTrackedPositions(String dimension) {
        Set<BlockPos> positions = trackedPositions.get(dimension);
        return positions != null ? Collections.unmodifiableSet(positions) : Collections.emptySet();
    }
    
    /**
     * Event data for redstone changes.
     */
    public static class RedstoneChangeEvent {
        private final BlockPos position;
        private final String dimension;
        private final int oldPowerLevel;
        private final int newPowerLevel;
        
        public RedstoneChangeEvent(BlockPos position, String dimension, int oldPowerLevel, int newPowerLevel) {
            this.position = position;
            this.dimension = dimension;
            this.oldPowerLevel = oldPowerLevel;
            this.newPowerLevel = newPowerLevel;
        }
        
        public BlockPos getPosition() { return position; }
        public String getDimension() { return dimension; }
        public int getOldPowerLevel() { return oldPowerLevel; }
        public int getNewPowerLevel() { return newPowerLevel; }
        public boolean isPoweredOn() { return oldPowerLevel == 0 && newPowerLevel > 0; }
        public boolean isPoweredOff() { return oldPowerLevel > 0 && newPowerLevel == 0; }
    }
}
