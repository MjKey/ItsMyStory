package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents a literal value (string, number, boolean, null).
 */
public class LiteralNode extends ExpressionNode {
    
    public enum LiteralType {
        STRING,
        NUMBER,
        BOOLEAN,
        NULL
    }
    
    private final Object value;
    private final LiteralType type;
    
    public LiteralNode(Object value, LiteralType type) {
        super();
        this.value = value;
        this.type = type;
    }
    
    public LiteralNode(Object value, LiteralType type, SourceLocation location) {
        super(location);
        this.value = value;
        this.type = type;
    }
    
    public Object getValue() {
        return value;
    }
    
    public LiteralType getType() {
        return type;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitLiteral(this);
    }
}
