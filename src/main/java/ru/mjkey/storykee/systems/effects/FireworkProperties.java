package ru.mjkey.storykee.systems.effects;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.item.component.FireworkExplosion;

import java.util.ArrayList;
import java.util.List;

/**
 * Properties for customizing firework rockets.
 * 
 * Requirements: 57.1, 57.4
 */
public class FireworkProperties {
    
    public static final FireworkProperties DEFAULT = new FireworkProperties()
        .addEffect(new ExplosionEffect(FireworkExplosion.Shape.SMALL_BALL, 0xFF0000));
    
    public static final FireworkProperties CELEBRATION = new FireworkProperties()
        .setFlightDuration(2)
        .addEffect(new ExplosionEffect(FireworkExplosion.Shape.LARGE_BALL, 0xFFD700, 0xFF4500).withTrail().withTwinkle())
        .addEffect(new ExplosionEffect(FireworkExplosion.Shape.STAR, 0x00FF00, 0x0000FF).withTrail());
    
    private int flightDuration;
    private List<ExplosionEffect> effects;
    
    public FireworkProperties() {
        this.flightDuration = 1;
        this.effects = new ArrayList<>();
    }
    
    public int getFlightDuration() {
        return flightDuration;
    }
    
    public FireworkProperties setFlightDuration(int duration) {
        this.flightDuration = Math.max(0, Math.min(3, duration));
        return this;
    }
    
    public List<ExplosionEffect> getEffects() {
        return effects;
    }
    
    public FireworkProperties addEffect(ExplosionEffect effect) {
        this.effects.add(effect);
        return this;
    }
    
    public FireworkProperties clearEffects() {
        this.effects.clear();
        return this;
    }
    
    /**
     * Represents a single explosion effect in a firework.
     * Requirement 57.4: WHEN fireworks are customized THEN the Runtime SHALL support shapes, fades, and trails
     */
    public static class ExplosionEffect {
        private FireworkExplosion.Shape shape;
        private IntList colors;
        private IntList fadeColors;
        private boolean trail;
        private boolean twinkle;
        
        public ExplosionEffect(FireworkExplosion.Shape shape, int... colors) {
            this.shape = shape;
            this.colors = new IntArrayList(colors);
            this.fadeColors = new IntArrayList();
            this.trail = false;
            this.twinkle = false;
        }
        
        public FireworkExplosion.Shape getShape() { return shape; }
        public IntList getColors() { return colors; }
        public IntList getFadeColors() { return fadeColors; }
        public boolean hasTrail() { return trail; }
        public boolean hasTwinkle() { return twinkle; }
        
        public ExplosionEffect setShape(FireworkExplosion.Shape shape) {
            this.shape = shape;
            return this;
        }
        
        public ExplosionEffect addColor(int color) {
            this.colors.add(color);
            return this;
        }
        
        public ExplosionEffect addFadeColor(int color) {
            this.fadeColors.add(color);
            return this;
        }
        
        public ExplosionEffect withTrail() {
            this.trail = true;
            return this;
        }
        
        public ExplosionEffect withTwinkle() {
            this.twinkle = true;
            return this;
        }
        
        public ExplosionEffect setTrail(boolean trail) {
            this.trail = trail;
            return this;
        }
        
        public ExplosionEffect setTwinkle(boolean twinkle) {
            this.twinkle = twinkle;
            return this;
        }
    }
    
    // ===== Builder Methods for Common Firework Types =====
    
    /**
     * Creates a simple single-color firework.
     */
    public static FireworkProperties simple(int color) {
        return new FireworkProperties()
            .addEffect(new ExplosionEffect(FireworkExplosion.Shape.SMALL_BALL, color));
    }
    
    /**
     * Creates a large burst firework.
     */
    public static FireworkProperties largeBurst(int... colors) {
        return new FireworkProperties()
            .setFlightDuration(2)
            .addEffect(new ExplosionEffect(FireworkExplosion.Shape.LARGE_BALL, colors).withTrail());
    }
    
    /**
     * Creates a star-shaped firework.
     */
    public static FireworkProperties star(int... colors) {
        return new FireworkProperties()
            .setFlightDuration(2)
            .addEffect(new ExplosionEffect(FireworkExplosion.Shape.STAR, colors).withTwinkle());
    }
    
    /**
     * Creates a creeper-face firework.
     */
    public static FireworkProperties creeper(int color) {
        return new FireworkProperties()
            .setFlightDuration(2)
            .addEffect(new ExplosionEffect(FireworkExplosion.Shape.CREEPER, color));
    }
    
    /**
     * Creates a burst firework.
     */
    public static FireworkProperties burst(int... colors) {
        return new FireworkProperties()
            .setFlightDuration(1)
            .addEffect(new ExplosionEffect(FireworkExplosion.Shape.BURST, colors));
    }
    
    // ===== Color Constants =====
    
    public static final int RED = 0xFF0000;
    public static final int GREEN = 0x00FF00;
    public static final int BLUE = 0x0000FF;
    public static final int YELLOW = 0xFFFF00;
    public static final int ORANGE = 0xFF8000;
    public static final int PURPLE = 0x8000FF;
    public static final int PINK = 0xFF80FF;
    public static final int WHITE = 0xFFFFFF;
    public static final int GOLD = 0xFFD700;
    public static final int CYAN = 0x00FFFF;
}
