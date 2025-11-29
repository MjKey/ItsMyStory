package ru.mjkey.storykee.systems.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages custom loot tables for Storykee scripts.
 * 
 * Requirements: 58.1, 58.2, 58.3, 58.4, 58.5
 */
public class LootManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LootManager.class);
    
    private static LootManager instance;
    
    // Registered loot tables
    private final Map<String, StoryLootTable> lootTables = new ConcurrentHashMap<>();
    
    // Loot tables by story
    private final Map<String, Set<String>> tablesByStory = new ConcurrentHashMap<>();
    
    // Loot generation callbacks
    private final Map<String, List<Consumer<LootGenerationEvent>>> lootCallbacks = new ConcurrentHashMap<>();
    
    // Global loot callbacks
    private final List<Consumer<LootGenerationEvent>> globalCallbacks = Collections.synchronizedList(new ArrayList<>());
    
    private MinecraftServer server;
    
    private LootManager() {
    }
    
    public static LootManager getInstance() {
        if (instance == null) {
            instance = new LootManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Loot Table Registration (Requirement 58.1) =====
    
    /**
     * Registers a custom loot table.
     * Requirement 58.1: WHEN a script defines a loot table THEN the Runtime SHALL register it with the loot system
     */
    public boolean registerLootTable(StoryLootTable table) {
        if (table == null || table.getId() == null) {
            LOGGER.warn("registerLootTable: Invalid loot table");
            return false;
        }
        
        String tableId = table.getId();
        
        if (lootTables.containsKey(tableId)) {
            LOGGER.warn("registerLootTable: Loot table already exists - {}", tableId);
            return false;
        }
        
        lootTables.put(tableId, table);
        
        // Track by story
        if (table.getStoryId() != null) {
            tablesByStory.computeIfAbsent(table.getStoryId(), k -> ConcurrentHashMap.newKeySet())
                .add(tableId);
        }
        
        LOGGER.info("registerLootTable: Registered loot table - {}", tableId);
        return true;
    }

    // ===== Loot Generation (Requirement 58.2) =====
    
    /**
     * Generates loot from a table.
     * Requirement 58.2: WHEN loot is generated THEN the Runtime SHALL use the custom loot table if specified
     */
    public List<ItemStack> generateLoot(String tableId, LootContext context) {
        StoryLootTable table = lootTables.get(tableId);
        if (table == null) {
            LOGGER.warn("generateLoot: Unknown loot table - {}", tableId);
            return Collections.emptyList();
        }
        
        // Generate base loot
        List<ItemStack> loot = new ArrayList<>();
        
        for (StoryLootTable.LootPool pool : table.getPools()) {
            if (!pool.checkConditions(context)) {
                continue;
            }
            
            int rolls = pool.getRolls(context.getRandom());
            
            for (int i = 0; i < rolls; i++) {
                StoryLootTable.LootEntry entry = pool.selectEntry(context.getRandom(), context);
                if (entry != null) {
                    ItemStack item = createItemFromEntry(entry, context);
                    if (!item.isEmpty()) {
                        loot.add(item);
                    }
                }
            }
        }
        
        // Trigger callbacks
        LootGenerationEvent event = new LootGenerationEvent(tableId, context, loot);
        triggerCallbacks(tableId, event);
        
        LOGGER.debug("generateLoot: Generated {} items from table {}", loot.size(), tableId);
        return event.getLoot();
    }
    
    /**
     * Creates an ItemStack from a loot entry.
     */
    private ItemStack createItemFromEntry(StoryLootTable.LootEntry entry, LootContext context) {
        String itemId = entry.getItemId();
        int count = entry.getCount(context.getRandom());
        
        // Parse item ID
        if (!itemId.contains(":")) {
            itemId = "minecraft:" + itemId;
        }
        
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(location);
            
            if (itemOpt.isPresent() && itemOpt.get() != Items.AIR) {
                ItemStack stack = new ItemStack(itemOpt.get(), count);
                // Apply item properties if needed
                return stack;
            }
        } catch (Exception e) {
            LOGGER.warn("createItemFromEntry: Invalid item ID - {}", itemId);
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * Generates loot and gives it to a player.
     */
    public List<ItemStack> generateAndGiveLoot(String tableId, ServerPlayer player) {
        LootContext context = new LootContext.Builder()
            .player(player)
            .position(player.blockPosition())
            .dimension(player.level().dimension().location().toString())
            .build();
        
        List<ItemStack> loot = generateLoot(tableId, context);
        
        for (ItemStack item : loot) {
            if (!player.getInventory().add(item.copy())) {
                // Drop if inventory full
                player.drop(item.copy(), false);
            }
        }
        
        return loot;
    }

    // ===== Loot Callbacks (Requirement 58.5) =====
    
    /**
     * Registers a callback for loot generation.
     * Requirement 58.5: WHEN loot is generated THEN the Runtime SHALL optionally trigger callback events
     */
    public void onLootGenerated(String tableId, Consumer<LootGenerationEvent> callback) {
        if (tableId == null || callback == null) return;
        
        lootCallbacks.computeIfAbsent(tableId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(callback);
    }
    
    /**
     * Registers a global loot callback.
     */
    public void onAnyLootGenerated(Consumer<LootGenerationEvent> callback) {
        if (callback != null) {
            globalCallbacks.add(callback);
        }
    }
    
    private void triggerCallbacks(String tableId, LootGenerationEvent event) {
        // Table-specific callbacks
        List<Consumer<LootGenerationEvent>> callbacks = lootCallbacks.get(tableId);
        if (callbacks != null) {
            for (Consumer<LootGenerationEvent> callback : callbacks) {
                try {
                    callback.accept(event);
                } catch (Exception e) {
                    LOGGER.error("Error in loot callback for table {}: {}", tableId, e.getMessage());
                }
            }
        }
        
        // Global callbacks
        for (Consumer<LootGenerationEvent> callback : globalCallbacks) {
            try {
                callback.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in global loot callback: {}", e.getMessage());
            }
        }
    }

    // ===== Loot Table Modification (Requirement 58.4) =====
    
    /**
     * Modifies an existing loot table by adding a pool.
     * Requirement 58.4: WHEN a script modifies existing loot tables THEN the Runtime SHALL merge or replace entries
     */
    public boolean addPoolToTable(String tableId, StoryLootTable.LootPool pool) {
        StoryLootTable table = lootTables.get(tableId);
        if (table == null) {
            LOGGER.warn("addPoolToTable: Unknown loot table - {}", tableId);
            return false;
        }
        
        // Create a new table with the additional pool
        StoryLootTable.Builder builder = new StoryLootTable.Builder(tableId)
            .storyId(table.getStoryId());
        
        for (StoryLootTable.LootPool existingPool : table.getPools()) {
            builder.addPool(existingPool);
        }
        builder.addPool(pool);
        
        lootTables.put(tableId, builder.build());
        
        LOGGER.info("addPoolToTable: Added pool to loot table {}", tableId);
        return true;
    }

    // ===== Loot Table Removal =====
    
    /**
     * Removes a loot table.
     */
    public boolean removeLootTable(String tableId) {
        StoryLootTable removed = lootTables.remove(tableId);
        if (removed != null) {
            if (removed.getStoryId() != null) {
                Set<String> storyTables = tablesByStory.get(removed.getStoryId());
                if (storyTables != null) {
                    storyTables.remove(tableId);
                }
            }
            lootCallbacks.remove(tableId);
            LOGGER.info("removeLootTable: Removed loot table {}", tableId);
            return true;
        }
        return false;
    }
    
    /**
     * Removes all loot tables for a story.
     */
    public int removeLootTablesByStory(String storyId) {
        Set<String> storyTables = tablesByStory.remove(storyId);
        if (storyTables == null) return 0;
        
        int count = 0;
        for (String tableId : storyTables) {
            if (lootTables.remove(tableId) != null) {
                lootCallbacks.remove(tableId);
                count++;
            }
        }
        
        LOGGER.info("removeLootTablesByStory: Removed {} loot tables for story {}", count, storyId);
        return count;
    }

    // ===== Queries =====
    
    public StoryLootTable getLootTable(String tableId) {
        return lootTables.get(tableId);
    }
    
    public Collection<StoryLootTable> getAllLootTables() {
        return Collections.unmodifiableCollection(lootTables.values());
    }
    
    public boolean hasLootTable(String tableId) {
        return lootTables.containsKey(tableId);
    }
    
    public int getLootTableCount() {
        return lootTables.size();
    }

    // ===== Utility =====
    
    public void clear() {
        lootTables.clear();
        tablesByStory.clear();
        lootCallbacks.clear();
        globalCallbacks.clear();
        LOGGER.info("clear: Cleared all loot tables");
    }
    
    /**
     * Event data for loot generation.
     */
    public static class LootGenerationEvent {
        private final String tableId;
        private final LootContext context;
        private final List<ItemStack> loot;
        
        public LootGenerationEvent(String tableId, LootContext context, List<ItemStack> loot) {
            this.tableId = tableId;
            this.context = context;
            this.loot = loot;
        }
        
        public String getTableId() { return tableId; }
        public LootContext getContext() { return context; }
        public List<ItemStack> getLoot() { return loot; }
        
        public void addItem(ItemStack item) {
            loot.add(item);
        }
        
        public void removeItem(int index) {
            if (index >= 0 && index < loot.size()) {
                loot.remove(index);
            }
        }
        
        public void clearLoot() {
            loot.clear();
        }
    }
}
