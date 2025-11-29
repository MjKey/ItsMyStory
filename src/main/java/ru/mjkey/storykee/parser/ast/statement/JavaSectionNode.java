package ru.mjkey.storykee.parser.ast.statement;

import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.SourceLocation;

/**
 * Represents a Java code section: #java-section-start ... #java-section-end
 */
public class JavaSectionNode extends StatementNode {
    
    private final String javaCode;
    
    public JavaSectionNode(String javaCode) {
        super();
        this.javaCode = javaCode;
    }
    
    public JavaSectionNode(String javaCode, SourceLocation location) {
        super(location);
        this.javaCode = javaCode;
    }
    
    public String getJavaCode() {
        return javaCode;
    }
    
    @Override
    public <T> T accept(ASTVisitor<T> visitor) {
        return visitor.visitJavaSection(this);
    }
}
