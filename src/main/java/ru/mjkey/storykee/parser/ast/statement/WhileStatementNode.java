package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents a while loop: while (condition) { }
 */
public class WhileStatementNode extends StatementNode {
    
    private final ExpressionNode condition;
    private final BlockNode body;
    
    public WhileStatementNode(ExpressionNode condition, BlockNode body) {
        super();
        this.condition = condition;
        this.body = body;
        
        if (condition != null) condition.setParent(this);
        if (body != null) body.setParent(this);
    }
    
    public WhileStatementNode(ExpressionNode condition, BlockNode body, SourceLocation location) {
        super(location);
        this.condition = condition;
        this.body = body;
        
        if (condition != null) condition.setParent(this);
        if (body != null) body.setParent(this);
    }
    
    public ExpressionNode getCondition() {
        return condition;
    }
    
    public BlockNode getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitWhileStatement(this);
    }
}
