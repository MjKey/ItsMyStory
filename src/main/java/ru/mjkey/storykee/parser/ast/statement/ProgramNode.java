package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTNode;
import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Root node of the AST representing an entire Storykee program.
 */
public class ProgramNode extends ASTNode {
    
    private final List<StatementNode> statements;
    
    public ProgramNode() {
        super();
        this.statements = new ArrayList<>();
    }
    
    public ProgramNode(SourceLocation location) {
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
        return visitor.visitProgram(this);
    }
}
