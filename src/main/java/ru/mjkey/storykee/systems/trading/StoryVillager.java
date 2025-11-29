package ru.mjkey.storykee.systems.trading;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerProfession;
import java.util.*;

/**
 * Represents a custom villager with story-defined trades.
 * 
 * Requirements: 59.1, 59.2, 59.5
 */
public class StoryVillager {
    
    private final String id;
    private final String name;
    private final Holder<VillagerProfession> profession;
    private final List<StoryTrade> trades;
    private final String storyId;
    private UUID entityUUID;
    private boolean customAppearance;
    private String skinTexture;
    
    private StoryVillager(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.profession = builder.profession;
        this.trades = new ArrayList<>(builder.trades);
        this.storyId = builder.storyId;
        this.customAppearance = builder.customAppearance;
        this.skinTexture = builder.skinTexture;
    }
    
    public String getId() { return id; }
    public String getName() { return name; }
    public Holder<VillagerProfession> getProfession() { return profession; }
    public List<StoryTrade> getTrades() { return Collections.unmodifiableList(trades); }
    public String getStoryId() { return storyId; }
    public UUID getEntityUUID() { return entityUUID; }
    public boolean hasCustomAppearance() { return customAppearance; }
    public String getSkinTexture() { return skinTexture; }
    
    public void setEntityUUID(UUID uuid) {
        this.entityUUID = uuid;
    }
    
    public void addTrade(StoryTrade trade) {
        trades.add(trade);
    }
    
    public void removeTrade(int index) {
        if (index >= 0 && index < trades.size()) {
            trades.remove(index);
        }
    }
    
    public void clearTrades() {
        trades.clear();
    }
    
    public void resetAllTrades() {
        for (StoryTrade trade : trades) {
            trade.resetUses();
        }
    }
    
    /**
     * Builder for creating StoryVillager instances.
     */
    public static class Builder {
        private String id;
        private String name = "Merchant";
        private Holder<VillagerProfession> profession = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(net.minecraft.world.entity.npc.VillagerProfession.NONE);
        private List<StoryTrade> trades = new ArrayList<>();
        private String storyId;
        private boolean customAppearance = false;
        private String skinTexture;
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder profession(net.minecraft.resources.ResourceKey<VillagerProfession> professionKey) {
            this.profession = BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(professionKey);
            return this;
        }
        
        public Builder profession(Holder<VillagerProfession> profession) {
            this.profession = profession;
            return this;
        }
        
        public Builder addTrade(StoryTrade trade) {
            this.trades.add(trade);
            return this;
        }
        
        public Builder trades(List<StoryTrade> trades) {
            this.trades = new ArrayList<>(trades);
            return this;
        }
        
        public Builder storyId(String storyId) {
            this.storyId = storyId;
            return this;
        }
        
        public Builder customAppearance(boolean custom) {
            this.customAppearance = custom;
            return this;
        }
        
        public Builder skinTexture(String texture) {
            this.skinTexture = texture;
            this.customAppearance = true;
            return this;
        }
        
        public StoryVillager build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Villager ID is required");
            }
            return new StoryVillager(this);
        }
    }
}
