package ru.mjkey.storykee.systems.dialogue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.statement.DialogueDeclarationNode;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.systems.localization.LocalizationManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages dialogue interactions in the story system.
 * Handles dialogue registration, lookup, state tracking per player, and choice handling.
 * 
 * Requirements: 7.1, 7.3, 7.4
 */
public class DialogueManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueManager.class);
    
    // Pattern for variable interpolation: ${variableName}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    
    private static DialogueManager instance;
    
    // Registered dialogues by ID
    private final Map<String, Dialogue> dialogues;
    
    // Active dialogue states per player (by UUID)
    private final Map<UUID, DialogueState> playerDialogueStates;
    
    // Dialogue parser for AST conversion
    private final DialogueParser dialogueParser;
    
    // Callbacks for dialogue events
    private final List<DialogueEventListener> eventListeners;
    
    private DialogueManager() {
        this.dialogues = new ConcurrentHashMap<>();
        this.playerDialogueStates = new ConcurrentHashMap<>();
        this.dialogueParser = new DialogueParser();
        this.eventListeners = new ArrayList<>();
    }
    
    public static DialogueManager getInstance() {
        if (instance == null) {
            instance = new DialogueManager();
        }
        return instance;
    }
    
    // ===== Dialogue Registration =====
    
    /**
     * Registers a dialogue from an AST node.
     */
    public void registerDialogue(DialogueDeclarationNode node, String scriptId) {
        Dialogue dialogue = dialogueParser.parseDialogue(node, scriptId);
        registerDialogue(dialogue);
    }
    
    /**
     * Registers a dialogue directly.
     */
    public void registerDialogue(Dialogue dialogue) {
        if (dialogue == null || dialogue.getId() == null) {
            LOGGER.warn("Cannot register null dialogue or dialogue with null ID");
            return;
        }
        
        // Validate dialogue structure
        List<String> errors = dialogue.validate();
        if (!errors.isEmpty()) {
            LOGGER.warn("Dialogue '{}' has validation errors: {}", dialogue.getId(), errors);
        }
        
        dialogues.put(dialogue.getId(), dialogue);
        LOGGER.info("Registered dialogue: {}", dialogue.getId());
    }
    
    /**
     * Unregisters a dialogue by ID.
     */
    public void unregisterDialogue(String dialogueId) {
        Dialogue removed = dialogues.remove(dialogueId);
        if (removed != null) {
            LOGGER.info("Unregistered dialogue: {}", dialogueId);
            
            // Close any active instances of this dialogue
            playerDialogueStates.entrySet().removeIf(entry -> {
                if (entry.getValue().getDialogueId().equals(dialogueId)) {
                    LOGGER.debug("Closing active dialogue {} for player {}", dialogueId, entry.getKey());
                    return true;
                }
                return false;
            });
        }
    }
    
    /**
     * Unregisters all dialogues from a specific script.
     */
    public void unregisterDialoguesFromScript(String scriptId) {
        dialogues.entrySet().removeIf(entry -> {
            if (scriptId.equals(entry.getValue().getScriptId())) {
                LOGGER.debug("Unregistering dialogue {} from script {}", entry.getKey(), scriptId);
                return true;
            }
            return false;
        });
    }
    
    // ===== Dialogue Lookup =====
    
    /**
     * Gets a dialogue by ID.
     */
    public Dialogue getDialogue(String dialogueId) {
        return dialogues.get(dialogueId);
    }
    
    /**
     * Checks if a dialogue exists.
     */
    public boolean hasDialogue(String dialogueId) {
        return dialogues.containsKey(dialogueId);
    }
    
    /**
     * Gets all registered dialogue IDs.
     */
    public Set<String> getDialogueIds() {
        return Collections.unmodifiableSet(dialogues.keySet());
    }
    
    // ===== Dialogue State Management =====
    
    /**
     * Starts a dialogue for a player.
     * @param playerId The player's UUID
     * @param dialogueId The dialogue ID to start
     * @return true if dialogue was started successfully
     */
    public boolean startDialogue(UUID playerId, String dialogueId) {
        if (playerId == null) {
            LOGGER.warn("Cannot start dialogue for null player");
            return false;
        }
        
        Dialogue dialogue = dialogues.get(dialogueId);
        if (dialogue == null) {
            LOGGER.warn("Dialogue '{}' not found", dialogueId);
            return false;
        }
        
        // Check if player already has an active dialogue
        if (playerDialogueStates.containsKey(playerId)) {
            LOGGER.debug("Player {} already has active dialogue, closing it first", playerId);
            closeDialogue(playerId);
        }
        
        // Create new dialogue state
        DialogueState state = new DialogueState(playerId, dialogueId, dialogue.getStartNodeId());
        playerDialogueStates.put(playerId, state);
        
        LOGGER.info("Started dialogue '{}' for player {}", dialogueId, playerId);
        
        // Notify listeners
        fireDialogueStarted(playerId, dialogue);
        
        // Show the first node
        showCurrentNode(playerId);
        
        return true;
    }
    
    /**
     * Closes the active dialogue for a player.
     */
    public void closeDialogue(UUID playerId) {
        if (playerId == null) {
            return;
        }
        
        DialogueState state = playerDialogueStates.remove(playerId);
        
        if (state != null) {
            Dialogue dialogue = dialogues.get(state.getDialogueId());
            LOGGER.info("Closed dialogue '{}' for player {}", state.getDialogueId(), playerId);
            
            // Notify listeners
            fireDialogueEnded(playerId, dialogue, state.isCompleted());
        }
    }
    
    /**
     * Gets the current dialogue state for a player.
     */
    public DialogueState getDialogueState(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return playerDialogueStates.get(playerId);
    }
    
    /**
     * Checks if a player has an active dialogue.
     */
    public boolean hasActiveDialogue(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return playerDialogueStates.containsKey(playerId);
    }
    
    // ===== Choice Handling =====
    
    /**
     * Handles a player selecting a choice in the current dialogue.
     * @param playerId The player's UUID
     * @param choiceIndex The index of the selected choice (0-based)
     * @return true if choice was handled successfully
     */
    public boolean selectChoice(UUID playerId, int choiceIndex) {
        if (playerId == null) {
            return false;
        }
        
        DialogueState state = playerDialogueStates.get(playerId);
        
        if (state == null) {
            LOGGER.warn("Player {} has no active dialogue", playerId);
            return false;
        }
        
        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            LOGGER.error("Active dialogue '{}' not found in registry", state.getDialogueId());
            closeDialogue(playerId);
            return false;
        }
        
        DialogueNode currentNode = dialogue.getNode(state.getCurrentNodeId());
        if (currentNode == null) {
            LOGGER.error("Current node '{}' not found in dialogue '{}'", state.getCurrentNodeId(), dialogue.getId());
            closeDialogue(playerId);
            return false;
        }
        
        // Get available choices for this player
        List<DialogueChoice> availableChoices = getAvailableChoices(playerId, currentNode);
        
        if (choiceIndex < 0 || choiceIndex >= availableChoices.size()) {
            LOGGER.warn("Invalid choice index {} for player {}", choiceIndex, playerId);
            return false;
        }
        
        DialogueChoice selectedChoice = availableChoices.get(choiceIndex);
        
        LOGGER.debug("Player {} selected choice '{}' in dialogue '{}'", 
            playerId, selectedChoice.getText(), dialogue.getId());
        
        // Execute choice action
        selectedChoice.executeAction(playerId);
        
        // Notify listeners
        fireChoiceSelected(playerId, dialogue, currentNode, selectedChoice);
        
        // Advance to next node
        String nextNodeId = selectedChoice.getNextNodeId();
        advanceToNode(playerId, state, dialogue, nextNodeId);
        
        return true;
    }
    
    /**
     * Advances to the next node in the dialogue (for dialogues without choices).
     */
    public boolean advanceDialogue(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        
        DialogueState state = playerDialogueStates.get(playerId);
        
        if (state == null) {
            return false;
        }
        
        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            closeDialogue(playerId);
            return false;
        }
        
        DialogueNode currentNode = dialogue.getNode(state.getCurrentNodeId());
        if (currentNode == null) {
            closeDialogue(playerId);
            return false;
        }
        
        // If node has choices, don't auto-advance
        if (currentNode.hasChoices()) {
            return false;
        }
        
        // Advance to next node
        String nextNodeId = currentNode.getNextNodeId();
        advanceToNode(playerId, state, dialogue, nextNodeId);
        
        return true;
    }
    
    /**
     * Advances to a specific node in the dialogue.
     */
    private void advanceToNode(UUID playerId, DialogueState state, Dialogue dialogue, String nextNodeId) {
        if (nextNodeId == null) {
            // Dialogue complete
            state.setCompleted(true);
            closeDialogue(playerId);
            return;
        }
        
        DialogueNode nextNode = dialogue.getNode(nextNodeId);
        if (nextNode == null) {
            LOGGER.error("Next node '{}' not found in dialogue '{}'", nextNodeId, dialogue.getId());
            closeDialogue(playerId);
            return;
        }
        
        state.setCurrentNodeId(nextNodeId);
        showCurrentNode(playerId);
    }
    
    /**
     * Gets the available choices for a player at the current node.
     */
    public List<DialogueChoice> getAvailableChoices(UUID playerId, DialogueNode node) {
        List<DialogueChoice> available = new ArrayList<>();
        
        for (DialogueChoice choice : node.getChoices()) {
            if (choice.isAvailable(playerId)) {
                available.add(choice);
            }
        }
        
        return available;
    }
    
    // ===== Text Processing =====
    
    /**
     * Interpolates variables and translation keys in dialogue text.
     * Replaces ${variableName} with actual variable values.
     * Replaces @{key} or @key with localized text.
     * Requirement 26.5: Substitute translation keys with localized text in dialogues.
     */
    public String interpolateText(String text, UUID playerId, ExecutionContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // First, process translation keys (Requirement 26.5)
        String processedText = LocalizationManager.getInstance().processTranslationKeys(text);
        
        // Then, interpolate variables
        Matcher matcher = VARIABLE_PATTERN.matcher(processedText);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = resolveVariable(varName, playerId, context);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Resolves a variable name to its value.
     */
    private String resolveVariable(String varName, UUID playerId, ExecutionContext context) {
        // Try execution context first
        if (context != null && context.hasVariable(varName)) {
            Object value = context.getVariable(varName);
            return String.valueOf(value);
        }
        
        // Variable resolution from player/global stores would require access to VariableManager
        // For now, return the placeholder - the server-side interpolation should handle this
        // before sending to client
        
        // Return placeholder if not found
        return "${" + varName + "}";
    }
    
    // ===== Display Methods =====
    
    /**
     * Shows the current dialogue node to the player.
     * This method should be called by the renderer/UI system.
     */
    private void showCurrentNode(UUID playerId) {
        DialogueState state = playerDialogueStates.get(playerId);
        if (state == null) {
            return;
        }
        
        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            return;
        }
        
        DialogueNode node = dialogue.getNode(state.getCurrentNodeId());
        if (node == null) {
            return;
        }
        
        // Notify listeners to display the node
        fireNodeDisplayed(playerId, dialogue, node);
    }
    
    /**
     * Gets the current node for a player's active dialogue.
     */
    public DialogueNode getCurrentNode(UUID playerId) {
        DialogueState state = playerDialogueStates.get(playerId);
        if (state == null) {
            return null;
        }
        
        Dialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            return null;
        }
        
        return dialogue.getNode(state.getCurrentNodeId());
    }
    
    // ===== Event Listeners =====
    
    /**
     * Adds a dialogue event listener.
     */
    public void addEventListener(DialogueEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Removes a dialogue event listener.
     */
    public void removeEventListener(DialogueEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void fireDialogueStarted(UUID playerId, Dialogue dialogue) {
        for (DialogueEventListener listener : eventListeners) {
            try {
                listener.onDialogueStarted(playerId, dialogue);
            } catch (Exception e) {
                LOGGER.error("Error in dialogue event listener", e);
            }
        }
    }
    
    private void fireDialogueEnded(UUID playerId, Dialogue dialogue, boolean completed) {
        for (DialogueEventListener listener : eventListeners) {
            try {
                listener.onDialogueEnded(playerId, dialogue, completed);
            } catch (Exception e) {
                LOGGER.error("Error in dialogue event listener", e);
            }
        }
    }
    
    private void fireNodeDisplayed(UUID playerId, Dialogue dialogue, DialogueNode node) {
        for (DialogueEventListener listener : eventListeners) {
            try {
                listener.onNodeDisplayed(playerId, dialogue, node);
            } catch (Exception e) {
                LOGGER.error("Error in dialogue event listener", e);
            }
        }
    }
    
    private void fireChoiceSelected(UUID playerId, Dialogue dialogue, DialogueNode node, DialogueChoice choice) {
        for (DialogueEventListener listener : eventListeners) {
            try {
                listener.onChoiceSelected(playerId, dialogue, node, choice);
            } catch (Exception e) {
                LOGGER.error("Error in dialogue event listener", e);
            }
        }
    }
    
    // ===== Cleanup =====
    
    /**
     * Clears all registered dialogues and active states.
     */
    public void clear() {
        dialogues.clear();
        playerDialogueStates.clear();
        LOGGER.info("Cleared all dialogues");
    }
    
    /**
     * Interface for dialogue event listeners.
     */
    public interface DialogueEventListener {
        default void onDialogueStarted(UUID playerId, Dialogue dialogue) {}
        default void onDialogueEnded(UUID playerId, Dialogue dialogue, boolean completed) {}
        default void onNodeDisplayed(UUID playerId, Dialogue dialogue, DialogueNode node) {}
        default void onChoiceSelected(UUID playerId, Dialogue dialogue, DialogueNode node, DialogueChoice choice) {}
    }
}
