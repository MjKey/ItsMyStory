package ru.mjkey.storykee.systems.advancement;

import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Represents a criterion that must be met to complete an advancement.
 * 
 * Requirements: 51.2
 */
public class AdvancementCriterion {
    
    private final String name;
    private final Predicate<CriterionContext> condition;
    
    public AdvancementCriterion(String name, Predicate<CriterionContext> condition) {
        this.name = name;
        this.condition = condition;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Checks if this criterion is met.
     * Requirement 51.2: WHEN advancement criteria are met THEN the Runtime SHALL grant it to the player
     */
    public boolean isMet(UUID playerId, Map<String, Object> context) {
        CriterionContext ctx = new CriterionContext(playerId, context);
        return condition != null && condition.test(ctx);
    }
    
    @Override
    public String toString() {
        return "AdvancementCriterion{name='" + name + "'}";
    }
}
