package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Text field GUI component.
 * Provides an interactive text input field.
 * 
 * Requirements: 9.4
 */
public class TextFieldComponent extends AbstractGUIComponent {
    
    private final EditBox editBox;
    private Consumer<String> onChanged;
    
    public TextFieldComponent(String id, int x, int y, int width, int height) {
        this(id, x, y, width, height, Component.empty());
    }
    
    public TextFieldComponent(String id, int x, int y, int width, int height, Component placeholder) {
        super(id, x, y, width, height);
        
        Minecraft client = Minecraft.getInstance();
        this.editBox = new EditBox(client.font, x, y, width, height, placeholder);
        
        this.editBox.setResponder(text -> {
            if (onChanged != null) {
                onChanged.accept(text);
            }
        });
    }
    
    public TextFieldComponent(String id, int x, int y, int width, int height, String placeholder) {
        this(id, x, y, width, height, Component.literal(placeholder));
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (visible) {
            editBox.render(graphics, mouseX, mouseY, delta);
        }
    }
    
    @Override
    public void update() {
        // EditBox doesn't have tick() in this version
    }
    
    @Override
    public boolean isInteractive() {
        return true;
    }
    
    @Override
    public AbstractWidget asWidget() {
        return editBox;
    }
    
    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        editBox.setPosition(x, y);
    }
    
    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        editBox.setWidth(width);
        editBox.setHeight(height);
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        editBox.setVisible(visible);
    }
    
    public String getText() {
        return editBox.getValue();
    }
    
    public void setText(String text) {
        editBox.setValue(text);
    }
    
    public Component getPlaceholder() {
        return editBox.getMessage();
    }
    
    public void setPlaceholder(Component placeholder) {
        editBox.setHint(placeholder);
    }
    
    public void setPlaceholder(String placeholder) {
        setPlaceholder(Component.literal(placeholder));
    }
    
    public Consumer<String> getOnChanged() {
        return onChanged;
    }
    
    public void setOnChanged(Consumer<String> onChanged) {
        this.onChanged = onChanged;
    }
    
    public void setMaxLength(int maxLength) {
        editBox.setMaxLength(maxLength);
    }
    
    public void setEditable(boolean editable) {
        editBox.setEditable(editable);
    }
    
    public boolean isEditable() {
        return editBox.isActive();
    }
    
    public void setFocused(boolean focused) {
        editBox.setFocused(focused);
    }
    
    public boolean isFocused() {
        return editBox.isFocused();
    }
}
