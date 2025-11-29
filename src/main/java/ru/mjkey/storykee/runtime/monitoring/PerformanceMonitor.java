package ru.mjkey.storykee.runtime.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Monitors script execution performance.
 * Tracks execution times, warns about slow scripts, and can throttle problematic scripts.
 * 
 * Requirements: 30.1, 30.2, 30.3, 30.4, 30.5
 */
public class PerformanceMonitor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceMonitor.class);
    private static PerformanceMonitor instance;
    
    // Configuration
    private volatile boolean enabled = false;
    private volatile long warningThresholdMs = 100; // Warn if script takes longer than this
    private volatile long throttleThresholdMs = 500; // Throttle if script consistently exceeds this
    private volatile int throttleAfterWarnings = 3; // Number of warnings before throttling
    private volatile long throttleDurationMs = 5000; // How long to throttle a script
    
    // Statistics per script
    private final Map<String, ScriptStatistics> statistics = new ConcurrentHashMap<>();
    
    // Throttled scripts
    private final Map<String, ThrottleInfo> throttledScripts = new ConcurrentHashMap<>();
    
    // Warning counts for throttle decisions
    private final Map<String, Integer> consecutiveWarnings = new ConcurrentHashMap<>();
    
    // Listeners for performance events
    private final List<Consumer<PerformanceEvent>> listeners = new ArrayList<>();
    
    private PerformanceMonitor() {}
    
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Enables or disables performance monitoring.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LOGGER.info("Performance monitoring {}", enabled ? "enabled" : "disabled");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Sets the warning threshold in milliseconds.
     */
    public void setWarningThresholdMs(long thresholdMs) {
        this.warningThresholdMs = thresholdMs;
    }
    
    public long getWarningThresholdMs() {
        return warningThresholdMs;
    }
    
    /**
     * Sets the throttle threshold in milliseconds.
     */
    public void setThrottleThresholdMs(long thresholdMs) {
        this.throttleThresholdMs = thresholdMs;
    }
    
    public long getThrottleThresholdMs() {
        return throttleThresholdMs;
    }
    
    /**
     * Sets how many consecutive warnings before throttling.
     */
    public void setThrottleAfterWarnings(int count) {
        this.throttleAfterWarnings = count;
    }
    
    /**
     * Sets how long to throttle a script.
     */
    public void setThrottleDurationMs(long durationMs) {
        this.throttleDurationMs = durationMs;
    }
    
    /**
     * Checks if a script is currently throttled.
     */
    public boolean isThrottled(String scriptId) {
        ThrottleInfo info = throttledScripts.get(scriptId);
        if (info == null) {
            return false;
        }
        
        // Check if throttle has expired
        if (System.currentTimeMillis() > info.throttleUntil) {
            throttledScripts.remove(scriptId);
            LOGGER.info("Script {} throttle expired", scriptId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Records the start of a script execution.
     * Returns an ExecutionTracker to track the execution.
     */
    public ExecutionTracker startExecution(String scriptId) {
        return new ExecutionTracker(scriptId, System.currentTimeMillis());
    }
    
    /**
     * Records the completion of a script execution.
     */
    public void recordExecution(String scriptId, long executionTimeMs, boolean success) {
        if (!enabled) return;
        
        // Get or create statistics
        ScriptStatistics stats = statistics.computeIfAbsent(scriptId, ScriptStatistics::new);
        stats.recordExecution(executionTimeMs);
        
        if (!success) {
            stats.recordError();
        }
        
        // Check for slow execution
        if (executionTimeMs > warningThresholdMs) {
            handleSlowExecution(scriptId, executionTimeMs, stats);
        } else {
            // Reset consecutive warnings on fast execution
            consecutiveWarnings.remove(scriptId);
        }
        
        LOGGER.debug("Script {} executed in {}ms", scriptId, executionTimeMs);
    }
    
    private void handleSlowExecution(String scriptId, long executionTimeMs, ScriptStatistics stats) {
        stats.recordWarning();
        
        int warnings = consecutiveWarnings.merge(scriptId, 1, Integer::sum);
        
        LOGGER.warn("Script {} took {}ms (threshold: {}ms) - warning {}/{}", 
            scriptId, executionTimeMs, warningThresholdMs, warnings, throttleAfterWarnings);
        
        // Notify listeners
        fireEvent(new PerformanceEvent(
            PerformanceEvent.Type.SLOW_EXECUTION,
            scriptId,
            executionTimeMs,
            String.format("Script took %dms (threshold: %dms)", executionTimeMs, warningThresholdMs)
        ));
        
        // Check if we should throttle
        if (executionTimeMs > throttleThresholdMs && warnings >= throttleAfterWarnings) {
            throttleScript(scriptId, stats);
        }
    }
    
    private void throttleScript(String scriptId, ScriptStatistics stats) {
        long throttleUntil = System.currentTimeMillis() + throttleDurationMs;
        throttledScripts.put(scriptId, new ThrottleInfo(scriptId, throttleUntil));
        stats.recordThrottle();
        consecutiveWarnings.remove(scriptId);
        
        LOGGER.warn("Script {} throttled for {}ms due to repeated slow execution", 
            scriptId, throttleDurationMs);
        
        // Notify listeners
        fireEvent(new PerformanceEvent(
            PerformanceEvent.Type.SCRIPT_THROTTLED,
            scriptId,
            throttleDurationMs,
            String.format("Script throttled for %dms", throttleDurationMs)
        ));
    }
    
    /**
     * Manually unthrottles a script.
     */
    public void unthrottle(String scriptId) {
        if (throttledScripts.remove(scriptId) != null) {
            LOGGER.info("Script {} manually unthrottled", scriptId);
        }
    }
    
    /**
     * Gets statistics for a specific script.
     */
    public ScriptStatistics getStatistics(String scriptId) {
        return statistics.get(scriptId);
    }
    
    /**
     * Gets statistics for all scripts.
     */
    public Map<String, ScriptStatistics> getAllStatistics() {
        return Collections.unmodifiableMap(new HashMap<>(statistics));
    }
    
    /**
     * Gets a list of currently throttled scripts.
     */
    public List<String> getThrottledScripts() {
        // Clean up expired throttles
        long now = System.currentTimeMillis();
        throttledScripts.entrySet().removeIf(e -> now > e.getValue().throttleUntil);
        
        return new ArrayList<>(throttledScripts.keySet());
    }
    
    /**
     * Resets all statistics.
     */
    public void resetStatistics() {
        statistics.clear();
        consecutiveWarnings.clear();
        LOGGER.info("Performance statistics reset");
    }
    
    /**
     * Resets statistics for a specific script.
     */
    public void resetStatistics(String scriptId) {
        ScriptStatistics stats = statistics.get(scriptId);
        if (stats != null) {
            stats.reset();
        }
        consecutiveWarnings.remove(scriptId);
    }
    
    /**
     * Adds a listener for performance events.
     */
    public void addListener(Consumer<PerformanceEvent> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a listener.
     */
    public void removeListener(Consumer<PerformanceEvent> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private void fireEvent(PerformanceEvent event) {
        synchronized (listeners) {
            for (Consumer<PerformanceEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    LOGGER.error("Error in performance event listener", e);
                }
            }
        }
    }
    
    /**
     * Generates a performance report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Report ===\n");
        sb.append(String.format("Monitoring: %s\n", enabled ? "enabled" : "disabled"));
        sb.append(String.format("Warning threshold: %dms\n", warningThresholdMs));
        sb.append(String.format("Throttle threshold: %dms\n", throttleThresholdMs));
        sb.append("\n--- Script Statistics ---\n");
        
        List<ScriptStatistics> sortedStats = new ArrayList<>(statistics.values());
        sortedStats.sort((a, b) -> Double.compare(b.getAverageExecutionTimeMs(), a.getAverageExecutionTimeMs()));
        
        for (ScriptStatistics stats : sortedStats) {
            sb.append(String.format(
                "%s: %d executions, avg=%.2fms, min=%dms, max=%dms, errors=%d, warnings=%d\n",
                stats.getScriptId(),
                stats.getExecutionCount(),
                stats.getAverageExecutionTimeMs(),
                stats.getMinExecutionTimeMs(),
                stats.getMaxExecutionTimeMs(),
                stats.getErrorCount(),
                stats.getWarningCount()
            ));
        }
        
        List<String> throttled = getThrottledScripts();
        if (!throttled.isEmpty()) {
            sb.append("\n--- Throttled Scripts ---\n");
            for (String scriptId : throttled) {
                ThrottleInfo info = throttledScripts.get(scriptId);
                if (info != null) {
                    long remaining = info.throttleUntil - System.currentTimeMillis();
                    sb.append(String.format("%s: %dms remaining\n", scriptId, Math.max(0, remaining)));
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Tracks a single script execution.
     */
    public class ExecutionTracker {
        private final String scriptId;
        private final long startTime;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        
        ExecutionTracker(String scriptId, long startTime) {
            this.scriptId = scriptId;
            this.startTime = startTime;
        }
        
        public void complete(boolean success) {
            if (completed.compareAndSet(false, true)) {
                long executionTime = System.currentTimeMillis() - startTime;
                recordExecution(scriptId, executionTime, success);
            }
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public String getScriptId() {
            return scriptId;
        }
    }
    
    /**
     * Information about a throttled script.
     */
    private static class ThrottleInfo {
        final String scriptId;
        final long throttleUntil;
        
        ThrottleInfo(String scriptId, long throttleUntil) {
            this.scriptId = scriptId;
            this.throttleUntil = throttleUntil;
        }
    }
}
