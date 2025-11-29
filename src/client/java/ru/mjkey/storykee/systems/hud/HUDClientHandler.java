package ru.mjkey.storykee.systems.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.network.StoryNetworkManager;

import java.util.Map;

/**
 * Client-side handler for HUD operations.
 * Receives packets from the server to create, update, and remove HUD elements.
 * 
 * Requirements: 9.1, 9.2, 9.3
 */
@Environment(EnvType.CLIENT)
public class HUDClientHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HUDClientHandler.class);
    
    /**
     * Registers all HUD-related packet receivers.
     * Should be called during client initialization.
     */
    public static void register() {
        LOGGER.info("Registering HUD client packet handlers");
        
        // Register HUD show packet receiver
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.HUDShowPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handleHUDShow(payload));
            }
        );
        
        // Register HUD hide packet receiver
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.HUDHidePayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handleHUDHide(payload));
            }
        );
        
        // Register HUD update packet receiver
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.HUDUpdatePayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handleHUDUpdate(payload));
            }
        );
        
        // Register HUD clear packet receiver
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.HUDClearPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handleHUDClear(payload));
            }
        );
        
        LOGGER.info("HUD client handler registered");
    }
    
    /**
     * Handles HUD show payload from server.
     */
    private static void handleHUDShow(StoryNetworkManager.HUDShowPayload payload) {
        String elementType = payload.elementType();
        Map<String, String> props = payload.properties();
        
        switch (elementType.toLowerCase()) {
            case "text":
                handleCreateText(
                    payload.elementId(),
                    payload.x(),
                    payload.y(),
                    props.getOrDefault("text", ""),
                    parseColor(props.getOrDefault("color", "0xFFFFFFFF")),
                    parseFloat(props.getOrDefault("scale", "1.0")),
                    Boolean.parseBoolean(props.getOrDefault("shadow", "true"))
                );
                break;
            case "image":
                handleCreateImage(
                    payload.elementId(),
                    payload.x(),
                    payload.y(),
                    payload.width(),
                    payload.height(),
                    props.getOrDefault("texture", "")
                );
                break;
            case "progress":
                handleCreateProgressBar(
                    payload.elementId(),
                    payload.x(),
                    payload.y(),
                    payload.width(),
                    payload.height(),
                    parseFloat(props.getOrDefault("progress", "0.0")),
                    parseColor(props.getOrDefault("backgroundColor", "0xFF333333")),
                    parseColor(props.getOrDefault("foregroundColor", "0xFF00FF00"))
                );
                break;
            default:
                LOGGER.warn("Unknown HUD element type: {}", elementType);
        }
    }
    
    /**
     * Handles HUD hide payload from server.
     */
    private static void handleHUDHide(StoryNetworkManager.HUDHidePayload payload) {
        handleRemove(payload.elementId());
    }
    
    /**
     * Handles HUD update payload from server.
     */
    private static void handleHUDUpdate(StoryNetworkManager.HUDUpdatePayload payload) {
        String elementId = payload.elementId();
        Map<String, String> updates = payload.updates();
        
        HUDManager manager = HUDManager.getInstance();
        HUDElement element = manager.getElement(elementId);
        
        if (element == null) {
            LOGGER.warn("Cannot update HUD element '{}': not found", elementId);
            return;
        }
        
        // Update position if specified
        if (updates.containsKey("x") || updates.containsKey("y")) {
            int x = updates.containsKey("x") ? Integer.parseInt(updates.get("x")) : element.getX();
            int y = updates.containsKey("y") ? Integer.parseInt(updates.get("y")) : element.getY();
            element.setPosition(x, y);
        }
        
        // Update visibility if specified
        if (updates.containsKey("visible")) {
            element.setVisible(Boolean.parseBoolean(updates.get("visible")));
        }
        
        // Type-specific updates
        if (element instanceof TextHUDElement textElement) {
            if (updates.containsKey("text")) {
                textElement.setText(updates.get("text"));
            }
            if (updates.containsKey("color")) {
                textElement.setColor(parseColor(updates.get("color")));
            }
        } else if (element instanceof ProgressBarHUDElement progressElement) {
            if (updates.containsKey("progress")) {
                float progress = parseFloat(updates.get("progress"));
                boolean animated = Boolean.parseBoolean(updates.getOrDefault("animated", "false"));
                if (animated) {
                    progressElement.setAnimationSpeed(0.05f);
                    progressElement.setTargetProgress(progress);
                } else {
                    progressElement.setProgress(progress);
                }
            }
        }
        
        LOGGER.debug("Updated HUD element: {}", elementId);
    }
    
    /**
     * Handles HUD clear payload from server.
     */
    private static void handleHUDClear(StoryNetworkManager.HUDClearPayload payload) {
        String group = payload.group();
        if (group == null || group.isEmpty()) {
            handleClearAll();
        } else {
            handleClearGroup(group);
        }
    }
    
    /**
     * Parses a color string to int (supports hex format like "0xFFFFFFFF" or decimal).
     */
    private static int parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                return (int) Long.parseLong(colorStr.substring(2), 16);
            } else if (colorStr.startsWith("#")) {
                return (int) Long.parseLong(colorStr.substring(1), 16);
            }
            return Integer.parseInt(colorStr);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF; // Default white
        }
    }
    
    /**
     * Parses a float string safely.
     */
    private static float parseFloat(String floatStr) {
        try {
            return Float.parseFloat(floatStr);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }
    
    /**
     * Handles creating a text HUD element.
     * 
     * @param id Element ID
     * @param x X position
     * @param y Y position
     * @param text Text content
     * @param color ARGB color
     * @param scale Text scale
     * @param shadow Whether to show shadow
     */
    public static void handleCreateText(String id, int x, int y, String text, int color, float scale, boolean shadow) {
        HUDManager manager = HUDManager.getInstance();
        
        // Remove existing element with same ID if present
        if (manager.hasElement(id)) {
            manager.unregisterElement(id);
        }
        
        TextHUDElement element = new TextHUDElement(id, x, y, text, color, scale, shadow);
        manager.registerElement(element);
        
        LOGGER.debug("Created text HUD element: {}", id);
    }
    
    /**
     * Handles creating an image HUD element.
     * 
     * @param id Element ID
     * @param x X position
     * @param y Y position
     * @param width Display width
     * @param height Display height
     * @param texturePath Texture path
     */
    public static void handleCreateImage(String id, int x, int y, int width, int height, String texturePath) {
        HUDManager manager = HUDManager.getInstance();
        
        // Remove existing element with same ID if present
        if (manager.hasElement(id)) {
            manager.unregisterElement(id);
        }
        
        ImageHUDElement element = ImageHUDElement.fromPath(id, x, y, width, height, texturePath);
        manager.registerElement(element);
        
        LOGGER.debug("Created image HUD element: {}", id);
    }
    
    /**
     * Handles creating a progress bar HUD element.
     * 
     * @param id Element ID
     * @param x X position
     * @param y Y position
     * @param width Bar width
     * @param height Bar height
     * @param progress Initial progress (0.0 to 1.0)
     * @param backgroundColor Background color
     * @param foregroundColor Fill color
     */
    public static void handleCreateProgressBar(String id, int x, int y, int width, int height,
                                                float progress, int backgroundColor, int foregroundColor) {
        HUDManager manager = HUDManager.getInstance();
        
        // Remove existing element with same ID if present
        if (manager.hasElement(id)) {
            manager.unregisterElement(id);
        }
        
        ProgressBarHUDElement element = new ProgressBarHUDElement(id, x, y, width, height, 
                                                                   progress, backgroundColor, foregroundColor);
        manager.registerElement(element);
        
        LOGGER.debug("Created progress bar HUD element: {}", id);
    }
    
    /**
     * Handles updating a text HUD element.
     * 
     * @param id Element ID
     * @param text New text content (null to keep current)
     * @param color New color (-1 to keep current)
     */
    public static void handleUpdateText(String id, String text, int color) {
        HUDManager manager = HUDManager.getInstance();
        TextHUDElement element = manager.getElement(id, TextHUDElement.class);
        
        if (element == null) {
            LOGGER.warn("Cannot update text element '{}': not found", id);
            return;
        }
        
        if (text != null) {
            element.setText(text);
        }
        if (color != -1) {
            element.setColor(color);
        }
        
        LOGGER.debug("Updated text HUD element: {}", id);
    }
    
    /**
     * Handles updating a progress bar HUD element.
     * 
     * @param id Element ID
     * @param progress New progress value (negative to keep current)
     * @param animated Whether to animate the change
     */
    public static void handleUpdateProgress(String id, float progress, boolean animated) {
        HUDManager manager = HUDManager.getInstance();
        ProgressBarHUDElement element = manager.getElement(id, ProgressBarHUDElement.class);
        
        if (element == null) {
            LOGGER.warn("Cannot update progress element '{}': not found", id);
            return;
        }
        
        if (progress >= 0) {
            if (animated) {
                element.setAnimationSpeed(0.05f); // Smooth animation
                element.setTargetProgress(progress);
            } else {
                element.setProgress(progress);
            }
        }
        
        LOGGER.debug("Updated progress HUD element: {} to {}", id, progress);
    }
    
    /**
     * Handles updating element position.
     * 
     * @param id Element ID
     * @param x New X position
     * @param y New Y position
     */
    public static void handleUpdatePosition(String id, int x, int y) {
        HUDManager manager = HUDManager.getInstance();
        HUDElement element = manager.getElement(id);
        
        if (element == null) {
            LOGGER.warn("Cannot update position of element '{}': not found", id);
            return;
        }
        
        element.setPosition(x, y);
        LOGGER.debug("Updated position of HUD element: {} to ({}, {})", id, x, y);
    }
    
    /**
     * Handles removing a HUD element.
     * 
     * @param id Element ID to remove
     */
    public static void handleRemove(String id) {
        HUDManager manager = HUDManager.getInstance();
        HUDElement removed = manager.unregisterElement(id);
        
        if (removed != null) {
            LOGGER.debug("Removed HUD element: {}", id);
        } else {
            LOGGER.warn("Cannot remove element '{}': not found", id);
        }
    }
    
    /**
     * Handles showing or hiding a HUD element.
     * 
     * @param id Element ID
     * @param visible Whether to show (true) or hide (false)
     */
    public static void handleShowHide(String id, boolean visible) {
        HUDManager manager = HUDManager.getInstance();
        
        if (visible) {
            manager.showElement(id);
        } else {
            manager.hideElement(id);
        }
    }
    
    /**
     * Handles clearing all HUD elements.
     */
    public static void handleClearAll() {
        HUDManager.getInstance().clear();
        LOGGER.debug("Cleared all HUD elements");
    }
    
    /**
     * Handles clearing a group of HUD elements.
     * 
     * @param group Group name to clear
     */
    public static void handleClearGroup(String group) {
        HUDManager.getInstance().clearGroup(group);
        LOGGER.debug("Cleared HUD group: {}", group);
    }
}
