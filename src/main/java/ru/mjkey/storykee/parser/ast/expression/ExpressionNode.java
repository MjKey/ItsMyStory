package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTNode;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Base class for all expression nodes in the AST.
 */
public abstract class ExpressionNode extends ASTNode {
    
    protected ExpressionNode() {
        super();
    }
    
    protected ExpressionNode(SourceLocation location) {
        super(location);
    }
}
