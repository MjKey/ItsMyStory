package ru.mjkey.storykee.runtime.executor;

/**
 * Result of script execution.
 */
public class ExecutionResult {
    
    private final boolean success;
    private final Object returnValue;
    private final Exception error;
    private final long executionTimeMs;
    
    private ExecutionResult(boolean success, Object returnValue, Exception error, long executionTimeMs) {
        this.success = success;
        this.returnValue = returnValue;
        this.error = error;
        this.executionTimeMs = executionTimeMs;
    }
    
    public static ExecutionResult success(Object returnValue, long executionTimeMs) {
        return new ExecutionResult(true, returnValue, null, executionTimeMs);
    }
    
    public static ExecutionResult failure(Exception error, long executionTimeMs) {
        return new ExecutionResult(false, null, error, executionTimeMs);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Object getReturnValue() {
        return returnValue;
    }
    
    public Exception getError() {
        return error;
    }
    
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
}
