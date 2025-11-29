package ru.mjkey.storykee.systems.loot;

import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a custom loot table defined by a Storykee script.
 * 
 * Requirements: 58.1, 58.3
 */
public class StoryLootTable {
    
    private final String id;
    private final List<LootPool> pools;
    private final String storyId;
    
    private StoryLootTable(Builder builder) {
        this.id = builder.id;
        this.pools = new ArrayList<>(builder.pools);
        this.storyId = builder.storyId;
    }
    
    public String getId() {
        return id;
    }
    
    public List<LootPool> getPools() {
        return Collections.unmodifiableList(pools);
    }
    
    public String getStoryId() {
        return storyId;
    }
    
    /**
     * Generates loot from this table.
     */
    public List<ItemStack> generateLoot(LootContext context) {
        List<ItemStack> result = new ArrayList<>();
        Random random = context.getRandom();
        
        for (LootPool pool : pools) {
            // Check pool conditions
            if (!pool.checkConditions(context)) {
                continue;
            }
            
            // Determine number of rolls
            int rolls = pool.getRolls(random);
            
            for (int i = 0; i < rolls; i++) {
                LootEntry entry = pool.selectEntry(random, context);
                if (entry != null) {
                    ItemStack item = entry.generateItem(random, context);
                    if (!item.isEmpty()) {
                        result.add(item);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Builder for creating StoryLootTable instances.
     */
    public static class Builder {
        private String id;
        private List<LootPool> pools = new ArrayList<>();
        private String storyId;
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder addPool(LootPool pool) {
            this.pools.add(pool);
            return this;
        }
        
        public Builder storyId(String storyId) {
            this.storyId = storyId;
            return this;
        }
        
        public StoryLootTable build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Loot table ID is required");
            }
            return new StoryLootTable(this);
        }
    }
    
    /**
     * Represents a pool of loot entries.
     */
    public static class LootPool {
        private final List<LootEntry> entries;
        private final int minRolls;
        private final int maxRolls;
        private final List<Predicate<LootContext>> conditions;
        
        public LootPool(List<LootEntry> entries, int minRolls, int maxRolls, List<Predicate<LootContext>> conditions) {
            this.entries = new ArrayList<>(entries);
            this.minRolls = minRolls;
            this.maxRolls = maxRolls;
            this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
        }
        
        public int getRolls(Random random) {
            if (minRolls == maxRolls) return minRolls;
            return minRolls + random.nextInt(maxRolls - minRolls + 1);
        }
        
        public boolean checkConditions(LootContext context) {
            for (Predicate<LootContext> condition : conditions) {
                if (!condition.test(context)) {
                    return false;
                }
            }
            return true;
        }
        
        public LootEntry selectEntry(Random random, LootContext context) {
            // Filter entries by conditions
            List<LootEntry> validEntries = new ArrayList<>();
            int totalWeight = 0;
            
            for (LootEntry entry : entries) {
                if (entry.checkConditions(context)) {
                    validEntries.add(entry);
                    totalWeight += entry.getWeight();
                }
            }
            
            if (validEntries.isEmpty() || totalWeight <= 0) {
                return null;
            }
            
            // Weighted random selection
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            
            for (LootEntry entry : validEntries) {
                cumulative += entry.getWeight();
                if (roll < cumulative) {
                    return entry;
                }
            }
            
            return validEntries.get(validEntries.size() - 1);
        }
        
        public static class Builder {
            private List<LootEntry> entries = new ArrayList<>();
            private int minRolls = 1;
            private int maxRolls = 1;
            private List<Predicate<LootContext>> conditions = new ArrayList<>();
            
            public Builder addEntry(LootEntry entry) {
                this.entries.add(entry);
                return this;
            }
            
            public Builder rolls(int rolls) {
                this.minRolls = rolls;
                this.maxRolls = rolls;
                return this;
            }
            
            public Builder rolls(int min, int max) {
                this.minRolls = min;
                this.maxRolls = max;
                return this;
            }
            
            public Builder condition(Predicate<LootContext> condition) {
                this.conditions.add(condition);
                return this;
            }
            
            public LootPool build() {
                return new LootPool(entries, minRolls, maxRolls, conditions);
            }
        }
    }
    
    /**
     * Represents a single loot entry.
     */
    public static class LootEntry {
        private final String itemId;
        private final int minCount;
        private final int maxCount;
        private final int weight;
        private final List<Predicate<LootContext>> conditions;
        private final Map<String, Object> itemProperties;
        
        public LootEntry(String itemId, int minCount, int maxCount, int weight, 
                        List<Predicate<LootContext>> conditions, Map<String, Object> itemProperties) {
            this.itemId = itemId;
            this.minCount = minCount;
            this.maxCount = maxCount;
            this.weight = weight;
            this.conditions = conditions != null ? new ArrayList<>(conditions) : new ArrayList<>();
            this.itemProperties = itemProperties != null ? new HashMap<>(itemProperties) : new HashMap<>();
        }
        
        public String getItemId() { return itemId; }
        public int getWeight() { return weight; }
        
        public boolean checkConditions(LootContext context) {
            for (Predicate<LootContext> condition : conditions) {
                if (!condition.test(context)) {
                    return false;
                }
            }
            return true;
        }
        
        public int getCount(Random random) {
            if (minCount == maxCount) return minCount;
            return minCount + random.nextInt(maxCount - minCount + 1);
        }
        
        public ItemStack generateItem(Random random, LootContext context) {
            // Item creation is handled by LootManager using InventoryManager
            return ItemStack.EMPTY; // Placeholder - actual creation in LootManager
        }
        
        public Map<String, Object> getItemProperties() {
            return Collections.unmodifiableMap(itemProperties);
        }
        
        public static class Builder {
            private String itemId;
            private int minCount = 1;
            private int maxCount = 1;
            private int weight = 1;
            private List<Predicate<LootContext>> conditions = new ArrayList<>();
            private Map<String, Object> itemProperties = new HashMap<>();
            
            public Builder(String itemId) {
                this.itemId = itemId;
            }
            
            public Builder count(int count) {
                this.minCount = count;
                this.maxCount = count;
                return this;
            }
            
            public Builder count(int min, int max) {
                this.minCount = min;
                this.maxCount = max;
                return this;
            }
            
            public Builder weight(int weight) {
                this.weight = weight;
                return this;
            }
            
            public Builder condition(Predicate<LootContext> condition) {
                this.conditions.add(condition);
                return this;
            }
            
            public Builder property(String key, Object value) {
                this.itemProperties.put(key, value);
                return this;
            }
            
            public LootEntry build() {
                return new LootEntry(itemId, minCount, maxCount, weight, conditions, itemProperties);
            }
        }
    }
}
