package ru.mjkey.storykee.systems.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.network.StoryNetworkManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Client-side handler for GUI operations.
 * Receives packets from the server to open and close GUIs.
 * 
 * Requirements: 9.4, 9.5
 */
@Environment(EnvType.CLIENT)
public class GUIClientHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GUIClientHandler.class);
    
    // Registered GUI factories on client side
    private static final Map<String, Function<Map<String, String>, StoryGUI>> guiFactories = new ConcurrentHashMap<>();
    
    // Currently open GUI ID
    private static String currentGUIId = null;
    
    /**
     * Registers all GUI-related packet receivers.
     * Should be called during client initialization.
     */
    public static void register() {
        LOGGER.info("Registering GUI client packet handlers");
        
        // Register GUI open packet receiver
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.GUIOpenPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handleGUIOpen(payload));
            }
        );
        
        // Register GUI close packet receiver
        ClientPlayNetworking.registerGlobalReceiver(
            StoryNetworkManager.GUIClosePayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> handleGUIClose());
            }
        );
        
        LOGGER.info("GUI client handler registered");
    }
    
    /**
     * Registers a GUI factory on the client side.
     * 
     * @param guiId Unique GUI ID
     * @param factory Factory function that creates the GUI given data from server
     */
    public static void registerGUI(String guiId, Function<Map<String, String>, StoryGUI> factory) {
        if (guiId == null || guiId.isEmpty()) {
            LOGGER.warn("Cannot register GUI with null or empty ID");
            return;
        }
        
        guiFactories.put(guiId, factory);
        LOGGER.debug("Registered client GUI factory: {}", guiId);
    }
    
    /**
     * Unregisters a GUI factory.
     * 
     * @param guiId GUI ID to unregister
     */
    public static void unregisterGUI(String guiId) {
        guiFactories.remove(guiId);
        LOGGER.debug("Unregistered client GUI factory: {}", guiId);
    }
    
    /**
     * Checks if a GUI factory is registered.
     * 
     * @param guiId GUI ID to check
     * @return true if registered
     */
    public static boolean isRegistered(String guiId) {
        return guiFactories.containsKey(guiId);
    }
    
    /**
     * Gets the currently open GUI ID.
     * 
     * @return Current GUI ID, or null if no GUI is open
     */
    public static String getCurrentGUIId() {
        return currentGUIId;
    }
    
    /**
     * Handles GUI open payload from server.
     */
    private static void handleGUIOpen(StoryNetworkManager.GUIOpenPayload payload) {
        String guiId = payload.guiId();
        Map<String, String> data = payload.data();
        
        LOGGER.debug("Received GUI open request for: {}", guiId);
        
        Function<Map<String, String>, StoryGUI> factory = guiFactories.get(guiId);
        
        if (factory == null) {
            LOGGER.warn("Cannot open unregistered GUI: {}", guiId);
            return;
        }
        
        try {
            StoryGUI gui = factory.apply(data);
            
            if (gui != null) {
                Minecraft client = Minecraft.getInstance();
                client.setScreen(gui);
                currentGUIId = guiId;
                LOGGER.info("Opened GUI: {}", guiId);
            } else {
                LOGGER.warn("GUI factory returned null for: {}", guiId);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating GUI '{}': {}", guiId, e.getMessage(), e);
        }
    }
    
    /**
     * Handles GUI close payload from server.
     */
    private static void handleGUIClose() {
        LOGGER.debug("Received GUI close request");
        
        Minecraft client = Minecraft.getInstance();
        
        // Only close if we have a story GUI open
        if (client.screen instanceof StoryGUI) {
            client.setScreen(null);
            LOGGER.info("Closed GUI: {}", currentGUIId);
            currentGUIId = null;
        }
    }
    
    /**
     * Closes the current GUI programmatically (client-side).
     */
    public static void closeCurrentGUI() {
        Minecraft client = Minecraft.getInstance();
        
        if (client.screen instanceof StoryGUI) {
            client.setScreen(null);
            currentGUIId = null;
        }
    }
    
    /**
     * Clears all registered GUI factories.
     */
    public static void clearRegistrations() {
        guiFactories.clear();
        LOGGER.debug("Cleared all client GUI registrations");
    }
}
