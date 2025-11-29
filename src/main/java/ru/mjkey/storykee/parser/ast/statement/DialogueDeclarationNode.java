package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents a dialogue declaration: dialogue name { lines and choices }
 */
public class DialogueDeclarationNode extends StatementNode {
    
    private final String name;
    private final BlockNode body;
    
    public DialogueDeclarationNode(String name, BlockNode body) {
        super();
        this.name = name;
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public DialogueDeclarationNode(String name, BlockNode body, SourceLocation location) {
        super(location);
        this.name = name;
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public String getName() {
        return name;
    }
    
    public BlockNode getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitDialogueDeclaration(this);
    }
}
