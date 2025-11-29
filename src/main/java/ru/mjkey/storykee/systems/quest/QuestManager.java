package ru.mjkey.storykee.systems.quest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.statement.QuestDeclarationNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages quests and objectives in the story system.
 * Handles quest registration, lookup, starting, completion, and objective tracking.
 * Requirements: 8.1, 8.4
 */
public class QuestManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestManager.class);
    
    private static QuestManager instance;
    
    // Registered quests by ID
    private final Map<String, Quest> quests;
    
    // Player quest progress (playerId -> questId -> progress)
    private final Map<UUID, Map<String, QuestProgress>> playerProgress;
    
    // Quest parser for AST conversion
    private final QuestParser questParser;
    
    // Quest tracker for objective detection
    private QuestTracker questTracker;
    
    // Event listeners
    private final List<QuestEventListener> eventListeners;
    
    private QuestManager() {
        this.quests = new ConcurrentHashMap<>();
        this.playerProgress = new ConcurrentHashMap<>();
        this.questParser = new QuestParser();
        this.eventListeners = new ArrayList<>();
    }
    
    public static QuestManager getInstance() {
        if (instance == null) {
            instance = new QuestManager();
        }
        return instance;
    }
    
    /**
     * Sets the quest tracker for objective completion detection.
     */
    public void setQuestTracker(QuestTracker tracker) {
        this.questTracker = tracker;
    }
    
    public QuestTracker getQuestTracker() {
        return questTracker;
    }
    
    // ===== Quest Registration =====
    
    /**
     * Registers a quest from an AST node.
     */
    public void registerQuest(QuestDeclarationNode node, String scriptId) {
        Quest quest = questParser.parseQuest(node, scriptId);
        registerQuest(quest);
    }
    
    /**
     * Registers a quest directly.
     */
    public void registerQuest(Quest quest) {
        if (quest == null || quest.getId() == null) {
            LOGGER.warn("Cannot register null quest or quest with null ID");
            return;
        }
        
        // Validate quest structure
        List<String> errors = quest.validate();
        if (!errors.isEmpty()) {
            LOGGER.warn("Quest '{}' has validation errors: {}", quest.getId(), errors);
        }
        
        quests.put(quest.getId(), quest);
        LOGGER.info("Registered quest: {}", quest.getId());
    }
    
    /**
     * Unregisters a quest by ID.
     */
    public void unregisterQuest(String questId) {
        Quest removed = quests.remove(questId);
        if (removed != null) {
            LOGGER.info("Unregistered quest: {}", questId);
        }
    }
    
    /**
     * Unregisters all quests from a specific script.
     */
    public void unregisterQuestsFromScript(String scriptId) {
        quests.entrySet().removeIf(entry -> {
            if (scriptId.equals(entry.getValue().getScriptId())) {
                LOGGER.debug("Unregistering quest {} from script {}", entry.getKey(), scriptId);
                return true;
            }
            return false;
        });
    }
    
    // ===== Quest Lookup =====
    
    /**
     * Gets a quest by ID.
     */
    public Quest getQuest(String questId) {
        return quests.get(questId);
    }
    
    /**
     * Checks if a quest exists.
     */
    public boolean hasQuest(String questId) {
        return quests.containsKey(questId);
    }
    
    /**
     * Gets all registered quest IDs.
     */
    public Set<String> getQuestIds() {
        return Collections.unmodifiableSet(quests.keySet());
    }
    
    /**
     * Gets all registered quests.
     */
    public Collection<Quest> getAllQuests() {
        return Collections.unmodifiableCollection(quests.values());
    }
    
    // ===== Quest Starting =====
    
    /**
     * Starts a quest for a player.
     * @param playerId The player's UUID
     * @param questId The quest ID to start
     * @return true if quest was started successfully
     */
    public boolean startQuest(UUID playerId, String questId) {
        if (playerId == null) {
            LOGGER.warn("Cannot start quest for null player");
            return false;
        }
        
        Quest quest = quests.get(questId);
        if (quest == null) {
            LOGGER.warn("Quest '{}' not found", questId);
            return false;
        }
        
        // Check prerequisites
        if (!checkPrerequisites(playerId, quest)) {
            LOGGER.debug("Player {} does not meet prerequisites for quest {}", playerId, questId);
            return false;
        }
        
        // Get or create player progress map
        Map<String, QuestProgress> playerQuests = playerProgress.computeIfAbsent(
            playerId, k -> new ConcurrentHashMap<>()
        );
        
        // Check if quest can be started
        QuestProgress existing = playerQuests.get(questId);
        if (existing != null && !existing.canStart(quest)) {
            LOGGER.debug("Player {} cannot start quest {} (status: {})", 
                playerId, questId, existing.getStatus());
            return false;
        }
        
        // Create or reset progress
        QuestProgress progress;
        if (existing != null && quest.isRepeatable()) {
            existing.reset();
            progress = existing;
        } else {
            progress = new QuestProgress(questId, playerId);
        }
        
        progress.setStatus(QuestStatus.IN_PROGRESS);
        progress.setStartTime(System.currentTimeMillis());
        playerQuests.put(questId, progress);
        
        LOGGER.info("Started quest '{}' for player {}", questId, playerId);
        
        // Notify listeners
        fireQuestStarted(playerId, quest, progress);
        
        return true;
    }
    
    /**
     * Checks if a player meets the prerequisites for a quest.
     */
    public boolean checkPrerequisites(UUID playerId, Quest quest) {
        if (!quest.hasPrerequisites()) {
            return true;
        }
        
        Map<String, QuestProgress> playerQuests = playerProgress.get(playerId);
        if (playerQuests == null) {
            return false;
        }
        
        for (String prereqId : quest.getPrerequisites()) {
            QuestProgress prereqProgress = playerQuests.get(prereqId);
            if (prereqProgress == null || !prereqProgress.isCompleted()) {
                return false;
            }
        }
        
        return true;
    }
    
    // ===== Quest Completion =====
    
    /**
     * Completes a quest for a player.
     * @param playerId The player's UUID
     * @param questId The quest ID to complete
     * @return true if quest was completed successfully
     */
    public boolean completeQuest(UUID playerId, String questId) {
        if (playerId == null) {
            return false;
        }
        
        Quest quest = quests.get(questId);
        if (quest == null) {
            LOGGER.warn("Quest '{}' not found", questId);
            return false;
        }
        
        QuestProgress progress = getQuestProgress(playerId, questId);
        if (progress == null || !progress.isActive()) {
            LOGGER.debug("Quest '{}' is not active for player {}", questId, playerId);
            return false;
        }
        
        // Mark as completed
        progress.setStatus(QuestStatus.COMPLETED);
        progress.setCompleteTime(System.currentTimeMillis());
        progress.incrementCompletionCount();
        
        LOGGER.info("Completed quest '{}' for player {}", questId, playerId);
        
        // Notify listeners (rewards are granted by listeners)
        fireQuestCompleted(playerId, quest, progress);
        
        return true;
    }
    
    /**
     * Fails a quest for a player.
     */
    public boolean failQuest(UUID playerId, String questId) {
        if (playerId == null) {
            return false;
        }
        
        QuestProgress progress = getQuestProgress(playerId, questId);
        if (progress == null || !progress.isActive()) {
            return false;
        }
        
        progress.setStatus(QuestStatus.FAILED);
        progress.setCompleteTime(System.currentTimeMillis());
        
        Quest quest = quests.get(questId);
        LOGGER.info("Failed quest '{}' for player {}", questId, playerId);
        
        fireQuestFailed(playerId, quest, progress);
        
        return true;
    }
    
    /**
     * Abandons a quest for a player.
     */
    public boolean abandonQuest(UUID playerId, String questId) {
        if (playerId == null) {
            return false;
        }
        
        QuestProgress progress = getQuestProgress(playerId, questId);
        if (progress == null || !progress.isActive()) {
            return false;
        }
        
        progress.setStatus(QuestStatus.ABANDONED);
        
        Quest quest = quests.get(questId);
        LOGGER.info("Abandoned quest '{}' for player {}", questId, playerId);
        
        fireQuestAbandoned(playerId, quest, progress);
        
        return true;
    }
    
    // ===== Objective Tracking =====
    
    /**
     * Updates progress on a quest objective.
     * @param playerId The player's UUID
     * @param questId The quest ID
     * @param objectiveId The objective ID
     * @param progress The new progress value
     * @return true if objective was updated
     */
    public boolean updateObjective(UUID playerId, String questId, String objectiveId, int progress) {
        if (playerId == null) {
            return false;
        }
        
        Quest quest = quests.get(questId);
        if (quest == null) {
            return false;
        }
        
        QuestObjective objective = quest.getObjective(objectiveId);
        if (objective == null) {
            LOGGER.warn("Objective '{}' not found in quest '{}'", objectiveId, questId);
            return false;
        }
        
        QuestProgress questProgress = getQuestProgress(playerId, questId);
        if (questProgress == null || !questProgress.isActive()) {
            return false;
        }
        
        int oldProgress = questProgress.getObjectiveProgress(objectiveId);
        questProgress.setObjectiveProgress(objectiveId, progress);
        
        LOGGER.debug("Updated objective '{}' in quest '{}' for player {}: {} -> {}", 
            objectiveId, questId, playerId, oldProgress, progress);
        
        // Check if objective is now complete
        if (progress >= objective.getTargetCount() && !questProgress.isObjectiveCompleted(objectiveId)) {
            completeObjective(playerId, questId, objectiveId);
        }
        
        // Notify listeners
        fireObjectiveUpdated(playerId, quest, objective, questProgress);
        
        return true;
    }
    
    /**
     * Increments progress on a quest objective.
     */
    public boolean incrementObjective(UUID playerId, String questId, String objectiveId) {
        return incrementObjective(playerId, questId, objectiveId, 1);
    }
    
    /**
     * Increments progress on a quest objective by a specified amount.
     */
    public boolean incrementObjective(UUID playerId, String questId, String objectiveId, int amount) {
        QuestProgress progress = getQuestProgress(playerId, questId);
        if (progress == null || !progress.isActive()) {
            return false;
        }
        
        int newProgress = progress.getObjectiveProgress(objectiveId) + amount;
        return updateObjective(playerId, questId, objectiveId, newProgress);
    }
    
    /**
     * Marks an objective as completed.
     */
    private void completeObjective(UUID playerId, String questId, String objectiveId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return;
        }
        
        QuestProgress progress = getQuestProgress(playerId, questId);
        if (progress == null) {
            return;
        }
        
        progress.markObjectiveCompleted(objectiveId);
        
        QuestObjective objective = quest.getObjective(objectiveId);
        LOGGER.info("Completed objective '{}' in quest '{}' for player {}", 
            objectiveId, questId, playerId);
        
        // Notify listeners
        fireObjectiveCompleted(playerId, quest, objective, progress);
        
        // Check if all required objectives are complete
        checkQuestCompletion(playerId, quest, progress);
    }
    
    /**
     * Checks if all required objectives are complete and auto-completes the quest.
     */
    private void checkQuestCompletion(UUID playerId, Quest quest, QuestProgress progress) {
        for (QuestObjective objective : quest.getObjectives()) {
            if (!objective.isOptional() && !progress.isObjectiveCompleted(objective.getId())) {
                return; // Not all required objectives complete
            }
        }
        
        // All required objectives complete - complete the quest
        completeQuest(playerId, quest.getId());
    }
    
    // ===== Progress Queries =====
    
    /**
     * Gets a player's progress on a specific quest.
     */
    public QuestProgress getQuestProgress(UUID playerId, String questId) {
        if (playerId == null) {
            return null;
        }
        
        Map<String, QuestProgress> playerQuests = playerProgress.get(playerId);
        if (playerQuests == null) {
            return null;
        }
        
        return playerQuests.get(questId);
    }
    
    /**
     * Gets all quest progress for a player.
     */
    public Map<String, QuestProgress> getPlayerProgress(UUID playerId) {
        if (playerId == null) {
            return Collections.emptyMap();
        }
        
        Map<String, QuestProgress> playerQuests = playerProgress.get(playerId);
        if (playerQuests == null) {
            return Collections.emptyMap();
        }
        
        return Collections.unmodifiableMap(playerQuests);
    }
    
    /**
     * Gets all active quests for a player.
     */
    public List<Quest> getActiveQuests(UUID playerId) {
        List<Quest> active = new ArrayList<>();
        
        Map<String, QuestProgress> playerQuests = playerProgress.get(playerId);
        if (playerQuests == null) {
            return active;
        }
        
        for (QuestProgress progress : playerQuests.values()) {
            if (progress.isActive()) {
                Quest quest = quests.get(progress.getQuestId());
                if (quest != null) {
                    active.add(quest);
                }
            }
        }
        
        return active;
    }
    
    /**
     * Gets all completed quests for a player.
     */
    public List<Quest> getCompletedQuests(UUID playerId) {
        List<Quest> completed = new ArrayList<>();
        
        Map<String, QuestProgress> playerQuests = playerProgress.get(playerId);
        if (playerQuests == null) {
            return completed;
        }
        
        for (QuestProgress progress : playerQuests.values()) {
            if (progress.isCompleted()) {
                Quest quest = quests.get(progress.getQuestId());
                if (quest != null) {
                    completed.add(quest);
                }
            }
        }
        
        return completed;
    }
    
    /**
     * Checks if a player has an active quest.
     */
    public boolean hasActiveQuest(UUID playerId, String questId) {
        QuestProgress progress = getQuestProgress(playerId, questId);
        return progress != null && progress.isActive();
    }
    
    /**
     * Checks if a player has completed a quest.
     */
    public boolean hasCompletedQuest(UUID playerId, String questId) {
        QuestProgress progress = getQuestProgress(playerId, questId);
        return progress != null && progress.isCompleted();
    }
    
    // ===== Persistence =====
    
    /**
     * Gets all progress data for a player (for saving).
     */
    public List<QuestProgress.QuestProgressData> getPlayerProgressData(UUID playerId) {
        List<QuestProgress.QuestProgressData> dataList = new ArrayList<>();
        
        Map<String, QuestProgress> playerQuests = playerProgress.get(playerId);
        if (playerQuests != null) {
            for (QuestProgress progress : playerQuests.values()) {
                dataList.add(progress.toData());
            }
        }
        
        return dataList;
    }
    
    /**
     * Loads progress data for a player.
     */
    public void loadPlayerProgressData(UUID playerId, List<QuestProgress.QuestProgressData> dataList) {
        if (playerId == null || dataList == null) {
            return;
        }
        
        Map<String, QuestProgress> playerQuests = playerProgress.computeIfAbsent(
            playerId, k -> new ConcurrentHashMap<>()
        );
        
        for (QuestProgress.QuestProgressData data : dataList) {
            try {
                QuestProgress progress = QuestProgress.fromData(data);
                playerQuests.put(progress.getQuestId(), progress);
            } catch (Exception e) {
                LOGGER.error("Failed to load quest progress for {}: {}", data.questId, e.getMessage());
            }
        }
        
        LOGGER.debug("Loaded {} quest progress entries for player {}", dataList.size(), playerId);
    }
    
    /**
     * Clears progress data for a player (on disconnect).
     */
    public void clearPlayerProgress(UUID playerId) {
        playerProgress.remove(playerId);
    }
    
    // ===== Event Listeners =====
    
    public void addEventListener(QuestEventListener listener) {
        eventListeners.add(listener);
    }
    
    public void removeEventListener(QuestEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void fireQuestStarted(UUID playerId, Quest quest, QuestProgress progress) {
        for (QuestEventListener listener : eventListeners) {
            try {
                listener.onQuestStarted(playerId, quest, progress);
            } catch (Exception e) {
                LOGGER.error("Error in quest event listener", e);
            }
        }
    }
    
    private void fireQuestCompleted(UUID playerId, Quest quest, QuestProgress progress) {
        for (QuestEventListener listener : eventListeners) {
            try {
                listener.onQuestCompleted(playerId, quest, progress);
            } catch (Exception e) {
                LOGGER.error("Error in quest event listener", e);
            }
        }
    }
    
    private void fireQuestFailed(UUID playerId, Quest quest, QuestProgress progress) {
        for (QuestEventListener listener : eventListeners) {
            try {
                listener.onQuestFailed(playerId, quest, progress);
            } catch (Exception e) {
                LOGGER.error("Error in quest event listener", e);
            }
        }
    }
    
    private void fireQuestAbandoned(UUID playerId, Quest quest, QuestProgress progress) {
        for (QuestEventListener listener : eventListeners) {
            try {
                listener.onQuestAbandoned(playerId, quest, progress);
            } catch (Exception e) {
                LOGGER.error("Error in quest event listener", e);
            }
        }
    }
    
    private void fireObjectiveUpdated(UUID playerId, Quest quest, QuestObjective objective, QuestProgress progress) {
        for (QuestEventListener listener : eventListeners) {
            try {
                listener.onObjectiveUpdated(playerId, quest, objective, progress);
            } catch (Exception e) {
                LOGGER.error("Error in quest event listener", e);
            }
        }
    }
    
    private void fireObjectiveCompleted(UUID playerId, Quest quest, QuestObjective objective, QuestProgress progress) {
        for (QuestEventListener listener : eventListeners) {
            try {
                listener.onObjectiveCompleted(playerId, quest, objective, progress);
            } catch (Exception e) {
                LOGGER.error("Error in quest event listener", e);
            }
        }
    }
    
    // ===== Cleanup =====
    
    /**
     * Clears all registered quests and progress.
     */
    public void clear() {
        quests.clear();
        playerProgress.clear();
        LOGGER.info("Cleared all quests");
    }
    
    /**
     * Interface for quest event listeners.
     */
    public interface QuestEventListener {
        default void onQuestStarted(UUID playerId, Quest quest, QuestProgress progress) {}
        default void onQuestCompleted(UUID playerId, Quest quest, QuestProgress progress) {}
        default void onQuestFailed(UUID playerId, Quest quest, QuestProgress progress) {}
        default void onQuestAbandoned(UUID playerId, Quest quest, QuestProgress progress) {}
        default void onObjectiveUpdated(UUID playerId, Quest quest, QuestObjective objective, QuestProgress progress) {}
        default void onObjectiveCompleted(UUID playerId, Quest quest, QuestObjective objective, QuestProgress progress) {}
    }
}
