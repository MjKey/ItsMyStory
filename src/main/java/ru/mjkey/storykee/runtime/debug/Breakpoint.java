package ru.mjkey.storykee.runtime.debug;

import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.Objects;

/**
 * Represents a breakpoint in a Storykee script.
 */
public class Breakpoint {
    
    private final String scriptId;
    private final int line;
    private final String condition; // Optional condition expression
    private boolean enabled;
    private int hitCount;
    
    public Breakpoint(String scriptId, int line) {
        this(scriptId, line, null);
    }
    
    public Breakpoint(String scriptId, int line, String condition) {
        this.scriptId = scriptId;
        this.line = line;
        this.condition = condition;
        this.enabled = true;
        this.hitCount = 0;
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public int getLine() {
        return line;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public boolean hasCondition() {
        return condition != null && !condition.isEmpty();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getHitCount() {
        return hitCount;
    }
    
    public void incrementHitCount() {
        hitCount++;
    }
    
    public void resetHitCount() {
        hitCount = 0;
    }
    
    /**
     * Checks if this breakpoint matches the given location.
     */
    public boolean matches(String scriptId, SourceLocation location) {
        if (!this.scriptId.equals(scriptId)) {
            return false;
        }
        return location != null && location.line() == this.line;
    }
    
    /**
     * Checks if this breakpoint matches the given script and line.
     */
    public boolean matches(String scriptId, int line) {
        return this.scriptId.equals(scriptId) && this.line == line;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Breakpoint that = (Breakpoint) o;
        return line == that.line && Objects.equals(scriptId, that.scriptId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(scriptId, line);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(scriptId).append(":").append(line);
        if (hasCondition()) {
            sb.append(" [").append(condition).append("]");
        }
        if (!enabled) {
            sb.append(" (disabled)");
        }
        return sb.toString();
    }
}
