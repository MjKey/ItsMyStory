package ru.mjkey.storykee.systems.timer;

import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.async.MinecraftThreadBridge;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages timers and scheduled tasks in the story system.
 * Provides delayed and repeating task scheduling with pause/resume functionality.
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4
 * 
 * 14.1: WHEN a script schedules a delayed action THEN the Runtime SHALL execute it after the specified time
 * 14.2: WHEN a script creates a repeating timer THEN the Runtime SHALL execute the action at each interval
 * 14.3: WHEN a timer is cancelled THEN the Runtime SHALL prevent future executions
 * 14.4: WHEN the game is paused THEN the Runtime SHALL pause all story timers
 */
public class TimerManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TimerManager.class);
    private static TimerManager instance;
    
    private final Map<UUID, StoryTimer> timers;
    private final AtomicBoolean paused;
    private final AtomicBoolean initialized;
    private final AtomicLong currentTick;
    private final AtomicLong pausedAtTick;
    
    private MinecraftServer server;
    
    private TimerManager() {
        this.timers = new ConcurrentHashMap<>();
        this.paused = new AtomicBoolean(false);
        this.initialized = new AtomicBoolean(false);
        this.currentTick = new AtomicLong(0);
        this.pausedAtTick = new AtomicLong(0);
    }
    
    public static TimerManager getInstance() {
        if (instance == null) {
            synchronized (TimerManager.class) {
                if (instance == null) {
                    instance = new TimerManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initializes the TimerManager with the Minecraft server instance.
     * This should be called when the server starts.
     * 
     * @param server The Minecraft server instance
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        this.initialized.set(true);
        this.currentTick.set(server.getTickCount());
        LOGGER.info("TimerManager initialized");
    }

    /**
     * Schedules a task to execute after a delay.
     * 
     * Requirement 14.1: WHEN a script schedules a delayed action THEN the Runtime SHALL execute it after the specified time
     * 
     * @param task The task to execute
     * @param delayTicks The delay in game ticks (20 ticks = 1 second)
     * @return The unique ID of the scheduled timer
     */
    public UUID scheduleDelayed(Runnable task, long delayTicks) {
        if (!initialized.get()) {
            LOGGER.warn("TimerManager not initialized, cannot schedule delayed task");
            return null;
        }
        
        if (task == null) {
            LOGGER.warn("Cannot schedule null task");
            return null;
        }
        
        if (delayTicks < 0) {
            LOGGER.warn("Delay ticks cannot be negative, using 0");
            delayTicks = 0;
        }
        
        UUID timerId = UUID.randomUUID();
        long executionTick = currentTick.get() + delayTicks;
        
        StoryTimer timer = new StoryTimer(
            timerId,
            task,
            executionTick,
            0, // Not repeating
            false,
            System.currentTimeMillis()
        );
        
        timers.put(timerId, timer);
        LOGGER.debug("Scheduled delayed task {} to execute at tick {} (delay: {} ticks)", 
            timerId, executionTick, delayTicks);
        
        return timerId;
    }
    
    /**
     * Schedules a task to execute repeatedly at a fixed interval.
     * 
     * Requirement 14.2: WHEN a script creates a repeating timer THEN the Runtime SHALL execute the action at each interval
     * 
     * @param task The task to execute
     * @param intervalTicks The interval between executions in game ticks (20 ticks = 1 second)
     * @return The unique ID of the scheduled timer
     */
    public UUID scheduleRepeating(Runnable task, long intervalTicks) {
        return scheduleRepeating(task, intervalTicks, 0);
    }
    
    /**
     * Schedules a task to execute repeatedly at a fixed interval with an initial delay.
     * 
     * Requirement 14.2: WHEN a script creates a repeating timer THEN the Runtime SHALL execute the action at each interval
     * 
     * @param task The task to execute
     * @param intervalTicks The interval between executions in game ticks (20 ticks = 1 second)
     * @param initialDelayTicks The initial delay before the first execution
     * @return The unique ID of the scheduled timer
     */
    public UUID scheduleRepeating(Runnable task, long intervalTicks, long initialDelayTicks) {
        if (!initialized.get()) {
            LOGGER.warn("TimerManager not initialized, cannot schedule repeating task");
            return null;
        }
        
        if (task == null) {
            LOGGER.warn("Cannot schedule null task");
            return null;
        }
        
        if (intervalTicks <= 0) {
            LOGGER.warn("Interval ticks must be positive, using 1");
            intervalTicks = 1;
        }
        
        if (initialDelayTicks < 0) {
            LOGGER.warn("Initial delay ticks cannot be negative, using 0");
            initialDelayTicks = 0;
        }
        
        UUID timerId = UUID.randomUUID();
        long executionTick = currentTick.get() + initialDelayTicks;
        
        StoryTimer timer = new StoryTimer(
            timerId,
            task,
            executionTick,
            intervalTicks,
            true,
            System.currentTimeMillis()
        );
        
        timers.put(timerId, timer);
        LOGGER.debug("Scheduled repeating task {} to execute at tick {} (interval: {} ticks)", 
            timerId, executionTick, intervalTicks);
        
        return timerId;
    }
    
    /**
     * Cancels a scheduled timer.
     * 
     * Requirement 14.3: WHEN a timer is cancelled THEN the Runtime SHALL prevent future executions
     * 
     * @param timerId The ID of the timer to cancel
     * @return true if the timer was found and cancelled, false otherwise
     */
    public boolean cancel(UUID timerId) {
        if (timerId == null) {
            return false;
        }
        
        StoryTimer removed = timers.remove(timerId);
        if (removed != null) {
            LOGGER.debug("Cancelled timer {}", timerId);
            return true;
        }
        
        LOGGER.debug("Timer {} not found for cancellation", timerId);
        return false;
    }

    /**
     * Pauses all story timers.
     * 
     * Requirement 14.4: WHEN the game is paused THEN the Runtime SHALL pause all story timers
     */
    public void pauseAll() {
        if (paused.compareAndSet(false, true)) {
            pausedAtTick.set(currentTick.get());
            LOGGER.info("All story timers paused at tick {}", pausedAtTick.get());
        }
    }
    
    /**
     * Resumes all story timers.
     * Adjusts execution times to account for the pause duration.
     * 
     * Requirement 14.4: WHEN the game is paused THEN the Runtime SHALL pause all story timers
     */
    public void resumeAll() {
        if (paused.compareAndSet(true, false)) {
            long pauseDuration = currentTick.get() - pausedAtTick.get();
            
            // Adjust all timer execution times to account for the pause
            for (StoryTimer timer : timers.values()) {
                timer.adjustForPause(pauseDuration);
            }
            
            LOGGER.info("All story timers resumed, adjusted for {} tick pause", pauseDuration);
        }
    }
    
    /**
     * Returns whether timers are currently paused.
     * 
     * @return true if timers are paused, false otherwise
     */
    public boolean isPaused() {
        return paused.get();
    }
    
    /**
     * Processes all timers. This should be called every server tick.
     * Executes any timers that are due and reschedules repeating timers.
     * 
     * Requirement 14.5: WHEN multiple timers expire simultaneously THEN the Runtime SHALL execute them in creation order
     */
    public void tick() {
        if (!initialized.get()) {
            return;
        }
        
        // Update current tick
        if (server != null) {
            currentTick.set(server.getTickCount());
        } else {
            currentTick.incrementAndGet();
        }
        
        // Don't process timers if paused
        if (paused.get()) {
            return;
        }
        
        long tick = currentTick.get();
        
        // Sort timers by creation time to ensure execution order (Requirement 14.5)
        timers.values().stream()
            .filter(timer -> timer.getNextExecutionTick() <= tick)
            .sorted((a, b) -> Long.compare(a.getCreationTime(), b.getCreationTime()))
            .forEach(timer -> executeTimer(timer, tick));
    }
    
    /**
     * Executes a timer and handles rescheduling for repeating timers.
     */
    private void executeTimer(StoryTimer timer, long currentTick) {
        try {
            // Execute on main thread to ensure thread safety
            MinecraftThreadBridge bridge = MinecraftThreadBridge.getInstance();
            if (bridge.isInitialized()) {
                bridge.executeOnMainThread(timer.getTask());
            } else {
                timer.getTask().run();
            }
            
            LOGGER.debug("Executed timer {} at tick {}", timer.getId(), currentTick);
            
            // Handle repeating timers
            if (timer.isRepeating()) {
                timer.reschedule(currentTick);
                LOGGER.debug("Rescheduled repeating timer {} for tick {}", 
                    timer.getId(), timer.getNextExecutionTick());
            } else {
                // Remove one-shot timers after execution
                timers.remove(timer.getId());
                LOGGER.debug("Removed one-shot timer {} after execution", timer.getId());
            }
        } catch (Exception e) {
            LOGGER.error("Error executing timer {}: {}", timer.getId(), e.getMessage(), e);
            // Remove failed timers to prevent repeated errors
            if (!timer.isRepeating()) {
                timers.remove(timer.getId());
            }
        }
    }
    
    /**
     * Returns the number of active timers.
     * 
     * @return The count of active timers
     */
    public int getActiveTimerCount() {
        return timers.size();
    }
    
    /**
     * Returns whether a timer with the given ID exists.
     * 
     * @param timerId The timer ID to check
     * @return true if the timer exists, false otherwise
     */
    public boolean hasTimer(UUID timerId) {
        return timerId != null && timers.containsKey(timerId);
    }
    
    /**
     * Returns the remaining ticks until a timer executes.
     * 
     * @param timerId The timer ID
     * @return The remaining ticks, or -1 if the timer doesn't exist
     */
    public long getRemainingTicks(UUID timerId) {
        if (timerId == null) {
            return -1;
        }
        
        StoryTimer timer = timers.get(timerId);
        if (timer == null) {
            return -1;
        }
        
        return Math.max(0, timer.getNextExecutionTick() - currentTick.get());
    }
    
    /**
     * Returns whether the TimerManager is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return initialized.get();
    }
    
    /**
     * Returns the current tick count.
     * 
     * @return The current tick
     */
    public long getCurrentTick() {
        return currentTick.get();
    }
    
    /**
     * Cancels all active timers.
     */
    public void cancelAll() {
        int count = timers.size();
        timers.clear();
        LOGGER.info("Cancelled all {} timers", count);
    }
    
    /**
     * Shuts down the TimerManager.
     * Cancels all timers and resets state.
     */
    public void shutdown() {
        LOGGER.info("Shutting down TimerManager...");
        cancelAll();
        paused.set(false);
        initialized.set(false);
        server = null;
        LOGGER.info("TimerManager shut down successfully");
    }

    /**
     * Internal class representing a scheduled timer.
     */
    private static class StoryTimer {
        private final UUID id;
        private final Runnable task;
        private long nextExecutionTick;
        private final long intervalTicks;
        private final boolean repeating;
        private final long creationTime;
        
        StoryTimer(UUID id, Runnable task, long nextExecutionTick, 
                   long intervalTicks, boolean repeating, long creationTime) {
            this.id = id;
            this.task = task;
            this.nextExecutionTick = nextExecutionTick;
            this.intervalTicks = intervalTicks;
            this.repeating = repeating;
            this.creationTime = creationTime;
        }
        
        UUID getId() {
            return id;
        }
        
        Runnable getTask() {
            return task;
        }
        
        long getNextExecutionTick() {
            return nextExecutionTick;
        }
        
        long getIntervalTicks() {
            return intervalTicks;
        }
        
        boolean isRepeating() {
            return repeating;
        }
        
        long getCreationTime() {
            return creationTime;
        }
        
        /**
         * Reschedules the timer for the next execution.
         * 
         * @param currentTick The current tick
         */
        void reschedule(long currentTick) {
            nextExecutionTick = currentTick + intervalTicks;
        }
        
        /**
         * Adjusts the execution time to account for a pause.
         * 
         * @param pauseDuration The duration of the pause in ticks
         */
        void adjustForPause(long pauseDuration) {
            nextExecutionTick += pauseDuration;
        }
    }
}
