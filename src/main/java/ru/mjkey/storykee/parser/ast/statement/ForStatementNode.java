package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents a for loop: for item in collection { }
 */
public class ForStatementNode extends StatementNode {
    
    private final String variable;
    private final ExpressionNode collection;
    private final BlockNode body;
    
    public ForStatementNode(String variable, ExpressionNode collection, BlockNode body) {
        super();
        this.variable = variable;
        this.collection = collection;
        this.body = body;
        
        if (collection != null) collection.setParent(this);
        if (body != null) body.setParent(this);
    }
    
    public ForStatementNode(String variable, ExpressionNode collection, BlockNode body, SourceLocation location) {
        super(location);
        this.variable = variable;
        this.collection = collection;
        this.body = body;
        
        if (collection != null) collection.setParent(this);
        if (body != null) body.setParent(this);
    }
    
    public String getVariable() {
        return variable;
    }
    
    public ExpressionNode getCollection() {
        return collection;
    }
    
    public BlockNode getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitForStatement(this);
    }
}
