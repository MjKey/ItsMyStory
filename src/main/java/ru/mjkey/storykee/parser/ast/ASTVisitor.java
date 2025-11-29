package ru.mjkey.storykee.parser.ast;

import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;

/**
 * Visitor interface for traversing the AST.
 * Implements the Visitor pattern for AST traversal.
 * 
 * @param <T> The return type of visit methods
 */
public interface ASTVisitor<T> {
    
    // Program
    T visitProgram(ProgramNode node);
    
    // Statements
    T visitVariableDeclaration(VariableDeclarationNode node);
    T visitFunctionDeclaration(FunctionDeclarationNode node);
    T visitNPCDeclaration(NPCDeclarationNode node);
    T visitDialogueDeclaration(DialogueDeclarationNode node);
    T visitQuestDeclaration(QuestDeclarationNode node);
    T visitEventHandler(EventHandlerNode node);
    T visitBlock(BlockNode node);
    T visitIfStatement(IfStatementNode node);
    T visitForStatement(ForStatementNode node);
    T visitForCStyleStatement(ForCStyleStatementNode node);
    T visitWhileStatement(WhileStatementNode node);
    T visitReturnStatement(ReturnStatementNode node);
    T visitExpressionStatement(ExpressionStatementNode node);
    T visitPropertyStatement(PropertyStatementNode node);
    T visitNodeDeclaration(NodeDeclarationNode node);
    T visitJavaSection(JavaSectionNode node);
    
    // Expressions
    T visitBinaryExpression(BinaryExpressionNode node);
    T visitUnaryExpression(UnaryExpressionNode node);
    T visitFunctionCall(FunctionCallNode node);
    T visitMemberAccess(MemberAccessNode node);
    T visitArrayAccess(ArrayAccessNode node);
    T visitAssignment(AssignmentNode node);
    T visitIdentifier(IdentifierNode node);
    T visitLiteral(LiteralNode node);
    T visitArrayLiteral(ArrayLiteralNode node);
    T visitObjectLiteral(ObjectLiteralNode node);
}
