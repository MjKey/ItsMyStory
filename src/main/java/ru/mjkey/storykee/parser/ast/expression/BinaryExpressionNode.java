package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents a binary expression: left operator right
 */
public class BinaryExpressionNode extends ExpressionNode {
    
    private final ExpressionNode left;
    private final String operator;
    private final ExpressionNode right;
    
    public BinaryExpressionNode(ExpressionNode left, String operator, ExpressionNode right) {
        super();
        this.left = left;
        this.operator = operator;
        this.right = right;
        
        if (left != null) left.setParent(this);
        if (right != null) right.setParent(this);
    }
    
    public BinaryExpressionNode(ExpressionNode left, String operator, ExpressionNode right, SourceLocation location) {
        super(location);
        this.left = left;
        this.operator = operator;
        this.right = right;
        
        if (left != null) left.setParent(this);
        if (right != null) right.setParent(this);
    }
    
    public ExpressionNode getLeft() {
        return left;
    }
    
    public String getOperator() {
        return operator;
    }
    
    public ExpressionNode getRight() {
        return right;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBinaryExpression(this);
    }
}
