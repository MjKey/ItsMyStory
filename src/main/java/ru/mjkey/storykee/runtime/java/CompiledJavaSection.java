package ru.mjkey.storykee.runtime.java;

import ru.mjkey.storykee.runtime.context.ExecutionContext;

import java.lang.reflect.Method;

/**
 * Represents a compiled Java code section ready for execution.
 * Handles loading and executing compiled classes with proper exception handling and thread safety.
 */
public class CompiledJavaSection {
    
    private final Class<?> compiledClass;
    private final Method executeMethod;
    
    public CompiledJavaSection(Class<?> compiledClass) throws CompilationException {
        this.compiledClass = compiledClass;
        
        // Find the execute method
        try {
            this.executeMethod = compiledClass.getMethod("execute", ExecutionContext.class);
        } catch (NoSuchMethodException e) {
            throw new CompilationException("Compiled class does not have execute(ExecutionContext) method");
        }
    }
    
    /**
     * Executes the compiled Java section.
     * 
     * @param context The execution context
     * @return The result of execution
     * @throws Exception If execution fails
     */
    public Object execute(ExecutionContext context) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("Execution context cannot be null");
        }
        
        try {
            // Create an instance of the compiled class
            Object instance = compiledClass.getDeclaredConstructor().newInstance();
            
            // Invoke the execute method
            return executeMethod.invoke(instance, context);
            
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Unwrap the actual exception thrown by the Java section
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            } else {
                throw new RuntimeException("Java section execution failed", cause);
            }
        } catch (Exception e) {
            // Handle reflection errors
            throw new RuntimeException("Failed to execute Java section: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes the Java section with exception handling and logging.
     * This method catches all exceptions and wraps them with context information.
     * 
     * @param context The execution context
     * @return The result of execution, or null if an error occurred
     */
    public Object executeSafely(ExecutionContext context) {
        try {
            return execute(context);
        } catch (Exception e) {
            System.err.println("[Storykee] Java section execution error in script " + context.getScriptId());
            System.err.println("[Storykee] Error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public Class<?> getCompiledClass() {
        return compiledClass;
    }
    
    public Method getExecuteMethod() {
        return executeMethod;
    }
}
