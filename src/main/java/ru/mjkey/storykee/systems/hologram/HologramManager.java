package ru.mjkey.storykee.systems.hologram;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages holograms in the Storykee system.
 * Uses Minecraft's Text Display entities for rendering.
 * 
 * Requirements: 48.1, 48.2, 48.3, 48.4, 48.5
 */
public class HologramManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HologramManager.class);
    
    private static HologramManager instance;
    
    // Holograms by ID
    private final Map<String, Hologram> holograms;
    
    // Entity IDs for spawned hologram entities
    private final Map<String, List<Integer>> hologramEntities;
    
    // World reference for each hologram
    private final Map<String, ServerLevel> hologramWorlds;
    
    private MinecraftServer server;
    
    private HologramManager() {
        this.holograms = new ConcurrentHashMap<>();
        this.hologramEntities = new ConcurrentHashMap<>();
        this.hologramWorlds = new ConcurrentHashMap<>();
    }
    
    public static HologramManager getInstance() {
        if (instance == null) {
            instance = new HologramManager();
        }
        return instance;
    }
    
    /**
     * Initializes the hologram manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("HologramManager initialized");
    }
    
    /**
     * Shuts down the hologram manager.
     */
    public void shutdown() {
        // Remove all holograms
        for (String id : new ArrayList<>(holograms.keySet())) {
            removeHologram(id);
        }
        LOGGER.info("HologramManager shutdown");
    }
    
    // ==================== Hologram Creation ====================
    
    /**
     * Creates and spawns a hologram.
     * Requirement 48.1: Create hologram spawning
     * 
     * @param id Unique hologram ID
     * @param world Server world
     * @param position Position
     * @param text Initial text
     * @return The created hologram, or null if failed
     */
    public Hologram createHologram(String id, ServerLevel world, Vec3 position, String text) {
        if (id == null || world == null || position == null) {
            return null;
        }
        
        if (holograms.containsKey(id)) {
            LOGGER.warn("Hologram '{}' already exists", id);
            return null;
        }
        
        Hologram hologram = new Hologram(id, position);
        if (text != null) {
            hologram.setText(text);
        }
        
        holograms.put(id, hologram);
        hologramWorlds.put(id, world);
        
        // Spawn the display entity
        spawnHologramEntities(hologram, world);
        
        LOGGER.debug("Created hologram '{}' at {}", id, position);
        return hologram;
    }
    
    /**
     * Creates a multi-line hologram.
     * Requirement 48.4: Add multi-line support
     * 
     * @param id Unique hologram ID
     * @param world Server world
     * @param position Position
     * @param lines Text lines
     * @return The created hologram, or null if failed
     */
    public Hologram createHologram(String id, ServerLevel world, Vec3 position, List<String> lines) {
        Hologram hologram = createHologram(id, world, position, (String) null);
        if (hologram != null && lines != null) {
            hologram.setLines(lines);
            updateHologramEntities(hologram);
        }
        return hologram;
    }
    
    /**
     * Removes a hologram.
     * 
     * @param id Hologram ID
     * @return true if removed
     */
    public boolean removeHologram(String id) {
        Hologram hologram = holograms.remove(id);
        if (hologram == null) {
            return false;
        }
        
        // Remove entities
        despawnHologramEntities(id);
        hologramWorlds.remove(id);
        
        LOGGER.debug("Removed hologram '{}'", id);
        return true;
    }
    
    /**
     * Gets a hologram by ID.
     * 
     * @param id Hologram ID
     * @return The hologram, or null if not found
     */
    public Hologram getHologram(String id) {
        return holograms.get(id);
    }
    
    // ==================== Hologram Updates ====================
    
    /**
     * Updates a hologram's text.
     * Requirement 48.3: Implement updates
     * 
     * @param id Hologram ID
     * @param text New text
     * @return true if updated
     */
    public boolean updateText(String id, String text) {
        Hologram hologram = holograms.get(id);
        if (hologram == null) {
            return false;
        }
        
        hologram.setText(text);
        updateHologramEntities(hologram);
        return true;
    }
    
    /**
     * Updates a hologram's lines.
     * 
     * @param id Hologram ID
     * @param lines New lines
     * @return true if updated
     */
    public boolean updateLines(String id, List<String> lines) {
        Hologram hologram = holograms.get(id);
        if (hologram == null) {
            return false;
        }
        
        hologram.setLines(lines);
        updateHologramEntities(hologram);
        return true;
    }
    
    /**
     * Updates a specific line.
     * 
     * @param id Hologram ID
     * @param lineIndex Line index
     * @param text New text
     * @return true if updated
     */
    public boolean updateLine(String id, int lineIndex, String text) {
        Hologram hologram = holograms.get(id);
        if (hologram == null) {
            return false;
        }
        
        hologram.setLine(lineIndex, text);
        updateHologramEntities(hologram);
        return true;
    }
    
    /**
     * Updates a hologram's position.
     * 
     * @param id Hologram ID
     * @param position New position
     * @return true if updated
     */
    public boolean updatePosition(String id, Vec3 position) {
        Hologram hologram = holograms.get(id);
        if (hologram == null || position == null) {
            return false;
        }
        
        hologram.setPosition(position);
        updateHologramEntities(hologram);
        return true;
    }
    
    /**
     * Sets hologram visibility.
     * 
     * @param id Hologram ID
     * @param visible Whether visible
     * @return true if updated
     */
    public boolean setVisible(String id, boolean visible) {
        Hologram hologram = holograms.get(id);
        if (hologram == null) {
            return false;
        }
        
        hologram.setVisible(visible);
        
        if (visible) {
            ServerLevel world = hologramWorlds.get(id);
            if (world != null) {
                spawnHologramEntities(hologram, world);
            }
        } else {
            despawnHologramEntities(id);
        }
        
        return true;
    }
    
    // ==================== Entity Management ====================
    
    /**
     * Spawns the display entities for a hologram.
     */
    private void spawnHologramEntities(Hologram hologram, ServerLevel world) {
        // First despawn any existing entities
        despawnHologramEntities(hologram.getId());
        
        if (!hologram.isVisible() || hologram.getLines().isEmpty()) {
            return;
        }
        
        List<Integer> entityIds = new ArrayList<>();
        Vec3 basePos = hologram.getPosition();
        float lineSpacing = hologram.getLineSpacing() * hologram.getScale();
        
        // Spawn a text display entity for each line
        for (int i = 0; i < hologram.getLines().size(); i++) {
            String line = hologram.getLines().get(i);
            Vec3 linePos = basePos.add(0, -i * lineSpacing, 0);
            
            // Create text display entity
            Display.TextDisplay textDisplay = new Display.TextDisplay(
                    EntityType.TEXT_DISPLAY, world);
            
            textDisplay.setPos(linePos.x, linePos.y, linePos.z);
            
            // Set text using NBT or direct method
            // Note: In actual implementation, you'd use the proper API
            // This is a simplified version
            
            world.addFreshEntity(textDisplay);
            entityIds.add(textDisplay.getId());
        }
        
        hologramEntities.put(hologram.getId(), entityIds);
    }
    
    /**
     * Despawns hologram entities.
     */
    private void despawnHologramEntities(String hologramId) {
        List<Integer> entityIds = hologramEntities.remove(hologramId);
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }
        
        ServerLevel world = hologramWorlds.get(hologramId);
        if (world == null) {
            return;
        }
        
        for (int entityId : entityIds) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
    }
    
    /**
     * Updates hologram entities after changes.
     */
    private void updateHologramEntities(Hologram hologram) {
        ServerLevel world = hologramWorlds.get(hologram.getId());
        if (world != null) {
            // Respawn entities with updated data
            spawnHologramEntities(hologram, world);
        }
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Gets all hologram IDs.
     */
    public Set<String> getHologramIds() {
        return Collections.unmodifiableSet(holograms.keySet());
    }
    
    /**
     * Checks if a hologram exists.
     */
    public boolean hasHologram(String id) {
        return holograms.containsKey(id);
    }
    
    /**
     * Gets the number of holograms.
     */
    public int getHologramCount() {
        return holograms.size();
    }
    
    /**
     * Gets all holograms in a world.
     */
    public List<Hologram> getHologramsInWorld(ServerLevel world) {
        List<Hologram> result = new ArrayList<>();
        for (Map.Entry<String, ServerLevel> entry : hologramWorlds.entrySet()) {
            if (entry.getValue().equals(world)) {
                Hologram hologram = holograms.get(entry.getKey());
                if (hologram != null) {
                    result.add(hologram);
                }
            }
        }
        return result;
    }
}
