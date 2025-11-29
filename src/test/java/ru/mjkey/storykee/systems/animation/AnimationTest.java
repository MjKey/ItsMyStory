package ru.mjkey.storykee.systems.animation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Animation and AnimationFrame classes.
 * 
 * Requirements: 10.1, 10.2, 10.4
 */
class AnimationTest {
    
    private static final float EPSILON = 0.0001f;
    private Animation animation;
    
    @BeforeEach
    void setUp() {
        animation = new Animation("test-anim");
    }
    
    @Test
    void testAnimationCreation() {
        assertEquals("test-anim", animation.getId());
        assertEquals(0, animation.getFrameCount());
        assertEquals(0, animation.getDuration(), EPSILON);
        assertFalse(animation.isLoop());
    }
    
    @Test
    void testAnimationWithProperties() {
        Animation anim = new Animation("anim", 2.5f, true);
        
        assertEquals("anim", anim.getId());
        assertEquals(2.5f, anim.getDuration(), EPSILON);
        assertTrue(anim.isLoop());
    }
    
    @Test
    void testAddFrame() {
        AnimationFrame frame = new AnimationFrame(0, 0);
        animation.addFrame(frame);
        
        assertEquals(1, animation.getFrameCount());
        assertEquals(frame, animation.getFrame(0));
    }
    
    @Test
    void testFramesSortedByTimestamp() {
        AnimationFrame frame1 = new AnimationFrame(0, 1.0f);
        AnimationFrame frame2 = new AnimationFrame(1, 0.5f);
        AnimationFrame frame3 = new AnimationFrame(2, 1.5f);
        
        animation.addFrame(frame1);
        animation.addFrame(frame2);
        animation.addFrame(frame3);
        
        // Frames should be sorted by timestamp
        assertEquals(0.5f, animation.getFrame(0).getTimestamp(), EPSILON);
        assertEquals(1.0f, animation.getFrame(1).getTimestamp(), EPSILON);
        assertEquals(1.5f, animation.getFrame(2).getTimestamp(), EPSILON);
    }
    
    @Test
    void testDurationUpdatedOnAddFrame() {
        animation.addFrame(new AnimationFrame(0, 0));
        animation.addFrame(new AnimationFrame(1, 2.0f));
        
        assertEquals(2.0f, animation.getDuration(), EPSILON);
    }
    
    @Test
    void testSetDuration() {
        animation.setDuration(5.0f);
        assertEquals(5.0f, animation.getDuration(), EPSILON);
        
        // Negative duration should be clamped to 0
        animation.setDuration(-1.0f);
        assertEquals(0, animation.getDuration(), EPSILON);
    }
    
    @Test
    void testSetLoop() {
        animation.setLoop(true);
        assertTrue(animation.isLoop());
        
        animation.setLoop(false);
        assertFalse(animation.isLoop());
    }
    
    @Test
    void testFrameRate() {
        assertEquals(20.0f, animation.getFrameRate(), EPSILON); // Default
        
        animation.setFrameRate(30.0f);
        assertEquals(30.0f, animation.getFrameRate(), EPSILON);
        
        // Minimum frame rate is 1
        animation.setFrameRate(0);
        assertEquals(1.0f, animation.getFrameRate(), EPSILON);
    }
    
    @Test
    void testDurationTicks() {
        animation.setDuration(2.0f);
        assertEquals(40, animation.getDurationTicks()); // 2 seconds * 20 TPS
        
        animation.setDurationTicks(60);
        assertEquals(3.0f, animation.getDuration(), EPSILON);
    }
    
    @Test
    void testRemoveFrame() {
        AnimationFrame frame1 = new AnimationFrame(0, 0);
        AnimationFrame frame2 = new AnimationFrame(1, 1.0f);
        
        animation.addFrame(frame1);
        animation.addFrame(frame2);
        
        AnimationFrame removed = animation.removeFrame(0);
        
        assertEquals(frame1, removed);
        assertEquals(1, animation.getFrameCount());
    }
    
    @Test
    void testClearFrames() {
        animation.addFrame(new AnimationFrame(0, 0));
        animation.addFrame(new AnimationFrame(1, 1.0f));
        
        animation.clearFrames();
        
        assertEquals(0, animation.getFrameCount());
    }
    
    @Test
    void testCopy() {
        animation.setDuration(2.0f);
        animation.setLoop(true);
        animation.addFrame(new AnimationFrame(0, 0));
        
        Animation copy = animation.copy();
        
        assertEquals(animation.getId(), copy.getId());
        assertEquals(animation.getDuration(), copy.getDuration(), EPSILON);
        assertEquals(animation.isLoop(), copy.isLoop());
        assertEquals(animation.getFrameCount(), copy.getFrameCount());
    }
    
    @Test
    void testGetFrameAtTimeEmpty() {
        AnimationFrame frame = animation.getFrameAtTime(0.5f);
        
        // Empty animation returns default frame
        assertEquals(0, frame.getFrameNumber());
    }
    
    @Test
    void testGetFrameAtTimeSingleFrame() {
        AnimationFrame frame = new AnimationFrame(0, 0);
        frame.setBoneTransform("bone", new Transform(1, 2, 3));
        animation.addFrame(frame);
        
        AnimationFrame result = animation.getFrameAtTime(0.5f);
        
        assertNotNull(result);
        assertTrue(result.hasBoneTransform("bone"));
    }
    
    @Test
    void testGetFrameAtTimeInterpolation() {
        AnimationFrame frame1 = new AnimationFrame(0, 0);
        frame1.setBoneTransform("bone", new Transform(0, 0, 0));
        
        AnimationFrame frame2 = new AnimationFrame(1, 1.0f);
        frame2.setBoneTransform("bone", new Transform(10, 10, 10));
        
        animation.addFrame(frame1);
        animation.addFrame(frame2);
        animation.setDuration(1.0f);
        
        AnimationFrame mid = animation.getFrameAtTime(0.5f);
        Transform t = mid.getBoneTransform("bone");
        
        assertEquals(5, t.getX(), EPSILON);
        assertEquals(5, t.getY(), EPSILON);
        assertEquals(5, t.getZ(), EPSILON);
    }
    
    @Test
    void testGetFrameAtTimeLooping() {
        animation.setLoop(true);
        animation.setDuration(1.0f);
        
        AnimationFrame frame1 = new AnimationFrame(0, 0);
        frame1.setBoneTransform("bone", new Transform(0, 0, 0));
        
        AnimationFrame frame2 = new AnimationFrame(1, 1.0f);
        frame2.setBoneTransform("bone", new Transform(10, 10, 10));
        
        animation.addFrame(frame1);
        animation.addFrame(frame2);
        
        // Time 1.5 should wrap to 0.5 when looping
        AnimationFrame result = animation.getFrameAtTime(1.5f);
        Transform t = result.getBoneTransform("bone");
        
        assertEquals(5, t.getX(), EPSILON);
    }
    
    @Test
    void testCreateSimple() {
        Transform start = new Transform(0, 0, 0);
        Transform end = new Transform(10, 10, 10);
        
        Animation simple = Animation.createSimple("simple", "bone", start, end, 2.0f, false);
        
        assertEquals("simple", simple.getId());
        assertEquals(2.0f, simple.getDuration(), EPSILON);
        assertFalse(simple.isLoop());
        assertEquals(2, simple.getFrameCount());
    }
}
