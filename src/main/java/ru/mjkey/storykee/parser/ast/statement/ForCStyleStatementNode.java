package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;
import ru.mjkey.storykee.parser.ast.expression.ExpressionNode;

/**
 * Represents a C-style for loop: for (var i = 0; i < 10; i = i + 1) { }
 */
public class ForCStyleStatementNode extends StatementNode {
    
    private final String initVariable;
    private final ExpressionNode initValue;
    private final boolean declareVariable;
    private final ExpressionNode condition;
    private final String updateVariable;
    private final ExpressionNode updateValue;
    private final BlockNode body;
    
    public ForCStyleStatementNode(String initVariable, ExpressionNode initValue, boolean declareVariable,
                                   ExpressionNode condition, String updateVariable, ExpressionNode updateValue,
                                   BlockNode body, SourceLocation location) {
        super(location);
        this.initVariable = initVariable;
        this.initValue = initValue;
        this.declareVariable = declareVariable;
        this.condition = condition;
        this.updateVariable = updateVariable;
        this.updateValue = updateValue;
        this.body = body;
        
        if (initValue != null) initValue.setParent(this);
        if (condition != null) condition.setParent(this);
        if (updateValue != null) updateValue.setParent(this);
        if (body != null) body.setParent(this);
    }
    
    public String getInitVariable() { return initVariable; }
    public ExpressionNode getInitValue() { return initValue; }
    public boolean isDeclareVariable() { return declareVariable; }
    public ExpressionNode getCondition() { return condition; }
    public String getUpdateVariable() { return updateVariable; }
    public ExpressionNode getUpdateValue() { return updateValue; }
    public BlockNode getBody() { return body; }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitForCStyleStatement(this);
    }
}
