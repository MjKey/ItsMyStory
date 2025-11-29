package ru.mjkey.storykee.systems.crafting;

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
 * Manages custom recipes registered by Storykee scripts.
 * Provides functions for registering, removing, and triggering recipe callbacks.
 * 
 * Requirements: 50.1, 50.2, 50.3, 50.4, 50.5
 */
public class RecipeManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeManager.class);
    
    private static RecipeManager instance;
    
    // All registered custom recipes
    private final Map<String, StoryRecipe> recipes = new ConcurrentHashMap<>();
    
    // Recipes grouped by story ID for cleanup
    private final Map<String, Set<String>> recipesByStory = new ConcurrentHashMap<>();
    
    // Crafting callbacks - called when a recipe is crafted
    private final Map<String, List<Consumer<RecipeContext>>> craftingCallbacks = new ConcurrentHashMap<>();
    
    // Global crafting callbacks - called for any crafting
    private final List<Consumer<RecipeContext>> globalCallbacks = Collections.synchronizedList(new ArrayList<>());
    
    private MinecraftServer server;
    
    private RecipeManager() {
    }
    
    public static RecipeManager getInstance() {
        if (instance == null) {
            instance = new RecipeManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    public MinecraftServer getServer() {
        return server;
    }

    // ===== Recipe Registration (Requirement 50.1) =====
    
    /**
     * Registers a custom recipe.
     * Requirement 50.1: WHEN a script registers a recipe THEN the Runtime SHALL add it to the crafting system
     * 
     * @param recipe The recipe to register
     * @return true if registration was successful
     */
    public boolean registerRecipe(StoryRecipe recipe) {
        if (recipe == null || recipe.getId() == null) {
            LOGGER.warn("registerRecipe: Invalid recipe");
            return false;
        }
        
        String recipeId = recipe.getId();
        
        if (recipes.containsKey(recipeId)) {
            LOGGER.warn("registerRecipe: Recipe already exists - {}", recipeId);
            return false;
        }
        
        recipes.put(recipeId, recipe);
        
        // Track by story ID for cleanup
        if (recipe.getStoryId() != null) {
            recipesByStory.computeIfAbsent(recipe.getStoryId(), k -> ConcurrentHashMap.newKeySet())
                .add(recipeId);
        }
        
        LOGGER.info("registerRecipe: Registered recipe - {}", recipeId);
        return true;
    }
    
    /**
     * Registers a shaped recipe.
     * Requirement 50.4: WHEN custom recipes are defined THEN the Runtime SHALL support shaped patterns
     * 
     * @param id Recipe ID
     * @param result The result item stack
     * @param pattern The crafting pattern (up to 3 rows)
     * @param key Map of pattern characters to item IDs
     * @param storyId The story that owns this recipe
     * @return true if registration was successful
     */
    public boolean registerShapedRecipe(String id, ItemStack result, String[] pattern, 
                                         Map<Character, String> key, String storyId) {
        StoryRecipe recipe = new StoryRecipe.Builder(id)
            .shaped()
            .result(result)
            .pattern(pattern)
            .patternKey(key)
            .storyId(storyId)
            .build();
        
        return registerRecipe(recipe);
    }
    
    /**
     * Registers a shapeless recipe.
     * Requirement 50.4: WHEN custom recipes are defined THEN the Runtime SHALL support shapeless patterns
     * 
     * @param id Recipe ID
     * @param result The result item stack
     * @param ingredients List of ingredient item IDs
     * @param storyId The story that owns this recipe
     * @return true if registration was successful
     */
    public boolean registerShapelessRecipe(String id, ItemStack result, List<String> ingredients, String storyId) {
        StoryRecipe recipe = new StoryRecipe.Builder(id)
            .shapeless()
            .result(result)
            .ingredients(ingredients)
            .storyId(storyId)
            .build();
        
        return registerRecipe(recipe);
    }

    // ===== Recipe Removal (Requirement 50.3) =====
    
    /**
     * Removes a registered recipe.
     * Requirement 50.3: WHEN a script removes a recipe THEN the Runtime SHALL unregister it from the system
     * 
     * @param recipeId The recipe ID to remove
     * @return true if the recipe was removed
     */
    public boolean removeRecipe(String recipeId) {
        if (recipeId == null) {
            return false;
        }
        
        StoryRecipe removed = recipes.remove(recipeId);
        
        if (removed != null) {
            // Remove from story tracking
            if (removed.getStoryId() != null) {
                Set<String> storyRecipes = recipesByStory.get(removed.getStoryId());
                if (storyRecipes != null) {
                    storyRecipes.remove(recipeId);
                }
            }
            
            // Remove callbacks
            craftingCallbacks.remove(recipeId);
            
            LOGGER.info("removeRecipe: Removed recipe - {}", recipeId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Removes all recipes registered by a specific story.
     * 
     * @param storyId The story ID
     * @return The number of recipes removed
     */
    public int removeRecipesByStory(String storyId) {
        if (storyId == null) {
            return 0;
        }
        
        Set<String> storyRecipes = recipesByStory.remove(storyId);
        if (storyRecipes == null || storyRecipes.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        for (String recipeId : storyRecipes) {
            if (recipes.remove(recipeId) != null) {
                craftingCallbacks.remove(recipeId);
                count++;
            }
        }
        
        LOGGER.info("removeRecipesByStory: Removed {} recipes for story {}", count, storyId);
        return count;
    }

    // ===== Crafting Callbacks (Requirement 50.2) =====
    
    /**
     * Registers a callback for when a specific recipe is crafted.
     * Requirement 50.2: WHEN a recipe is crafted THEN the Runtime SHALL optionally trigger callback events
     * 
     * @param recipeId The recipe ID
     * @param callback The callback to execute
     */
    public void onCraft(String recipeId, Consumer<RecipeContext> callback) {
        if (recipeId == null || callback == null) {
            return;
        }
        
        craftingCallbacks.computeIfAbsent(recipeId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(callback);
        
        LOGGER.debug("onCraft: Registered callback for recipe {}", recipeId);
    }
    
    /**
     * Registers a global callback for any crafting event.
     * 
     * @param callback The callback to execute
     */
    public void onAnyCraft(Consumer<RecipeContext> callback) {
        if (callback != null) {
            globalCallbacks.add(callback);
        }
    }
    
    /**
     * Triggers crafting callbacks for a recipe.
     * Requirement 50.2: WHEN a recipe is crafted THEN the Runtime SHALL optionally trigger callback events
     * 
     * @param recipeId The recipe ID
     * @param player The player who crafted
     * @param result The crafted item
     * @return The recipe context (may be cancelled)
     */
    public RecipeContext triggerCraftingCallbacks(String recipeId, ServerPlayer player, ItemStack result) {
        StoryRecipe recipe = recipes.get(recipeId);
        UUID playerId = player != null ? player.getUUID() : null;
        
        RecipeContext context = new RecipeContext(playerId, player, recipe, result);
        
        // Check conditions first
        if (recipe != null && !recipe.checkConditions(context)) {
            context.cancel();
            return context;
        }
        
        // Execute recipe-specific callbacks
        List<Consumer<RecipeContext>> callbacks = craftingCallbacks.get(recipeId);
        if (callbacks != null) {
            for (Consumer<RecipeContext> callback : callbacks) {
                try {
                    callback.accept(context);
                    if (context.isCancelled()) {
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error in crafting callback for recipe {}: {}", recipeId, e.getMessage());
                }
            }
        }
        
        // Execute global callbacks if not cancelled
        if (!context.isCancelled()) {
            for (Consumer<RecipeContext> callback : globalCallbacks) {
                try {
                    callback.accept(context);
                    if (context.isCancelled()) {
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.error("Error in global crafting callback: {}", e.getMessage());
                }
            }
        }
        
        return context;
    }

    // ===== Recipe Queries =====
    
    /**
     * Gets a recipe by ID.
     */
    public StoryRecipe getRecipe(String recipeId) {
        return recipes.get(recipeId);
    }
    
    /**
     * Gets all registered recipes.
     */
    public Collection<StoryRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }
    
    /**
     * Gets all recipes for a specific story.
     */
    public List<StoryRecipe> getRecipesByStory(String storyId) {
        Set<String> recipeIds = recipesByStory.get(storyId);
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<StoryRecipe> result = new ArrayList<>();
        for (String id : recipeIds) {
            StoryRecipe recipe = recipes.get(id);
            if (recipe != null) {
                result.add(recipe);
            }
        }
        return result;
    }
    
    /**
     * Checks if a recipe exists.
     */
    public boolean hasRecipe(String recipeId) {
        return recipes.containsKey(recipeId);
    }
    
    /**
     * Gets the count of registered recipes.
     */
    public int getRecipeCount() {
        return recipes.size();
    }

    // ===== Recipe Enable/Disable =====
    
    /**
     * Enables a recipe.
     */
    public boolean enableRecipe(String recipeId) {
        StoryRecipe recipe = recipes.get(recipeId);
        if (recipe != null) {
            recipe.setEnabled(true);
            LOGGER.debug("enableRecipe: Enabled recipe {}", recipeId);
            return true;
        }
        return false;
    }
    
    /**
     * Disables a recipe.
     */
    public boolean disableRecipe(String recipeId) {
        StoryRecipe recipe = recipes.get(recipeId);
        if (recipe != null) {
            recipe.setEnabled(false);
            LOGGER.debug("disableRecipe: Disabled recipe {}", recipeId);
            return true;
        }
        return false;
    }

    // ===== Utility Methods =====
    
    /**
     * Creates an ItemStack from an item ID string.
     */
    public ItemStack createItemStack(String itemId, int count) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
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
    
    /**
     * Clears all recipes and callbacks.
     */
    public void clear() {
        recipes.clear();
        recipesByStory.clear();
        craftingCallbacks.clear();
        globalCallbacks.clear();
        LOGGER.info("clear: Cleared all recipes");
    }
    
    /**
     * Removes all callbacks for a recipe.
     */
    public void clearCallbacks(String recipeId) {
        craftingCallbacks.remove(recipeId);
    }
    
    /**
     * Removes all global callbacks.
     */
    public void clearGlobalCallbacks() {
        globalCallbacks.clear();
    }
}
