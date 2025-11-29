package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * List GUI component.
 * Displays a scrollable list of items.
 * 
 * Requirements: 9.4
 */
public class ListComponent extends AbstractGUIComponent {
    
    private final List<ListItem> items;
    private int selectedIndex;
    private int scrollOffset;
    private int itemHeight;
    private int visibleItems;
    private Consumer<Integer> onSelect;
    
    private int backgroundColor;
    private int selectedColor;
    private int hoverColor;
    private int textColor;
    
    public ListComponent(String id, int x, int y, int width, int height, int itemHeight) {
        super(id, x, y, width, height);
        this.items = new ArrayList<>();
        this.selectedIndex = -1;
        this.scrollOffset = 0;
        this.itemHeight = itemHeight;
        this.visibleItems = height / itemHeight;
        
        this.backgroundColor = 0x80000000;
        this.selectedColor = 0x80FFFFFF;
        this.hoverColor = 0x40FFFFFF;
        this.textColor = 0xFFFFFF;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible) {
            return;
        }
        
        Minecraft client = Minecraft.getInstance();
        
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        
        int startIndex = scrollOffset;
        int endIndex = Math.min(startIndex + visibleItems, items.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            int itemY = y + (i - startIndex) * itemHeight;
            ListItem item = items.get(i);
            
            boolean isHovered = mouseX >= x && mouseX < x + width && 
                              mouseY >= itemY && mouseY < itemY + itemHeight;
            
            if (i == selectedIndex) {
                graphics.fill(x, itemY, x + width, itemY + itemHeight, selectedColor);
            } else if (isHovered) {
                graphics.fill(x, itemY, x + width, itemY + itemHeight, hoverColor);
            }
            
            graphics.drawString(
                client.font,
                item.getText(),
                x + 4,
                itemY + (itemHeight - client.font.lineHeight) / 2,
                textColor,
                false
            );
        }
        
        if (items.size() > visibleItems) {
            drawScrollbar(graphics);
        }
    }
    
    private void drawScrollbar(GuiGraphics graphics) {
        int scrollbarX = x + width - 6;
        int scrollbarWidth = 4;
        int scrollbarHeight = height;
        
        graphics.fill(scrollbarX, y, scrollbarX + scrollbarWidth, y + scrollbarHeight, 0x40000000);
        
        float scrollPercentage = (float) scrollOffset / (items.size() - visibleItems);
        int thumbHeight = Math.max(20, (visibleItems * scrollbarHeight) / items.size());
        int thumbY = y + (int) (scrollPercentage * (scrollbarHeight - thumbHeight));
        
        graphics.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0x80FFFFFF);
    }
    
    @Override
    public boolean isInteractive() {
        return true;
    }
    
    @Override
    public AbstractWidget asWidget() {
        return null;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0) {
            return false;
        }
        
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        int relativeY = (int) mouseY - y;
        int clickedIndex = scrollOffset + (relativeY / itemHeight);
        
        if (clickedIndex >= 0 && clickedIndex < items.size()) {
            setSelectedIndex(clickedIndex);
            if (onSelect != null) {
                onSelect.accept(clickedIndex);
            }
            return true;
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible) {
            return false;
        }
        
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        
        int maxScroll = Math.max(0, items.size() - visibleItems);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) amount));
        
        return true;
    }
    
    public void addItem(String text) {
        items.add(new ListItem(Component.literal(text)));
    }
    
    public void addItem(Component text) {
        items.add(new ListItem(text));
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
            
            if (selectedIndex == index) {
                selectedIndex = -1;
            } else if (selectedIndex > index) {
                selectedIndex--;
            }
            
            int maxScroll = Math.max(0, items.size() - visibleItems);
            scrollOffset = Math.min(scrollOffset, maxScroll);
        }
    }
    
    public void clearItems() {
        items.clear();
        selectedIndex = -1;
        scrollOffset = 0;
    }
    
    public ListItem getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }
    
    public List<ListItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public int getSelectedIndex() {
        return selectedIndex;
    }
    
    public void setSelectedIndex(int index) {
        if (index >= -1 && index < items.size()) {
            this.selectedIndex = index;
            
            if (index >= 0) {
                if (index < scrollOffset) {
                    scrollOffset = index;
                } else if (index >= scrollOffset + visibleItems) {
                    scrollOffset = index - visibleItems + 1;
                }
            }
        }
    }
    
    public ListItem getSelectedItem() {
        return getItem(selectedIndex);
    }
    
    public Consumer<Integer> getOnSelect() {
        return onSelect;
    }
    
    public void setOnSelect(Consumer<Integer> onSelect) {
        this.onSelect = onSelect;
    }
    
    public int getItemHeight() {
        return itemHeight;
    }
    
    public void setItemHeight(int itemHeight) {
        this.itemHeight = itemHeight;
        this.visibleItems = height / itemHeight;
    }
    
    public int getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public int getSelectedColor() {
        return selectedColor;
    }
    
    public void setSelectedColor(int selectedColor) {
        this.selectedColor = selectedColor;
    }
    
    public int getHoverColor() {
        return hoverColor;
    }
    
    public void setHoverColor(int hoverColor) {
        this.hoverColor = hoverColor;
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    
    public static class ListItem {
        private Component text;
        private Object data;
        
        public ListItem(Component text) {
            this.text = text;
        }
        
        public ListItem(String text) {
            this.text = Component.literal(text);
        }
        
        public Component getText() {
            return text;
        }
        
        public void setText(Component text) {
            this.text = text;
        }
        
        public void setText(String text) {
            this.text = Component.literal(text);
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
    }
}
