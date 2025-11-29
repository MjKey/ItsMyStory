package ru.mjkey.storykee.systems.npc;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controls NPC movement behaviors including patrol paths, following players,
 * and AI behaviors like wandering and looking at players.
 * 
 * Requirements: 6.1, 6.4
 */
public class NPCMovementController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NPCMovementController.class);
    
    private final StoryNPC npc;
    
    // Patrol state
    private List<PatrolWaypoint> patrolPath;
    private int currentPatrolIndex;
    private boolean patrolLoop;
    private boolean patrolActive;
    private long patrolPauseEndTime;
    private double patrolSpeed;
    
    // Follow state
    private UUID followTargetId;
    private double followDistance;
    private double followSpeed;
    private boolean followActive;
    
    // Wander state
    private Vec3 wanderCenter;
    private double wanderRadius;
    private boolean wanderActive;
    private long nextWanderTime;
    private static final long WANDER_INTERVAL_TICKS = 100; // 5 seconds
    
    // Spawn point for return behavior
    private Vec3 spawnPoint;
    
    // Look at player state
    private boolean lookAtNearestPlayer;
    private double lookAtRange;
    
    public NPCMovementController(StoryNPC npc) {
        this.npc = npc;
        this.patrolPath = new ArrayList<>();
        this.currentPatrolIndex = 0;
        this.patrolLoop = true;
        this.patrolActive = false;
        this.patrolSpeed = 1.0;
        this.followDistance = 3.0;
        this.followSpeed = 1.0;
        this.followActive = false;
        this.wanderRadius = 10.0;
        this.wanderActive = false;
        this.lookAtNearestPlayer = false;
        this.lookAtRange = 8.0;
        
        // Store spawn point
        this.spawnPoint = npc.position();
    }

    
    // ===== Patrol Methods (Task 34.1) =====
    
    /**
     * Sets the patrol path for this NPC.
     * 
     * @param waypoints List of waypoints to patrol
     * @param loop Whether to loop back to the start after reaching the end
     * @param speed Movement speed multiplier
     */
    public void setPatrolPath(List<PatrolWaypoint> waypoints, boolean loop, double speed) {
        if (waypoints == null || waypoints.isEmpty()) {
            LOGGER.warn("Cannot set empty patrol path for NPC {}", npc.getNpcId());
            return;
        }
        
        this.patrolPath = new ArrayList<>(waypoints);
        this.patrolLoop = loop;
        this.patrolSpeed = speed;
        this.currentPatrolIndex = 0;
        this.patrolActive = true;
        this.patrolPauseEndTime = 0;
        
        // Stop other movement behaviors
        stopFollowing();
        stopWandering();
        
        LOGGER.debug("NPC {} patrol path set with {} waypoints, loop={}, speed={}", 
                npc.getNpcId(), waypoints.size(), loop, speed);
        
        // Start moving to first waypoint
        moveToCurrentWaypoint();
    }
    
    /**
     * Sets the patrol path from a list of Vec3 positions with default pause time.
     * 
     * @param positions List of positions
     * @param loop Whether to loop
     * @param speed Movement speed
     */
    public void setPatrolPath(List<Vec3> positions, boolean loop, double speed, long defaultPauseTicks) {
        List<PatrolWaypoint> waypoints = new ArrayList<>();
        for (Vec3 pos : positions) {
            waypoints.add(new PatrolWaypoint(pos, defaultPauseTicks));
        }
        setPatrolPath(waypoints, loop, speed);
    }
    
    /**
     * Starts patrolling the current path.
     */
    public void startPatrol() {
        if (patrolPath.isEmpty()) {
            LOGGER.warn("Cannot start patrol for NPC {}: no path set", npc.getNpcId());
            return;
        }
        
        this.patrolActive = true;
        moveToCurrentWaypoint();
    }
    
    /**
     * Stops patrolling.
     */
    public void stopPatrol() {
        this.patrolActive = false;
        npc.stopMovement();
        LOGGER.debug("NPC {} stopped patrolling", npc.getNpcId());
    }
    
    /**
     * Checks if the NPC is currently patrolling.
     */
    public boolean isPatrolling() {
        return patrolActive && !patrolPath.isEmpty();
    }
    
    /**
     * Gets the current patrol waypoint index.
     */
    public int getCurrentPatrolIndex() {
        return currentPatrolIndex;
    }
    
    /**
     * Gets the patrol path.
     */
    public List<PatrolWaypoint> getPatrolPath() {
        return new ArrayList<>(patrolPath);
    }
    
    /**
     * Clears the patrol path.
     */
    public void clearPatrolPath() {
        this.patrolPath.clear();
        this.patrolActive = false;
        this.currentPatrolIndex = 0;
    }
    
    private void moveToCurrentWaypoint() {
        if (!patrolActive || patrolPath.isEmpty()) {
            return;
        }
        
        PatrolWaypoint waypoint = patrolPath.get(currentPatrolIndex);
        npc.moveTo(waypoint.position().x, waypoint.position().y, waypoint.position().z, patrolSpeed);
    }
    
    private void advanceToNextWaypoint() {
        currentPatrolIndex++;
        
        if (currentPatrolIndex >= patrolPath.size()) {
            if (patrolLoop) {
                currentPatrolIndex = 0;
            } else {
                patrolActive = false;
                LOGGER.debug("NPC {} completed patrol path", npc.getNpcId());
                return;
            }
        }
        
        moveToCurrentWaypoint();
    }

    
    // ===== Follow Methods (Task 34.2) =====
    
    /**
     * Makes the NPC follow a player.
     * 
     * @param playerId The UUID of the player to follow
     * @param distance The distance to maintain from the player
     * @param speed Movement speed multiplier
     */
    public void followPlayer(UUID playerId, double distance, double speed) {
        if (playerId == null) {
            LOGGER.warn("Cannot follow null player for NPC {}", npc.getNpcId());
            return;
        }
        
        this.followTargetId = playerId;
        this.followDistance = Math.max(1.0, distance);
        this.followSpeed = speed;
        this.followActive = true;
        
        // Stop other movement behaviors
        stopPatrol();
        stopWandering();
        
        LOGGER.debug("NPC {} now following player {} at distance {}", 
                npc.getNpcId(), playerId, distance);
    }
    
    /**
     * Stops following the current target.
     */
    public void stopFollowing() {
        if (followActive) {
            this.followActive = false;
            this.followTargetId = null;
            npc.stopMovement();
            LOGGER.debug("NPC {} stopped following", npc.getNpcId());
        }
    }
    
    /**
     * Checks if the NPC is currently following a player.
     */
    public boolean isFollowing() {
        return followActive && followTargetId != null;
    }
    
    /**
     * Gets the UUID of the player being followed.
     */
    public UUID getFollowTargetId() {
        return followTargetId;
    }
    
    /**
     * Gets the follow distance.
     */
    public double getFollowDistance() {
        return followDistance;
    }
    
    /**
     * Sets the follow distance.
     */
    public void setFollowDistance(double distance) {
        this.followDistance = Math.max(1.0, distance);
    }
    
    // ===== AI Behavior Methods (Task 34.3) =====
    
    /**
     * Enables looking at the nearest player within range.
     * 
     * @param range The range to look for players
     */
    public void enableLookAtNearestPlayer(double range) {
        this.lookAtNearestPlayer = true;
        this.lookAtRange = range;
        LOGGER.debug("NPC {} will look at nearest player within {} blocks", npc.getNpcId(), range);
    }
    
    /**
     * Disables looking at the nearest player.
     */
    public void disableLookAtNearestPlayer() {
        this.lookAtNearestPlayer = false;
    }
    
    /**
     * Checks if the NPC is looking at the nearest player.
     */
    public boolean isLookingAtNearestPlayer() {
        return lookAtNearestPlayer;
    }
    
    /**
     * Starts wandering randomly within a radius from the current position.
     * 
     * @param radius The radius to wander within
     */
    public void startWandering(double radius) {
        this.wanderCenter = npc.position();
        this.wanderRadius = Math.max(1.0, radius);
        this.wanderActive = true;
        this.nextWanderTime = 0;
        
        // Stop other movement behaviors
        stopPatrol();
        stopFollowing();
        
        LOGGER.debug("NPC {} started wandering within {} blocks", npc.getNpcId(), radius);
    }
    
    /**
     * Stops wandering.
     */
    public void stopWandering() {
        if (wanderActive) {
            this.wanderActive = false;
            npc.stopMovement();
            LOGGER.debug("NPC {} stopped wandering", npc.getNpcId());
        }
    }
    
    /**
     * Checks if the NPC is currently wandering.
     */
    public boolean isWandering() {
        return wanderActive;
    }
    
    /**
     * Gets the wander radius.
     */
    public double getWanderRadius() {
        return wanderRadius;
    }
    
    /**
     * Makes the NPC return to its spawn point.
     * 
     * @param speed Movement speed multiplier
     */
    public void returnToSpawn(double speed) {
        // Stop all other behaviors
        stopPatrol();
        stopFollowing();
        stopWandering();
        
        npc.moveTo(spawnPoint.x, spawnPoint.y, spawnPoint.z, speed);
        LOGGER.debug("NPC {} returning to spawn point", npc.getNpcId());
    }
    
    /**
     * Sets the spawn point for this NPC.
     * 
     * @param position The spawn point position
     */
    public void setSpawnPoint(Vec3 position) {
        this.spawnPoint = position;
    }
    
    /**
     * Gets the spawn point.
     */
    public Vec3 getSpawnPoint() {
        return spawnPoint;
    }

    
    // ===== Update Method =====
    
    /**
     * Updates the movement controller. Called every tick.
     */
    public void tick() {
        long currentTime = npc.level().getGameTime();
        
        // Handle patrol behavior
        if (patrolActive && !patrolPath.isEmpty()) {
            updatePatrol(currentTime);
        }
        
        // Handle follow behavior
        if (followActive && followTargetId != null) {
            updateFollow();
        }
        
        // Handle wander behavior
        if (wanderActive) {
            updateWander(currentTime);
        }
        
        // Handle look at nearest player
        if (lookAtNearestPlayer) {
            updateLookAtNearestPlayer();
        }
    }
    
    private void updatePatrol(long currentTime) {
        // Check if we're pausing at a waypoint
        if (patrolPauseEndTime > 0) {
            if (currentTime >= patrolPauseEndTime) {
                patrolPauseEndTime = 0;
                advanceToNextWaypoint();
            }
            return;
        }
        
        // Check if we've reached the current waypoint
        if (npc.hasReachedTarget()) {
            PatrolWaypoint waypoint = patrolPath.get(currentPatrolIndex);
            
            if (waypoint.pauseTicks() > 0) {
                // Start pause at this waypoint
                patrolPauseEndTime = currentTime + waypoint.pauseTicks();
                npc.stopMovement();
            } else {
                // Move to next waypoint immediately
                advanceToNextWaypoint();
            }
        }
    }
    
    private void updateFollow() {
        Player target = findPlayerById(followTargetId);
        if (target == null) {
            // Target player not found, stop following
            stopFollowing();
            return;
        }
        
        double distance = npc.distanceTo(target);
        
        if (distance > followDistance + 0.5) {
            // Too far, move closer
            Vec3 targetPos = target.position();
            
            // Calculate position at follow distance from target
            Vec3 direction = npc.position().subtract(targetPos).normalize();
            Vec3 followPos = targetPos.add(direction.scale(followDistance));
            
            npc.moveTo(followPos.x, followPos.y, followPos.z, followSpeed);
        } else if (distance < followDistance - 0.5) {
            // Too close, back up slightly
            npc.stopMovement();
        }
        
        // Always look at the target
        npc.lookAtPlayer(target);
    }
    
    private void updateWander(long currentTime) {
        if (currentTime < nextWanderTime) {
            return;
        }
        
        // Check if we've reached our wander target or are idle
        if (npc.hasReachedTarget() || npc.getNavigation().isDone()) {
            // Pick a new random position within the wander radius
            Vec3 newTarget = getRandomWanderPosition();
            if (newTarget != null) {
                npc.moveTo(newTarget.x, newTarget.y, newTarget.z, 0.6);
            }
            
            // Set next wander time with some randomness
            nextWanderTime = currentTime + WANDER_INTERVAL_TICKS + 
                    (long)(Math.random() * WANDER_INTERVAL_TICKS);
        }
    }
    
    private Vec3 getRandomWanderPosition() {
        // Try to find a valid position within the wander radius
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = Math.random() * wanderRadius;
            
            double x = wanderCenter.x + Math.cos(angle) * distance;
            double z = wanderCenter.z + Math.sin(angle) * distance;
            double y = wanderCenter.y;
            
            // Check if the position is valid (simple ground check)
            if (npc.getNavigation().createPath(x, y, z, 1) != null) {
                return new Vec3(x, y, z);
            }
        }
        
        return null;
    }
    
    private void updateLookAtNearestPlayer() {
        Player nearest = findNearestPlayer(lookAtRange);
        if (nearest != null) {
            npc.lookAtPlayer(nearest);
        }
    }
    
    private Player findPlayerById(UUID playerId) {
        if (npc.level().isClientSide()) {
            return null;
        }
        
        for (Player player : npc.level().players()) {
            if (player.getUUID().equals(playerId)) {
                return player;
            }
        }
        return null;
    }
    
    private Player findNearestPlayer(double range) {
        Player nearest = null;
        double nearestDistance = range;
        
        for (Player player : npc.level().players()) {
            double distance = npc.distanceTo(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Stops all movement behaviors.
     */
    public void stopAll() {
        stopPatrol();
        stopFollowing();
        stopWandering();
        disableLookAtNearestPlayer();
        npc.stopMovement();
    }
    
    /**
     * Record class for patrol waypoints.
     */
    public record PatrolWaypoint(Vec3 position, long pauseTicks) {
        public PatrolWaypoint(Vec3 position) {
            this(position, 0);
        }
        
        public PatrolWaypoint(double x, double y, double z, long pauseTicks) {
            this(new Vec3(x, y, z), pauseTicks);
        }
        
        public PatrolWaypoint(double x, double y, double z) {
            this(new Vec3(x, y, z), 0);
        }
    }
}
