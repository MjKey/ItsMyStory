package ru.mjkey.storykee.systems.camera;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Cutscene class.
 * 
 * Requirements: 19.1, 19.2, 19.4, 19.5
 */
class CutsceneTest {
    
    private static final float EPSILON = 0.0001f;
    private Cutscene.Builder builder;
    
    @BeforeEach
    void setUp() {
        builder = new Cutscene.Builder("test-cutscene");
    }
    
    @Test
    void testCutsceneCreation() {
        Cutscene cutscene = builder.build();
        
        assertEquals("test-cutscene", cutscene.getId());
        assertTrue(cutscene.isSkippable()); // Default
        assertTrue(cutscene.getWaypoints().isEmpty());
    }
    
    @Test
    void testAddWaypoints() {
        // Requirement 19.2: Define camera waypoints
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 64, 0), 0, 0, 40);
        CameraWaypoint wp2 = new CameraWaypoint(new Vec3(10, 64, 10), -15, 45, 60);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .addWaypoint(wp2)
                .build();
        
        assertEquals(2, cutscene.getWaypoints().size());
        assertEquals(wp1, cutscene.getWaypoints().get(0));
        assertEquals(wp2, cutscene.getWaypoints().get(1));
    }
    
    @Test
    void testTotalDuration() {
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 40);
        CameraWaypoint wp2 = new CameraWaypoint(new Vec3(10, 0, 0), 0, 0, 60);
        CameraWaypoint wp3 = new CameraWaypoint(new Vec3(20, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .addWaypoint(wp2)
                .addWaypoint(wp3)
                .build();
        
        assertEquals(200, cutscene.getTotalDuration());
    }
    
    @Test
    void testSkippable() {
        // Requirement 19.5: Skip functionality
        Cutscene skippable = builder.skippable(true).build();
        assertTrue(skippable.isSkippable());
        
        Cutscene notSkippable = new Cutscene.Builder("test").skippable(false).build();
        assertFalse(notSkippable.isSkippable());
    }
    
    @Test
    void testOnComplete() {
        // Requirement 19.4: Return camera control on complete
        AtomicBoolean completed = new AtomicBoolean(false);
        
        Cutscene cutscene = builder
                .onComplete(() -> completed.set(true))
                .build();
        
        assertNotNull(cutscene.getOnComplete());
        cutscene.getOnComplete().run();
        assertTrue(completed.get());
    }
    
    @Test
    void testOnSkip() {
        AtomicBoolean skipped = new AtomicBoolean(false);
        
        Cutscene cutscene = builder
                .onSkip(() -> skipped.set(true))
                .build();
        
        assertNotNull(cutscene.getOnSkip());
        cutscene.getOnSkip().run();
        assertTrue(skipped.get());
    }
    
    @Test
    void testGetWaypointAtTimeEmpty() {
        Cutscene cutscene = builder.build();
        
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(0);
        
        assertNull(progress);
    }
    
    @Test
    void testGetWaypointAtTimeStart() {
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 100);
        CameraWaypoint wp2 = new CameraWaypoint(new Vec3(10, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .addWaypoint(wp2)
                .build();
        
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(0);
        
        assertNotNull(progress);
        assertEquals(wp1, progress.from());
        assertEquals(wp1, progress.to());
        assertEquals(0, progress.progress(), EPSILON);
    }
    
    @Test
    void testGetWaypointAtTimeMid() {
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 100);
        CameraWaypoint wp2 = new CameraWaypoint(new Vec3(10, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .addWaypoint(wp2)
                .build();
        
        // At tick 50, we're halfway through first waypoint
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(50);
        
        assertNotNull(progress);
        assertEquals(wp1, progress.from());
        assertEquals(wp1, progress.to());
        assertEquals(0.5f, progress.progress(), EPSILON);
    }
    
    @Test
    void testGetWaypointAtTimeSecondWaypoint() {
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 100);
        CameraWaypoint wp2 = new CameraWaypoint(new Vec3(10, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .addWaypoint(wp2)
                .build();
        
        // At tick 150, we're halfway through second waypoint
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(150);
        
        assertNotNull(progress);
        assertEquals(wp1, progress.from());
        assertEquals(wp2, progress.to());
        assertEquals(0.5f, progress.progress(), EPSILON);
    }
    
    @Test
    void testGetWaypointAtTimeComplete() {
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .build();
        
        // Past the end
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(150);
        
        assertNull(progress); // Cutscene complete
    }
    
    @Test
    void testWaypointsUnmodifiable() {
        CameraWaypoint wp = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder.addWaypoint(wp).build();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            cutscene.getWaypoints().add(new CameraWaypoint(new Vec3(1, 1, 1), 0, 0, 50));
        });
    }
    
    @Test
    void testZeroDurationWaypoint() {
        CameraWaypoint wp1 = new CameraWaypoint(new Vec3(0, 0, 0), 0, 0, 0);
        CameraWaypoint wp2 = new CameraWaypoint(new Vec3(10, 0, 0), 0, 0, 100);
        
        Cutscene cutscene = builder
                .addWaypoint(wp1)
                .addWaypoint(wp2)
                .build();
        
        // Zero duration waypoint is skipped, we should be at second waypoint with progress 0
        Cutscene.WaypointProgress progress = cutscene.getWaypointAtTime(0);
        
        assertNotNull(progress);
        // At time 0, first waypoint (duration=0) is complete, so we're at start of second waypoint
        assertEquals(wp1, progress.from());
        assertEquals(wp2, progress.to());
        assertEquals(0.0f, progress.progress(), EPSILON);
    }
}