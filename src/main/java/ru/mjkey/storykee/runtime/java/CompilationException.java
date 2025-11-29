package ru.mjkey.storykee.runtime.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when Java code compilation fails.
 */
public class CompilationException extends Exception {
    
    private final List<CompilationError> errors;
    
    public CompilationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }
    
    public CompilationException(String message, List<CompilationError> errors) {
        super(message);
        this.errors = new ArrayList<>(errors);
    }
    
    public List<CompilationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    @Override
    public String getMessage() {
        if (errors.isEmpty()) {
            return super.getMessage();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Compilation errors:\n");
        for (CompilationError error : errors) {
            sb.append("  Line ").append(error.line()).append(": ").append(error.message()).append("\n");
        }
        return sb.toString();
    }
    
    public record CompilationError(int line, String message) {}
}
