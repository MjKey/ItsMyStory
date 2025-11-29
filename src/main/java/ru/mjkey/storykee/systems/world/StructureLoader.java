package ru.mjkey.storykee.systems.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

/**
 * Loads and places structures from schematic files.
 * Requirement 21.4: WHEN a script spawns structures THEN the Runtime SHALL load them from schematic files in assets
 * 
 * Note: This is a simplified implementation that supports basic structure placement.
 * For full NBT structure support, additional parsing would be needed.
 */
public class StructureLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StructureLoader.class);
    
    private static StructureLoader instance;
    
    // Cache for loaded structures
    private final Map<String, LoadedStructure> structureCache;
    
    // Maximum structure size
    private static final int MAX_STRUCTURE_SIZE = 128;
    
    private StructureLoader() {
        this.structureCache = new HashMap<>();
    }
    
    public static StructureLoader getInstance() {
        if (instance == null) {
            instance = new StructureLoader();
        }
        return instance;
    }
    
    /**
     * Loads a structure from a file.
     */
    public LoadedStructure loadStructure(String structureId) {
        // Check cache first
        if (structureCache.containsKey(structureId)) {
            return structureCache.get(structureId);
        }
        
        // Try to find the structure file
        Path structurePath = findStructureFile(structureId);
        
        if (structurePath == null) {
            LOGGER.warn("loadStructure: Structure file not found: {}", structureId);
            return null;
        }
        
        try {
            LoadedStructure structure = loadStructureFromFile(structurePath);
            
            if (structure != null) {
                structureCache.put(structureId, structure);
                LOGGER.info("loadStructure: Loaded structure {} ({}x{}x{}, {} blocks)", 
                    structureId, structure.sizeX, structure.sizeY, structure.sizeZ, structure.blocks.size());
            }
            
            return structure;
            
        } catch (Exception e) {
            LOGGER.error("loadStructure: Error loading structure {}: {}", structureId, e.getMessage(), e);
            return null;
        }
    }
    
    private Path findStructureFile(String structureId) {
        Path assetsDir = StorykeeRuntime.getInstance().getAssetsDirectory();
        Path schematicsDir = assetsDir.resolve("schematics");
        
        String[] extensions = {".nbt", ".schem", ".schematic", ".txt"};
        
        for (String ext : extensions) {
            Path path = schematicsDir.resolve(structureId + ext);
            if (Files.exists(path)) {
                return path;
            }
        }
        
        Path directPath = schematicsDir.resolve(structureId);
        if (Files.exists(directPath)) {
            return directPath;
        }
        
        return null;
    }
    
    private LoadedStructure loadStructureFromFile(Path path) throws IOException {
        String filename = path.getFileName().toString().toLowerCase();
        
        // For now, support a simple text-based format for structures
        // Format: each line is "x,y,z,block_id"
        if (filename.endsWith(".txt")) {
            return loadTextStructure(path);
        }
        
        // For NBT files, log a warning and return null
        // Full NBT support would require more complex parsing
        LOGGER.warn("loadStructureFromFile: NBT structure files not fully supported yet. Use .txt format.");
        LOGGER.info("Text format: each line should be 'x,y,z,block_id' (e.g., '0,0,0,minecraft:stone')");
        return null;
    }

    
    /**
     * Loads a structure from a simple text format.
     * Format: each line is "x,y,z,block_id"
     */
    private LoadedStructure loadTextStructure(Path path) throws IOException {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        int maxX = 0, maxY = 0, maxZ = 0;
        
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int lineNum = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String[] parts = line.split(",");
                if (parts.length < 4) {
                    LOGGER.warn("loadTextStructure: Invalid line {} in {}: {}", lineNum, path, line);
                    continue;
                }
                
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    int z = Integer.parseInt(parts[2].trim());
                    String blockId = parts[3].trim();
                    
                    // Validate position
                    if (x < 0 || y < 0 || z < 0 || x > MAX_STRUCTURE_SIZE || y > MAX_STRUCTURE_SIZE || z > MAX_STRUCTURE_SIZE) {
                        LOGGER.warn("loadTextStructure: Position out of bounds at line {}: {},{},{}", lineNum, x, y, z);
                        continue;
                    }
                    
                    // Parse block state
                    BlockState state = parseBlockState(blockId);
                    if (state != null && !state.isAir()) {
                        blocks.put(new BlockPos(x, y, z), state);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                    
                } catch (NumberFormatException e) {
                    LOGGER.warn("loadTextStructure: Invalid coordinates at line {}: {}", lineNum, line);
                }
            }
        }
        
        if (blocks.isEmpty()) {
            LOGGER.warn("loadTextStructure: No valid blocks found in {}", path);
            return null;
        }
        
        return new LoadedStructure(maxX + 1, maxY + 1, maxZ + 1, blocks);
    }
    
    /**
     * Parses a block ID string into a BlockState.
     */
    private BlockState parseBlockState(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }
        
        // Add minecraft namespace if not present
        String fullId = blockId.contains(":") ? blockId : "minecraft:" + blockId;
        
        try {
            ResourceLocation location = ResourceLocation.parse(fullId);
            Optional<Block> blockOpt = BuiltInRegistries.BLOCK.getOptional(location);
            
            if (blockOpt.isPresent()) {
                return blockOpt.get().defaultBlockState();
            }
            
            // Try without namespace
            location = ResourceLocation.withDefaultNamespace(blockId);
            blockOpt = BuiltInRegistries.BLOCK.getOptional(location);
            
            if (blockOpt.isPresent()) {
                return blockOpt.get().defaultBlockState();
            }
            
            return null;
            
        } catch (Exception e) {
            LOGGER.warn("parseBlockState: Invalid block ID: {}", blockId);
            return null;
        }
    }

    
    // ===== Structure Placement =====
    
    public boolean placeStructure(Level world, BlockPos pos, String structureId) {
        return placeStructure(world, pos, structureId, StructurePlacementOptions.DEFAULT);
    }
    
    public boolean placeStructure(Level world, BlockPos pos, String structureId, 
                                  StructurePlacementOptions options) {
        LoadedStructure structure = loadStructure(structureId);
        
        if (structure == null) {
            LOGGER.warn("placeStructure: Structure not found: {}", structureId);
            return false;
        }
        
        return placeStructure(world, pos, structure, options);
    }
    
    public boolean placeStructure(Level world, BlockPos pos, LoadedStructure structure,
                                  StructurePlacementOptions options) {
        if (world == null || pos == null || structure == null) {
            return false;
        }
        
        WorldModifier worldModifier = WorldModifier.getInstance();
        WorldPermissionChecker permissionChecker = worldModifier.getPermissionChecker();
        
        int placedCount = 0;
        int skippedCount = 0;
        
        for (Map.Entry<BlockPos, BlockState> entry : structure.blocks.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockState state = entry.getValue();
            
            // Apply rotation if specified
            if (options.rotation != 0) {
                relativePos = rotatePosition(relativePos, structure.sizeX, structure.sizeZ, options.rotation);
                state = rotateBlockState(state, options.rotation);
            }
            
            // Calculate world position
            BlockPos worldPos = pos.offset(relativePos);
            
            // Check permissions
            if (!permissionChecker.canModifyBlock(world, worldPos)) {
                skippedCount++;
                continue;
            }
            
            // Check if we should replace existing blocks
            if (!options.replaceBlocks) {
                BlockState existing = world.getBlockState(worldPos);
                if (!existing.isAir()) {
                    skippedCount++;
                    continue;
                }
            }
            
            // Place the block
            if (worldModifier.setBlock(world, worldPos, state)) {
                placedCount++;
            }
        }
        
        LOGGER.info("placeStructure: Placed {} blocks, skipped {} at {}", placedCount, skippedCount, pos);
        return placedCount > 0;
    }
    
    public CompletableFuture<Boolean> placeStructureAsync(Level world, BlockPos pos, String structureId,
                                                          StructurePlacementOptions options,
                                                          Consumer<Integer> progressCallback) {
        LoadedStructure structure = loadStructure(structureId);
        
        if (structure == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        return placeStructureAsync(world, pos, structure, options, progressCallback);
    }
    
    public CompletableFuture<Boolean> placeStructureAsync(Level world, BlockPos pos, 
                                                          LoadedStructure structure,
                                                          StructurePlacementOptions options,
                                                          Consumer<Integer> progressCallback) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        WorldModifier worldModifier = WorldModifier.getInstance();
        
        List<Map.Entry<BlockPos, BlockState>> blockList = new ArrayList<>(structure.blocks.entrySet());
        int totalBlocks = blockList.size();
        int batchSize = 500;
        int[] processedCount = {0};
        int[] placedCount = {0};
        
        processStructureBatch(world, pos, blockList, options, batchSize, 
                             processedCount, placedCount, totalBlocks, 
                             progressCallback, future, worldModifier);
        
        return future;
    }
    
    private void processStructureBatch(Level world, BlockPos basePos, 
                                       List<Map.Entry<BlockPos, BlockState>> blockList,
                                       StructurePlacementOptions options,
                                       int batchSize, int[] processedCount, int[] placedCount,
                                       int totalBlocks, Consumer<Integer> progressCallback,
                                       CompletableFuture<Boolean> future,
                                       WorldModifier worldModifier) {
        
        ru.mjkey.storykee.runtime.async.MinecraftThreadBridge.getInstance().executeOnMainThread(() -> {
            WorldPermissionChecker permissionChecker = worldModifier.getPermissionChecker();
            
            int processed = 0;
            
            while (processedCount[0] < totalBlocks && processed < batchSize) {
                Map.Entry<BlockPos, BlockState> entry = blockList.get(processedCount[0]);
                BlockPos relativePos = entry.getKey();
                BlockState state = entry.getValue();
                
                BlockPos worldPos = basePos.offset(relativePos);
                
                if (permissionChecker.canModifyBlock(world, worldPos)) {
                    if (options.replaceBlocks || world.getBlockState(worldPos).isAir()) {
                        if (worldModifier.setBlock(world, worldPos, state)) {
                            placedCount[0]++;
                        }
                    }
                }
                
                processedCount[0]++;
                processed++;
            }
            
            if (progressCallback != null) {
                int progress = (int) ((processedCount[0] * 100.0) / totalBlocks);
                progressCallback.accept(progress);
            }
            
            if (processedCount[0] >= totalBlocks) {
                future.complete(placedCount[0] > 0);
            } else {
                processStructureBatch(world, basePos, blockList, options, batchSize,
                                     processedCount, placedCount, totalBlocks,
                                     progressCallback, future, worldModifier);
            }
        });
    }

    
    // ===== Rotation Helpers =====
    
    private BlockPos rotatePosition(BlockPos pos, int sizeX, int sizeZ, int rotation) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        return switch (rotation % 360) {
            case 90 -> new BlockPos(sizeZ - 1 - z, y, x);
            case 180 -> new BlockPos(sizeX - 1 - x, y, sizeZ - 1 - z);
            case 270 -> new BlockPos(z, y, sizeX - 1 - x);
            default -> pos;
        };
    }
    
    private BlockState rotateBlockState(BlockState state, int rotation) {
        if (rotation == 0) {
            return state;
        }
        
        int rotationCount = (rotation / 90) % 4;
        
        for (int i = 0; i < rotationCount; i++) {
            state = state.rotate(Rotation.CLOCKWISE_90);
        }
        
        return state;
    }
    
    // ===== Cache Management =====
    
    public void clearCache() {
        structureCache.clear();
        LOGGER.info("Structure cache cleared");
    }
    
    public void removeFromCache(String structureId) {
        structureCache.remove(structureId);
    }
    
    public int getCacheSize() {
        return structureCache.size();
    }
    
    // ===== Inner Classes =====
    
    public static class LoadedStructure {
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;
        public final Map<BlockPos, BlockState> blocks;
        
        public LoadedStructure(int sizeX, int sizeY, int sizeZ, Map<BlockPos, BlockState> blocks) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.blocks = Collections.unmodifiableMap(blocks);
        }
        
        public int getBlockCount() {
            return blocks.size();
        }
        
        public int getVolume() {
            return sizeX * sizeY * sizeZ;
        }
    }
    
    public static class StructurePlacementOptions {
        public static final StructurePlacementOptions DEFAULT = new StructurePlacementOptions();
        
        public boolean replaceBlocks = true;
        public int rotation = 0;
        public boolean includeAir = false;
        public boolean updateNeighbors = true;
        
        public StructurePlacementOptions() {}
        
        public StructurePlacementOptions replaceBlocks(boolean replace) {
            this.replaceBlocks = replace;
            return this;
        }
        
        public StructurePlacementOptions rotation(int degrees) {
            this.rotation = degrees % 360;
            return this;
        }
        
        public StructurePlacementOptions includeAir(boolean include) {
            this.includeAir = include;
            return this;
        }
        
        public StructurePlacementOptions updateNeighbors(boolean update) {
            this.updateNeighbors = update;
            return this;
        }
    }
}
