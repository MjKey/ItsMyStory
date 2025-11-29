package ru.mjkey.storykee.systems.teleport;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SavedLocation class.
 * 
 * Requirements: 43.2
 */
class SavedLocationTest {
    
    private static final double EPSILON = 0.0001;
    private SavedLocation location;
    
    @BeforeEach
    void setUp() {
        location = new SavedLocation(
                "spawn",
                new Vec3(100, 64, 200),
                90.0f,
                -15.0f,
                ResourceLocation.withDefaultNamespace("overworld")
        );
    }
    
    @Test
    void testLocationCreation() {
        assertEquals("spawn", location.getId());
        assertEquals(100, location.getX(), EPSILON);
        assertEquals(64, location.getY(), EPSILON);
        assertEquals(200, location.getZ(), EPSILON);
        assertEquals(90.0f, location.getYaw(), EPSILON);
        assertEquals(-15.0f, location.getPitch(), EPSILON);
        assertEquals("minecraft:overworld", location.getDimension().toString());
    }
    
    @Test
    void testGetPosition() {
        Vec3 pos = location.getPosition();
        
        assertEquals(100, pos.x, EPSILON);
        assertEquals(64, pos.y, EPSILON);
        assertEquals(200, pos.z, EPSILON);
    }
    
    @Test
    void testWithId() {
        SavedLocation newLocation = location.withId("home");
        
        assertEquals("home", newLocation.getId());
        assertEquals(location.getX(), newLocation.getX(), EPSILON);
        assertEquals(location.getYaw(), newLocation.getYaw(), EPSILON);
    }
    
    @Test
    void testWithPosition() {
        Vec3 newPos = new Vec3(500, 100, 600);
        SavedLocation newLocation = location.withPosition(newPos);
        
        assertEquals(location.getId(), newLocation.getId());
        assertEquals(500, newLocation.getX(), EPSILON);
        assertEquals(100, newLocation.getY(), EPSILON);
        assertEquals(600, newLocation.getZ(), EPSILON);
        assertEquals(location.getYaw(), newLocation.getYaw(), EPSILON);
    }
    
    @Test
    void testWithRotation() {
        SavedLocation newLocation = location.withRotation(180.0f, 45.0f);
        
        assertEquals(location.getId(), newLocation.getId());
        assertEquals(location.getX(), newLocation.getX(), EPSILON);
        assertEquals(180.0f, newLocation.getYaw(), EPSILON);
        assertEquals(45.0f, newLocation.getPitch(), EPSILON);
    }
    
    @Test
    void testToString() {
        String str = location.toString();
        
        assertTrue(str.contains("spawn"));
        // Check that position values are present (format may vary)
        assertTrue(str.contains("100") || str.contains("100.0"));
        assertTrue(str.contains("64") || str.contains("64.0"));
        assertTrue(str.contains("200") || str.contains("200.0"));
    }
    
    @Test
    void testDifferentDimensions() {
        SavedLocation nether = new SavedLocation(
                "nether-portal",
                new Vec3(50, 32, 100),
                0, 0,
                ResourceLocation.withDefaultNamespace("the_nether")
        );
        
        assertEquals("minecraft:the_nether", nether.getDimension().toString());
    }
}
