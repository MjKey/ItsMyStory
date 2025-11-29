package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents a unary expression: operator operand
 */
public class UnaryExpressionNode extends ExpressionNode {
    
    private final String operator;
    private final ExpressionNode operand;
    
    public UnaryExpressionNode(String operator, ExpressionNode operand) {
        super();
        this.operator = operator;
        this.operand = operand;
        if (operand != null) operand.setParent(this);
    }
    
    public UnaryExpressionNode(String operator, ExpressionNode operand, SourceLocation location) {
        super(location);
        this.operator = operator;
        this.operand = operand;
        if (operand != null) operand.setParent(this);
    }
    
    public String getOperator() {
        return operator;
    }
    
    public ExpressionNode getOperand() {
        return operand;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitUnaryExpression(this);
    }
}
