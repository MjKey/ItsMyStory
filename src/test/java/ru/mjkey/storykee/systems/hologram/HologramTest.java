package ru.mjkey.storykee.systems.hologram;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Hologram class.
 * 
 * Requirements: 48.1, 48.2, 48.3, 48.4
 */
class HologramTest {
    
    private static final float EPSILON = 0.0001f;
    private Hologram hologram;
    
    @BeforeEach
    void setUp() {
        hologram = new Hologram("test-holo", new Vec3(0, 100, 0));
    }
    
    @Test
    void testHologramCreation() {
        // Requirement 48.1: Create hologram spawning
        assertEquals("test-holo", hologram.getId());
        assertEquals(0, hologram.getPosition().x, EPSILON);
        assertEquals(100, hologram.getPosition().y, EPSILON);
        assertEquals(0, hologram.getPosition().z, EPSILON);
        assertTrue(hologram.isVisible());
        assertTrue(hologram.isBillboard());
    }
    
    @Test
    void testSetText() {
        // Requirement 48.2: Add text display
        hologram.setText("Hello World");
        
        assertEquals(1, hologram.getLineCount());
        assertEquals("Hello World", hologram.getLines().get(0));
    }
    
    @Test
    void testSetTextNull() {
        hologram.setText("Initial");
        hologram.setText(null);
        
        assertEquals(0, hologram.getLineCount());
    }
    
    @Test
    void testSetLines() {
        // Requirement 48.4: Add multi-line support
        List<String> lines = Arrays.asList("Line 1", "Line 2", "Line 3");
        hologram.setLines(lines);
        
        assertEquals(3, hologram.getLineCount());
        assertEquals("Line 1", hologram.getLines().get(0));
        assertEquals("Line 2", hologram.getLines().get(1));
        assertEquals("Line 3", hologram.getLines().get(2));
    }
    
    @Test
    void testSetLinesNull() {
        hologram.setText("Initial");
        hologram.setLines(null);
        
        assertEquals(0, hologram.getLineCount());
    }
    
    @Test
    void testAddLine() {
        hologram.addLine("First");
        hologram.addLine("Second");
        
        assertEquals(2, hologram.getLineCount());
        assertEquals("First", hologram.getLines().get(0));
        assertEquals("Second", hologram.getLines().get(1));
    }
    
    @Test
    void testAddLineNull() {
        hologram.addLine(null);
        assertEquals(0, hologram.getLineCount());
    }
    
    @Test
    void testSetLine() {
        hologram.setLines(Arrays.asList("A", "B", "C"));
        
        hologram.setLine(1, "Modified");
        
        assertEquals("A", hologram.getLines().get(0));
        assertEquals("Modified", hologram.getLines().get(1));
        assertEquals("C", hologram.getLines().get(2));
    }
    
    @Test
    void testSetLineAtEnd() {
        hologram.setLines(Arrays.asList("A", "B"));
        
        // Setting at index == size should append
        hologram.setLine(2, "C");
        
        assertEquals(3, hologram.getLineCount());
        assertEquals("C", hologram.getLines().get(2));
    }
    
    @Test
    void testSetLineOutOfBounds() {
        hologram.setLines(Arrays.asList("A", "B"));
        
        // Setting beyond size should do nothing
        hologram.setLine(5, "X");
        
        assertEquals(2, hologram.getLineCount());
    }
    
    @Test
    void testRemoveLine() {
        hologram.setLines(Arrays.asList("A", "B", "C"));
        
        hologram.removeLine(1);
        
        assertEquals(2, hologram.getLineCount());
        assertEquals("A", hologram.getLines().get(0));
        assertEquals("C", hologram.getLines().get(1));
    }
    
    @Test
    void testRemoveLineOutOfBounds() {
        hologram.setLines(Arrays.asList("A", "B"));
        
        hologram.removeLine(-1);
        hologram.removeLine(5);
        
        assertEquals(2, hologram.getLineCount());
    }
    
    @Test
    void testClearLines() {
        hologram.setLines(Arrays.asList("A", "B", "C"));
        
        hologram.clearLines();
        
        assertEquals(0, hologram.getLineCount());
    }
    
    @Test
    void testPosition() {
        // Requirement 48.3: Implement updates
        Vec3 newPos = new Vec3(10, 200, 30);
        hologram.setPosition(newPos);
        
        assertEquals(10, hologram.getPosition().x, EPSILON);
        assertEquals(200, hologram.getPosition().y, EPSILON);
        assertEquals(30, hologram.getPosition().z, EPSILON);
    }
    
    @Test
    void testColor() {
        hologram.setColor(0xFF0000); // Red
        assertEquals(0xFF0000, hologram.getColor());
        
        hologram.setColor(255, 128, 0); // Orange
        assertEquals(0xFF8000, hologram.getColor());
    }
    
    @Test
    void testScale() {
        hologram.setScale(2.0f);
        assertEquals(2.0f, hologram.getScale(), EPSILON);
        
        // Minimum scale is 0.1
        hologram.setScale(0.05f);
        assertEquals(0.1f, hologram.getScale(), EPSILON);
    }
    
    @Test
    void testVisibility() {
        assertTrue(hologram.isVisible());
        
        hologram.setVisible(false);
        assertFalse(hologram.isVisible());
        
        hologram.setVisible(true);
        assertTrue(hologram.isVisible());
    }
    
    @Test
    void testBillboard() {
        assertTrue(hologram.isBillboard());
        
        hologram.setBillboard(false);
        assertFalse(hologram.isBillboard());
    }
    
    @Test
    void testLineSpacing() {
        assertEquals(0.25f, hologram.getLineSpacing(), EPSILON);
        
        hologram.setLineSpacing(0.5f);
        assertEquals(0.5f, hologram.getLineSpacing(), EPSILON);
    }
    
    @Test
    void testTotalHeight() {
        hologram.setLines(Arrays.asList("A", "B", "C"));
        hologram.setLineSpacing(0.25f);
        hologram.setScale(1.0f);
        
        // 3 lines = 2 gaps * 0.25 spacing * 1.0 scale = 0.5
        assertEquals(0.5f, hologram.getTotalHeight(), EPSILON);
    }
    
    @Test
    void testTotalHeightWithScale() {
        hologram.setLines(Arrays.asList("A", "B", "C"));
        hologram.setLineSpacing(0.25f);
        hologram.setScale(2.0f);
        
        // 3 lines = 2 gaps * 0.25 spacing * 2.0 scale = 1.0
        assertEquals(1.0f, hologram.getTotalHeight(), EPSILON);
    }
    
    @Test
    void testTotalHeightEmpty() {
        assertEquals(0, hologram.getTotalHeight(), EPSILON);
    }
    
    @Test
    void testLinesUnmodifiable() {
        hologram.setLines(Arrays.asList("A", "B"));
        
        List<String> lines = hologram.getLines();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            lines.add("C");
        });
    }
}
