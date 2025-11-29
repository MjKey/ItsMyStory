package ru.mjkey.storykee.systems.hud;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base class for all HUD elements in the Storykee system.
 * HUD elements are rendered on the player's screen overlay.
 * 
 * Requirements: 9.1, 9.2
 */
public abstract class HUDElement {
    
    protected String id;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible;
    protected int zIndex;
    
    /**
     * Creates a new HUD element.
     * 
     * @param id Unique ResourceLocation for this element
     * @param x X position on screen
     * @param y Y position on screen
     * @param width Width of the element
     * @param height Height of the element
     */
    public HUDElement(String id, int x, int y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
        this.zIndex = 0;
    }
    
    /**
     * Renders this HUD element.
     * 
     * @param graphics The draw context for rendering
     * @param tickDelta Partial tick for smooth animations
     */
    public abstract void render(GuiGraphics graphics, float tickDelta);
    
    /**
     * Updates this HUD element. Called each frame.
     * 
     * @param tickDelta Partial tick for smooth animations
     */
    public void update(float tickDelta) {
        // Default implementation does nothing
        // Subclasses can override for animations or dynamic content
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getWidth() {
        return width;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public void show() {
        this.visible = true;
    }
    
    public void hide() {
        this.visible = false;
    }
    
    public int getZIndex() {
        return zIndex;
    }
    
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }
    
    /**
     * Checks if a point is within this element's bounds.
     * 
     * @param pointX X coordinate to check
     * @param pointY Y coordinate to check
     * @return true if the point is within bounds
     */
    public boolean containsPoint(int pointX, int pointY) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id='" + id + "', x=" + x + ", y=" + y + 
               ", width=" + width + ", height=" + height + ", visible=" + visible + "}";
    }
}
