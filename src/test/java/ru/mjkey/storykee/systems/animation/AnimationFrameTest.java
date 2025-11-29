package ru.mjkey.storykee.systems.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnimationFrame class.
 * 
 * Requirements: 10.1
 */
class AnimationFrameTest {
    
    private static final float EPSILON = 0.0001f;
    private AnimationFrame frame;
    
    @BeforeEach
    void setUp() {
        frame = new AnimationFrame(0, 0.5f);
    }
    
    @Test
    void testFrameCreation() {
        assertEquals(0, frame.getFrameNumber());
        assertEquals(0.5f, frame.getTimestamp(), EPSILON);
        assertEquals(0, frame.getBoneCount());
    }
    
    @Test
    void testSetBoneTransform() {
        Transform t = new Transform(1, 2, 3);
        frame.setBoneTransform("arm", t);
        
        assertTrue(frame.hasBoneTransform("arm"));
        assertEquals(1, frame.getBoneCount());
        
        Transform retrieved = frame.getBoneTransform("arm");
        assertEquals(1, retrieved.getX(), EPSILON);
        assertEquals(2, retrieved.getY(), EPSILON);
        assertEquals(3, retrieved.getZ(), EPSILON);
    }
    
    @Test
    void testGetBoneTransformDefault() {
        // Non-existent bone returns identity transform
        Transform t = frame.getBoneTransform("nonexistent");
        
        assertNotNull(t);
        assertEquals(0, t.getX(), EPSILON);
        assertEquals(0, t.getY(), EPSILON);
        assertEquals(0, t.getZ(), EPSILON);
        assertEquals(1, t.getScaleX(), EPSILON);
    }
    
    @Test
    void testRemoveBoneTransform() {
        frame.setBoneTransform("arm", new Transform(1, 2, 3));
        
        Transform removed = frame.removeBoneTransform("arm");
        
        assertNotNull(removed);
        assertFalse(frame.hasBoneTransform("arm"));
        assertEquals(0, frame.getBoneCount());
    }
    
    @Test
    void testRemoveNonexistentBone() {
        Transform removed = frame.removeBoneTransform("nonexistent");
        assertNull(removed);
    }
    
    @Test
    void testGetBoneTransforms() {
        frame.setBoneTransform("arm", new Transform(1, 0, 0));
        frame.setBoneTransform("leg", new Transform(0, 1, 0));
        
        Map<String, Transform> transforms = frame.getBoneTransforms();
        
        assertEquals(2, transforms.size());
        assertTrue(transforms.containsKey("arm"));
        assertTrue(transforms.containsKey("leg"));
    }
    
    @Test
    void testBoneTransformsUnmodifiable() {
        frame.setBoneTransform("arm", new Transform(1, 0, 0));
        
        Map<String, Transform> transforms = frame.getBoneTransforms();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            transforms.put("leg", new Transform());
        });
    }
    
    @Test
    void testLerp() {
        AnimationFrame frame1 = new AnimationFrame(0, 0);
        frame1.setBoneTransform("bone", new Transform(0, 0, 0));
        
        AnimationFrame frame2 = new AnimationFrame(1, 1.0f);
        frame2.setBoneTransform("bone", new Transform(10, 20, 30));
        
        AnimationFrame mid = frame1.lerp(frame2, 0.5f);
        
        assertEquals(0.5f, mid.getTimestamp(), EPSILON);
        
        Transform t = mid.getBoneTransform("bone");
        assertEquals(5, t.getX(), EPSILON);
        assertEquals(10, t.getY(), EPSILON);
        assertEquals(15, t.getZ(), EPSILON);
    }
    
    @Test
    void testLerpWithDifferentBones() {
        AnimationFrame frame1 = new AnimationFrame(0, 0);
        frame1.setBoneTransform("arm", new Transform(0, 0, 0));
        
        AnimationFrame frame2 = new AnimationFrame(1, 1.0f);
        frame2.setBoneTransform("arm", new Transform(10, 10, 10));
        frame2.setBoneTransform("leg", new Transform(20, 20, 20));
        
        AnimationFrame mid = frame1.lerp(frame2, 0.5f);
        
        // Both bones should be present
        assertTrue(mid.hasBoneTransform("arm"));
        assertTrue(mid.hasBoneTransform("leg"));
        
        // Arm interpolated from both frames
        Transform arm = mid.getBoneTransform("arm");
        assertEquals(5, arm.getX(), EPSILON);
        
        // Leg interpolated from identity to frame2
        Transform leg = mid.getBoneTransform("leg");
        assertEquals(10, leg.getX(), EPSILON);
    }
    
    @Test
    void testLerpClamping() {
        AnimationFrame frame1 = new AnimationFrame(0, 0);
        frame1.setBoneTransform("bone", new Transform(0, 0, 0));
        
        AnimationFrame frame2 = new AnimationFrame(1, 1.0f);
        frame2.setBoneTransform("bone", new Transform(10, 10, 10));
        
        // Values outside 0-1 should be clamped
        AnimationFrame belowZero = frame1.lerp(frame2, -0.5f);
        AnimationFrame aboveOne = frame1.lerp(frame2, 1.5f);
        
        assertEquals(0, belowZero.getBoneTransform("bone").getX(), EPSILON);
        assertEquals(10, aboveOne.getBoneTransform("bone").getX(), EPSILON);
    }
    
    @Test
    void testCopy() {
        frame.setBoneTransform("arm", new Transform(1, 2, 3));
        frame.setBoneTransform("leg", new Transform(4, 5, 6));
        
        AnimationFrame copy = frame.copy();
        
        assertEquals(frame.getFrameNumber(), copy.getFrameNumber());
        assertEquals(frame.getTimestamp(), copy.getTimestamp(), EPSILON);
        assertEquals(frame.getBoneCount(), copy.getBoneCount());
        
        // Verify transforms are copied
        Transform armCopy = copy.getBoneTransform("arm");
        assertEquals(1, armCopy.getX(), EPSILON);
    }
    
    @Test
    void testCopyIndependence() {
        frame.setBoneTransform("arm", new Transform(1, 2, 3));
        
        AnimationFrame copy = frame.copy();
        copy.setBoneTransform("arm", new Transform(10, 20, 30));
        
        // Original should be unchanged
        Transform original = frame.getBoneTransform("arm");
        assertEquals(1, original.getX(), EPSILON);
    }
}
