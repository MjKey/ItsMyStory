package ru.mjkey.storykee.runtime.async;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AsyncExecutor functionality.
 * 
 * Requirements: 39.1, 39.2
 */
class AsyncExecutorTest {
    
    private AsyncExecutor executor;
    
    @BeforeEach
    void setUp() {
        executor = AsyncExecutor.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // Note: We don't shutdown the singleton instance as it may be used by other tests
    }
    
    @Test
    void testExecuteAsyncCallable() throws Exception {
        // Test that async execution works with Callable
        CompletableFuture<Integer> future = executor.executeAsync(() -> {
            Thread.sleep(100);
            return 42;
        });
        
        Integer result = future.get(2, TimeUnit.SECONDS);
        assertEquals(42, result);
    }
    
    @Test
    void testExecuteAsyncRunnable() throws Exception {
        // Test that async execution works with Runnable
        AtomicBoolean executed = new AtomicBoolean(false);
        
        CompletableFuture<Void> future = executor.executeAsync(() -> {
            executed.set(true);
        });
        
        future.get(2, TimeUnit.SECONDS);
        assertTrue(executed.get());
    }
    
    @Test
    void testAsyncExecutionOnBackgroundThread() throws Exception {
        // Verify that async tasks run on a different thread
        String mainThreadName = Thread.currentThread().getName();
        
        CompletableFuture<String> future = executor.executeAsync(() -> {
            return Thread.currentThread().getName();
        });
        
        String asyncThreadName = future.get(2, TimeUnit.SECONDS);
        assertNotEquals(mainThreadName, asyncThreadName);
        assertTrue(asyncThreadName.startsWith("Storykee-Async-"));
    }
    
    @Test
    void testAsyncExceptionHandling() {
        // Test that exceptions in async tasks are properly handled
        CompletableFuture<Integer> future = executor.executeAsync(() -> {
            throw new RuntimeException("Test exception");
        });
        
        assertThrows(ExecutionException.class, () -> {
            future.get(2, TimeUnit.SECONDS);
        });
    }
    
    @Test
    void testScheduleAsync() throws Exception {
        // Test scheduled async execution
        AtomicBoolean executed = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        
        CompletableFuture<Void> future = executor.scheduleAsync(() -> {
            executed.set(true);
        }, 200, TimeUnit.MILLISECONDS);
        
        future.get(2, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        assertTrue(executed.get());
        assertTrue(endTime - startTime >= 200, "Task should execute after delay");
    }
    
    @Test
    void testMultipleConcurrentTasks() throws Exception {
        // Test that multiple tasks can run concurrently
        int taskCount = 10;
        AtomicInteger counter = new AtomicInteger(0);
        
        CompletableFuture<?>[] futures = new CompletableFuture[taskCount];
        for (int i = 0; i < taskCount; i++) {
            futures[i] = executor.executeAsync(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                counter.incrementAndGet();
            });
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        assertEquals(taskCount, counter.get());
    }
    
    @Test
    void testGetActiveThreadCount() {
        // Test that we can query active thread count
        int activeCount = executor.getActiveThreadCount();
        assertTrue(activeCount >= 0);
    }
    
    @Test
    void testGetQueuedTaskCount() {
        // Test that we can query queued task count
        int queuedCount = executor.getQueuedTaskCount();
        assertTrue(queuedCount >= 0);
    }
}
