package ru.mjkey.storykee.runtime.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.runtime.monitoring.PerformanceMonitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes Storykee scripts.
 * Manages script lifecycle including loading, validation, execution, and cleanup.
 */
public class ScriptExecutor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptExecutor.class);
    private static ScriptExecutor instance;
    
    // Track running scripts
    private final Map<String, ScriptState> runningScripts = new ConcurrentHashMap<>();
    
    // Interpreter for executing AST
    private final ASTInterpreter interpreter;
    
    // Performance monitor
    private final PerformanceMonitor performanceMonitor;
    
    private ScriptExecutor() {
        this.interpreter = new ASTInterpreter();
        this.performanceMonitor = PerformanceMonitor.getInstance();
    }
    
    public static ScriptExecutor getInstance() {
        if (instance == null) {
            instance = new ScriptExecutor();
        }
        return instance;
    }
    
    /**
     * Executes a parsed program AST.
     * 
     * @param ast The program AST to execute
     * @param context The execution context
     * @return The execution result
     */
    public ExecutionResult execute(ProgramNode ast, ExecutionContext context) {
        String scriptId = context.getScriptId();
        long startTime = System.currentTimeMillis();
        
        // Check if script is throttled
        if (performanceMonitor.isThrottled(scriptId)) {
            LOGGER.warn("Script {} is throttled, skipping execution", scriptId);
            return ExecutionResult.failure(
                new RuntimeException("Script is throttled due to performance issues"),
                0
            );
        }
        
        // Start performance tracking
        PerformanceMonitor.ExecutionTracker tracker = performanceMonitor.startExecution(scriptId);
        
        try {
            // Validate AST before execution
            if (!validateAST(ast)) {
                String errorMsg = "AST validation failed for script: " + scriptId;
                LOGGER.error(errorMsg);
                tracker.complete(false);
                return ExecutionResult.failure(
                    new IllegalArgumentException(errorMsg),
                    System.currentTimeMillis() - startTime
                );
            }
            
            // Mark script as running
            ScriptState state = new ScriptState(scriptId);
            runningScripts.put(scriptId, state);
            
            try {
                // Execute the AST using the interpreter
                Object result = interpreter.visitProgram(ast, context);
                
                long executionTime = System.currentTimeMillis() - startTime;
                LOGGER.debug("Script {} executed successfully in {}ms", scriptId, executionTime);
                
                // Record successful execution
                tracker.complete(true);
                
                return ExecutionResult.success(result, executionTime);
                
            } catch (Exception e) {
                // Log runtime error with context
                long executionTime = System.currentTimeMillis() - startTime;
                LOGGER.error("Runtime error in script {}: {}\nCall stack:\n{}", 
                    scriptId, e.getMessage(), context.getCallStack(), e);
                
                // Record failed execution
                tracker.complete(false);
                
                return ExecutionResult.failure(e, executionTime);
                
            } finally {
                // Cleanup: remove from running scripts
                runningScripts.remove(scriptId);
            }
            
        } catch (Exception e) {
            // Catch any unexpected errors
            long executionTime = System.currentTimeMillis() - startTime;
            LOGGER.error("Unexpected error executing script {}: {}", scriptId, e.getMessage(), e);
            tracker.complete(false);
            return ExecutionResult.failure(e, executionTime);
        }
    }
    
    /**
     * Validates the AST before execution.
     * 
     * @param ast The AST to validate
     * @return true if valid, false otherwise
     */
    private boolean validateAST(ProgramNode ast) {
        if (ast == null) {
            LOGGER.error("AST is null");
            return false;
        }
        
        if (ast.getStatements() == null) {
            LOGGER.error("AST statements list is null");
            return false;
        }
        
        // AST is valid
        return true;
    }
    
    /**
     * Stops a running script.
     * 
     * @param scriptId The ID of the script to stop
     */
    public void stop(String scriptId) {
        ScriptState state = runningScripts.get(scriptId);
        if (state != null) {
            state.stop();
            LOGGER.info("Script {} stopped", scriptId);
        } else {
            LOGGER.warn("Attempted to stop script {} but it is not running", scriptId);
        }
    }
    
    /**
     * Checks if a script is currently running.
     * 
     * @param scriptId The ID of the script to check
     * @return true if running, false otherwise
     */
    public boolean isRunning(String scriptId) {
        return runningScripts.containsKey(scriptId);
    }
    
    /**
     * Gets the interpreter instance.
     * 
     * @return The AST interpreter
     */
    public ASTInterpreter getInterpreter() {
        return interpreter;
    }
    
    /**
     * Gets the performance monitor instance.
     * 
     * @return The performance monitor
     */
    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
    
    /**
     * Represents the state of a running script.
     */
    private static class ScriptState {
        private final String scriptId;
        private final AtomicBoolean stopped;
        
        public ScriptState(String scriptId) {
            this.scriptId = scriptId;
            this.stopped = new AtomicBoolean(false);
        }
        
        public void stop() {
            stopped.set(true);
        }
        
        public boolean isStopped() {
            return stopped.get();
        }
        
        public String getScriptId() {
            return scriptId;
        }
    }
}
