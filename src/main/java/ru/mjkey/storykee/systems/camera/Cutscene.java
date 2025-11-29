package ru.mjkey.storykee.systems.camera;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a cinematic cutscene with camera waypoints.
 * 
 * Requirements: 19.1, 19.2
 */
public class Cutscene {
    
    private final String id;
    private final List<CameraWaypoint> waypoints;
    private final boolean skippable;
    private final Runnable onComplete;
    private final Runnable onSkip;
    
    private Cutscene(Builder builder) {
        this.id = builder.id;
        this.waypoints = Collections.unmodifiableList(new ArrayList<>(builder.waypoints));
        this.skippable = builder.skippable;
        this.onComplete = builder.onComplete;
        this.onSkip = builder.onSkip;
    }
    
    public String getId() {
        return id;
    }
    
    public List<CameraWaypoint> getWaypoints() {
        return waypoints;
    }
    
    public boolean isSkippable() {
        return skippable;
    }
    
    public Runnable getOnComplete() {
        return onComplete;
    }
    
    public Runnable getOnSkip() {
        return onSkip;
    }
    
    /**
     * Gets the total duration of the cutscene in ticks.
     * 
     * @return Total duration
     */
    public long getTotalDuration() {
        return waypoints.stream()
                .mapToLong(CameraWaypoint::getDurationTicks)
                .sum();
    }
    
    /**
     * Gets the waypoint at a specific time.
     * 
     * @param ticksElapsed Ticks since cutscene start
     * @return Array of [currentWaypoint, nextWaypoint, progress] or null if complete
     */
    public WaypointProgress getWaypointAtTime(long ticksElapsed) {
        if (waypoints.isEmpty()) {
            return null;
        }
        
        long accumulated = 0;
        for (int i = 0; i < waypoints.size(); i++) {
            CameraWaypoint waypoint = waypoints.get(i);
            long waypointEnd = accumulated + waypoint.getDurationTicks();
            
            if (ticksElapsed < waypointEnd) {
                CameraWaypoint previous = i > 0 ? waypoints.get(i - 1) : waypoint;
                float progress = waypoint.getDurationTicks() > 0 
                        ? (float)(ticksElapsed - accumulated) / waypoint.getDurationTicks()
                        : 1.0f;
                return new WaypointProgress(previous, waypoint, progress);
            }
            
            accumulated = waypointEnd;
        }
        
        // Cutscene complete
        return null;
    }
    
    /**
     * Builder for creating cutscenes.
     */
    public static class Builder {
        private final String id;
        private final List<CameraWaypoint> waypoints = new ArrayList<>();
        private boolean skippable = true;
        private Runnable onComplete;
        private Runnable onSkip;
        
        public Builder(String id) {
            this.id = id;
        }
        
        public Builder addWaypoint(CameraWaypoint waypoint) {
            waypoints.add(waypoint);
            return this;
        }
        
        public Builder skippable(boolean skippable) {
            this.skippable = skippable;
            return this;
        }
        
        public Builder onComplete(Runnable onComplete) {
            this.onComplete = onComplete;
            return this;
        }
        
        public Builder onSkip(Runnable onSkip) {
            this.onSkip = onSkip;
            return this;
        }
        
        public Cutscene build() {
            return new Cutscene(this);
        }
    }
    
    /**
     * Represents progress between two waypoints.
     */
    public record WaypointProgress(CameraWaypoint from, CameraWaypoint to, float progress) {
    }
}
