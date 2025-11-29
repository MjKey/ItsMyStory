package ru.mjkey.storykee.runtime.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.ASTNode;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.runtime.context.ExecutionContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Debugger for Storykee scripts.
 * Supports breakpoints, stepping, variable inspection, and watch expressions.
 * 
 * Requirements: 40.1, 40.2, 40.3, 40.4, 40.5
 */
public class Debugger {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Debugger.class);
    private static Debugger instance;
    
    // Debug mode flag
    private volatile boolean debugMode = false;
    
    // Breakpoints by script ID
    private final Map<String, Set<Breakpoint>> breakpoints = new ConcurrentHashMap<>();
    
    // Watch expressions
    private final Map<String, WatchExpression> watchExpressions = new ConcurrentHashMap<>();
    private final AtomicInteger watchIdCounter = new AtomicInteger(0);
    
    // Current debug state (when paused)
    private volatile DebugState currentState = null;
    
    // Latch for pausing execution
    private volatile CountDownLatch pauseLatch = null;
    
    // Step mode for next execution
    private volatile DebugState.StepMode stepMode = DebugState.StepMode.CONTINUE;
    
    // Step tracking
    private volatile int stepDepth = 0;
    private volatile String stepScriptId = null;
    
    // Listeners for debug events
    private final List<Consumer<DebugEvent>> listeners = new ArrayList<>();
    
    private Debugger() {}
    
    public static synchronized Debugger getInstance() {
        if (instance == null) {
            instance = new Debugger();
        }
        return instance;
    }
    
    // ===== Debug Mode =====
    
    /**
     * Enables or disables debug mode.
     */
    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        LOGGER.info("Debug mode {}", enabled ? "enabled" : "disabled");
        
        if (!enabled) {
            // Resume if paused
            resume();
        }
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    // ===== Breakpoints =====
    
    /**
     * Adds a breakpoint at the specified location.
     */
    public Breakpoint addBreakpoint(String scriptId, int line) {
        return addBreakpoint(scriptId, line, null);
    }
    
    /**
     * Adds a conditional breakpoint.
     */
    public Breakpoint addBreakpoint(String scriptId, int line, String condition) {
        Breakpoint bp = new Breakpoint(scriptId, line, condition);
        breakpoints.computeIfAbsent(scriptId, k -> ConcurrentHashMap.newKeySet()).add(bp);
        
        LOGGER.info("Added breakpoint: {}", bp);
        fireEvent(new DebugEvent(DebugEvent.Type.BREAKPOINT_ADDED, bp.toString()));
        
        return bp;
    }
    
    /**
     * Removes a breakpoint.
     */
    public boolean removeBreakpoint(String scriptId, int line) {
        Set<Breakpoint> scriptBreakpoints = breakpoints.get(scriptId);
        if (scriptBreakpoints != null) {
            boolean removed = scriptBreakpoints.removeIf(bp -> bp.getLine() == line);
            if (removed) {
                LOGGER.info("Removed breakpoint at {}:{}", scriptId, line);
                fireEvent(new DebugEvent(DebugEvent.Type.BREAKPOINT_REMOVED, scriptId + ":" + line));
            }
            return removed;
        }
        return false;
    }
    
    /**
     * Removes all breakpoints for a script.
     */
    public void clearBreakpoints(String scriptId) {
        breakpoints.remove(scriptId);
        LOGGER.info("Cleared all breakpoints for script: {}", scriptId);
    }
    
    /**
     * Removes all breakpoints.
     */
    public void clearAllBreakpoints() {
        breakpoints.clear();
        LOGGER.info("Cleared all breakpoints");
    }
    
    /**
     * Gets all breakpoints for a script.
     */
    public Set<Breakpoint> getBreakpoints(String scriptId) {
        return breakpoints.getOrDefault(scriptId, Collections.emptySet());
    }
    
    /**
     * Gets all breakpoints.
     */
    public Map<String, Set<Breakpoint>> getAllBreakpoints() {
        return Collections.unmodifiableMap(breakpoints);
    }
    
    // ===== Watch Expressions =====
    
    /**
     * Adds a watch expression.
     */
    public WatchExpression addWatch(String expression) {
        String id = "watch_" + watchIdCounter.incrementAndGet();
        WatchExpression watch = new WatchExpression(id, expression);
        watchExpressions.put(id, watch);
        
        LOGGER.info("Added watch expression: {}", expression);
        return watch;
    }
    
    /**
     * Removes a watch expression.
     */
    public boolean removeWatch(String id) {
        WatchExpression removed = watchExpressions.remove(id);
        if (removed != null) {
            LOGGER.info("Removed watch expression: {}", removed.getExpression());
            return true;
        }
        return false;
    }
    
    /**
     * Clears all watch expressions.
     */
    public void clearWatches() {
        watchExpressions.clear();
        LOGGER.info("Cleared all watch expressions");
    }
    
    /**
     * Gets all watch expressions.
     */
    public Collection<WatchExpression> getWatches() {
        return Collections.unmodifiableCollection(watchExpressions.values());
    }
    
    /**
     * Evaluates all watch expressions with the current context.
     */
    public void evaluateWatches(ExecutionContext context) {
        for (WatchExpression watch : watchExpressions.values()) {
            if (!watch.isEnabled()) continue;
            
            try {
                // Simple variable lookup for now
                String expr = watch.getExpression().trim();
                if (context.hasVariable(expr)) {
                    watch.setLastValue(context.getVariable(expr));
                } else {
                    watch.setLastError("Variable not found: " + expr);
                }
            } catch (Exception e) {
                watch.setLastError(e.getMessage());
            }
        }
    }
    
    // ===== Execution Control =====
    
    /**
     * Called before executing a statement to check for breakpoints.
     * Returns true if execution should pause.
     */
    public boolean shouldPause(String scriptId, ASTNode node, ExecutionContext context) {
        if (!debugMode) return false;
        
        SourceLocation location = node.getLocation();
        if (location == null) return false;
        
        // Check step mode
        if (shouldPauseForStep(scriptId, context)) {
            pause(scriptId, location, context, null);
            return true;
        }
        
        // Check breakpoints
        Breakpoint hitBreakpoint = checkBreakpoints(scriptId, location, context);
        if (hitBreakpoint != null) {
            pause(scriptId, location, context, hitBreakpoint);
            return true;
        }
        
        return false;
    }
    
    private boolean shouldPauseForStep(String scriptId, ExecutionContext context) {
        switch (stepMode) {
            case STEP_OVER:
                // Pause if we're at the same or lower depth
                return scriptId.equals(stepScriptId) && 
                       context.getCallStack().getDepth() <= stepDepth;
                       
            case STEP_INTO:
                // Always pause on next statement
                return true;
                
            case STEP_OUT:
                // Pause when we return to a lower depth
                return scriptId.equals(stepScriptId) && 
                       context.getCallStack().getDepth() < stepDepth;
                       
            default:
                return false;
        }
    }
    
    private Breakpoint checkBreakpoints(String scriptId, SourceLocation location, ExecutionContext context) {
        Set<Breakpoint> scriptBreakpoints = breakpoints.get(scriptId);
        if (scriptBreakpoints == null || scriptBreakpoints.isEmpty()) {
            return null;
        }
        
        for (Breakpoint bp : scriptBreakpoints) {
            if (!bp.isEnabled()) continue;
            if (!bp.matches(scriptId, location)) continue;
            
            // Check condition if present
            if (bp.hasCondition()) {
                try {
                    // Simple condition evaluation - just check if variable is truthy
                    String condition = bp.getCondition().trim();
                    if (context.hasVariable(condition)) {
                        Object value = context.getVariable(condition);
                        if (!isTruthy(value)) continue;
                    } else {
                        continue; // Condition variable not found, skip
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error evaluating breakpoint condition: {}", e.getMessage());
                    continue;
                }
            }
            
            bp.incrementHitCount();
            return bp;
        }
        
        return null;
    }
    
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof String) return !((String) value).isEmpty();
        return true;
    }
    
    /**
     * Pauses execution at the current location.
     */
    private void pause(String scriptId, SourceLocation location, ExecutionContext context, Breakpoint breakpoint) {
        // Reset step mode
        stepMode = DebugState.StepMode.CONTINUE;
        
        // Create debug state
        currentState = new DebugState(scriptId, location, context, breakpoint, stepMode);
        
        // Evaluate watch expressions
        evaluateWatches(context);
        
        // Notify listeners
        String message = breakpoint != null 
            ? "Hit breakpoint at " + breakpoint 
            : "Paused at " + scriptId + ":" + location.line();
        fireEvent(new DebugEvent(DebugEvent.Type.PAUSED, message, currentState));
        
        LOGGER.info("Debugger paused: {}", message);
        
        // Wait for resume
        pauseLatch = new CountDownLatch(1);
        try {
            pauseLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Debug pause interrupted");
        }
        
        currentState = null;
    }
    
    /**
     * Resumes execution.
     */
    public void resume() {
        stepMode = DebugState.StepMode.CONTINUE;
        releasePause();
        fireEvent(new DebugEvent(DebugEvent.Type.RESUMED, "Execution resumed"));
        LOGGER.info("Debugger resumed");
    }
    
    /**
     * Steps over the current statement.
     */
    public void stepOver() {
        if (currentState != null) {
            stepMode = DebugState.StepMode.STEP_OVER;
            stepScriptId = currentState.getScriptId();
            stepDepth = currentState.getContext().getCallStack().getDepth();
        }
        releasePause();
        fireEvent(new DebugEvent(DebugEvent.Type.STEPPING, "Step over"));
        LOGGER.debug("Step over");
    }
    
    /**
     * Steps into function calls.
     */
    public void stepInto() {
        if (currentState != null) {
            stepMode = DebugState.StepMode.STEP_INTO;
            stepScriptId = currentState.getScriptId();
            stepDepth = currentState.getContext().getCallStack().getDepth();
        }
        releasePause();
        fireEvent(new DebugEvent(DebugEvent.Type.STEPPING, "Step into"));
        LOGGER.debug("Step into");
    }
    
    /**
     * Steps out of the current function.
     */
    public void stepOut() {
        if (currentState != null) {
            stepMode = DebugState.StepMode.STEP_OUT;
            stepScriptId = currentState.getScriptId();
            stepDepth = currentState.getContext().getCallStack().getDepth();
        }
        releasePause();
        fireEvent(new DebugEvent(DebugEvent.Type.STEPPING, "Step out"));
        LOGGER.debug("Step out");
    }
    
    private void releasePause() {
        CountDownLatch latch = pauseLatch;
        if (latch != null) {
            latch.countDown();
        }
    }
    
    // ===== State Inspection =====
    
    /**
     * Checks if the debugger is currently paused.
     */
    public boolean isPaused() {
        return currentState != null;
    }
    
    /**
     * Gets the current debug state (when paused).
     */
    public DebugState getCurrentState() {
        return currentState;
    }
    
    /**
     * Inspects a variable in the current context.
     */
    public Object inspectVariable(String name) {
        if (currentState == null) {
            return null;
        }
        return currentState.getVariable(name);
    }
    
    /**
     * Gets all variables in the current scope.
     */
    public Map<String, Object> inspectAllVariables() {
        if (currentState == null) {
            return Collections.emptyMap();
        }
        return currentState.getAllVariables();
    }
    
    /**
     * Gets the current call stack.
     */
    public String getCallStack() {
        if (currentState == null) {
            return "Not paused";
        }
        return currentState.getCallStackString();
    }
    
    // ===== Event Listeners =====
    
    /**
     * Adds a debug event listener.
     */
    public void addListener(Consumer<DebugEvent> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a debug event listener.
     */
    public void removeListener(Consumer<DebugEvent> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private void fireEvent(DebugEvent event) {
        synchronized (listeners) {
            for (Consumer<DebugEvent> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (Exception e) {
                    LOGGER.error("Error in debug event listener", e);
                }
            }
        }
    }
    
    /**
     * Generates a debug status report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Debug Status ===\n");
        sb.append(String.format("Debug mode: %s\n", debugMode ? "enabled" : "disabled"));
        sb.append(String.format("Paused: %s\n", isPaused()));
        
        if (currentState != null) {
            sb.append(String.format("Location: %s:%d\n", 
                currentState.getScriptId(), 
                currentState.getLocation() != null ? currentState.getLocation().line() : -1));
        }
        
        sb.append("\n--- Breakpoints ---\n");
        for (Map.Entry<String, Set<Breakpoint>> entry : breakpoints.entrySet()) {
            for (Breakpoint bp : entry.getValue()) {
                sb.append(bp).append("\n");
            }
        }
        
        sb.append("\n--- Watch Expressions ---\n");
        for (WatchExpression watch : watchExpressions.values()) {
            sb.append(watch).append("\n");
        }
        
        if (currentState != null) {
            sb.append("\n--- Variables ---\n");
            for (Map.Entry<String, Object> entry : currentState.getAllVariables().entrySet()) {
                sb.append(String.format("%s = %s\n", entry.getKey(), entry.getValue()));
            }
            
            sb.append("\n--- Call Stack ---\n");
            sb.append(currentState.getCallStackString());
        }
        
        return sb.toString();
    }
}
