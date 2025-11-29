package ru.mjkey.storykee.systems.particle;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Properties for custom particle effects.
 * 
 * Requirements: 20.2
 */
public class ParticleProperties {
    
    private int color = 0xFFFFFF;
    private float size = 1.0f;
    private Vec3 velocity = Vec3.ZERO;
    private int lifetime = 20; // ticks
    private float gravity = 0.0f;
    private boolean collision = false;
    private float alpha = 1.0f;
    private ResourceLocation customTexture = null;
    private int count = 1;
    private Vec3 spread = Vec3.ZERO;
    
    public ParticleProperties() {}
    
    public int getColor() {
        return color;
    }
    
    public ParticleProperties setColor(int color) {
        this.color = color;
        return this;
    }
    
    public ParticleProperties setColor(int r, int g, int b) {
        this.color = (r << 16) | (g << 8) | b;
        return this;
    }
    
    public float getSize() {
        return size;
    }
    
    public ParticleProperties setSize(float size) {
        this.size = size;
        return this;
    }
    
    public Vec3 getVelocity() {
        return velocity;
    }
    
    public ParticleProperties setVelocity(Vec3 velocity) {
        this.velocity = velocity;
        return this;
    }
    
    public ParticleProperties setVelocity(double x, double y, double z) {
        this.velocity = new Vec3(x, y, z);
        return this;
    }
    
    public int getLifetime() {
        return lifetime;
    }
    
    public ParticleProperties setLifetime(int lifetime) {
        this.lifetime = lifetime;
        return this;
    }
    
    public float getGravity() {
        return gravity;
    }
    
    public ParticleProperties setGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }
    
    public boolean hasCollision() {
        return collision;
    }
    
    public ParticleProperties setCollision(boolean collision) {
        this.collision = collision;
        return this;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public ParticleProperties setAlpha(float alpha) {
        this.alpha = Math.max(0, Math.min(1, alpha));
        return this;
    }
    
    public ResourceLocation getCustomTexture() {
        return customTexture;
    }
    
    public ParticleProperties setCustomTexture(ResourceLocation customTexture) {
        this.customTexture = customTexture;
        return this;
    }
    
    public int getCount() {
        return count;
    }
    
    public ParticleProperties setCount(int count) {
        this.count = Math.max(1, count);
        return this;
    }
    
    public Vec3 getSpread() {
        return spread;
    }
    
    public ParticleProperties setSpread(Vec3 spread) {
        this.spread = spread;
        return this;
    }
    
    public ParticleProperties setSpread(double x, double y, double z) {
        this.spread = new Vec3(x, y, z);
        return this;
    }
    
    /**
     * Gets the red component (0-255).
     */
    public int getRed() {
        return (color >> 16) & 0xFF;
    }
    
    /**
     * Gets the green component (0-255).
     */
    public int getGreen() {
        return (color >> 8) & 0xFF;
    }
    
    /**
     * Gets the blue component (0-255).
     */
    public int getBlue() {
        return color & 0xFF;
    }
    
    /**
     * Creates a copy of these properties.
     */
    public ParticleProperties copy() {
        ParticleProperties copy = new ParticleProperties();
        copy.color = this.color;
        copy.size = this.size;
        copy.velocity = this.velocity;
        copy.lifetime = this.lifetime;
        copy.gravity = this.gravity;
        copy.collision = this.collision;
        copy.alpha = this.alpha;
        copy.customTexture = this.customTexture;
        copy.count = this.count;
        copy.spread = this.spread;
        return copy;
    }
}
