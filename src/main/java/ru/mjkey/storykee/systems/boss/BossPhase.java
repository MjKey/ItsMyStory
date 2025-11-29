package ru.mjkey.storykee.systems.boss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a phase in a boss fight.
 * Each phase has a health threshold and a set of abilities.
 * 
 * Requirements: 18.4 - Phase transitions based on health thresholds
 */
public class BossPhase {
    
    private final String id;
    private final float healthThreshold; // 0.0 to 1.0, phase activates when health drops below this
    private final List<BossAbility> abilities;
    private final Map<String, Object> properties;
    
    public BossPhase(String id, float healthThreshold) {
        this.id = id;
        this.healthThreshold = Math.max(0.0f, Math.min(1.0f, healthThreshold));
        this.abilities = new ArrayList<>();
        this.properties = new HashMap<>();
    }
    
    /**
     * Adds an ability to this phase.
     */
    public BossPhase addAbility(BossAbility ability) {
        this.abilities.add(ability);
        return this;
    }
    
    /**
     * Sets a custom property for this phase.
     */
    public BossPhase setProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }
    
    /**
     * Checks if this phase should be active based on current health percentage.
     * 
     * @param healthPercent Current health as percentage (0.0 to 1.0)
     * @return true if this phase should be active
     */
    public boolean shouldActivate(float healthPercent) {
        return healthPercent <= healthThreshold;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public float getHealthThreshold() {
        return healthThreshold;
    }
    
    public List<BossAbility> getAbilities() {
        return new ArrayList<>(abilities);
    }
    
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    @Override
    public String toString() {
        return "BossPhase{id='" + id + "', healthThreshold=" + healthThreshold + 
               ", abilities=" + abilities.size() + "}";
    }
}
