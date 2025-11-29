package ru.mjkey.storykee.parser;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;
import ru.mjkey.storykee.parser.generated.StorykeeParser;
import ru.mjkey.storykee.parser.generated.StorykeeParserBaseVisitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visitor that builds a typed AST from the ANTLR parse tree.
 * Converts ANTLR's generic parse tree into our domain-specific AST nodes.
 */
public class StorykeeASTBuilder extends StorykeeParserBaseVisitor<Object> {
    
    private final String fileName;
    private final List<ParseException.ParseError> errors;
    
    public StorykeeASTBuilder(String fileName) {
        this.fileName = fileName;
        this.errors = new ArrayList<>();
    }
    
    public List<ParseException.ParseError> getErrors() {
        return errors;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    private SourceLocation getLocation(ParserRuleContext ctx) {
        Token start = ctx.getStart();
        return new SourceLocation(fileName, start.getLine(), start.getCharPositionInLine() + 1);
    }
    
    private SourceLocation getLocation(Token token) {
        return new SourceLocation(fileName, token.getLine(), token.getCharPositionInLine() + 1);
    }
    
    private void addError(ParserRuleContext ctx, String message) {
        errors.add(new ParseException.ParseError(getLocation(ctx), message));
    }

    // ==================== Program ====================
    
    @Override
    public ProgramNode visitProgram(StorykeeParser.ProgramContext ctx) {
        ProgramNode program = new ProgramNode(getLocation(ctx));
        
        for (StorykeeParser.StatementContext stmtCtx : ctx.statement()) {
            StatementNode stmt = visitStatement(stmtCtx);
            if (stmt != null) {
                program.addStatement(stmt);
            }
        }
        
        return program;
    }
    
    // ==================== Statements ====================
    
    @Override
    public StatementNode visitStatement(StorykeeParser.StatementContext ctx) {
        if (ctx.variableDeclaration() != null) {
            return visitVariableDeclaration(ctx.variableDeclaration());
        } else if (ctx.npcDeclaration() != null) {
            return visitNpcDeclaration(ctx.npcDeclaration());
        } else if (ctx.dialogueDeclaration() != null) {
            return visitDialogueDeclaration(ctx.dialogueDeclaration());
        } else if (ctx.questDeclaration() != null) {
            return visitQuestDeclaration(ctx.questDeclaration());
        } else if (ctx.functionDeclaration() != null) {
            return visitFunctionDeclaration(ctx.functionDeclaration());
        } else if (ctx.eventHandler() != null) {
            return visitEventHandler(ctx.eventHandler());
        } else if (ctx.ifStatement() != null) {
            return visitIfStatement(ctx.ifStatement());
        } else if (ctx.forStatement() != null) {
            return (StatementNode) visit(ctx.forStatement());
        } else if (ctx.whileStatement() != null) {
            return visitWhileStatement(ctx.whileStatement());
        } else if (ctx.returnStatement() != null) {
            return visitReturnStatement(ctx.returnStatement());
        } else if (ctx.propertyStatement() != null) {
            return visitPropertyStatement(ctx.propertyStatement());
        } else if (ctx.nodeDeclaration() != null) {
            return visitNodeDeclaration(ctx.nodeDeclaration());
        } else if (ctx.expressionStatement() != null) {
            return visitExpressionStatement(ctx.expressionStatement());
        } else if (ctx.block() != null) {
            return visitBlock(ctx.block());
        } else if (ctx.javaSection() != null) {
            return visitJavaSection(ctx.javaSection());
        }
        
        addError(ctx, "Unknown statement type");
        return null;
    }
    
    @Override
    public PropertyStatementNode visitPropertyStatement(StorykeeParser.PropertyStatementContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        ExpressionNode value = visitExpression(ctx.expression());
        return new PropertyStatementNode(name, value, getLocation(ctx));
    }
    
    @Override
    public NodeDeclarationNode visitNodeDeclaration(StorykeeParser.NodeDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        BlockNode body = visitBlock(ctx.block());
        return new NodeDeclarationNode(name, body, getLocation(ctx));
    }
    
    @Override
    public VariableDeclarationNode visitVariableDeclaration(StorykeeParser.VariableDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        ExpressionNode value = visitExpression(ctx.expression());
        return new VariableDeclarationNode(name, value, getLocation(ctx));
    }
    
    @Override
    public NPCDeclarationNode visitNpcDeclaration(StorykeeParser.NpcDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        BlockNode body = visitBlock(ctx.block());
        return new NPCDeclarationNode(name, body, getLocation(ctx));
    }
    
    @Override
    public DialogueDeclarationNode visitDialogueDeclaration(StorykeeParser.DialogueDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        BlockNode body = visitBlock(ctx.block());
        return new DialogueDeclarationNode(name, body, getLocation(ctx));
    }
    
    @Override
    public QuestDeclarationNode visitQuestDeclaration(StorykeeParser.QuestDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        BlockNode body = visitBlock(ctx.block());
        return new QuestDeclarationNode(name, body, getLocation(ctx));
    }
    
    @Override
    public FunctionDeclarationNode visitFunctionDeclaration(StorykeeParser.FunctionDeclarationContext ctx) {
        String name = ctx.IDENTIFIER().getText();
        List<String> parameters = new ArrayList<>();
        
        if (ctx.parameterList() != null) {
            for (TerminalNode id : ctx.parameterList().IDENTIFIER()) {
                parameters.add(id.getText());
            }
        }
        
        BlockNode body = visitBlock(ctx.block());
        return new FunctionDeclarationNode(name, parameters, body, getLocation(ctx));
    }
    
    @Override
    public EventHandlerNode visitEventHandler(StorykeeParser.EventHandlerContext ctx) {
        String eventType = ctx.IDENTIFIER().getText();
        BlockNode body = visitBlock(ctx.block());
        return new EventHandlerNode(eventType, body, getLocation(ctx));
    }
    
    @Override
    public IfStatementNode visitIfStatement(StorykeeParser.IfStatementContext ctx) {
        ExpressionNode condition = visitExpression(ctx.expression());
        BlockNode thenBlock = visitBlock(ctx.block(0));
        BlockNode elseBlock = ctx.block().size() > 1 ? visitBlock(ctx.block(1)) : null;
        return new IfStatementNode(condition, thenBlock, elseBlock, getLocation(ctx));
    }
    
    @Override
    public ForStatementNode visitForInStatement(StorykeeParser.ForInStatementContext ctx) {
        String variable = ctx.loopVariable().getText();
        ExpressionNode collection = visitExpression(ctx.expression());
        BlockNode body = visitBlock(ctx.block());
        return new ForStatementNode(variable, collection, body, getLocation(ctx));
    }
    
    @Override
    public ForCStyleStatementNode visitForCStyleStatement(StorykeeParser.ForCStyleStatementContext ctx) {
        // Parse init: var i = 0 or i = 0
        String initVar = null;
        ExpressionNode initValue = null;
        boolean declareVar = false;
        if (ctx.forInit() != null) {
            var init = ctx.forInit();
            initVar = init.IDENTIFIER().getText();
            initValue = visitExpression(init.expression());
            declareVar = init.VAR() != null;
        }
        
        // Parse condition: i < 5
        ExpressionNode condition = ctx.expression() != null ? visitExpression(ctx.expression()) : null;
        
        // Parse update: i = i + 1
        String updateVar = null;
        ExpressionNode updateValue = null;
        if (ctx.forUpdate() != null) {
            var update = ctx.forUpdate();
            updateVar = update.IDENTIFIER().getText();
            updateValue = visitExpression(update.expression());
        }
        
        BlockNode body = visitBlock(ctx.block());
        return new ForCStyleStatementNode(initVar, initValue, declareVar, condition, updateVar, updateValue, body, getLocation(ctx));
    }
    
    @Override
    public WhileStatementNode visitWhileStatement(StorykeeParser.WhileStatementContext ctx) {
        ExpressionNode condition = visitExpression(ctx.expression());
        BlockNode body = visitBlock(ctx.block());
        return new WhileStatementNode(condition, body, getLocation(ctx));
    }
    
    @Override
    public ReturnStatementNode visitReturnStatement(StorykeeParser.ReturnStatementContext ctx) {
        ExpressionNode value = ctx.expression() != null ? visitExpression(ctx.expression()) : null;
        return new ReturnStatementNode(value, getLocation(ctx));
    }
    
    @Override
    public ExpressionStatementNode visitExpressionStatement(StorykeeParser.ExpressionStatementContext ctx) {
        ExpressionNode expression = visitExpression(ctx.expression());
        return new ExpressionStatementNode(expression, getLocation(ctx));
    }
    
    @Override
    public BlockNode visitBlock(StorykeeParser.BlockContext ctx) {
        BlockNode block = new BlockNode(getLocation(ctx));
        
        for (StorykeeParser.StatementContext stmtCtx : ctx.statement()) {
            StatementNode stmt = visitStatement(stmtCtx);
            if (stmt != null) {
                block.addStatement(stmt);
            }
        }
        
        return block;
    }
    
    @Override
    public JavaSectionNode visitJavaSection(StorykeeParser.JavaSectionContext ctx) {
        StringBuilder javaCode = new StringBuilder();
        for (TerminalNode codeNode : ctx.JAVA_CODE()) {
            javaCode.append(codeNode.getText());
        }
        return new JavaSectionNode(javaCode.toString().trim(), getLocation(ctx));
    }

    // ==================== Expressions ====================
    
    private ExpressionNode visitExpression(StorykeeParser.ExpressionContext ctx) {
        return (ExpressionNode) visit(ctx);
    }
    
    @Override
    public ExpressionNode visitPrimaryExpr(StorykeeParser.PrimaryExprContext ctx) {
        return (ExpressionNode) visit(ctx.primary());
    }
    
    @Override
    public ExpressionNode visitMemberAccess(StorykeeParser.MemberAccessContext ctx) {
        ExpressionNode object = visitExpression(ctx.expression());
        String member = ctx.memberName().getText();
        return new MemberAccessNode(object, member, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitArrayAccess(StorykeeParser.ArrayAccessContext ctx) {
        ExpressionNode array = visitExpression(ctx.expression(0));
        ExpressionNode index = visitExpression(ctx.expression(1));
        return new ArrayAccessNode(array, index, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitFunctionCall(StorykeeParser.FunctionCallContext ctx) {
        ExpressionNode callee = visitExpression(ctx.expression());
        List<ExpressionNode> arguments = new ArrayList<>();
        
        if (ctx.argumentList() != null) {
            for (StorykeeParser.ExpressionContext argCtx : ctx.argumentList().expression()) {
                arguments.add(visitExpression(argCtx));
            }
        }
        
        return new FunctionCallNode(callee, arguments, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitUnaryNot(StorykeeParser.UnaryNotContext ctx) {
        ExpressionNode operand = visitExpression(ctx.expression());
        return new UnaryExpressionNode("!", operand, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitUnaryMinus(StorykeeParser.UnaryMinusContext ctx) {
        ExpressionNode operand = visitExpression(ctx.expression());
        return new UnaryExpressionNode("-", operand, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitMulDiv(StorykeeParser.MulDivContext ctx) {
        ExpressionNode left = visitExpression(ctx.expression(0));
        String operator = ctx.STAR() != null ? "*" : "/";
        ExpressionNode right = visitExpression(ctx.expression(1));
        return new BinaryExpressionNode(left, operator, right, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitAddSub(StorykeeParser.AddSubContext ctx) {
        ExpressionNode left = visitExpression(ctx.expression(0));
        String operator = ctx.PLUS() != null ? "+" : "-";
        ExpressionNode right = visitExpression(ctx.expression(1));
        return new BinaryExpressionNode(left, operator, right, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitComparison(StorykeeParser.ComparisonContext ctx) {
        ExpressionNode left = visitExpression(ctx.expression(0));
        String operator;
        if (ctx.LT() != null) operator = "<";
        else if (ctx.GT() != null) operator = ">";
        else if (ctx.LE() != null) operator = "<=";
        else operator = ">=";
        ExpressionNode right = visitExpression(ctx.expression(1));
        return new BinaryExpressionNode(left, operator, right, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitEquality(StorykeeParser.EqualityContext ctx) {
        ExpressionNode left = visitExpression(ctx.expression(0));
        String operator = ctx.EQ() != null ? "==" : "!=";
        ExpressionNode right = visitExpression(ctx.expression(1));
        return new BinaryExpressionNode(left, operator, right, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitLogicalAnd(StorykeeParser.LogicalAndContext ctx) {
        ExpressionNode left = visitExpression(ctx.expression(0));
        ExpressionNode right = visitExpression(ctx.expression(1));
        return new BinaryExpressionNode(left, "&&", right, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitLogicalOr(StorykeeParser.LogicalOrContext ctx) {
        ExpressionNode left = visitExpression(ctx.expression(0));
        ExpressionNode right = visitExpression(ctx.expression(1));
        return new BinaryExpressionNode(left, "||", right, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitAssignment(StorykeeParser.AssignmentContext ctx) {
        String target = ctx.IDENTIFIER().getText();
        ExpressionNode value = visitExpression(ctx.expression());
        return new AssignmentNode(target, value, getLocation(ctx));
    }

    // ==================== Primary Expressions ====================
    
    @Override
    public ExpressionNode visitParenExpr(StorykeeParser.ParenExprContext ctx) {
        return visitExpression(ctx.expression());
    }
    
    @Override
    public ExpressionNode visitIdentifier(StorykeeParser.IdentifierContext ctx) {
        // identifierOrKeyword can be IDENTIFIER or a keyword (NPC, QUEST, etc.)
        String name = ctx.identifierOrKeyword().getText();
        return new IdentifierNode(name, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitStringLiteral(StorykeeParser.StringLiteralContext ctx) {
        String text = ctx.STRING().getText();
        // Remove surrounding quotes and process escape sequences
        String value = processStringLiteral(text);
        return new LiteralNode(value, LiteralNode.LiteralType.STRING, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitNumberLiteral(StorykeeParser.NumberLiteralContext ctx) {
        String text = ctx.NUMBER().getText();
        Number value;
        if (text.contains(".")) {
            value = Double.parseDouble(text);
        } else {
            value = Long.parseLong(text);
        }
        return new LiteralNode(value, LiteralNode.LiteralType.NUMBER, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitTrueLiteral(StorykeeParser.TrueLiteralContext ctx) {
        return new LiteralNode(true, LiteralNode.LiteralType.BOOLEAN, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitFalseLiteral(StorykeeParser.FalseLiteralContext ctx) {
        return new LiteralNode(false, LiteralNode.LiteralType.BOOLEAN, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitNullLiteral(StorykeeParser.NullLiteralContext ctx) {
        return new LiteralNode(null, LiteralNode.LiteralType.NULL, getLocation(ctx));
    }
    
    @Override
    public ExpressionNode visitArrayLiteralExpr(StorykeeParser.ArrayLiteralExprContext ctx) {
        return visitArrayLiteral(ctx.arrayLiteral());
    }
    
    @Override
    public ExpressionNode visitObjectLiteralExpr(StorykeeParser.ObjectLiteralExprContext ctx) {
        return visitObjectLiteral(ctx.objectLiteral());
    }
    
    @Override
    public ArrayLiteralNode visitArrayLiteral(StorykeeParser.ArrayLiteralContext ctx) {
        List<ExpressionNode> elements = new ArrayList<>();
        for (StorykeeParser.ExpressionContext exprCtx : ctx.expression()) {
            elements.add(visitExpression(exprCtx));
        }
        return new ArrayLiteralNode(elements, getLocation(ctx));
    }
    
    @Override
    public ObjectLiteralNode visitObjectLiteral(StorykeeParser.ObjectLiteralContext ctx) {
        Map<String, ExpressionNode> properties = new LinkedHashMap<>();
        for (StorykeeParser.ObjectPropertyContext propCtx : ctx.objectProperty()) {
            String key = getPropertyName(propCtx.propertyName());
            ExpressionNode value = visitExpression(propCtx.expression());
            properties.put(key, value);
        }
        return new ObjectLiteralNode(properties, getLocation(ctx));
    }
    
    /**
     * Extract property name from propertyName rule (handles identifiers, strings, and keywords).
     */
    private String getPropertyName(StorykeeParser.PropertyNameContext ctx) {
        if (ctx.STRING() != null) {
            return processStringLiteral(ctx.STRING().getText());
        }
        // IDENTIFIER or keyword (NPC, QUEST, DIALOGUE, NODE)
        return ctx.getText();
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Process a string literal by removing quotes and handling escape sequences.
     */
    private String processStringLiteral(String text) {
        // Remove surrounding quotes
        String content = text.substring(1, text.length() - 1);
        
        // Process escape sequences
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\' && i + 1 < content.length()) {
                char next = content.charAt(i + 1);
                switch (next) {
                    case 'n' -> { result.append('\n'); i++; }
                    case 't' -> { result.append('\t'); i++; }
                    case 'r' -> { result.append('\r'); i++; }
                    case '\\' -> { result.append('\\'); i++; }
                    case '"' -> { result.append('"'); i++; }
                    default -> result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}
