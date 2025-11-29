package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents a quest declaration: quest name { objectives and rewards }
 */
public class QuestDeclarationNode extends StatementNode {
    
    private final String name;
    private final BlockNode body;
    
    public QuestDeclarationNode(String name, BlockNode body) {
        super();
        this.name = name;
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public QuestDeclarationNode(String name, BlockNode body, SourceLocation location) {
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
        return visitor.visitQuestDeclaration(this);
    }
}
