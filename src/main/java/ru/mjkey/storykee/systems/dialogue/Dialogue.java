package ru.mjkey.storykee.systems.dialogue;

import java.util.*;

/**
 * Represents a complete dialogue with multiple nodes and branching paths.
 * Dialogues are registered with the DialogueManager and can be triggered by scripts.
 */
public class Dialogue {
    
    private final String id;
    private String title;
    private final Map<String, DialogueNode> nodes;
    private String startNodeId;
    private String scriptId;
    private String defaultSpeaker;
    
    public Dialogue(String id) {
        this.id = id;
        this.nodes = new LinkedHashMap<>();
    }
    
    public Dialogue(String id, String title) {
        this.id = id;
        this.title = title;
        this.nodes = new LinkedHashMap<>();
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getScriptId() {
        return scriptId;
    }
    
    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }
    
    public String getDefaultSpeaker() {
        return defaultSpeaker;
    }
    
    public void setDefaultSpeaker(String defaultSpeaker) {
        this.defaultSpeaker = defaultSpeaker;
    }
    
    public String getStartNodeId() {
        return startNodeId;
    }
    
    public void setStartNodeId(String startNodeId) {
        this.startNodeId = startNodeId;
    }
    
    public DialogueNode getStartNode() {
        return nodes.get(startNodeId);
    }
    
    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }
    
    public void addNode(DialogueNode node) {
        nodes.put(node.getId(), node);
        // Node named "start" becomes the start node, otherwise first node added
        if ("start".equals(node.getId())) {
            startNodeId = node.getId();
        } else if (startNodeId == null) {
            startNodeId = node.getId();
        }
    }
    
    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        if (nodeId.equals(startNodeId)) {
            startNodeId = nodes.isEmpty() ? null : nodes.keySet().iterator().next();
        }
    }
    
    public Collection<DialogueNode> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public boolean hasNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }
    
    /**
     * Validates the dialogue structure.
     * @return List of validation errors, empty if valid
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        
        if (startNodeId == null) {
            errors.add("Dialogue has no start node");
        } else if (!nodes.containsKey(startNodeId)) {
            errors.add("Start node '" + startNodeId + "' does not exist");
        }
        
        // Check all node references
        for (DialogueNode node : nodes.values()) {
            if (node.getNextNodeId() != null && !nodes.containsKey(node.getNextNodeId())) {
                errors.add("Node '" + node.getId() + "' references non-existent next node '" + node.getNextNodeId() + "'");
            }
            
            for (DialogueChoice choice : node.getChoices()) {
                if (choice.getNextNodeId() != null && !nodes.containsKey(choice.getNextNodeId())) {
                    errors.add("Choice '" + choice.getId() + "' in node '" + node.getId() + 
                        "' references non-existent node '" + choice.getNextNodeId() + "'");
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Builder for creating Dialogue instances.
     */
    public static class Builder {
        private String id;
        private String title;
        private String startNodeId;
        private String scriptId;
        private final List<DialogueNode> nodes = new ArrayList<>();
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder startNodeId(String startNodeId) {
            this.startNodeId = startNodeId;
            return this;
        }
        
        public Builder scriptId(String scriptId) {
            this.scriptId = scriptId;
            return this;
        }
        
        public Builder addNode(DialogueNode node) {
            this.nodes.add(node);
            return this;
        }
        
        public Dialogue build() {
            Dialogue dialogue = new Dialogue(id, title);
            dialogue.setScriptId(scriptId);
            for (DialogueNode node : nodes) {
                dialogue.addNode(node);
            }
            if (startNodeId != null) {
                dialogue.setStartNodeId(startNodeId);
            }
            return dialogue;
        }
    }
}
