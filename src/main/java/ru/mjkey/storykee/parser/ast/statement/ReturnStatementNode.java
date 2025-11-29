package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents a return statement: return value;
 */
public class ReturnStatementNode extends StatementNode {
    
    private final ExpressionNode value; // nullable
    
    public ReturnStatementNode(ExpressionNode value) {
        super();
        this.value = value;
        if (value != null) value.setParent(this);
    }
    
    public ReturnStatementNode(ExpressionNode value, SourceLocation location) {
        super(location);
        this.value = value;
        if (value != null) value.setParent(this);
    }
    
    public ExpressionNode getValue() {
        return value;
    }
    
    public boolean hasValue() {
        return value != null;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitReturnStatement(this);
    }
}
