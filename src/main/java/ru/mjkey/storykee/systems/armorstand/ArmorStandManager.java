package ru.mjkey.storykee.systems.armorstand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages armor stand manipulation for Storykee scripts.
 * 
 * Requirements: 56.1, 56.2, 56.3, 56.4, 56.5
 */
public class ArmorStandManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ArmorStandManager.class);
    
    private static ArmorStandManager instance;
    
    // Tracked armor stands by ID
    private final Map<String, UUID> trackedStands = new ConcurrentHashMap<>();
    
    private MinecraftServer server;
    
    private ArmorStandManager() {
    }
    
    public static ArmorStandManager getInstance() {
        if (instance == null) {
            instance = new ArmorStandManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Armor Stand Spawning (Requirement 56.1) =====
    
    /**
     * Spawns an armor stand at a location.
     * Requirement 56.1: WHEN a script spawns an armor stand THEN the Runtime SHALL create it at the specified location
     */
    public ArmorStand spawnArmorStand(String id, double x, double y, double z, String dimension) {
        return spawnArmorStand(id, x, y, z, dimension, new HashMap<>());
    }
    
    /**
     * Spawns an armor stand with properties.
     */
    public ArmorStand spawnArmorStand(String id, double x, double y, double z, String dimension, Map<String, Object> properties) {
        if (server == null) {
            LOGGER.warn("spawnArmorStand: Server not available");
            return null;
        }
        
        ServerLevel level = getLevel(dimension);
        if (level == null) {
            LOGGER.warn("spawnArmorStand: Unknown dimension - {}", dimension);
            return null;
        }
        
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
        stand.setPos(x, y, z);
        
        // Apply properties
        applyProperties(stand, properties);
        
        // Spawn the entity
        if (level.addFreshEntity(stand)) {
            trackedStands.put(id, stand.getUUID());
            LOGGER.info("spawnArmorStand: Spawned armor stand {} at ({}, {}, {})", id, x, y, z);
            return stand;
        }
        
        return null;
    }
    
    private void applyProperties(ArmorStand stand, Map<String, Object> properties) {
        // Custom name
        if (properties.containsKey("name")) {
            stand.setCustomName(net.minecraft.network.chat.Component.literal(String.valueOf(properties.get("name"))));
            stand.setCustomNameVisible(toBoolean(properties.get("nameVisible"), false));
        }
        
        // Visibility (Requirement 56.4)
        if (properties.containsKey("invisible")) {
            stand.setInvisible(toBoolean(properties.get("invisible"), false));
        }
        
        // Small and Marker variants are set via entity data in 1.21+
        // These properties are typically set at spawn time via NBT
        
        // Show arms
        if (properties.containsKey("showArms")) {
            stand.setShowArms(toBoolean(properties.get("showArms"), false));
        }
        
        // No base plate
        if (properties.containsKey("noBasePlate")) {
            stand.setNoBasePlate(toBoolean(properties.get("noBasePlate"), false));
        }
        
        // Invulnerable
        if (properties.containsKey("invulnerable")) {
            stand.setInvulnerable(toBoolean(properties.get("invulnerable"), false));
        }
        
        // No gravity
        if (properties.containsKey("noGravity")) {
            stand.setNoGravity(toBoolean(properties.get("noGravity"), false));
        }
        
        // Rotation
        if (properties.containsKey("rotation")) {
            float rotation = toFloat(properties.get("rotation"));
            stand.setYRot(rotation);
        }
    }

    // ===== Pose Setting (Requirement 56.2) =====
    
    /**
     * Sets the pose of an armor stand.
     * Requirement 56.2: WHEN a script sets armor stand pose THEN the Runtime SHALL apply the specified limb rotations
     */
    public boolean setPose(String id, ArmorStandPose pose) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) {
            LOGGER.warn("setPose: Armor stand not found - {}", id);
            return false;
        }
        
        if (pose.getHead() != null) {
            stand.setHeadPose(pose.getHead());
        }
        if (pose.getBody() != null) {
            stand.setBodyPose(pose.getBody());
        }
        if (pose.getLeftArm() != null) {
            stand.setLeftArmPose(pose.getLeftArm());
        }
        if (pose.getRightArm() != null) {
            stand.setRightArmPose(pose.getRightArm());
        }
        if (pose.getLeftLeg() != null) {
            stand.setLeftLegPose(pose.getLeftLeg());
        }
        if (pose.getRightLeg() != null) {
            stand.setRightLegPose(pose.getRightLeg());
        }
        
        LOGGER.debug("setPose: Set pose for armor stand {}", id);
        return true;
    }
    
    /**
     * Sets individual limb rotation.
     */
    public boolean setLimbRotation(String id, String limb, float x, float y, float z) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        Rotations rotation = new Rotations(x, y, z);
        
        switch (limb.toLowerCase()) {
            case "head" -> stand.setHeadPose(rotation);
            case "body" -> stand.setBodyPose(rotation);
            case "leftarm", "left_arm" -> stand.setLeftArmPose(rotation);
            case "rightarm", "right_arm" -> stand.setRightArmPose(rotation);
            case "leftleg", "left_leg" -> stand.setLeftLegPose(rotation);
            case "rightleg", "right_leg" -> stand.setRightLegPose(rotation);
            default -> {
                LOGGER.warn("setLimbRotation: Unknown limb - {}", limb);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Resets pose to default.
     */
    public boolean resetPose(String id) {
        return setPose(id, ArmorStandPose.DEFAULT);
    }

    // ===== Equipment (Requirement 56.3) =====
    
    /**
     * Sets equipment on an armor stand.
     * Requirement 56.3: WHEN a script equips items on an armor stand THEN the Runtime SHALL place them in the appropriate slots
     */
    public boolean setEquipment(String id, EquipmentSlot slot, ItemStack item) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) {
            LOGGER.warn("setEquipment: Armor stand not found - {}", id);
            return false;
        }
        
        stand.setItemSlot(slot, item);
        LOGGER.debug("setEquipment: Set {} slot for armor stand {}", slot, id);
        return true;
    }
    
    /**
     * Sets all equipment at once.
     */
    public boolean setAllEquipment(String id, ItemStack helmet, ItemStack chestplate, 
                                    ItemStack leggings, ItemStack boots, 
                                    ItemStack mainHand, ItemStack offHand) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        if (helmet != null) stand.setItemSlot(EquipmentSlot.HEAD, helmet);
        if (chestplate != null) stand.setItemSlot(EquipmentSlot.CHEST, chestplate);
        if (leggings != null) stand.setItemSlot(EquipmentSlot.LEGS, leggings);
        if (boots != null) stand.setItemSlot(EquipmentSlot.FEET, boots);
        if (mainHand != null) stand.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
        if (offHand != null) stand.setItemSlot(EquipmentSlot.OFFHAND, offHand);
        
        LOGGER.debug("setAllEquipment: Set all equipment for armor stand {}", id);
        return true;
    }
    
    /**
     * Gets equipment from an armor stand.
     */
    public ItemStack getEquipment(String id, EquipmentSlot slot) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return ItemStack.EMPTY;
        
        return stand.getItemBySlot(slot);
    }
    
    /**
     * Clears all equipment.
     */
    public boolean clearEquipment(String id) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            stand.setItemSlot(slot, ItemStack.EMPTY);
        }
        
        return true;
    }

    // ===== Visibility Control (Requirement 56.4) =====
    
    /**
     * Sets armor stand visibility.
     * Requirement 56.4: WHEN a script makes an armor stand invisible THEN the Runtime SHALL hide the base while showing equipment
     */
    public boolean setInvisible(String id, boolean invisible) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        stand.setInvisible(invisible);
        LOGGER.debug("setInvisible: Set armor stand {} invisible={}", id, invisible);
        return true;
    }
    
    /**
     * Sets whether the armor stand shows arms.
     */
    public boolean setShowArms(String id, boolean showArms) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        stand.setShowArms(showArms);
        return true;
    }
    
    /**
     * Sets whether the armor stand has a base plate.
     */
    public boolean setNoBasePlate(String id, boolean noBasePlate) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        stand.setNoBasePlate(noBasePlate);
        return true;
    }

    // ===== Marker and Small Variants (Requirement 56.5) =====
    // Note: In Minecraft 1.21+, small and marker properties are typically set at spawn time
    // and may require NBT manipulation or entity data access
    
    /**
     * Checks if armor stand is small variant.
     */
    public boolean isSmall(String id) {
        ArmorStand stand = getArmorStand(id);
        return stand != null && stand.isSmall();
    }
    
    /**
     * Checks if armor stand is marker variant.
     */
    public boolean isMarker(String id) {
        ArmorStand stand = getArmorStand(id);
        return stand != null && stand.isMarker();
    }

    // ===== Movement and Rotation =====
    
    /**
     * Moves an armor stand to a new position.
     */
    public boolean moveTo(String id, double x, double y, double z) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        stand.setPos(x, y, z);
        return true;
    }
    
    /**
     * Rotates an armor stand.
     */
    public boolean setRotation(String id, float yaw) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        stand.setYRot(yaw);
        return true;
    }

    // ===== Removal =====
    
    /**
     * Removes an armor stand.
     */
    public boolean removeArmorStand(String id) {
        ArmorStand stand = getArmorStand(id);
        if (stand == null) return false;
        
        stand.discard();
        trackedStands.remove(id);
        
        LOGGER.info("removeArmorStand: Removed armor stand {}", id);
        return true;
    }
    
    /**
     * Removes all tracked armor stands.
     */
    public int removeAllArmorStands() {
        int count = 0;
        for (String id : new ArrayList<>(trackedStands.keySet())) {
            if (removeArmorStand(id)) {
                count++;
            }
        }
        return count;
    }

    // ===== Queries =====
    
    /**
     * Gets an armor stand by ID.
     */
    public ArmorStand getArmorStand(String id) {
        UUID uuid = trackedStands.get(id);
        if (uuid == null || server == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof ArmorStand stand) {
                return stand;
            }
        }
        
        // Entity no longer exists
        trackedStands.remove(id);
        return null;
    }
    
    /**
     * Gets all armor stands in a region.
     */
    public List<ArmorStand> getArmorStandsInRegion(BlockPos min, BlockPos max, String dimension) {
        if (server == null) return Collections.emptyList();
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return Collections.emptyList();
        
        AABB box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        List<ArmorStand> result = new ArrayList<>();
        
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ArmorStand stand && box.contains(entity.position())) {
                result.add(stand);
            }
        }
        
        return result;
    }
    
    /**
     * Checks if an armor stand exists.
     */
    public boolean exists(String id) {
        return getArmorStand(id) != null;
    }
    
    /**
     * Gets all tracked armor stand IDs.
     */
    public Set<String> getTrackedIds() {
        return Collections.unmodifiableSet(trackedStands.keySet());
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
    
    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return Boolean.parseBoolean(String.valueOf(value));
        return defaultValue;
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
     * Clears all tracking data.
     */
    public void clear() {
        removeAllArmorStands();
        trackedStands.clear();
        LOGGER.info("clear: Cleared all armor stand tracking");
    }
}
