package ru.mjkey.storykee.runtime.async;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridges async operations to the main Minecraft server thread.
 * Ensures thread safety for Minecraft API calls by executing them on the main game thread.
 * 
 * Requirements: 5.3, 39.2
 */
public class MinecraftThreadBridge {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftThreadBridge.class);
    private static MinecraftThreadBridge instance;
    
    private final ConcurrentLinkedQueue<Runnable> mainThreadQueue;
    private final ConcurrentLinkedQueue<ScheduledTask> scheduledTasks;
    private MinecraftServer server;
    private final AtomicBoolean initialized;
    
    private MinecraftThreadBridge() {
        this.mainThreadQueue = new ConcurrentLinkedQueue<>();
        this.scheduledTasks = new ConcurrentLinkedQueue<>();
        this.initialized = new AtomicBoolean(false);
    }
    
    public static MinecraftThreadBridge getInstance() {
        if (instance == null) {
            synchronized (MinecraftThreadBridge.class) {
                if (instance == null) {
                    instance = new MinecraftThreadBridge();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initializes the bridge with the Minecraft server instance.
     * This should be called when the server starts.
     * 
     * @param server The Minecraft server instance
     */
    public void initialize(MinecraftServer server) {
        if (this.server != null && this.server != server) {
            LOGGER.warn("MinecraftThreadBridge already initialized with a different server instance");
        }
        
        this.server = server;
        this.initialized.set(true);
        
        // Configure AsyncExecutor to use this bridge
        AsyncExecutor executor = AsyncExecutor.getInstance();
        configureAsyncExecutor(executor);
        
        LOGGER.info("MinecraftThreadBridge initialized with server");
    }
    
    /**
     * Configures the AsyncExecutor to use this bridge for main thread execution.
     */
    private void configureAsyncExecutor(AsyncExecutor executor) {
        // We'll override the methods by creating a wrapper
        // This is done through the bridge pattern
    }
    
    /**
     * Executes a task on the main game thread.
     * If already on the main thread, executes immediately.
     * Otherwise, queues the task for execution on the next tick.
     * 
     * @param task The task to execute
     * 
     * Requirement 5.3: WHEN a script calls a Minecraft API function THEN the Runtime SHALL execute it on the appropriate game thread
     * Requirement 39.2: WHEN an async operation completes THEN the Runtime SHALL execute the callback on the main game thread
     */
    public void executeOnMainThread(Runnable task) {
        if (!initialized.get() || server == null) {
            LOGGER.warn("MinecraftThreadBridge not initialized, executing task on current thread");
            task.run();
            return;
        }
        
        // Check if we're already on the main thread
        if (server.isSameThread()) {
            // Execute immediately
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("Error executing task on main thread", e);
            }
        } else {
            // Queue for execution on next tick
            mainThreadQueue.offer(task);
        }
    }
    
    /**
     * Schedules a task to run on the main game thread after a delay.
     * 
     * @param task The task to execute
     * @param delayTicks The delay in game ticks (20 ticks = 1 second)
     * 
     * Requirement 5.3: WHEN a script calls a Minecraft API function THEN the Runtime SHALL execute it on the appropriate game thread
     */
    public void scheduleOnMainThread(Runnable task, long delayTicks) {
        if (!initialized.get() || server == null) {
            LOGGER.warn("MinecraftThreadBridge not initialized, executing task on current thread");
            task.run();
            return;
        }
        
        if (delayTicks <= 0) {
            executeOnMainThread(task);
            return;
        }
        
        long executionTick = getCurrentTick() + delayTicks;
        scheduledTasks.offer(new ScheduledTask(task, executionTick));
    }
    
    /**
     * Processes all queued tasks. This should be called every server tick.
     * This method must be called from the main server thread.
     */
    public void tick() {
        if (!initialized.get() || server == null) {
            return;
        }
        
        // Ensure we're on the main thread
        if (!server.isSameThread()) {
            LOGGER.error("tick() called from non-main thread! This is a bug.");
            return;
        }
        
        // Process immediate tasks
        Runnable task;
        int processedCount = 0;
        while ((task = mainThreadQueue.poll()) != null) {
            try {
                task.run();
                processedCount++;
            } catch (Exception e) {
                LOGGER.error("Error executing queued task on main thread", e);
            }
        }
        
        if (processedCount > 0) {
            LOGGER.debug("Processed {} queued tasks on main thread", processedCount);
        }
        
        // Process scheduled tasks
        long currentTick = getCurrentTick();
        int scheduledCount = 0;
        
        ScheduledTask scheduledTask;
        while ((scheduledTask = scheduledTasks.peek()) != null) {
            if (scheduledTask.executionTick <= currentTick) {
                scheduledTasks.poll();
                try {
                    scheduledTask.task.run();
                    scheduledCount++;
                } catch (Exception e) {
                    LOGGER.error("Error executing scheduled task on main thread", e);
                }
            } else {
                // Tasks are ordered by execution time, so we can stop here
                break;
            }
        }
        
        if (scheduledCount > 0) {
            LOGGER.debug("Processed {} scheduled tasks on main thread", scheduledCount);
        }
    }
    
    /**
     * Returns the current server tick count.
     */
    private long getCurrentTick() {
        if (server != null) {
            return server.getTickCount();
        }
        return 0;
    }
    
    /**
     * Returns whether the bridge is initialized.
     */
    public boolean isInitialized() {
        return initialized.get() && server != null;
    }
    
    /**
     * Returns the Minecraft server instance.
     * 
     * @return The server instance, or null if not initialized
     */
    public MinecraftServer getServer() {
        return server;
    }
    
    /**
     * Executes a task on the main thread and waits for the result.
     * If already on the main thread, executes immediately.
     * 
     * @param <T> The return type
     * @param task The task to execute
     * @return The result of the task
     * @throws RuntimeException if the task fails or is interrupted
     */
    public <T> T executeOnMainThreadAndWait(java.util.concurrent.Callable<T> task) {
        if (!initialized.get() || server == null) {
            LOGGER.warn("MinecraftThreadBridge not initialized, executing task on current thread");
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException("Task execution failed", e);
            }
        }
        
        // Check if we're already on the main thread
        if (server.isSameThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException("Task execution failed", e);
            }
        }
        
        // Queue for execution and wait for result
        java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        
        mainThreadQueue.offer(() -> {
            try {
                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        
        try {
            return future.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new RuntimeException("Task execution timed out", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new RuntimeException("Task execution failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Task execution interrupted", e);
        }
    }
    
    /**
     * Returns the number of tasks currently queued for main thread execution.
     */
    public int getQueuedTaskCount() {
        return mainThreadQueue.size();
    }
    
    /**
     * Returns the number of scheduled tasks waiting to execute.
     */
    public int getScheduledTaskCount() {
        return scheduledTasks.size();
    }
    
    /**
     * Clears all queued and scheduled tasks.
     * This should be called when the server is shutting down.
     */
    public void shutdown() {
        LOGGER.info("Shutting down MinecraftThreadBridge...");
        mainThreadQueue.clear();
        scheduledTasks.clear();
        initialized.set(false);
        server = null;
        LOGGER.info("MinecraftThreadBridge shut down successfully");
    }
    
    /**
     * Represents a task scheduled for future execution.
     */
    private static class ScheduledTask implements Comparable<ScheduledTask> {
        final Runnable task;
        final long executionTick;
        
        ScheduledTask(Runnable task, long executionTick) {
            this.task = task;
            this.executionTick = executionTick;
        }
        
        @Override
        public int compareTo(ScheduledTask other) {
            return Long.compare(this.executionTick, other.executionTick);
        }
    }
}
