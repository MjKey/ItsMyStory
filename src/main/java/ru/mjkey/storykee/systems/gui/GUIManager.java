package ru.mjkey.storykee.systems.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Manages custom GUI screens for the Storykee system.
 * 
 * GUIManager handles:
 * - Registration of GUI factories
 * - Opening and closing GUIs per player
 * - Tracking active GUIs
 * - GUI lifecycle management
 * 
 * This is a server-side manager that coordinates GUI display across clients.
 * 
 * Requirements: 9.4, 9.5
 */
public class GUIManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GUIManager.class);
    
    private static GUIManager instance;
    
    // Registered GUI factories by ID
    private final Map<String, GUIFactory> guiFactories;
    
    // Currently open GUIs per player
    private final Map<UUID, String> activeGUIs;
    
    private GUIManager() {
        this.guiFactories = new ConcurrentHashMap<>();
        this.activeGUIs = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance of GUIManager.
     * 
     * @return The GUIManager instance
     */
    public static GUIManager getInstance() {
        if (instance == null) {
            instance = new GUIManager();
        }
        return instance;
    }
    
    /**
     * Registers a GUI factory.
     * 
     * @param guiId Unique GUI ResourceLocation
     * @param factory Factory function to create the GUI
     * @return true if registered successfully, false if ID already exists
     */
    public boolean registerGUI(String guiId, GUIFactory factory) {
        if (guiId == null || guiId.isEmpty()) {
            LOGGER.warn("Cannot register GUI with null or empty ID");
            return false;
        }
        
        if (factory == null) {
            LOGGER.warn("Cannot register GUI '{}' with null factory", guiId);
            return false;
        }
        
        if (guiFactories.containsKey(guiId)) {
            LOGGER.warn("GUI with ID '{}' is already registered", guiId);
            return false;
        }
        
        guiFactories.put(guiId, factory);
        LOGGER.debug("Registered GUI: {}", guiId);
        return true;
    }
    
    /**
     * Registers a GUI factory using a simple function.
     * 
     * @param guiId Unique GUI ResourceLocation
     * @param factoryFunction Function that creates the GUI given a player UUID
     * @return true if registered successfully
     */
    public boolean registerGUI(String guiId, Function<UUID, Object> factoryFunction) {
        return registerGUI(guiId, new GUIFactory() {
            @Override
            public Object create(UUID playerId) {
                return factoryFunction.apply(playerId);
            }
        });
    }
    
    /**
     * Unregisters a GUI factory.
     * 
     * @param guiId GUI ResourceLocation
     * @return true if unregistered, false if not found
     */
    public boolean unregisterGUI(String guiId) {
        GUIFactory removed = guiFactories.remove(guiId);
        
        if (removed != null) {
            // Close any active instances of this GUI
            activeGUIs.entrySet().removeIf(entry -> {
                if (entry.getValue().equals(guiId)) {
                    LOGGER.debug("Closing active GUI '{}' for player {} due to unregistration", 
                                guiId, entry.getKey());
                    return true;
                }
                return false;
            });
            
            LOGGER.debug("Unregistered GUI: {}", guiId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a GUI is registered.
     * 
     * @param guiId GUI ResourceLocation
     * @return true if registered
     */
    public boolean isRegistered(String guiId) {
        return guiFactories.containsKey(guiId);
    }
    
    /**
     * Opens a GUI for a player.
     * This method should be called from the server side.
     * 
     * @param playerId The player UUID to open the GUI for
     * @param guiId The GUI ResourceLocation
     * @return true if the GUI was opened successfully
     */
    public boolean openGUI(UUID playerId, String guiId) {
        if (playerId == null) {
            LOGGER.warn("Cannot open GUI for null player");
            return false;
        }
        
        if (!guiFactories.containsKey(guiId)) {
            LOGGER.warn("Cannot open unregistered GUI '{}' for player {}", guiId, playerId);
            return false;
        }
        
        // Close any currently open GUI for this player
        if (activeGUIs.containsKey(playerId)) {
            String currentGUI = activeGUIs.get(playerId);
            LOGGER.debug("Closing current GUI '{}' before opening '{}'", currentGUI, guiId);
        }
        
        // Mark this GUI as active for the player
        activeGUIs.put(playerId, guiId);
        
        // Send packet to client to open the GUI
        // This will be handled by the client-side GUI system
        sendOpenGUIPacket(playerId, guiId);
        
        LOGGER.debug("Opened GUI '{}' for player {}", guiId, playerId);
        return true;
    }
    
    /**
     * Closes the currently open GUI for a player.
     * 
     * @param playerId The player UUID
     * @return true if a GUI was closed
     */
    public boolean closeGUI(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        
        String guiId = activeGUIs.remove(playerId);
        
        if (guiId != null) {
            // Send packet to client to close the GUI
            sendCloseGUIPacket(playerId);
            
            LOGGER.debug("Closed GUI '{}' for player {}", guiId, playerId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets the currently open GUI ID for a player.
     * 
     * @param playerId Player UUID
     * @return The GUI ID, or null if no GUI is open
     */
    public String getCurrentGUI(UUID playerId) {
        return activeGUIs.get(playerId);
    }
    
    /**
     * Checks if a player has a GUI open.
     * 
     * @param playerId Player UUID
     * @return true if the player has a GUI open
     */
    public boolean hasGUIOpen(UUID playerId) {
        return activeGUIs.containsKey(playerId);
    }
    
    /**
     * Checks if a specific GUI is open for a player.
     * 
     * @param playerId Player UUID
     * @param guiId GUI ResourceLocation
     * @return true if the specified GUI is open for the player
     */
    public boolean isGUIOpen(UUID playerId, String guiId) {
        String currentGUI = activeGUIs.get(playerId);
        return currentGUI != null && currentGUI.equals(guiId);
    }
    
    /**
     * Creates a GUI instance for a player.
     * This is typically called on the client side.
     * 
     * @param guiId GUI ResourceLocation
     * @param playerId Player UUID
     * @return The created GUI, or null if the GUI is not registered
     */
    public Object createGUI(String guiId, UUID playerId) {
        GUIFactory factory = guiFactories.get(guiId);
        
        if (factory == null) {
            LOGGER.warn("Cannot create unregistered GUI '{}'", guiId);
            return null;
        }
        
        try {
            Object gui = factory.create(playerId);
            LOGGER.debug("Created GUI '{}' for player {}", guiId, playerId);
            return gui;
        } catch (Exception e) {
            LOGGER.error("Error creating GUI '{}' for player {}", guiId, playerId, e);
            return null;
        }
    }
    
    /**
     * Clears all active GUIs.
     * Useful for cleanup on server stop.
     */
    public void clearAll() {
        activeGUIs.clear();
        LOGGER.debug("Cleared all active GUIs");
    }
    
    /**
     * Clears all registered GUI factories.
     * Useful for hot reload scenarios.
     */
    public void clearRegistrations() {
        guiFactories.clear();
        activeGUIs.clear();
        LOGGER.debug("Cleared all GUI registrations");
    }
    
    /**
     * Gets the number of registered GUIs.
     * 
     * @return Registration count
     */
    public int getRegistrationCount() {
        return guiFactories.size();
    }
    
    /**
     * Gets the number of active GUIs.
     * 
     * @return Active GUI count
     */
    public int getActiveGUICount() {
        return activeGUIs.size();
    }
    
    /**
     * Sends a packet to the client to open a GUI.
     * Uses Fabric networking to send the packet.
     * 
     * @param playerId The player UUID
     * @param guiId The GUI ResourceLocation
     */
    private void sendOpenGUIPacket(UUID playerId, String guiId) {
        net.minecraft.server.level.ServerPlayer player = getServerPlayer(playerId);
        if (player != null) {
            ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                .sendGUIOpen(player, guiId, java.util.Collections.emptyMap());
            LOGGER.debug("Sent open GUI packet for '{}' to player {}", guiId, playerId);
        } else {
            LOGGER.warn("Cannot send open GUI packet - player {} not found", playerId);
        }
    }
    
    /**
     * Sends a packet to the client to close the current GUI.
     * Uses Fabric networking to send the packet.
     * 
     * @param playerId The player UUID
     */
    private void sendCloseGUIPacket(UUID playerId) {
        net.minecraft.server.level.ServerPlayer player = getServerPlayer(playerId);
        if (player != null) {
            ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                .sendGUIClose(player);
            LOGGER.debug("Sent close GUI packet to player {}", playerId);
        } else {
            LOGGER.warn("Cannot send close GUI packet - player {} not found", playerId);
        }
    }
    
    /**
     * Gets a ServerPlayer from UUID.
     */
    private net.minecraft.server.level.ServerPlayer getServerPlayer(UUID playerId) {
        net.minecraft.server.level.ServerLevel world = 
            ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
        if (world != null && world.getServer() != null) {
            return world.getServer().getPlayerList().getPlayer(playerId);
        }
        return null;
    }
    
    /**
     * Factory interface for creating GUI instances.
     */
    @FunctionalInterface
    public interface GUIFactory {
        /**
         * Creates a GUI instance for a player.
         * 
         * @param playerId Player UUID
         * @return The created GUI (client-side StoryGUI object)
         */
        Object create(UUID playerId);
    }
}
