package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents an event handler: on eventType { actions }
 */
public class EventHandlerNode extends StatementNode {
    
    private final String eventType;
    private final BlockNode body;
    
    public EventHandlerNode(String eventType, BlockNode body) {
        super();
        this.eventType = eventType;
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public EventHandlerNode(String eventType, BlockNode body, SourceLocation location) {
        super(location);
        this.eventType = eventType;
        this.body = body;
        if (body != null) {
            body.setParent(this);
        }
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public BlockNode getBody() {
        return body;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitEventHandler(this);
    }
}
