package ru.mjkey.storykee.runtime.backup;

/**
 * Result of a corruption check operation.
 */
public class CorruptionCheckResult {
    
    private final boolean valid;
    private final String reason;
    
    private CorruptionCheckResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }
    
    public static CorruptionCheckResult valid() {
        return new CorruptionCheckResult(true, null);
    }
    
    public static CorruptionCheckResult corrupted(String reason) {
        return new CorruptionCheckResult(false, reason);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public boolean isCorrupted() {
        return !valid;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        if (valid) {
            return "CorruptionCheckResult{valid=true}";
        } else {
            return "CorruptionCheckResult{valid=false, reason=" + reason + "}";
        }
    }
}
