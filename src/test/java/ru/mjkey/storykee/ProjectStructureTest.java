package ru.mjkey.storykee;

import org.junit.jupiter.api.Test;
import ru.mjkey.storykee.parser.ast.*;
import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;
import ru.mjkey.storykee.runtime.context.*;
import ru.mjkey.storykee.runtime.executor.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify the project structure is correctly set up.
 */
class ProjectStructureTest {
    
    @Test
    void testASTNodeCreation() {
        // Test that AST nodes can be created
        ProgramNode program = new ProgramNode();
        assertNotNull(program);
        assertTrue(program.getStatements().isEmpty());
    }
    
    @Test
    void testVariableDeclarationNode() {
        LiteralNode value = new LiteralNode("test", LiteralNode.LiteralType.STRING);
        VariableDeclarationNode varDecl = new VariableDeclarationNode("myVar", value);
        
        assertEquals("myVar", varDecl.getName());
        assertEquals(value, varDecl.getValue());
    }
    
    @Test
    void testExecutionContext() {
        ExecutionContext context = new ExecutionContext("test-script");
        
        assertEquals("test-script", context.getScriptId());
        assertFalse(context.hasPlayer());
        
        context.defineVariable("x", 42);
        assertEquals(42, context.getVariable("x"));
    }
    
    @Test
    void testScopeNesting() {
        ExecutionContext context = new ExecutionContext("test-script");
        
        context.defineVariable("outer", "outerValue");
        context.pushScope();
        context.defineVariable("inner", "innerValue");
        
        assertEquals("outerValue", context.getVariable("outer"));
        assertEquals("innerValue", context.getVariable("inner"));
        
        context.popScope();
        assertEquals("outerValue", context.getVariable("outer"));
        assertThrows(UndefinedVariableException.class, () -> context.getVariable("inner"));
    }
    
    @Test
    void testSourceLocation() {
        SourceLocation loc = new SourceLocation("test.skee", 10, 5);
        
        assertEquals("test.skee", loc.fileName());
        assertEquals(10, loc.line());
        assertEquals(5, loc.column());
        assertEquals("test.skee:10:5", loc.toString());
    }
}
