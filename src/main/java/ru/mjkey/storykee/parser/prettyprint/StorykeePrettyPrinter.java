package ru.mjkey.storykee.parser.prettyprint;

import ru.mjkey.storykee.parser.ast.ASTNode;
import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;

import java.util.List;
import java.util.Map;

/**
 * Pretty printer for converting AST back to formatted Storykee source code.
 * Implements the visitor pattern to traverse all AST node types and generate
 * properly formatted source code.
 * 
 * Requirements: 17.1, 17.2, 17.3
 */
public class StorykeePrettyPrinter implements ASTVisitor<String> {
    
    private final PrettyPrintOptions options;
    private int indentLevel = 0;
    
    public StorykeePrettyPrinter() {
        this.options = PrettyPrintOptions.defaults();
    }
    
    public StorykeePrettyPrinter(PrettyPrintOptions options) {
        this.options = options != null ? options : PrettyPrintOptions.defaults();
    }
    
    /**
     * Converts an AST node to formatted Storykee source code.
     * 
     * @param node The AST node to print
     * @return The formatted source code
     * @throws IllegalArgumentException if node is null
     */
    public String print(ASTNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Cannot print null AST node");
        }
        indentLevel = 0;
        return node.accept(this);
    }
    
    public PrettyPrintOptions getOptions() {
        return options;
    }
    
    // Helper methods for formatting
    
    private String indent() {
        return options.getIndentString().repeat(indentLevel);
    }

    private String newline() {
        return options.isInsertNewlines() ? "\n" : " ";
    }
    
    private void increaseIndent() {
        indentLevel++;
    }
    
    private void decreaseIndent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
    }
    
    // Program visitor
    
    @Override
    public String visitProgram(ProgramNode node) {
        StringBuilder sb = new StringBuilder();
        List<StatementNode> statements = node.getStatements();
        
        for (int i = 0; i < statements.size(); i++) {
            StatementNode stmt = statements.get(i);
            sb.append(stmt.accept(this));
            
            // Add blank line between top-level declarations for readability
            if (i < statements.size() - 1 && options.isInsertNewlines()) {
                if (isDeclaration(stmt) || isDeclaration(statements.get(i + 1))) {
                    sb.append(newline());
                }
            }
        }
        
        return sb.toString();
    }
    
    private boolean isDeclaration(StatementNode node) {
        return node instanceof FunctionDeclarationNode
            || node instanceof NPCDeclarationNode
            || node instanceof DialogueDeclarationNode
            || node instanceof QuestDeclarationNode
            || node instanceof EventHandlerNode;
    }
    
    // Statement visitors
    
    @Override
    public String visitVariableDeclaration(VariableDeclarationNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("var ").append(node.getName());
        
        if (node.getValue() != null) {
            sb.append(" = ").append(node.getValue().accept(this));
        }
        
        sb.append(";").append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitFunctionDeclaration(FunctionDeclarationNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("function ").append(node.getName()).append("(");
        sb.append(String.join(", ", node.getParameters()));
        sb.append(") ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }

    @Override
    public String visitNPCDeclaration(NPCDeclarationNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("npc ").append(node.getName()).append(" ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitDialogueDeclaration(DialogueDeclarationNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("dialogue ").append(node.getName()).append(" ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitQuestDeclaration(QuestDeclarationNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("quest ").append(node.getName()).append(" ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitEventHandler(EventHandlerNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("on ").append(node.getEventType()).append(" ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitBlock(BlockNode node) {
        return visitBlockInline(node);
    }
    
    /**
     * Visits a block node and returns the formatted block with braces.
     * This method handles the indentation for block contents.
     */
    private String visitBlockInline(BlockNode node) {
        if (node == null) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("{").append(newline());
        
        increaseIndent();
        for (StatementNode stmt : node.getStatements()) {
            sb.append(stmt.accept(this));
        }
        decreaseIndent();
        
        sb.append(indent()).append("}");
        return sb.toString();
    }
    
    @Override
    public String visitIfStatement(IfStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("if (").append(node.getCondition().accept(this)).append(") ");
        sb.append(visitBlockInline(node.getThenBlock()));
        
        if (node.hasElseBlock()) {
            sb.append(" else ");
            sb.append(visitBlockInline(node.getElseBlock()));
        }
        
        sb.append(newline());
        return sb.toString();
    }

    @Override
    public String visitForStatement(ForStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("for ").append(node.getVariable());
        sb.append(" in ").append(node.getCollection().accept(this)).append(" ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitForCStyleStatement(ForCStyleStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("for (");
        
        // Init
        if (node.getInitVariable() != null) {
            if (node.isDeclareVariable()) sb.append("var ");
            sb.append(node.getInitVariable()).append(" = ").append(node.getInitValue().accept(this));
        }
        sb.append("; ");
        
        // Condition
        if (node.getCondition() != null) {
            sb.append(node.getCondition().accept(this));
        }
        sb.append("; ");
        
        // Update
        if (node.getUpdateVariable() != null) {
            sb.append(node.getUpdateVariable()).append(" = ").append(node.getUpdateValue().accept(this));
        }
        sb.append(") ");
        
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitWhileStatement(WhileStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("while (").append(node.getCondition().accept(this)).append(") ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitReturnStatement(ReturnStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("return");
        
        if (node.hasValue()) {
            sb.append(" ").append(node.getValue().accept(this));
        }
        
        sb.append(";").append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitExpressionStatement(ExpressionStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append(node.getExpression().accept(this)).append(";").append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitPropertyStatement(PropertyStatementNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append(node.getName()).append(": ");
        sb.append(node.getValue().accept(this)).append(";").append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitNodeDeclaration(NodeDeclarationNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("node ").append(node.getName()).append(" ");
        sb.append(visitBlockInline(node.getBody()));
        sb.append(newline());
        return sb.toString();
    }
    
    @Override
    public String visitJavaSection(JavaSectionNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent()).append("#java-section-start").append(newline());
        
        // Preserve the Java code as-is, but ensure proper indentation for each line
        String javaCode = node.getJavaCode();
        if (javaCode != null && !javaCode.isEmpty()) {
            String[] lines = javaCode.split("\n", -1);
            for (String line : lines) {
                // Don't add extra indent to Java code - preserve original formatting
                sb.append(line).append(newline());
            }
        }
        
        sb.append(indent()).append("#java-section-end").append(newline());
        return sb.toString();
    }
    
    // Expression visitors
    
    @Override
    public String visitBinaryExpression(BinaryExpressionNode node) {
        StringBuilder sb = new StringBuilder();
        
        // Add parentheses for nested binary expressions to preserve precedence
        String left = node.getLeft().accept(this);
        String right = node.getRight().accept(this);
        
        if (needsParentheses(node.getLeft(), node)) {
            left = "(" + left + ")";
        }
        if (needsParentheses(node.getRight(), node)) {
            right = "(" + right + ")";
        }
        
        sb.append(left).append(" ").append(node.getOperator()).append(" ").append(right);
        return sb.toString();
    }
    
    /**
     * Determines if an expression needs parentheses based on operator precedence.
     */
    private boolean needsParentheses(ExpressionNode child, BinaryExpressionNode parent) {
        if (!(child instanceof BinaryExpressionNode)) {
            return false;
        }
        
        BinaryExpressionNode childBinary = (BinaryExpressionNode) child;
        int childPrecedence = getOperatorPrecedence(childBinary.getOperator());
        int parentPrecedence = getOperatorPrecedence(parent.getOperator());
        
        return childPrecedence < parentPrecedence;
    }

    /**
     * Returns the precedence level of an operator (higher = binds tighter).
     */
    private int getOperatorPrecedence(String operator) {
        switch (operator) {
            case "||":
                return 1;
            case "&&":
                return 2;
            case "==":
            case "!=":
                return 3;
            case "<":
            case ">":
            case "<=":
            case ">=":
                return 4;
            case "+":
            case "-":
                return 5;
            case "*":
            case "/":
            case "%":
                return 6;
            default:
                return 0;
        }
    }
    
    @Override
    public String visitUnaryExpression(UnaryExpressionNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getOperator());
        
        String operand = node.getOperand().accept(this);
        // Add parentheses for complex operands
        if (node.getOperand() instanceof BinaryExpressionNode) {
            operand = "(" + operand + ")";
        }
        
        sb.append(operand);
        return sb.toString();
    }
    
    @Override
    public String visitFunctionCall(FunctionCallNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getCallee().accept(this));
        sb.append("(");
        
        List<ExpressionNode> args = node.getArguments();
        for (int i = 0; i < args.size(); i++) {
            sb.append(args.get(i).accept(this));
            if (i < args.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public String visitMemberAccess(MemberAccessNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getObject().accept(this));
        sb.append(".").append(node.getMember());
        return sb.toString();
    }
    
    @Override
    public String visitArrayAccess(ArrayAccessNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getArray().accept(this));
        sb.append("[").append(node.getIndex().accept(this)).append("]");
        return sb.toString();
    }
    
    @Override
    public String visitAssignment(AssignmentNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getTarget()).append(" = ").append(node.getValue().accept(this));
        return sb.toString();
    }
    
    @Override
    public String visitIdentifier(IdentifierNode node) {
        return node.getName();
    }
    
    @Override
    public String visitLiteral(LiteralNode node) {
        switch (node.getType()) {
            case STRING:
                return formatString((String) node.getValue());
            case NUMBER:
                return formatNumber(node.getValue());
            case BOOLEAN:
                return node.getValue().toString();
            case NULL:
                return "null";
            default:
                return String.valueOf(node.getValue());
        }
    }

    /**
     * Formats a string literal with proper escaping and quotes.
     */
    private String formatString(String value) {
        if (value == null) {
            return "\"\"";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        
        sb.append("\"");
        return sb.toString();
    }
    
    /**
     * Formats a number literal, handling integers and decimals appropriately.
     */
    private String formatNumber(Object value) {
        if (value == null) {
            return "0";
        }
        
        if (value instanceof Double) {
            double d = (Double) value;
            // If it's a whole number, format without decimal point
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        } else if (value instanceof Float) {
            float f = (Float) value;
            if (f == Math.floor(f) && !Float.isInfinite(f)) {
                return String.valueOf((long) f);
            }
            return String.valueOf(f);
        }
        
        return String.valueOf(value);
    }
    
    @Override
    public String visitArrayLiteral(ArrayLiteralNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        
        List<ExpressionNode> elements = node.getElements();
        for (int i = 0; i < elements.size(); i++) {
            sb.append(elements.get(i).accept(this));
            if (i < elements.size() - 1) {
                sb.append(", ");
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public String visitObjectLiteral(ObjectLiteralNode node) {
        StringBuilder sb = new StringBuilder();
        Map<String, ExpressionNode> properties = node.getProperties();
        
        if (properties.isEmpty()) {
            return "{}";
        }
        
        sb.append("{");
        
        if (options.isInsertNewlines() && properties.size() > 1) {
            sb.append(newline());
            increaseIndent();
            
            int i = 0;
            for (Map.Entry<String, ExpressionNode> entry : properties.entrySet()) {
                sb.append(indent()).append(entry.getKey()).append(": ");
                sb.append(entry.getValue().accept(this));
                
                if (i < properties.size() - 1) {
                    sb.append(",");
                }
                sb.append(newline());
                i++;
            }
            
            decreaseIndent();
            sb.append(indent()).append("}");
        } else {
            // Single line format for small objects
            sb.append(" ");
            int i = 0;
            for (Map.Entry<String, ExpressionNode> entry : properties.entrySet()) {
                sb.append(entry.getKey()).append(": ");
                sb.append(entry.getValue().accept(this));
                
                if (i < properties.size() - 1) {
                    sb.append(", ");
                }
                i++;
            }
            sb.append(" }");
        }
        
        return sb.toString();
    }
}
