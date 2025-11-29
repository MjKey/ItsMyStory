package ru.mjkey.storykee.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.mjkey.storykee.parser.StorykeeParserFacade;
import ru.mjkey.storykee.parser.ParseException;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.runtime.context.UndefinedVariableException;
import ru.mjkey.storykee.runtime.executor.BuiltinFunctions;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling and recovery in Storykee.
 */
@DisplayName("Error Recovery Tests")
public class ErrorRecoveryTest {

    private StorykeeParserFacade parser;
    private BuiltinFunctions builtinFunctions;

    @BeforeEach
    void setUp() {
        parser = new StorykeeParserFacade();
        builtinFunctions = new BuiltinFunctions();
    }

    // Parser Error Tests

    @Test
    @DisplayName("Report missing semicolon")
    void testMissingSemicolon() {
        assertThrows(ParseException.class, () -> parser.parse("var x = 10 var y = 20;"));
    }

    @Test
    @DisplayName("Report unclosed brace")
    void testUnclosedBrace() {
        assertThrows(ParseException.class, () -> parser.parse("npc test { name: \"Test\";"));
    }

    @Test
    @DisplayName("Report unclosed string")
    void testUnclosedString() {
        assertThrows(ParseException.class, () -> parser.parse("var message = \"Hello;"));
    }

    @Test
    @DisplayName("Report unexpected token")
    void testUnexpectedToken() {
        assertThrows(ParseException.class, () -> parser.parse("var x = = 10;"));
    }

    // Runtime Error Tests

    @Test
    @DisplayName("Handle undefined variable access")
    void testUndefinedVariableAccess() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertThrows(UndefinedVariableException.class, () -> context.getVariable("nonexistent"));
    }

    @Test
    @DisplayName("Handle undefined variable assignment")
    void testUndefinedVariableAssignment() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertThrows(UndefinedVariableException.class, () -> context.setVariable("nonexistent", "value"));
    }

    @Test
    @DisplayName("Handle unknown function call")
    void testUnknownFunctionCall() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertThrows(RuntimeException.class, 
            () -> builtinFunctions.call("nonexistentFunction", Collections.emptyList(), context));
    }

    @Test
    @DisplayName("Handle wrong argument count")
    void testWrongArgumentCount() {
        ExecutionContext context = new ExecutionContext("test", null);
        // log() requires 2 arguments: level and message
        // Calling with 0 arguments should throw RuntimeException (wrapping IllegalArgumentException)
        assertThrows(RuntimeException.class,
            () -> builtinFunctions.call("log", Collections.emptyList(), context));
    }

    // Graceful Degradation Tests

    @Test
    @DisplayName("Parse empty script")
    void testEmptyScript() throws ParseException {
        ProgramNode ast = parser.parse("");
        assertNotNull(ast);
        assertTrue(ast.getStatements().isEmpty());
    }

    @Test
    @DisplayName("Parse whitespace script")
    void testWhitespaceScript() throws ParseException {
        ProgramNode ast = parser.parse("   \n\n   \t   ");
        assertNotNull(ast);
        assertTrue(ast.getStatements().isEmpty());
    }

    @Test
    @DisplayName("Handle null player ID")
    void testNullPlayerId() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertNull(context.getPlayerId());
        assertEquals("test", context.getScriptId());
        context.defineVariable("test", "value");
        assertEquals("value", context.getVariable("test"));
    }

    @Test
    @DisplayName("Handle scope underflow gracefully")
    void testScopeUnderflow() {
        ExecutionContext context = new ExecutionContext("test", null);
        // Popping without extra push should be handled gracefully
        assertDoesNotThrow(() -> context.popScope());
    }

    @Test
    @DisplayName("Handle call stack operations")
    void testCallStackOperations() {
        ExecutionContext context = new ExecutionContext("test", null);
        for (int i = 0; i < 10; i++) {
            context.getCallStack().push("function" + i, null);
        }
        assertEquals(10, context.getCallStack().depth());
        for (int i = 0; i < 10; i++) {
            context.getCallStack().pop();
        }
        assertEquals(0, context.getCallStack().depth());
    }

    // Builtin Function Error Handling

    @Test
    @DisplayName("Print handles null")
    void testPrintNull() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertDoesNotThrow(() -> builtinFunctions.call("print", Collections.singletonList(null), context));
    }

    @Test
    @DisplayName("Print handles empty args")
    void testPrintEmpty() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertDoesNotThrow(() -> builtinFunctions.call("print", Collections.emptyList(), context));
    }

    @Test
    @DisplayName("Log handles invalid level")
    void testLogInvalidLevel() {
        ExecutionContext context = new ExecutionContext("test", null);
        assertDoesNotThrow(() -> builtinFunctions.call("log", Arrays.asList("INVALID", "message"), context));
    }

    @Test
    @DisplayName("Random returns value")
    void testRandomReturnsValue() {
        ExecutionContext context = new ExecutionContext("test", null);
        Object result = builtinFunctions.call("random", Arrays.asList(0, 100), context);
        assertNotNull(result);
        assertTrue(result instanceof Double);
    }

    @Test
    @DisplayName("Parse unicode strings")
    void testUnicodeStrings() throws ParseException {
        ProgramNode ast = parser.parse("var russian = \"Привет\"; var emoji = \"🎮\";");
        assertNotNull(ast);
        assertEquals(2, ast.getStatements().size());
    }

    @Test
    @DisplayName("Parse long identifiers")
    void testLongIdentifiers() throws ParseException {
        String longName = "a".repeat(100);
        ProgramNode ast = parser.parse("var " + longName + " = 10;");
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Parse nested expressions")
    void testNestedExpressions() throws ParseException {
        String nested = "var x = ((((10))));";
        ProgramNode ast = parser.parse(nested);
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Parse empty blocks")
    void testEmptyBlocks() throws ParseException {
        String script = "function empty() {} if (true) {} for x in [] {}";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
    }
}
