package ru.mjkey.storykee.parser.ast.expression;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents member access: object.member
 */
public class MemberAccessNode extends ExpressionNode {
    
    private final ExpressionNode object;
    private final String member;
    
    public MemberAccessNode(ExpressionNode object, String member) {
        super();
        this.object = object;
        this.member = member;
        if (object != null) object.setParent(this);
    }
    
    public MemberAccessNode(ExpressionNode object, String member, SourceLocation location) {
        super(location);
        this.object = object;
        this.member = member;
        if (object != null) object.setParent(this);
    }
    
    public ExpressionNode getObject() {
        return object;
    }
    
    public String getMember() {
        return member;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitMemberAccess(this);
    }
}
