package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents an NPC declaration: npc name { properties }
 */
public class NPCDeclarationNode extends StatementNode {
    
    private final String name;
    private final BlockNode body;
    
    public NPCDeclarationNode(String name, BlockNode body) {
        super();
        this.name = name;
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public NPCDeclarationNode(String name, BlockNode body, SourceLocation location) {
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
        return visitor.visitNPCDeclaration(this);
    }
}
