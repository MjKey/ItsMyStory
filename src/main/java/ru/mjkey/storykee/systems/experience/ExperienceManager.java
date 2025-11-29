package ru.mjkey.storykee.systems.experience;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages player experience and levels in the Storykee system.
 * 
 * Requirements: 46.1, 46.2, 46.3, 46.4, 46.5
 */
public class ExperienceManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperienceManager.class);
    
    private static ExperienceManager instance;
    
    // Event listeners for level-up events
    private final List<LevelUpListener> levelUpListeners;
    
    private MinecraftServer server;
    
    private ExperienceManager() {
        this.levelUpListeners = new ArrayList<>();
    }
    
    public static ExperienceManager getInstance() {
        if (instance == null) {
            instance = new ExperienceManager();
        }
        return instance;
    }
    
    /**
     * Initializes the experience manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("ExperienceManager initialized");
    }
    
    /**
     * Shuts down the experience manager.
     */
    public void shutdown() {
        levelUpListeners.clear();
        LOGGER.info("ExperienceManager shutdown");
    }
    
    // ==================== Experience Giving ====================
    
    /**
     * Gives experience points to a player.
     * Requirement 46.1: Create experience giving
     * 
     * @param player Player to give experience to
     * @param amount Experience points to give
     */
    public void giveExperience(ServerPlayer player, int amount) {
        if (player == null || amount == 0) {
            return;
        }
        
        int oldLevel = player.experienceLevel;
        
        player.giveExperiencePoints(amount);
        
        int newLevel = player.experienceLevel;
        
        // Check for level up
        // Requirement 46.4: Add level-up events
        if (newLevel > oldLevel) {
            notifyLevelUp(player, oldLevel, newLevel);
        }
        
        LOGGER.debug("Gave {} XP to {} (level {} -> {})", 
                amount, player.getName().getString(), oldLevel, newLevel);
    }
    
    /**
     * Gives experience levels to a player.
     * 
     * @param player Player to give levels to
     * @param levels Levels to give
     */
    public void giveLevels(ServerPlayer player, int levels) {
        if (player == null || levels == 0) {
            return;
        }
        
        int oldLevel = player.experienceLevel;
        
        player.giveExperienceLevels(levels);
        
        int newLevel = player.experienceLevel;
        
        if (newLevel > oldLevel) {
            notifyLevelUp(player, oldLevel, newLevel);
        }
        
        LOGGER.debug("Gave {} levels to {} (level {} -> {})", 
                levels, player.getName().getString(), oldLevel, newLevel);
    }
    
    /**
     * Removes experience points from a player.
     * 
     * @param player Player to remove experience from
     * @param amount Experience points to remove
     */
    public void removeExperience(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        
        // Negative experience to remove
        player.giveExperiencePoints(-amount);
        
        LOGGER.debug("Removed {} XP from {}", amount, player.getName().getString());
    }
    
    // ==================== Level Setting ====================
    
    /**
     * Sets a player's experience level.
     * Requirement 46.2: Add level setting
     * 
     * @param player Player
     * @param level Level to set
     */
    public void setLevel(ServerPlayer player, int level) {
        if (player == null || level < 0) {
            return;
        }
        
        int oldLevel = player.experienceLevel;
        
        player.experienceLevel = level;
        player.experienceProgress = 0;
        player.setExperiencePoints(0);
        
        if (level > oldLevel) {
            notifyLevelUp(player, oldLevel, level);
        }
        
        LOGGER.debug("Set {} level to {}", player.getName().getString(), level);
    }
    
    /**
     * Sets a player's experience progress within current level.
     * 
     * @param player Player
     * @param progress Progress (0.0 to 1.0)
     */
    public void setExperienceProgress(ServerPlayer player, float progress) {
        if (player == null) {
            return;
        }
        
        player.experienceProgress = Math.max(0, Math.min(1, progress));
    }
    
    /**
     * Sets a player's total experience points.
     * 
     * @param player Player
     * @param totalXp Total experience points
     */
    public void setTotalExperience(ServerPlayer player, int totalXp) {
        if (player == null || totalXp < 0) {
            return;
        }
        
        int oldLevel = player.experienceLevel;
        
        // Reset and add
        player.experienceLevel = 0;
        player.experienceProgress = 0;
        player.setExperiencePoints(0);
        player.giveExperiencePoints(totalXp);
        
        int newLevel = player.experienceLevel;
        
        if (newLevel > oldLevel) {
            notifyLevelUp(player, oldLevel, newLevel);
        }
    }
    
    // ==================== Experience Checking ====================
    
    /**
     * Gets a player's current level.
     * Requirement 46.3: Implement experience checking
     * 
     * @param player Player
     * @return Current level
     */
    public int getLevel(ServerPlayer player) {
        return player != null ? player.experienceLevel : 0;
    }
    
    /**
     * Gets a player's experience progress within current level.
     * 
     * @param player Player
     * @return Progress (0.0 to 1.0)
     */
    public float getExperienceProgress(ServerPlayer player) {
        return player != null ? player.experienceProgress : 0;
    }
    
    /**
     * Gets a player's total experience points.
     * 
     * @param player Player
     * @return Total experience points
     */
    public int getTotalExperience(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        // Calculate total XP from level and progress
        int level = player.experienceLevel;
        float progress = player.experienceProgress;
        
        int totalFromLevels = calculateTotalXpForLevel(level);
        int xpForNextLevel = getXpForNextLevel(level);
        int progressXp = (int) (progress * xpForNextLevel);
        
        return totalFromLevels + progressXp;
    }
    
    /**
     * Gets the experience needed for the next level.
     * 
     * @param player Player
     * @return XP needed for next level
     */
    public int getXpToNextLevel(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        
        int level = player.experienceLevel;
        int xpForLevel = getXpForNextLevel(level);
        int currentProgress = (int) (player.experienceProgress * xpForLevel);
        
        return xpForLevel - currentProgress;
    }
    
    /**
     * Checks if a player has at least a certain level.
     * 
     * @param player Player
     * @param level Level to check
     * @return true if player has at least that level
     */
    public boolean hasLevel(ServerPlayer player, int level) {
        return player != null && player.experienceLevel >= level;
    }
    
    /**
     * Checks if a player has at least a certain amount of experience.
     * 
     * @param player Player
     * @param xp Experience to check
     * @return true if player has at least that much XP
     */
    public boolean hasExperience(ServerPlayer player, int xp) {
        return getTotalExperience(player) >= xp;
    }
    
    // ==================== Level-Up Events ====================
    
    /**
     * Adds a level-up listener.
     * Requirement 46.4: Add level-up events
     * 
     * @param listener Listener to add
     */
    public void addLevelUpListener(LevelUpListener listener) {
        levelUpListeners.add(listener);
    }
    
    /**
     * Removes a level-up listener.
     * 
     * @param listener Listener to remove
     */
    public void removeLevelUpListener(LevelUpListener listener) {
        levelUpListeners.remove(listener);
    }
    
    private void notifyLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        for (LevelUpListener listener : levelUpListeners) {
            try {
                listener.onLevelUp(player, oldLevel, newLevel);
            } catch (Exception e) {
                LOGGER.error("Error in level-up listener: {}", e.getMessage());
            }
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Calculates XP needed for a specific level.
     * Uses Minecraft's XP formula.
     */
    private int getXpForNextLevel(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        } else if (level >= 15) {
            return 37 + (level - 15) * 5;
        } else {
            return 7 + level * 2;
        }
    }
    
    /**
     * Calculates total XP needed to reach a level from 0.
     */
    private int calculateTotalXpForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }
    
    /**
     * Listener interface for level-up events.
     */
    public interface LevelUpListener {
        /**
         * Called when a player levels up.
         * 
         * @param player Player who leveled up
         * @param oldLevel Previous level
         * @param newLevel New level
         */
        void onLevelUp(ServerPlayer player, int oldLevel, int newLevel);
    }
}
