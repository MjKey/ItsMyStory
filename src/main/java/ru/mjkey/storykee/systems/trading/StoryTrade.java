package ru.mjkey.storykee.systems.trading;

import net.minecraft.world.item.ItemStack;

/**
 * Represents a custom trade for a story villager.
 * 
 * Requirements: 59.2
 */
public class StoryTrade {
    
    private final ItemStack costA;
    private final ItemStack costB; // Optional second cost
    private final ItemStack result;
    private final int maxUses;
    private final int xpReward;
    private final float priceMultiplier;
    private int uses;
    private boolean disabled;
    
    public StoryTrade(ItemStack costA, ItemStack costB, ItemStack result, int maxUses, int xpReward, float priceMultiplier) {
        this.costA = costA.copy();
        this.costB = costB != null ? costB.copy() : ItemStack.EMPTY;
        this.result = result.copy();
        this.maxUses = maxUses;
        this.xpReward = xpReward;
        this.priceMultiplier = priceMultiplier;
        this.uses = 0;
        this.disabled = false;
    }
    
    public ItemStack getCostA() { return costA.copy(); }
    public ItemStack getCostB() { return costB.copy(); }
    public ItemStack getResult() { return result.copy(); }
    public int getMaxUses() { return maxUses; }
    public int getXpReward() { return xpReward; }
    public float getPriceMultiplier() { return priceMultiplier; }
    public int getUses() { return uses; }
    public boolean isDisabled() { return disabled; }
    
    public void incrementUses() {
        uses++;
    }
    
    public void resetUses() {
        uses = 0;
    }
    
    public boolean hasStock() {
        return maxUses <= 0 || uses < maxUses;
    }
    
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
    
    public boolean hasCostB() {
        return !costB.isEmpty();
    }
    
    /**
     * Builder for creating StoryTrade instances.
     */
    public static class Builder {
        private ItemStack costA;
        private ItemStack costB = ItemStack.EMPTY;
        private ItemStack result;
        private int maxUses = 12;
        private int xpReward = 1;
        private float priceMultiplier = 0.05f;
        
        public Builder costA(ItemStack cost) {
            this.costA = cost;
            return this;
        }
        
        public Builder costB(ItemStack cost) {
            this.costB = cost;
            return this;
        }
        
        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }
        
        public Builder maxUses(int maxUses) {
            this.maxUses = maxUses;
            return this;
        }
        
        public Builder unlimitedUses() {
            this.maxUses = 0;
            return this;
        }
        
        public Builder xpReward(int xp) {
            this.xpReward = xp;
            return this;
        }
        
        public Builder priceMultiplier(float multiplier) {
            this.priceMultiplier = multiplier;
            return this;
        }
        
        public StoryTrade build() {
            if (costA == null || costA.isEmpty()) {
                throw new IllegalStateException("Trade cost A is required");
            }
            if (result == null || result.isEmpty()) {
                throw new IllegalStateException("Trade result is required");
            }
            return new StoryTrade(costA, costB, result, maxUses, xpReward, priceMultiplier);
        }
    }
}
