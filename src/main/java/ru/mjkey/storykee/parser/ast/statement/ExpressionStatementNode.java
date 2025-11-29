package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents an expression used as a statement: expression;
 */
public class ExpressionStatementNode extends StatementNode {
    
    private final ExpressionNode expression;
    
    public ExpressionStatementNode(ExpressionNode expression) {
        super();
        this.expression = expression;
        if (expression != null) expression.setParent(this);
    }
    
    public ExpressionStatementNode(ExpressionNode expression, SourceLocation location) {
        super(location);
        this.expression = expression;
        if (expression != null) expression.setParent(this);
    }
    
    public ExpressionNode getExpression() {
        return expression;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitExpressionStatement(this);
    }
}
