package ru.mjkey.storykee.runtime.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Handles asynchronous execution of tasks.
 * Provides a thread pool for background operations and manages execution on the main game thread.
 * 
 * Requirements: 39.1, 39.2
 */
public class AsyncExecutor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncExecutor.class);
    private static AsyncExecutor instance;
    
    private final ExecutorService threadPool;
    private final ScheduledExecutorService scheduledExecutor;
    private volatile boolean shutdown = false;
    
    private AsyncExecutor() {
        // Create a thread pool with a reasonable number of threads
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        int maxPoolSize = Runtime.getRuntime().availableProcessors();
        
        this.threadPool = new ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "Storykee-Async-" + counter++);
                    thread.setDaemon(true);
                    return thread;
                }
            }
        );
        
        // Create a scheduled executor for delayed tasks
        this.scheduledExecutor = Executors.newScheduledThreadPool(
            2,
            new ThreadFactory() {
                private int counter = 0;
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "Storykee-Scheduled-" + counter++);
                    thread.setDaemon(true);
                    return thread;
                }
            }
        );
        
        LOGGER.info("AsyncExecutor initialized with {} core threads and {} max threads", 
            corePoolSize, maxPoolSize);
    }
    
    public static AsyncExecutor getInstance() {
        if (instance == null) {
            synchronized (AsyncExecutor.class) {
                if (instance == null) {
                    instance = new AsyncExecutor();
                }
            }
        }
        return instance;
    }
    
    /**
     * Executes a task asynchronously on a background thread.
     * 
     * @param task The task to execute
     * @param <T> The return type of the task
     * @return A CompletableFuture that will complete with the task result
     * 
     * Requirement 39.1: WHEN a script starts an async operation THEN the Runtime SHALL execute it on a background thread
     */
    public <T> CompletableFuture<T> executeAsync(Callable<T> task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("AsyncExecutor has been shut down")
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                LOGGER.error("Async task execution failed", e);
                throw new CompletionException(e);
            }
        }, threadPool);
    }
    
    /**
     * Executes a runnable task asynchronously on a background thread.
     * 
     * @param task The task to execute
     * @return A CompletableFuture that will complete when the task finishes
     * 
     * Requirement 39.1: WHEN a script starts an async operation THEN the Runtime SHALL execute it on a background thread
     */
    public CompletableFuture<Void> executeAsync(Runnable task) {
        if (shutdown) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("AsyncExecutor has been shut down")
            );
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("Async task execution failed", e);
                throw new CompletionException(e);
            }
        }, threadPool);
    }
    
    /**
     * Schedules a task to run asynchronously after a delay.
     * 
     * @param task The task to execute
     * @param delay The delay before execution
     * @param unit The time unit of the delay
     * @return A CompletableFuture that will complete when the task finishes
     */
    public CompletableFuture<Void> scheduleAsync(Runnable task, long delay, TimeUnit unit) {
        if (shutdown) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("AsyncExecutor has been shut down")
            );
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        scheduledExecutor.schedule(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Exception e) {
                LOGGER.error("Scheduled async task execution failed", e);
                future.completeExceptionally(e);
            }
        }, delay, unit);
        
        return future;
    }
    
    /**
     * Executes a task on the main game thread.
     * Delegates to MinecraftThreadBridge for actual main thread execution.
     * 
     * @param task The task to execute on the main thread
     * 
     * Requirement 39.2: WHEN an async operation completes THEN the Runtime SHALL execute the callback on the main game thread
     */
    public void executeOnMainThread(Runnable task) {
        MinecraftThreadBridge bridge = MinecraftThreadBridge.getInstance();
        if (bridge.isInitialized()) {
            bridge.executeOnMainThread(task);
        } else {
            LOGGER.warn("MinecraftThreadBridge not initialized, executing task on current thread");
            task.run();
        }
    }
    
    /**
     * Schedules a task to run on the main game thread after a delay.
     * Delegates to MinecraftThreadBridge for actual main thread scheduling.
     * 
     * @param task The task to execute on the main thread
     * @param delayTicks The delay in game ticks
     * 
     * Requirement 39.2: WHEN an async operation completes THEN the Runtime SHALL execute the callback on the main game thread
     */
    public void scheduleOnMainThread(Runnable task, long delayTicks) {
        MinecraftThreadBridge bridge = MinecraftThreadBridge.getInstance();
        if (bridge.isInitialized()) {
            bridge.scheduleOnMainThread(task, delayTicks);
        } else {
            LOGGER.warn("MinecraftThreadBridge not initialized, executing task on current thread");
            task.run();
        }
    }
    
    /**
     * Shuts down the async executor and waits for pending tasks to complete.
     * 
     * Requirement 39.5: WHEN the game shuts down THEN the Runtime SHALL wait for pending async operations to complete or timeout
     */
    public void shutdown() {
        if (shutdown) {
            return;
        }
        
        shutdown = true;
        LOGGER.info("Shutting down AsyncExecutor...");
        
        threadPool.shutdown();
        scheduledExecutor.shutdown();
        
        try {
            // Wait up to 10 seconds for tasks to complete
            if (!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warn("Thread pool did not terminate in time, forcing shutdown");
                threadPool.shutdownNow();
            }
            
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warn("Scheduled executor did not terminate in time, forcing shutdown");
                scheduledExecutor.shutdownNow();
            }
            
            LOGGER.info("AsyncExecutor shut down successfully");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for executor shutdown", e);
            threadPool.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Returns whether the executor has been shut down.
     */
    public boolean isShutdown() {
        return shutdown;
    }
    
    /**
     * Returns the number of active threads in the pool.
     */
    public int getActiveThreadCount() {
        if (threadPool instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) threadPool).getActiveCount();
        }
        return 0;
    }
    
    /**
     * Returns the number of tasks currently queued for execution.
     */
    public int getQueuedTaskCount() {
        if (threadPool instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) threadPool).getQueue().size();
        }
        return 0;
    }
}
