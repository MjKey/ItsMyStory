package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents a property statement: name: value;
 * Used inside NPC, dialogue, quest, and node blocks.
 */
public class PropertyStatementNode extends StatementNode {
    
    private final String name;
    private final ExpressionNode value;
    
    public PropertyStatementNode(String name, ExpressionNode value) {
        super();
        this.name = name;
        this.value = value;
        if (value != null) {
            value.setParent(this);
        }
    }
    
    public PropertyStatementNode(String name, ExpressionNode value, SourceLocation location) {
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
        return visitor.visitPropertyStatement(this);
    }
}
