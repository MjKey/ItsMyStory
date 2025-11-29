package ru.mjkey.storykee.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import ru.mjkey.storykee.parser.StorykeeParserFacade;
import ru.mjkey.storykee.parser.ParseException;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;
import ru.mjkey.storykee.parser.prettyprint.StorykeePrettyPrinter;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.runtime.executor.BuiltinFunctions;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for complete story lifecycle.
 * Tests the full flow from script parsing to execution.
 */
@DisplayName("Story Lifecycle Integration Tests")
public class StoryLifecycleTest {

    private StorykeeParserFacade parser;
    private StorykeePrettyPrinter prettyPrinter;
    private BuiltinFunctions builtinFunctions;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new StorykeeParserFacade();
        prettyPrinter = new StorykeePrettyPrinter();
        builtinFunctions = new BuiltinFunctions();
    }

    @Test
    @DisplayName("Parse simple variable declaration")
    void testSimpleVariableDeclaration() throws ParseException {
        String script = "var message = \"Hello\"; var count = 42;";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
        assertEquals(2, ast.getStatements().size());
    }

    @Test
    @DisplayName("Parse NPC declaration")
    void testNPCDeclaration() throws ParseException {
        // NPC block contains expression statements
        String script = "npc merchant { }";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
        assertEquals(1, ast.getStatements().size());
    }

    @Test
    @DisplayName("Parse dialogue declaration")
    void testDialogueDeclaration() throws ParseException {
        // Dialogue block contains statements
        String script = "dialogue greeting { }";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Parse quest declaration")
    void testQuestDeclaration() throws ParseException {
        // Quest block contains statements
        String script = "quest test { }";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Parse event handler")
    void testEventHandler() throws ParseException {
        String script = "on playerJoin { var p = event.player; }";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
        assertEquals(1, ast.getStatements().size());
    }

    @Test
    @DisplayName("Parse function declaration")
    void testFunctionDeclaration() throws ParseException {
        String script = "function greet(name) { return \"Hello \" + name; }";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Parse control flow")
    void testControlFlow() throws ParseException {
        String script = "var x = 10; if (x > 5) { print(x); } else { print(0); }";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Parse and pretty print round-trip")
    void testRoundTrip() throws ParseException {
        String script = "var name = \"Test\"; var count = 42;";
        ProgramNode ast1 = parser.parse(script);
        String printed = prettyPrinter.print(ast1);
        assertNotNull(printed);
        assertFalse(printed.isEmpty());
        ProgramNode ast2 = parser.parse(printed);
        assertEquals(ast1.getStatements().size(), ast2.getStatements().size());
    }

    @Test
    @DisplayName("Handle syntax errors")
    void testSyntaxError() {
        String invalid = "var x = 10 var y = 20;"; // missing semicolon
        assertThrows(ParseException.class, () -> parser.parse(invalid));
    }

    @Test
    @DisplayName("Parse expressions")
    void testExpressions() throws ParseException {
        String script = "var a = 10 + 5; var b = a * 2; var c = a > b && true;";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
        assertEquals(3, ast.getStatements().size());
    }

    @Test
    @DisplayName("Parse arrays and objects")
    void testArraysAndObjects() throws ParseException {
        String script = "var arr = [1, 2, 3]; var obj = { name: \"test\", value: 42 };";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
        assertEquals(2, ast.getStatements().size());
    }

    @Test
    @DisplayName("Verify builtin functions exist")
    void testBuiltinFunctions() {
        assertTrue(builtinFunctions.hasFunction("print"));
        assertTrue(builtinFunctions.hasFunction("log"));
        assertTrue(builtinFunctions.hasFunction("random"));
        assertTrue(builtinFunctions.hasFunction("spawnNPC"));
        assertTrue(builtinFunctions.hasFunction("despawnNPC"));
        assertTrue(builtinFunctions.hasFunction("showDialogue"));
        assertTrue(builtinFunctions.hasFunction("startQuest"));
        assertTrue(builtinFunctions.hasFunction("completeQuest"));
    }

    @Test
    @DisplayName("Write and read script file")
    void testScriptFileIO() throws Exception {
        String script = "var storyName = \"File Test\";";
        Path scriptFile = tempDir.resolve("test.skee");
        Files.writeString(scriptFile, script);
        String readScript = Files.readString(scriptFile);
        ProgramNode ast = parser.parse(readScript);
        assertNotNull(ast);
    }

    @Test
    @DisplayName("Create execution context")
    void testExecutionContext() {
        String scriptId = "test-script";
        UUID playerId = UUID.randomUUID();
        ExecutionContext context = new ExecutionContext(scriptId, playerId);
        assertEquals(scriptId, context.getScriptId());
        assertEquals(playerId, context.getPlayerId());
        context.defineVariable("testVar", 42);
        assertEquals(42, context.getVariable("testVar"));
    }

    @Test
    @DisplayName("Test scope management")
    void testScopeManagement() {
        ExecutionContext context = new ExecutionContext("test", null);
        context.defineVariable("global", "globalValue");
        context.pushScope();
        context.defineVariable("local", "localValue");
        assertEquals("globalValue", context.getVariable("global"));
        assertEquals("localValue", context.getVariable("local"));
        context.popScope();
        assertEquals("globalValue", context.getVariable("global"));
    }

    @Test
    @DisplayName("Parse empty script")
    void testEmptyScript() throws ParseException {
        ProgramNode ast = parser.parse("");
        assertNotNull(ast);
        assertTrue(ast.getStatements().isEmpty());
    }

    @Test
    @DisplayName("Parse comments only")
    void testCommentsOnly() throws ParseException {
        String script = "// comment\n/* block */";
        ProgramNode ast = parser.parse(script);
        assertNotNull(ast);
        assertTrue(ast.getStatements().isEmpty());
    }
}
