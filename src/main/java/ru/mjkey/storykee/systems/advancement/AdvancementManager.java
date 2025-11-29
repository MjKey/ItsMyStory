package ru.mjkey.storykee.systems.advancement;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages custom advancements/achievements registered by Storykee scripts.
 * 
 * Requirements: 51.1, 51.2, 51.3, 51.4, 51.5
 */
public class AdvancementManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancementManager.class);
    
    private static AdvancementManager instance;
    
    // All registered advancements
    private final Map<String, StoryAdvancement> advancements = new ConcurrentHashMap<>();
    
    // Advancements grouped by story ID
    private final Map<String, Set<String>> advancementsByStory = new ConcurrentHashMap<>();
    
    // Player advancement progress: playerId -> advancementId -> granted
    private final Map<UUID, Map<String, Boolean>> playerProgress = new ConcurrentHashMap<>();
    
    // Callbacks when advancements are granted
    private final Map<String, List<Consumer<AdvancementContext>>> grantCallbacks = new ConcurrentHashMap<>();
    
    private MinecraftServer server;
    
    private AdvancementManager() {
    }
    
    public static AdvancementManager getInstance() {
        if (instance == null) {
            instance = new AdvancementManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Advancement Registration (Requirement 51.1) =====
    
    /**
     * Registers a custom advancement.
     * Requirement 51.1: WHEN a script defines an advancement THEN the Runtime SHALL register it
     */
    public boolean registerAdvancement(StoryAdvancement advancement) {
        if (advancement == null || advancement.getId() == null) {
            LOGGER.warn("registerAdvancement: Invalid advancement");
            return false;
        }
        
        String advId = advancement.getId();
        
        if (advancements.containsKey(advId)) {
            LOGGER.warn("registerAdvancement: Advancement already exists - {}", advId);
            return false;
        }
        
        advancements.put(advId, advancement);
        
        // Track by story ID
        if (advancement.getStoryId() != null) {
            advancementsByStory.computeIfAbsent(advancement.getStoryId(), k -> ConcurrentHashMap.newKeySet())
                .add(advId);
        }
        
        LOGGER.info("registerAdvancement: Registered advancement - {}", advId);
        return true;
    }
    
    /**
     * Creates and registers an advancement using a builder pattern.
     */
    public StoryAdvancement.Builder createAdvancement(String id) {
        return new StoryAdvancement.Builder(id);
    }

    // ===== Advancement Granting (Requirement 51.2, 51.3) =====
    
    /**
     * Grants an advancement to a player.
     * Requirement 51.2: WHEN advancement criteria are met THEN the Runtime SHALL grant it
     * Requirement 51.3: WHEN an advancement is granted THEN the Runtime SHALL display a notification
     */
    public boolean grantAdvancement(UUID playerId, String advancementId) {
        if (playerId == null || advancementId == null) {
            return false;
        }
        
        StoryAdvancement advancement = advancements.get(advancementId);
        if (advancement == null) {
            LOGGER.warn("grantAdvancement: Unknown advancement - {}", advancementId);
            return false;
        }
        
        // Check if already granted
        if (hasAdvancement(playerId, advancementId)) {
            LOGGER.debug("grantAdvancement: Player {} already has advancement {}", playerId, advancementId);
            return false;
        }
        
        // Mark as granted
        playerProgress.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(advancementId, true);
        
        // Show notification
        if (advancement.shouldAnnounceToChat()) {
            showAdvancementNotification(playerId, advancement);
        }
        
        // Execute rewards
        executeRewards(playerId, advancement);
        
        // Trigger callbacks
        triggerGrantCallbacks(playerId, advancement);
        
        LOGGER.info("grantAdvancement: Granted {} to player {}", advancementId, playerId);
        return true;
    }
    
    /**
     * Shows advancement notification to the player.
     * Requirement 51.3: WHEN an advancement is granted THEN the Runtime SHALL display a notification
     */
    private void showAdvancementNotification(UUID playerId, StoryAdvancement advancement) {
        if (server == null) return;
        
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;
        
        // Send chat message notification
        String frameType = switch (advancement.getType()) {
            case GOAL -> "Goal";
            case CHALLENGE -> "Challenge";
            default -> "Advancement";
        };
        
        Component message = Component.literal("§a" + frameType + " Made! §f" + advancement.getTitle());
        player.sendSystemMessage(message);
        
        // Play sound
        // Note: In a full implementation, we'd use the advancement toast system
    }
    
    /**
     * Executes rewards for an advancement.
     * Requirement 51.5: WHEN advancements have rewards THEN the Runtime SHALL execute reward scripts
     */
    private void executeRewards(UUID playerId, StoryAdvancement advancement) {
        for (AdvancementReward reward : advancement.getRewards()) {
            try {
                executeReward(playerId, reward);
            } catch (Exception e) {
                LOGGER.error("Error executing reward for advancement {}: {}", 
                    advancement.getId(), e.getMessage());
            }
        }
    }
    
    private void executeReward(UUID playerId, AdvancementReward reward) {
        switch (reward.getType()) {
            case EXPERIENCE -> {
                if (server != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        player.giveExperiencePoints(reward.getAmount());
                    }
                }
            }
            case CUSTOM -> {
                Consumer<UUID> action = reward.getCustomAction();
                if (action != null) {
                    action.accept(playerId);
                }
            }
            // ITEM, COMMAND, SCRIPT rewards would be handled by integration with other systems
            default -> LOGGER.debug("executeReward: Reward type {} requires external handling", reward.getType());
        }
    }
    
    private void triggerGrantCallbacks(UUID playerId, StoryAdvancement advancement) {
        List<Consumer<AdvancementContext>> callbacks = grantCallbacks.get(advancement.getId());
        if (callbacks == null) return;
        
        AdvancementContext context = new AdvancementContext(playerId, advancement);
        
        for (Consumer<AdvancementContext> callback : callbacks) {
            try {
                callback.accept(context);
            } catch (Exception e) {
                LOGGER.error("Error in advancement callback: {}", e.getMessage());
            }
        }
    }

    // ===== Advancement Checking (Requirement 51.4) =====
    
    /**
     * Checks if a player has an advancement.
     * Requirement 51.4: WHEN a script checks advancements THEN the Runtime SHALL return completion status
     */
    public boolean hasAdvancement(UUID playerId, String advancementId) {
        Map<String, Boolean> progress = playerProgress.get(playerId);
        return progress != null && Boolean.TRUE.equals(progress.get(advancementId));
    }
    
    /**
     * Gets all advancements a player has completed.
     */
    public Set<String> getPlayerAdvancements(UUID playerId) {
        Map<String, Boolean> progress = playerProgress.get(playerId);
        if (progress == null) {
            return Collections.emptySet();
        }
        
        Set<String> completed = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : progress.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }
    
    /**
     * Gets the completion percentage for a player in a story.
     */
    public double getCompletionPercentage(UUID playerId, String storyId) {
        Set<String> storyAdvancements = advancementsByStory.get(storyId);
        if (storyAdvancements == null || storyAdvancements.isEmpty()) {
            return 0.0;
        }
        
        int completed = 0;
        for (String advId : storyAdvancements) {
            if (hasAdvancement(playerId, advId)) {
                completed++;
            }
        }
        
        return (double) completed / storyAdvancements.size() * 100.0;
    }

    // ===== Criteria Checking =====
    
    /**
     * Checks criteria and grants advancement if all are met.
     * Requirement 51.2: WHEN advancement criteria are met THEN the Runtime SHALL grant it
     */
    public boolean checkAndGrant(UUID playerId, String advancementId, Map<String, Object> context) {
        StoryAdvancement advancement = advancements.get(advancementId);
        if (advancement == null) {
            return false;
        }
        
        if (hasAdvancement(playerId, advancementId)) {
            return false;
        }
        
        if (advancement.checkAllCriteria(playerId, context)) {
            return grantAdvancement(playerId, advancementId);
        }
        
        return false;
    }

    // ===== Callbacks =====
    
    /**
     * Registers a callback for when an advancement is granted.
     */
    public void onGrant(String advancementId, Consumer<AdvancementContext> callback) {
        if (advancementId == null || callback == null) return;
        
        grantCallbacks.computeIfAbsent(advancementId, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(callback);
    }

    // ===== Advancement Removal =====
    
    /**
     * Revokes an advancement from a player.
     */
    public boolean revokeAdvancement(UUID playerId, String advancementId) {
        Map<String, Boolean> progress = playerProgress.get(playerId);
        if (progress != null && progress.remove(advancementId) != null) {
            LOGGER.info("revokeAdvancement: Revoked {} from player {}", advancementId, playerId);
            return true;
        }
        return false;
    }
    
    /**
     * Removes an advancement definition.
     */
    public boolean removeAdvancement(String advancementId) {
        StoryAdvancement removed = advancements.remove(advancementId);
        if (removed != null) {
            if (removed.getStoryId() != null) {
                Set<String> storyAdvs = advancementsByStory.get(removed.getStoryId());
                if (storyAdvs != null) {
                    storyAdvs.remove(advancementId);
                }
            }
            grantCallbacks.remove(advancementId);
            LOGGER.info("removeAdvancement: Removed advancement {}", advancementId);
            return true;
        }
        return false;
    }
    
    /**
     * Removes all advancements for a story.
     */
    public int removeAdvancementsByStory(String storyId) {
        Set<String> storyAdvs = advancementsByStory.remove(storyId);
        if (storyAdvs == null) return 0;
        
        int count = 0;
        for (String advId : storyAdvs) {
            if (advancements.remove(advId) != null) {
                grantCallbacks.remove(advId);
                count++;
            }
        }
        
        LOGGER.info("removeAdvancementsByStory: Removed {} advancements for story {}", count, storyId);
        return count;
    }

    // ===== Queries =====
    
    public StoryAdvancement getAdvancement(String advancementId) {
        return advancements.get(advancementId);
    }
    
    public Collection<StoryAdvancement> getAllAdvancements() {
        return Collections.unmodifiableCollection(advancements.values());
    }
    
    public List<StoryAdvancement> getAdvancementsByStory(String storyId) {
        Set<String> advIds = advancementsByStory.get(storyId);
        if (advIds == null) return Collections.emptyList();
        
        List<StoryAdvancement> result = new ArrayList<>();
        for (String id : advIds) {
            StoryAdvancement adv = advancements.get(id);
            if (adv != null) result.add(adv);
        }
        return result;
    }
    
    public int getAdvancementCount() {
        return advancements.size();
    }

    // ===== Persistence =====
    
    /**
     * Gets player progress data for saving.
     */
    public Map<String, Boolean> getPlayerProgressData(UUID playerId) {
        Map<String, Boolean> progress = playerProgress.get(playerId);
        return progress != null ? new HashMap<>(progress) : new HashMap<>();
    }
    
    /**
     * Loads player progress data.
     */
    public void loadPlayerProgressData(UUID playerId, Map<String, Boolean> data) {
        if (data != null && !data.isEmpty()) {
            playerProgress.put(playerId, new ConcurrentHashMap<>(data));
        }
    }
    
    /**
     * Clears all data.
     */
    public void clear() {
        advancements.clear();
        advancementsByStory.clear();
        playerProgress.clear();
        grantCallbacks.clear();
        LOGGER.info("clear: Cleared all advancements");
    }
    
    /**
     * Clears player progress.
     */
    public void clearPlayerProgress(UUID playerId) {
        playerProgress.remove(playerId);
    }
    
    /**
     * Context for advancement callbacks.
     */
    public static class AdvancementContext {
        private final UUID playerId;
        private final StoryAdvancement advancement;
        
        public AdvancementContext(UUID playerId, StoryAdvancement advancement) {
            this.playerId = playerId;
            this.advancement = advancement;
        }
        
        public UUID getPlayerId() { return playerId; }
        public StoryAdvancement getAdvancement() { return advancement; }
        public String getAdvancementId() { return advancement.getId(); }
    }
}
