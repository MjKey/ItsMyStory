package ru.mjkey.storykee.systems.hologram;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a hologram with text display.
 * 
 * Requirements: 48.1, 48.4
 */
public class Hologram {
    
    private final String id;
    private Vec3 position;
    private final List<String> lines;
    private int color;
    private float scale;
    private boolean visible;
    private boolean billboard; // Always face player
    private float lineSpacing;
    
    public Hologram(String id, Vec3 position) {
        this.id = id;
        this.position = position;
        this.lines = new ArrayList<>();
        this.color = 0xFFFFFF;
        this.scale = 1.0f;
        this.visible = true;
        this.billboard = true;
        this.lineSpacing = 0.25f;
    }
    
    public String getId() {
        return id;
    }
    
    public Vec3 getPosition() {
        return position;
    }
    
    public void setPosition(Vec3 position) {
        this.position = position;
    }
    
    public List<String> getLines() {
        return Collections.unmodifiableList(lines);
    }
    
    /**
     * Sets the hologram text (single line).
     * Requirement 48.2: Add text display
     * 
     * @param text Text to display
     */
    public void setText(String text) {
        lines.clear();
        if (text != null) {
            lines.add(text);
        }
    }
    
    /**
     * Sets multiple lines of text.
     * Requirement 48.4: Add multi-line support
     * 
     * @param textLines Lines of text
     */
    public void setLines(List<String> textLines) {
        lines.clear();
        if (textLines != null) {
            lines.addAll(textLines);
        }
    }
    
    /**
     * Adds a line of text.
     * 
     * @param line Line to add
     */
    public void addLine(String line) {
        if (line != null) {
            lines.add(line);
        }
    }
    
    /**
     * Sets a specific line.
     * 
     * @param index Line index
     * @param text Text to set
     */
    public void setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, text != null ? text : "");
        } else if (index == lines.size() && text != null) {
            lines.add(text);
        }
    }
    
    /**
     * Removes a line.
     * 
     * @param index Line index
     */
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
        }
    }
    
    /**
     * Clears all lines.
     */
    public void clearLines() {
        lines.clear();
    }
    
    public int getLineCount() {
        return lines.size();
    }
    
    public int getColor() {
        return color;
    }
    
    public void setColor(int color) {
        this.color = color;
    }
    
    public void setColor(int r, int g, int b) {
        this.color = (r << 16) | (g << 8) | b;
    }
    
    public float getScale() {
        return scale;
    }
    
    public void setScale(float scale) {
        this.scale = Math.max(0.1f, scale);
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isBillboard() {
        return billboard;
    }
    
    public void setBillboard(boolean billboard) {
        this.billboard = billboard;
    }
    
    public float getLineSpacing() {
        return lineSpacing;
    }
    
    public void setLineSpacing(float lineSpacing) {
        this.lineSpacing = lineSpacing;
    }
    
    /**
     * Gets the total height of the hologram.
     */
    public float getTotalHeight() {
        if (lines.isEmpty()) {
            return 0;
        }
        return (lines.size() - 1) * lineSpacing * scale;
    }
}
