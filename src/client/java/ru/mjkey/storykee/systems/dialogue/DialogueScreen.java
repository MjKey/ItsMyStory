package ru.mjkey.storykee.systems.dialogue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client-side screen for displaying dialogues.
 * Renders dialogue text, speaker information, and choice buttons.
 * 
 * Requirements: 7.1, 7.2, 7.3
 */
public class DialogueScreen extends Screen {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueScreen.class);
    
    // Pattern for variable interpolation: ${variableName}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    
    // UI Constants
    private static final int DIALOGUE_AABB_HEIGHT = 120;
    private static final int DIALOGUE_AABB_MARGIN = 20;
    private static final int TEXT_PADDING = 15;
    private static final int CHOICE_BUTTON_HEIGHT = 20;
    private static final int CHOICE_BUTTON_SPACING = 5;
    private static final int SPEAKER_PORTRAIT_SIZE = 64;
    
    // Colors
    private static final int BACKGROUND_COLOR = 0xCC000000;
    private static final int BORDER_COLOR = 0xFF444444;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SPEAKER_NAME_COLOR = 0xFFFFD700;
    
    // Current dialogue data
    private String dialogueId;
    private String currentNodeId;
    private String speakerName;
    private String speakerTexture;
    private String dialogueText;
    private List<ChoiceData> choices;
    
    // UI state
    private List<Button> choiceButtons;
    private boolean hasChoices;
    
    public DialogueScreen() {
        super(Component.literal("Dialogue"));
        this.choices = new ArrayList<>();
        this.choiceButtons = new ArrayList<>();
    }
    
    /**
     * Updates the dialogue content to display.
     */
    public void setDialogueContent(String dialogueId, String nodeId, String speakerName, 
                                    String speakerTexture, String text, List<ChoiceData> choices) {
        this.dialogueId = dialogueId;
        this.currentNodeId = nodeId;
        this.speakerName = speakerName;
        this.speakerTexture = speakerTexture;
        this.dialogueText = interpolateVariables(text);
        this.choices = choices != null ? choices : new ArrayList<>();
        this.hasChoices = !this.choices.isEmpty();
        
        // Rebuild choice buttons
        rebuildChoiceButtons();
    }
    
    /**
     * Interpolates variables in text (client-side version).
     */
    private String interpolateVariables(String text) {
        if (text == null) {
            return "";
        }
        
        // Client-side variable interpolation is limited
        // Most interpolation should happen server-side
        // Here we just handle any remaining placeholders gracefully
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            // Try to get from client-side cache or leave as placeholder
            String replacement = getClientVariable(varName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * Gets a variable value from client-side cache.
     */
    private String getClientVariable(String varName) {
        // Special variables
        if ("playerName".equals(varName) && minecraft != null && minecraft.player != null) {
            return minecraft.player.getName().getString();
        }
        
        // Return placeholder for unknown variables
        return "${" + varName + "}";
    }
    
    @Override
    protected void init() {
        super.init();
        rebuildChoiceButtons();
    }
    
    /**
     * Rebuilds the choice buttons based on current choices.
     */
    private void rebuildChoiceButtons() {
        // Clear existing buttons
        for (Button button : choiceButtons) {
            removeWidget(button);
        }
        choiceButtons.clear();
        
        if (choices.isEmpty()) {
            return;
        }
        
        int AABBY = height - DIALOGUE_AABB_HEIGHT - DIALOGUE_AABB_MARGIN;
        int buttonWidth = width - DIALOGUE_AABB_MARGIN * 2 - TEXT_PADDING * 2;
        int buttonX = DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        
        // Calculate starting Y for buttons (above the dialogue text area)
        int totalButtonHeight = choices.size() * (CHOICE_BUTTON_HEIGHT + CHOICE_BUTTON_SPACING);
        int buttonStartY = AABBY - totalButtonHeight - 10;
        
        for (int i = 0; i < choices.size(); i++) {
            final int choiceIndex = i;
            ChoiceData choice = choices.get(i);
            
            int buttonY = buttonStartY + i * (CHOICE_BUTTON_HEIGHT + CHOICE_BUTTON_SPACING);
            
            Button button = Button.builder(
                Component.literal(choice.text()),
                btn -> onChoiceSelected(choiceIndex)
            )
            .bounds(buttonX, buttonY, buttonWidth, CHOICE_BUTTON_HEIGHT)
            .build();
            
            choiceButtons.add(button);
            addRenderableWidget(button);
        }
    }
    
    /**
     * Called when a choice button is clicked.
     */
    private void onChoiceSelected(int choiceIndex) {
        LOGGER.debug("Choice {} selected in dialogue {}", choiceIndex, dialogueId);
        
        // Send choice selection to server
        DialogueClientHandler.sendChoiceSelection(choiceIndex);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Render semi-transparent background
        super.render(graphics, mouseX, mouseY, delta);
        
        // Render dialogue AABB
        renderDialogueAABB(graphics);
        
        // Render speaker portrait if available
        if (speakerTexture != null && !speakerTexture.isEmpty()) {
            renderSpeakerPortrait(graphics);
        }
        
        // Render speaker name
        if (speakerName != null && !speakerName.isEmpty()) {
            renderSpeakerName(graphics);
        }
        
        // Render dialogue text
        renderDialogueText(graphics);
        
        // Render continue indicator if no choices
        if (!hasChoices) {
            renderContinueIndicator(graphics);
        }
    }
    
    /**
     * Renders the main dialogue AABB.
     */
    private void renderDialogueAABB(GuiGraphics graphics) {
        int AABBX = DIALOGUE_AABB_MARGIN;
        int AABBY = height - DIALOGUE_AABB_HEIGHT - DIALOGUE_AABB_MARGIN;
        int AABBWidth = width - DIALOGUE_AABB_MARGIN * 2;
        
        // Background
        graphics.fill(AABBX, AABBY, AABBX + AABBWidth, AABBY + DIALOGUE_AABB_HEIGHT, BACKGROUND_COLOR);
        
        // Border (draw 4 lines)
        graphics.fill(AABBX, AABBY, AABBX + AABBWidth, AABBY + 1, BORDER_COLOR); // Top
        graphics.fill(AABBX, AABBY + DIALOGUE_AABB_HEIGHT - 1, AABBX + AABBWidth, AABBY + DIALOGUE_AABB_HEIGHT, BORDER_COLOR); // Bottom
        graphics.fill(AABBX, AABBY, AABBX + 1, AABBY + DIALOGUE_AABB_HEIGHT, BORDER_COLOR); // Left
        graphics.fill(AABBX + AABBWidth - 1, AABBY, AABBX + AABBWidth, AABBY + DIALOGUE_AABB_HEIGHT, BORDER_COLOR); // Right
    }
    
    /**
     * Renders the speaker portrait.
     */
    private void renderSpeakerPortrait(GuiGraphics graphics) {
        int portraitX = DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        int portraitY = height - DIALOGUE_AABB_HEIGHT - DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        
        // Try to load and render the speaker texture
        try {
            ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath("itsmystory", "textures/dialogue/" + speakerTexture);
            graphics.blit(textureId, portraitX, portraitY, 0, 0, 
                SPEAKER_PORTRAIT_SIZE, SPEAKER_PORTRAIT_SIZE, SPEAKER_PORTRAIT_SIZE, SPEAKER_PORTRAIT_SIZE);
        } catch (Exception e) {
            // Draw placeholder if texture not found
            graphics.fill(portraitX, portraitY, 
                portraitX + SPEAKER_PORTRAIT_SIZE, portraitY + SPEAKER_PORTRAIT_SIZE, 
                0xFF333333);
        }
    }
    
    /**
     * Renders the speaker name.
     */
    private void renderSpeakerName(GuiGraphics graphics) {
        int nameX = DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        if (speakerTexture != null && !speakerTexture.isEmpty()) {
            nameX += SPEAKER_PORTRAIT_SIZE + TEXT_PADDING;
        }
        int nameY = height - DIALOGUE_AABB_HEIGHT - DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        
        graphics.drawString(font, speakerName, nameX, nameY, SPEAKER_NAME_COLOR, true);
    }
    
    /**
     * Renders the dialogue text with word wrapping.
     */
    private void renderDialogueText(GuiGraphics graphics) {
        if (dialogueText == null || dialogueText.isEmpty()) {
            return;
        }
        
        int textX = DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        int textY = height - DIALOGUE_AABB_HEIGHT - DIALOGUE_AABB_MARGIN + TEXT_PADDING;
        
        // Adjust for speaker portrait
        if (speakerTexture != null && !speakerTexture.isEmpty()) {
            textX += SPEAKER_PORTRAIT_SIZE + TEXT_PADDING;
        }
        
        // Adjust for speaker name
        if (speakerName != null && !speakerName.isEmpty()) {
            textY += font.lineHeight + 5;
        }
        
        int maxWidth = width - DIALOGUE_AABB_MARGIN * 2 - TEXT_PADDING * 2;
        if (speakerTexture != null && !speakerTexture.isEmpty()) {
            maxWidth -= SPEAKER_PORTRAIT_SIZE + TEXT_PADDING;
        }
        
        // Word wrap and render text
        List<String> lines = wrapText(dialogueText, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), textX, textY + i * (font.lineHeight + 2), TEXT_COLOR, false);
        }
    }
    
    /**
     * Wraps text to fit within the specified width.
     */
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            
            if (font.width(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    /**
     * Renders the "Press SPACE to continue" indicator.
     */
    private void renderContinueIndicator(GuiGraphics graphics) {
        String continueText = "Press SPACE to continue...";
        int textWidth = font.width(continueText);
        int x = width - DIALOGUE_AABB_MARGIN - TEXT_PADDING - textWidth;
        int y = height - DIALOGUE_AABB_MARGIN - TEXT_PADDING - font.lineHeight;
        
        // Pulsing effect
        float alpha = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0));
        int color = (int) (alpha * 255) << 24 | 0xFFFFFF;
        
        graphics.drawString(font, continueText, x, y, color, false);
    }
    
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        int keyCode = keyEvent.key();
        
        // SPACE or ENTER to continue (if no choices)
        if (!hasChoices && (keyCode == 32 || keyCode == 257)) { // SPACE or ENTER
            DialogueClientHandler.sendAdvanceDialogue();
            return true;
        }
        
        // ESC to close dialogue
        if (keyCode == 256) { // ESC
            DialogueClientHandler.sendCloseDialogue();
            onClose();
            return true;
        }
        
        // Number keys for quick choice selection
        if (hasChoices && keyCode >= 49 && keyCode <= 57) { // 1-9
            int choiceIndex = keyCode - 49;
            if (choiceIndex < choices.size()) {
                onChoiceSelected(choiceIndex);
                return true;
            }
        }
        
        return super.keyPressed(keyEvent);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game during dialogue
    }
    
    /**
     * Data class for choice information.
     */
    public record ChoiceData(String id, String text, String nextNodeId) {}
}
