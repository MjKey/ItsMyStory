package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents a variable declaration: var name = value;
 */
public class VariableDeclarationNode extends StatementNode {
    
    private final String name;
    private final ExpressionNode value;
    
    public VariableDeclarationNode(String name, ExpressionNode value) {
        super();
        this.name = name;
        this.value = value;
        if (value != null) {
            value.setParent(this);
        }
    }
    
    public VariableDeclarationNode(String name, ExpressionNode value, SourceLocation location) {
        super(location);
        this.name = name;
        this.value = value;
        if (value != null) {
            value.setParent(this);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public ExpressionNode getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitVariableDeclaration(this);
    }
}
