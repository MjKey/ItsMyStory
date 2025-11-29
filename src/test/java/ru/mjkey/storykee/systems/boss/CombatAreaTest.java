package ru.mjkey.storykee.systems.boss;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CombatArea class.
 * 
 * Requirements: 18.6
 */
class CombatAreaTest {
    
    private static final double EPSILON = 0.0001;
    
    @Test
    void testSphereCreation() {
        CombatArea area = new CombatArea(new Vec3(100, 64, 200), 50);
        
        assertEquals(CombatArea.Shape.SPHERE, area.getShape());
        assertEquals(100, area.getCenter().x, EPSILON);
        assertEquals(64, area.getCenter().y, EPSILON);
        assertEquals(200, area.getCenter().z, EPSILON);
        assertEquals(50, area.getRadius(), EPSILON);
    }
    
    @Test
    void testSphereCreationWithCoordinates() {
        CombatArea area = new CombatArea(100, 64, 200, 50);
        
        assertEquals(CombatArea.Shape.SPHERE, area.getShape());
        assertEquals(100, area.getCenter().x, EPSILON);
    }
    
    @Test
    void testAABBCreation() {
        AABB bounds = new AABB(0, 0, 0, 100, 100, 100);
        CombatArea area = new CombatArea(bounds);
        
        assertEquals(CombatArea.Shape.AABB, area.getShape());
        assertEquals(50, area.getCenter().x, EPSILON);
        assertEquals(50, area.getCenter().y, EPSILON);
        assertEquals(50, area.getCenter().z, EPSILON);
    }
    
    @Test
    void testAABBCreationFromCorners() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), new Vec3(100, 100, 100));
        
        assertEquals(CombatArea.Shape.AABB, area.getShape());
    }
    
    @Test
    void testSphereContains() {
        // Requirement 18.6: Prevent boss from leaving designated combat area
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        assertTrue(area.contains(new Vec3(0, 0, 0)));      // Center
        assertTrue(area.contains(new Vec3(5, 0, 0)));      // Inside
        assertTrue(area.contains(new Vec3(10, 0, 0)));     // On edge
        assertFalse(area.contains(new Vec3(11, 0, 0)));    // Outside
        assertFalse(area.contains(new Vec3(100, 0, 0)));   // Far outside
    }
    
    @Test
    void testAABBContains() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), new Vec3(100, 100, 100));
        
        assertTrue(area.contains(new Vec3(50, 50, 50)));   // Center
        assertTrue(area.contains(new Vec3(1, 1, 1)));      // Near min corner
        assertTrue(area.contains(new Vec3(99, 99, 99)));   // Near max corner
        assertFalse(area.contains(new Vec3(-1, 50, 50)));  // Outside
        assertFalse(area.contains(new Vec3(101, 50, 50))); // Outside
    }
    
    @Test
    void testSphereDistanceToEdge() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        // At center, distance to edge is radius
        assertEquals(10, area.getDistanceToEdge(new Vec3(0, 0, 0)), EPSILON);
        
        // At 5 units from center, distance to edge is 5
        assertEquals(5, area.getDistanceToEdge(new Vec3(5, 0, 0)), EPSILON);
        
        // At edge, distance is 0
        assertEquals(0, area.getDistanceToEdge(new Vec3(10, 0, 0)), EPSILON);
        
        // Outside, distance is negative
        assertTrue(area.getDistanceToEdge(new Vec3(15, 0, 0)) < 0);
    }
    
    @Test
    void testAABBDistanceToEdge() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), new Vec3(100, 100, 100));
        
        // At center, distance to nearest wall
        assertEquals(50, area.getDistanceToEdge(new Vec3(50, 50, 50)), EPSILON);
        
        // Near edge
        assertEquals(10, area.getDistanceToEdge(new Vec3(10, 50, 50)), EPSILON);
        
        // Outside, distance is negative
        assertTrue(area.getDistanceToEdge(new Vec3(-10, 50, 50)) < 0);
    }
    
    @Test
    void testSphereNearestPointInside() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        // Point inside stays the same
        Vec3 inside = area.getNearestPointInside(new Vec3(5, 0, 0));
        assertEquals(5, inside.x, EPSILON);
        
        // Point outside is pulled to edge (slightly inside)
        Vec3 outside = area.getNearestPointInside(new Vec3(20, 0, 0));
        assertTrue(outside.x < 10); // Should be inside
        assertTrue(outside.x > 9);  // Should be near edge
    }
    
    @Test
    void testAABBNearestPointInside() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), new Vec3(100, 100, 100));
        
        // Point inside stays the same
        Vec3 inside = area.getNearestPointInside(new Vec3(50, 50, 50));
        assertEquals(50, inside.x, EPSILON);
        
        // Point outside is clamped to edge
        Vec3 outside = area.getNearestPointInside(new Vec3(150, 50, 50));
        assertEquals(100, outside.x, EPSILON);
        assertEquals(50, outside.y, EPSILON);
    }
    
    @Test
    void testDirectionToCenter() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        Vec3 direction = area.getDirectionToCenter(new Vec3(10, 0, 0));
        
        assertEquals(-1, direction.x, EPSILON);
        assertEquals(0, direction.y, EPSILON);
        assertEquals(0, direction.z, EPSILON);
    }
    
    @Test
    void testCalculatePullForceInside() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        // Inside the area, no force
        Vec3 force = area.calculatePullForce(new Vec3(5, 0, 0), 1.0);
        
        assertEquals(0, force.x, EPSILON);
        assertEquals(0, force.y, EPSILON);
        assertEquals(0, force.z, EPSILON);
    }
    
    @Test
    void testCalculatePullForceOutside() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        // Outside the area, force towards center
        Vec3 force = area.calculatePullForce(new Vec3(20, 0, 0), 1.0);
        
        assertTrue(force.x < 0); // Pulling towards center (negative x)
        assertEquals(0, force.y, EPSILON);
        assertEquals(0, force.z, EPSILON);
    }
    
    @Test
    void testGetEffectiveBoundingAABBSphere() {
        CombatArea area = new CombatArea(new Vec3(0, 0, 0), 10);
        
        AABB bounds = area.getEffectiveBoundingAABB();
        
        assertEquals(-10, bounds.minX, EPSILON);
        assertEquals(-10, bounds.minY, EPSILON);
        assertEquals(-10, bounds.minZ, EPSILON);
        assertEquals(10, bounds.maxX, EPSILON);
        assertEquals(10, bounds.maxY, EPSILON);
        assertEquals(10, bounds.maxZ, EPSILON);
    }
    
    @Test
    void testGetEffectiveBoundingAABBBox() {
        AABB original = new AABB(0, 0, 0, 100, 100, 100);
        CombatArea area = new CombatArea(original);
        
        AABB bounds = area.getEffectiveBoundingAABB();
        
        assertEquals(original, bounds);
    }
    
    @Test
    void testToString() {
        CombatArea sphere = new CombatArea(new Vec3(0, 0, 0), 10);
        CombatArea box = new CombatArea(new Vec3(0, 0, 0), new Vec3(100, 100, 100));
        
        assertTrue(sphere.toString().contains("sphere"));
        assertTrue(box.toString().contains("AABB"));
    }
}
