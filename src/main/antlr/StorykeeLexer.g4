/**
 * Storykee Lexer Grammar
 * Defines all tokens for the Storykee scripting language
 */
lexer grammar StorykeeLexer;

// Keywords
VAR: 'var';
NPC: 'npc';
DIALOGUE: 'dialogue';
QUEST: 'quest';
NODE: 'node';
ON: 'on';
IF: 'if';
ELSE: 'else';
FOR: 'for';
WHILE: 'while';
FUNCTION: 'function';
RETURN: 'return';
IN: 'in';
TRUE: 'true';
FALSE: 'false';
NULL: 'null';

// Java Section Markers
JAVA_SECTION_START: '#java-section-start' -> pushMode(JAVA_MODE);

// Operators
ASSIGN: '=';
PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';
EQ: '==';
NEQ: '!=';
LT: '<';
GT: '>';
LE: '<=';
GE: '>=';
AND: '&&';
OR: '||';
NOT: '!';

// Delimiters
LBRACE: '{';
RBRACE: '}';
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
COMMA: ',';
SEMICOLON: ';';
COLON: ':';
DOT: '.';

// Literals
STRING: '"' (~["\r\n\\] | '\\' .)* '"';
NUMBER: [0-9]+ ('.' [0-9]+)?;

// Identifiers
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;

// Comments
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;

// Whitespace
WS: [ \t\r\n]+ -> skip;

// Java code mode
mode JAVA_MODE;
JAVA_SECTION_END: '#java-section-end' -> popMode;
JAVA_CODE: (~[#] | '#' ~'j')+ ;
