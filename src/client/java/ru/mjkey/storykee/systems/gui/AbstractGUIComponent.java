package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Abstract base class for GUI components.
 * Provides common functionality for all component types.
 * 
 * Requirements: 9.4
 */
public abstract class AbstractGUIComponent implements GUIComponent {
    
    protected final String id;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible;
    
    /**
     * Creates a new GUI component.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Width
     * @param height Height
     */
    public AbstractGUIComponent(String id, int x, int y, int width, int height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.visible = true;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public void update() {
        // Default implementation does nothing
        // Subclasses can override for custom update logic
    }
    
    @Override
    public boolean isInteractive() {
        // Default is non-interactive
        // Interactive components override this
        return false;
    }
    
    @Override
    public AbstractWidget asWidget() {
        // Default returns null for non-interactive components
        return null;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
    
    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
