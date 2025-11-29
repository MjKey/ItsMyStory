package ru.mjkey.storykee.systems.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.events.EventData;
import ru.mjkey.storykee.events.EventManager;

import ru.mjkey.storykee.systems.animation.Animation;
import ru.mjkey.storykee.systems.animation.AnimationFrame;
import ru.mjkey.storykee.systems.animation.AnimationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom NPC entity for the Storykee story system.
 * Extends PathfinderMob to support AI-based movement and behaviors.
 * 
 * Requirements: 6.1, 6.2, 6.3, 10.2
 */
public class StoryNPC extends PathfinderMob {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryNPC.class);
    
    private String npcId;
    private String skinUrl;
    private Map<String, Object> customData;
    private boolean interactionEnabled = true;
    
    // Movement target for scripted movement
    private Vec3 movementTarget;
    private double movementSpeed = 1.0;
    
    // Movement controller for advanced behaviors (Requirements: 6.4)
    private NPCMovementController movementController;
    
    // Animation support (Requirements: 10.2)
    private String currentAnimationId;
    private float animationTime;
    private boolean animationLoop;
    private Runnable animationCallback;
    private AnimationFrame currentAnimationFrame;
    
    public StoryNPC(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.customData = new HashMap<>();
        this.npcId = UUID.randomUUID().toString();
        this.movementController = new NPCMovementController(this);
    }
    
    /**
     * Creates attribute supplier for StoryNPC entities.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }
    
    @Override
    protected void registerGoals() {
        // Basic AI goals for natural NPC behavior
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }
    
    /**
     * Applies properties to this NPC.
     * Requirement 6.2: Apply specified name, skin, and model properties.
     * 
     * @param properties The properties to apply
     */
    public void applyProperties(NPCProperties properties) {
        if (properties == null) {
            return;
        }
        
        // Set display name
        if (properties.getName() != null) {
            this.setCustomName(Component.literal(properties.getName()));
            this.setCustomNameVisible(true);
        }
        
        // Set skin URL for custom rendering
        this.skinUrl = properties.getSkinUrl();
        
        // Set position
        this.setPos(properties.getX(), properties.getY(), properties.getZ());
        
        // Set rotation
        this.setYRot(properties.getYaw());
        this.setXRot(properties.getPitch());
        
        // Set entity flags
        this.setInvulnerable(properties.isInvulnerable());
        this.setSilent(properties.isSilent());
        this.setNoGravity(properties.isNoGravity());
        
        // Store custom data
        this.customData = new HashMap<>(properties.getCustomData());
        
        LOGGER.debug("Applied properties to NPC {}: {}", npcId, properties);
    }
    
    /**
     * Updates specific properties on this NPC.
     * Requirement 6.4: Reflect changes immediately in the game world.
     * 
     * @param properties The properties to update
     */
    public void updateProperties(NPCProperties properties) {
        applyProperties(properties);
    }
    
    /**
     * Handles player interaction with this NPC.
     * Requirement 6.3: Trigger associated script event on interaction.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!interactionEnabled) {
            return InteractionResult.PASS;
        }
        
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            LOGGER.debug("Player {} interacted with NPC {}", player.getName().getString(), npcId);
            
            // Fire NPC interaction event (both npcInteract and onNPCInteract for compatibility)
            EventData eventData = new EventData("npcInteract");
            eventData.set("npc", this.npcId);
            eventData.set("npcName", this.getCustomName() != null ? this.getCustomName().getString() : "NPC");
            eventData.set("player", player.getUUID());
            eventData.set("playerName", player.getName().getString());
            eventData.set("x", this.getX());
            eventData.set("y", this.getY());
            eventData.set("z", this.getZ());
            
            // Add custom data to event
            for (Map.Entry<String, Object> entry : customData.entrySet()) {
                eventData.set("custom_" + entry.getKey(), entry.getValue());
            }
            
            // Fire both event names for compatibility
            EventManager.getInstance().fireEvent("npcInteract", eventData);
            EventManager.getInstance().fireEvent("onNPCInteract", eventData);
            
            return eventData.isCancelled() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
        }
        
        return this.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }
    
    /**
     * Moves the NPC to a target position.
     * 
     * @param x Target X coordinate
     * @param y Target Y coordinate
     * @param z Target Z coordinate
     * @param speed Movement speed multiplier
     */
    public void moveTo(double x, double y, double z, double speed) {
        this.movementTarget = new Vec3(x, y, z);
        this.movementSpeed = speed;
        
        // Use navigation to move to target
        this.getNavigation().moveTo(x, y, z, speed);
        
        LOGGER.debug("NPC {} moving to ({}, {}, {}) at speed {}", npcId, x, y, z, speed);
    }
    
    /**
     * Stops the NPC's current movement.
     */
    public void stopMovement() {
        this.movementTarget = null;
        this.getNavigation().stop();
    }
    
    /**
     * Checks if the NPC has reached its movement target.
     */
    public boolean hasReachedTarget() {
        if (movementTarget == null) {
            return true;
        }
        
        double distance = this.position().distanceTo(movementTarget);
        return distance < 1.0;
    }
    
    /**
     * Makes the NPC look at a specific position.
     */
    public void lookAt(double x, double y, double z) {
        this.getLookControl().setLookAt(x, y, z);
    }
    
    /**
     * Makes the NPC look at a player.
     */
    public void lookAtPlayer(Player player) {
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }
    
    // ===== Animation Methods (Requirements: 10.2, 10.3, 10.4) =====
    
    /**
     * Plays an animation on this NPC.
     * 
     * @param animationId The animation ID to play
     * @param loop Whether to loop the animation
     * @param callback Optional callback when animation completes
     * @return true if animation started successfully
     */
    public boolean playAnimation(String animationId, boolean loop, Runnable callback) {
        Animation animation = AnimationManager.getInstance().getAnimation(animationId);
        if (animation == null) {
            LOGGER.warn("Animation '{}' not found for NPC {}", animationId, npcId);
            return false;
        }
        
        // Stop any current animation
        if (this.currentAnimationId != null) {
            stopAnimation();
        }
        
        this.currentAnimationId = animationId;
        this.animationTime = 0;
        this.animationLoop = loop || animation.isLoop();
        this.animationCallback = callback;
        this.currentAnimationFrame = animation.getFrameAtTime(0);
        
        LOGGER.debug("NPC {} started animation '{}'", npcId, animationId);
        return true;
    }
    
    /**
     * Plays an animation on this NPC without looping.
     * 
     * @param animationId The animation ID to play
     * @return true if animation started successfully
     */
    public boolean playAnimation(String animationId) {
        return playAnimation(animationId, false, null);
    }
    
    /**
     * Stops the current animation.
     */
    public void stopAnimation() {
        if (currentAnimationId != null) {
            LOGGER.debug("NPC {} stopped animation '{}'", npcId, currentAnimationId);
            currentAnimationId = null;
            animationTime = 0;
            animationLoop = false;
            animationCallback = null;
            currentAnimationFrame = null;
        }
    }
    
    /**
     * Checks if this NPC is currently playing an animation.
     * 
     * @return true if an animation is playing
     */
    public boolean isAnimating() {
        return currentAnimationId != null;
    }
    
    /**
     * Gets the current animation ID.
     * 
     * @return The animation ID, or null if not animating
     */
    public String getCurrentAnimationId() {
        return currentAnimationId;
    }
    
    /**
     * Gets the current animation time in seconds.
     * 
     * @return Animation time
     */
    public float getAnimationTime() {
        return animationTime;
    }
    
    /**
     * Gets the current animation frame.
     * 
     * @return Current frame, or null if not animating
     */
    public AnimationFrame getCurrentAnimationFrame() {
        return currentAnimationFrame;
    }
    
    /**
     * Updates the animation state. Called every tick.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void updateAnimation(float deltaTime) {
        if (currentAnimationId == null) {
            return;
        }
        
        Animation animation = AnimationManager.getInstance().getAnimation(currentAnimationId);
        if (animation == null) {
            stopAnimation();
            return;
        }
        
        animationTime += deltaTime;
        
        // Check if animation completed
        if (animationTime >= animation.getDuration()) {
            if (animationLoop) {
                // Loop the animation
                animationTime = animationTime % animation.getDuration();
            } else {
                // Animation completed
                Runnable callback = this.animationCallback;
                stopAnimation();
                
                // Fire callback after stopping
                if (callback != null) {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        LOGGER.error("Error in animation callback for NPC {}: {}", npcId, e.getMessage());
                    }
                }
                return;
            }
        }
        
        // Update current frame
        currentAnimationFrame = animation.getFrameAtTime(animationTime);
    }
    
    /**
     * Called every tick to update NPC state including animations and movement.
     */
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide()) {
            // Update animation (1 tick = 0.05 seconds at 20 TPS)
            updateAnimation(0.05f);
            
            // Update movement controller
            if (movementController != null) {
                movementController.tick();
            }
        }
    }
    
    /**
     * Gets the movement controller for this NPC.
     * 
     * @return The movement controller
     */
    public NPCMovementController getMovementController() {
        return movementController;
    }
    
    // Getters and setters
    
    public String getNpcId() {
        return npcId;
    }
    
    public void setNpcId(String npcId) {
        this.npcId = npcId;
    }
    
    public String getSkinUrl() {
        return skinUrl;
    }
    
    public void setSkinUrl(String skinUrl) {
        this.skinUrl = skinUrl;
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
    
    public boolean isInteractionEnabled() {
        return interactionEnabled;
    }
    
    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
    }
    
    /**
     * Gets the current properties of this NPC.
     */
    public NPCProperties getCurrentProperties() {
        NPCProperties props = new NPCProperties();
        
        String name = this.getCustomName() != null ? this.getCustomName().getString() : "NPC";
        props.name(name)
             .skinUrl(this.skinUrl)
             .position(this.getX(), this.getY(), this.getZ())
             .rotation(this.getYRot(), this.getXRot())
             .invulnerable(this.isInvulnerable())
             .silent(this.isSilent())
             .noGravity(this.isNoGravity())
             .customData(this.customData);
        
        return props;
    }
    
    /**
     * Cleans up resources when the NPC is removed.
     * Requirement 6.5: Clean up resources on removal.
     */
    @Override
    public void remove(RemovalReason reason) {
        LOGGER.debug("Removing NPC {} with reason {}", npcId, reason);
        
        // Fire NPC removal event
        EventData eventData = new EventData("onNPCRemove");
        eventData.set("npc", this.npcId);
        eventData.set("npcName", this.getCustomName() != null ? this.getCustomName().getString() : "NPC");
        eventData.set("reason", reason.name());
        
        EventManager.getInstance().fireEvent("onNPCRemove", eventData);
        
        // Clear custom data
        this.customData.clear();
        
        super.remove(reason);
    }
    
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        // Story NPCs should not despawn naturally
        return false;
    }
    
    @Override
    public boolean isPersistenceRequired() {
        // Story NPCs should persist
        return true;
    }
}
