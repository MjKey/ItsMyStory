package ru.mjkey.storykee.systems.advancement;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a reward granted when an advancement is completed.
 * 
 * Requirements: 51.5
 */
public class AdvancementReward {
    
    public enum RewardType {
        EXPERIENCE,
        ITEM,
        COMMAND,
        SCRIPT,
        CUSTOM
    }
    
    private final RewardType type;
    private final String value;
    private final int amount;
    private final Consumer<UUID> customAction;
    
    private AdvancementReward(RewardType type, String value, int amount, Consumer<UUID> customAction) {
        this.type = type;
        this.value = value;
        this.amount = amount;
        this.customAction = customAction;
    }
    
    public RewardType getType() {
        return type;
    }
    
    public String getValue() {
        return value;
    }
    
    public int getAmount() {
        return amount;
    }
    
    public Consumer<UUID> getCustomAction() {
        return customAction;
    }
    
    /**
     * Creates an experience reward.
     */
    public static AdvancementReward experience(int amount) {
        return new AdvancementReward(RewardType.EXPERIENCE, null, amount, null);
    }
    
    /**
     * Creates an item reward.
     */
    public static AdvancementReward item(String itemId, int count) {
        return new AdvancementReward(RewardType.ITEM, itemId, count, null);
    }
    
    /**
     * Creates a command reward.
     */
    public static AdvancementReward command(String command) {
        return new AdvancementReward(RewardType.COMMAND, command, 0, null);
    }
    
    /**
     * Creates a script function reward.
     */
    public static AdvancementReward script(String functionName) {
        return new AdvancementReward(RewardType.SCRIPT, functionName, 0, null);
    }
    
    /**
     * Creates a custom action reward.
     */
    public static AdvancementReward custom(Consumer<UUID> action) {
        return new AdvancementReward(RewardType.CUSTOM, null, 0, action);
    }
    
    @Override
    public String toString() {
        return "AdvancementReward{type=" + type + ", value='" + value + "', amount=" + amount + "}";
    }
}
