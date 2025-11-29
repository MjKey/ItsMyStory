package ru.mjkey.storykee.runtime.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.ASTNode;
import ru.mjkey.storykee.parser.ast.ASTVisitor;
import ru.mjkey.storykee.parser.ast.expression.*;
import ru.mjkey.storykee.parser.ast.statement.*;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.runtime.context.UndefinedVariableException;
import ru.mjkey.storykee.runtime.debug.Debugger;

import java.util.*;

/**
 * Interprets and executes Storykee AST nodes.
 * Implements the Visitor pattern to traverse and execute the AST.
 */
public class ASTInterpreter implements ASTVisitor<Object> {
    
    /**
     * Interface for callable script functions.
     */
    public interface ScriptCallable {
        Object call(List<Object> arguments, ASTInterpreter interpreter);
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ASTInterpreter.class);
    
    // Current execution context
    private ExecutionContext context;
    
    // Built-in functions registry
    private final BuiltinFunctions builtinFunctions;
    
    // Debugger reference
    private final Debugger debugger;
    
    // Return value handling
    private Object returnValue = null;
    private boolean hasReturned = false;
    
    public ASTInterpreter() {
        this.builtinFunctions = new BuiltinFunctions();
        this.debugger = Debugger.getInstance();
    }
    
    /**
     * Checks for debug breakpoints before executing a statement.
     */
    private void checkDebugBreakpoint(ASTNode node) {
        if (context != null && debugger.isDebugMode()) {
            debugger.shouldPause(context.getScriptId(), node, context);
        }
    }
    
    /**
     * Executes a program node with the given context.
     */
    public Object visitProgram(ProgramNode node, ExecutionContext context) {
        this.context = context;
        this.hasReturned = false;
        this.returnValue = null;
        
        return visitProgram(node);
    }
    
    @Override
    public Object visitProgram(ProgramNode node) {
        Object lastResult = null;
        
        for (StatementNode statement : node.getStatements()) {
            if (hasReturned) {
                break;
            }
            lastResult = statement.accept(this);
        }
        
        return hasReturned ? returnValue : lastResult;
    }
    
    // ===== Statement Visitors =====
    
    @Override
    public Object visitVariableDeclaration(VariableDeclarationNode node) {
        checkDebugBreakpoint(node);
        
        String name = node.getName();
        Object value = node.getValue().accept(this);
        
        context.defineVariable(name, value);
        LOGGER.debug("Defined variable: {} = {}", name, value);
        
        return null;
    }
    
    @Override
    public Object visitFunctionDeclaration(FunctionDeclarationNode node) {
        // Store function as a callable object in the context
        String name = node.getName();
        UserFunction function = new UserFunction(node, context);
        
        context.defineVariable(name, function);
        LOGGER.debug("Defined function: {}", name);
        
        return null;
    }
    
    @Override
    public Object visitNPCDeclaration(NPCDeclarationNode node) {
        // NPC declarations are handled by the NPC system
        // For now, just log that we encountered one
        LOGGER.debug("NPC declaration: {}", node.getName());
        
        // Execute the body to evaluate properties
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        
        // TODO: Call NPC system to create NPC (task 10)
        LOGGER.warn("NPC system not yet implemented - NPC {} will not be created", node.getName());
        
        return null;
    }
    
    @Override
    public Object visitDialogueDeclaration(DialogueDeclarationNode node) {
        // Dialogue declarations are handled by the dialogue system
        LOGGER.debug("Dialogue declaration: {}", node.getName());
        
        // Register the dialogue with the DialogueManager
        ru.mjkey.storykee.systems.dialogue.DialogueManager.getInstance()
            .registerDialogue(node, context.getScriptId());
        
        LOGGER.info("Registered dialogue: {}", node.getName());
        
        return null;
    }
    
    @Override
    public Object visitQuestDeclaration(QuestDeclarationNode node) {
        // Quest declarations are handled by the quest system
        LOGGER.debug("Quest declaration: {}", node.getName());
        
        // Register the quest with the QuestManager
        ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
            .registerQuest(node, context.getScriptId());
        
        LOGGER.info("Registered quest: {}", node.getName());
        
        return null;
    }
    
