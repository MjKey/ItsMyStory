package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an array literal: [element1, element2, ...]
 */
public class ArrayLiteralNode extends ExpressionNode {
    
    private final List<ExpressionNode> elements;
    
    public ArrayLiteralNode(List<ExpressionNode> elements) {
        super();
        this.elements = new ArrayList<>(elements);
        for (ExpressionNode element : this.elements) {
            if (element != null) element.setParent(this);
        }
    }
    
    public ArrayLiteralNode(List<ExpressionNode> elements, SourceLocation location) {
        super(location);
        this.elements = new ArrayList<>(elements);
        for (ExpressionNode element : this.elements) {
            if (element != null) element.setParent(this);
        }
    }
    
    public List<ExpressionNode> getElements() {
        return Collections.unmodifiableList(elements);
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitArrayLiteral(this);
    }
}
