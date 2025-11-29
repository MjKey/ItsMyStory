package ru.mjkey.storykee.runtime.context;

/**
 * Exception thrown when accessing an undefined variable.
 */
public class UndefinedVariableException extends RuntimeException {
    
    public UndefinedVariableException(String message) {
        super(message);
    }
    
    public UndefinedVariableException(String message, Throwable cause) {
        super(message, cause);
    }
}
