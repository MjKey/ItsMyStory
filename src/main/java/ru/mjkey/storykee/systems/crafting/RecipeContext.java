package ru.mjkey.storykee.systems.crafting;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/**
 * Context information for recipe condition evaluation and callbacks.
 * 
 * Requirements: 50.2, 50.5
 */
public class RecipeContext {
    
    private final UUID playerId;
    private final ServerPlayer player;
    private final StoryRecipe recipe;
    private final ItemStack result;
    private boolean cancelled;
    
    public RecipeContext(UUID playerId, ServerPlayer player, StoryRecipe recipe, ItemStack result) {
        this.playerId = playerId;
        this.player = player;
        this.recipe = recipe;
        this.result = result;
        this.cancelled = false;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public ServerPlayer getPlayer() {
        return player;
    }
    
    public StoryRecipe getRecipe() {
        return recipe;
    }
    
    public String getRecipeId() {
        return recipe != null ? recipe.getId() : null;
    }
    
    public ItemStack getResult() {
        return result;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Cancels the crafting operation.
     */
    public void cancel() {
        this.cancelled = true;
    }
}
