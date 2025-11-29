package ru.mjkey.storykee.runtime.java;

import org.junit.jupiter.api.Test;
import ru.mjkey.storykee.runtime.context.ExecutionContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JavaSectionCompiler.
 */
class JavaSectionCompilerTest {
    
    @Test
    void testSimpleJavaCodeCompilation() throws Exception {
        JavaSectionCompiler compiler = JavaSectionCompiler.getInstance();
        ExecutionContext context = new ExecutionContext("test-script");
        
        String javaCode = """
            int x = 5;
            int y = 10;
            int result = x + y;
            api.setVariable("result", result);
            """;
        
        CompiledJavaSection compiled = compiler.compile(javaCode, context);
        assertNotNull(compiled);
        assertNotNull(compiled.getCompiledClass());
    }
    
    @Test
    void testJavaCodeExecution() throws Exception {
        JavaSectionCompiler compiler = JavaSectionCompiler.getInstance();
        ExecutionContext context = new ExecutionContext("test-script");
        
        String javaCode = """
            int x = 5;
            int y = 10;
            int result = x + y;
            api.setVariable("result", result);
            """;
        
        CompiledJavaSection compiled = compiler.compile(javaCode, context);
        compiled.execute(context);
        
        // Verify the variable was set
        assertEquals(15, context.getVariable("result"));
    }
    
    @Test
    void testJavaCodeWithVariableAccess() throws Exception {
        JavaSectionCompiler compiler = JavaSectionCompiler.getInstance();
        ExecutionContext context = new ExecutionContext("test-script");
        
        // Set up initial variables
        context.defineVariable("input", 42);
        
        String javaCode = """
            Object inputValue = api.getVariable("input");
            int doubled = ((Number) inputValue).intValue() * 2;
            api.setVariable("output", doubled);
            """;
        
        CompiledJavaSection compiled = compiler.compile(javaCode, context);
        compiled.execute(context);
        
        // Verify the output
        assertEquals(84, context.getVariable("output"));
    }
    
    @Test
    void testCompilationErrorHandling() {
        JavaSectionCompiler compiler = JavaSectionCompiler.getInstance();
        ExecutionContext context = new ExecutionContext("test-script");
        
        // Invalid Java code (missing semicolon)
        String javaCode = """
            int x = 5
            int y = 10;
            """;
        
        assertThrows(CompilationException.class, () -> {
            compiler.compile(javaCode, context);
        });
    }
    
    @Test
    void testRuntimeErrorHandling() throws Exception {
        JavaSectionCompiler compiler = JavaSectionCompiler.getInstance();
        ExecutionContext context = new ExecutionContext("test-script");
        
        // Code that will throw a runtime exception
        String javaCode = """
            if (true) {
                throw new RuntimeException("Test error");
            }
            """;
        
        CompiledJavaSection compiled = compiler.compile(javaCode, context);
        
        Exception exception = assertThrows(Exception.class, () -> {
            compiled.execute(context);
        });
        
        // Verify the exception message
        assertTrue(exception.getMessage().contains("Test error"));
    }
    
    @Test
    void testMultipleCompilations() throws Exception {
        JavaSectionCompiler compiler = JavaSectionCompiler.getInstance();
        ExecutionContext context1 = new ExecutionContext("test-script-1");
        ExecutionContext context2 = new ExecutionContext("test-script-2");
        
        String javaCode1 = "api.setVariable(\"value\", 100);";
        String javaCode2 = "api.setVariable(\"value\", 200);";
        
        CompiledJavaSection compiled1 = compiler.compile(javaCode1, context1);
        CompiledJavaSection compiled2 = compiler.compile(javaCode2, context2);
        
        compiled1.execute(context1);
        compiled2.execute(context2);
        
        // Verify contexts are isolated
        assertEquals(100, context1.getVariable("value"));
        assertEquals(200, context2.getVariable("value"));
    }
}
