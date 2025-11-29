package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all custom GUI screens in the Storykee system.
 * Extends Minecraft's Screen class to provide story-specific GUI functionality.
 * 
 * Requirements: 9.4
 */
public abstract class StoryGUI extends Screen {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryGUI.class);
    
    protected final String id;
    protected final UUID playerId;
    protected final List<GUIComponent> components;
    protected final Screen previousScreen;
    
    protected boolean initialized;
    protected boolean inputEnabled;
    
    /**
     * Creates a new StoryGUI.
     * 
     * @param id Unique ResourceLocation for this GUI
     * @param title Title text displayed on the GUI
     * @param playerId UUID of the player viewing this GUI
     */
    public StoryGUI(String id, Component title, UUID playerId) {
        this(id, title, playerId, null);
    }
    
    /**
     * Creates a new StoryGUI with a previous screen to return to.
     * 
     * @param id Unique ResourceLocation for this GUI
     * @param title Title text displayed on the GUI
     * @param playerId UUID of the player viewing this GUI
     * @param previousScreen Screen to return to when closed
     */
    public StoryGUI(String id, Component title, UUID playerId, Screen previousScreen) {
        super(title);
        this.id = id;
        this.playerId = playerId;
        this.previousScreen = previousScreen;
        this.components = new ArrayList<>();
        this.initialized = false;
        this.inputEnabled = true;
    }
    
    @Override
    protected void init() {
        super.init();
        
        if (!initialized) {
            onOpen();
            initialized = true;
        }
        
        components.clear();
        clearWidgets();
        
        initComponents();
        
        for (GUIComponent component : components) {
            if (component.isInteractive() && component.asWidget() != null) {
                addRenderableWidget(component.asWidget());
            }
        }
        
        LOGGER.debug("Initialized StoryGUI '{}' for player {}", id, playerId);
    }
    
    /**
     * Initialize GUI components.
     * Override this method to add custom components to the GUI.
     */
    protected abstract void initComponents();
    
    /**
     * Called when the GUI is first opened.
     */
    protected void onOpen() {
    }
    
    /**
     * Called when the GUI is closed.
     */
    protected void onClosed() {
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        
        for (GUIComponent component : components) {
            if (!component.isInteractive() && component.isVisible()) {
                component.render(graphics, mouseX, mouseY, delta);
            }
        }
        
        super.render(graphics, mouseX, mouseY, delta);
        
        renderTitle(graphics);
        renderOverlay(graphics, mouseX, mouseY, delta);
    }
    
    /**
     * Renders the GUI title.
     */
    protected void renderTitle(GuiGraphics graphics) {
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }
    
    /**
     * Renders custom overlay elements.
     */
    protected void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }
    
    @Override
    public void tick() {
        super.tick();
        for (GUIComponent component : components) {
            component.update();
        }
    }
    
    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!inputEnabled) {
            return false;
        }
        if (handleKeyInput(keyEvent)) {
            return true;
        }
        return super.keyPressed(keyEvent);
    }
    
    /**
     * Handles key input for the GUI.
     * Override to implement custom key handling.
     * 
     * @param keyEvent Key event
     * @return true if the input was handled
     */
    protected boolean handleKeyInput(KeyEvent keyEvent) {
        return false;
    }
    
    @Override
    public void onClose() {
        onClosed();
        if (previousScreen != null && minecraft != null) {
            minecraft.setScreen(previousScreen);
        } else {
            super.onClose();
        }
        LOGGER.debug("Closed StoryGUI '{}' for player {}", id, playerId);
    }
    
    public void addComponent(GUIComponent component) {
        components.add(component);
        if (initialized && component.isInteractive() && component.asWidget() != null) {
            addRenderableWidget(component.asWidget());
        }
    }
    
    public void removeComponent(GUIComponent component) {
        components.remove(component);
        if (component.isInteractive() && component.asWidget() != null) {
            removeWidget(component.asWidget());
        }
    }
    
    public GUIComponent getComponent(String componentId) {
        for (GUIComponent component : components) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends GUIComponent> T getComponent(String componentId, Class<T> type) {
        GUIComponent component = getComponent(componentId);
        if (component != null && type.isInstance(component)) {
            return (T) component;
        }
        return null;
    }
    
    public List<GUIComponent> getComponents() {
        return new ArrayList<>(components);
    }
    
    public void clearComponents() {
        components.clear();
        clearWidgets();
    }
    
    public String getId() {
        return id;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean isInputEnabled() {
        return inputEnabled;
    }
    
    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }
    
    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
