package ru.mjkey.storykee.systems.crafting;

import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a custom recipe registered by a Storykee script.
 * Supports both shaped and shapeless recipes with optional conditions.
 * 
 * Requirements: 50.1, 50.4, 50.5
 */
public class StoryRecipe {
    
    private final String id;
    private final RecipeType type;
    private final ItemStack result;
    private final List<String> ingredients;
    private final String[] pattern; // For shaped recipes (3x3 grid)
    private final Map<Character, String> patternKey; // Maps pattern chars to item IDs
    private final List<Predicate<RecipeContext>> conditions;
    private final String storyId;
    private boolean enabled;
    
    /**
     * Recipe types supported by the system.
     */
    public enum RecipeType {
        SHAPED,
        SHAPELESS,
        SMELTING,
        BLASTING,
        SMOKING,
        CAMPFIRE,
        STONECUTTING,
        SMITHING
    }
    
    private StoryRecipe(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.result = builder.result;
        this.ingredients = new ArrayList<>(builder.ingredients);
        this.pattern = builder.pattern;
        this.patternKey = builder.patternKey != null ? new HashMap<>(builder.patternKey) : null;
        this.conditions = new ArrayList<>(builder.conditions);
        this.storyId = builder.storyId;
        this.enabled = true;
    }
    
    public String getId() {
        return id;
    }
    
    public RecipeType getType() {
        return type;
    }
    
    public ItemStack getResult() {
        return result.copy();
    }
    
    public List<String> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }
    
    public String[] getPattern() {
        return pattern != null ? pattern.clone() : null;
    }
    
    public Map<Character, String> getPatternKey() {
        return patternKey != null ? Collections.unmodifiableMap(patternKey) : null;
    }
    
    public String getStoryId() {
        return storyId;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Checks if all conditions are met for this recipe.
     * Requirement 50.5: WHEN recipe conditions are specified THEN the Runtime SHALL validate them
     */
    public boolean checkConditions(RecipeContext context) {
        if (conditions.isEmpty()) {
            return true;
        }
        for (Predicate<RecipeContext> condition : conditions) {
            if (!condition.test(context)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isShaped() {
        return type == RecipeType.SHAPED;
    }
    
    public boolean isShapeless() {
        return type == RecipeType.SHAPELESS;
    }
    
    @Override
    public String toString() {
        return "StoryRecipe{id='" + id + "', type=" + type + ", result=" + result + "}";
    }
    
    /**
     * Builder for creating StoryRecipe instances.
     */
    public static class Builder {
        private String id;
        private RecipeType type = RecipeType.SHAPELESS;
        private ItemStack result;
        private List<String> ingredients = new ArrayList<>();
        private String[] pattern;
        private Map<Character, String> patternKey;
        private List<Predicate<RecipeContext>> conditions = new ArrayList<>();
        private String storyId;
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder type(RecipeType type) {
            this.type = type;
            return this;
        }
        
        public Builder shaped() {
            this.type = RecipeType.SHAPED;
            return this;
        }
        
        public Builder shapeless() {
            this.type = RecipeType.SHAPELESS;
            return this;
        }
        
        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }
        
        public Builder addIngredient(String itemId) {
            this.ingredients.add(itemId);
            return this;
        }
        
        public Builder ingredients(List<String> ingredients) {
            this.ingredients = new ArrayList<>(ingredients);
            return this;
        }
        
        public Builder pattern(String... pattern) {
            this.pattern = pattern;
            this.type = RecipeType.SHAPED;
            return this;
        }
        
        public Builder patternKey(Map<Character, String> key) {
            this.patternKey = new HashMap<>(key);
            return this;
        }
        
        public Builder define(char symbol, String itemId) {
            if (this.patternKey == null) {
                this.patternKey = new HashMap<>();
            }
            this.patternKey.put(symbol, itemId);
            return this;
        }
        
        public Builder condition(Predicate<RecipeContext> condition) {
            this.conditions.add(condition);
            return this;
        }
        
        public Builder storyId(String storyId) {
            this.storyId = storyId;
            return this;
        }
        
        public StoryRecipe build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Recipe ID is required");
            }
            if (result == null || result.isEmpty()) {
                throw new IllegalStateException("Recipe result is required");
            }
            return new StoryRecipe(this);
        }
    }
}
