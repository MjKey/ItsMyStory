package ru.mjkey.storykee.systems.advancement;

import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a custom advancement/achievement registered by a Storykee script.
 * 
 * Requirements: 51.1, 51.2, 51.3, 51.4, 51.5
 */
public class StoryAdvancement {
    
    private final String id;
    private final String title;
    private final String description;
    private final ItemStack icon;
    private final String parentId; // Parent advancement for tree structure
    private final AdvancementType type;
    private final List<AdvancementCriterion> criteria;
    private final List<AdvancementReward> rewards;
    private final String storyId;
    private final boolean announceToChat;
    private final boolean hidden;
    
    /**
     * Types of advancement display frames.
     */
    public enum AdvancementType {
        TASK,       // Normal advancement
        GOAL,       // Important goal
        CHALLENGE   // Difficult challenge
    }
    
    private StoryAdvancement(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.icon = builder.icon;
        this.parentId = builder.parentId;
        this.type = builder.type;
        this.criteria = new ArrayList<>(builder.criteria);
        this.rewards = new ArrayList<>(builder.rewards);
        this.storyId = builder.storyId;
        this.announceToChat = builder.announceToChat;
        this.hidden = builder.hidden;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ItemStack getIcon() {
        return icon != null ? icon.copy() : ItemStack.EMPTY;
    }
    
    public String getParentId() {
        return parentId;
    }
    
    public AdvancementType getType() {
        return type;
    }
    
    public List<AdvancementCriterion> getCriteria() {
        return Collections.unmodifiableList(criteria);
    }
    
    public List<AdvancementReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }
    
    public String getStoryId() {
        return storyId;
    }
    
    public boolean shouldAnnounceToChat() {
        return announceToChat;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    /**
     * Checks if all criteria are met for a player.
     */
    public boolean checkAllCriteria(UUID playerId, Map<String, Object> context) {
        for (AdvancementCriterion criterion : criteria) {
            if (!criterion.isMet(playerId, context)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "StoryAdvancement{id='" + id + "', title='" + title + "'}";
    }
    
    /**
     * Builder for creating StoryAdvancement instances.
     */
    public static class Builder {
        private String id;
        private String title;
        private String description = "";
        private ItemStack icon;
        private String parentId;
        private AdvancementType type = AdvancementType.TASK;
        private List<AdvancementCriterion> criteria = new ArrayList<>();
        private List<AdvancementReward> rewards = new ArrayList<>();
        private String storyId;
        private boolean announceToChat = true;
        private boolean hidden = false;
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder icon(ItemStack icon) {
            this.icon = icon;
            return this;
        }
        
        public Builder parent(String parentId) {
            this.parentId = parentId;
            return this;
        }
        
        public Builder type(AdvancementType type) {
            this.type = type;
            return this;
        }
        
        public Builder addCriterion(AdvancementCriterion criterion) {
            this.criteria.add(criterion);
            return this;
        }
        
        public Builder addCriterion(String name, Predicate<CriterionContext> condition) {
            this.criteria.add(new AdvancementCriterion(name, condition));
            return this;
        }
        
        public Builder addReward(AdvancementReward reward) {
            this.rewards.add(reward);
            return this;
        }
        
        public Builder storyId(String storyId) {
            this.storyId = storyId;
            return this;
        }
        
        public Builder announceToChat(boolean announce) {
            this.announceToChat = announce;
            return this;
        }
        
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }
        
        public StoryAdvancement build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalStateException("Advancement ID is required");
            }
            if (title == null || title.isEmpty()) {
                title = id;
            }
            return new StoryAdvancement(this);
        }
    }
}
