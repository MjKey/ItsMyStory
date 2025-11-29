package ru.mjkey.storykee.parser.ast;

/**
 * Base class for all Abstract Syntax Tree nodes.
 * Every node in the AST extends this class.
 */
public abstract class ASTNode {
    
    private SourceLocation location;
    private ASTNode parent;
    
    protected ASTNode() {
        this.location = SourceLocation.unknown();
    }
    
    protected ASTNode(SourceLocation location) {
        this.location = location;
    }
    
    public SourceLocation getLocation() {
        return location;
    }
    
    public void setLocation(SourceLocation location) {
        this.location = location;
    }
    
    public ASTNode getParent() {
        return parent;
    }
    
    public void setParent(ASTNode parent) {
        this.parent = parent;
    }
    
    /**
     * Accept a visitor for traversing the AST.
     * @param visitor The visitor to accept
     * @param <T> The return type of the visitor
     * @return The result of visiting this node
     */
    public abstract <T> T accept(ASTVisitor<T> visitor);
}
