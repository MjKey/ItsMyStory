package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a function declaration: function name(params) { body }
 */
public class FunctionDeclarationNode extends StatementNode {
    
    private final String name;
    private final List<String> parameters;
    private final BlockNode body;
    
    public FunctionDeclarationNode(String name, List<String> parameters, BlockNode body) {
        super();
        this.name = name;
        this.parameters = new ArrayList<>(parameters);
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public FunctionDeclarationNode(String name, List<String> parameters, BlockNode body, SourceLocation location) {
        super(location);
        this.name = name;
        this.parameters = new ArrayList<>(parameters);
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public List<String> getParameters() {
        return Collections.unmodifiableList(parameters);
    }
    
    public BlockNode getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitFunctionDeclaration(this);
    }
}
