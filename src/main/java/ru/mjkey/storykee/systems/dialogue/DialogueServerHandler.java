package ru.mjkey.storykee.systems.dialogue;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-side handler for dialogue networking.
 * Handles receiving player actions and sending dialogue updates.
 * 
 * Requirements: 7.1, 7.3, 7.4
 */
public class DialogueServerHandler implements DialogueManager.DialogueEventListener {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueServerHandler.class);
    
    // Packet ResourceLocations
    public static final ResourceLocation DIALOGUE_OPEN_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "dialogue_open");
    public static final ResourceLocation DIALOGUE_UPDATE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "dialogue_update");
    public static final ResourceLocation DIALOGUE_CLOSE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "dialogue_close");
    public static final ResourceLocation DIALOGUE_CHOICE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "dialogue_choice");
    public static final ResourceLocation DIALOGUE_ADVANCE_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "dialogue_advance");
    public static final ResourceLocation DIALOGUE_CLOSE_REQUEST_ID = ResourceLocation.fromNamespaceAndPath("itsmystory", "dialogue_close_request");
    
    private static DialogueServerHandler instance;
    
    private DialogueServerHandler() {}
    
    public static DialogueServerHandler getInstance() {
        if (instance == null) {
            instance = new DialogueServerHandler();
        }
        return instance;
    }
    
    /**
     * Registers server-side packet handlers.
     */
    public void register() {
        LOGGER.info("Registering dialogue server handlers");
        
        // Register payload types for server-to-client packets
        PayloadTypeRegistry.playS2C().register(DialogueOpenPayload.TYPE, DialogueOpenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DialogueUpdatePayload.TYPE, DialogueUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DialogueClosePayload.TYPE, DialogueClosePayload.CODEC);
        
        // Register payload types for client-to-server packets
        PayloadTypeRegistry.playC2S().register(DialogueChoicePayload.TYPE, DialogueChoicePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DialogueAdvancePayload.TYPE, DialogueAdvancePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DialogueCloseRequestPayload.TYPE, DialogueCloseRequestPayload.CODEC);
        
        // Register receivers for client-to-server packets
        ServerPlayNetworking.registerGlobalReceiver(DialogueChoicePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleChoiceSelection(player, payload.choiceIndex()));
        });
        
        ServerPlayNetworking.registerGlobalReceiver(DialogueAdvancePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleAdvanceDialogue(player));
        });
        
        ServerPlayNetworking.registerGlobalReceiver(DialogueCloseRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleCloseRequest(player));
        });
        
        // Register as dialogue event listener
        DialogueManager.getInstance().addEventListener(this);
    }
    
    /**
     * Handles choice selection from client.
     */
    private void handleChoiceSelection(ServerPlayer player, int choiceIndex) {
        LOGGER.debug("Player {} selected choice {}", player.getName().getString(), choiceIndex);
        DialogueManager.getInstance().selectChoice(player.getUUID(), choiceIndex);
    }
    
    /**
     * Handles advance dialogue request from client.
     */
    private void handleAdvanceDialogue(ServerPlayer player) {
        LOGGER.debug("Player {} requested dialogue advance", player.getName().getString());
        DialogueManager.getInstance().advanceDialogue(player.getUUID());
    }
    
    /**
     * Handles close dialogue request from client.
     */
    private void handleCloseRequest(ServerPlayer player) {
        LOGGER.debug("Player {} requested dialogue close", player.getName().getString());
        DialogueManager.getInstance().closeDialogue(player.getUUID());
    }
    
    // ===== DialogueEventListener Implementation =====
    
    @Override
    public void onDialogueStarted(UUID playerId, Dialogue dialogue) {
        // Node display will handle sending the packet
    }
    
    @Override
    public void onNodeDisplayed(UUID playerId, Dialogue dialogue, DialogueNode node) {
        // Find the player by UUID and send the dialogue packet
        ServerPlayer player = findPlayerByUUID(playerId);
        if (player != null) {
            sendDialogueOpen(player, dialogue, node);
            LOGGER.debug("Sent dialogue open packet to player {} for dialogue {}", player.getName().getString(), dialogue.getId());
        } else {
            LOGGER.warn("Could not find player with UUID {} to send dialogue packet", playerId);
        }
    }
    
    @Override
    public void onDialogueEnded(UUID playerId, Dialogue dialogue, boolean completed) {
        // Find the player and send close packet
        ServerPlayer player = findPlayerByUUID(playerId);
        if (player != null) {
            sendDialogueClose(player);
            LOGGER.debug("Sent dialogue close packet to player {}", player.getName().getString());
        }
    }
    
    /**
     * Finds a ServerPlayer by their UUID.
     */
    private ServerPlayer findPlayerByUUID(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        
        // Get the server from the current server instance
        net.minecraft.server.MinecraftServer server = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getServer();
        if (server != null) {
            return server.getPlayerList().getPlayer(playerId);
        }
        return null;
    }
    
    // ===== Packet Sending (called externally with player reference) =====
    
    /**
     * Sends dialogue open packet to client.
     */
    public void sendDialogueOpen(ServerPlayer player, Dialogue dialogue, DialogueNode node) {
        String text = DialogueManager.getInstance().interpolateText(node.getText(), player.getUUID(), null);
        List<ChoiceData> choices = convertChoices(node, player.getUUID());
        
        DialogueOpenPayload payload = new DialogueOpenPayload(
            dialogue.getId(),
            node.getId(),
            node.getSpeakerName() != null ? node.getSpeakerName() : "",
            node.getSpeakerTexture() != null ? node.getSpeakerTexture() : "",
            text != null ? text : "",
            choices
        );
        
        ServerPlayNetworking.send(player, payload);
    }
    
    /**
     * Sends dialogue update packet to client.
     */
    public void sendDialogueUpdate(ServerPlayer player, Dialogue dialogue, DialogueNode node) {
        String text = DialogueManager.getInstance().interpolateText(node.getText(), player.getUUID(), null);
        List<ChoiceData> choices = convertChoices(node, player.getUUID());
        
        DialogueUpdatePayload payload = new DialogueUpdatePayload(
            dialogue.getId(),
            node.getId(),
            node.getSpeakerName() != null ? node.getSpeakerName() : "",
            node.getSpeakerTexture() != null ? node.getSpeakerTexture() : "",
            text != null ? text : "",
            choices
        );
        
        ServerPlayNetworking.send(player, payload);
    }
    
    /**
     * Sends dialogue close packet to client.
     */
    public void sendDialogueClose(ServerPlayer player) {
        ServerPlayNetworking.send(player, new DialogueClosePayload());
    }
    
    /**
     * Converts dialogue choices to network-friendly format.
     */
    private List<ChoiceData> convertChoices(DialogueNode node, UUID playerId) {
        List<ChoiceData> choices = new ArrayList<>();
        
        for (DialogueChoice choice : DialogueManager.getInstance().getAvailableChoices(playerId, node)) {
            String text = DialogueManager.getInstance().interpolateText(choice.getText(), playerId, null);
            choices.add(new ChoiceData(choice.getId(), text, choice.getNextNodeId()));
        }
        
        return choices;
    }
    
    // ===== Data Classes =====
    
    /**
     * Simple data class for choice information.
     */
    public record ChoiceData(String id, String text, String nextNodeId) {}
    
    // ===== Payload Classes =====
    
    /**
     * Payload for opening a dialogue (server -> client).
     */
    public record DialogueOpenPayload(
        String dialogueId,
        String nodeId,
        String speakerName,
        String speakerTexture,
        String text,
        List<ChoiceData> choices
    ) implements CustomPacketPayload {
        
        public static final Type<DialogueOpenPayload> TYPE = new Type<>(DIALOGUE_OPEN_ID);
        
        public static final StreamCodec<FriendlyByteBuf, DialogueOpenPayload> CODEC = 
            StreamCodec.of(DialogueOpenPayload::write, DialogueOpenPayload::read);
        
        public static DialogueOpenPayload read(FriendlyByteBuf buf) {
            String dialogueId = buf.readUtf();
            String nodeId = buf.readUtf();
            String speakerName = buf.readUtf();
            String speakerTexture = buf.readUtf();
            String text = buf.readUtf();
            
            int choiceCount = buf.readVarInt();
            List<ChoiceData> choices = new ArrayList<>();
            for (int i = 0; i < choiceCount; i++) {
                String choiceId = buf.readUtf();
                String choiceText = buf.readUtf();
                String nextNodeId = buf.readUtf();
                choices.add(new ChoiceData(choiceId, choiceText, 
                    nextNodeId.isEmpty() ? null : nextNodeId));
            }
            
            return new DialogueOpenPayload(dialogueId, nodeId, speakerName, speakerTexture, text, choices);
        }
        
        public static void write(FriendlyByteBuf buf, DialogueOpenPayload payload) {
            buf.writeUtf(payload.dialogueId != null ? payload.dialogueId : "");
            buf.writeUtf(payload.nodeId != null ? payload.nodeId : "");
            buf.writeUtf(payload.speakerName != null ? payload.speakerName : "");
            buf.writeUtf(payload.speakerTexture != null ? payload.speakerTexture : "");
            buf.writeUtf(payload.text != null ? payload.text : "");
            
            buf.writeVarInt(payload.choices != null ? payload.choices.size() : 0);
            if (payload.choices != null) {
                for (ChoiceData choice : payload.choices) {
                    buf.writeUtf(choice.id() != null ? choice.id() : "");
                    buf.writeUtf(choice.text() != null ? choice.text() : "");
                    buf.writeUtf(choice.nextNodeId() != null ? choice.nextNodeId() : "");
                }
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for updating dialogue content (server -> client).
     */
    public record DialogueUpdatePayload(
        String dialogueId,
        String nodeId,
        String speakerName,
        String speakerTexture,
        String text,
        List<ChoiceData> choices
    ) implements CustomPacketPayload {
        
        public static final Type<DialogueUpdatePayload> TYPE = new Type<>(DIALOGUE_UPDATE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, DialogueUpdatePayload> CODEC = 
            StreamCodec.of(DialogueUpdatePayload::write, DialogueUpdatePayload::read);
        
        public static DialogueUpdatePayload read(FriendlyByteBuf buf) {
            String dialogueId = buf.readUtf();
            String nodeId = buf.readUtf();
            String speakerName = buf.readUtf();
            String speakerTexture = buf.readUtf();
            String text = buf.readUtf();
            
            int choiceCount = buf.readVarInt();
            List<ChoiceData> choices = new ArrayList<>();
            for (int i = 0; i < choiceCount; i++) {
                String choiceId = buf.readUtf();
                String choiceText = buf.readUtf();
                String nextNodeId = buf.readUtf();
                choices.add(new ChoiceData(choiceId, choiceText, 
                    nextNodeId.isEmpty() ? null : nextNodeId));
            }
            
            return new DialogueUpdatePayload(dialogueId, nodeId, speakerName, speakerTexture, text, choices);
        }
        
        public static void write(FriendlyByteBuf buf, DialogueUpdatePayload payload) {
            buf.writeUtf(payload.dialogueId != null ? payload.dialogueId : "");
            buf.writeUtf(payload.nodeId != null ? payload.nodeId : "");
            buf.writeUtf(payload.speakerName != null ? payload.speakerName : "");
            buf.writeUtf(payload.speakerTexture != null ? payload.speakerTexture : "");
            buf.writeUtf(payload.text != null ? payload.text : "");
            
            buf.writeVarInt(payload.choices != null ? payload.choices.size() : 0);
            if (payload.choices != null) {
                for (ChoiceData choice : payload.choices) {
                    buf.writeUtf(choice.id() != null ? choice.id() : "");
                    buf.writeUtf(choice.text() != null ? choice.text() : "");
                    buf.writeUtf(choice.nextNodeId() != null ? choice.nextNodeId() : "");
                }
            }
        }
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for closing dialogue (server -> client).
     */
    public record DialogueClosePayload() implements CustomPacketPayload {
        
        public static final Type<DialogueClosePayload> TYPE = new Type<>(DIALOGUE_CLOSE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, DialogueClosePayload> CODEC = 
            StreamCodec.of((buf, payload) -> {}, buf -> new DialogueClosePayload());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for choice selection (client -> server).
     */
    public record DialogueChoicePayload(int choiceIndex) implements CustomPacketPayload {
        
        public static final Type<DialogueChoicePayload> TYPE = new Type<>(DIALOGUE_CHOICE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, DialogueChoicePayload> CODEC = 
            StreamCodec.of(
                (buf, payload) -> buf.writeVarInt(payload.choiceIndex),
                buf -> new DialogueChoicePayload(buf.readVarInt())
            );
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for advancing dialogue (client -> server).
     */
    public record DialogueAdvancePayload() implements CustomPacketPayload {
        
        public static final Type<DialogueAdvancePayload> TYPE = new Type<>(DIALOGUE_ADVANCE_ID);
        
        public static final StreamCodec<FriendlyByteBuf, DialogueAdvancePayload> CODEC = 
            StreamCodec.of((buf, payload) -> {}, buf -> new DialogueAdvancePayload());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
    
    /**
     * Payload for close dialogue request (client -> server).
     */
    public record DialogueCloseRequestPayload() implements CustomPacketPayload {
        
        public static final Type<DialogueCloseRequestPayload> TYPE = new Type<>(DIALOGUE_CLOSE_REQUEST_ID);
        
        public static final StreamCodec<FriendlyByteBuf, DialogueCloseRequestPayload> CODEC = 
            StreamCodec.of((buf, payload) -> {}, buf -> new DialogueCloseRequestPayload());
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
