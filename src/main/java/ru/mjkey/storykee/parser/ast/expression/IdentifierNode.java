package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents an identifier reference.
 */
public class IdentifierNode extends ExpressionNode {
    
    private final String name;
    
    public IdentifierNode(String name) {
        super();
        this.name = name;
    }
    
    public IdentifierNode(String name, SourceLocation location) {
        super(location);
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitIdentifier(this);
    }
}
