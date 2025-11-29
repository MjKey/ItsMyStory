package ru.mjkey.storykee.systems.trading;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages custom villagers and trading for Storykee scripts.
 * 
 * Requirements: 59.1, 59.2, 59.3, 59.4, 59.5
 */
public class TradingManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TradingManager.class);
    
    private static TradingManager instance;
    
    // Registered story villagers
    private final Map<String, StoryVillager> villagers = new ConcurrentHashMap<>();
    
    // Villagers by story
    private final Map<String, Set<String>> villagersByStory = new ConcurrentHashMap<>();
    
    // Trade callbacks
    private final Map<String, List<Consumer<TradeEvent>>> tradeCallbacks = new ConcurrentHashMap<>();
    
    // Global trade callbacks
    private final List<Consumer<TradeEvent>> globalTradeCallbacks = Collections.synchronizedList(new ArrayList<>());
    
    private MinecraftServer server;
    
    private TradingManager() {
    }
    
    public static TradingManager getInstance() {
        if (instance == null) {
            instance = new TradingManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Villager Creation (Requirement 59.1) =====
    
    /**
     * Creates and spawns a custom villager.
     * Requirement 59.1: WHEN a script creates a villager THEN the Runtime SHALL spawn it with specified profession and trades
     */
    public Villager spawnVillager(StoryVillager storyVillager, double x, double y, double z, String dimension) {
        if (server == null) {
            LOGGER.warn("spawnVillager: Server not available");
            return null;
        }
        
        ServerLevel level = getLevel(dimension);
        if (level == null) {
            LOGGER.warn("spawnVillager: Unknown dimension - {}", dimension);
            return null;
        }
        
        // Create villager entity
        Villager villager = new Villager(EntityType.VILLAGER, level);
        villager.setPos(x, y, z);
        
        // Set profession
        villager.setVillagerData(villager.getVillagerData().withProfession(storyVillager.getProfession()));
        
        // Set custom name
        if (storyVillager.getName() != null) {
            villager.setCustomName(Component.literal(storyVillager.getName()));
            villager.setCustomNameVisible(true);
        }
        
        // Apply trades
        applyTrades(villager, storyVillager);
        
        // Spawn the entity
        if (level.addFreshEntity(villager)) {
            storyVillager.setEntityUUID(villager.getUUID());
            villagers.put(storyVillager.getId(), storyVillager);
            
            // Track by story
            if (storyVillager.getStoryId() != null) {
                villagersByStory.computeIfAbsent(storyVillager.getStoryId(), k -> ConcurrentHashMap.newKeySet())
                    .add(storyVillager.getId());
            }
            
            LOGGER.info("spawnVillager: Spawned villager {} at ({}, {}, {})", storyVillager.getId(), x, y, z);
            return villager;
        }
        
        return null;
    }
    
    /**
     * Applies custom trades to a villager.
     * Requirement 59.2: WHEN trades are defined THEN the Runtime SHALL set the items, prices, and stock limits
     */
    private void applyTrades(Villager villager, StoryVillager storyVillager) {
        MerchantOffers offers = new MerchantOffers();
        
        for (StoryTrade trade : storyVillager.getTrades()) {
            if (trade.isDisabled()) continue;
            
            ItemCost costA = new ItemCost(trade.getCostA().getItem(), trade.getCostA().getCount());
            Optional<ItemCost> costB = trade.hasCostB() ? 
                Optional.of(new ItemCost(trade.getCostB().getItem(), trade.getCostB().getCount())) : 
                Optional.empty();
            
            MerchantOffer offer = new MerchantOffer(
                costA,
                costB,
                trade.getResult(),
                trade.getUses(),
                trade.getMaxUses(),
                trade.getXpReward(),
                trade.getPriceMultiplier()
            );
            
            offers.add(offer);
        }
        
        villager.setOffers(offers);
    }

    // ===== Trade Callbacks (Requirement 59.3) =====
    
    /**
     * Registers a callback for when a trade is made.
     * Requirement 59.3: WHEN a player trades THEN the Runtime SHALL optionally trigger trade event handlers
     */
    public void onTrade(String villagerId, Consumer<TradeEvent> callback) {
        if (villagerId == null || callback == null) return;
        
        tradeCallbacks.computeIfAbsent(villagerId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(callback);
    }
    
    /**
     * Registers a global trade callback.
     */
    public void onAnyTrade(Consumer<TradeEvent> callback) {
        if (callback != null) {
            globalTradeCallbacks.add(callback);
        }
    }
    
    /**
     * Triggers trade callbacks.
     */
    public void triggerTradeCallbacks(String villagerId, ServerPlayer player, StoryTrade trade, ItemStack result) {
        TradeEvent event = new TradeEvent(villagerId, player, trade, result);
        
        // Villager-specific callbacks
        List<Consumer<TradeEvent>> callbacks = tradeCallbacks.get(villagerId);
        if (callbacks != null) {
            for (Consumer<TradeEvent> callback : callbacks) {
                try {
                    callback.accept(event);
                } catch (Exception e) {
                    LOGGER.error("Error in trade callback for villager {}: {}", villagerId, e.getMessage());
                }
            }
        }
        
        // Global callbacks
        for (Consumer<TradeEvent> callback : globalTradeCallbacks) {
            try {
                callback.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in global trade callback: {}", e.getMessage());
            }
        }
    }

    // ===== Villager Updates (Requirement 59.4) =====
    
    /**
     * Updates villager properties.
     * Requirement 59.4: WHEN villager properties are updated THEN the Runtime SHALL refresh the trading interface
     */
    public boolean updateVillager(String villagerId, StoryVillager updatedVillager) {
        StoryVillager existing = villagers.get(villagerId);
        if (existing == null) {
            LOGGER.warn("updateVillager: Unknown villager - {}", villagerId);
            return false;
        }
        
        Villager entity = getVillagerEntity(villagerId);
        if (entity == null) {
            LOGGER.warn("updateVillager: Villager entity not found - {}", villagerId);
            return false;
        }
        
        // Update name
        if (updatedVillager.getName() != null) {
            entity.setCustomName(Component.literal(updatedVillager.getName()));
        }
        
        // Update profession
        entity.setVillagerData(entity.getVillagerData().withProfession(updatedVillager.getProfession()));
        
        // Update trades
        applyTrades(entity, updatedVillager);
        
        // Update stored villager
        villagers.put(villagerId, updatedVillager);
        updatedVillager.setEntityUUID(existing.getEntityUUID());
        
        LOGGER.info("updateVillager: Updated villager {}", villagerId);
        return true;
    }
    
    /**
     * Adds a trade to an existing villager.
     */
    public boolean addTrade(String villagerId, StoryTrade trade) {
        StoryVillager storyVillager = villagers.get(villagerId);
        if (storyVillager == null) return false;
        
        storyVillager.addTrade(trade);
        
        Villager entity = getVillagerEntity(villagerId);
        if (entity != null) {
            applyTrades(entity, storyVillager);
        }
        
        return true;
    }
    
    /**
     * Refreshes villager trades (restocks).
     */
    public boolean restockVillager(String villagerId) {
        StoryVillager storyVillager = villagers.get(villagerId);
        if (storyVillager == null) return false;
        
        storyVillager.resetAllTrades();
        
        Villager entity = getVillagerEntity(villagerId);
        if (entity != null) {
            applyTrades(entity, storyVillager);
        }
        
        LOGGER.debug("restockVillager: Restocked villager {}", villagerId);
        return true;
    }

    // ===== Villager Removal =====
    
    /**
     * Removes a villager.
     */
    public boolean removeVillager(String villagerId) {
        StoryVillager storyVillager = villagers.remove(villagerId);
        if (storyVillager == null) return false;
        
        // Remove entity
        Villager entity = getVillagerEntity(villagerId);
        if (entity != null) {
            entity.discard();
        }
        
        // Remove from story tracking
        if (storyVillager.getStoryId() != null) {
            Set<String> storyVillagers = villagersByStory.get(storyVillager.getStoryId());
            if (storyVillagers != null) {
                storyVillagers.remove(villagerId);
            }
        }
        
        // Remove callbacks
        tradeCallbacks.remove(villagerId);
        
        LOGGER.info("removeVillager: Removed villager {}", villagerId);
        return true;
    }
    
    /**
     * Removes all villagers for a story.
     */
    public int removeVillagersByStory(String storyId) {
        Set<String> storyVillagers = villagersByStory.remove(storyId);
        if (storyVillagers == null) return 0;
        
        int count = 0;
        for (String villagerId : storyVillagers) {
            StoryVillager storyVillager = villagers.remove(villagerId);
            if (storyVillager != null) {
                Villager entity = getVillagerEntity(villagerId);
                if (entity != null) {
                    entity.discard();
                }
                tradeCallbacks.remove(villagerId);
                count++;
            }
        }
        
        LOGGER.info("removeVillagersByStory: Removed {} villagers for story {}", count, storyId);
        return count;
    }

    // ===== Queries =====
    
    public StoryVillager getStoryVillager(String villagerId) {
        return villagers.get(villagerId);
    }
    
    public Villager getVillagerEntity(String villagerId) {
        StoryVillager storyVillager = villagers.get(villagerId);
        if (storyVillager == null || storyVillager.getEntityUUID() == null || server == null) {
            return null;
        }
        
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(storyVillager.getEntityUUID());
            if (entity instanceof Villager villager) {
                return villager;
            }
        }
        
        return null;
    }
    
    public Collection<StoryVillager> getAllVillagers() {
        return Collections.unmodifiableCollection(villagers.values());
    }
    
    public List<StoryVillager> getVillagersByStory(String storyId) {
        Set<String> ids = villagersByStory.get(storyId);
        if (ids == null) return Collections.emptyList();
        
        List<StoryVillager> result = new ArrayList<>();
        for (String id : ids) {
            StoryVillager v = villagers.get(id);
            if (v != null) result.add(v);
        }
        return result;
    }
    
    public boolean hasVillager(String villagerId) {
        return villagers.containsKey(villagerId);
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
     * Creates an ItemStack from an item ID.
     */
    public ItemStack createItemStack(String itemId, int count) {
        if (itemId == null) return ItemStack.EMPTY;
        
        if (!itemId.contains(":")) {
            itemId = "minecraft:" + itemId;
        }
        
        try {
            ResourceLocation location = ResourceLocation.parse(itemId);
            Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(location);
            
            if (itemOpt.isPresent() && itemOpt.get() != Items.AIR) {
                return new ItemStack(itemOpt.get(), count);
            }
        } catch (Exception e) {
            LOGGER.warn("createItemStack: Invalid item ID - {}", itemId);
        }
        
        return ItemStack.EMPTY;
    }
    
    public void clear() {
        // Remove all villager entities
        for (String villagerId : new ArrayList<>(villagers.keySet())) {
            removeVillager(villagerId);
        }
        
        villagers.clear();
        villagersByStory.clear();
        tradeCallbacks.clear();
        globalTradeCallbacks.clear();
        
        LOGGER.info("clear: Cleared all villagers");
    }
    
    /**
     * Event data for trades.
     */
    public static class TradeEvent {
        private final String villagerId;
        private final ServerPlayer player;
        private final StoryTrade trade;
        private final ItemStack result;
        private boolean cancelled;
        
        public TradeEvent(String villagerId, ServerPlayer player, StoryTrade trade, ItemStack result) {
            this.villagerId = villagerId;
            this.player = player;
            this.trade = trade;
            this.result = result;
            this.cancelled = false;
        }
        
        public String getVillagerId() { return villagerId; }
        public ServerPlayer getPlayer() { return player; }
        public UUID getPlayerId() { return player != null ? player.getUUID() : null; }
        public StoryTrade getTrade() { return trade; }
        public ItemStack getResult() { return result; }
        public boolean isCancelled() { return cancelled; }
        
        public void cancel() {
            this.cancelled = true;
        }
    }
}
