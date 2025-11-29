package ru.mjkey.storykee.systems.dialogue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses dialogue definitions from AST nodes.
 * Converts DialogueDeclarationNode into Dialogue data structures.
 * Supports both declarative syntax (node start { ... }) and function syntax (say(), choice()).
 */
public class DialogueParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DialogueParser.class);
    
    /**
     * Parses a DialogueDeclarationNode into a Dialogue object.
     */
    public Dialogue parseDialogue(DialogueDeclarationNode node, String scriptId) {
        String dialogueId = node.getName();
        Dialogue dialogue = new Dialogue(dialogueId);
        dialogue.setScriptId(scriptId);
        
        if (node.getBody() == null) {
            LOGGER.warn("Dialogue '{}' has no body", dialogueId);
            return dialogue;
        }
        
        // First pass: extract dialogue-level properties (speaker, etc.)
        String defaultSpeaker = null;
        for (StatementNode stmt : node.getBody().getStatements()) {
            if (stmt instanceof PropertyStatementNode propStmt) {
                String propName = propStmt.getName();
                if ("speaker".equals(propName)) {
                    defaultSpeaker = evaluateStringExpression(propStmt.getValue());
                    dialogue.setDefaultSpeaker(defaultSpeaker);
                }
            }
        }
        
        // Second pass: parse node declarations and other statements
        List<DialogueNode> parsedNodes = new ArrayList<>();
        int nodeCounter = 0;
        
        for (StatementNode stmt : node.getBody().getStatements()) {
            if (stmt instanceof NodeDeclarationNode nodeDecl) {
                // Declarative node syntax: node start { ... }
                DialogueNode dialogueNode = parseNodeDeclaration(nodeDecl, dialogueId, defaultSpeaker);
                if (dialogueNode != null) {
                    parsedNodes.add(dialogueNode);
                }
            } else if (!(stmt instanceof PropertyStatementNode)) {
                // Legacy function-based syntax
                DialogueNode dialogueNode = parseStatement(stmt, dialogueId, nodeCounter);
                if (dialogueNode != null) {
                    parsedNodes.add(dialogueNode);
                    nodeCounter++;
                }
            }
        }
        
        // Add all nodes to dialogue
        for (DialogueNode dn : parsedNodes) {
            dialogue.addNode(dn);
        }
        
        // Link sequential nodes that don't have explicit next
        for (int i = 0; i < parsedNodes.size(); i++) {
            DialogueNode current = parsedNodes.get(i);
            if (current.getNextNodeId() == null && !current.hasChoices() && i + 1 < parsedNodes.size()) {
                current.setNextNodeId(parsedNodes.get(i + 1).getId());
            }
        }
        
        LOGGER.debug("Parsed dialogue '{}' with {} nodes, start node: {}", 
            dialogueId, parsedNodes.size(), dialogue.getStartNodeId());
        
        return dialogue;
    }
    
    /**
     * Parses a NodeDeclarationNode (node name { ... }) into a DialogueNode.
     */
    private DialogueNode parseNodeDeclaration(NodeDeclarationNode nodeDecl, String dialogueId, String defaultSpeaker) {
        String nodeId = nodeDecl.getName();
        DialogueNode dialogueNode = new DialogueNode(nodeId);
        
        if (defaultSpeaker != null) {
            dialogueNode.setSpeakerName(defaultSpeaker);
        }
        
        if (nodeDecl.getBody() == null) {
            return dialogueNode;
        }
        
        List<String> textLines = new ArrayList<>();
        
        for (StatementNode stmt : nodeDecl.getBody().getStatements()) {
            if (stmt instanceof PropertyStatementNode propStmt) {
                parseNodeProperty(propStmt, dialogueNode, textLines, dialogueId);
            } else if (stmt instanceof ExpressionStatementNode exprStmt) {
                // Support for action: functionCall(); syntax
                if (exprStmt.getExpression() instanceof FunctionCallNode) {
                    // Store action for later execution
                    LOGGER.debug("Node '{}' has action expression", nodeId);
                }
            }
        }
        
        // Combine text lines
        if (!textLines.isEmpty()) {
            dialogueNode.setText(String.join("\n", textLines));
        }
        
        return dialogueNode;
    }
    
    /**
     * Parses a property statement inside a node declaration.
     */
    private void parseNodeProperty(PropertyStatementNode propStmt, DialogueNode dialogueNode, 
                                   List<String> textLines, String dialogueId) {
        String propName = propStmt.getName();
        ExpressionNode value = propStmt.getValue();
        
        switch (propName) {
            case "text":
                String text = evaluateStringExpression(value);
                if (text != null && !text.isEmpty()) {
                    textLines.add(text);
                }
                break;
                
            case "speaker":
                dialogueNode.setSpeakerName(evaluateStringExpression(value));
                break;
                
            case "speakerTexture":
                dialogueNode.setSpeakerTexture(evaluateStringExpression(value));
                break;
                
            case "next":
                dialogueNode.setNextNodeId(evaluateStringExpression(value));
                break;
                
            case "choices":
                if (value instanceof ArrayLiteralNode choicesArray) {
                    int choiceIndex = 0;
                    for (ExpressionNode choiceExpr : choicesArray.getElements()) {
                        DialogueChoice choice = parseChoice(choiceExpr, dialogueNode.getId() + "_choice_" + choiceIndex);
                        if (choice != null) {
                            dialogueNode.addChoice(choice);
                            choiceIndex++;
                        }
                    }
                }
                break;
                
            case "action":
                // Store action expression for execution when node is displayed
                if (value instanceof FunctionCallNode) {
                    dialogueNode.addAction(value);
                    LOGGER.debug("Node '{}' has action: {}", dialogueNode.getId(), value);
                }
                break;
                
            default:
                LOGGER.debug("Unknown node property: {}", propName);
        }
    }
    
    /**
     * Parses a statement into a DialogueNode.
     */
    private DialogueNode parseStatement(StatementNode stmt, String dialogueId, int nodeIndex) {
        String nodeId = dialogueId + "_node_" + nodeIndex;
        
        if (stmt instanceof ExpressionStatementNode exprStmt) {
            return parseExpressionStatement(exprStmt, nodeId);
        } else if (stmt instanceof BlockNode block) {
            // Nested block - parse as a single node with choices
            return parseBlockAsNode(block, nodeId);
        }
        
        LOGGER.debug("Skipping unsupported statement type in dialogue: {}", stmt.getClass().getSimpleName());
        return null;
    }
    
    /**
     * Parses an expression statement as a dialogue node.
     * Supports function calls like say(), choice(), etc.
     */
    private DialogueNode parseExpressionStatement(ExpressionStatementNode stmt, String nodeId) {
        ExpressionNode expr = stmt.getExpression();
        
        if (expr instanceof FunctionCallNode funcCall) {
            return parseFunctionCall(funcCall, nodeId);
        } else if (expr instanceof LiteralNode literal) {
            // Simple string literal as dialogue text
            if (literal.getType() == LiteralNode.LiteralType.STRING) {
                DialogueNode node = new DialogueNode(nodeId);
                node.setText(String.valueOf(literal.getValue()));
                return node;
            }
        }
        
        return null;
    }
    
    /**
     * Parses a function call as a dialogue node.
     */
    private DialogueNode parseFunctionCall(FunctionCallNode funcCall, String nodeId) {
        ExpressionNode callee = funcCall.getCallee();
        if (!(callee instanceof IdentifierNode)) {
            return null;
        }
        
        String funcName = ((IdentifierNode) callee).getName();
        List<ExpressionNode> args = funcCall.getArguments();
        
        switch (funcName) {
            case "say":
            case "text":
            case "line":
                return parseSayFunction(args, nodeId);
            case "speaker":
                return parseSpeakerFunction(args, nodeId);
            case "node":
                return parseNodeFunction(args, nodeId);
            default:
                LOGGER.debug("Unknown dialogue function: {}", funcName);
                return null;
        }
    }
    
    /**
     * Parses say/text/line function: say("text") or say("speaker", "text")
     */
    private DialogueNode parseSayFunction(List<ExpressionNode> args, String nodeId) {
        if (args.isEmpty()) {
            return null;
        }
        
        DialogueNode node = new DialogueNode(nodeId);
        
        if (args.size() == 1) {
            // say("text")
            node.setText(evaluateStringExpression(args.get(0)));
        } else if (args.size() >= 2) {
            // say("speaker", "text")
            node.setSpeakerName(evaluateStringExpression(args.get(0)));
            node.setText(evaluateStringExpression(args.get(1)));
        }
        
        return node;
    }
    
    /**
     * Parses speaker function: speaker("name", "texture")
     */
    private DialogueNode parseSpeakerFunction(List<ExpressionNode> args, String nodeId) {
        DialogueNode node = new DialogueNode(nodeId);
        
        if (args.size() >= 1) {
            node.setSpeakerName(evaluateStringExpression(args.get(0)));
        }
        if (args.size() >= 2) {
            node.setSpeakerTexture(evaluateStringExpression(args.get(1)));
        }
        
        return node;
    }
    
    /**
     * Parses node function with object literal: node({ text: "...", choices: [...] })
     */
    private DialogueNode parseNodeFunction(List<ExpressionNode> args, String nodeId) {
        if (args.isEmpty() || !(args.get(0) instanceof ObjectLiteralNode)) {
            return null;
        }
        
        ObjectLiteralNode objLiteral = (ObjectLiteralNode) args.get(0);
        Map<String, ExpressionNode> props = objLiteral.getProperties();
        
        DialogueNode node = new DialogueNode(nodeId);
        
        // Parse text
        if (props.containsKey("text")) {
            node.setText(evaluateStringExpression(props.get("text")));
        }
        
        // Parse speaker
        if (props.containsKey("speaker")) {
            node.setSpeakerName(evaluateStringExpression(props.get("speaker")));
        }
        
        // Parse speakerTexture
        if (props.containsKey("speakerTexture")) {
            node.setSpeakerTexture(evaluateStringExpression(props.get("speakerTexture")));
        }
        
        // Parse next
        if (props.containsKey("next")) {
            node.setNextNodeId(evaluateStringExpression(props.get("next")));
        }
        
        // Parse choices
        if (props.containsKey("choices") && props.get("choices") instanceof ArrayLiteralNode) {
            ArrayLiteralNode choicesArray = (ArrayLiteralNode) props.get("choices");
            int choiceIndex = 0;
            for (ExpressionNode choiceExpr : choicesArray.getElements()) {
                DialogueChoice choice = parseChoice(choiceExpr, nodeId + "_choice_" + choiceIndex);
                if (choice != null) {
                    node.addChoice(choice);
                    choiceIndex++;
                }
            }
        }
        
        return node;
    }
    
    /**
     * Parses a choice from an expression.
     */
    private DialogueChoice parseChoice(ExpressionNode expr, String choiceId) {
        if (expr instanceof ObjectLiteralNode objLiteral) {
            Map<String, ExpressionNode> props = objLiteral.getProperties();
            
            String text = props.containsKey("text") ? evaluateStringExpression(props.get("text")) : "";
            String nextNodeId = props.containsKey("next") ? evaluateStringExpression(props.get("next")) : null;
            
            return new DialogueChoice(choiceId, text, nextNodeId);
        } else if (expr instanceof LiteralNode literal && literal.getType() == LiteralNode.LiteralType.STRING) {
            // Simple string choice
            return new DialogueChoice(choiceId, String.valueOf(literal.getValue()), null);
        }
        
        return null;
    }
    
    /**
     * Parses a block as a dialogue node with potential choices.
     */
    private DialogueNode parseBlockAsNode(BlockNode block, String nodeId) {
        DialogueNode node = new DialogueNode(nodeId);
        
        for (StatementNode stmt : block.getStatements()) {
            if (stmt instanceof ExpressionStatementNode exprStmt) {
                ExpressionNode expr = exprStmt.getExpression();
                if (expr instanceof FunctionCallNode funcCall) {
                    ExpressionNode callee = funcCall.getCallee();
                    if (callee instanceof IdentifierNode) {
                        String funcName = ((IdentifierNode) callee).getName();
                        if ("choice".equals(funcName)) {
                            DialogueChoice choice = parseChoiceFunction(funcCall, nodeId + "_choice_" + node.getChoices().size());
                            if (choice != null) {
                                node.addChoice(choice);
                            }
                        } else if ("text".equals(funcName) || "say".equals(funcName)) {
                            if (!funcCall.getArguments().isEmpty()) {
                                node.setText(evaluateStringExpression(funcCall.getArguments().get(0)));
                            }
                        }
                    }
                }
            }
        }
        
        return node;
    }
    
    /**
     * Parses choice function: choice("text", "nextNodeId")
     */
    private DialogueChoice parseChoiceFunction(FunctionCallNode funcCall, String choiceId) {
        List<ExpressionNode> args = funcCall.getArguments();
        if (args.isEmpty()) {
            return null;
        }
        
        String text = evaluateStringExpression(args.get(0));
        String nextNodeId = args.size() > 1 ? evaluateStringExpression(args.get(1)) : null;
        
        return new DialogueChoice(choiceId, text, nextNodeId);
    }
    
    /**
     * Evaluates a string expression to get its value.
     */
    private String evaluateStringExpression(ExpressionNode expr) {
        if (expr instanceof LiteralNode literal) {
            return String.valueOf(literal.getValue());
        } else if (expr instanceof IdentifierNode identifier) {
            // Return the identifier name as a variable reference placeholder
            return "${" + identifier.getName() + "}";
        }
        return "";
    }
}
