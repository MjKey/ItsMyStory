package ru.mjkey.storykee.systems.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Renders HUD elements using Fabric's HudRenderCallback.
 * Integrates with Minecraft's rendering pipeline to display custom HUD elements.
 * 
 * Requirements: 9.1, 9.2, 9.3
 */
@Environment(EnvType.CLIENT)
public class HUDRenderer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HUDRenderer.class);
    
    private static HUDRenderer instance;
    private static boolean registered = false;
    
    private final HUDManager hudManager;
    private boolean enabled;
    private boolean debugMode;
    
    private HUDRenderer() {
        this.hudManager = HUDManager.getInstance();
        this.enabled = true;
        this.debugMode = false;
    }
    
    /**
     * Gets the singleton instance of HUDRenderer.
     * 
     * @return The HUDRenderer instance
     */
    public static HUDRenderer getInstance() {
        if (instance == null) {
            instance = new HUDRenderer();
        }
        return instance;
    }
    
    /**
     * Registers the HUD renderer with Fabric's rendering system.
     * Should be called during client initialization.
     */
    public static void register() {
        if (registered) {
            LOGGER.warn("HUDRenderer already registered");
            return;
        }
        
        HUDRenderer renderer = getInstance();
        
        // Register with Fabric's HUD render callback
        // Note: HudRenderCallback is deprecated but still functional in Fabric 1.21+
        @SuppressWarnings("deprecation")
        var callback = HudRenderCallback.EVENT;
        callback.register((graphics, tickCounter) -> {
            renderer.render(graphics, tickCounter.getGameTimeDeltaPartialTick(true));
        });
        
        registered = true;
        LOGGER.info("HUDRenderer registered with Fabric rendering system");
    }
    
    /**
     * Main render method called every frame.
     * 
     * @param graphics The draw context for rendering
     * @param tickDelta Partial tick for smooth animations
     */
    public void render(GuiGraphics graphics, float tickDelta) {
        if (!enabled) {
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        
        // Don't render if no player or in certain screens
        if (client.player == null) {
            return;
        }
        
        // Don't render HUD when debug screen (F3) is open
        if (client.getDebugOverlay().showDebugScreen()) {
            return;
        }
        
        try {
            // Update all elements
            hudManager.updateAll(tickDelta);
            
            // Get elements in render order (sorted by z-index)
            List<HUDElement> elements = hudManager.getElementsInRenderOrder();
            
            // Render each visible element
            for (HUDElement element : elements) {
                if (element.isVisible()) {
                    renderElement(graphics, element, tickDelta);
                }
            }
            
            // Render debug info if enabled
            if (debugMode) {
                renderDebugInfo(graphics);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error rendering HUD elements", e);
        }
    }
    
    /**
     * Renders a single HUD element with error handling.
     * 
     * @param graphics The draw context
     * @param element The element to render
     * @param tickDelta Partial tick
     */
    private void renderElement(GuiGraphics graphics, HUDElement element, float tickDelta) {
        try {
            element.render(graphics, tickDelta);
        } catch (Exception e) {
            LOGGER.error("Error rendering HUD element '{}': {}", element.getId(), e.getMessage());
            if (debugMode) {
                LOGGER.error("Stack trace:", e);
            }
        }
    }
    
    /**
     * Renders debug information about HUD elements.
     * 
     * @param graphics The draw context
     */
    private void renderDebugInfo(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) {
            return;
        }
        
        int screenWidth = client.getWindow().getGuiScaledWidth();
        
        String debugText = String.format("HUD Elements: %d visible / %d total",
            hudManager.getVisibleElementCount(), hudManager.getElementCount());
        
        int textWidth = client.font.width(debugText);
        int x = screenWidth - textWidth - 5;
        int y = 5;
        
        // Background
        graphics.fill(x - 2, y - 2, x + textWidth + 2, y + client.font.lineHeight + 2, 0x80000000);
        
        // Text
        graphics.drawString(client.font, debugText, x, y, 0xFFFFFF00, false);
    }
    
    /**
     * Checks if the renderer is enabled.
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Enables or disables the HUD renderer.
     * 
     * @param enabled Whether to enable rendering
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.debug("HUD rendering {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Toggles the HUD renderer on/off.
     * 
     * @return The new enabled state
     */
    public boolean toggle() {
        enabled = !enabled;
        LOGGER.debug("HUD rendering toggled to {}", enabled);
        return enabled;
    }
    
    /**
     * Checks if debug mode is enabled.
     * 
     * @return true if debug mode is on
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Enables or disables debug mode.
     * 
     * @param debugMode Whether to enable debug mode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
    
    /**
     * Gets the HUD manager used by this renderer.
     * 
     * @return The HUDManager instance
     */
    public HUDManager getHudManager() {
        return hudManager;
    }
    
    /**
     * Checks if the renderer has been registered.
     * 
     * @return true if registered
     */
    public static boolean isRegistered() {
        return registered;
    }
}
