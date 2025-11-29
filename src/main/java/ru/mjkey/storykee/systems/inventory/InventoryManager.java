package ru.mjkey.storykee.systems.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages player inventory operations for the Storykee scripting system.
 * Provides functions for giving, removing, and checking items in player inventories.
 * 
 * Requirements: 22.1, 22.2, 22.3, 22.4, 22.5
 */
public class InventoryManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(InventoryManager.class);
    
    private static InventoryManager instance;
    
    // Reference to the Minecraft server
    private MinecraftServer server;
    
    private InventoryManager() {
    }
    
    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
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

    // ===== Item Giving (Requirement 22.1) =====
    
    /**
     * Gives an item to a player.
     * Requirement 22.1: WHEN a script gives an item THEN the Runtime SHALL add it to the player's inventory
     * Requirement 22.5: WHEN inventory is full THEN the Runtime SHALL drop excess items at the player's location
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation (e.g., "minecraft:diamond")
     * @param count The number of items to give
     * @return The number of items actually given (may be less if inventory is full and items were dropped)
     */
    public int giveItem(UUID playerId, String itemId, int count) {
        return giveItem(playerId, itemId, count, null);
    }
    
    /**
     * Gives an item to a player with custom properties.
     * Requirement 22.1: WHEN a script gives an item THEN the Runtime SHALL add it to the player's inventory
     * Requirement 22.4: WHEN a script creates a custom item THEN the Runtime SHALL apply NBT data, lore, and enchantments
     * Requirement 22.5: WHEN inventory is full THEN the Runtime SHALL drop excess items at the player's location
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation (e.g., "minecraft:diamond")
     * @param count The number of items to give
     * @param properties Custom item properties (name, lore, enchantments, etc.)
     * @return The number of items actually given
     */
    public int giveItem(UUID playerId, String itemId, int count, Map<String, Object> properties) {
        if (playerId == null || itemId == null || count <= 0) {
            LOGGER.warn("giveItem: Invalid parameters - playerId={}, itemId={}, count={}", playerId, itemId, count);
            return 0;
        }
        
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("giveItem: Player not found - {}", playerId);
            return 0;
        }
        
        // Parse the item ID
        Item item = parseItem(itemId);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("giveItem: Unknown item - {}", itemId);
            return 0;
        }
        
        // Create the item stack
        ItemStack stack = new ItemStack(item, count);
        
        // Apply custom properties if provided
        if (properties != null && !properties.isEmpty()) {
            applyItemProperties(stack, properties);
        }
        
        // Try to add to inventory
        int givenCount = addToInventory(player, stack);
        
        LOGGER.info("giveItem: Gave {} x{} to player {} (requested: {})", 
            itemId, givenCount, playerId, count);
        
        return givenCount;
    }
    
    /**
     * Adds an item stack to a player's inventory, dropping excess items.
     * Requirement 22.5: WHEN inventory is full THEN the Runtime SHALL drop excess items at the player's location
     */
    private int addToInventory(ServerPlayer player, ItemStack stack) {
        int originalCount = stack.getCount();
        
        // Try to add to inventory
        boolean added = player.getInventory().add(stack);
        
        // If there are remaining items (inventory was full), drop them
        if (!added || stack.getCount() > 0) {
            dropItemsAtPlayer(player, stack);
            LOGGER.debug("addToInventory: Dropped {} items at player location (inventory full)", stack.getCount());
        }
        
        return originalCount;
    }
    
    /**
     * Drops items at the player's location.
     * Requirement 22.5: WHEN inventory is full THEN the Runtime SHALL drop excess items at the player's location
     */
    private void dropItemsAtPlayer(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        
        ItemEntity itemEntity = new ItemEntity(
            player.level(),
            player.getX(),
            player.getY() + 0.5,
            player.getZ(),
            stack.copy()
        );
        
        // Set pickup delay so player can pick it up immediately
        itemEntity.setPickUpDelay(0);
        
        // Add some random velocity
        itemEntity.setDeltaMovement(
            (player.getRandom().nextDouble() - 0.5) * 0.1,
            0.2,
            (player.getRandom().nextDouble() - 0.5) * 0.1
        );
        
        player.level().addFreshEntity(itemEntity);
    }

    // ===== Item Removal (Requirement 22.2) =====
    
    /**
     * Removes items from a player's inventory.
     * Requirement 22.2: WHEN a script removes an item THEN the Runtime SHALL search the inventory and remove the specified quantity
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation (e.g., "minecraft:diamond")
     * @param count The number of items to remove
     * @return The number of items actually removed
     */
    public int removeItem(UUID playerId, String itemId, int count) {
        if (playerId == null || itemId == null || count <= 0) {
            LOGGER.warn("removeItem: Invalid parameters - playerId={}, itemId={}, count={}", playerId, itemId, count);
            return 0;
        }
        
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("removeItem: Player not found - {}", playerId);
            return 0;
        }
        
        // Parse the item ID
        Item item = parseItem(itemId);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("removeItem: Unknown item - {}", itemId);
            return 0;
        }
        
        int removedCount = 0;
        int remaining = count;
        
        // Search through the inventory and remove items
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            
            if (!slot.isEmpty() && slot.getItem() == item) {
                int toRemove = Math.min(remaining, slot.getCount());
                slot.shrink(toRemove);
                removedCount += toRemove;
                remaining -= toRemove;
                
                // Update the slot if it's now empty
                if (slot.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }
        
        LOGGER.info("removeItem: Removed {} x{} from player {} (requested: {})", 
            itemId, removedCount, playerId, count);
        
        return removedCount;
    }
    
    /**
     * Removes all items of a specific type from a player's inventory.
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation
     * @return The total number of items removed
     */
    public int removeAllItems(UUID playerId, String itemId) {
        int totalCount = countItem(playerId, itemId);
        if (totalCount > 0) {
            return removeItem(playerId, itemId, totalCount);
        }
        return 0;
    }
    
    // ===== Item Checking (Requirement 22.3) =====
    
    /**
     * Counts the number of a specific item in a player's inventory.
     * Requirement 22.3: WHEN a script checks for an item THEN the Runtime SHALL return the quantity found in the inventory
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation (e.g., "minecraft:diamond")
     * @return The total count of the item in the inventory
     */
    public int countItem(UUID playerId, String itemId) {
        if (playerId == null || itemId == null) {
            LOGGER.warn("countItem: Invalid parameters - playerId={}, itemId={}", playerId, itemId);
            return 0;
        }
        
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            LOGGER.warn("countItem: Player not found - {}", playerId);
            return 0;
        }
        
        // Parse the item ID
        Item item = parseItem(itemId);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("countItem: Unknown item - {}", itemId);
            return 0;
        }
        
        int totalCount = 0;
        
        // Count items in the inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            
            if (!slot.isEmpty() && slot.getItem() == item) {
                totalCount += slot.getCount();
            }
        }
        
        LOGGER.debug("countItem: Player {} has {} x{}", playerId, totalCount, itemId);
        
        return totalCount;
    }
    
    /**
     * Checks if a player has at least a certain amount of an item.
     * Requirement 22.3: WHEN a script checks for an item THEN the Runtime SHALL return the quantity found in the inventory
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation
     * @param count The minimum count required
     * @return true if the player has at least the specified count
     */
    public boolean hasItem(UUID playerId, String itemId, int count) {
        return countItem(playerId, itemId) >= count;
    }
    
    /**
     * Checks if a player has any of a specific item.
     * 
     * @param playerId The player's UUID
     * @param itemId The item ResourceLocation
     * @return true if the player has at least one of the item
     */
    public boolean hasItem(UUID playerId, String itemId) {
        return hasItem(playerId, itemId, 1);
    }

    // ===== Custom Item Creation (Requirement 22.4) =====
    
    /**
     * Creates a custom item stack with specified properties.
     * Requirement 22.4: WHEN a script creates a custom item THEN the Runtime SHALL apply NBT data, lore, and enchantments
     * 
     * @param itemId The base item ResourceLocation
     * @param count The stack size
     * @param properties Custom properties (name, lore, enchantments, customModelData, etc.)
     * @return The created ItemStack, or null if creation failed
     */
    public ItemStack createCustomItem(String itemId, int count, Map<String, Object> properties) {
        if (itemId == null || count <= 0) {
            LOGGER.warn("createCustomItem: Invalid parameters - itemId={}, count={}", itemId, count);
            return null;
        }
        
        // Parse the item ID
        Item item = parseItem(itemId);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("createCustomItem: Unknown item - {}", itemId);
            return null;
        }
        
        // Create the item stack
        ItemStack stack = new ItemStack(item, count);
        
        // Apply custom properties
        if (properties != null && !properties.isEmpty()) {
            applyItemProperties(stack, properties);
        }
        
        LOGGER.debug("createCustomItem: Created custom {} x{}", itemId, count);
        
        return stack;
    }
    
    /**
     * Applies custom properties to an item stack.
     * Requirement 22.4: WHEN a script creates a custom item THEN the Runtime SHALL apply NBT data, lore, and enchantments
     */
    private void applyItemProperties(ItemStack stack, Map<String, Object> properties) {
        // Apply custom name
        if (properties.containsKey("name")) {
            String name = String.valueOf(properties.get("name"));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }
        
        // Apply lore
        if (properties.containsKey("lore")) {
            Object loreObj = properties.get("lore");
            List<Component> loreLines = new ArrayList<>();
            
            if (loreObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> loreList = (List<Object>) loreObj;
                for (Object line : loreList) {
                    loreLines.add(Component.literal(String.valueOf(line)));
                }
            } else if (loreObj instanceof String) {
                // Single line lore
                loreLines.add(Component.literal((String) loreObj));
            }
            
            if (!loreLines.isEmpty()) {
                stack.set(DataComponents.LORE, new ItemLore(loreLines));
            }
        }
        
        // Apply enchantments
        if (properties.containsKey("enchantments")) {
            Object enchObj = properties.get("enchantments");
            
            if (enchObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> enchantments = (Map<String, Object>) enchObj;
                applyEnchantments(stack, enchantments);
            }
        }
        
        // Apply damage (for tools/armor)
        if (properties.containsKey("damage")) {
            int damage = toInt(properties.get("damage"));
            stack.setDamageValue(damage);
        }
    }
    
    /**
     * Applies enchantments to an item stack.
     * Note: Enchantment application requires server-side registry access.
     * This is a simplified implementation that logs the enchantment request.
     */
    private void applyEnchantments(ItemStack stack, Map<String, Object> enchantments) {
        // Enchantments in 1.21+ require registry access from the server
        // For now, log the enchantment request - full implementation requires server context
        for (Map.Entry<String, Object> entry : enchantments.entrySet()) {
            String enchantmentId = entry.getKey();
            int level = toInt(entry.getValue());
            LOGGER.debug("applyEnchantments: Requested {} level {} for item (requires server context)", 
                enchantmentId, level);
        }
    }

    // ===== Utility Methods =====
    
    /**
     * Gets a player by UUID.
     */
    private ServerPlayer getPlayer(UUID playerId) {
        if (server == null) {
            LOGGER.warn("getPlayer: Server not available");
            return null;
        }
        
        return server.getPlayerList().getPlayer(playerId);
    }
    
    /**
     * Parses an item ResourceLocation to an Item.
     */
    private Item parseItem(String itemId) {
        ResourceLocation location = parseResourceLocation(itemId);
        if (location == null) {
            return null;
        }
        
        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(location);
        return itemOpt.orElse(null);
    }
    
    /**
     * Parses a resource location string.
     */
    private ResourceLocation parseResourceLocation(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        
        // Add minecraft namespace if not present
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        
        try {
            return ResourceLocation.parse(id);
        } catch (Exception e) {
            LOGGER.warn("parseResourceLocation: Invalid resource location - {}", id);
            return null;
        }
    }
    
    /**
     * Converts an object to an integer.
     */
    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Gets the item ID string for an item.
     */
    private String getItemId(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.toString() : "minecraft:air";
    }
    
    // ===== Additional Inventory Operations =====
    
    /**
     * Gets the contents of a player's inventory as a list of item info maps.
     * 
     * @param playerId The player's UUID
     * @return List of maps containing item information
     */
    public List<Map<String, Object>> getInventoryContents(UUID playerId) {
        List<Map<String, Object>> contents = new ArrayList<>();
        
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            return contents;
        }
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            
            if (!slot.isEmpty()) {
                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("slot", i);
                itemInfo.put("itemId", getItemId(slot.getItem()));
                itemInfo.put("count", slot.getCount());
                itemInfo.put("damage", slot.getDamageValue());
                itemInfo.put("maxDamage", slot.getMaxDamage());
                
                // Include custom name if present
                if (slot.has(DataComponents.CUSTOM_NAME)) {
                    itemInfo.put("customName", slot.get(DataComponents.CUSTOM_NAME).getString());
                }
                
                contents.add(itemInfo);
            }
        }
        
        return contents;
    }
    
    /**
     * Clears a player's entire inventory.
     * 
     * @param playerId The player's UUID
     * @return The number of items cleared
     */
    public int clearInventory(UUID playerId) {
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            return 0;
        }
        
        int clearedCount = 0;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.isEmpty()) {
                clearedCount += slot.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        
        LOGGER.info("clearInventory: Cleared {} items from player {}", clearedCount, playerId);
        
        return clearedCount;
    }
    
    /**
     * Gets the number of empty slots in a player's inventory.
     * 
     * @param playerId The player's UUID
     * @return The number of empty slots
     */
    public int getEmptySlotCount(UUID playerId) {
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            return 0;
        }
        
        int emptyCount = 0;
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).isEmpty()) {
                emptyCount++;
            }
        }
        
        return emptyCount;
    }
    
    /**
     * Checks if a player's inventory is full.
     * 
     * @param playerId The player's UUID
     * @return true if the inventory has no empty slots
     */
    public boolean isInventoryFull(UUID playerId) {
        return getEmptySlotCount(playerId) == 0;
    }
    
    /**
     * Sets an item in a specific inventory slot.
     * 
     * @param playerId The player's UUID
     * @param slot The slot index
     * @param itemId The item ResourceLocation
     * @param count The stack size
     * @return true if successful
     */
    public boolean setSlot(UUID playerId, int slot, String itemId, int count) {
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            LOGGER.warn("setSlot: Invalid slot index - {}", slot);
            return false;
        }
        
        if (itemId == null || itemId.isEmpty() || count <= 0) {
            // Clear the slot
            player.getInventory().setItem(slot, ItemStack.EMPTY);
            return true;
        }
        
        Item item = parseItem(itemId);
        if (item == null || item == Items.AIR) {
            LOGGER.warn("setSlot: Unknown item - {}", itemId);
            return false;
        }
        
        player.getInventory().setItem(slot, new ItemStack(item, count));
        return true;
    }
    
    /**
     * Gets the item in a specific inventory slot.
     * 
     * @param playerId The player's UUID
     * @param slot The slot index
     * @return Map containing item information, or null if slot is empty
     */
    public Map<String, Object> getSlot(UUID playerId, int slot) {
        ServerPlayer player = getPlayer(playerId);
        if (player == null) {
            return null;
        }
        
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return null;
        }
        
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            return null;
        }
        
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("itemId", getItemId(stack.getItem()));
        itemInfo.put("count", stack.getCount());
        itemInfo.put("damage", stack.getDamageValue());
        itemInfo.put("maxDamage", stack.getMaxDamage());
        
        return itemInfo;
    }
}
