package ru.mjkey.storykee.runtime.context;

import java.util.Stack;
import java.util.UUID;

/**
 * Manages the execution state of a Storykee script.
 * Contains variable scopes, call stack, and player context.
 */
public class ExecutionContext {
    
    private final String scriptId;
    private final Scope globalScope;
    private final Stack<Scope> scopeStack;
    private final CallStack callStack;
    private final UUID playerId; // null for global scripts
    
    public ExecutionContext(String scriptId) {
        this(scriptId, null);
    }
    
    public ExecutionContext(String scriptId, UUID playerId) {
        this.scriptId = scriptId;
        this.playerId = playerId;
        this.globalScope = new Scope(null);
        this.scopeStack = new Stack<>();
        this.scopeStack.push(globalScope);
        this.callStack = new CallStack();
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public boolean hasPlayer() {
        return playerId != null;
    }
    
    public void defineVariable(String name, Object value) {
        currentScope().define(name, value);
    }
    
    public Object getVariable(String name) {
        return currentScope().get(name);
    }
    
    public void setVariable(String name, Object value) {
        currentScope().set(name, value);
    }
    
    public boolean hasVariable(String name) {
        return currentScope().has(name);
    }
    
    public void pushScope() {
        scopeStack.push(new Scope(currentScope()));
    }
    
    public void popScope() {
        if (scopeStack.size() > 1) {
            scopeStack.pop();
        }
    }
    
    public Scope currentScope() {
        return scopeStack.peek();
    }
    
    public Scope getGlobalScope() {
        return globalScope;
    }
    
    public CallStack getCallStack() {
        return callStack;
    }
}
