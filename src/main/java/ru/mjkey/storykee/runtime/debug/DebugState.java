package ru.mjkey.storykee.runtime.debug;

import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.runtime.context.Scope;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the current state of a paused debug session.
 */
public class DebugState {
    
    private final String scriptId;
    private final SourceLocation location;
    private final ExecutionContext context;
    private final Breakpoint hitBreakpoint;
    private final StepMode stepMode;
    private final long pauseTimestamp;
    
    public DebugState(String scriptId, SourceLocation location, ExecutionContext context, 
                      Breakpoint hitBreakpoint, StepMode stepMode) {
        this.scriptId = scriptId;
        this.location = location;
        this.context = context;
        this.hitBreakpoint = hitBreakpoint;
        this.stepMode = stepMode;
        this.pauseTimestamp = System.currentTimeMillis();
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public SourceLocation getLocation() {
        return location;
    }
    
    public ExecutionContext getContext() {
        return context;
    }
    
    public Breakpoint getHitBreakpoint() {
        return hitBreakpoint;
    }
    
    public boolean wasBreakpointHit() {
        return hitBreakpoint != null;
    }
    
    public StepMode getStepMode() {
        return stepMode;
    }
    
    public long getPauseTimestamp() {
        return pauseTimestamp;
    }
    
    /**
     * Gets all variables in the current scope.
     */
    public Map<String, Object> getLocalVariables() {
        Map<String, Object> variables = new HashMap<>();
        Scope scope = context.currentScope();
        
        // Collect variables from current scope
        for (String name : scope.getVariableNames()) {
            variables.put(name, scope.get(name));
        }
        
        return variables;
    }
    
    /**
     * Gets all variables including parent scopes.
     */
    public Map<String, Object> getAllVariables() {
        Map<String, Object> variables = new HashMap<>();
        Scope scope = context.currentScope();
        
        // Collect variables from all scopes (current scope overrides parent)
        collectVariables(scope, variables);
        
        return variables;
    }
    
    private void collectVariables(Scope scope, Map<String, Object> variables) {
        if (scope == null) return;
        
        // First collect from parent (so child can override)
        if (scope.getParent() != null) {
            collectVariables(scope.getParent(), variables);
        }
        
        // Then collect from current scope
        for (String name : scope.getVariableNames()) {
            variables.put(name, scope.get(name));
        }
    }
    
    /**
     * Gets the value of a specific variable.
     */
    public Object getVariable(String name) {
        if (context.hasVariable(name)) {
            return context.getVariable(name);
        }
        return null;
    }
    
    /**
     * Gets the call stack as a string.
     */
    public String getCallStackString() {
        return context.getCallStack().toString();
    }
    
    @Override
    public String toString() {
        return String.format("DebugState{script='%s', location=%s, breakpoint=%s, stepMode=%s}",
            scriptId, location, hitBreakpoint, stepMode);
    }
    
    /**
     * Step modes for debugging.
     */
    public enum StepMode {
        /** Continue execution until next breakpoint */
        CONTINUE,
        /** Execute one statement and pause */
        STEP_OVER,
        /** Step into function calls */
        STEP_INTO,
        /** Step out of current function */
        STEP_OUT
    }
}
