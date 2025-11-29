package ru.mjkey.storykee.parser;

import org.antlr.v4.runtime.*;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;
import ru.mjkey.storykee.parser.generated.StorykeeLexer;
import ru.mjkey.storykee.parser.generated.StorykeeParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade for parsing Storykee scripts.
 * Provides a simple interface for parsing source code into AST.
 */
public class StorykeeParserFacade {
    
    /**
     * Parses Storykee source code into an AST.
     * 
     * @param source The source code to parse
     * @param fileName The name of the source file (for error reporting)
     * @return The parsed program AST
     * @throws ParseException If parsing fails
     */
    public ProgramNode parse(String source, String fileName) throws ParseException {
        List<ParseException.ParseError> errors = new ArrayList<>();
        
        // Create lexer
        CharStream input = CharStreams.fromString(source);
        StorykeeLexer lexer = new StorykeeLexer(input);
        
        // Add error listener for lexer
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add(new ParseException.ParseError(
                    new SourceLocation(fileName, line, charPositionInLine + 1),
                    msg
                ));
            }
        });
        
        // Create parser
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        StorykeeParser parser = new StorykeeParser(tokens);
        
        // Add error listener for parser
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add(new ParseException.ParseError(
                    new SourceLocation(fileName, line, charPositionInLine + 1),
                    msg
                ));
            }
        });
        
        // Parse
        StorykeeParser.ProgramContext tree = parser.program();
        
        // Check for syntax errors
        if (!errors.isEmpty()) {
            throw new ParseException("Syntax errors found", errors);
        }
        
        // Build AST
        StorykeeASTBuilder builder = new StorykeeASTBuilder(fileName);
        ProgramNode program = builder.visitProgram(tree);
        
        // Check for semantic errors during AST building
        if (builder.hasErrors()) {
            throw new ParseException("Semantic errors found", builder.getErrors());
        }
        
        return program;
    }
    
    /**
     * Parses Storykee source code into an AST.
     * 
     * @param source The source code to parse
     * @return The parsed program AST
     * @throws ParseException If parsing fails
     */
    public ProgramNode parse(String source) throws ParseException {
        return parse(source, "<input>");
    }
}
