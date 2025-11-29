package ru.mjkey.storykee.systems.region;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages regions and player enter/exit detection.
 * 
 * Requirements: 49.1, 49.2, 49.3, 49.4, 49.5
 */
public class RegionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionManager.class);
    
    private static RegionManager instance;
    
    // Regions by ID
    private final Map<String, Region> regions;
    
    // Player's current regions (for enter/exit detection)
    private final Map<UUID, Set<String>> playerRegions;
    
    // Event listeners
    private final List<RegionEventListener> eventListeners;
    
    private MinecraftServer server;
    
    private RegionManager() {
        this.regions = new ConcurrentHashMap<>();
        this.playerRegions = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
    }
    
    public static RegionManager getInstance() {
        if (instance == null) {
            instance = new RegionManager();
        }
        return instance;
    }
    
    /**
     * Initializes the region manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("RegionManager initialized");
    }
    
    /**
     * Shuts down the region manager.
     */
    public void shutdown() {
        regions.clear();
        playerRegions.clear();
        LOGGER.info("RegionManager shutdown");
    }
    
    // ==================== Region Management ====================
    
    /**
     * Creates a region.
     * Requirement 49.1: Create region definition
     * 
     * @param id Unique region ID
     * @param corner1 First corner
     * @param corner2 Second corner
     * @return The created region, or null if failed
     */
    public Region createRegion(String id, Vec3 corner1, Vec3 corner2) {
        if (id == null || corner1 == null || corner2 == null) {
            return null;
        }
        
        if (regions.containsKey(id)) {
            LOGGER.warn("Region '{}' already exists", id);
            return null;
        }
        
        Region region = new Region(id, corner1, corner2);
        regions.put(id, region);
        
        LOGGER.debug("Created region '{}' from {} to {}", id, corner1, corner2);
        return region;
    }
    
    /**
     * Creates a region from block positions.
     */
    public Region createRegion(String id, BlockPos corner1, BlockPos corner2) {
        return createRegion(id, Vec3.atLowerCornerOf(corner1), Vec3.atLowerCornerOf(corner2).add(1, 1, 1));
    }
    
    /**
     * Removes a region.
     * 
     * @param id Region ID
     * @return true if removed
     */
    public boolean removeRegion(String id) {
        Region removed = regions.remove(id);
        if (removed != null) {
            // Remove from all player region sets
            for (Set<String> playerRegionSet : playerRegions.values()) {
                playerRegionSet.remove(id);
            }
            LOGGER.debug("Removed region '{}'", id);
            return true;
        }
        return false;
    }
    
    /**
     * Gets a region by ID.
     * 
     * @param id Region ID
     * @return The region, or null if not found
     */
    public Region getRegion(String id) {
        return regions.get(id);
    }
    
    /**
     * Gets all region IDs.
     */
    public Set<String> getRegionIds() {
        return Collections.unmodifiableSet(regions.keySet());
    }
    
    /**
     * Gets all regions.
     */
    public Collection<Region> getAllRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }
    
    // ==================== Containment Checking ====================
    
    /**
     * Checks if a position is in a region.
     * Requirement 49.3: Implement containment checking
     * 
     * @param regionId Region ID
     * @param position Position to check
     * @return true if inside
     */
    public boolean isInRegion(String regionId, Vec3 position) {
        Region region = regions.get(regionId);
        return region != null && region.contains(position);
    }
    
    /**
     * Gets all regions containing a position.
     * Requirement 49.4: Handle overlapping regions
     * 
     * @param position Position to check
     * @return List of regions containing the position, sorted by priority
     */
    public List<Region> getRegionsAt(Vec3 position) {
        List<Region> containing = new ArrayList<>();
        
        for (Region region : regions.values()) {
            if (region.contains(position)) {
                containing.add(region);
            }
        }
        
        // Sort by priority (higher priority first)
        containing.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        return containing;
    }
    
    /**
     * Gets the highest priority region at a position.
     * 
     * @param position Position to check
     * @return The region, or null if none
     */
    public Region getPrimaryRegionAt(Vec3 position) {
        List<Region> regions = getRegionsAt(position);
        return regions.isEmpty() ? null : regions.get(0);
    }
    
    /**
     * Gets all regions a player is currently in.
     * 
     * @param playerId Player UUID
     * @return Set of region IDs
     */
    public Set<String> getPlayerRegions(UUID playerId) {
        Set<String> regions = playerRegions.get(playerId);
        return regions != null ? Collections.unmodifiableSet(regions) : Collections.emptySet();
    }
    
    /**
     * Checks if a player is in a specific region.
     * 
     * @param playerId Player UUID
     * @param regionId Region ID
     * @return true if player is in the region
     */
    public boolean isPlayerInRegion(UUID playerId, String regionId) {
        Set<String> regions = playerRegions.get(playerId);
        return regions != null && regions.contains(regionId);
    }
    
    // ==================== Enter/Exit Detection ====================
    
    /**
     * Updates player region tracking.
     * Should be called every tick.
     * Requirement 49.2: Add enter/exit detection
     */
    public void tick() {
        if (server == null) {
            return;
        }
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updatePlayerRegions(player);
        }
    }
    
    /**
     * Updates region tracking for a specific player.
     */
    private void updatePlayerRegions(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Vec3 position = player.position();
        
        Set<String> currentRegions = playerRegions.computeIfAbsent(playerId, k -> new HashSet<>());
        Set<String> newRegions = new HashSet<>();
        
        // Find all regions the player is currently in
        for (Region region : regions.values()) {
            if (region.contains(position)) {
                newRegions.add(region.getId());
            }
        }
        
        // Detect exits (was in region, now not)
        for (String regionId : currentRegions) {
            if (!newRegions.contains(regionId)) {
                Region region = regions.get(regionId);
                if (region != null) {
                    notifyRegionExit(player, region);
                }
            }
        }
        
        // Detect entries (now in region, wasn't before)
        for (String regionId : newRegions) {
            if (!currentRegions.contains(regionId)) {
                Region region = regions.get(regionId);
                if (region != null) {
                    notifyRegionEnter(player, region);
                }
            }
        }
        
        // Update stored regions
        currentRegions.clear();
        currentRegions.addAll(newRegions);
    }
    
    /**
     * Clears player region tracking (e.g., on disconnect).
     * 
     * @param playerId Player UUID
     */
    public void clearPlayerRegions(UUID playerId) {
        playerRegions.remove(playerId);
    }
    
    // ==================== Overlapping Regions ====================
    
    /**
     * Gets all regions that overlap with a given region.
     * Requirement 49.4: Handle overlapping regions
     * 
     * @param regionId Region ID
     * @return List of overlapping regions
     */
    public List<Region> getOverlappingRegions(String regionId) {
        Region region = regions.get(regionId);
        if (region == null) {
            return Collections.emptyList();
        }
        
        List<Region> overlapping = new ArrayList<>();
        for (Region other : regions.values()) {
            if (!other.getId().equals(regionId) && region.overlaps(other)) {
                overlapping.add(other);
            }
        }
        
        return overlapping;
    }
    
    // ==================== Event Listeners ====================
    
    public void addEventListener(RegionEventListener listener) {
        eventListeners.add(listener);
    }
    
    public void removeEventListener(RegionEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void notifyRegionEnter(ServerPlayer player, Region region) {
        LOGGER.debug("Player {} entered region '{}'", player.getName().getString(), region.getId());
        
        for (RegionEventListener listener : eventListeners) {
            try {
                listener.onRegionEnter(player, region);
            } catch (Exception e) {
                LOGGER.error("Error in region enter listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyRegionExit(ServerPlayer player, Region region) {
        LOGGER.debug("Player {} exited region '{}'", player.getName().getString(), region.getId());
        
        for (RegionEventListener listener : eventListeners) {
            try {
                listener.onRegionExit(player, region);
            } catch (Exception e) {
                LOGGER.error("Error in region exit listener: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Listener interface for region events.
     */
    public interface RegionEventListener {
        /**
         * Called when a player enters a region.
         * 
         * @param player Player who entered
         * @param region Region entered
         */
        void onRegionEnter(ServerPlayer player, Region region);
        
        /**
         * Called when a player exits a region.
         * 
         * @param player Player who exited
         * @param region Region exited
         */
        void onRegionExit(ServerPlayer player, Region region);
    }
    
    /**
     * Adapter class with empty default implementations.
     */
    public static class RegionEventAdapter implements RegionEventListener {
        @Override
        public void onRegionEnter(ServerPlayer player, Region region) {}
        
        @Override
        public void onRegionExit(ServerPlayer player, Region region) {}
    }
}
