package ru.mjkey.storykee.systems.boss;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Properties for configuring a StoryBoss entity.
 * Uses builder pattern for fluent configuration.
 * 
 * Requirements: 18.1 - Boss spawning with specified health, damage, and abilities
 */
public class BossProperties {
    
    private String name = "Boss";
    private double maxHealth = 100.0;
    private double attackDamage = 10.0;
    private double movementSpeed = 0.3;
    private double followRange = 32.0;
    private double knockbackResistance = 0.5;
    
    // Position
    private double x = 0;
    private double y = 0;
    private double z = 0;
    
    // Combat area restriction (Requirements: 18.6)
    private Vec3 combatAreaCenter;
    private double combatAreaRadius = 0; // 0 means no restriction
    
    // Phases
    private List<BossPhase> phases = new ArrayList<>();
    
    // Custom data
    private Map<String, Object> customData = new HashMap<>();
    
    // Boss bar settings
    private int bossBarColor = 0; // BossBar.Color ordinal
    private int bossBarStyle = 0; // BossBar.Overlay ordinal
    
    public BossProperties() {
    }
    
    // Builder methods
    
    public BossProperties name(String name) {
        this.name = name;
        return this;
    }
    
    public BossProperties maxHealth(double maxHealth) {
        this.maxHealth = Math.max(1.0, maxHealth);
        return this;
    }
    
    public BossProperties attackDamage(double attackDamage) {
        this.attackDamage = Math.max(0.0, attackDamage);
        return this;
    }
    
    public BossProperties movementSpeed(double movementSpeed) {
        this.movementSpeed = Math.max(0.0, movementSpeed);
        return this;
    }
    
    public BossProperties followRange(double followRange) {
        this.followRange = Math.max(1.0, followRange);
        return this;
    }
    
    public BossProperties knockbackResistance(double knockbackResistance) {
        this.knockbackResistance = Math.max(0.0, Math.min(1.0, knockbackResistance));
        return this;
    }
    
    public BossProperties position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    public BossProperties combatArea(double centerX, double centerY, double centerZ, double radius) {
        this.combatAreaCenter = new Vec3(centerX, centerY, centerZ);
        this.combatAreaRadius = Math.max(0.0, radius);
        return this;
    }
    
    public BossProperties addPhase(BossPhase phase) {
        this.phases.add(phase);
        return this;
    }
    
    public BossProperties customData(Map<String, Object> customData) {
        this.customData = new HashMap<>(customData);
        return this;
    }
    
    public BossProperties setCustomData(String key, Object value) {
        this.customData.put(key, value);
        return this;
    }
    
    public BossProperties bossBarColor(int color) {
        this.bossBarColor = color;
        return this;
    }
    
    public BossProperties bossBarStyle(int style) {
        this.bossBarStyle = style;
        return this;
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public double getMaxHealth() {
        return maxHealth;
    }
    
    public double getAttackDamage() {
        return attackDamage;
    }
    
    public double getMovementSpeed() {
        return movementSpeed;
    }
    
    public double getFollowRange() {
        return followRange;
    }
    
    public double getKnockbackResistance() {
        return knockbackResistance;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public double getZ() {
        return z;
    }
    
    public Vec3 getCombatAreaCenter() {
        return combatAreaCenter;
    }
    
    public double getCombatAreaRadius() {
        return combatAreaRadius;
    }
    
    public boolean hasCombatArea() {
        return combatAreaCenter != null && combatAreaRadius > 0;
    }
    
    public List<BossPhase> getPhases() {
        return new ArrayList<>(phases);
    }
    
    public Map<String, Object> getCustomData() {
        return new HashMap<>(customData);
    }
    
    public int getBossBarColor() {
        return bossBarColor;
    }
    
    public int getBossBarStyle() {
        return bossBarStyle;
    }
    
    @Override
    public String toString() {
        return "BossProperties{name='" + name + "', maxHealth=" + maxHealth + 
               ", attackDamage=" + attackDamage + ", phases=" + phases.size() + "}";
    }
}
