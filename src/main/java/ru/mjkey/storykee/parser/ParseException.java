package ru.mjkey.storykee.parser;

import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when parsing fails.
 * Contains detailed error information including location and message.
 */
public class ParseException extends Exception {
    
    private final List<ParseError> errors;
    
    public ParseException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }
    
    public ParseException(String message, List<ParseError> errors) {
        super(message);
        this.errors = new ArrayList<>(errors);
    }
    
    public ParseException(ParseError error) {
        super(error.message());
        this.errors = new ArrayList<>();
        this.errors.add(error);
    }
    
    public List<ParseError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    public void addError(ParseError error) {
        errors.add(error);
    }
    
    @Override
    public String getMessage() {
        if (errors.isEmpty()) {
            return super.getMessage();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Parse errors:\n");
        for (ParseError error : errors) {
            sb.append("  ").append(error.location()).append(": ").append(error.message()).append("\n");
        }
        return sb.toString();
    }
    
    public record ParseError(SourceLocation location, String message) {}
}
