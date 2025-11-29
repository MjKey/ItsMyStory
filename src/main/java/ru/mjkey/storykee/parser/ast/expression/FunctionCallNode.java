package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a function call: name(arguments)
 */
public class FunctionCallNode extends ExpressionNode {
    
    private final ExpressionNode callee;
    private final List<ExpressionNode> arguments;
    
    public FunctionCallNode(ExpressionNode callee, List<ExpressionNode> arguments) {
        super();
        this.callee = callee;
        this.arguments = new ArrayList<>(arguments);
        
        if (callee != null) callee.setParent(this);
        for (ExpressionNode arg : this.arguments) {
            if (arg != null) arg.setParent(this);
        }
    }
    
    public FunctionCallNode(ExpressionNode callee, List<ExpressionNode> arguments, SourceLocation location) {
        super(location);
        this.callee = callee;
        this.arguments = new ArrayList<>(arguments);
        
        if (callee != null) callee.setParent(this);
        for (ExpressionNode arg : this.arguments) {
            if (arg != null) arg.setParent(this);
        }
    }
    
    public ExpressionNode getCallee() {
        return callee;
    }
    
    public List<ExpressionNode> getArguments() {
        return Collections.unmodifiableList(arguments);
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunctionCall(this);
    }
}
