package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Button GUI component.
 * Provides an interactive button that can trigger actions when clicked.
 * 
 * Requirements: 9.4
 */
public class ButtonComponent extends AbstractGUIComponent {
    
    private final Button button;
    private Component label;
    private Runnable onPress;
    
    public ButtonComponent(String id, int x, int y, int width, int height, Component label, Runnable onPress) {
        super(id, x, y, width, height);
        this.label = label;
        this.onPress = onPress;
        
        this.button = Button.builder(label, btn -> {
            if (this.onPress != null) {
                this.onPress.run();
            }
        }).bounds(x, y, width, height).build();
    }
    
    public ButtonComponent(String id, int x, int y, int width, int height, String label, Runnable onPress) {
        this(id, x, y, width, height, Component.literal(label), onPress);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (visible) {
            button.render(graphics, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public boolean isInteractive() {
        return true;
    }
    
    @Override
    public AbstractWidget asWidget() {
        return button;
    }
    
    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        button.setPosition(x, y);
    }
    
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        button.setWidth(width);
        button.setHeight(height);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        button.visible = visible;
    }
    
    public Component getLabel() {
        return label;
    }
    
    public void setLabel(Component label) {
        this.label = label;
        button.setMessage(label);
    }
    
    public void setLabel(String label) {
        setLabel(Component.literal(label));
    }
    
    public Runnable getOnPress() {
        return onPress;
    }
    
    public void setOnPress(Runnable onPress) {
        this.onPress = onPress;
    }
    
    public void setEnabled(boolean enabled) {
        button.active = enabled;
    }
    
    public boolean isEnabled() {
        return button.active;
    }
}
