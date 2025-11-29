package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Label GUI component.
 * Displays non-interactive text on the GUI.
 * 
 * Requirements: 9.4
 */
public class LabelComponent extends AbstractGUIComponent {
    
    private Component text;
    private int color;
    private boolean shadow;
    private HorizontalAlignment horizontalAlignment;
    private VerticalAlignment verticalAlignment;
    
    public LabelComponent(String id, int x, int y, Component text) {
        this(id, x, y, text, 0xFFFFFF);
    }
    
    public LabelComponent(String id, int x, int y, Component text, int color) {
        super(id, x, y, 0, 0);
        this.text = text;
        this.color = color;
        this.shadow = true;
        this.horizontalAlignment = HorizontalAlignment.LEFT;
        this.verticalAlignment = VerticalAlignment.TOP;
        updateSize();
    }
    
    public LabelComponent(String id, int x, int y, String text) {
        this(id, x, y, Component.literal(text));
    }
    
    public LabelComponent(String id, int x, int y, String text, int color) {
        this(id, x, y, Component.literal(text), color);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) {
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        
        int renderX = x;
        int renderY = y;
        
        switch (horizontalAlignment) {
            case CENTER:
                renderX = x - width / 2;
                break;
            case RIGHT:
                renderX = x - width;
                break;
            case LEFT:
            default:
                break;
        }
        
        switch (verticalAlignment) {
            case MIDDLE:
                renderY = y - height / 2;
                break;
            case BOTTOM:
                renderY = y - height;
                break;
            case TOP:
            default:
                break;
        }
        
        if (shadow) {
            graphics.drawString(client.font, text, renderX, renderY, color, true);
        } else {
            graphics.drawString(client.font, text, renderX, renderY, color, false);
        }
    }
    
    private void updateSize() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.font != null) {
            this.width = client.font.width(text);
            this.height = client.font.lineHeight;
        }
    }
    
    public Component getText() {
        return text;
    }
    
    public void setText(Component text) {
        this.text = text;
        updateSize();
    }
    
    public void setText(String text) {
        setText(Component.literal(text));
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public boolean hasShadow() {
        return shadow;
    }
    
    public void setShadow(boolean shadow) {
        this.shadow = shadow;
    }
    
    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }
    
    public void setHorizontalAlignment(HorizontalAlignment alignment) {
        this.horizontalAlignment = alignment;
    }
    
    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }
    
    public void setVerticalAlignment(VerticalAlignment alignment) {
        this.verticalAlignment = alignment;
    }
    
    public enum HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }
    
    public enum VerticalAlignment {
        TOP, MIDDLE, BOTTOM
    }
}
