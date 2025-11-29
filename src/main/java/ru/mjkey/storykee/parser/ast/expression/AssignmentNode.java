package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents an assignment expression: target = value
 */
public class AssignmentNode extends ExpressionNode {
    
    private final String target;
    private final ExpressionNode value;
    
    public AssignmentNode(String target, ExpressionNode value) {
        super();
        this.target = target;
        this.value = value;
        if (value != null) value.setParent(this);
    }
    
    public AssignmentNode(String target, ExpressionNode value, SourceLocation location) {
        super(location);
        this.target = target;
        this.value = value;
        if (value != null) value.setParent(this);
    }
    
    public String getTarget() {
        return target;
    }
    
    public ExpressionNode getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitAssignment(this);
    }
}
