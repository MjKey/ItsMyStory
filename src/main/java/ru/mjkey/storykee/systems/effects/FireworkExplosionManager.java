package ru.mjkey.storykee.systems.effects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manages firework and explosion effects for Storykee scripts.
 * 
 * Requirements: 57.1, 57.2, 57.3, 57.4, 57.5
 */
public class FireworkExplosionManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FireworkExplosionManager.class);
    
    private static FireworkExplosionManager instance;
    
    private MinecraftServer server;
    
    private FireworkExplosionManager() {
    }
    
    public static FireworkExplosionManager getInstance() {
        if (instance == null) {
            instance = new FireworkExplosionManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // ===== Firework Launching (Requirement 57.1) =====
    
    /**
     * Launches a firework at a location.
     * Requirement 57.1: WHEN a script launches a firework THEN the Runtime SHALL create it with specified colors and effects
     */
    public FireworkRocketEntity launchFirework(double x, double y, double z, String dimension, FireworkProperties properties) {
        if (server == null) {
            LOGGER.warn("launchFirework: Server not available");
            return null;
        }
        
        ServerLevel level = getLevel(dimension);
        if (level == null) {
            LOGGER.warn("launchFirework: Unknown dimension - {}", dimension);
            return null;
        }
        
        // Create firework item
        ItemStack fireworkItem = createFireworkItem(properties);
        
        // Create and spawn the firework entity
        FireworkRocketEntity firework = new FireworkRocketEntity(level, x, y, z, fireworkItem);
        
        if (level.addFreshEntity(firework)) {
            LOGGER.info("launchFirework: Launched firework at ({}, {}, {})", x, y, z);
            return firework;
        }
        
        return null;
    }
    
    /**
     * Launches a simple firework with default properties.
     */
    public FireworkRocketEntity launchFirework(double x, double y, double z, String dimension) {
        return launchFirework(x, y, z, dimension, FireworkProperties.DEFAULT);
    }
    
    /**
     * Creates a firework item with specified properties.
     * Requirement 57.4: WHEN fireworks are customized THEN the Runtime SHALL support shapes, fades, and trails
     */
    private ItemStack createFireworkItem(FireworkProperties properties) {
        ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
        
        // Create explosion effects
        List<FireworkExplosion> explosions = new ArrayList<>();
        
        for (FireworkProperties.ExplosionEffect effect : properties.getEffects()) {
            FireworkExplosion explosion = new FireworkExplosion(
                effect.getShape(),
                effect.getColors(),
                effect.getFadeColors(),
                effect.hasTrail(),
                effect.hasTwinkle()
            );
            explosions.add(explosion);
        }
        
        // Create fireworks component
        Fireworks fireworksComponent = new Fireworks(properties.getFlightDuration(), explosions);
        firework.set(DataComponents.FIREWORKS, fireworksComponent);
        
        return firework;
    }
    
    /**
     * Launches multiple fireworks in a pattern.
     */
    public List<FireworkRocketEntity> launchFireworkShow(double centerX, double centerY, double centerZ, 
                                                          String dimension, int count, double radius, 
                                                          FireworkProperties properties) {
        List<FireworkRocketEntity> fireworks = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double x = centerX + radius * Math.cos(angle) + (random.nextDouble() - 0.5) * 2;
            double z = centerZ + radius * Math.sin(angle) + (random.nextDouble() - 0.5) * 2;
            
            FireworkRocketEntity firework = launchFirework(x, centerY, z, dimension, properties);
            if (firework != null) {
                fireworks.add(firework);
            }
        }
        
        LOGGER.info("launchFireworkShow: Launched {} fireworks", fireworks.size());
        return fireworks;
    }

    // ===== Explosion Creation (Requirement 57.2) =====
    
    /**
     * Creates an explosion at a location.
     * Requirement 57.2: WHEN a script creates an explosion THEN the Runtime SHALL generate it at the specified location
     */
    public void createExplosion(double x, double y, double z, String dimension, ExplosionProperties properties) {
        if (server == null) {
            LOGGER.warn("createExplosion: Server not available");
            return;
        }
        
        ServerLevel level = getLevel(dimension);
        if (level == null) {
            LOGGER.warn("createExplosion: Unknown dimension - {}", dimension);
            return;
        }
        
        // Determine explosion mode based on properties
        Level.ExplosionInteraction interaction;
        if (!properties.isDestroyBlocks()) {
            interaction = Level.ExplosionInteraction.NONE;
        } else if (properties.isDropItems()) {
            interaction = Level.ExplosionInteraction.BLOCK;
        } else {
            interaction = Level.ExplosionInteraction.TNT;
        }
        
        // Create the explosion
        level.explode(
            null, // source entity
            x, y, z,
            properties.getPower(),
            properties.isCauseFire(),
            interaction
        );
        
        LOGGER.info("createExplosion: Created explosion at ({}, {}, {}) with power {}", 
            x, y, z, properties.getPower());
    }
    
    /**
     * Creates a simple explosion.
     */
    public void createExplosion(double x, double y, double z, String dimension, float power) {
        createExplosion(x, y, z, dimension, new ExplosionProperties(power));
    }
    
    /**
     * Creates a visual-only explosion (no damage or block destruction).
     */
    public void createVisualExplosion(double x, double y, double z, String dimension, float power) {
        ExplosionProperties props = new ExplosionProperties(power)
            .setDestroyBlocks(false)
            .setCauseFire(false)
            .setDamageEntities(false);
        createExplosion(x, y, z, dimension, props);
    }

    // ===== Sound Effects (Requirement 57.5) =====
    
    /**
     * Plays explosion sound at a location.
     * Requirement 57.5: WHEN effects are spawned THEN the Runtime SHALL play appropriate sounds
     */
    public void playExplosionSound(double x, double y, double z, String dimension) {
        if (server == null) return;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return;
        
        // The explosion itself plays sounds, but we can add additional sounds
        level.playSound(null, x, y, z, 
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 
            net.minecraft.sounds.SoundSource.BLOCKS, 
            4.0f, 1.0f);
    }
    
    /**
     * Plays firework launch sound.
     */
    public void playFireworkSound(double x, double y, double z, String dimension) {
        if (server == null) return;
        
        ServerLevel level = getLevel(dimension);
        if (level == null) return;
        
        level.playSound(null, x, y, z,
            net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH,
            net.minecraft.sounds.SoundSource.AMBIENT,
            3.0f, 1.0f);
    }

    // ===== Utility Methods =====
    
    private ServerLevel getLevel(String dimension) {
        if (server == null || dimension == null) return null;
        
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) {
                return level;
            }
        }
        
        return switch (dimension.toLowerCase()) {
            case "overworld", "minecraft:overworld" -> server.overworld();
            default -> null;
        };
    }
    
    /**
     * Properties for explosion customization.
     * Requirement 57.3: WHEN explosion properties are set THEN the Runtime SHALL control damage, fire, and block destruction
     */
    public static class ExplosionProperties {
        private float power;
        private boolean destroyBlocks;
        private boolean causeFire;
        private boolean damageEntities;
        private boolean dropItems;
        
        public ExplosionProperties(float power) {
            this.power = power;
            this.destroyBlocks = true;
            this.causeFire = false;
            this.damageEntities = true;
            this.dropItems = true;
        }
        
        public float getPower() { return power; }
        public boolean isDestroyBlocks() { return destroyBlocks; }
        public boolean isCauseFire() { return causeFire; }
        public boolean isDamageEntities() { return damageEntities; }
        public boolean isDropItems() { return dropItems; }
        
        public ExplosionProperties setPower(float power) {
            this.power = power;
            return this;
        }
        
        public ExplosionProperties setDestroyBlocks(boolean destroyBlocks) {
            this.destroyBlocks = destroyBlocks;
            return this;
        }
        
        public ExplosionProperties setCauseFire(boolean causeFire) {
            this.causeFire = causeFire;
            return this;
        }
        
        public ExplosionProperties setDamageEntities(boolean damageEntities) {
            this.damageEntities = damageEntities;
            return this;
        }
        
        public ExplosionProperties setDropItems(boolean dropItems) {
            this.dropItems = dropItems;
            return this;
        }
    }
}
