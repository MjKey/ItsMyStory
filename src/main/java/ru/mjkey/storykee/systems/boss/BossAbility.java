package ru.mjkey.storykee.systems.boss;

import java.util.function.Consumer;

/**
 * Represents an ability that a boss can use during combat.
 * 
 * Requirements: 18.3 - Boss abilities execute associated script actions
 */
public class BossAbility {
    
    private final String id;
    private final String name;
    private final int cooldownTicks;
    private final Consumer<StoryBoss> execute;
    
    private int currentCooldown = 0;
    
    public BossAbility(String id, String name, int cooldownTicks, Consumer<StoryBoss> execute) {
        this.id = id;
        this.name = name;
        this.cooldownTicks = cooldownTicks;
        this.execute = execute;
    }
    
    /**
     * Attempts to use this ability.
     * 
     * @param boss The boss using the ability
     * @return true if the ability was used, false if on cooldown
     */
    public boolean tryUse(StoryBoss boss) {
        if (currentCooldown > 0) {
            return false;
        }
        
        if (execute != null) {
            execute.accept(boss);
        }
        
        currentCooldown = cooldownTicks;
        return true;
    }
    
    /**
     * Updates the cooldown timer.
     * Should be called every tick.
     */
    public void tick() {
        if (currentCooldown > 0) {
            currentCooldown--;
        }
    }
    
    /**
     * Checks if this ability is ready to use.
     */
    public boolean isReady() {
        return currentCooldown <= 0;
    }
    
    /**
     * Resets the cooldown to zero.
     */
    public void resetCooldown() {
        this.currentCooldown = 0;
    }
    
    /**
     * Forces the ability on cooldown.
     */
    public void startCooldown() {
        this.currentCooldown = cooldownTicks;
    }
    
    // Getters
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    public int getCurrentCooldown() {
        return currentCooldown;
    }
    
    public Consumer<StoryBoss> getExecute() {
        return execute;
    }
    
    @Override
    public String toString() {
        return "BossAbility{id='" + id + "', name='" + name + "', cooldown=" + cooldownTicks + "}";
    }
}