    @Override
    public Object visitEventHandler(EventHandlerNode node) {
        String eventType = node.getEventType();
        BlockNode body = node.getBody();
        
        LOGGER.debug("Registering event handler for: {}", eventType);
        
        // Capture the current context which contains all defined functions
        final ExecutionContext scriptContext = this.context;
        
        // Create a handler that executes the body when the event fires
        ru.mjkey.storykee.events.EventHandler handler = (eventData, handlerContext) -> {
            // Use the script context which has all the functions defined
            // Push a new scope for the event handler
            scriptContext.pushScope();
            try {
                // Inject event data as 'event' variable
                java.util.Map<String, Object> eventMap = new java.util.LinkedHashMap<>();
                eventMap.putAll(eventData.getProperties());
                scriptContext.defineVariable("event", eventMap);
                
                // Execute the handler body with a new interpreter instance using script context
                ASTInterpreter handlerInterpreter = new ASTInterpreter();
                handlerInterpreter.context = scriptContext;
                handlerInterpreter.hasReturned = false;
                handlerInterpreter.returnValue = null;
                
                body.accept(handlerInterpreter);
                
            } catch (Exception e) {
                LOGGER.error("Error executing event handler for {}: {}", eventType, e.getMessage(), e);
            } finally {
                scriptContext.popScope();
            }
        };
        
        // Register with event system
        ru.mjkey.storykee.events.EventManager.getInstance()
            .registerHandler(eventType, context.getScriptId(), handler);
        
        LOGGER.info("Registered event handler for '{}' in script '{}'", eventType, context.getScriptId());
        
        return null;
    }
    
    @Override
    public Object visitBlock(BlockNode node) {
        // Push a new scope for the block
        context.pushScope();
        
        try {
            Object lastResult = null;
            
            for (StatementNode statement : node.getStatements()) {
                if (hasReturned) {
                    break;
                }
                lastResult = statement.accept(this);
            }
            
            return lastResult;
            
        } finally {
            // Always pop the scope
            context.popScope();
        }
    }
    
    @Override
    public Object visitIfStatement(IfStatementNode node) {
        checkDebugBreakpoint(node);
        
        Object conditionValue = node.getCondition().accept(this);
        boolean condition = toBoolean(conditionValue);
        
        if (condition) {
            return node.getThenBlock().accept(this);
        } else if (node.hasElseBlock()) {
            return node.getElseBlock().accept(this);
        }
        
        return null;
    }
    
    @Override
    public Object visitForStatement(ForStatementNode node) {
        checkDebugBreakpoint(node);
        
        Object collectionValue = node.getCollection().accept(this);
        
        if (!(collectionValue instanceof List)) {
            throw new RuntimeException("For loop requires a collection, got: " + 
                (collectionValue != null ? collectionValue.getClass().getSimpleName() : "null"));
        }
        
        @SuppressWarnings("unchecked")
        List<Object> collection = (List<Object>) collectionValue;
        
        Object lastResult = null;
        
        for (Object item : collection) {
            if (hasReturned) {
                break;
            }
            
            // Create new scope for loop iteration
            context.pushScope();
            try {
                context.defineVariable(node.getVariable(), item);
                lastResult = node.getBody().accept(this);
            } finally {
                context.popScope();
            }
        }
        
        return lastResult;
    }
    
    @Override
    public Object visitForCStyleStatement(ForCStyleStatementNode node) {
        checkDebugBreakpoint(node);
        
        // Create scope for the loop
        context.pushScope();
        try {
            // Initialize
            if (node.getInitVariable() != null && node.getInitValue() != null) {
                Object initVal = node.getInitValue().accept(this);
                if (node.isDeclareVariable()) {
                    context.defineVariable(node.getInitVariable(), initVal);
                } else {
                    context.setVariable(node.getInitVariable(), initVal);
                }
            }
            
            Object lastResult = null;
            
            // Loop
            while (true) {
                if (hasReturned) break;
                
                // Check condition
                if (node.getCondition() != null) {
                    Object condVal = node.getCondition().accept(this);
                    if (!toBoolean(condVal)) break;
                }
                
                // Execute body
                lastResult = node.getBody().accept(this);
                
                // Update
                if (node.getUpdateVariable() != null && node.getUpdateValue() != null) {
                    Object updateVal = node.getUpdateValue().accept(this);
                    context.setVariable(node.getUpdateVariable(), updateVal);
                }
            }
            
            return lastResult;
        } finally {
            context.popScope();
        }
    }
    
