package ru.mjkey.storykee.systems.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads animation data from JSON files in the assets directory.
 * 
 * Animation JSON format:
 * {
 *   "id": "animation_name",
 *   "duration": 2.0,
 *   "loop": false,
 *   "frameRate": 20,
 *   "frames": [
 *     {
 *       "frame": 0,
 *       "time": 0.0,
 *       "bones": {
 *         "bone_name": {
 *           "position": [0, 0, 0],
 *           "rotation": [0, 0, 0],
 *           "scale": [1, 1, 1]
 *         }
 *       }
 *     }
 *   ]
 * }
 * 
 * Requirements: 10.1
 */
public class AnimationLoader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimationLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * Loads an animation from a JSON file.
     * 
     * @param path Path to the animation JSON file
     * @return Loaded animation, or null if loading failed
     */
    public static Animation loadFromFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return parseAnimation(json);
        } catch (IOException e) {
            LOGGER.error("Failed to load animation from {}: {}", path, e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.error("Failed to parse animation from {}: {}", path, e.getMessage());
            return null;
        }
    }
    
    /**
     * Loads an animation from a JSON string.
     * 
     * @param json JSON string
     * @return Loaded animation, or null if parsing failed
     */
    public static Animation loadFromString(String json) {
        try {
            JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();
            return parseAnimation(jsonObj);
        } catch (Exception e) {
            LOGGER.error("Failed to parse animation from JSON: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parses an animation from a JSON object.
     * 
     * @param json JSON object
     * @return Parsed animation
     */
    public static Animation parseAnimation(JsonObject json) {
        String id = json.has("id") ? json.get("id").getAsString() : "unnamed";
        float duration = json.has("duration") ? json.get("duration").getAsFloat() : 1.0f;
        boolean loop = json.has("loop") && json.get("loop").getAsBoolean();
        float frameRate = json.has("frameRate") ? json.get("frameRate").getAsFloat() : 20.0f;
        
        Animation animation = new Animation(id, duration, loop);
        animation.setFrameRate(frameRate);
        
        if (json.has("frames")) {
            JsonArray framesArray = json.getAsJsonArray("frames");
            for (JsonElement frameElement : framesArray) {
                AnimationFrame frame = parseFrame(frameElement.getAsJsonObject());
                animation.addFrame(frame);
            }
        }
        
        LOGGER.debug("Loaded animation '{}' with {} frames, duration {}s", 
                id, animation.getFrameCount(), duration);
        
        return animation;
    }
    
    /**
     * Parses an animation frame from a JSON object.
     * 
     * @param json JSON object
     * @return Parsed frame
     */
    private static AnimationFrame parseFrame(JsonObject json) {
        int frameNumber = json.has("frame") ? json.get("frame").getAsInt() : 0;
        float timestamp = json.has("time") ? json.get("time").getAsFloat() : 0;
        
        Map<String, Transform> boneTransforms = new HashMap<>();
        
        if (json.has("bones")) {
            JsonObject bonesObj = json.getAsJsonObject("bones");
            for (String boneName : bonesObj.keySet()) {
                Transform transform = parseTransform(bonesObj.getAsJsonObject(boneName));
                boneTransforms.put(boneName, transform);
            }
        }
        
        return new AnimationFrame(frameNumber, timestamp, boneTransforms);
    }
    
    /**
     * Parses a transform from a JSON object.
     * 
     * @param json JSON object
     * @return Parsed transform
     */
    private static Transform parseTransform(JsonObject json) {
        Vector3f position = parseVector3f(json, "position", new Vector3f(0, 0, 0));
        Vector3f rotation = parseVector3f(json, "rotation", new Vector3f(0, 0, 0));
        Vector3f scale = parseVector3f(json, "scale", new Vector3f(1, 1, 1));
        
        return new Transform(position, rotation, scale);
    }
    
    /**
     * Parses a Vector3f from a JSON array.
     * 
     * @param json Parent JSON object
     * @param key Key for the array
     * @param defaultValue Default value if not found
     * @return Parsed vector
     */
    private static Vector3f parseVector3f(JsonObject json, String key, Vector3f defaultValue) {
        if (!json.has(key)) {
            return defaultValue;
        }
        
        JsonArray array = json.getAsJsonArray(key);
        if (array.size() < 3) {
            return defaultValue;
        }
        
        return new Vector3f(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    }
    
    /**
     * Saves an animation to a JSON file.
     * 
     * @param animation Animation to save
     * @param path Path to save to
     * @return true if saved successfully
     */
    public static boolean saveToFile(Animation animation, Path path) {
        try {
            JsonObject json = serializeAnimation(animation);
            String jsonString = GSON.toJson(json);
            Files.writeString(path, jsonString);
            LOGGER.debug("Saved animation '{}' to {}", animation.getId(), path);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed to save animation to {}: {}", path, e.getMessage());
            return false;
        }
    }
    
    /**
     * Serializes an animation to a JSON object.
     * 
     * @param animation Animation to serialize
     * @return JSON object
     */
    public static JsonObject serializeAnimation(Animation animation) {
        JsonObject json = new JsonObject();
        json.addProperty("id", animation.getId());
        json.addProperty("duration", animation.getDuration());
        json.addProperty("loop", animation.isLoop());
        json.addProperty("frameRate", animation.getFrameRate());
        
        JsonArray framesArray = new JsonArray();
        for (AnimationFrame frame : animation.getFrames()) {
            framesArray.add(serializeFrame(frame));
        }
        json.add("frames", framesArray);
        
        return json;
    }
    
    /**
     * Serializes an animation frame to a JSON object.
     * 
     * @param frame Frame to serialize
     * @return JSON object
     */
    private static JsonObject serializeFrame(AnimationFrame frame) {
        JsonObject json = new JsonObject();
        json.addProperty("frame", frame.getFrameNumber());
        json.addProperty("time", frame.getTimestamp());
        
        JsonObject bonesObj = new JsonObject();
        for (Map.Entry<String, Transform> entry : frame.getBoneTransforms().entrySet()) {
            bonesObj.add(entry.getKey(), serializeTransform(entry.getValue()));
        }
        json.add("bones", bonesObj);
        
        return json;
    }
    
    /**
     * Serializes a transform to a JSON object.
     * 
     * @param transform Transform to serialize
     * @return JSON object
     */
    private static JsonObject serializeTransform(Transform transform) {
        JsonObject json = new JsonObject();
        
        JsonArray posArray = new JsonArray();
        posArray.add(transform.getX());
        posArray.add(transform.getY());
        posArray.add(transform.getZ());
        json.add("position", posArray);
        
        JsonArray rotArray = new JsonArray();
        rotArray.add(transform.getPitch());
        rotArray.add(transform.getYaw());
        rotArray.add(transform.getRoll());
        json.add("rotation", rotArray);
        
        JsonArray scaleArray = new JsonArray();
        scaleArray.add(transform.getScaleX());
        scaleArray.add(transform.getScaleY());
        scaleArray.add(transform.getScaleZ());
        json.add("scale", scaleArray);
        
        return json;
    }
    
    /**
     * Loads all animations from a directory.
     * 
     * @param directory Directory containing animation JSON files
     * @return Map of animation ID to animation
     */
    public static Map<String, Animation> loadFromDirectory(Path directory) {
        Map<String, Animation> animations = new HashMap<>();
        
        if (!Files.isDirectory(directory)) {
            LOGGER.warn("Animation directory does not exist: {}", directory);
            return animations;
        }
        
        try {
            Files.list(directory)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        Animation anim = loadFromFile(path);
                        if (anim != null) {
                            animations.put(anim.getId(), anim);
                        }
                    });
        } catch (IOException e) {
            LOGGER.error("Failed to list animation directory {}: {}", directory, e.getMessage());
        }
        
        LOGGER.info("Loaded {} animations from {}", animations.size(), directory);
        return animations;
    }
}
