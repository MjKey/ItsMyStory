package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents array access: array[index]
 */
public class ArrayAccessNode extends ExpressionNode {
    
    private final ExpressionNode array;
    private final ExpressionNode index;
    
    public ArrayAccessNode(ExpressionNode array, ExpressionNode index) {
        super();
        this.array = array;
        this.index = index;
        if (array != null) array.setParent(this);
        if (index != null) index.setParent(this);
    }
    
    public ArrayAccessNode(ExpressionNode array, ExpressionNode index, SourceLocation location) {
        super(location);
        this.array = array;
        this.index = index;
        if (array != null) array.setParent(this);
        if (index != null) index.setParent(this);
    }
    
    public ExpressionNode getArray() {
        return array;
    }
    
    public ExpressionNode getIndex() {
        return index;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitArrayAccess(this);
    }
}
