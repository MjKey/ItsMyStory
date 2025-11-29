package ru.mjkey.storykee.runtime.debug;

/**
 * Represents a watch expression that is evaluated during debugging.
 */
public class WatchExpression {
    
    private final String id;
    private final String expression;
    private Object lastValue;
    private String lastError;
    private boolean enabled;
    
    public WatchExpression(String id, String expression) {
        this.id = id;
        this.expression = expression;
        this.enabled = true;
    }
    
    public String getId() {
        return id;
    }
    
    public String getExpression() {
        return expression;
    }
    
    public Object getLastValue() {
        return lastValue;
    }
    
    public void setLastValue(Object value) {
        this.lastValue = value;
        this.lastError = null;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String error) {
        this.lastError = error;
        this.lastValue = null;
    }
    
    public boolean hasError() {
        return lastError != null;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(expression).append(" = ");
        if (hasError()) {
            sb.append("<error: ").append(lastError).append(">");
        } else if (lastValue == null) {
            sb.append("null");
        } else {
            sb.append(lastValue);
        }
        return sb.toString();
    }
}
