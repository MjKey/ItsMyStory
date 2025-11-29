package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an object literal: { key1: value1, key2: value2, ... }
 */
public class ObjectLiteralNode extends ExpressionNode {
    
    private final Map<String, ExpressionNode> properties;
    
    public ObjectLiteralNode(Map<String, ExpressionNode> properties) {
        super();
        this.properties = new LinkedHashMap<>(properties);
        for (ExpressionNode value : this.properties.values()) {
            if (value != null) value.setParent(this);
        }
    }
    
    public ObjectLiteralNode(Map<String, ExpressionNode> properties, SourceLocation location) {
        super(location);
        this.properties = new LinkedHashMap<>(properties);
        for (ExpressionNode value : this.properties.values()) {
            if (value != null) value.setParent(this);
        }
    }
    
    public Map<String, ExpressionNode> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitObjectLiteral(this);
    }
}