    @Override
    public Object visitWhileStatement(WhileStatementNode node) {
        checkDebugBreakpoint(node);
        
        Object lastResult = null;
        
        while (true) {
            if (hasReturned) {
                break;
            }
            
            Object conditionValue = node.getCondition().accept(this);
            boolean condition = toBoolean(conditionValue);
            
            if (!condition) {
                break;
            }
            
            lastResult = node.getBody().accept(this);
        }
        
        return lastResult;
    }
    
    @Override
    public Object visitReturnStatement(ReturnStatementNode node) {
        checkDebugBreakpoint(node);
        
        if (node.hasValue()) {
            returnValue = node.getValue().accept(this);
        } else {
            returnValue = null;
        }
        
        hasReturned = true;
        return returnValue;
    }
    
    @Override
    public Object visitExpressionStatement(ExpressionStatementNode node) {
        checkDebugBreakpoint(node);
        return node.getExpression().accept(this);
    }
    
    @Override
    public Object visitPropertyStatement(PropertyStatementNode node) {
        checkDebugBreakpoint(node);
        // Property statements define properties in the current scope
        String name = node.getName();
        Object value = node.getValue().accept(this);
        context.defineVariable(name, value);
        LOGGER.debug("Defined property: {} = {}", name, value);
        return null;
    }
    
    @Override
    public Object visitNodeDeclaration(NodeDeclarationNode node) {
        checkDebugBreakpoint(node);
        // Node declarations are used inside dialogues
        LOGGER.debug("Node declaration: {}", node.getName());
        // The body contains property statements for the node
        if (node.getBody() != null) {
            node.getBody().accept(this);
        }
        return null;
    }
    
    @Override
    public Object visitJavaSection(JavaSectionNode node) {
        // Java sections are compiled and executed separately
        LOGGER.debug("Java section encountered in script: {}", context.getScriptId());
        
        try {
            // Get the Java compiler
            ru.mjkey.storykee.runtime.java.JavaSectionCompiler compiler = 
                ru.mjkey.storykee.runtime.java.JavaSectionCompiler.getInstance();
            
            // Compile the Java code
            ru.mjkey.storykee.runtime.java.CompiledJavaSection compiled = 
                compiler.compile(node.getJavaCode(), context);
            
            // Execute the compiled code
            Object result = compiled.execute(context);
            
            LOGGER.debug("Java section executed successfully");
            return result;
            
        } catch (ru.mjkey.storykee.runtime.java.CompilationException e) {
            LOGGER.error("Java section compilation failed in script {}: {}", 
                context.getScriptId(), e.getMessage());
            throw new RuntimeException("Java section compilation failed: " + e.getMessage(), e);
            
        } catch (Exception e) {
            LOGGER.error("Java section execution failed in script {}: {}", 
                context.getScriptId(), e.getMessage(), e);
            throw new RuntimeException("Java section execution failed: " + e.getMessage(), e);
        }
    }
    
    // ===== Expression Visitors =====
    
    @Override
    public Object visitBinaryExpression(BinaryExpressionNode node) {
        Object left = node.getLeft().accept(this);
        Object right = node.getRight().accept(this);
        String operator = node.getOperator();
        
        switch (operator) {
            // Arithmetic operators
            case "+":
                return add(left, right);
            case "-":
                return subtract(left, right);
            case "*":
                return multiply(left, right);
            case "/":
                return divide(left, right);
            
            // Comparison operators
            case "==":
                return equals(left, right);
            case "!=":
                return !equals(left, right);
            case "<":
                return lessThan(left, right);
            case ">":
                return greaterThan(left, right);
            case "<=":
                return lessOrEqual(left, right);
            case ">=":
                return greaterOrEqual(left, right);
            
            // Logical operators
            case "&&":
                return toBoolean(left) && toBoolean(right);
            case "||":
                return toBoolean(left) || toBoolean(right);
            
            default:
                throw new RuntimeException("Unknown binary operator: " + operator);
        }
    }
    
