package ru.mjkey.storykee.systems.region;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Region class.
 * 
 * Requirements: 49.1, 49.3, 49.4
 */
class RegionTest {
    
    private static final double EPSILON = 0.0001;
    private Region region;
    
    @BeforeEach
    void setUp() {
        // Create a 10x10x10 region from (0,0,0) to (10,10,10)
        region = new Region("test-region", new Vec3(0, 0, 0), new Vec3(10, 10, 10));
    }
    
    @Test
    void testRegionCreation() {
        // Requirement 49.1: Create region definition
        assertEquals("test-region", region.getId());
        assertEquals(0, region.getMin().x, EPSILON);
        assertEquals(0, region.getMin().y, EPSILON);
        assertEquals(0, region.getMin().z, EPSILON);
        assertEquals(10, region.getMax().x, EPSILON);
        assertEquals(10, region.getMax().y, EPSILON);
        assertEquals(10, region.getMax().z, EPSILON);
    }
    
    @Test
    void testRegionCreationNormalizesCorners() {
        // Corners should be normalized regardless of order
        Region reversed = new Region("reversed", new Vec3(10, 10, 10), new Vec3(0, 0, 0));
        
        assertEquals(0, reversed.getMin().x, EPSILON);
        assertEquals(10, reversed.getMax().x, EPSILON);
    }
    
    @Test
    void testRegionFromBlockPos() {
        Region blockRegion = new Region("block-region", new BlockPos(0, 0, 0), new BlockPos(5, 5, 5));
        
        assertEquals(0, blockRegion.getMin().x, EPSILON);
        assertEquals(6, blockRegion.getMax().x, EPSILON); // BlockPos adds 1 to max
    }
    
    @Test
    void testSphereRegion() {
        Region sphere = Region.sphere("sphere", new Vec3(5, 5, 5), 3);
        
        assertEquals("sphere", sphere.getId());
        assertEquals(2, sphere.getMin().x, EPSILON);
        assertEquals(8, sphere.getMax().x, EPSILON);
    }
    
    @Test
    void testGetCenter() {
        Vec3 center = region.getCenter();
        
        assertEquals(5, center.x, EPSILON);
        assertEquals(5, center.y, EPSILON);
        assertEquals(5, center.z, EPSILON);
    }
    
    @Test
    void testGetSize() {
        Vec3 size = region.getSize();
        
        assertEquals(10, size.x, EPSILON);
        assertEquals(10, size.y, EPSILON);
        assertEquals(10, size.z, EPSILON);
    }
    
    @Test
    void testGetVolume() {
        assertEquals(1000, region.getVolume(), EPSILON);
    }
    
    @Test
    void testContainsVec3Inside() {
        // Requirement 49.3: Implement containment checking
        assertTrue(region.contains(new Vec3(5, 5, 5)));
        assertTrue(region.contains(new Vec3(0, 0, 0)));
        assertTrue(region.contains(new Vec3(10, 10, 10)));
    }
    
    @Test
    void testContainsVec3Outside() {
        assertFalse(region.contains(new Vec3(-1, 5, 5)));
        assertFalse(region.contains(new Vec3(11, 5, 5)));
        assertFalse(region.contains(new Vec3(5, -1, 5)));
        assertFalse(region.contains(new Vec3(5, 11, 5)));
    }
    
    @Test
    void testContainsBlockPos() {
        assertTrue(region.contains(new BlockPos(5, 5, 5)));
        assertFalse(region.contains(new BlockPos(-1, 5, 5)));
    }
    
    @Test
    void testOverlaps() {
        // Requirement 49.4: Handle overlapping regions
        Region overlapping = new Region("overlap", new Vec3(5, 5, 5), new Vec3(15, 15, 15));
        Region nonOverlapping = new Region("no-overlap", new Vec3(20, 20, 20), new Vec3(30, 30, 30));
        
        assertTrue(region.overlaps(overlapping));
        assertFalse(region.overlaps(nonOverlapping));
    }
    
    @Test
    void testFullyContains() {
        Region inner = new Region("inner", new Vec3(2, 2, 2), new Vec3(8, 8, 8));
        Region outer = new Region("outer", new Vec3(-5, -5, -5), new Vec3(15, 15, 15));
        
        assertTrue(region.fullyContains(inner));
        assertFalse(region.fullyContains(outer));
    }
    
    @Test
    void testPriority() {
        assertEquals(0, region.getPriority());
        
        region.setPriority(5);
        assertEquals(5, region.getPriority());
    }
    
    @Test
    void testCustomData() {
        region.setData("questId", "main-quest");
        region.setData("level", 10);
        
        assertTrue(region.hasData("questId"));
        assertEquals("main-quest", region.getData("questId"));
        assertEquals(10, (int) region.getData("level"));
        
        region.removeData("questId");
        assertFalse(region.hasData("questId"));
    }
    
    @Test
    void testEquality() {
        Region sameId = new Region("test-region", new Vec3(100, 100, 100), new Vec3(200, 200, 200));
        Region differentId = new Region("other-region", new Vec3(0, 0, 0), new Vec3(10, 10, 10));
        
        assertEquals(region, sameId); // Same ID
        assertNotEquals(region, differentId);
    }
    
    @Test
    void testHashCode() {
        Region sameId = new Region("test-region", new Vec3(100, 100, 100), new Vec3(200, 200, 200));
        
        assertEquals(region.hashCode(), sameId.hashCode());
    }
}
