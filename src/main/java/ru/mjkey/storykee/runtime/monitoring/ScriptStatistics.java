package ru.mjkey.storykee.runtime.monitoring;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for a single script's execution.
 * Thread-safe for concurrent access.
 */
public class ScriptStatistics {
    
    private final String scriptId;
    private final AtomicLong executionCount = new AtomicLong(0);
    private final AtomicLong totalExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong minExecutionTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTimeMs = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong warningCount = new AtomicLong(0);
    private final AtomicLong throttleCount = new AtomicLong(0);
    private volatile long lastExecutionTimeMs = 0;
    private volatile long lastExecutionTimestamp = 0;
    
    public ScriptStatistics(String scriptId) {
        this.scriptId = scriptId;
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public void recordExecution(long executionTimeMs) {
        executionCount.incrementAndGet();
        totalExecutionTimeMs.addAndGet(executionTimeMs);
        lastExecutionTimeMs = executionTimeMs;
        lastExecutionTimestamp = System.currentTimeMillis();
        
        // Update min/max atomically
        long currentMin;
        do {
            currentMin = minExecutionTimeMs.get();
            if (executionTimeMs >= currentMin) break;
        } while (!minExecutionTimeMs.compareAndSet(currentMin, executionTimeMs));
        
        long currentMax;
        do {
            currentMax = maxExecutionTimeMs.get();
            if (executionTimeMs <= currentMax) break;
        } while (!maxExecutionTimeMs.compareAndSet(currentMax, executionTimeMs));
    }
    
    public void recordError() {
        errorCount.incrementAndGet();
    }
    
    public void recordWarning() {
        warningCount.incrementAndGet();
    }
    
    public void recordThrottle() {
        throttleCount.incrementAndGet();
    }
    
    public long getExecutionCount() {
        return executionCount.get();
    }
    
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs.get();
    }
    
    public long getMinExecutionTimeMs() {
        long min = minExecutionTimeMs.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }
    
    public long getMaxExecutionTimeMs() {
        return maxExecutionTimeMs.get();
    }
    
    public double getAverageExecutionTimeMs() {
        long count = executionCount.get();
        if (count == 0) return 0.0;
        return (double) totalExecutionTimeMs.get() / count;
    }
    
    public long getLastExecutionTimeMs() {
        return lastExecutionTimeMs;
    }
    
    public long getLastExecutionTimestamp() {
        return lastExecutionTimestamp;
    }
    
    public long getErrorCount() {
        return errorCount.get();
    }
    
    public long getWarningCount() {
        return warningCount.get();
    }
    
    public long getThrottleCount() {
        return throttleCount.get();
    }
    
    public void reset() {
        executionCount.set(0);
        totalExecutionTimeMs.set(0);
        minExecutionTimeMs.set(Long.MAX_VALUE);
        maxExecutionTimeMs.set(0);
        errorCount.set(0);
        warningCount.set(0);
        throttleCount.set(0);
        lastExecutionTimeMs = 0;
        lastExecutionTimestamp = 0;
    }
    
    @Override
    public String toString() {
        return String.format(
            "ScriptStatistics{scriptId='%s', executions=%d, avgTime=%.2fms, minTime=%dms, maxTime=%dms, errors=%d, warnings=%d, throttles=%d}",
            scriptId, getExecutionCount(), getAverageExecutionTimeMs(), 
            getMinExecutionTimeMs(), getMaxExecutionTimeMs(),
            getErrorCount(), getWarningCount(), getThrottleCount()
        );
    }
}
