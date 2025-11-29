package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTNode;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Base class for all statement nodes in the AST.
 */
public abstract class StatementNode extends ASTNode {
    
    protected StatementNode() {
        super();
    }
    
    protected StatementNode(SourceLocation location) {
        super(location);
    }
}
