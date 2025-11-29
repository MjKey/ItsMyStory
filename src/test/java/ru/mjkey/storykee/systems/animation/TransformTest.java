package ru.mjkey.storykee.systems.animation;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Transform class.
 * 
 * Requirements: 10.1
 */
class TransformTest {
    
    private static final float EPSILON = 0.0001f;
    
    @Test
    void testDefaultTransform() {
        Transform t = new Transform();
        
        assertEquals(0, t.getX(), EPSILON);
        assertEquals(0, t.getY(), EPSILON);
        assertEquals(0, t.getZ(), EPSILON);
        assertEquals(0, t.getPitch(), EPSILON);
        assertEquals(0, t.getYaw(), EPSILON);
        assertEquals(0, t.getRoll(), EPSILON);
        assertEquals(1, t.getScaleX(), EPSILON);
        assertEquals(1, t.getScaleY(), EPSILON);
        assertEquals(1, t.getScaleZ(), EPSILON);
    }
    
    @Test
    void testPositionTransform() {
        Transform t = new Transform(1.5f, 2.5f, 3.5f);
        
        assertEquals(1.5f, t.getX(), EPSILON);
        assertEquals(2.5f, t.getY(), EPSILON);
        assertEquals(3.5f, t.getZ(), EPSILON);
    }
    
    @Test
    void testFullTransform() {
        Vector3f pos = new Vector3f(1, 2, 3);
        Vector3f rot = new Vector3f(45, 90, 0);
        Vector3f scale = new Vector3f(2, 2, 2);
        
        Transform t = new Transform(pos, rot, scale);
        
        assertEquals(1, t.getX(), EPSILON);
        assertEquals(2, t.getY(), EPSILON);
        assertEquals(3, t.getZ(), EPSILON);
        assertEquals(45, t.getPitch(), EPSILON);
        assertEquals(90, t.getYaw(), EPSILON);
        assertEquals(0, t.getRoll(), EPSILON);
        assertEquals(2, t.getScaleX(), EPSILON);
    }
    
    @Test
    void testCopyConstructor() {
        Transform original = new Transform(1, 2, 3);
        original.setRotation(45, 90, 0);
        original.setScale(2, 2, 2);
        
        Transform copy = new Transform(original);
        
        assertEquals(original.getX(), copy.getX(), EPSILON);
        assertEquals(original.getY(), copy.getY(), EPSILON);
        assertEquals(original.getZ(), copy.getZ(), EPSILON);
        assertEquals(original.getPitch(), copy.getPitch(), EPSILON);
        assertEquals(original.getYaw(), copy.getYaw(), EPSILON);
        assertEquals(original.getScaleX(), copy.getScaleX(), EPSILON);
    }
    
    @Test
    void testSetPosition() {
        Transform t = new Transform();
        t.setPosition(5, 10, 15);
        
        assertEquals(5, t.getX(), EPSILON);
        assertEquals(10, t.getY(), EPSILON);
        assertEquals(15, t.getZ(), EPSILON);
    }
    
    @Test
    void testSetRotation() {
        Transform t = new Transform();
        t.setRotation(30, 60, 90);
        
        assertEquals(30, t.getPitch(), EPSILON);
        assertEquals(60, t.getYaw(), EPSILON);
        assertEquals(90, t.getRoll(), EPSILON);
    }
    
    @Test
    void testSetScale() {
        Transform t = new Transform();
        t.setScale(2, 3, 4);
        
        assertEquals(2, t.getScaleX(), EPSILON);
        assertEquals(3, t.getScaleY(), EPSILON);
        assertEquals(4, t.getScaleZ(), EPSILON);
    }
    
    @Test
    void testUniformScale() {
        Transform t = new Transform();
        t.setScale(5);
        
        assertEquals(5, t.getScaleX(), EPSILON);
        assertEquals(5, t.getScaleY(), EPSILON);
        assertEquals(5, t.getScaleZ(), EPSILON);
    }
    
    @Test
    void testLerp() {
        Transform start = new Transform(0, 0, 0);
        Transform end = new Transform(10, 20, 30);
        
        Transform mid = start.lerp(end, 0.5f);
        
        assertEquals(5, mid.getX(), EPSILON);
        assertEquals(10, mid.getY(), EPSILON);
        assertEquals(15, mid.getZ(), EPSILON);
    }
    
    @Test
    void testLerpAtZero() {
        Transform start = new Transform(0, 0, 0);
        Transform end = new Transform(10, 20, 30);
        
        Transform result = start.lerp(end, 0);
        
        assertEquals(0, result.getX(), EPSILON);
        assertEquals(0, result.getY(), EPSILON);
        assertEquals(0, result.getZ(), EPSILON);
    }
    
    @Test
    void testLerpAtOne() {
        Transform start = new Transform(0, 0, 0);
        Transform end = new Transform(10, 20, 30);
        
        Transform result = start.lerp(end, 1);
        
        assertEquals(10, result.getX(), EPSILON);
        assertEquals(20, result.getY(), EPSILON);
        assertEquals(30, result.getZ(), EPSILON);
    }
    
    @Test
    void testLerpClamping() {
        Transform start = new Transform(0, 0, 0);
        Transform end = new Transform(10, 10, 10);
        
        // Values outside 0-1 should be clamped
        Transform belowZero = start.lerp(end, -0.5f);
        Transform aboveOne = start.lerp(end, 1.5f);
        
        assertEquals(0, belowZero.getX(), EPSILON);
        assertEquals(10, aboveOne.getX(), EPSILON);
    }
    
    @Test
    void testIdentity() {
        Transform identity = Transform.identity();
        
        assertEquals(0, identity.getX(), EPSILON);
        assertEquals(0, identity.getY(), EPSILON);
        assertEquals(0, identity.getZ(), EPSILON);
        assertEquals(1, identity.getScaleX(), EPSILON);
    }
    
    @Test
    void testFromPosition() {
        Transform t = Transform.fromPosition(5, 10, 15);
        
        assertEquals(5, t.getX(), EPSILON);
        assertEquals(10, t.getY(), EPSILON);
        assertEquals(15, t.getZ(), EPSILON);
        assertEquals(1, t.getScaleX(), EPSILON);
    }
    
    @Test
    void testFromRotation() {
        Transform t = Transform.fromRotation(30, 60, 90);
        
        assertEquals(30, t.getPitch(), EPSILON);
        assertEquals(60, t.getYaw(), EPSILON);
        assertEquals(90, t.getRoll(), EPSILON);
        assertEquals(0, t.getX(), EPSILON);
    }
    
    @Test
    void testEquality() {
        Transform t1 = new Transform(1, 2, 3);
        Transform t2 = new Transform(1, 2, 3);
        Transform t3 = new Transform(4, 5, 6);
        
        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
    }
}