    @Override
    public Object visitUnaryExpression(UnaryExpressionNode node) {
        Object operand = node.getOperand().accept(this);
        String operator = node.getOperator();
        
        switch (operator) {
            case "!":
                return !toBoolean(operand);
            case "-":
                return negate(operand);
            default:
                throw new RuntimeException("Unknown unary operator: " + operator);
        }
    }
    
    @Override
    public Object visitFunctionCall(FunctionCallNode node) {
        // Evaluate the callee to get the function name
        ExpressionNode callee = node.getCallee();
        
        // For now, we only support ResourceLocation callees (direct function names)
        if (!(callee instanceof IdentifierNode)) {
            throw new RuntimeException("Only direct function calls are supported");
        }
        
        String functionName = ((IdentifierNode) callee).getName();
        
        List<Object> arguments = new ArrayList<>();
        for (ExpressionNode arg : node.getArguments()) {
            arguments.add(arg.accept(this));
        }
        
        // Check if it's a built-in function
        if (builtinFunctions.hasFunction(functionName)) {
            return builtinFunctions.call(functionName, arguments, context);
        }
        
        // Check if it's a user-defined function
        if (context.hasVariable(functionName)) {
            Object functionValue = context.getVariable(functionName);
            
            if (functionValue instanceof UserFunction) {
                UserFunction function = (UserFunction) functionValue;
                return function.call(arguments, this);
            }
        }
        
        throw new RuntimeException("Undefined function: " + functionName);
    }
    
    @Override
    public Object visitMemberAccess(MemberAccessNode node) {
        Object object = node.getObject().accept(this);
        String member = node.getMember();
        
        if (object == null) {
            throw new RuntimeException("Cannot access member '" + member + "' of null");
        }
        
        if (object instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) object;
            return map.get(member);
        }
        
