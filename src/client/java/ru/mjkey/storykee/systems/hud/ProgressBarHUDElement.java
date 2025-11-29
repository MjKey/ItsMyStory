package ru.mjkey.storykee.systems.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * HUD element that displays a progress bar.
 * Supports customizable colors, borders, and optional text display.
 * 
 * Requirements: 9.1, 9.2
 */
public class ProgressBarHUDElement extends HUDElement {
    
    private float progress;
    private float targetProgress;
    private float animationSpeed;
    private int backgroundColor;
    private int foregroundColor;
    private int borderColor;
    private int borderWidth;
    private boolean showText;
    private String textFormat;
    private int textColor;
    private Direction direction;
    
    /**
     * Direction of progress bar fill.
     */
    public enum Direction {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        BOTTOM_TO_TOP,
        TOP_TO_BOTTOM
    }
    
    /**
     * Creates a new progress bar HUD element with default settings.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Bar width
     * @param height Bar height
     */
    public ProgressBarHUDElement(String id, int x, int y, int width, int height) {
        this(id, x, y, width, height, 0.0f, 0xFF333333, 0xFF00AA00);
    }
    
    /**
     * Creates a new progress bar HUD element with custom colors.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Bar width
     * @param height Bar height
     * @param progress Initial progress (0.0 to 1.0)
     * @param backgroundColor Background color (ARGB)
     * @param foregroundColor Foreground/fill color (ARGB)
     */
    public ProgressBarHUDElement(String id, int x, int y, int width, int height,
                                  float progress, int backgroundColor, int foregroundColor) {
        super(id, x, y, width, height);
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        this.targetProgress = this.progress;
        this.animationSpeed = 0.0f; // No animation by default
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        this.borderColor = 0xFF000000;
        this.borderWidth = 1;
        this.showText = false;
        this.textFormat = "%.0f%%";
        this.textColor = 0xFFFFFFFF;
        this.direction = Direction.LEFT_TO_RIGHT;
    }
    
    @Override
    public void update(float tickDelta) {
        // Animate progress if animation is enabled
        if (animationSpeed > 0 && Math.abs(progress - targetProgress) > 0.001f) {
            float delta = targetProgress - progress;
            float step = animationSpeed * tickDelta;
            
            if (Math.abs(delta) <= step) {
                progress = targetProgress;
            } else {
                progress += Math.signum(delta) * step;
            }
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, float tickDelta) {
        if (!visible) {
            return;
        }
        
        // Update animation
        update(tickDelta);
        
        // Draw border
        if (borderWidth > 0) {
            graphics.fill(x - borderWidth, y - borderWidth, 
                         x + width + borderWidth, y + height + borderWidth, borderColor);
        }
        
        // Draw background
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        
        // Draw progress fill
        if (progress > 0) {
            int fillX1 = x;
            int fillY1 = y;
            int fillX2 = x + width;
            int fillY2 = y + height;
            
            switch (direction) {
                case LEFT_TO_RIGHT:
                    fillX2 = x + (int) (width * progress);
                    break;
                case RIGHT_TO_LEFT:
                    fillX1 = x + width - (int) (width * progress);
                    break;
                case BOTTOM_TO_TOP:
                    fillY1 = y + height - (int) (height * progress);
                    break;
                case TOP_TO_BOTTOM:
                    fillY2 = y + (int) (height * progress);
                    break;
            }
            
            graphics.fill(fillX1, fillY1, fillX2, fillY2, foregroundColor);
        }
        
        // Draw text if enabled
        if (showText) {
            renderText(graphics);
        }
    }
    
    /**
     * Renders the progress text centered on the bar.
     */
    private void renderText(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        if (font == null) {
            return;
        }
        
        String text = String.format(textFormat, progress * 100);
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - font.lineHeight) / 2;
        
        graphics.drawString(font, text, textX, textY, textColor, true);
    }
    
    // Getters and setters
    
    public float getProgress() {
        return progress;
    }
    
    /**
     * Sets the progress value immediately.
     * 
     * @param progress Progress value (0.0 to 1.0)
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
        this.targetProgress = this.progress;
    }
    
    /**
     * Sets the target progress for animated transitions.
     * 
     * @param progress Target progress value (0.0 to 1.0)
     */
    public void setTargetProgress(float progress) {
        this.targetProgress = Math.max(0.0f, Math.min(1.0f, progress));
    }
    
    public float getTargetProgress() {
        return targetProgress;
    }
    
    public float getAnimationSpeed() {
        return animationSpeed;
    }
    
    /**
     * Sets the animation speed for progress transitions.
     * 
     * @param speed Animation speed (0 = instant, higher = faster)
     */
    public void setAnimationSpeed(float speed) {
        this.animationSpeed = Math.max(0.0f, speed);
    }
    
    public int getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public int getForegroundColor() {
        return foregroundColor;
    }
    
    public void setForegroundColor(int foregroundColor) {
        this.foregroundColor = foregroundColor;
    }
    
    public int getBorderColor() {
        return borderColor;
    }
    
    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }
    
    public int getBorderWidth() {
        return borderWidth;
    }
    
    public void setBorderWidth(int borderWidth) {
        this.borderWidth = Math.max(0, borderWidth);
    }
    
    public boolean isShowText() {
        return showText;
    }
    
    public void setShowText(boolean showText) {
        this.showText = showText;
    }
    
    public String getTextFormat() {
        return textFormat;
    }
    
    /**
     * Sets the text format string.
     * Use %.0f%% for percentage, %.2f for decimal, etc.
     * 
     * @param textFormat Format string for progress display
     */
    public void setTextFormat(String textFormat) {
        this.textFormat = textFormat != null ? textFormat : "%.0f%%";
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
    public void setDirection(Direction direction) {
        this.direction = direction;
    }
    
    /**
     * Convenience method to increment progress.
     * 
     * @param amount Amount to add to progress
     */
    public void incrementProgress(float amount) {
        setProgress(progress + amount);
    }
    
    /**
     * Convenience method to decrement progress.
     * 
     * @param amount Amount to subtract from progress
     */
    public void decrementProgress(float amount) {
        setProgress(progress - amount);
    }
    
    @Override
    public String toString() {
        return "ProgressBarHUDElement{id='" + id + "', progress=" + progress + 
               ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + 
               ", visible=" + visible + "}";
    }
}
