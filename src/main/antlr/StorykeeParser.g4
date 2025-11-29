/**
 * Storykee Parser Grammar
 * Defines the grammar rules for the Storykee scripting language
 */
parser grammar StorykeeParser;

options { tokenVocab=StorykeeLexer; }

// Entry point
program: statement* EOF;

// Statements
statement
    : variableDeclaration
    | npcDeclaration
    | dialogueDeclaration
    | questDeclaration
    | functionDeclaration
    | eventHandler
    | ifStatement
    | forStatement
    | whileStatement
    | returnStatement
    | propertyStatement
    | nodeDeclaration
    | expressionStatement
    | block
    | javaSection
    ;

// Property statement: name: value; (used inside npc, dialogue, quest blocks)
propertyStatement: IDENTIFIER COLON expression SEMICOLON;

// Node declaration: node name { ... } (used inside dialogue blocks)
nodeDeclaration: NODE IDENTIFIER block;

// Variable declaration: var name = value;
variableDeclaration: VAR IDENTIFIER ASSIGN expression SEMICOLON;

// NPC declaration: npc name { properties }
npcDeclaration: NPC IDENTIFIER block;

// Dialogue declaration: dialogue name { lines and choices }
dialogueDeclaration: DIALOGUE IDENTIFIER block;

// Quest declaration: quest name { objectives and rewards }
questDeclaration: QUEST IDENTIFIER block;

// Function declaration: function name(params) { body }
functionDeclaration: FUNCTION IDENTIFIER LPAREN parameterList? RPAREN block;

parameterList: IDENTIFIER (COMMA IDENTIFIER)*;

// Event handler: on eventType { actions }
eventHandler: ON IDENTIFIER block;

// If statement: if condition { } else { }
ifStatement: IF LPAREN expression RPAREN block (ELSE block)?;

// For loop: for item in collection { } OR for (init; condition; update) { }
forStatement
    : FOR loopVariable IN expression block                                    # forInStatement
    | FOR LPAREN forInit? SEMICOLON expression? SEMICOLON forUpdate? RPAREN block  # forCStyleStatement
    ;

// Loop variable can be identifier or keyword used as variable name
loopVariable: IDENTIFIER | NPC | QUEST | DIALOGUE | NODE;

// C-style for loop parts
forInit: VAR IDENTIFIER ASSIGN expression | IDENTIFIER ASSIGN expression;
forUpdate: IDENTIFIER ASSIGN expression;

// While loop: while condition { }
whileStatement: WHILE LPAREN expression RPAREN block;

// Return statement: return value;
returnStatement: RETURN expression? SEMICOLON;

// Expression statement: expression;
expressionStatement: expression SEMICOLON;

// Block: { statements }
block: LBRACE statement* RBRACE;

// Java section: #java-section-start ... #java-section-end
javaSection: JAVA_SECTION_START JAVA_CODE* JAVA_SECTION_END;

// Expressions
expression
    : primary                                           # primaryExpr
    | expression DOT memberName                         # memberAccess
    | expression LBRACKET expression RBRACKET           # arrayAccess
    | expression LPAREN argumentList? RPAREN            # functionCall
    | NOT expression                                    # unaryNot
    | MINUS expression                                  # unaryMinus
    | expression (STAR | SLASH) expression              # mulDiv
    | expression (PLUS | MINUS) expression              # addSub
    | expression (LT | GT | LE | GE) expression         # comparison
    | expression (EQ | NEQ) expression                  # equality
    | expression AND expression                         # logicalAnd
    | expression OR expression                          # logicalOr
    | IDENTIFIER ASSIGN expression                      # assignment
    ;

// Member names can be identifiers or keywords (for event.npc, event.quest, etc.)
memberName: IDENTIFIER | NPC | QUEST | DIALOGUE | NODE;

primary
    : LPAREN expression RPAREN                          # parenExpr
    | identifierOrKeyword                               # identifier
    | STRING                                            # stringLiteral
    | NUMBER                                            # numberLiteral
    | TRUE                                              # trueLiteral
    | FALSE                                             # falseLiteral
    | NULL                                              # nullLiteral
    | arrayLiteral                                      # arrayLiteralExpr
    | objectLiteral                                     # objectLiteralExpr
    ;

// Identifiers can also be keywords when used as variable names
identifierOrKeyword: IDENTIFIER | NPC | QUEST | DIALOGUE | NODE;

argumentList: expression (COMMA expression)*;

arrayLiteral: LBRACKET (expression (COMMA expression)*)? RBRACKET;

// Object literals support both comma and semicolon separators
objectLiteral: LBRACE (objectProperty ((COMMA | SEMICOLON) objectProperty)* SEMICOLON?)? RBRACE;

objectProperty: propertyName COLON expression;

// Property names can be identifiers, strings, or keywords
propertyName: IDENTIFIER | STRING | NPC | QUEST | DIALOGUE | NODE;
