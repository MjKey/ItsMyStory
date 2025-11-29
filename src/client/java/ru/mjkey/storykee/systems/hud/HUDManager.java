package ru.mjkey.storykee.systems.hud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HUD elements for the Storykee system.
 * Handles registration, visibility, and lifecycle of HUD elements.
 * 
 * This is a client-side manager that tracks HUD elements per player.
 * 
 * Requirements: 9.1
 */
public class HUDManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HUDManager.class);
    
    private static HUDManager instance;
    
    // All registered HUD elements by ID
    private final Map<String, HUDElement> elements;
    
    // Elements grouped by layer/category for organized rendering
    private final Map<String, Set<String>> elementGroups;
    
    // Sorted list of elements for rendering (by z-index)
    private final List<HUDElement> renderOrder;
    private boolean renderOrderDirty;
    
    private HUDManager() {
        this.elements = new ConcurrentHashMap<>();
        this.elementGroups = new ConcurrentHashMap<>();
        this.renderOrder = new ArrayList<>();
        this.renderOrderDirty = false;
    }
    
    /**
     * Gets the singleton instance of HUDManager.
     * 
     * @return The HUDManager instance
     */
    public static HUDManager getInstance() {
        if (instance == null) {
            instance = new HUDManager();
        }
        return instance;
    }
    
    /**
     * Registers a HUD element.
     * 
     * @param element The element to register
     * @return true if registered successfully, false if ID already exists
     */
    public boolean registerElement(HUDElement element) {
        if (element == null || element.getId() == null) {
            LOGGER.warn("Cannot register null element or element with null ID");
            return false;
        }
        
        if (elements.containsKey(element.getId())) {
            LOGGER.warn("HUD element with ID '{}' already exists", element.getId());
            return false;
        }
        
        elements.put(element.getId(), element);
        renderOrderDirty = true;
        LOGGER.debug("Registered HUD element: {}", element.getId());
        return true;
    }
    
    /**
     * Registers a HUD element in a specific group.
     * 
     * @param element The element to register
     * @param group The group name
     * @return true if registered successfully
     */
    public boolean registerElement(HUDElement element, String group) {
        if (!registerElement(element)) {
            return false;
        }
        
        if (group != null && !group.isEmpty()) {
            elementGroups.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet())
                        .add(element.getId());
        }
        
        return true;
    }
    
    /**
     * Unregisters a HUD element by ID.
     * 
     * @param elementId The element ID to unregister
     * @return The removed element, or null if not found
     */
    public HUDElement unregisterElement(String elementId) {
        HUDElement removed = elements.remove(elementId);
        
        if (removed != null) {
            // Remove from all groups
            for (Set<String> group : elementGroups.values()) {
                group.remove(elementId);
            }
            renderOrderDirty = true;
            LOGGER.debug("Unregistered HUD element: {}", elementId);
        }
        
        return removed;
    }
    
    /**
     * Gets a HUD element by ID.
     * 
     * @param elementId The element ID
     * @return The element, or null if not found
     */
    public HUDElement getElement(String elementId) {
        return elements.get(elementId);
    }
    
    /**
     * Gets a HUD element by ID with type casting.
     * 
     * @param elementId The element ID
     * @param type The expected element type
     * @return The element cast to the specified type, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    public <T extends HUDElement> T getElement(String elementId, Class<T> type) {
        HUDElement element = elements.get(elementId);
        if (element != null && type.isInstance(element)) {
            return (T) element;
        }
        return null;
    }
    
    /**
     * Checks if an element with the given ID exists.
     * 
     * @param elementId The element ID
     * @return true if the element exists
     */
    public boolean hasElement(String elementId) {
        return elements.containsKey(elementId);
    }
    
    /**
     * Gets all registered elements.
     * 
     * @return Unmodifiable collection of all elements
     */
    public Collection<HUDElement> getAllElements() {
        return Collections.unmodifiableCollection(elements.values());
    }
    
    /**
     * Gets all element IDs in a group.
     * 
     * @param group The group name
     * @return Set of element IDs in the group
     */
    public Set<String> getElementsInGroup(String group) {
        Set<String> groupElements = elementGroups.get(group);
        return groupElements != null ? Collections.unmodifiableSet(groupElements) : Collections.emptySet();
    }
    
    /**
     * Shows a HUD element.
     * 
     * @param elementId The element ID
     */
    public void showElement(String elementId) {
        HUDElement element = elements.get(elementId);
        if (element != null) {
            element.setVisible(true);
            LOGGER.debug("Showing HUD element: {}", elementId);
        }
    }
    
    /**
     * Hides a HUD element.
     * 
     * @param elementId The element ID
     */
    public void hideElement(String elementId) {
        HUDElement element = elements.get(elementId);
        if (element != null) {
            element.setVisible(false);
            LOGGER.debug("Hiding HUD element: {}", elementId);
        }
    }
    
    /**
     * Toggles visibility of a HUD element.
     * 
     * @param elementId The element ID
     * @return The new visibility state, or false if element not found
     */
    public boolean toggleElement(String elementId) {
        HUDElement element = elements.get(elementId);
        if (element != null) {
            element.setVisible(!element.isVisible());
            return element.isVisible();
        }
        return false;
    }
    
    /**
     * Shows all elements in a group.
     * 
     * @param group The group name
     */
    public void showGroup(String group) {
        Set<String> groupElements = elementGroups.get(group);
        if (groupElements != null) {
            for (String elementId : groupElements) {
                showElement(elementId);
            }
        }
    }
    
    /**
     * Hides all elements in a group.
     * 
     * @param group The group name
     */
    public void hideGroup(String group) {
        Set<String> groupElements = elementGroups.get(group);
        if (groupElements != null) {
            for (String elementId : groupElements) {
                hideElement(elementId);
            }
        }
    }
    
    /**
     * Shows all HUD elements.
     */
    public void showAll() {
        for (HUDElement element : elements.values()) {
            element.setVisible(true);
        }
    }
    
    /**
     * Hides all HUD elements.
     */
    public void hideAll() {
        for (HUDElement element : elements.values()) {
            element.setVisible(false);
        }
    }
    
    /**
     * Gets elements sorted by z-index for rendering.
     * 
     * @return List of elements in render order
     */
    public List<HUDElement> getElementsInRenderOrder() {
        if (renderOrderDirty) {
            updateRenderOrder();
        }
        return Collections.unmodifiableList(renderOrder);
    }
    
    /**
     * Updates the render order based on z-index.
     */
    private void updateRenderOrder() {
        renderOrder.clear();
        renderOrder.addAll(elements.values());
        renderOrder.sort(Comparator.comparingInt(HUDElement::getZIndex));
        renderOrderDirty = false;
    }
    
    /**
     * Marks the render order as needing update.
     * Call this after changing z-index of elements.
     */
    public void invalidateRenderOrder() {
        renderOrderDirty = true;
    }
    
    /**
     * Updates all HUD elements.
     * 
     * @param tickDelta Partial tick for animations
     */
    public void updateAll(float tickDelta) {
        for (HUDElement element : elements.values()) {
            if (element.isVisible()) {
                element.update(tickDelta);
            }
        }
    }
    
    /**
     * Clears all HUD elements.
     */
    public void clear() {
        elements.clear();
        elementGroups.clear();
        renderOrder.clear();
        renderOrderDirty = false;
        LOGGER.debug("Cleared all HUD elements");
    }
    
    /**
     * Clears all elements in a specific group.
     * 
     * @param group The group name
     */
    public void clearGroup(String group) {
        Set<String> groupElements = elementGroups.remove(group);
        if (groupElements != null) {
            for (String elementId : groupElements) {
                elements.remove(elementId);
            }
            renderOrderDirty = true;
            LOGGER.debug("Cleared HUD group: {}", group);
        }
    }
    
    /**
     * Gets the number of registered elements.
     * 
     * @return Element count
     */
    public int getElementCount() {
        return elements.size();
    }
    
    /**
     * Gets the number of visible elements.
     * 
     * @return Visible element count
     */
    public int getVisibleElementCount() {
        return (int) elements.values().stream().filter(HUDElement::isVisible).count();
    }
    
    // Factory methods for convenience
    
    /**
     * Creates and registers a text HUD element.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param text Text to display
     * @return The created element
     */
    public TextHUDElement createTextElement(String id, int x, int y, String text) {
        TextHUDElement element = new TextHUDElement(id, x, y, text);
        registerElement(element);
        return element;
    }
    
    /**
     * Creates and registers a text HUD element with color.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param text Text to display
     * @param color ARGB color
     * @return The created element
     */
    public TextHUDElement createTextElement(String id, int x, int y, String text, int color) {
        TextHUDElement element = new TextHUDElement(id, x, y, text, color);
        registerElement(element);
        return element;
    }
    
    /**
     * Creates and registers a localized text HUD element.
     * The text will be resolved from the current language using the translation key.
     * Requirement 26.5: Support translation keys in HUD elements.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param translationKey Translation key for localized text
     * @return The created element
     */
    public TextHUDElement createLocalizedTextElement(String id, int x, int y, String translationKey) {
        TextHUDElement element = new TextHUDElement(id, x, y, "");
        element.setTranslationKey(translationKey);
        registerElement(element);
        return element;
    }
    
    /**
     * Creates and registers a localized text HUD element with color.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param translationKey Translation key for localized text
     * @param color ARGB color
     * @return The created element
     */
    public TextHUDElement createLocalizedTextElement(String id, int x, int y, String translationKey, int color) {
        TextHUDElement element = new TextHUDElement(id, x, y, "", color);
        element.setTranslationKey(translationKey);
        registerElement(element);
        return element;
    }
    
    /**
     * Creates and registers an image HUD element.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Display width
     * @param height Display height
     * @param texturePath Texture path
     * @return The created element
     */
    public ImageHUDElement createImageElement(String id, int x, int y, int width, int height, String texturePath) {
        ImageHUDElement element = ImageHUDElement.fromPath(id, x, y, width, height, texturePath);
        registerElement(element);
        return element;
    }
    
    /**
     * Creates and registers a progress bar HUD element.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Bar width
     * @param height Bar height
     * @return The created element
     */
    public ProgressBarHUDElement createProgressBar(String id, int x, int y, int width, int height) {
        ProgressBarHUDElement element = new ProgressBarHUDElement(id, x, y, width, height);
        registerElement(element);
        return element;
    }
    
    /**
     * Creates and registers a progress bar HUD element with custom colors.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Bar width
     * @param height Bar height
     * @param backgroundColor Background color
     * @param foregroundColor Fill color
     * @return The created element
     */
    public ProgressBarHUDElement createProgressBar(String id, int x, int y, int width, int height,
                                                    int backgroundColor, int foregroundColor) {
        ProgressBarHUDElement element = new ProgressBarHUDElement(id, x, y, width, height, 0.0f, backgroundColor, foregroundColor);
        registerElement(element);
        return element;
    }
}
