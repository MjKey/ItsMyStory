package ru.mjkey.storykee.systems.dialogue;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side handler for dialogue networking.
 * Handles receiving dialogue updates from server and sending player actions.
 * 
 * Requirements: 7.1, 7.3
 */
public class DialogueClientHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueClientHandler.class);
    
    // Current dialogue screen
    private static DialogueScreen currentScreen = null;
    
    /**
     * Registers client-side packet handlers.
     * Uses payload types from DialogueServerHandler for consistency.
     */
    public static void register() {
        LOGGER.info("Registering dialogue client handlers");
        
        // Register receivers for server-to-client packets using server payload types
        ClientPlayNetworking.registerGlobalReceiver(DialogueServerHandler.DialogueOpenPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> handleDialogueOpen(payload));
        });
        
        ClientPlayNetworking.registerGlobalReceiver(DialogueServerHandler.DialogueUpdatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> handleDialogueUpdate(payload));
        });
        
        ClientPlayNetworking.registerGlobalReceiver(DialogueServerHandler.DialogueClosePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> handleDialogueClose());
        });
    }
    
    /**
     * Handles opening a new dialogue.
     */
    private static void handleDialogueOpen(DialogueServerHandler.DialogueOpenPayload payload) {
        LOGGER.debug("Opening dialogue: {}", payload.dialogueId());
        
        Minecraft client = Minecraft.getInstance();
        
        // Convert server ChoiceData to client ChoiceData
        List<DialogueScreen.ChoiceData> choices = new ArrayList<>();
        if (payload.choices() != null) {
            for (DialogueServerHandler.ChoiceData serverChoice : payload.choices()) {
                choices.add(new DialogueScreen.ChoiceData(
                    serverChoice.id(),
                    serverChoice.text(),
                    serverChoice.nextNodeId()
                ));
            }
        }
        
        // Create and show dialogue screen
        currentScreen = new DialogueScreen();
        currentScreen.setDialogueContent(
            payload.dialogueId(),
            payload.nodeId(),
            payload.speakerName(),
            payload.speakerTexture(),
            payload.text(),
            choices
        );
        
        client.setScreen(currentScreen);
    }
    
    /**
     * Handles updating the current dialogue.
     */
    private static void handleDialogueUpdate(DialogueServerHandler.DialogueUpdatePayload payload) {
        LOGGER.debug("Updating dialogue node: {}", payload.nodeId());
        
        if (currentScreen != null) {
            // Convert server ChoiceData to client ChoiceData
            List<DialogueScreen.ChoiceData> choices = new ArrayList<>();
            if (payload.choices() != null) {
                for (DialogueServerHandler.ChoiceData serverChoice : payload.choices()) {
                    choices.add(new DialogueScreen.ChoiceData(
                        serverChoice.id(),
                        serverChoice.text(),
                        serverChoice.nextNodeId()
                    ));
                }
            }
            
            currentScreen.setDialogueContent(
                payload.dialogueId(),
                payload.nodeId(),
                payload.speakerName(),
                payload.speakerTexture(),
                payload.text(),
                choices
            );
        }
    }
    
    /**
     * Handles closing the dialogue.
     */
    private static void handleDialogueClose() {
        LOGGER.debug("Closing dialogue");
        
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof DialogueScreen) {
            client.setScreen(null);
        }
        currentScreen = null;
    }
    
    /**
     * Sends a choice selection to the server.
     */
    public static void sendChoiceSelection(int choiceIndex) {
        LOGGER.debug("Sending choice selection: {}", choiceIndex);
        ClientPlayNetworking.send(new DialogueServerHandler.DialogueChoicePayload(choiceIndex));
    }
    
    /**
     * Sends an advance dialogue request to the server.
     */
    public static void sendAdvanceDialogue() {
        LOGGER.debug("Sending advance dialogue request");
        ClientPlayNetworking.send(new DialogueServerHandler.DialogueAdvancePayload());
    }
    
    /**
     * Sends a close dialogue request to the server.
     */
    public static void sendCloseDialogue() {
        LOGGER.debug("Sending close dialogue request");
        ClientPlayNetworking.send(new DialogueServerHandler.DialogueCloseRequestPayload());
    }
}
