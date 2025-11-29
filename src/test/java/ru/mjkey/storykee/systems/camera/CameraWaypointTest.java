package ru.mjkey.storykee.systems.camera;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CameraWaypoint class.
 * 
 * Requirements: 19.2
 */
class CameraWaypointTest {
    
    private static final float EPSILON = 0.0001f;
    
    @Test
    void testWaypointCreation() {
        Vec3 pos = new Vec3(100, 64, 200);
        CameraWaypoint waypoint = new CameraWaypoint(pos, -30, 45, 100);
        
        assertEquals(100, waypoint.getPosition().x, EPSILON);
        assertEquals(64, waypoint.getPosition().y, EPSILON);
        assertEquals(200, waypoint.getPosition().z, EPSILON);
        assertEquals(-30, waypoint.getPitch(), EPSILON);
        assertEquals(45, waypoint.getYaw(), EPSILON);
        assertEquals(100, waypoint.getDurationTicks());
        assertEquals(CameraWaypoint.InterpolationType.LINEAR, waypoint.getInterpolation());
    }
    
    @Test
    void testWaypointWithInterpolation() {
        Vec3 pos = new Vec3(0, 0, 0);
        CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 60, CameraWaypoint.InterpolationType.EASE_IN_OUT);
        
        assertEquals(CameraWaypoint.InterpolationType.EASE_IN_OUT, waypoint.getInterpolation());
    }
    
    @Test
    void testLinearInterpolation() {
        Vec3 pos = new Vec3(0, 0, 0);
        CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 100, CameraWaypoint.InterpolationType.LINEAR);
        
        assertEquals(0, waypoint.applyInterpolation(0), EPSILON);
        assertEquals(0.25f, waypoint.applyInterpolation(0.25f), EPSILON);
        assertEquals(0.5f, waypoint.applyInterpolation(0.5f), EPSILON);
        assertEquals(0.75f, waypoint.applyInterpolation(0.75f), EPSILON);
        assertEquals(1.0f, waypoint.applyInterpolation(1.0f), EPSILON);
    }
    
    @Test
    void testEaseInInterpolation() {
        Vec3 pos = new Vec3(0, 0, 0);
        CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 100, CameraWaypoint.InterpolationType.EASE_IN);
        
        assertEquals(0, waypoint.applyInterpolation(0), EPSILON);
        // Ease in starts slow: 0.5^2 = 0.25
        assertEquals(0.25f, waypoint.applyInterpolation(0.5f), EPSILON);
        assertEquals(1.0f, waypoint.applyInterpolation(1.0f), EPSILON);
    }
    
    @Test
    void testEaseOutInterpolation() {
        Vec3 pos = new Vec3(0, 0, 0);
        CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 100, CameraWaypoint.InterpolationType.EASE_OUT);
        
        assertEquals(0, waypoint.applyInterpolation(0), EPSILON);
        // Ease out ends slow: 1 - (1-0.5)^2 = 0.75
        assertEquals(0.75f, waypoint.applyInterpolation(0.5f), EPSILON);
        assertEquals(1.0f, waypoint.applyInterpolation(1.0f), EPSILON);
    }
    
    @Test
    void testEaseInOutInterpolation() {
        Vec3 pos = new Vec3(0, 0, 0);
        CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 100, CameraWaypoint.InterpolationType.EASE_IN_OUT);
        
        assertEquals(0, waypoint.applyInterpolation(0), EPSILON);
        // At 0.5, ease in/out should be at 0.5
        assertEquals(0.5f, waypoint.applyInterpolation(0.5f), EPSILON);
        assertEquals(1.0f, waypoint.applyInterpolation(1.0f), EPSILON);
        
        // First half should be slower than linear
        float quarter = waypoint.applyInterpolation(0.25f);
        assertTrue(quarter < 0.25f);
        
        // Second half should be faster than linear
        float threeQuarter = waypoint.applyInterpolation(0.75f);
        assertTrue(threeQuarter > 0.75f);
    }
    
    @Test
    void testSmoothInterpolation() {
        Vec3 pos = new Vec3(0, 0, 0);
        CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 100, CameraWaypoint.InterpolationType.SMOOTH);
        
        assertEquals(0, waypoint.applyInterpolation(0), EPSILON);
        // Smoothstep at 0.5: 0.5^2 * (3 - 2*0.5) = 0.25 * 2 = 0.5
        assertEquals(0.5f, waypoint.applyInterpolation(0.5f), EPSILON);
        assertEquals(1.0f, waypoint.applyInterpolation(1.0f), EPSILON);
    }
    
    @Test
    void testInterpolationBoundaries() {
        Vec3 pos = new Vec3(0, 0, 0);
        
        for (CameraWaypoint.InterpolationType type : CameraWaypoint.InterpolationType.values()) {
            CameraWaypoint waypoint = new CameraWaypoint(pos, 0, 0, 100, type);
            
            // All interpolation types should return 0 at start and 1 at end
            assertEquals(0, waypoint.applyInterpolation(0), EPSILON, 
                    "Interpolation " + type + " should return 0 at start");
            assertEquals(1.0f, waypoint.applyInterpolation(1.0f), EPSILON, 
                    "Interpolation " + type + " should return 1 at end");
        }
    }
}
