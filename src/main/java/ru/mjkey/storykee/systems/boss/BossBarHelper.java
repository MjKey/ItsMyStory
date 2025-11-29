package ru.mjkey.storykee.systems.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Helper class for managing boss bar display and customization.
 * Works with Minecraft's built-in ServerBossEvent system.
 * 
 * Requirements: 18.2 - Display boss health bar to all nearby players
 */
public class BossBarHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BossBarHelper.class);
    
    /**
     * Updates the boss bar name.
     */
    public static void setName(StoryBoss boss, String name) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setName(Component.literal(name));
        }
    }
    
    /**
     * Updates the boss bar progress (0.0 to 1.0).
     */
    public static void setProgress(StoryBoss boss, float progress) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
        }
    }
    
    /**
     * Sets the boss bar color.
     */
    public static void setColor(StoryBoss boss, BossEvent.BossBarColor color) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setColor(color);
        }
    }
    
    /**
     * Sets the boss bar color by name.
     */
    public static void setColor(StoryBoss boss, String colorName) {
        BossEvent.BossBarColor color = parseColor(colorName);
        setColor(boss, color);
    }
    
    /**
     * Sets the boss bar overlay style.
     */
    public static void setOverlay(StoryBoss boss, BossEvent.BossBarOverlay overlay) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setOverlay(overlay);
        }
    }
    
    /**
     * Sets the boss bar overlay style by name.
     */
    public static void setOverlay(StoryBoss boss, String overlayName) {
        BossEvent.BossBarOverlay overlay = parseOverlay(overlayName);
        setOverlay(boss, overlay);
    }
    
    /**
     * Sets whether the boss bar should darken the sky.
     */
    public static void setDarkenScreen(StoryBoss boss, boolean darken) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setDarkenScreen(darken);
        }
    }
    
    /**
     * Sets whether the boss bar should play boss music.
     */
    public static void setPlayBossMusic(StoryBoss boss, boolean playMusic) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setPlayBossMusic(playMusic);
        }
    }
    
    /**
     * Sets whether the boss bar should create world fog.
     */
    public static void setCreateWorldFog(StoryBoss boss, boolean createFog) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setCreateWorldFog(createFog);
        }
    }
    
    /**
     * Manually adds a player to see the boss bar.
     */
    public static void addPlayer(StoryBoss boss, ServerPlayer player) {
        if (boss != null && boss.getBossEvent() != null && player != null) {
            boss.getBossEvent().addPlayer(player);
        }
    }
    
    /**
     * Manually removes a player from seeing the boss bar.
     */
    public static void removePlayer(StoryBoss boss, ServerPlayer player) {
        if (boss != null && boss.getBossEvent() != null && player != null) {
            boss.getBossEvent().removePlayer(player);
        }
    }
    
    /**
     * Gets all players currently seeing the boss bar.
     */
    public static Collection<ServerPlayer> getPlayers(StoryBoss boss) {
        if (boss != null && boss.getBossEvent() != null) {
            return boss.getBossEvent().getPlayers();
        }
        return java.util.Collections.emptyList();
    }
    
    /**
     * Sets the boss bar visibility.
     */
    public static void setVisible(StoryBoss boss, boolean visible) {
        if (boss != null && boss.getBossEvent() != null) {
            boss.getBossEvent().setVisible(visible);
        }
    }
    
    /**
     * Parses a color name to BossBarColor enum.
     */
    public static BossEvent.BossBarColor parseColor(String colorName) {
        if (colorName == null) {
            return BossEvent.BossBarColor.PURPLE;
        }
        
        return switch (colorName.toLowerCase()) {
            case "pink" -> BossEvent.BossBarColor.PINK;
            case "blue" -> BossEvent.BossBarColor.BLUE;
            case "red" -> BossEvent.BossBarColor.RED;
            case "green" -> BossEvent.BossBarColor.GREEN;
            case "yellow" -> BossEvent.BossBarColor.YELLOW;
            case "purple" -> BossEvent.BossBarColor.PURPLE;
            case "white" -> BossEvent.BossBarColor.WHITE;
            default -> BossEvent.BossBarColor.PURPLE;
        };
    }
    
    /**
     * Parses an overlay name to BossBarOverlay enum.
     */
    public static BossEvent.BossBarOverlay parseOverlay(String overlayName) {
        if (overlayName == null) {
            return BossEvent.BossBarOverlay.PROGRESS;
        }
        
        return switch (overlayName.toLowerCase()) {
            case "progress" -> BossEvent.BossBarOverlay.PROGRESS;
            case "notched_6" -> BossEvent.BossBarOverlay.NOTCHED_6;
            case "notched_10" -> BossEvent.BossBarOverlay.NOTCHED_10;
            case "notched_12" -> BossEvent.BossBarOverlay.NOTCHED_12;
            case "notched_20" -> BossEvent.BossBarOverlay.NOTCHED_20;
            default -> BossEvent.BossBarOverlay.PROGRESS;
        };
    }
}
