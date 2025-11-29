package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents an if statement: if (condition) { } else { }
 */
public class IfStatementNode extends StatementNode {
    
    private final ExpressionNode condition;
    private final BlockNode thenBlock;
    private final BlockNode elseBlock; // nullable
    
    public IfStatementNode(ExpressionNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        super();
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        
        if (condition != null) condition.setParent(this);
        if (thenBlock != null) thenBlock.setParent(this);
        if (elseBlock != null) elseBlock.setParent(this);
    }
    
    public IfStatementNode(ExpressionNode condition, BlockNode thenBlock, BlockNode elseBlock, SourceLocation location) {
        super(location);
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        
        if (condition != null) condition.setParent(this);
        if (thenBlock != null) thenBlock.setParent(this);
        if (elseBlock != null) elseBlock.setParent(this);
    }
    
    public ExpressionNode getCondition() {
        return condition;
    }
    
    public BlockNode getThenBlock() {
        return thenBlock;
    }
    
    public BlockNode getElseBlock() {
        return elseBlock;
    }
    
    public boolean hasElseBlock() {
        return elseBlock != null;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIfStatement(this);
    }
}
