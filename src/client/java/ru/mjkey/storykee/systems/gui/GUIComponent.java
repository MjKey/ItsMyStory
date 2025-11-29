package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Base interface for all GUI components in the Storykee system.
 * 
 * GUI components can be either interactive (buttons, text fields) or
 * non-interactive (labels, images). Interactive components are rendered
 * as Minecraft widgets, while non-interactive components are rendered directly.
 * 
 * Requirements: 9.4
 */
public interface GUIComponent {
    
    /**
     * Gets the unique ResourceLocation for this component.
     * 
     * @return Component ID
     */
    String getId();
    
    /**
     * Renders this component.
     * 
     * @param graphics Graphics context
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param delta Partial tick
     */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float delta);
    
    /**
     * Updates this component. Called each tick.
     */
    void update();
    
    /**
     * Checks if this component is interactive (can receive input).
     * 
     * @return true if interactive
     */
    boolean isInteractive();
    
    /**
     * Gets this component as a Minecraft widget.
     * Only applicable for interactive components.
     * 
     * @return The widget, or null if not interactive
     */
    AbstractWidget asWidget();
    
    /**
     * Checks if this component is visible.
     * 
     * @return true if visible
     */
    boolean isVisible();
    
    /**
     * Sets the visibility of this component.
     * 
     * @param visible Visibility state
     */
    void setVisible(boolean visible);
    
    /**
     * Gets the X position of this component.
     * 
     * @return X position
     */
    int getX();
    
    /**
     * Gets the Y position of this component.
     * 
     * @return Y position
     */
    int getY();
    
    /**
     * Sets the position of this component.
     * 
     * @param x X position
     * @param y Y position
     */
    void setPosition(int x, int y);
    
    /**
     * Gets the width of this component.
     * 
     * @return Width
     */
    int getWidth();
    
    /**
     * Gets the height of this component.
     * 
     * @return Height
     */
    int getHeight();
    
    /**
     * Sets the size of this component.
     * 
     * @param width Width
     * @param height Height
     */
    void setSize(int width, int height);
}
