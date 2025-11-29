package ru.mjkey.storykee.parser.ast;

/**
 * Represents a location in source code for error reporting and debugging.
 */
public record SourceLocation(String fileName, int line, int column) {
    
    public static SourceLocation unknown() {
        return new SourceLocation("<unknown>", 0, 0);
    }
    
    @Override
    public String toString() {
        return String.format("%s:%d:%d", fileName, line, column);
    }
}
