package ru.mjkey.storykee.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for story multiplayer synchronization.
 * Handles sending story events and state to clients.
 * 
 * Requirements: 31.1, 31.2, 31.3, 31.4, 31.5
 */
public class StoryNetworkManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryNetworkManager.class);
    
    // Packet ResourceLocations
    public static final ResourceLocation STORY_EVENT_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "story_event");
    public static final ResourceLocation STORY_STATE_SYNC_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "story_state_sync");
    public static final ResourceLocation STORY_INTERACTION_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "story_interaction");
    public static final ResourceLocation STORY_FULL_STATE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "story_full_state");
    
    // HUD Packet ResourceLocations (Requirements: 9.1, 9.2, 9.3)
    public static final ResourceLocation HUD_SHOW_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "hud_show");
    public static final ResourceLocation HUD_HIDE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "hud_hide");
    public static final ResourceLocation HUD_UPDATE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "hud_update");
    public static final ResourceLocation HUD_CLEAR_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "hud_clear");
    
    // GUI Packet ResourceLocations (Requirements: 9.4, 9.5)
    public static final ResourceLocation GUI_OPEN_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "gui_open");
    public static final ResourceLocation GUI_CLOSE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "gui_close");
    
    // NPC Animation Packet ResourceLocations (Requirements: 10.2)
    public static final ResourceLocation NPC_ANIMATION_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "npc_animation");
    public static final ResourceLocation NPC_ANIMATION_STOP_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "npc_animation_stop");
    
    private static StoryNetworkManager instance;
    
    private MinecraftServer server;
    
    // Track story state for synchronization
    private final Map<String, Map<String, Object>> storyStates;
    
    // Track which players are in which story contexts
    private final Map<UUID, Set<String>> playerStoryContexts;
    
    private StoryNetworkManager() {
        this.storyStates = new ConcurrentHashMap<>();
        this.playerStoryContexts = new ConcurrentHashMap<>();
    }
    
    public static StoryNetworkManager getInstance() {
        if (instance == null) {
            instance = new StoryNetworkManager();
        }
        return instance;
    }

    /**
     * Registers all network packet types and handlers.
     * Should be called during mod initialization.
     */
    public void register() {
        LOGGER.info("Registering story network packets");
        
        // Register payload types for server-to-client packets
        PayloadTypeRegistry.playS2C().register(StoryEventPayload.TYPE, StoryEventPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StoryStateSyncPayload.TYPE, StoryStateSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StoryFullStatePayload.TYPE, StoryFullStatePayload.CODEC);
        
        // Register HUD packet types (Requirements: 9.1, 9.2, 9.3)
        PayloadTypeRegistry.playS2C().register(HUDShowPayload.TYPE, HUDShowPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HUDHidePayload.TYPE, HUDHidePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HUDUpdatePayload.TYPE, HUDUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HUDClearPayload.TYPE, HUDClearPayload.CODEC);
        
        // Register GUI packet types (Requirements: 9.4, 9.5)
        PayloadTypeRegistry.playS2C().register(GUIOpenPayload.TYPE, GUIOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GUIClosePayload.TYPE, GUIClosePayload.CODEC);
        
        // Register NPC Animation packet types (Requirements: 10.2)
        PayloadTypeRegistry.playS2C().register(NPCAnimationPayload.TYPE, NPCAnimationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(NPCAnimationStopPayload.TYPE, NPCAnimationStopPayload.CODEC);
        
        // Register payload types for client-to-server packets
        PayloadTypeRegistry.playC2S().register(StoryInteractionPayload.TYPE, StoryInteractionPayload.CODEC);
        
        // Register receiver for client interactions
        ServerPlayNetworking.registerGlobalReceiver(StoryInteractionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleInteraction(player, payload));
        });
        
        // Register player join handler for state synchronization
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            LOGGER.debug("Player {} joined, sending story state", player.getName().getString());
            sendFullStateToPlayer(player);
        });
        
        // Register player disconnect handler for cleanup
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.getPlayer().getUUID();
            playerStoryContexts.remove(playerId);
            LOGGER.debug("Player {} disconnected, cleaned up story contexts", handler.getPlayer().getName().getString());
        });
    }
    
    /**
     * Initializes the network manager with the server instance.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("Story network manager initialized");
    }
    
    /**
     * Shuts down the network manager.
     */
    public void shutdown() {
        storyStates.clear();
        playerStoryContexts.clear();
        server = null;
        LOGGER.info("Story network manager shut down");
    }
    
    // ===== Event Broadcasting =====
    
    /**
     * Broadcasts a story event to all relevant clients.
     * 
     * Requirements: 31.1
     * 
     * @param eventType The type of story event
     * @param eventId Unique identifier for this event
     * @param data Event data as key-value pairs
     */
    public void broadcastEvent(StoryEventType eventType, String eventId, Map<String, String> data) {
        if (server == null) {
            LOGGER.warn("Cannot broadcast event - server not initialized");
            return;
        }
        
        StoryEventPayload payload = new StoryEventPayload(eventType.name(), eventId, data);
        
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
        
        LOGGER.debug("Broadcast event {} to all players", eventType);
    }
    
    /**
     * Broadcasts a story event to players near a specific location.
     * 
     * @param eventType The type of story event
     * @param eventId Unique identifier for this event
     * @param data Event data
     * @param position Center position
     * @param radius Radius in blocks
     */
    public void broadcastEventNearby(StoryEventType eventType, String eventId, 
                                      Map<String, String> data, Vec3 position, double radius) {
        if (server == null) {
            return;
        }
        
        StoryEventPayload payload = new StoryEventPayload(eventType.name(), eventId, data);
        
        for (ServerPlayer player : PlayerLookup.all(server)) {
            if (player.position().distanceTo(position) <= radius) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
    
    /**
     * Sends a story event to a specific player.
     * 
     * @param player The target player
     * @param eventType The type of story event
     * @param eventId Unique identifier for this event
     * @param data Event data
     */
    public void sendEventToPlayer(ServerPlayer player, StoryEventType eventType, 
                                   String eventId, Map<String, String> data) {
        StoryEventPayload payload = new StoryEventPayload(eventType.name(), eventId, data);
        ServerPlayNetworking.send(player, payload);
    }
    
    /**
     * Broadcasts a player interaction to other players.
     * 
     * Requirements: 31.2
     * 
     * @param sourcePlayer The player who performed the interaction
     * @param interactionType Type of interaction
     * @param targetId ID of the target (NPC, object, etc.)
     * @param data Additional interaction data
     */
    public void broadcastInteraction(ServerPlayer sourcePlayer, String interactionType,
                                      String targetId, Map<String, String> data) {
        if (server == null) {
            return;
        }
        
        Map<String, String> eventData = new HashMap<>(data);
        eventData.put("sourcePlayer", sourcePlayer.getUUID().toString());
        eventData.put("sourcePlayerName", sourcePlayer.getName().getString());
        eventData.put("interactionType", interactionType);
        eventData.put("targetId", targetId);
        
        StoryEventPayload payload = new StoryEventPayload(
            StoryEventType.CUSTOM_EVENT.name(),
            "interaction_" + System.currentTimeMillis(),
            eventData
        );
        
        // Send to all players except the source
        for (ServerPlayer player : PlayerLookup.all(server)) {
            if (!player.getUUID().equals(sourcePlayer.getUUID())) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    // ===== State Synchronization =====
    
    /**
     * Updates and synchronizes story state.
     * 
     * Requirements: 31.3
     * 
     * @param storyId The story identifier
     * @param key State key
     * @param value State value (will be converted to string)
     */
    public void updateState(String storyId, String key, Object value) {
        storyStates.computeIfAbsent(storyId, k -> new ConcurrentHashMap<>())
                   .put(key, value);
        
        // Broadcast state update to all players
        if (server != null) {
            Map<String, String> stateData = new HashMap<>();
            stateData.put(key, value != null ? value.toString() : "");
            
            StoryStateSyncPayload payload = new StoryStateSyncPayload(storyId, stateData);
            
            for (ServerPlayer player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
    
    /**
     * Updates multiple state values at once.
     * 
     * @param storyId The story identifier
     * @param updates Map of key-value updates
     */
    public void updateStateBatch(String storyId, Map<String, Object> updates) {
        Map<String, Object> storyState = storyStates.computeIfAbsent(storyId, k -> new ConcurrentHashMap<>());
        storyState.putAll(updates);
        
        // Convert to string map for network transmission
        Map<String, String> stateData = new HashMap<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            stateData.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
        }
        
        if (server != null) {
            StoryStateSyncPayload payload = new StoryStateSyncPayload(storyId, stateData);
            
            for (ServerPlayer player : PlayerLookup.all(server)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
    
    /**
     * Gets the current state value for a story.
     * 
     * @param storyId The story identifier
     * @param key State key
     * @return The state value, or null if not set
     */
    public Object getState(String storyId, String key) {
        Map<String, Object> storyState = storyStates.get(storyId);
        return storyState != null ? storyState.get(key) : null;
    }
    
    /**
     * Gets all state for a story.
     * 
     * @param storyId The story identifier
     * @return Copy of the state map
     */
    public Map<String, Object> getAllState(String storyId) {
        Map<String, Object> storyState = storyStates.get(storyId);
        return storyState != null ? new HashMap<>(storyState) : new HashMap<>();
    }
    
    /**
     * Sends full story state to a player who just joined.
     * 
     * Requirements: 31.4
     * 
     * @param player The player to send state to
     */
    public void sendFullStateToPlayer(ServerPlayer player) {
        // Collect all story states
        Map<String, Map<String, String>> allStates = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : storyStates.entrySet()) {
            Map<String, String> stringState = new HashMap<>();
            for (Map.Entry<String, Object> stateEntry : entry.getValue().entrySet()) {
                stringState.put(stateEntry.getKey(), 
                    stateEntry.getValue() != null ? stateEntry.getValue().toString() : "");
            }
            allStates.put(entry.getKey(), stringState);
        }
        
        StoryFullStatePayload payload = new StoryFullStatePayload(allStates);
        ServerPlayNetworking.send(player, payload);
        
        LOGGER.debug("Sent full story state to player {}", player.getName().getString());
    }
    
    /**
     * Clears state for a specific story.
     * 
     * @param storyId The story identifier
     */
    public void clearState(String storyId) {
        storyStates.remove(storyId);
    }
    
    /**
     * Clears all story states.
     */
    public void clearAllStates() {
        storyStates.clear();
    }
    
    // ===== Player Context Management =====
    
    /**
     * Adds a player to a story context.
     * 
     * @param playerId The player's UUID
     * @param storyId The story identifier
     */
    public void addPlayerToContext(UUID playerId, String storyId) {
        playerStoryContexts.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                           .add(storyId);
    }
    
    /**
     * Removes a player from a story context.
     * 
     * @param playerId The player's UUID
     * @param storyId The story identifier
     */
    public void removePlayerFromContext(UUID playerId, String storyId) {
        Set<String> contexts = playerStoryContexts.get(playerId);
        if (contexts != null) {
            contexts.remove(storyId);
        }
    }
    
    /**
     * Gets all story contexts a player is in.
     * 
     * @param playerId The player's UUID
     * @return Set of story IDs
     */
    public Set<String> getPlayerContexts(UUID playerId) {
        Set<String> contexts = playerStoryContexts.get(playerId);
        return contexts != null ? new HashSet<>(contexts) : new HashSet<>();
    }
    
    // ===== Interaction Handling =====
    
    /**
     * Handles an interaction received from a client.
     */
    private void handleInteraction(ServerPlayer player, StoryInteractionPayload payload) {
        LOGGER.debug("Received interaction from {}: type={}, target={}", 
            player.getName().getString(), payload.interactionType(), payload.targetId());
        
        // Handle start_story interaction
        if ("start_story".equals(payload.interactionType())) {
            LOGGER.info("Starting story for player: {}", player.getName().getString());
            ru.mjkey.storykee.runtime.StorykeeRuntime.getInstance().executeForPlayer(player);
            return;
        }
        
        // Broadcast the interaction to other players
        broadcastInteraction(player, payload.interactionType(), payload.targetId(), payload.data());
    }

    // ===== Payload Classes =====
    
    /**
     * Payload for story events (server -> client).
     * Uses efficient serialization with string maps.
     * 
     * Requirements: 31.1, 31.5
     */
    public record StoryEventPayload(
        String eventType,
        String eventId,
        Map<String, String> data
    ) implements CustomPacketPayload {
        
        public static final Type<StoryEventPayload> TYPE = new Type<>(STORY_EVENT_ID);
        
        public static final StreamCodec<FriendlyByteBuf, StoryEventPayload> CODEC = 
            StreamCodec.of(StoryEventPayload::write, StoryEventPayload::read);
        
        public static StoryEventPayload read(FriendlyByteBuf buf) {
            String eventType = buf.readUtf();
            String eventId = buf.readUtf();
            
            int dataSize = buf.readVarInt();
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < dataSize; i++) {
                String key = buf.readUtf();
                String value = buf.readUtf();
                data.put(key, value);
            }
            
            return new StoryEventPayload(eventType, eventId, data);
        }
        
        public static void write(FriendlyByteBuf buf, StoryEventPayload payload) {
            buf.writeUtf(payload.eventType != null ? payload.eventType : "");
            buf.writeUtf(payload.eventId != null ? payload.eventId : "");
            
            Map<String, String> data = payload.data != null ? payload.data : Collections.emptyMap();
            buf.writeVarInt(data.size());
            for (Map.Entry<String, String> entry : data.entrySet()) {
                buf.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buf.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for state synchronization (server -> client).
     * 
     * Requirements: 31.3, 31.5
     */
    public record StoryStateSyncPayload(
        String storyId,
        Map<String, String> stateUpdates
    ) implements CustomPacketPayload {
        
        public static final Type<StoryStateSyncPayload> TYPE = new Type<>(STORY_STATE_SYNC_ID);
        
        public static final StreamCodec<FriendlyByteBuf, StoryStateSyncPayload> CODEC = 
            StreamCodec.of(StoryStateSyncPayload::write, StoryStateSyncPayload::read);
        
        public static StoryStateSyncPayload read(FriendlyByteBuf buf) {
            String storyId = buf.readUtf();
            
            int stateSize = buf.readVarInt();
            Map<String, String> stateUpdates = new HashMap<>();
            for (int i = 0; i < stateSize; i++) {
                String key = buf.readUtf();
                String value = buf.readUtf();
                stateUpdates.put(key, value);
            }
            
            return new StoryStateSyncPayload(storyId, stateUpdates);
        }
        
        public static void write(FriendlyByteBuf buf, StoryStateSyncPayload payload) {
            buf.writeUtf(payload.storyId != null ? payload.storyId : "");
            
            Map<String, String> updates = payload.stateUpdates != null ? payload.stateUpdates : Collections.emptyMap();
            buf.writeVarInt(updates.size());
            for (Map.Entry<String, String> entry : updates.entrySet()) {
                buf.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buf.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for full state synchronization on player join (server -> client).
     * 
     * Requirements: 31.4, 31.5
     */
    public record StoryFullStatePayload(
        Map<String, Map<String, String>> allStates
    ) implements CustomPacketPayload {
        
        public static final Type<StoryFullStatePayload> TYPE = new Type<>(STORY_FULL_STATE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, StoryFullStatePayload> CODEC = 
            StreamCodec.of(StoryFullStatePayload::write, StoryFullStatePayload::read);
        
        public static StoryFullStatePayload read(FriendlyByteBuf buf) {
            int storyCount = buf.readVarInt();
            Map<String, Map<String, String>> allStates = new HashMap<>();
            
            for (int i = 0; i < storyCount; i++) {
                String storyId = buf.readUtf();
                int stateSize = buf.readVarInt();
                Map<String, String> state = new HashMap<>();
                
                for (int j = 0; j < stateSize; j++) {
                    String key = buf.readUtf();
                    String value = buf.readUtf();
                    state.put(key, value);
                }
                
                allStates.put(storyId, state);
            }
            
            return new StoryFullStatePayload(allStates);
        }
        
        public static void write(FriendlyByteBuf buf, StoryFullStatePayload payload) {
            Map<String, Map<String, String>> states = payload.allStates != null ? payload.allStates : Collections.emptyMap();
            buf.writeVarInt(states.size());
            
            for (Map.Entry<String, Map<String, String>> storyEntry : states.entrySet()) {
                buf.writeUtf(storyEntry.getKey() != null ? storyEntry.getKey() : "");
                
                Map<String, String> state = storyEntry.getValue() != null ? storyEntry.getValue() : Collections.emptyMap();
                buf.writeVarInt(state.size());
                
                for (Map.Entry<String, String> stateEntry : state.entrySet()) {
                    buf.writeUtf(stateEntry.getKey() != null ? stateEntry.getKey() : "");
                    buf.writeUtf(stateEntry.getValue() != null ? stateEntry.getValue() : "");
                }
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for player interactions (client -> server).
     * 
     * Requirements: 31.2, 31.5
     */
    public record StoryInteractionPayload(
        String interactionType,
        String targetId,
        Map<String, String> data
    ) implements CustomPacketPayload {
        
        public static final Type<StoryInteractionPayload> TYPE = new Type<>(STORY_INTERACTION_ID);
        
        public static final StreamCodec<FriendlyByteBuf, StoryInteractionPayload> CODEC = 
            StreamCodec.of(StoryInteractionPayload::write, StoryInteractionPayload::read);
        
        public static StoryInteractionPayload read(FriendlyByteBuf buf) {
            String interactionType = buf.readUtf();
            String targetId = buf.readUtf();
            
            int dataSize = buf.readVarInt();
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < dataSize; i++) {
                String key = buf.readUtf();
                String value = buf.readUtf();
                data.put(key, value);
            }
            
            return new StoryInteractionPayload(interactionType, targetId, data);
        }
        
        public static void write(FriendlyByteBuf buf, StoryInteractionPayload payload) {
            buf.writeUtf(payload.interactionType != null ? payload.interactionType : "");
            buf.writeUtf(payload.targetId != null ? payload.targetId : "");
            
            Map<String, String> data = payload.data != null ? payload.data : Collections.emptyMap();
            buf.writeVarInt(data.size());
            for (Map.Entry<String, String> entry : data.entrySet()) {
                buf.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buf.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ===== HUD Payload Classes (Requirements: 9.1, 9.2, 9.3) =====
    
    /**
     * Payload for showing/creating a HUD element (server -> client).
     * Supports text, image, and progress bar elements.
     */
    public record HUDShowPayload(
        String elementId,
        String elementType,  // "text", "image", "progress"
        int x,
        int y,
        int width,
        int height,
        Map<String, String> properties
    ) implements CustomPacketPayload {
        
        public static final Type<HUDShowPayload> TYPE = new Type<>(HUD_SHOW_ID);
        
        public static final StreamCodec<FriendlyByteBuf, HUDShowPayload> CODEC = 
            StreamCodec.of(HUDShowPayload::write, HUDShowPayload::read);
        
        public static HUDShowPayload read(FriendlyByteBuf buf) {
            String elementId = buf.readUtf();
            String elementType = buf.readUtf();
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            int width = buf.readVarInt();
            int height = buf.readVarInt();
            
            int propsSize = buf.readVarInt();
            Map<String, String> properties = new HashMap<>();
            for (int i = 0; i < propsSize; i++) {
                properties.put(buf.readUtf(), buf.readUtf());
            }
            
            return new HUDShowPayload(elementId, elementType, x, y, width, height, properties);
        }
        
        public static void write(FriendlyByteBuf buf, HUDShowPayload payload) {
            buf.writeUtf(payload.elementId != null ? payload.elementId : "");
            buf.writeUtf(payload.elementType != null ? payload.elementType : "text");
            buf.writeVarInt(payload.x);
            buf.writeVarInt(payload.y);
            buf.writeVarInt(payload.width);
            buf.writeVarInt(payload.height);
            
            Map<String, String> props = payload.properties != null ? payload.properties : Collections.emptyMap();
            buf.writeVarInt(props.size());
            for (Map.Entry<String, String> entry : props.entrySet()) {
                buf.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buf.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for hiding a HUD element (server -> client).
     */
    public record HUDHidePayload(
        String elementId
    ) implements CustomPacketPayload {
        
        public static final Type<HUDHidePayload> TYPE = new Type<>(HUD_HIDE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, HUDHidePayload> CODEC = 
            StreamCodec.of(HUDHidePayload::write, HUDHidePayload::read);
        
        public static HUDHidePayload read(FriendlyByteBuf buf) {
            return new HUDHidePayload(buf.readUtf());
        }
        
        public static void write(FriendlyByteBuf buf, HUDHidePayload payload) {
            buf.writeUtf(payload.elementId != null ? payload.elementId : "");
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for updating a HUD element (server -> client).
     */
    public record HUDUpdatePayload(
        String elementId,
        Map<String, String> updates
    ) implements CustomPacketPayload {
        
        public static final Type<HUDUpdatePayload> TYPE = new Type<>(HUD_UPDATE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, HUDUpdatePayload> CODEC = 
            StreamCodec.of(HUDUpdatePayload::write, HUDUpdatePayload::read);
        
        public static HUDUpdatePayload read(FriendlyByteBuf buf) {
            String elementId = buf.readUtf();
            
            int size = buf.readVarInt();
            Map<String, String> updates = new HashMap<>();
            for (int i = 0; i < size; i++) {
                updates.put(buf.readUtf(), buf.readUtf());
            }
            
            return new HUDUpdatePayload(elementId, updates);
        }
        
        public static void write(FriendlyByteBuf buf, HUDUpdatePayload payload) {
            buf.writeUtf(payload.elementId != null ? payload.elementId : "");
            
            Map<String, String> updates = payload.updates != null ? payload.updates : Collections.emptyMap();
            buf.writeVarInt(updates.size());
            for (Map.Entry<String, String> entry : updates.entrySet()) {
                buf.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buf.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for clearing HUD elements (server -> client).
     */
    public record HUDClearPayload(
        String group  // Empty string means clear all
    ) implements CustomPacketPayload {
        
        public static final Type<HUDClearPayload> TYPE = new Type<>(HUD_CLEAR_ID);
        
        public static final StreamCodec<FriendlyByteBuf, HUDClearPayload> CODEC = 
            StreamCodec.of(HUDClearPayload::write, HUDClearPayload::read);
        
        public static HUDClearPayload read(FriendlyByteBuf buf) {
            return new HUDClearPayload(buf.readUtf());
        }
        
        public static void write(FriendlyByteBuf buf, HUDClearPayload payload) {
            buf.writeUtf(payload.group != null ? payload.group : "");
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ===== GUI Payload Classes (Requirements: 9.4, 9.5) =====
    
    /**
     * Payload for opening a GUI (server -> client).
     */
    public record GUIOpenPayload(
        String guiId,
        Map<String, String> data
    ) implements CustomPacketPayload {
        
        public static final Type<GUIOpenPayload> TYPE = new Type<>(GUI_OPEN_ID);
        
        public static final StreamCodec<FriendlyByteBuf, GUIOpenPayload> CODEC = 
            StreamCodec.of(GUIOpenPayload::write, GUIOpenPayload::read);
        
        public static GUIOpenPayload read(FriendlyByteBuf buf) {
            String guiId = buf.readUtf();
            
            int size = buf.readVarInt();
            Map<String, String> data = new HashMap<>();
            for (int i = 0; i < size; i++) {
                data.put(buf.readUtf(), buf.readUtf());
            }
            
            return new GUIOpenPayload(guiId, data);
        }
        
        public static void write(FriendlyByteBuf buf, GUIOpenPayload payload) {
            buf.writeUtf(payload.guiId != null ? payload.guiId : "");
            
            Map<String, String> data = payload.data != null ? payload.data : Collections.emptyMap();
            buf.writeVarInt(data.size());
            for (Map.Entry<String, String> entry : data.entrySet()) {
                buf.writeUtf(entry.getKey() != null ? entry.getKey() : "");
                buf.writeUtf(entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for closing a GUI (server -> client).
     */
    public record GUIClosePayload() implements CustomPacketPayload {
        
        public static final Type<GUIClosePayload> TYPE = new Type<>(GUI_CLOSE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, GUIClosePayload> CODEC = 
            StreamCodec.of(GUIClosePayload::write, GUIClosePayload::read);
        
        public static GUIClosePayload read(FriendlyByteBuf buf) {
            return new GUIClosePayload();
        }
        
        public static void write(FriendlyByteBuf buf, GUIClosePayload payload) {
            // No data to write
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ===== HUD/GUI Helper Methods =====
    
    /**
     * Sends a HUD show packet to a player.
     * 
     * @param player The target player
     * @param elementId Unique element ID
     * @param elementType Type of element ("text", "image", "progress")
     * @param x X position
     * @param y Y position
     * @param width Element width
     * @param height Element height
     * @param properties Additional properties
     */
    public void sendHUDShow(ServerPlayer player, String elementId, String elementType,
                            int x, int y, int width, int height, Map<String, String> properties) {
        HUDShowPayload payload = new HUDShowPayload(elementId, elementType, x, y, width, height, properties);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent HUD show packet for '{}' to player {}", elementId, player.getName().getString());
    }
    
    /**
     * Sends a HUD hide packet to a player.
     * 
     * @param player The target player
     * @param elementId Element ID to hide
     */
    public void sendHUDHide(ServerPlayer player, String elementId) {
        HUDHidePayload payload = new HUDHidePayload(elementId);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent HUD hide packet for '{}' to player {}", elementId, player.getName().getString());
    }
    
    /**
     * Sends a HUD update packet to a player.
     * 
     * @param player The target player
     * @param elementId Element ID to update
     * @param updates Property updates
     */
    public void sendHUDUpdate(ServerPlayer player, String elementId, Map<String, String> updates) {
        HUDUpdatePayload payload = new HUDUpdatePayload(elementId, updates);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent HUD update packet for '{}' to player {}", elementId, player.getName().getString());
    }
    
    /**
     * Sends a HUD clear packet to a player.
     * 
     * @param player The target player
     * @param group Group to clear (empty string for all)
     */
    public void sendHUDClear(ServerPlayer player, String group) {
        HUDClearPayload payload = new HUDClearPayload(group);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent HUD clear packet (group='{}') to player {}", group, player.getName().getString());
    }
    
    /**
     * Sends a GUI open packet to a player.
     * 
     * @param player The target player
     * @param guiId GUI ID to open
     * @param data Additional data for the GUI
     */
    public void sendGUIOpen(ServerPlayer player, String guiId, Map<String, String> data) {
        GUIOpenPayload payload = new GUIOpenPayload(guiId, data);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent GUI open packet for '{}' to player {}", guiId, player.getName().getString());
    }
    
    /**
     * Sends a GUI close packet to a player.
     * 
     * @param player The target player
     */
    public void sendGUIClose(ServerPlayer player) {
        GUIClosePayload payload = new GUIClosePayload();
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent GUI close packet to player {}", player.getName().getString());
    }
    
    // ===== NPC Animation Payload Classes (Requirements: 10.2) =====
    
    /**
     * Payload for starting an NPC animation (server -> client).
     */
    public record NPCAnimationPayload(
        String npcId,
        String animationId,
        boolean loop
    ) implements CustomPacketPayload {
        
        public static final Type<NPCAnimationPayload> TYPE = new Type<>(NPC_ANIMATION_ID);
        
        public static final StreamCodec<FriendlyByteBuf, NPCAnimationPayload> CODEC = 
            StreamCodec.of(NPCAnimationPayload::write, NPCAnimationPayload::read);
        
        public static NPCAnimationPayload read(FriendlyByteBuf buf) {
            String npcId = buf.readUtf();
            String animationId = buf.readUtf();
            boolean loop = buf.readBoolean();
            return new NPCAnimationPayload(npcId, animationId, loop);
        }
        
        public static void write(FriendlyByteBuf buf, NPCAnimationPayload payload) {
            buf.writeUtf(payload.npcId != null ? payload.npcId : "");
            buf.writeUtf(payload.animationId != null ? payload.animationId : "");
            buf.writeBoolean(payload.loop);
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for stopping an NPC animation (server -> client).
     */
    public record NPCAnimationStopPayload(
        String npcId
    ) implements CustomPacketPayload {
        
        public static final Type<NPCAnimationStopPayload> TYPE = new Type<>(NPC_ANIMATION_STOP_ID);
        
        public static final StreamCodec<FriendlyByteBuf, NPCAnimationStopPayload> CODEC = 
            StreamCodec.of(NPCAnimationStopPayload::write, NPCAnimationStopPayload::read);
        
        public static NPCAnimationStopPayload read(FriendlyByteBuf buf) {
            return new NPCAnimationStopPayload(buf.readUtf());
        }
        
        public static void write(FriendlyByteBuf buf, NPCAnimationStopPayload payload) {
            buf.writeUtf(payload.npcId != null ? payload.npcId : "");
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    // ===== NPC Animation Helper Methods =====
    
    /**
     * Broadcasts an NPC animation to all players in range.
     * 
     * @param npcId The NPC ID
     * @param animationId The animation to play
     * @param loop Whether to loop the animation
     */
    public void broadcastNPCAnimation(String npcId, String animationId, boolean loop) {
        if (server == null) {
            LOGGER.warn("Cannot broadcast NPC animation - server not initialized");
            return;
        }
        
        NPCAnimationPayload payload = new NPCAnimationPayload(npcId, animationId, loop);
        
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
        
        LOGGER.debug("Broadcast NPC animation '{}' for NPC '{}' to all players", animationId, npcId);
    }
    
    /**
     * Broadcasts an NPC animation stop to all players.
     * 
     * @param npcId The NPC ID
     */
    public void broadcastNPCAnimationStop(String npcId) {
        if (server == null) {
            LOGGER.warn("Cannot broadcast NPC animation stop - server not initialized");
            return;
        }
        
        NPCAnimationStopPayload payload = new NPCAnimationStopPayload(npcId);
        
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, payload);
        }
        
        LOGGER.debug("Broadcast NPC animation stop for NPC '{}' to all players", npcId);
    }
    
    /**
     * Sends an NPC animation to a specific player.
     * 
     * @param player The target player
     * @param npcId The NPC ID
     * @param animationId The animation to play
     * @param loop Whether to loop the animation
     */
    public void sendNPCAnimation(ServerPlayer player, String npcId, String animationId, boolean loop) {
        NPCAnimationPayload payload = new NPCAnimationPayload(npcId, animationId, loop);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent NPC animation '{}' for NPC '{}' to player {}", 
            animationId, npcId, player.getName().getString());
    }
    
    /**
     * Sends an NPC animation stop to a specific player.
     * 
     * @param player The target player
     * @param npcId The NPC ID
     */
    public void sendNPCAnimationStop(ServerPlayer player, String npcId) {
        NPCAnimationStopPayload payload = new NPCAnimationStopPayload(npcId);
        ServerPlayNetworking.send(player, payload);
        LOGGER.debug("Sent NPC animation stop for NPC '{}' to player {}", npcId, player.getName().getString());
    }
}
