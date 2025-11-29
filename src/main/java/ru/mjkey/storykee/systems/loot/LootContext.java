package ru.mjkey.storykee.systems.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.*;

/**
 * Context information for loot generation.
 * 
 * Requirements: 58.3
 */
public class LootContext {
    
    private final UUID playerId;
    private final ServerPlayer player;
    private final Entity sourceEntity;
    private final BlockPos position;
    private final String dimension;
    private final String lootType;
    private final Map<String, Object> customData;
    private final Random random;
    private final float luck;
    
    private LootContext(Builder builder) {
        this.playerId = builder.playerId;
        this.player = builder.player;
        this.sourceEntity = builder.sourceEntity;
        this.position = builder.position;
        this.dimension = builder.dimension;
        this.lootType = builder.lootType;
        this.customData = new HashMap<>(builder.customData);
        this.random = builder.random != null ? builder.random : new Random();
        this.luck = builder.luck;
    }
    
    public UUID getPlayerId() { return playerId; }
    public ServerPlayer getPlayer() { return player; }
    public Entity getSourceEntity() { return sourceEntity; }
    public BlockPos getPosition() { return position; }
    public String getDimension() { return dimension; }
    public String getLootType() { return lootType; }
    public Random getRandom() { return random; }
    public float getLuck() { return luck; }
    
    public Object get(String key) {
        return customData.get(key);
    }
    
    public String getString(String key) {
        Object value = customData.get(key);
        return value != null ? String.valueOf(value) : null;
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = customData.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = customData.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public boolean has(String key) {
        return customData.containsKey(key);
    }
    
    /**
     * Builder for creating LootContext instances.
     */
    public static class Builder {
        private UUID playerId;
        private ServerPlayer player;
        private Entity sourceEntity;
        private BlockPos position;
        private String dimension;
        private String lootType;
        private Map<String, Object> customData = new HashMap<>();
        private Random random;
        private float luck = 0f;
        
        public Builder player(ServerPlayer player) {
            this.player = player;
            this.playerId = player != null ? player.getUUID() : null;
            return this;
        }
        
        public Builder playerId(UUID playerId) {
            this.playerId = playerId;
            return this;
        }
        
        public Builder sourceEntity(Entity entity) {
            this.sourceEntity = entity;
            return this;
        }
        
        public Builder position(BlockPos position) {
            this.position = position;
            return this;
        }
        
        public Builder dimension(String dimension) {
            this.dimension = dimension;
            return this;
        }
        
        public Builder lootType(String lootType) {
            this.lootType = lootType;
            return this;
        }
        
        public Builder data(String key, Object value) {
            this.customData.put(key, value);
            return this;
        }
        
        public Builder random(Random random) {
            this.random = random;
            return this;
        }
        
        public Builder luck(float luck) {
            this.luck = luck;
            return this;
        }
        
        public LootContext build() {
            return new LootContext(this);
        }
    }
}
