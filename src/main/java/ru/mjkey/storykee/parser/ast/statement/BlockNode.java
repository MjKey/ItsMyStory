package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a block of statements enclosed in braces.
 */
public class BlockNode extends StatementNode {
    
    private final List<StatementNode> statements;
    
    public BlockNode() {
        super();
        this.statements = new ArrayList<>();
    }
    
    public BlockNode(SourceLocation location) {
        super(location);
        this.statements = new ArrayList<>();
    }
    
    public void addStatement(StatementNode statement) {
        statements.add(statement);
        statement.setParent(this);
    }
    
    public List<StatementNode> getStatements() {
        return Collections.unmodifiableList(statements);
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitBlock(this);
    }
}
