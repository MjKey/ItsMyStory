package ru.mjkey.storykee.systems.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.events.EventData;
import ru.mjkey.storykee.events.EventManager;

import java.util.*;

/**
 * Custom boss entity for the Storykee story system.
 * Extends Monster to support combat behaviors and boss mechanics.
 * 
 * Requirements: 18.1, 18.3, 18.4, 18.6
 */
public class StoryBoss extends Monster {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryBoss.class);
    
    private String bossId;
    private final ServerBossEvent bossEvent;
    private final List<BossPhase> phases = new ArrayList<>();
    private BossPhase currentPhase;
    private int currentPhaseIndex = -1;
    private Map<String, Object> customData = new HashMap<>();
    
    // Combat area restriction (Requirements: 18.6)
    private Vec3 combatAreaCenter;
    private double combatAreaRadius = 0;
    
    // Ability tick counter
    private int abilityTickCounter = 0;
    private static final int ABILITY_CHECK_INTERVAL = 20; // Check abilities every second
    
    public StoryBoss(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.bossId = UUID.randomUUID().toString();
        
        // Initialize boss bar
        this.bossEvent = new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
        );
        
        this.setHealth(this.getMaxHealth());
    }
    
    /**
     * Creates attribute supplier for StoryBoss entities.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 4.0);
    }
    
    @Override
    protected void registerGoals() {
        // Combat AI goals
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        
        // Target selection
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    
    /**
     * Applies properties to this boss.
     * Requirements: 18.1 - Spawn boss with specified health, damage, and abilities
     */
    public void applyProperties(BossProperties properties) {
        if (properties == null) {
            return;
        }
        
        // Set display name
        this.setCustomName(Component.literal(properties.getName()));
        this.setCustomNameVisible(true);
        
        // Update boss bar name
        this.bossEvent.setName(this.getDisplayName());
        
        // Set position
        this.setPos(properties.getX(), properties.getY(), properties.getZ());
        
        // Set combat area (Requirements: 18.6)
        if (properties.hasCombatArea()) {
            this.combatAreaCenter = properties.getCombatAreaCenter();
            this.combatAreaRadius = properties.getCombatAreaRadius();
        }
        
        // Set boss bar color and style
        this.bossEvent.setColor(BossEvent.BossBarColor.values()[
            Math.min(properties.getBossBarColor(), BossEvent.BossBarColor.values().length - 1)
        ]);
        this.bossEvent.setOverlay(BossEvent.BossBarOverlay.values()[
            Math.min(properties.getBossBarStyle(), BossEvent.BossBarOverlay.values().length - 1)
        ]);
        
        // Add phases
        this.phases.clear();
        this.phases.addAll(properties.getPhases());
        
        // Sort phases by health threshold (highest first)
        this.phases.sort((a, b) -> Float.compare(b.getHealthThreshold(), a.getHealthThreshold()));
        
        // Store custom data
        this.customData = new HashMap<>(properties.getCustomData());
        
        // Initialize first phase if available
        if (!this.phases.isEmpty()) {
            checkPhaseTransition();
        }
        
        LOGGER.debug("Applied properties to boss {}: {}", bossId, properties);
    }

    
    @Override
    public void tick() {
        super.tick();
        
        // Update boss bar progress
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        
        // Check for phase transitions (Requirements: 18.4)
        checkPhaseTransition();
        
        // Tick abilities
        tickAbilities();
        
        // Enforce combat area restriction (Requirements: 18.6)
        enforceCombatArea();
    }
    
    /**
     * Checks and handles phase transitions based on health.
     * Requirements: 18.4 - Trigger phase transition events
     */
    private void checkPhaseTransition() {
        if (phases.isEmpty()) {
            return;
        }
        
        float healthPercent = this.getHealth() / this.getMaxHealth();
        
        // Find the appropriate phase for current health
        for (int i = 0; i < phases.size(); i++) {
            BossPhase phase = phases.get(i);
            if (phase.shouldActivate(healthPercent)) {
                if (currentPhaseIndex != i) {
                    transitionToPhase(i);
                }
                break;
            }
        }
    }
    
    /**
     * Transitions to a new phase.
     */
    private void transitionToPhase(int phaseIndex) {
        BossPhase oldPhase = currentPhase;
        currentPhaseIndex = phaseIndex;
        currentPhase = phases.get(phaseIndex);
        
        LOGGER.debug("Boss {} transitioning to phase {}", bossId, currentPhase.getId());
        
        // Fire phase transition event
        EventData eventData = new EventData("onBossPhaseChange");
        eventData.set("boss", this.bossId);
        eventData.set("bossName", this.getCustomName() != null ? this.getCustomName().getString() : "Boss");
        eventData.set("oldPhase", oldPhase != null ? oldPhase.getId() : null);
        eventData.set("newPhase", currentPhase.getId());
        eventData.set("phaseIndex", phaseIndex);
        eventData.set("healthPercent", this.getHealth() / this.getMaxHealth());
        
        EventManager.getInstance().fireEvent("onBossPhaseChange", eventData);
        
        // Reset ability cooldowns for new phase
        for (BossAbility ability : currentPhase.getAbilities()) {
            ability.resetCooldown();
        }
    }
    
    /**
     * Ticks abilities and attempts to use them.
     * Requirements: 18.3 - Boss uses abilities
     */
    private void tickAbilities() {
        if (currentPhase == null) {
            return;
        }
        
        // Tick all ability cooldowns
        for (BossAbility ability : currentPhase.getAbilities()) {
            ability.tick();
        }
        
        // Check abilities periodically
        abilityTickCounter++;
        if (abilityTickCounter >= ABILITY_CHECK_INTERVAL) {
            abilityTickCounter = 0;
            
            // Try to use a ready ability
            if (this.getTarget() != null) {
                for (BossAbility ability : currentPhase.getAbilities()) {
                    if (ability.isReady()) {
                        if (ability.tryUse(this)) {
                            // Fire ability use event
                            EventData eventData = new EventData("onBossAbility");
                            eventData.set("boss", this.bossId);
                            eventData.set("bossName", this.getCustomName() != null ? this.getCustomName().getString() : "Boss");
                            eventData.set("ability", ability.getId());
                            eventData.set("abilityName", ability.getName());
                            eventData.set("target", this.getTarget().getUUID());
                            
                            EventManager.getInstance().fireEvent("onBossAbility", eventData);
                            
                            LOGGER.debug("Boss {} used ability {}", bossId, ability.getId());
                            break; // Only use one ability per check
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Enforces combat area restriction.
     * Requirements: 18.6 - Prevent boss from leaving designated combat area
     */
    private void enforceCombatArea() {
        if (combatAreaCenter == null || combatAreaRadius <= 0) {
            return;
        }
        
        Vec3 currentPos = this.position();
        double distance = currentPos.distanceTo(combatAreaCenter);
        
        if (distance > combatAreaRadius) {
            // Calculate direction back to center
            Vec3 direction = combatAreaCenter.subtract(currentPos).normalize();
            
            // Move boss back towards center
            double pullStrength = Math.min(0.5, (distance - combatAreaRadius) * 0.1);
            Vec3 newVelocity = this.getDeltaMovement().add(direction.scale(pullStrength));
            this.setDeltaMovement(newVelocity);
            
            // If too far out, teleport back
            if (distance > combatAreaRadius * 1.5) {
                Vec3 safePos = combatAreaCenter.add(direction.scale(-combatAreaRadius * 0.9));
                this.teleportTo(safePos.x, safePos.y, safePos.z);
                LOGGER.debug("Boss {} teleported back to combat area", bossId);
            }
        }
    }
    
    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        super.actuallyHurt(level, source, amount);
        
        // Fire boss damaged event
        EventData eventData = new EventData("onBossDamaged");
        eventData.set("boss", this.bossId);
        eventData.set("bossName", this.getCustomName() != null ? this.getCustomName().getString() : "Boss");
        eventData.set("damage", amount);
        eventData.set("health", this.getHealth());
        eventData.set("maxHealth", this.getMaxHealth());
        eventData.set("healthPercent", this.getHealth() / this.getMaxHealth());
        
        if (source.getEntity() instanceof Player player) {
            eventData.set("attacker", player.getUUID());
            eventData.set("attackerName", player.getName().getString());
        }
        
        EventManager.getInstance().fireEvent("onBossDamaged", eventData);
    }
    
    @Override
    public void die(DamageSource source) {
        // Fire boss defeated event before death (Requirements: 18.5)
        if (!this.level().isClientSide()) {
            EventData eventData = new EventData("onBossDefeated");
            eventData.set("boss", this.bossId);
            eventData.set("bossName", this.getCustomName() != null ? this.getCustomName().getString() : "Boss");
            eventData.set("x", this.getX());
            eventData.set("y", this.getY());
            eventData.set("z", this.getZ());
            
            if (source.getEntity() instanceof Player player) {
                eventData.set("killer", player.getUUID());
                eventData.set("killerName", player.getName().getString());
            }
            
            // Add all players who participated
            List<UUID> participants = new ArrayList<>();
            for (ServerPlayer player : bossEvent.getPlayers()) {
                participants.add(player.getUUID());
            }
            eventData.set("participants", participants);
            
            EventManager.getInstance().fireEvent("onBossDefeated", eventData);
            
            LOGGER.info("Boss {} defeated", bossId);
        }
        
        super.die(source);
    }
    
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }
    
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }
    
    @Override
    public void remove(RemovalReason reason) {
        LOGGER.debug("Removing boss {} with reason {}", bossId, reason);
        
        // Remove boss bar from all players
        this.bossEvent.removeAllPlayers();
        
        // Fire boss removal event
        EventData eventData = new EventData("onBossRemove");
        eventData.set("boss", this.bossId);
        eventData.set("bossName", this.getCustomName() != null ? this.getCustomName().getString() : "Boss");
        eventData.set("reason", reason.name());
        
        EventManager.getInstance().fireEvent("onBossRemove", eventData);
        
        super.remove(reason);
    }
    
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Story bosses should not despawn naturally
        return false;
    }
    
    @Override
    public boolean isPersistenceRequired() {
        return true;
    }
    
    // Getters and setters
    
    public String getBossId() {
        return bossId;
    }
    
    public void setBossId(String bossId) {
        this.bossId = bossId;
    }
    
    public ServerBossEvent getBossEvent() {
        return bossEvent;
    }
    
    public List<BossPhase> getPhases() {
        return new ArrayList<>(phases);
    }
    
    public BossPhase getCurrentPhase() {
        return currentPhase;
    }
    
    public int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }
    
    public Map<String, Object> getCustomData() {
        return new HashMap<>(customData);
    }
    
    public Object getCustomData(String key) {
        return customData.get(key);
    }
    
    public void setCustomData(String key, Object value) {
        this.customData.put(key, value);
    }
    
    public Vec3 getCombatAreaCenter() {
        return combatAreaCenter;
    }
    
    public void setCombatAreaCenter(Vec3 center) {
        this.combatAreaCenter = center;
    }
    
    public double getCombatAreaRadius() {
        return combatAreaRadius;
    }
    
    public void setCombatAreaRadius(double radius) {
        this.combatAreaRadius = Math.max(0, radius);
    }
    
    /**
     * Sets the combat area for this boss.
     * Requirements: 18.6
     */
    public void setCombatArea(double centerX, double centerY, double centerZ, double radius) {
        this.combatAreaCenter = new Vec3(centerX, centerY, centerZ);
        this.combatAreaRadius = Math.max(0, radius);
    }
    
    /**
     * Clears the combat area restriction.
     */
    public void clearCombatArea() {
        this.combatAreaCenter = null;
        this.combatAreaRadius = 0;
    }
    
    /**
     * Adds a phase to this boss.
     */
    public void addPhase(BossPhase phase) {
        this.phases.add(phase);
        // Re-sort phases by health threshold
        this.phases.sort((a, b) -> Float.compare(b.getHealthThreshold(), a.getHealthThreshold()));
    }
    
    /**
     * Gets the current properties of this boss.
     */
    public BossProperties getCurrentProperties() {
        BossProperties props = new BossProperties();
        
        String name = this.getCustomName() != null ? this.getCustomName().getString() : "Boss";
        props.name(name)
             .maxHealth(this.getMaxHealth())
             .attackDamage(this.getAttributeValue(Attributes.ATTACK_DAMAGE))
             .movementSpeed(this.getAttributeValue(Attributes.MOVEMENT_SPEED))
             .followRange(this.getAttributeValue(Attributes.FOLLOW_RANGE))
             .knockbackResistance(this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE))
             .position(this.getX(), this.getY(), this.getZ())
             .customData(this.customData);
        
        if (combatAreaCenter != null && combatAreaRadius > 0) {
            props.combatArea(combatAreaCenter.x, combatAreaCenter.y, combatAreaCenter.z, combatAreaRadius);
        }
        
        for (BossPhase phase : phases) {
            props.addPhase(phase);
        }
        
        return props;
    }
}
