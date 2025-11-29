package ru.mjkey.storykee.runtime.context;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a variable scope in the execution context.
 * Supports nested scopes with parent lookup.
 */
public class Scope {
    
    private final Scope parent;
    private final Map<String, Object> variables;
    
    public Scope(Scope parent) {
        this.parent = parent;
        this.variables = new HashMap<>();
    }
    
    public void define(String name, Object value) {
        variables.put(name, value);
    }
    
    public Object get(String name) {
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.get(name);
        }
        throw new UndefinedVariableException("Undefined variable: " + name);
    }
    
    public void set(String name, Object value) {
        if (variables.containsKey(name)) {
            variables.put(name, value);
            return;
        }
        if (parent != null && parent.has(name)) {
            parent.set(name, value);
            return;
        }
        throw new UndefinedVariableException("Cannot assign to undefined variable: " + name);
    }
    
    public boolean has(String name) {
        if (variables.containsKey(name)) {
            return true;
        }
        return parent != null && parent.has(name);
    }
    
    public Scope getParent() {
        return parent;
    }
    
    public Map<String, Object> getLocalVariables() {
        return new HashMap<>(variables);
    }
    
    /**
     * Gets the names of all variables defined in this scope (not including parent scopes).
     */
    public java.util.Set<String> getVariableNames() {
        return new java.util.HashSet<>(variables.keySet());
    }
}
