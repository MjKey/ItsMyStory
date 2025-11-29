package ru.mjkey.storykee.systems.npc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Properties for configuring a StoryNPC entity.
 * Contains all customizable attributes like name, skin, position, and custom data.
 * 
 * Requirements: 6.1, 6.2
 */
public class NPCProperties {
    
    private String name;
    private String skinUrl;
    private double x;
    private double y;
    private double z;
    private String dimension;
    private float yaw;
    private float pitch;
    private boolean invulnerable;
    private boolean silent;
    private boolean noGravity;
    private Map<String, Object> customData;
    
    public NPCProperties() {
        this.name = "NPC";
        this.skinUrl = null;
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.dimension = "minecraft:overworld";
        this.yaw = 0;
        this.pitch = 0;
        this.invulnerable = true;
        this.silent = true;
        this.noGravity = false;
        this.customData = new HashMap<>();
    }
    
    // Builder-style setters
    
    public NPCProperties name(String name) {
        this.name = name;
        return this;
    }
    
    public NPCProperties skinUrl(String skinUrl) {
        this.skinUrl = skinUrl;
        return this;
    }
    
    public NPCProperties position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }
    
    public NPCProperties dimension(String dimension) {
        this.dimension = dimension;
        return this;
    }
    
    public NPCProperties rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        return this;
    }
    
    public NPCProperties invulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
        return this;
    }
    
    public NPCProperties silent(boolean silent) {
        this.silent = silent;
        return this;
    }
    
    public NPCProperties noGravity(boolean noGravity) {
        this.noGravity = noGravity;
        return this;
    }
    
    public NPCProperties customData(Map<String, Object> customData) {
        this.customData = customData != null ? new HashMap<>(customData) : new HashMap<>();
        return this;
    }
    
    public NPCProperties setCustomData(String key, Object value) {
        this.customData.put(key, value);
        return this;
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public String getSkinUrl() {
        return skinUrl;
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
    
    public String getDimension() {
        return dimension;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public boolean isInvulnerable() {
        return invulnerable;
    }
    
    public boolean isSilent() {
        return silent;
    }
    
    public boolean isNoGravity() {
        return noGravity;
    }
    
    public Map<String, Object> getCustomData() {
        return new HashMap<>(customData);
    }
    
    public Object getCustomData(String key) {
        return customData.get(key);
    }
    
    /**
     * Creates a copy of these properties.
     */
    public NPCProperties copy() {
        NPCProperties copy = new NPCProperties();
        copy.name = this.name;
        copy.skinUrl = this.skinUrl;
        copy.x = this.x;
        copy.y = this.y;
        copy.z = this.z;
        copy.dimension = this.dimension;
        copy.yaw = this.yaw;
        copy.pitch = this.pitch;
        copy.invulnerable = this.invulnerable;
        copy.silent = this.silent;
        copy.noGravity = this.noGravity;
        copy.customData = new HashMap<>(this.customData);
        return copy;
    }
    
    /**
     * Creates NPCProperties from a map (typically from script).
     */
    public static NPCProperties fromMap(Map<String, Object> map) {
        NPCProperties props = new NPCProperties();
        
        if (map.containsKey("name")) {
            props.name(String.valueOf(map.get("name")));
        }
        if (map.containsKey("skin") || map.containsKey("skinUrl")) {
            props.skinUrl(String.valueOf(map.getOrDefault("skin", map.get("skinUrl"))));
        }
        if (map.containsKey("x")) {
            props.x = toDouble(map.get("x"));
        }
        if (map.containsKey("y")) {
            props.y = toDouble(map.get("y"));
        }
        if (map.containsKey("z")) {
            props.z = toDouble(map.get("z"));
        }
        if (map.containsKey("dimension")) {
            props.dimension(String.valueOf(map.get("dimension")));
        }
        if (map.containsKey("yaw")) {
            props.yaw = (float) toDouble(map.get("yaw"));
        }
        if (map.containsKey("pitch")) {
            props.pitch = (float) toDouble(map.get("pitch"));
        }
        if (map.containsKey("invulnerable")) {
            props.invulnerable(toBoolean(map.get("invulnerable")));
        }
        if (map.containsKey("silent")) {
            props.silent(toBoolean(map.get("silent")));
        }
        if (map.containsKey("noGravity")) {
            props.noGravity(toBoolean(map.get("noGravity")));
        }
        
        // Copy remaining keys as custom data
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!isStandardProperty(key)) {
                props.customData.put(key, entry.getValue());
            }
        }
        
        return props;
    }
    
    private static boolean isStandardProperty(String key) {
        return key.equals("name") || key.equals("skin") || key.equals("skinUrl") ||
               key.equals("x") || key.equals("y") || key.equals("z") ||
               key.equals("dimension") || key.equals("yaw") || key.equals("pitch") ||
               key.equals("invulnerable") || key.equals("silent") || key.equals("noGravity");
    }
    
    private static double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
    
    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NPCProperties that = (NPCProperties) o;
        return Double.compare(that.x, x) == 0 &&
               Double.compare(that.y, y) == 0 &&
               Double.compare(that.z, z) == 0 &&
               Float.compare(that.yaw, yaw) == 0 &&
               Float.compare(that.pitch, pitch) == 0 &&
               invulnerable == that.invulnerable &&
               silent == that.silent &&
               noGravity == that.noGravity &&
               Objects.equals(name, that.name) &&
               Objects.equals(skinUrl, that.skinUrl) &&
               Objects.equals(dimension, that.dimension) &&
               Objects.equals(customData, that.customData);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, skinUrl, x, y, z, dimension, yaw, pitch, 
                           invulnerable, silent, noGravity, customData);
    }
    
    @Override
    public String toString() {
        return "NPCProperties{" +
               "name='" + name + '\'' +
               ", skinUrl='" + skinUrl + '\'' +
               ", position=(" + x + ", " + y + ", " + z + ")" +
               ", dimension='" + dimension + '\'' +
               ", rotation=(" + yaw + ", " + pitch + ")" +
               ", invulnerable=" + invulnerable +
               ", silent=" + silent +
               ", noGravity=" + noGravity +
               ", customData=" + customData +
               '}';
    }
}
