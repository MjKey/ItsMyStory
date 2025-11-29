package ru.mjkey.storykee.runtime.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MinecraftThreadBridge functionality.
 * Note: These tests verify the bridge's queuing and scheduling logic without a real MinecraftServer.
 * Full integration tests with a real server would be done in a separate test environment.
 * 
 * Requirements: 5.3, 39.2
 */
class MinecraftThreadBridgeTest {
    
    private MinecraftThreadBridge bridge;
    
    @BeforeEach
    void setUp() {
        bridge = MinecraftThreadBridge.getInstance();
        // Note: Without a real MinecraftServer, the bridge will execute tasks immediately
        // This tests the fallback behavior
    }
    
    @Test
    void testBridgeInstanceCreation() {
        assertNotNull(bridge);
        assertSame(bridge, MinecraftThreadBridge.getInstance(), "Should return same instance");
    }
    
    @Test
    void testExecuteOnMainThreadWithoutServer() {
        // Without a server initialized, tasks should execute immediately with a warning
        AtomicBoolean executed = new AtomicBoolean(false);
        
        bridge.executeOnMainThread(() -> {
            executed.set(true);
        });
        
        assertTrue(executed.get(), "Task should execute immediately when no server is initialized");
    }
    
    @Test
    void testScheduleOnMainThreadWithoutServer() {
        // Without a server initialized, scheduled tasks should execute immediately with a warning
        AtomicBoolean executed = new AtomicBoolean(false);
        
        bridge.scheduleOnMainThread(() -> {
            executed.set(true);
        }, 10);
        
        assertTrue(executed.get(), "Task should execute immediately when no server is initialized");
    }
    
    @Test
    void testGetQueuedTaskCount() {
        // Test that we can query queued task count
        int count = bridge.getQueuedTaskCount();
        assertTrue(count >= 0, "Queued task count should be non-negative");
    }
    
    @Test
    void testGetScheduledTaskCount() {
        // Test that we can query scheduled task count
        int count = bridge.getScheduledTaskCount();
        assertTrue(count >= 0, "Scheduled task count should be non-negative");
    }
    
    @Test
    void testShutdown() {
        // Test that shutdown clears state
        bridge.shutdown();
        
        assertFalse(bridge.isInitialized(), "Bridge should not be initialized after shutdown");
        assertEquals(0, bridge.getQueuedTaskCount(), "Queued tasks should be cleared");
        assertEquals(0, bridge.getScheduledTaskCount(), "Scheduled tasks should be cleared");
    }
    
    @Test
    void testMultipleTasksWithoutServer() {
        // Test that multiple tasks can be executed
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < 5; i++) {
            bridge.executeOnMainThread(() -> {
                counter.incrementAndGet();
            });
        }
        
        assertEquals(5, counter.get(), "All tasks should execute");
    }
    
    @Test
    void testExceptionHandlingInTask() {
        // Test that exceptions in tasks don't crash the system
        AtomicBoolean secondTaskExecuted = new AtomicBoolean(false);
        
        // Execute a task that throws an exception
        try {
            bridge.executeOnMainThread(() -> {
                throw new RuntimeException("Test exception");
            });
        } catch (Exception e) {
            // Exception might be thrown or caught internally
        }
        
        // Execute a second task that should still work
        bridge.executeOnMainThread(() -> {
            secondTaskExecuted.set(true);
        });
        
        assertTrue(secondTaskExecuted.get(), "Second task should execute despite first task throwing");
    }
}