        throw new RuntimeException("Member access not supported for type: " + 
            object.getClass().getSimpleName());
    }
    
    @Override
    public Object visitArrayAccess(ArrayAccessNode node) {
        Object array = node.getArray().accept(this);
        Object index = node.getIndex().accept(this);
        
        if (array == null) {
            throw new RuntimeException("Cannot access index of null");
        }
        
        if (!(array instanceof List)) {
            throw new RuntimeException("Array access requires a list, got: " + 
                array.getClass().getSimpleName());
        }
        
        if (!(index instanceof Number)) {
            throw new RuntimeException("Array index must be a number, got: " + 
                index.getClass().getSimpleName());
        }
        
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) array;
        int idx = ((Number) index).intValue();
        
        if (idx < 0 || idx >= list.size()) {
            throw new RuntimeException("Array index out of bounds: " + idx);
        }
        
        return list.get(idx);
    }
    
    @Override
    public Object visitAssignment(AssignmentNode node) {
        String target = node.getTarget();
        Object value = node.getValue().accept(this);
        
        context.setVariable(target, value);
        return value;
    }
    
    @Override
    public Object visitIdentifier(IdentifierNode node) {
        String name = node.getName();
        
        if (!context.hasVariable(name)) {
            throw new UndefinedVariableException("Undefined variable: " + name);
        }
        
        return context.getVariable(name);
    }
    
    @Override
    public Object visitLiteral(LiteralNode node) {
        return node.getValue();
    }
    
    @Override
    public Object visitArrayLiteral(ArrayLiteralNode node) {
        List<Object> array = new ArrayList<>();
        
        for (ExpressionNode element : node.getElements()) {
            array.add(element.accept(this));
        }
        
        return array;
    }
    
    @Override
    public Object visitObjectLiteral(ObjectLiteralNode node) {
        Map<String, Object> object = new LinkedHashMap<>();
        
        for (Map.Entry<String, ExpressionNode> entry : node.getProperties().entrySet()) {
            object.put(entry.getKey(), entry.getValue().accept(this));
        }
        
        return object;
    }
    
    // ===== Helper Methods =====
    
    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0.0;
        }
        if (value instanceof String) {
            return !((String) value).isEmpty();
        }
        if (value instanceof List) {
            return !((List<?>) value).isEmpty();
        }
        if (value instanceof Map) {
            return !((Map<?, ?>) value).isEmpty();
        }
        return true;
    }
    
    private Object add(Object left, Object right) {
        // String concatenation
        if (left instanceof String || right instanceof String) {
            return String.valueOf(left) + String.valueOf(right);
        }
        
        // Numeric addition
        if (left instanceof Number && right instanceof Number) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            
            // Return integer if both are integers
            if (left instanceof Integer && right instanceof Integer) {
                return (int) l + (int) r;
            }
            
            return l + r;
        }
        
        throw new RuntimeException("Cannot add " + left.getClass().getSimpleName() + 
            " and " + right.getClass().getSimpleName());
    }
    
    private Object subtract(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot subtract non-numeric values");
        }
        
        double l = ((Number) left).doubleValue();
        double r = ((Number) right).doubleValue();
        
        if (left instanceof Integer && right instanceof Integer) {
            return (int) l - (int) r;
        }
        
        return l - r;
    }
    
    private Object multiply(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot multiply non-numeric values");
        }
        
        double l = ((Number) left).doubleValue();
        double r = ((Number) right).doubleValue();
        
        if (left instanceof Integer && right instanceof Integer) {
            return (int) l * (int) r;
        }
        
        return l * r;
    }
    
    private Object divide(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot divide non-numeric values");
        }
        
        double l = ((Number) left).doubleValue();
        double r = ((Number) right).doubleValue();
        
        if (r == 0.0) {
            throw new RuntimeException("Division by zero");
        }
        
        return l / r;
    }
    
    private Object negate(Object operand) {
        if (!(operand instanceof Number)) {
            throw new RuntimeException("Cannot negate non-numeric value");
        }
        
        double value = ((Number) operand).doubleValue();
        
        if (operand instanceof Integer) {
            return -(int) value;
        }
        
        return -value;
    }
    
    private boolean equals(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        
        // Numeric comparison
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() == ((Number) right).doubleValue();
        }
        
        return left.equals(right);
    }
    
    private boolean lessThan(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot compare non-numeric values");
        }
        
        return ((Number) left).doubleValue() < ((Number) right).doubleValue();
    }
    
    private boolean greaterThan(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot compare non-numeric values");
        }
        
        return ((Number) left).doubleValue() > ((Number) right).doubleValue();
    }
    
    private boolean lessOrEqual(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot compare non-numeric values");
        }
        
        return ((Number) left).doubleValue() <= ((Number) right).doubleValue();
    }
    
    private boolean greaterOrEqual(Object left, Object right) {
        if (!(left instanceof Number && right instanceof Number)) {
            throw new RuntimeException("Cannot compare non-numeric values");
        }
        
        return ((Number) left).doubleValue() >= ((Number) right).doubleValue();
    }
    
    /**
     * Represents a user-defined function.
     * Made public static so it can be called from BuiltinFunctions.
     */
    public static class UserFunction implements ScriptCallable {
        private final FunctionDeclarationNode declaration;
        private final ExecutionContext definitionContext;
        
        public UserFunction(FunctionDeclarationNode declaration, ExecutionContext definitionContext) {
            this.declaration = declaration;
            this.definitionContext = definitionContext;
        }
        
        public String getName() {
            return declaration.getName();
        }
        
        public ExecutionContext getContext() {
            return definitionContext;
        }
        
        @Override
        public Object call(List<Object> arguments, ASTInterpreter interpreter) {
            // Use the definition context if interpreter has no context
            ExecutionContext execContext = interpreter.context != null ? interpreter.context : definitionContext;
            ExecutionContext oldContext = interpreter.context;
            interpreter.context = execContext;
            
            // Create new scope for function execution
            execContext.pushScope();
            
            try {
                // Bind parameters
                List<String> params = declaration.getParameters();
                for (int i = 0; i < params.size(); i++) {
                    Object value = i < arguments.size() ? arguments.get(i) : null;
                    execContext.defineVariable(params.get(i), value);
                }
                
                // Execute function body
                boolean oldHasReturned = interpreter.hasReturned;
                Object oldReturnValue = interpreter.returnValue;
                interpreter.hasReturned = false;
                interpreter.returnValue = null;
                
                declaration.getBody().accept(interpreter);
                
                Object result = interpreter.returnValue;
                
                // Restore return state
                interpreter.hasReturned = oldHasReturned;
                interpreter.returnValue = oldReturnValue;
                
                return result;
                
            } finally {
                execContext.popScope();
                interpreter.context = oldContext;
            }
        }
    }
}
