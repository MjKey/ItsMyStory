package ru.mjkey.storykee.systems.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tracks quest objective completion by monitoring game events.
 * Implements progress tracking per player and objective completion detection.
 * Requirements: 8.2, 8.3, 8.4
 */
public class QuestTracker {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestTracker.class);
    
    private final QuestManager questManager;
    
    public QuestTracker(QuestManager questManager) {
        this.questManager = questManager;
    }
    
    // ===== Entity Kill Tracking =====
    
    /**
     * Called when a player kills an entity.
     * Updates KILL_ENTITY objectives.
     */
    public void onEntityKilled(ServerPlayer player, Entity killedEntity) {
        if (player == null || killedEntity == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        String entityType = getEntityTypeId(killedEntity);
        String entityName = killedEntity.hasCustomName() ? 
            killedEntity.getCustomName().getString() : null;
        
        // Check all active quests for matching objectives
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.KILL_ENTITY) {
                    continue;
                }
                
                if (matchesKillCriteria(objective, entityType, entityName)) {
                    questManager.incrementObjective(playerId, quest.getId(), objective.getId());
                    LOGGER.debug("Player {} killed {} for quest {} objective {}", 
                        playerId, entityType, quest.getId(), objective.getId());
                }
            }
        }
    }
    
    /**
     * Checks if a killed entity matches the objective criteria.
     */
    private boolean matchesKillCriteria(QuestObjective objective, String entityType, String entityName) {
        // Check entity type
        Object requiredType = objective.getCriterion("entityType");
        if (requiredType != null && !requiredType.toString().equals(entityType)) {
            return false;
        }
        
        // Check entity name (optional)
        Object requiredName = objective.getCriterion("entityName");
        if (requiredName != null) {
            if (entityName == null || !requiredName.toString().equals(entityName)) {
                return false;
            }
        }
        
        return true;
    }
    
    // ===== Item Collection Tracking =====
    
    /**
     * Called when a player picks up an item.
     * Updates COLLECT_ITEM objectives.
     */
    public void onItemPickup(ServerPlayer player, ItemStack itemStack) {
        if (player == null || itemStack == null || itemStack.isEmpty()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        String itemId = getItemId(itemStack);
        int count = itemStack.getCount();
        
        // Check all active quests for matching objectives
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.COLLECT_ITEM) {
                    continue;
                }
                
                if (matchesItemCriteria(objective, itemId, itemStack)) {
                    questManager.incrementObjective(playerId, quest.getId(), objective.getId(), count);
                    LOGGER.debug("Player {} collected {} x{} for quest {} objective {}", 
                        playerId, itemId, count, quest.getId(), objective.getId());
                }
            }
        }
    }
    
    /**
     * Checks if an item matches the objective criteria.
     */
    private boolean matchesItemCriteria(QuestObjective objective, String itemId, ItemStack itemStack) {
        Object requiredItem = objective.getCriterion("itemId");
        if (requiredItem == null) {
            return false;
        }
        
        return requiredItem.toString().equals(itemId);
    }
    
    /**
     * Checks a player's inventory for collected items.
     * Useful for objectives that track total items rather than pickups.
     */
    public void checkInventoryForObjectives(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Count items in inventory
        Map<String, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String itemId = getItemId(stack);
                itemCounts.merge(itemId, stack.getCount(), Integer::sum);
            }
        }
        
        // Update objectives based on inventory contents
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.COLLECT_ITEM) {
                    continue;
                }
                
                Object requiredItem = objective.getCriterion("itemId");
                if (requiredItem != null) {
                    int count = itemCounts.getOrDefault(requiredItem.toString(), 0);
                    QuestProgress progress = questManager.getQuestProgress(playerId, quest.getId());
                    if (progress != null) {
                        int currentProgress = progress.getObjectiveProgress(objective.getId());
                        if (count > currentProgress) {
                            questManager.updateObjective(playerId, quest.getId(), objective.getId(), count);
                        }
                    }
                }
            }
        }
    }
    
    // ===== Location Tracking =====
    
    /**
     * Called when a player moves.
     * Updates REACH_LOCATION objectives.
     */
    public void onPlayerMove(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        String dimension = player.level().dimension().location().toString();
        
        // Check all active quests for matching objectives
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.REACH_LOCATION) {
                    continue;
                }
                
                QuestProgress progress = questManager.getQuestProgress(playerId, quest.getId());
                if (progress != null && progress.isObjectiveCompleted(objective.getId())) {
                    continue; // Already completed
                }
                
                if (matchesLocationCriteria(objective, x, y, z, dimension)) {
                    questManager.updateObjective(playerId, quest.getId(), objective.getId(), 1);
                    LOGGER.debug("Player {} reached location for quest {} objective {}", 
                        playerId, quest.getId(), objective.getId());
                }
            }
        }
    }
    
    /**
     * Checks if a position matches the objective criteria.
     */
    private boolean matchesLocationCriteria(QuestObjective objective, double x, double y, double z, String dimension) {
        // Check dimension (optional)
        Object requiredDimension = objective.getCriterion("dimension");
        if (requiredDimension != null && !requiredDimension.toString().equals(dimension)) {
            return false;
        }
        
        // Get target coordinates
        Object targetX = objective.getCriterion("x");
        Object targetY = objective.getCriterion("y");
        Object targetZ = objective.getCriterion("z");
        
        if (targetX == null || targetY == null || targetZ == null) {
            return false;
        }
        
        double tx = ((Number) targetX).doubleValue();
        double ty = ((Number) targetY).doubleValue();
        double tz = ((Number) targetZ).doubleValue();
        
        // Get radius (default 3 blocks)
        double radius = 3.0;
        Object radiusObj = objective.getCriterion("radius");
        if (radiusObj instanceof Number n) {
            radius = n.doubleValue();
        }
        
        // Check distance
        double distSq = (x - tx) * (x - tx) + (y - ty) * (y - ty) + (z - tz) * (z - tz);
        return distSq <= radius * radius;
    }
    
    // ===== NPC Interaction Tracking =====
    
    /**
     * Called when a player interacts with an NPC.
     * Updates INTERACT_WITH_NPC objectives.
     */
    public void onNPCInteract(ServerPlayer player, String npcId) {
        if (player == null || npcId == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Check all active quests for matching objectives
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.INTERACT_WITH_NPC) {
                    continue;
                }
                
                Object requiredNpc = objective.getCriterion("npcId");
                if (requiredNpc != null && requiredNpc.toString().equals(npcId)) {
                    questManager.updateObjective(playerId, quest.getId(), objective.getId(), 1);
                    LOGGER.debug("Player {} interacted with NPC {} for quest {} objective {}", 
                        playerId, npcId, quest.getId(), objective.getId());
                }
            }
        }
    }
    
    // ===== Block Interaction Tracking =====
    
    /**
     * Called when a player interacts with a block.
     * Updates INTERACT_WITH_BLOCK objectives.
     */
    public void onBlockInteract(ServerPlayer player, BlockPos pos, BlockState blockState) {
        if (player == null || pos == null || blockState == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        String blockType = getBlockId(blockState);
        
        // Check all active quests for matching objectives
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.INTERACT_WITH_BLOCK) {
                    continue;
                }
                
                if (matchesBlockCriteria(objective, blockType, pos)) {
                    questManager.incrementObjective(playerId, quest.getId(), objective.getId());
                    LOGGER.debug("Player {} interacted with block {} for quest {} objective {}", 
                        playerId, blockType, quest.getId(), objective.getId());
                }
            }
        }
    }
    
    /**
     * Checks if a block matches the objective criteria.
     */
    private boolean matchesBlockCriteria(QuestObjective objective, String blockType, BlockPos pos) {
        // Check block type
        Object requiredBlock = objective.getCriterion("blockType");
        if (requiredBlock != null && !requiredBlock.toString().equals(blockType)) {
            return false;
        }
        
        // Check specific position (optional)
        Object targetX = objective.getCriterion("x");
        Object targetY = objective.getCriterion("y");
        Object targetZ = objective.getCriterion("z");
        
        if (targetX != null && targetY != null && targetZ != null) {
            int tx = ((Number) targetX).intValue();
            int ty = ((Number) targetY).intValue();
            int tz = ((Number) targetZ).intValue();
            
            if (pos.getX() != tx || pos.getY() != ty || pos.getZ() != tz) {
                return false;
            }
        }
        
        return true;
    }
    
    // ===== Dialogue Completion Tracking =====
    
    /**
     * Called when a player completes a dialogue.
     * Updates COMPLETE_DIALOGUE objectives.
     */
    public void onDialogueComplete(ServerPlayer player, String dialogueId) {
        if (player == null || dialogueId == null) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Check all active quests for matching objectives
        for (Quest quest : questManager.getActiveQuests(playerId)) {
            for (QuestObjective objective : quest.getObjectives()) {
                if (objective.getType() != ObjectiveType.COMPLETE_DIALOGUE) {
                    continue;
                }
                
                Object requiredDialogue = objective.getCriterion("dialogueId");
                if (requiredDialogue != null && requiredDialogue.toString().equals(dialogueId)) {
                    questManager.updateObjective(playerId, quest.getId(), objective.getId(), 1);
                    LOGGER.debug("Player {} completed dialogue {} for quest {} objective {}", 
                        playerId, dialogueId, quest.getId(), objective.getId());
                }
            }
        }
    }
    
    // ===== Custom Objective Tracking =====
    
    /**
     * Updates a custom objective by ID.
     * Used for CUSTOM type objectives that are tracked by scripts.
     */
    public void updateCustomObjective(UUID playerId, String questId, String objectiveId, int progress) {
        questManager.updateObjective(playerId, questId, objectiveId, progress);
    }
    
    /**
     * Increments a custom objective by ID.
     */
    public void incrementCustomObjective(UUID playerId, String questId, String objectiveId) {
        questManager.incrementObjective(playerId, questId, objectiveId);
    }
    
    // ===== Utility Methods =====
    
    /**
     * Gets the registry ID for an entity type.
     */
    private String getEntityTypeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }
    
    /**
     * Gets the registry ID for an item.
     */
    private String getItemId(ItemStack itemStack) {
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
    }
    
    /**
     * Gets the registry ID for a block.
     */
    private String getBlockId(BlockState blockState) {
        return BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();
    }
}
