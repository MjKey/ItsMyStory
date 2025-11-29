package ru.mjkey.storykee.systems.quest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;

import java.util.List;
import java.util.Map;

/**
 * Parses quest declarations from AST nodes into Quest objects.
 * Requirements: 8.1, 8.2
 */
public class QuestParser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestParser.class);
    
    /**
     * Parses a QuestDeclarationNode into a Quest object.
     */
    public Quest parseQuest(QuestDeclarationNode node, String scriptId) {
        if (node == null) {
            return null;
        }
        
        Quest.Builder builder = new Quest.Builder()
            .id(node.getName())
            .scriptId(scriptId);
        
        // Parse the quest body for properties, objectives, and rewards
        if (node.getBody() != null) {
            parseQuestBody(node.getBody(), builder);
        }
        
        Quest quest = builder.build();
        
        // Validate
        List<String> errors = quest.validate();
        if (!errors.isEmpty()) {
            LOGGER.warn("Quest '{}' has validation warnings: {}", quest.getId(), errors);
        }
        
        return quest;
    }
    
    /**
     * Parses the quest body block for properties and objectives.
     */
    private void parseQuestBody(BlockNode body, Quest.Builder builder) {
        for (StatementNode statement : body.getStatements()) {
            if (statement instanceof VariableDeclarationNode varDecl) {
                parseQuestProperty(varDecl, builder);
            } else if (statement instanceof PropertyStatementNode propStmt) {
                // Handle property syntax: title: "...";
                parsePropertyStatement(propStmt, builder);
            } else if (statement instanceof ExpressionStatementNode exprStmt) {
                parseQuestExpression(exprStmt.getExpression(), builder);
            }
        }
    }
    
    /**
     * Parses a property statement (title: "value";) as a quest property.
     */
    private void parsePropertyStatement(PropertyStatementNode propStmt, Quest.Builder builder) {
        String name = propStmt.getName();
        ExpressionNode valueExpr = propStmt.getValue();
        Object value = evaluateLiteral(valueExpr);
        
        switch (name.toLowerCase()) {
            case "title" -> {
                if (value instanceof String str) builder.title(str);
            }
            case "description" -> {
                if (value instanceof String str) builder.description(str);
            }
            case "repeatable" -> {
                if (value instanceof Boolean b) builder.repeatable(b);
            }
            case "cooldown" -> {
                if (value instanceof Number n) builder.cooldownTicks(n.intValue());
            }
            case "prerequisite", "requires" -> {
                if (value instanceof String str) builder.prerequisite(str);
            }
            case "objectives" -> {
                if (valueExpr instanceof ArrayLiteralNode arrayLit) {
                    parseObjectivesArray(arrayLit, builder);
                }
            }
            case "rewards" -> {
                if (valueExpr instanceof ObjectLiteralNode objLit) {
                    parseRewardsObject(objLit, builder);
                }
            }
            case "oncomplete" -> {
                if (value instanceof String str) builder.onCompleteCallback(str);
            }
        }
    }
    
    /**
     * Parses a variable declaration as a quest property.
     */
    private void parseQuestProperty(VariableDeclarationNode varDecl, Quest.Builder builder) {
        String name = varDecl.getName();
        ExpressionNode valueExpr = varDecl.getValue();
        Object value = evaluateLiteral(valueExpr);
        
        switch (name.toLowerCase()) {
            case "title" -> {
                if (value instanceof String str) {
                    builder.title(str);
                }
            }
            case "description" -> {
                if (value instanceof String str) {
                    builder.description(str);
                }
            }
            case "repeatable" -> {
                if (value instanceof Boolean b) {
                    builder.repeatable(b);
                }
            }
            case "cooldown" -> {
                if (value instanceof Number n) {
                    builder.cooldownTicks(n.intValue());
                }
            }
            case "prerequisite", "requires" -> {
                if (value instanceof String str) {
                    builder.prerequisite(str);
                }
            }
            case "objectives" -> {
                // Parse objectives array: objectives: [ { id: "...", ... }, ... ]
                if (valueExpr instanceof ArrayLiteralNode arrayLit) {
                    parseObjectivesArray(arrayLit, builder);
                }
            }
            case "rewards" -> {
                // Parse rewards object: rewards: { experience: 500, items: [...] }
                if (valueExpr instanceof ObjectLiteralNode objLit) {
                    parseRewardsObject(objLit, builder);
                }
            }
            case "oncomplete" -> {
                // Store callback function name
                if (value instanceof String str) {
                    builder.onCompleteCallback(str);
                }
            }
        }
    }
    
    /**
     * Parses objectives array format: objectives: [ { id: "...", description: "...", ... }, ... ]
     */
    private void parseObjectivesArray(ArrayLiteralNode arrayLit, Quest.Builder builder) {
        for (ExpressionNode element : arrayLit.getElements()) {
            if (element instanceof ObjectLiteralNode objLit) {
                QuestObjective.Builder objBuilder = new QuestObjective.Builder();
                
                for (Map.Entry<String, ExpressionNode> entry : objLit.getProperties().entrySet()) {
                    String key = entry.getKey();
                    Object val = evaluateLiteral(entry.getValue());
                    
                    switch (key.toLowerCase()) {
                        case "id" -> {
                            if (val instanceof String s) objBuilder.id(s);
                        }
                        case "description" -> {
                            if (val instanceof String s) objBuilder.description(s);
                        }
                        case "type" -> {
                            if (val instanceof String s) {
                                try {
                                    objBuilder.type(ObjectiveType.valueOf(s.toUpperCase().replace("-", "_")));
                                } catch (IllegalArgumentException e) {
                                    objBuilder.type(ObjectiveType.CUSTOM);
                                }
                            }
                        }
                        case "target" -> {
                            if (val instanceof String s) objBuilder.criterion("target", s);
                        }
                        case "region" -> {
                            if (val instanceof String s) objBuilder.criterion("region", s);
                        }
                        case "count" -> {
                            if (val instanceof Number n) objBuilder.targetCount(n.intValue());
                        }
                        case "optional" -> {
                            if (val instanceof Boolean b) objBuilder.optional(b);
                        }
                    }
                }
                
                builder.addObjective(objBuilder.build());
            }
        }
    }
    
    /**
     * Parses rewards object format: rewards: { experience: 500, items: [...] }
     */
    private void parseRewardsObject(ObjectLiteralNode objLit, Quest.Builder builder) {
        for (Map.Entry<String, ExpressionNode> entry : objLit.getProperties().entrySet()) {
            String key = entry.getKey();
            ExpressionNode valueExpr = entry.getValue();
            Object val = evaluateLiteral(valueExpr);
            
            switch (key.toLowerCase()) {
                case "experience", "xp" -> {
                    if (val instanceof Number n) {
                        builder.reward("experience", n.intValue());
                    }
                }
                case "items" -> {
                    if (valueExpr instanceof ArrayLiteralNode itemsArray) {
                        for (ExpressionNode itemExpr : itemsArray.getElements()) {
                            if (itemExpr instanceof ObjectLiteralNode itemObj) {
                                String itemId = null;
                                int count = 1;
                                for (Map.Entry<String, ExpressionNode> itemEntry : itemObj.getProperties().entrySet()) {
                                    Object itemVal = evaluateLiteral(itemEntry.getValue());
                                    if ("item".equals(itemEntry.getKey()) && itemVal instanceof String s) {
                                        itemId = s;
                                    } else if ("count".equals(itemEntry.getKey()) && itemVal instanceof Number n) {
                                        count = n.intValue();
                                    }
                                }
                                if (itemId != null) {
                                    builder.reward("item:" + itemId, count);
                                }
                            }
                        }
                    }
                }
                case "money", "currency" -> {
                    if (val instanceof Number n) {
                        builder.reward("money", n.intValue());
                    }
                }
            }
        }
    }
    
    /**
     * Parses an expression statement for objectives and rewards.
     */
    private void parseQuestExpression(ExpressionNode expr, Quest.Builder builder) {
        if (expr instanceof FunctionCallNode funcCall) {
            String funcName = getFunctionName(funcCall);
            List<ExpressionNode> args = funcCall.getArguments();
            
            switch (funcName.toLowerCase()) {
                case "objective" -> parseObjective(args, builder);
                case "reward" -> parseReward(args, builder);
                case "prerequisite", "requires" -> parsePrerequisite(args, builder);
            }
        }
    }
    
    /**
     * Parses an objective function call.
     * Format: objective(id, type, description, targetCount, criteria...)
     */
    private void parseObjective(List<ExpressionNode> args, Quest.Builder builder) {
        if (args.isEmpty()) {
            LOGGER.warn("Objective declaration requires at least an ID");
            return;
        }
        
        QuestObjective.Builder objBuilder = new QuestObjective.Builder();
        
        // First arg: ID
        Object idValue = evaluateLiteral(args.get(0));
        if (idValue instanceof String s) {
            objBuilder.id(s);
        } else {
            LOGGER.warn("Objective ID must be a string");
            return;
        }
        
        // Second arg: type (optional, defaults to CUSTOM)
        if (args.size() > 1) {
            Object typeValue = evaluateLiteral(args.get(1));
            if (typeValue instanceof String typeStr) {
                try {
                    objBuilder.type(ObjectiveType.valueOf(typeStr.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown objective type: {}, using CUSTOM", typeStr);
                    objBuilder.type(ObjectiveType.CUSTOM);
                }
            }
        }
        
        // Third arg: description
        if (args.size() > 2) {
            Object descValue = evaluateLiteral(args.get(2));
            if (descValue instanceof String descStr) {
                objBuilder.description(descStr);
            }
        }
        
        // Fourth arg: target count
        if (args.size() > 3) {
            Object countValue = evaluateLiteral(args.get(3));
            if (countValue instanceof Number n) {
                objBuilder.targetCount(n.intValue());
            }
        }
        
        // Fifth arg onwards: criteria as object literal or key-value pairs
        if (args.size() > 4) {
            ExpressionNode criteriaArg = args.get(4);
            if (criteriaArg instanceof ObjectLiteralNode objLit) {
                for (Map.Entry<String, ExpressionNode> entry : objLit.getProperties().entrySet()) {
                    Object value = evaluateLiteral(entry.getValue());
                    objBuilder.criterion(entry.getKey(), value);
                }
            }
        }
        
        builder.addObjective(objBuilder.build());
    }
    
    /**
     * Parses a reward function call.
     * Format: reward(type, value) or reward(type, itemId, count)
     */
    private void parseReward(List<ExpressionNode> args, Quest.Builder builder) {
        if (args.isEmpty()) {
            LOGGER.warn("Reward declaration requires at least a type");
            return;
        }
        
        Object typeValue = evaluateLiteral(args.get(0));
        if (!(typeValue instanceof String rewardType)) {
            LOGGER.warn("Reward type must be a string");
            return;
        }
        
        switch (rewardType.toLowerCase()) {
            case "experience", "xp" -> {
                if (args.size() > 1) {
                    Object amount = evaluateLiteral(args.get(1));
                    if (amount instanceof Number n) {
                        builder.reward("experience", n.intValue());
                    }
                }
            }
            case "item" -> {
                if (args.size() > 1) {
                    Object itemId = evaluateLiteral(args.get(1));
                    int count = 1;
                    if (args.size() > 2) {
                        Object countVal = evaluateLiteral(args.get(2));
                        if (countVal instanceof Number n) {
                            count = n.intValue();
                        }
                    }
                    builder.reward("item:" + itemId, count);
                }
            }
            case "money", "currency" -> {
                if (args.size() > 1) {
                    Object amount = evaluateLiteral(args.get(1));
                    if (amount instanceof Number n) {
                        builder.reward("money", n.intValue());
                    }
                }
            }
            case "command" -> {
                if (args.size() > 1) {
                    Object command = evaluateLiteral(args.get(1));
                    if (command instanceof String s) {
                        builder.reward("command", s);
                    }
                }
            }
            default -> {
                // Custom reward type
                if (args.size() > 1) {
                    Object value = evaluateLiteral(args.get(1));
                    builder.reward(rewardType, value);
                }
            }
        }
    }
    
    /**
     * Parses a prerequisite function call.
     */
    private void parsePrerequisite(List<ExpressionNode> args, Quest.Builder builder) {
        for (ExpressionNode arg : args) {
            Object value = evaluateLiteral(arg);
            if (value instanceof String s) {
                builder.prerequisite(s);
            }
        }
    }
    
    /**
     * Evaluates a literal expression to its value.
     */
    private Object evaluateLiteral(ExpressionNode expr) {
        if (expr instanceof LiteralNode literal) {
            return literal.getValue();
        } else if (expr instanceof IdentifierNode identifier) {
            return identifier.getName();
        }
        return null;
    }
    
    /**
     * Gets the function name from a FunctionCallNode.
     */
    private String getFunctionName(FunctionCallNode funcCall) {
        ExpressionNode callee = funcCall.getCallee();
        if (callee instanceof IdentifierNode identifier) {
            return identifier.getName();
        }
        return null;
    }
}
