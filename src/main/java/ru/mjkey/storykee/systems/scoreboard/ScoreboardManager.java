package ru.mjkey.storykee.systems.scoreboard;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages scoreboards in the Storykee system.
 * 
 * Requirements: 24.1, 24.2, 24.3, 24.4, 24.5
 */
public class ScoreboardManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreboardManager.class);
    
    private static ScoreboardManager instance;
    
    // Tracked story scoreboards
    private final Map<String, StoryScoreboard> storyScoreboards;
    
    // Player display states
    private final Map<UUID, String> playerDisplays;
    
    private MinecraftServer server;
    
    private ScoreboardManager() {
        this.storyScoreboards = new ConcurrentHashMap<>();
        this.playerDisplays = new ConcurrentHashMap<>();
    }
    
    public static ScoreboardManager getInstance() {
        if (instance == null) {
            instance = new ScoreboardManager();
        }
        return instance;
    }
    
    /**
     * Initializes the scoreboard manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("ScoreboardManager initialized");
    }
    
    /**
     * Shuts down the scoreboard manager.
     */
    public void shutdown() {
        // Remove all story scoreboards
        for (String id : new ArrayList<>(storyScoreboards.keySet())) {
            removeScoreboard(id);
        }
        playerDisplays.clear();
        LOGGER.info("ScoreboardManager shutdown");
    }
    
    // ==================== Scoreboard Registration ====================
    
    /**
     * Creates and registers a scoreboard.
     * Requirement 24.1: Register with Minecraft scoreboard system
     * 
     * @param id Unique scoreboard ID
     * @param displayName Display name
     * @return The created scoreboard, or null if failed
     */
    public StoryScoreboard createScoreboard(String id, String displayName) {
        if (id == null || server == null) {
            return null;
        }
        
        if (storyScoreboards.containsKey(id)) {
            LOGGER.warn("Scoreboard '{}' already exists", id);
            return null;
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        String objectiveName = "story_" + id;
        
        // Remove existing objective if present
        Objective existing = scoreboard.getObjective(objectiveName);
        if (existing != null) {
            scoreboard.removeObjective(existing);
        }
        
        // Create new objective
        Objective objective = scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                Component.literal(displayName),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
        );
        
        StoryScoreboard storyScoreboard = new StoryScoreboard(id, objective, displayName);
        storyScoreboards.put(id, storyScoreboard);
        
        LOGGER.debug("Created scoreboard: {}", id);
        return storyScoreboard;
    }
    
    /**
     * Removes a scoreboard.
     * Requirement 24.5: Unregister and clear display
     * 
     * @param id Scoreboard ID
     * @return true if removed
     */
    public boolean removeScoreboard(String id) {
        StoryScoreboard storyScoreboard = storyScoreboards.remove(id);
        if (storyScoreboard == null || server == null) {
            return false;
        }
        
        // Hide from all players showing this scoreboard
        for (Map.Entry<UUID, String> entry : new HashMap<>(playerDisplays).entrySet()) {
            if (entry.getValue().equals(id)) {
                hideScoreboard(entry.getKey());
            }
        }
        
        // Remove from Minecraft scoreboard
        Scoreboard scoreboard = server.getScoreboard();
        scoreboard.removeObjective(storyScoreboard.getObjective());
        
        LOGGER.debug("Removed scoreboard: {}", id);
        return true;
    }
    
    /**
     * Gets a scoreboard by ID.
     * 
     * @param id Scoreboard ID
     * @return The scoreboard, or null if not found
     */
    public StoryScoreboard getScoreboard(String id) {
        return storyScoreboards.get(id);
    }
    
    // ==================== Score Management ====================
    
    /**
     * Sets a player's score.
     * Requirement 24.2: Modify player's score value
     * 
     * @param scoreboardId Scoreboard ID
     * @param playerName Player name (display name for the score)
     * @param score Score value
     * @return true if set successfully
     */
    public boolean setScore(String scoreboardId, String playerName, int score) {
        StoryScoreboard storyScoreboard = storyScoreboards.get(scoreboardId);
        if (storyScoreboard == null || server == null) {
            return false;
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = storyScoreboard.getObjective();
        
        ScoreHolder holder = ScoreHolder.forNameOnly(playerName);
        ScoreAccess access = scoreboard.getOrCreatePlayerScore(holder, objective);
        access.set(score);
        
        storyScoreboard.updateScore(playerName, score);
        
        LOGGER.debug("Set score for '{}' in '{}': {}", playerName, scoreboardId, score);
        return true;
    }
    
    /**
     * Adds to a player's score.
     * 
     * @param scoreboardId Scoreboard ID
     * @param playerName Player name
     * @param amount Amount to add
     * @return New score, or -1 if failed
     */
    public int addScore(String scoreboardId, String playerName, int amount) {
        StoryScoreboard storyScoreboard = storyScoreboards.get(scoreboardId);
        if (storyScoreboard == null || server == null) {
            return -1;
        }
        
        int currentScore = getScore(scoreboardId, playerName);
        int newScore = currentScore + amount;
        setScore(scoreboardId, playerName, newScore);
        return newScore;
    }
    
    /**
     * Gets a player's score.
     * Requirement 24.4: Return current values
     * 
     * @param scoreboardId Scoreboard ID
     * @param playerName Player name
     * @return Score value, or 0 if not found
     */
    public int getScore(String scoreboardId, String playerName) {
        StoryScoreboard storyScoreboard = storyScoreboards.get(scoreboardId);
        if (storyScoreboard == null || server == null) {
            return 0;
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        Objective objective = storyScoreboard.getObjective();
        
        ScoreHolder holder = ScoreHolder.forNameOnly(playerName);
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(holder, objective);
        
        return score != null ? score.value() : 0;
    }
    
    /**
     * Removes a score entry.
     * 
     * @param scoreboardId Scoreboard ID
     * @param playerName Player name
     * @return true if removed
     */
    public boolean removeScore(String scoreboardId, String playerName) {
        StoryScoreboard storyScoreboard = storyScoreboards.get(scoreboardId);
        if (storyScoreboard == null || server == null) {
            return false;
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        ScoreHolder holder = ScoreHolder.forNameOnly(playerName);
        scoreboard.resetAllPlayerScores(holder);
        
        storyScoreboard.removeScore(playerName);
        return true;
    }
    
    // ==================== Display Management ====================
    
    /**
     * Shows a scoreboard to a player.
     * Requirement 24.3: Render in specified screen position
     * 
     * @param player Player
     * @param scoreboardId Scoreboard ID
     * @param slot Display slot (sidebar, list, belowName)
     * @return true if shown
     */
    public boolean showScoreboard(ServerPlayer player, String scoreboardId, DisplaySlot slot) {
        StoryScoreboard storyScoreboard = storyScoreboards.get(scoreboardId);
        if (storyScoreboard == null || server == null || player == null) {
            return false;
        }
        
        Scoreboard scoreboard = server.getScoreboard();
        scoreboard.setDisplayObjective(slot, storyScoreboard.getObjective());
        
        playerDisplays.put(player.getUUID(), scoreboardId);
        
        LOGGER.debug("Showing scoreboard '{}' to player {} in slot {}", 
                scoreboardId, player.getName().getString(), slot);
        return true;
    }
    
    /**
     * Shows a scoreboard in the sidebar.
     */
    public boolean showScoreboard(ServerPlayer player, String scoreboardId) {
        return showScoreboard(player, scoreboardId, DisplaySlot.SIDEBAR);
    }
    
    /**
     * Hides the scoreboard from a player.
     * 
     * @param playerId Player UUID
     * @return true if hidden
     */
    public boolean hideScoreboard(UUID playerId) {
        String scoreboardId = playerDisplays.remove(playerId);
        if (scoreboardId == null || server == null) {
            return false;
        }
        
        // Clear the sidebar display
        Scoreboard scoreboard = server.getScoreboard();
        scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, null);
        
        return true;
    }
    
    /**
     * Gets the scoreboard currently displayed to a player.
     * 
     * @param playerId Player UUID
     * @return Scoreboard ID, or null if none
     */
    public String getDisplayedScoreboard(UUID playerId) {
        return playerDisplays.get(playerId);
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Gets all scoreboard IDs.
     */
    public Set<String> getScoreboardIds() {
        return Collections.unmodifiableSet(storyScoreboards.keySet());
    }
    
    /**
     * Checks if a scoreboard exists.
     */
    public boolean hasScoreboard(String id) {
        return storyScoreboards.containsKey(id);
    }
    
    /**
     * Gets all scores for a scoreboard.
     * 
     * @param scoreboardId Scoreboard ID
     * @return Map of player names to scores
     */
    public Map<String, Integer> getAllScores(String scoreboardId) {
        StoryScoreboard storyScoreboard = storyScoreboards.get(scoreboardId);
        if (storyScoreboard == null) {
            return Collections.emptyMap();
        }
        return storyScoreboard.getScores();
    }
    
    /**
     * Represents a story scoreboard with cached scores.
     */
    public static class StoryScoreboard {
        private final String id;
        private final Objective objective;
        private final String displayName;
        private final Map<String, Integer> scores;
        
        StoryScoreboard(String id, Objective objective, String displayName) {
            this.id = id;
            this.objective = objective;
            this.displayName = displayName;
            this.scores = new ConcurrentHashMap<>();
        }
        
        public String getId() {
            return id;
        }
        
        public Objective getObjective() {
            return objective;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public Map<String, Integer> getScores() {
            return Collections.unmodifiableMap(scores);
        }
        
        void updateScore(String playerName, int score) {
            scores.put(playerName, score);
        }
        
        void removeScore(String playerName) {
            scores.remove(playerName);
        }
    }
}
