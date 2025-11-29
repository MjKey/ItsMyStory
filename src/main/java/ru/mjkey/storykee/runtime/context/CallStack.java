package ru.mjkey.storykee.runtime.context;

import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Tracks function call stack for debugging and error reporting.
 */
public class CallStack {
    
    private final Stack<CallFrame> frames;
    
    public CallStack() {
        this.frames = new Stack<>();
    }
    
    public void push(String functionName, SourceLocation location) {
        frames.push(new CallFrame(functionName, location));
    }
    
    public void pop() {
        if (!frames.isEmpty()) {
            frames.pop();
        }
    }
    
    public CallFrame current() {
        return frames.isEmpty() ? null : frames.peek();
    }
    
    public int depth() {
        return frames.size();
    }
    
    public int getDepth() {
        return depth();
    }
    
    public List<CallFrame> getFrames() {
        return Collections.unmodifiableList(new ArrayList<>(frames));
    }
    
    public String getStackTrace() {
        StringBuilder sb = new StringBuilder();
        List<CallFrame> frameList = new ArrayList<>(frames);
        Collections.reverse(frameList);
        
        for (CallFrame frame : frameList) {
            sb.append("  at ").append(frame.functionName())
              .append(" (").append(frame.location()).append(")\n");
        }
        return sb.toString();
    }
    
    public record CallFrame(String functionName, SourceLocation location) {}
}
