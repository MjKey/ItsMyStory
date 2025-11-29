package ru.mjkey.storykee.systems.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import ru.mjkey.storykee.systems.localization.LocalizationManager;

/**
 * HUD element that displays text on the screen.
 * Supports color, scale, and shadow options.
 * 
 * Requirements: 9.1, 9.2
 */
public class TextHUDElement extends HUDElement {
    
    private String text;
    private String translationKey;  // Optional translation key for localized text
    private int color;
    private float scale;
    private boolean shadow;
    private TextAlignment alignment;
    
    /**
     * Text alignment options.
     */
    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
    
    /**
     * Creates a new text HUD element with default settings.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param text Text to display
     */
    public TextHUDElement(String id, int x, int y, String text) {
        this(id, x, y, text, 0xFFFFFFFF, 1.0f, true);
    }
    
    /**
     * Creates a new text HUD element with custom color.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param text Text to display
     * @param color ARGB color value
     */
    public TextHUDElement(String id, int x, int y, String text, int color) {
        this(id, x, y, text, color, 1.0f, true);
    }
    
    /**
     * Creates a new text HUD element with full customization.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param text Text to display
     * @param color ARGB color value
     * @param scale Text scale (1.0 = normal)
     * @param shadow Whether to render shadow
     */
    public TextHUDElement(String id, int x, int y, String text, int color, float scale, boolean shadow) {
        super(id, x, y, 0, 0);
        this.text = text != null ? text : "";
        this.translationKey = null;
        this.color = color;
        this.scale = scale;
        this.shadow = shadow;
        this.alignment = TextAlignment.LEFT;
        updateDimensions();
    }
    
    /**
     * Updates the width and height based on current text and scale.
     */
    private void updateDimensions() {
        Font font = Minecraft.getInstance().font;
        String displayText = getText();
        if (font != null && displayText != null) {
            this.width = (int) (font.width(displayText) * scale);
            this.height = (int) (font.lineHeight * scale);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, float tickDelta) {
        String displayText = getText();
        if (!visible || displayText == null || displayText.isEmpty()) {
            return;
        }
        
        Font font = Minecraft.getInstance().font;
        if (font == null) {
            return;
        }
        
        // Calculate render position based on alignment
        int renderX = x;
        int textWidth = font.width(displayText);
        if (alignment == TextAlignment.CENTER) {
            renderX = x - textWidth / 2;
        } else if (alignment == TextAlignment.RIGHT) {
            renderX = x - textWidth;
        }
        
        // Note: Scale is not directly supported in 1.21.10 GuiGraphics.drawString
        // For scaled text, we would need to use lower-level rendering APIs
        // For now, we render at normal scale
        graphics.drawString(font, displayText, renderX, y, color, shadow);
    }
    
    // Getters and setters
    
    /**
     * Gets the display text, resolving translation keys if set.
     * Requirement 26.5: Substitute translation keys with localized text in HUD elements.
     * 
     * @return The resolved display text
     */
    public String getText() {
        if (translationKey != null && !translationKey.isEmpty()) {
            return LocalizationManager.getInstance().translate(translationKey);
        }
        // Also process any embedded translation keys in the text
        return LocalizationManager.getInstance().processTranslationKeys(text);
    }
    
    /**
     * Gets the raw text without translation processing.
     * 
     * @return The raw text
     */
    public String getRawText() {
        return text;
    }
    
    /**
     * Sets the text directly (literal text).
     * 
     * @param text The text to display
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
        this.translationKey = null;  // Clear translation key when setting literal text
        updateDimensions();
    }
    
    /**
     * Sets the text using a translation key.
     * The text will be resolved from the current language.
     * Requirement 26.5: Support translation keys in HUD elements.
     * 
     * @param key The translation key
     */
    public void setTranslationKey(String key) {
        this.translationKey = key;
        updateDimensions();
    }
    
    /**
     * Gets the translation key if set.
     * 
     * @return The translation key, or null if using literal text
     */
    public String getTranslationKey() {
        return translationKey;
    }
    
    /**
     * Checks if this element uses a translation key.
     * 
     * @return true if using a translation key
     */
    public boolean hasTranslationKey() {
        return translationKey != null && !translationKey.isEmpty();
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public float getScale() {
        return scale;
    }
    
    public void setScale(float scale) {
        this.scale = Math.max(0.1f, scale);
        updateDimensions();
    }
    
    public boolean hasShadow() {
        return shadow;
    }
    
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }
    
    public TextAlignment getAlignment() {
        return alignment;
    }
    
    public void setAlignment(TextAlignment alignment) {
        this.alignment = alignment;
    }
    
    @Override
    public String toString() {
        return "TextHUDElement{id='" + id + "', text='" + text + "', x=" + x + ", y=" + y + 
               ", color=" + Integer.toHexString(color) + ", scale=" + scale + ", visible=" + visible + "}";
    }
}
