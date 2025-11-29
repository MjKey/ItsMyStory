package ru.mjkey.storykee.runtime.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.context.ExecutionContext;
import ru.mjkey.storykee.systems.npc.NPCManager;
import ru.mjkey.storykee.systems.npc.NPCProperties;
import ru.mjkey.storykee.systems.npc.StoryNPC;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

/**
 * Registry of built-in functions available to Storykee scripts.
 * Provides functions for NPC, dialogue, quest, HUD/GUI, world, player, timer, and utility operations.
 */
public class BuiltinFunctions {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BuiltinFunctions.class);
    
    // Function registry: name -> (arguments, context) -> result
    private final Map<String, BiFunction<List<Object>, ExecutionContext, Object>> functions;
    
    public BuiltinFunctions() {
        this.functions = new HashMap<>();
        registerAllFunctions();
    }
    
    /**
     * Checks if a function with the given name exists.
     */
    public boolean hasFunction(String name) {
        return functions.containsKey(name);
    }
    
    /**
     * Calls a built-in function with the given arguments.
     */
    public Object call(String name, List<Object> arguments, ExecutionContext context) {
        BiFunction<List<Object>, ExecutionContext, Object> function = functions.get(name);
        
        if (function == null) {
            throw new RuntimeException("Unknown built-in function: " + name);
        }
        
        try {
            return function.apply(arguments, context);
        } catch (Exception e) {
            LOGGER.error("Error calling built-in function {}: {}", name, e.getMessage(), e);
            throw new RuntimeException("Error in built-in function " + name + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Registers all built-in functions.
     */
    private void registerAllFunctions() {
        // Utility functions
        registerUtilityFunctions();
        
        // NPC functions
        registerNPCFunctions();
        
        // Dialogue functions
        registerDialogueFunctions();
        
        // Quest functions
        registerQuestFunctions();
        
        // HUD/GUI functions
        registerHUDGUIFunctions();
        
        // World functions
        registerWorldFunctions();
        
        // Player functions
        registerPlayerFunctions();
        
        // Timer functions
        registerTimerFunctions();
        
        // Animation functions (Requirements: 10.2, 10.3)
        registerAnimationFunctions();
    }
    
    // ===== Utility Functions =====
    
    private void registerUtilityFunctions() {
        // print(message) - prints to console
        functions.put("print", (args, ctx) -> {
            if (args.isEmpty()) {
                LOGGER.info("[Script {}] ", ctx.getScriptId());
            } else {
                String message = args.stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + " " + b)
                    .orElse("");
                LOGGER.info("[Script {}] {}", ctx.getScriptId(), message);
            }
            return null;
        });
        
        // log(level, message) - logs with specified level
        functions.put("log", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("log() requires 2 arguments: level, message");
            }
            
            String level = String.valueOf(args.get(0)).toUpperCase();
            String message = String.valueOf(args.get(1));
            
            switch (level) {
                case "DEBUG":
                    LOGGER.debug("[Script {}] {}", ctx.getScriptId(), message);
                    break;
                case "INFO":
                    LOGGER.info("[Script {}] {}", ctx.getScriptId(), message);
                    break;
                case "WARN":
                    LOGGER.warn("[Script {}] {}", ctx.getScriptId(), message);
                    break;
                case "ERROR":
                    LOGGER.error("[Script {}] {}", ctx.getScriptId(), message);
                    break;
                default:
                    LOGGER.info("[Script {}] {}", ctx.getScriptId(), message);
            }
            
            return null;
        });
        
        // random(min, max) - generates random number
        functions.put("random", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("random() requires 2 arguments: min, max");
            }
            
            double min = toDouble(args.get(0));
            double max = toDouble(args.get(1));
            
            return ThreadLocalRandom.current().nextDouble(min, max);
        });
        
        // sqrt(value) - square root
        functions.put("sqrt", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("sqrt() requires 1 argument: value");
            }
            return Math.sqrt(toDouble(args.get(0)));
        });
        
        // vec3(x, y, z) - creates a position object
        functions.put("vec3", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("vec3() requires 3 arguments: x, y, z");
            }
            Map<String, Object> vec = new HashMap<>();
            vec.put("x", toDouble(args.get(0)));
            vec.put("y", toDouble(args.get(1)));
            vec.put("z", toDouble(args.get(2)));
            return vec;
        });
        
        // sendMessage(player, message) - sends chat message to player
        functions.put("sendMessage", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("sendMessage() requires 2 arguments: player, message");
            }
            
            Object player = args.get(0);
            String message = String.valueOf(args.get(1));
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
                return true;
            }
            return false;
        });
        
        // showTitle(player, title, subtitle, fadeIn, stay, fadeOut)
        functions.put("showTitle", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("showTitle() requires at least 2 arguments: player, title");
            }
            
            Object player = args.get(0);
            String title = String.valueOf(args.get(1));
            String subtitle = args.size() > 2 ? String.valueOf(args.get(2)) : "";
            int fadeIn = args.size() > 3 ? toInt(args.get(3)) : 10;
            int stay = args.size() > 4 ? toInt(args.get(4)) : 70;
            int fadeOut = args.size() > 5 ? toInt(args.get(5)) : 20;
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
                if (!title.isEmpty()) {
                    serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                        net.minecraft.network.chat.Component.literal(title)));
                }
                if (!subtitle.isEmpty()) {
                    serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                        net.minecraft.network.chat.Component.literal(subtitle)));
                }
                return true;
            }
            return false;
        });
        
        // getPlayerPosition(player) - returns player position as {x, y, z}
        functions.put("getPlayerPosition", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("getPlayerPosition() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            
            if (serverPlayer != null) {
                Map<String, Object> pos = new HashMap<>();
                pos.put("x", serverPlayer.getX());
                pos.put("y", serverPlayer.getY());
                pos.put("z", serverPlayer.getZ());
                return pos;
            }
            return null;
        });
        
        // getPlayerVar(player, varName) - gets player-specific variable
        // Uses context's player variable store
        functions.put("getPlayerVar", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("getPlayerVar() requires 2 arguments: player, varName");
            }
            
            Object player = args.get(0);
            String varName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                // Use StorykeeRuntime's variable manager
                return ru.mjkey.storykee.runtime.StorykeeRuntime.getInstance()
                    .getVariableManager().getPlayerVariable(playerId, varName);
            }
            return null;
        });
        
        // setPlayerVar(player, varName, value) - sets player-specific variable
        functions.put("setPlayerVar", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("setPlayerVar() requires 3 arguments: player, varName, value");
            }
            
            Object player = args.get(0);
            String varName = String.valueOf(args.get(1));
            Object value = args.get(2);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                ru.mjkey.storykee.runtime.StorykeeRuntime.getInstance()
                    .getVariableManager().setPlayerVariable(playerId, varName, value);
                return true;
            }
            return false;
        });
        
        // applyEffect(player, effectId, duration, amplifier)
        functions.put("applyEffect", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("applyEffect() requires at least 3 arguments: player, effectId, duration");
            }
            
            Object player = args.get(0);
            String effectId = String.valueOf(args.get(1));
            int duration = toInt(args.get(2));
            int amplifier = args.size() > 3 ? toInt(args.get(3)) : 0;
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                net.minecraft.resources.ResourceLocation effectLoc = net.minecraft.resources.ResourceLocation.tryParse(effectId);
                if (effectLoc != null) {
                    var effectHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(effectLoc);
                    if (effectHolder.isPresent()) {
                        serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            effectHolder.get(), duration, amplifier));
                        return true;
                    }
                }
            }
            return false;
        });
        
        // playSound(player, soundId, volume, pitch)
        functions.put("playSound", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("playSound() requires at least 2 arguments: player, soundId");
            }
            
            Object player = args.get(0);
            String soundId = String.valueOf(args.get(1));
            float volume = 1.0f;
            float pitch = 1.0f;
            
            // Safely parse volume and pitch
            if (args.size() > 2) {
                try {
                    volume = (float) toDouble(args.get(2));
                } catch (Exception e) {
                    LOGGER.warn("playSound: Invalid volume argument, using default 1.0");
                }
            }
            if (args.size() > 3) {
                try {
                    pitch = (float) toDouble(args.get(3));
                } catch (Exception e) {
                    LOGGER.warn("playSound: Invalid pitch argument, using default 1.0");
                }
            }
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                net.minecraft.resources.ResourceLocation soundLoc = net.minecraft.resources.ResourceLocation.tryParse(soundId);
                if (soundLoc != null) {
                    var soundHolder = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(soundLoc);
                    if (soundHolder.isPresent()) {
                        serverPlayer.playNotifySound(soundHolder.get().value(), 
                            net.minecraft.sounds.SoundSource.MASTER, volume, pitch);
                        return true;
                    } else {
                        LOGGER.warn("playSound: Sound '{}' not found in registry", soundId);
                    }
                }
            }
            return false;
        });
        
        // playMusic(player, musicId, loop, fadeIn) - simplified, just plays sound
        functions.put("playMusic", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("playMusic() requires at least 2 arguments: player, musicId");
            }
            
            Object player = args.get(0);
            String musicId = String.valueOf(args.get(1));
            
            // For now, just log - full music system would need client-side handling
            LOGGER.info("playMusic: {} for player {} (music system not fully implemented)", musicId, player);
            return true;
        });
        
        // giveItem(player, itemId, count)
        functions.put("giveItem", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("giveItem() requires at least 2 arguments: player, itemId");
            }
            
            Object player = args.get(0);
            String itemId = String.valueOf(args.get(1));
            int count = args.size() > 2 ? toInt(args.get(2)) : 1;
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                net.minecraft.resources.ResourceLocation itemLoc = net.minecraft.resources.ResourceLocation.tryParse(itemId);
                if (itemLoc != null) {
                    net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(itemLoc);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) {
                        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, count);
                        boolean added = serverPlayer.getInventory().add(stack);
                        if (!added) {
                            // Drop item if inventory full
                            serverPlayer.drop(stack, false);
                        }
                        LOGGER.info("giveItem: Gave {} x{} to player {}", itemId, count, serverPlayer.getName().getString());
                        return true;
                    } else {
                        LOGGER.warn("giveItem: Item '{}' not found", itemId);
                    }
                }
            }
            return false;
        });
        
        // addExperience(player, amount)
        functions.put("addExperience", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("addExperience() requires 2 arguments: player, amount");
            }
            
            Object player = args.get(0);
            int amount = toInt(args.get(1));
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                serverPlayer.giveExperiencePoints(amount);
                return true;
            }
            return false;
        });
        
        // spawnParticles(particleType, x, y, z, count, spread)
        functions.put("spawnParticles", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("spawnParticles() requires at least 4 arguments: particleType, x, y, z");
            }
            
            String particleType = String.valueOf(args.get(0));
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            int count = args.size() > 4 ? toInt(args.get(4)) : 1;
            double spread = args.size() > 5 ? toDouble(args.get(5)) : 0.5;
            
            // Get world from WorldModifier
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world != null) {
                ru.mjkey.storykee.systems.particle.ParticleProperties props = new ru.mjkey.storykee.systems.particle.ParticleProperties()
                    .setCount(count)
                    .setSpread(spread, spread, spread);
                ru.mjkey.storykee.systems.particle.ParticleManager.getInstance()
                    .spawnParticles(world, particleType, new net.minecraft.world.phys.Vec3(x, y, z), props);
                return true;
            }
            return false;
        });
        
        // createHologram(id, x, y, z, lines)
        functions.put("createHologram", (args, ctx) -> {
            if (args.size() < 5) {
                throw new IllegalArgumentException("createHologram() requires 5 arguments: id, x, y, z, lines");
            }
            
            String id = String.valueOf(args.get(0));
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            
            @SuppressWarnings("unchecked")
            List<Object> linesList = (List<Object>) args.get(4);
            List<String> lines = new ArrayList<>();
            for (Object line : linesList) {
                lines.add(String.valueOf(line));
            }
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world != null) {
                ru.mjkey.storykee.systems.hologram.HologramManager.getInstance()
                    .createHologram(id, world, new net.minecraft.world.phys.Vec3(x, y, z), lines);
            }
            return id;
        });
        
        // spawnFirework(x, y, z, properties)
        functions.put("spawnFirework", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("spawnFirework() requires at least 3 arguments: x, y, z");
            }
            
            double x = toDouble(args.get(0));
            double y = toDouble(args.get(1));
            double z = toDouble(args.get(2));
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world != null) {
                // Create firework entity
                net.minecraft.world.item.ItemStack fireworkStack = new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.FIREWORK_ROCKET);
                net.minecraft.world.entity.projectile.FireworkRocketEntity firework = 
                    new net.minecraft.world.entity.projectile.FireworkRocketEntity(world, x, y, z, fireworkStack);
                world.addFreshEntity(firework);
                return true;
            }
            return false;
        });
        
        // spawnBoss(id, properties) - simplified, logs warning for now
        functions.put("spawnBoss", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("spawnBoss() requires 2 arguments: id, properties");
            }
            
            String id = String.valueOf(args.get(0));
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) args.get(1);
            
            // TODO: Implement full boss spawning with BossProperties conversion
            LOGGER.warn("spawnBoss: Boss system spawn from script not fully implemented yet for boss '{}'", id);
            return id;
        });
        
        // showBossBar(player, bossId, name, color) - simplified
        functions.put("showBossBar", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("showBossBar() requires at least 3 arguments: player, bossId, name");
            }
            
            Object player = args.get(0);
            String bossId = String.valueOf(args.get(1));
            String name = String.valueOf(args.get(2));
            String color = args.size() > 3 ? String.valueOf(args.get(3)) : "red";
            
            // TODO: Implement boss bar display
            LOGGER.warn("showBossBar: Boss bar display not fully implemented yet for boss '{}'", bossId);
            return true;
        });
        
        // shakeScreen(player, intensity, durationMs) - simplified
        functions.put("shakeScreen", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("shakeScreen() requires 3 arguments: player, intensity, durationMs");
            }
            
            Object player = args.get(0);
            float intensity = (float) toDouble(args.get(1));
            int durationMs = toInt(args.get(2));
            
            // TODO: Implement screen shake via network packet
            LOGGER.warn("shakeScreen: Screen shake not fully implemented yet");
            return true;
        });
        
        // startCutscene(player, properties)
        functions.put("startCutscene", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("startCutscene() requires 2 arguments: player, properties");
            }
            
            Object player = args.get(0);
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) args.get(1);
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                // Create cutscene from properties
                String cutsceneId = "script_cutscene_" + System.currentTimeMillis();
                ru.mjkey.storykee.systems.camera.Cutscene.Builder builder = 
                    new ru.mjkey.storykee.systems.camera.Cutscene.Builder(cutsceneId);
                
                // Parse waypoints
                if (props.containsKey("waypoints")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> waypoints = (List<Map<String, Object>>) props.get("waypoints");
                    for (Map<String, Object> wp : waypoints) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> posMap = (Map<String, Object>) wp.get("position");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> lookAtMap = (Map<String, Object>) wp.get("lookAt");
                        
                        double px = posMap != null ? toDouble(posMap.get("x")) : 0;
                        double py = posMap != null ? toDouble(posMap.get("y")) : 0;
                        double pz = posMap != null ? toDouble(posMap.get("z")) : 0;
                        
                        double lx = lookAtMap != null ? toDouble(lookAtMap.get("x")) : px;
                        double ly = lookAtMap != null ? toDouble(lookAtMap.get("y")) : py;
                        double lz = lookAtMap != null ? toDouble(lookAtMap.get("z")) : pz;
                        
                        // Calculate pitch and yaw from lookAt
                        double dx = lx - px;
                        double dy = ly - py;
                        double dz = lz - pz;
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
                        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                        
                        // Duration in ms -> ticks (20 ticks/sec)
                        int durationMs = wp.containsKey("duration") ? toInt(wp.get("duration")) : 2000;
                        long durationTicks = durationMs / 50;
                        
                        builder.addWaypoint(new ru.mjkey.storykee.systems.camera.CameraWaypoint(
                            new net.minecraft.world.phys.Vec3(px, py, pz),
                            pitch, yaw, durationTicks
                        ));
                    }
                }
                
                // Parse skippable property
                if (props.containsKey("skippable")) {
                    builder.skippable((Boolean) props.get("skippable"));
                }
                
                ru.mjkey.storykee.systems.camera.Cutscene cutscene = builder.build();
                ru.mjkey.storykee.systems.camera.CinematicCameraManager.getInstance()
                    .startCutscene(serverPlayer, cutscene);
                return true;
            }
            return false;
        });
        
        // spawnFirework(x, y, z, properties) - simplified
        functions.put("spawnFirework", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("spawnFirework() requires at least 3 arguments: x, y, z");
            }
            
            double x = toDouble(args.get(0));
            double y = toDouble(args.get(1));
            double z = toDouble(args.get(2));
            
            // TODO: Implement firework spawning
            LOGGER.warn("spawnFirework: Firework spawning not fully implemented yet at ({}, {}, {})", x, y, z);
            return true;
        });
        
        // addExperience(player, amount)
        functions.put("addExperience", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("addExperience() requires 2 arguments: player, amount");
            }
            
            Object player = args.get(0);
            int amount = toInt(args.get(1));
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                serverPlayer.giveExperiencePoints(amount);
                return true;
            }
            return false;
        });
    }
    
    // ===== NPC Functions =====
    
    private void registerNPCFunctions() {
        // spawnNPC(name, x, y, z, properties)
        functions.put("spawnNPC", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("spawnNPC() requires at least 4 arguments: name, x, y, z");
            }
            
            String name = String.valueOf(args.get(0));
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> additionalProps = args.size() > 4 ? (Map<String, Object>) args.get(4) : new HashMap<>();
            
            // Build NPC properties
            NPCProperties properties = new NPCProperties()
                    .name(name)
                    .position(x, y, z);
            
            // Apply additional properties from map
            if (additionalProps.containsKey("skin") || additionalProps.containsKey("skinUrl")) {
                properties.skinUrl(String.valueOf(additionalProps.getOrDefault("skin", additionalProps.get("skinUrl"))));
            }
            if (additionalProps.containsKey("dimension")) {
                properties.dimension(String.valueOf(additionalProps.get("dimension")));
            }
            if (additionalProps.containsKey("yaw")) {
                properties.rotation((float) toDouble(additionalProps.get("yaw")), properties.getPitch());
            }
            if (additionalProps.containsKey("pitch")) {
                properties.rotation(properties.getYaw(), (float) toDouble(additionalProps.get("pitch")));
            }
            if (additionalProps.containsKey("invulnerable")) {
                properties.invulnerable(Boolean.parseBoolean(String.valueOf(additionalProps.get("invulnerable"))));
            }
            
            // Create the NPC
            StoryNPC npc = NPCManager.getInstance().createNPC(name, properties, ctx.getScriptId());
            
            if (npc != null) {
                LOGGER.info("spawnNPC: Created NPC {} at ({}, {}, {})", name, x, y, z);
                return npc.getNpcId();
            } else {
                LOGGER.error("spawnNPC: Failed to create NPC {}", name);
                return null;
            }
        });
        
        // despawnNPC(name)
        functions.put("despawnNPC", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("despawnNPC() requires 1 argument: name");
            }
            
            String name = String.valueOf(args.get(0));
            
            boolean removed = NPCManager.getInstance().removeNPC(name);
            
            if (removed) {
                LOGGER.info("despawnNPC: Removed NPC {}", name);
            } else {
                LOGGER.warn("despawnNPC: NPC {} not found", name);
            }
            
            return removed;
        });
        
        // moveNPC(name, x, y, z, speed)
        functions.put("moveNPC", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("moveNPC() requires at least 4 arguments: name, x, y, z");
            }
            
            String name = String.valueOf(args.get(0));
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            double speed = args.size() > 4 ? toDouble(args.get(4)) : 1.0;
            
            boolean moved = NPCManager.getInstance().moveNPC(name, x, y, z, speed);
            
            if (moved) {
                LOGGER.info("moveNPC: Moving NPC {} to ({}, {}, {}) at speed {}", name, x, y, z, speed);
            } else {
                LOGGER.warn("moveNPC: NPC {} not found", name);
            }
            
            return moved;
        });
        
        // setNPCSkin(name, skinUrl)
        functions.put("setNPCSkin", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("setNPCSkin() requires 2 arguments: name, skinUrl");
            }
            
            String name = String.valueOf(args.get(0));
            String skinUrl = String.valueOf(args.get(1));
            
            boolean set = NPCManager.getInstance().setNPCSkin(name, skinUrl);
            
            if (set) {
                LOGGER.info("setNPCSkin: Set skin for NPC {} to {}", name, skinUrl);
            } else {
                LOGGER.warn("setNPCSkin: NPC {} not found", name);
            }
            
            return set;
        });
        
        // getNPC(name) - returns NPC properties or null
        functions.put("getNPC", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("getNPC() requires 1 argument: name");
            }
            
            String name = String.valueOf(args.get(0));
            StoryNPC npc = NPCManager.getInstance().getNPC(name);
            
            if (npc == null) {
                return null;
            }
            
            // Return NPC info as a map
            Map<String, Object> info = new HashMap<>();
            info.put("id", npc.getNpcId());
            info.put("name", npc.getCustomName() != null ? npc.getCustomName().getString() : "NPC");
            info.put("x", npc.getX());
            info.put("y", npc.getY());
            info.put("z", npc.getZ());
            info.put("skinUrl", npc.getSkinUrl());
            info.put("customData", npc.getCustomData());
            
            return info;
        });
        
        // updateNPC(name, properties) - updates NPC properties
        functions.put("updateNPC", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("updateNPC() requires 2 arguments: name, properties");
            }
            
            String name = String.valueOf(args.get(0));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> propsMap = (Map<String, Object>) args.get(1);
            
            NPCProperties properties = NPCProperties.fromMap(propsMap);
            StoryNPC npc = NPCManager.getInstance().updateNPC(name, properties);
            
            return npc != null;
        });
        
        // ===== NPC Movement Functions (Task 34) =====
        
        // setPatrolPath(npcId, waypoints, loop) - sets patrol path for NPC
        functions.put("setPatrolPath", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("setPatrolPath() requires at least 2 arguments: npcId, waypoints");
            }
            
            String npcId = String.valueOf(args.get(0));
            
            @SuppressWarnings("unchecked")
            List<Object> waypointData = (List<Object>) args.get(1);
            boolean loop = args.size() > 2 ? toBoolean(args.get(2)) : true;
            double speed = args.size() > 3 ? toDouble(args.get(3)) : 1.0;
            
            // Convert waypoint data to PatrolWaypoint list
            List<ru.mjkey.storykee.systems.npc.NPCMovementController.PatrolWaypoint> waypoints = new ArrayList<>();
            for (Object wp : waypointData) {
                if (wp instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> wpMap = (Map<String, Object>) wp;
                    double x = toDouble(wpMap.get("x"));
                    double y = toDouble(wpMap.get("y"));
                    double z = toDouble(wpMap.get("z"));
                    long pauseTicks = wpMap.containsKey("pause") ? toLong(wpMap.get("pause")) : 0;
                    waypoints.add(new ru.mjkey.storykee.systems.npc.NPCMovementController.PatrolWaypoint(x, y, z, pauseTicks));
                } else if (wp instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> coords = (List<Object>) wp;
                    if (coords.size() >= 3) {
                        double x = toDouble(coords.get(0));
                        double y = toDouble(coords.get(1));
                        double z = toDouble(coords.get(2));
                        long pauseTicks = coords.size() > 3 ? toLong(coords.get(3)) : 0;
                        waypoints.add(new ru.mjkey.storykee.systems.npc.NPCMovementController.PatrolWaypoint(x, y, z, pauseTicks));
                    }
                }
            }
            
            if (waypoints.isEmpty()) {
                LOGGER.warn("setPatrolPath: No valid waypoints provided for NPC {}", npcId);
                return false;
            }
            
            boolean result = NPCManager.getInstance().setPatrolPath(npcId, waypoints, loop, speed);
            if (result) {
                LOGGER.info("setPatrolPath: Set {} waypoints for NPC {}, loop={}, speed={}", 
                    waypoints.size(), npcId, loop, speed);
            }
            return result;
        });
        
        // stopPatrol(npcId) - stops NPC patrol
        functions.put("stopPatrol", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("stopPatrol() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            return NPCManager.getInstance().stopPatrol(npcId);
        });
        
        // followPlayer(npcId, playerId, distance) - makes NPC follow a player
        functions.put("followPlayer", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("followPlayer() requires at least 2 arguments: npcId, playerId");
            }
            
            String npcId = String.valueOf(args.get(0));
            UUID playerId = extractPlayerId(args.get(1));
            double distance = args.size() > 2 ? toDouble(args.get(2)) : 3.0;
            double speed = args.size() > 3 ? toDouble(args.get(3)) : 1.0;
            
            if (playerId == null) {
                LOGGER.warn("followPlayer: Invalid player ID for NPC {}", npcId);
                return false;
            }
            
            boolean result = NPCManager.getInstance().followPlayer(npcId, playerId, distance, speed);
            if (result) {
                LOGGER.info("followPlayer: NPC {} now following player {} at distance {}", npcId, playerId, distance);
            }
            return result;
        });
        
        // stopFollowing(npcId) - stops NPC from following
        functions.put("stopFollowing", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("stopFollowing() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            return NPCManager.getInstance().stopFollowing(npcId);
        });
        
        // lookAtNearestPlayer(npcId, range) - makes NPC look at nearest player
        functions.put("lookAtNearestPlayer", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("lookAtNearestPlayer() requires at least 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            double range = args.size() > 1 ? toDouble(args.get(1)) : 8.0;
            
            return NPCManager.getInstance().enableLookAtNearestPlayer(npcId, range);
        });
        
        // wander(npcId, radius) - makes NPC wander randomly
        functions.put("wander", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("wander() requires at least 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            double radius = args.size() > 1 ? toDouble(args.get(1)) : 10.0;
            
            boolean result = NPCManager.getInstance().startWandering(npcId, radius);
            if (result) {
                LOGGER.info("wander: NPC {} started wandering within {} blocks", npcId, radius);
            }
            return result;
        });
        
        // stopWandering(npcId) - stops NPC wandering
        functions.put("stopWandering", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("stopWandering() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            return NPCManager.getInstance().stopWandering(npcId);
        });
        
        // returnToSpawn(npcId, speed) - makes NPC return to spawn point
        functions.put("returnToSpawn", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("returnToSpawn() requires at least 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            double speed = args.size() > 1 ? toDouble(args.get(1)) : 1.0;
            
            boolean result = NPCManager.getInstance().returnToSpawn(npcId, speed);
            if (result) {
                LOGGER.info("returnToSpawn: NPC {} returning to spawn point", npcId);
            }
            return result;
        });
        
        // stopAllMovement(npcId) - stops all NPC movement behaviors
        functions.put("stopAllMovement", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("stopAllMovement() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            return NPCManager.getInstance().stopAllMovement(npcId);
        });
    }
    
    // ===== Dialogue Functions =====
    
    private void registerDialogueFunctions() {
        // showDialogue(player, dialogueName)
        functions.put("showDialogue", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("showDialogue() requires 2 arguments: player, dialogueName");
            }
            
            Object player = args.get(0);
            String dialogueName = String.valueOf(args.get(1));
            
            LOGGER.info("showDialogue called: {} for player {}", dialogueName, player);
            
            // Get the player UUID
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean started = ru.mjkey.storykee.systems.dialogue.DialogueManager.getInstance()
                    .startDialogue(playerId, dialogueName);
                return started;
            } else {
                LOGGER.warn("showDialogue: Invalid player argument type: {}", 
                    player != null ? player.getClass().getSimpleName() : "null");
                return false;
            }
        });
        
        // closeDialogue(player)
        functions.put("closeDialogue", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("closeDialogue() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            LOGGER.info("closeDialogue called for player {}", player);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                ru.mjkey.storykee.systems.dialogue.DialogueManager.getInstance()
                    .closeDialogue(playerId);
                return true;
            } else {
                LOGGER.warn("closeDialogue: Invalid player argument type");
                return false;
            }
        });
        
        // advanceDialogue(player) - advances to next node in dialogue
        functions.put("advanceDialogue", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("advanceDialogue() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.dialogue.DialogueManager.getInstance()
                    .advanceDialogue(playerId);
            }
            return false;
        });
        
        // selectChoice(player, choiceIndex) - selects a choice in the current dialogue
        functions.put("selectChoice", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("selectChoice() requires 2 arguments: player, choiceIndex");
            }
            
            Object player = args.get(0);
            int choiceIndex = toInt(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.dialogue.DialogueManager.getInstance()
                    .selectChoice(playerId, choiceIndex);
            }
            return false;
        });
        
        // hasActiveDialogue(player) - checks if player has an active dialogue
        functions.put("hasActiveDialogue", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("hasActiveDialogue() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.dialogue.DialogueManager.getInstance()
                    .hasActiveDialogue(playerId);
            }
            return false;
        });
    }
    
    /**
     * Extracts a player UUID from various input types.
     */
    private UUID extractPlayerId(Object player) {
        if (player instanceof UUID) {
            return (UUID) player;
        } else if (player instanceof String) {
            try {
                return UUID.fromString((String) player);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        // For actual Player entities, we'd need to check the type at runtime
        // This is handled by the event system which passes UUIDs
        return null;
    }
    
    // ===== Quest Functions =====
    
    private void registerQuestFunctions() {
        // startQuest(player, questName)
        functions.put("startQuest", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("startQuest() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean started = ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .startQuest(playerId, questName);
                LOGGER.info("startQuest: {} for player {} - {}", questName, playerId, started ? "success" : "failed");
                return started;
            } else {
                LOGGER.warn("startQuest: Invalid player argument type");
                return false;
            }
        });
        
        // completeQuest(player, questName)
        functions.put("completeQuest", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("completeQuest() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean completed = ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .completeQuest(playerId, questName);
                LOGGER.info("completeQuest: {} for player {} - {}", questName, playerId, completed ? "success" : "failed");
                return completed;
            } else {
                LOGGER.warn("completeQuest: Invalid player argument type");
                return false;
            }
        });
        
        // failQuest(player, questName)
        functions.put("failQuest", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("failQuest() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean failed = ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .failQuest(playerId, questName);
                return failed;
            }
            return false;
        });
        
        // abandonQuest(player, questName)
        functions.put("abandonQuest", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("abandonQuest() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean abandoned = ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .abandonQuest(playerId, questName);
                return abandoned;
            }
            return false;
        });
        
        // updateObjective(player, questName, objectiveId, progress)
        functions.put("updateObjective", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("updateObjective() requires 4 arguments: player, questName, objectiveId, progress");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            String objectiveId = String.valueOf(args.get(2));
            int progress = toInt(args.get(3));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean updated = ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .updateObjective(playerId, questName, objectiveId, progress);
                LOGGER.debug("updateObjective: {} objective {} to {} for player {} - {}", 
                    questName, objectiveId, progress, playerId, updated ? "success" : "failed");
                return updated;
            } else {
                LOGGER.warn("updateObjective: Invalid player argument type");
                return false;
            }
        });
        
        // incrementObjective(player, questName, objectiveId)
        functions.put("incrementObjective", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("incrementObjective() requires 3 arguments: player, questName, objectiveId");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            String objectiveId = String.valueOf(args.get(2));
            int amount = args.size() > 3 ? toInt(args.get(3)) : 1;
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .incrementObjective(playerId, questName, objectiveId, amount);
            }
            return false;
        });
        
        // hasActiveQuest(player, questName)
        functions.put("hasActiveQuest", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("hasActiveQuest() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .hasActiveQuest(playerId, questName);
            }
            return false;
        });
        
        // hasCompletedQuest(player, questName)
        functions.put("hasCompletedQuest", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("hasCompletedQuest() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                    .hasCompletedQuest(playerId, questName);
            }
            return false;
        });
        
        // getQuestProgress(player, questName)
        functions.put("getQuestProgress", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("getQuestProgress() requires 2 arguments: player, questName");
            }
            
            Object player = args.get(0);
            String questName = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                ru.mjkey.storykee.systems.quest.QuestProgress progress = 
                    ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                        .getQuestProgress(playerId, questName);
                
                if (progress == null) {
                    return null;
                }
                
                // Return progress as a map
                Map<String, Object> result = new HashMap<>();
                result.put("questId", progress.getQuestId());
                result.put("status", progress.getStatus().name());
                result.put("objectiveProgress", progress.getAllObjectiveProgress());
                result.put("completedObjectives", new ArrayList<>(progress.getCompletedObjectives()));
                result.put("startTime", progress.getStartTime());
                result.put("completeTime", progress.getCompleteTime());
                result.put("completionCount", progress.getCompletionCount());
                return result;
            }
            return null;
        });
        
        // getActiveQuests(player)
        functions.put("getActiveQuests", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("getActiveQuests() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                List<ru.mjkey.storykee.systems.quest.Quest> quests = 
                    ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                        .getActiveQuests(playerId);
                
                List<String> questIds = new ArrayList<>();
                for (ru.mjkey.storykee.systems.quest.Quest quest : quests) {
                    questIds.add(quest.getId());
                }
                return questIds;
            }
            return new ArrayList<>();
        });
        
        // showActiveQuests(player) - displays active quests in chat
        functions.put("showActiveQuests", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("showActiveQuests() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            UUID playerId = extractPlayerId(player);
            
            if (serverPlayer != null && playerId != null) {
                List<ru.mjkey.storykee.systems.quest.Quest> quests = 
                    ru.mjkey.storykee.systems.quest.QuestManager.getInstance()
                        .getActiveQuests(playerId);
                
                if (quests.isEmpty()) {
                    serverPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§7У вас нет активных квестов."));
                } else {
                    serverPlayer.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("§e=== Активные квесты ==="));
                    for (ru.mjkey.storykee.systems.quest.Quest quest : quests) {
                        String title = quest.getTitle() != null ? quest.getTitle() : quest.getId();
                        serverPlayer.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal("§a• " + title));
                    }
                }
                return true;
            }
            return false;
        });
    }
    
    // ===== HUD/GUI Functions =====
    // Requirements: 9.1, 9.2, 9.3, 9.4, 9.5
    
    private void registerHUDGUIFunctions() {
        // showHUD(player, hudId, properties)
        // Requirement 9.1: WHEN a script defines a HUD element THEN the Runtime SHALL render it on the player's screen
        // Requirement 9.2: WHEN HUD properties are specified THEN the Runtime SHALL apply position, size, and texture settings
        functions.put("showHUD", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("showHUD() requires at least 2 arguments: player, hudId");
            }
            
            Object player = args.get(0);
            String hudId = String.valueOf(args.get(1));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = args.size() > 2 ? (Map<String, Object>) args.get(2) : new HashMap<>();
            
            // Validate required properties
            String elementType = String.valueOf(properties.getOrDefault("type", "text"));
            int x = toInt(properties.getOrDefault("x", 10));
            int y = toInt(properties.getOrDefault("y", 10));
            int width = toInt(properties.getOrDefault("width", 100));
            int height = toInt(properties.getOrDefault("height", 20));
            
            // Convert properties to string map for network transmission
            Map<String, String> stringProps = new HashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (entry.getValue() != null) {
                    stringProps.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            // Get the ServerPlayer and send packet
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                    .sendHUDShow(serverPlayer, hudId, elementType, x, y, width, height, stringProps);
                LOGGER.info("showHUD: Sent HUD '{}' (type={}) to player {}", hudId, elementType, serverPlayer.getName().getString());
                return true;
            } else {
                LOGGER.warn("showHUD: Could not find ServerPlayer for {}", player);
                return false;
            }
        });
        
        // hideHUD(player, hudId)
        functions.put("hideHUD", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("hideHUD() requires 2 arguments: player, hudId");
            }
            
            Object player = args.get(0);
            String hudId = String.valueOf(args.get(1));
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                    .sendHUDHide(serverPlayer, hudId);
                LOGGER.info("hideHUD: Hiding HUD '{}' for player {}", hudId, serverPlayer.getName().getString());
                return true;
            } else {
                LOGGER.warn("hideHUD: Could not find ServerPlayer for {}", player);
                return false;
            }
        });
        
        // updateHUD(player, hudId, updates)
        // Requirement 9.3: WHEN a script updates HUD content THEN the Runtime SHALL refresh the display immediately
        functions.put("updateHUD", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("updateHUD() requires 3 arguments: player, hudId, updates");
            }
            
            Object player = args.get(0);
            String hudId = String.valueOf(args.get(1));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> updates = (Map<String, Object>) args.get(2);
            
            // Convert to string map
            Map<String, String> stringUpdates = new HashMap<>();
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                if (entry.getValue() != null) {
                    stringUpdates.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                    .sendHUDUpdate(serverPlayer, hudId, stringUpdates);
                LOGGER.debug("updateHUD: Updated HUD '{}' for player {}", hudId, serverPlayer.getName().getString());
                return true;
            } else {
                LOGGER.warn("updateHUD: Could not find ServerPlayer for {}", player);
                return false;
            }
        });
        
        // clearHUD(player) or clearHUD(player, group)
        functions.put("clearHUD", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("clearHUD() requires at least 1 argument: player");
            }
            
            Object player = args.get(0);
            String group = args.size() > 1 ? String.valueOf(args.get(1)) : "";
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                    .sendHUDClear(serverPlayer, group);
                LOGGER.info("clearHUD: Cleared HUD (group='{}') for player {}", group, serverPlayer.getName().getString());
                return true;
            } else {
                LOGGER.warn("clearHUD: Could not find ServerPlayer for {}", player);
                return false;
            }
        });
        
        // openGUI(player, guiId) or openGUI(player, guiId, data)
        // Requirement 9.4: WHEN a script creates a GUI THEN the Runtime SHALL open the interface and handle player input
        functions.put("openGUI", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("openGUI() requires at least 2 arguments: player, guiId");
            }
            
            Object player = args.get(0);
            String guiId = String.valueOf(args.get(1));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = args.size() > 2 ? (Map<String, Object>) args.get(2) : new HashMap<>();
            
            // Convert to string map
            Map<String, String> stringData = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() != null) {
                    stringData.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                // Track the GUI on server side
                ru.mjkey.storykee.systems.gui.GUIManager.getInstance()
                    .openGUI(serverPlayer.getUUID(), guiId);
                
                // Send packet to client
                ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                    .sendGUIOpen(serverPlayer, guiId, stringData);
                LOGGER.info("openGUI: Opened GUI '{}' for player {}", guiId, serverPlayer.getName().getString());
                return true;
            } else {
                LOGGER.warn("openGUI: Could not find ServerPlayer for {}", player);
                return false;
            }
        });
        
        // closeGUI(player)
        // Requirement 9.5: WHEN a GUI is closed THEN the Runtime SHALL execute any associated cleanup actions
        functions.put("closeGUI", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("closeGUI() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            net.minecraft.server.level.ServerPlayer serverPlayer = getServerPlayer(player);
            if (serverPlayer != null) {
                // Track the GUI close on server side
                ru.mjkey.storykee.systems.gui.GUIManager.getInstance()
                    .closeGUI(serverPlayer.getUUID());
                
                // Send packet to client
                ru.mjkey.storykee.network.StoryNetworkManager.getInstance()
                    .sendGUIClose(serverPlayer);
                LOGGER.info("closeGUI: Closed GUI for player {}", serverPlayer.getName().getString());
                return true;
            } else {
                LOGGER.warn("closeGUI: Could not find ServerPlayer for {}", player);
                return false;
            }
        });
        
        // hasGUIOpen(player)
        functions.put("hasGUIOpen", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("hasGUIOpen() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            UUID playerId = extractPlayerId(player);
            
            if (playerId != null) {
                return ru.mjkey.storykee.systems.gui.GUIManager.getInstance().hasGUIOpen(playerId);
            }
            return false;
        });
    }
    
    /**
     * Gets a ServerPlayer from various input types.
     */
    private net.minecraft.server.level.ServerPlayer getServerPlayer(Object player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer) {
            return (net.minecraft.server.level.ServerPlayer) player;
        }
        
        UUID playerId = extractPlayerId(player);
        if (playerId != null) {
            // Try to get from server via WorldModifier (which has server access)
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world != null && world.getServer() != null) {
                return world.getServer().getPlayerList().getPlayer(playerId);
            }
        }
        
        return null;
    }
    
    // ===== World Functions =====
    
    private void registerWorldFunctions() {
        // setBlock(x, y, z, blockType)
        // Requirement 21.1: WHEN a script places a block THEN the Runtime SHALL set the block at the specified coordinates
        functions.put("setBlock", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("setBlock() requires 4 arguments: x, y, z, blockType");
            }
            
            int x = toInt(args.get(0));
            int y = toInt(args.get(1));
            int z = toInt(args.get(2));
            String blockType = String.valueOf(args.get(3));
            
            // Get the world from context or use overworld
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("setBlock: No world available");
                return false;
            }
            
            boolean result = ru.mjkey.storykee.systems.world.WorldModifier.getInstance()
                .setBlock(world, x, y, z, blockType);
            
            LOGGER.debug("setBlock: {} at ({}, {}, {}) - {}", blockType, x, y, z, result ? "success" : "failed");
            return result;
        });
        
        // getBlock(x, y, z)
        functions.put("getBlock", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("getBlock() requires 3 arguments: x, y, z");
            }
            
            int x = toInt(args.get(0));
            int y = toInt(args.get(1));
            int z = toInt(args.get(2));
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("getBlock: No world available");
                return "minecraft:air";
            }
            
            String blockId = ru.mjkey.storykee.systems.world.WorldModifier.getInstance()
                .getBlockId(world, x, y, z);
            
            LOGGER.debug("getBlock: {} at ({}, {}, {})", blockId, x, y, z);
            return blockId;
        });
        
        // removeBlock(x, y, z, dropItems)
        // Requirement 21.2: WHEN a script removes a block THEN the Runtime SHALL break the block and drop items according to game rules
        functions.put("removeBlock", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("removeBlock() requires at least 3 arguments: x, y, z");
            }
            
            int x = toInt(args.get(0));
            int y = toInt(args.get(1));
            int z = toInt(args.get(2));
            boolean dropItems = args.size() > 3 ? Boolean.parseBoolean(String.valueOf(args.get(3))) : true;
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("removeBlock: No world available");
                return false;
            }
            
            boolean result = ru.mjkey.storykee.systems.world.WorldModifier.getInstance()
                .removeBlock(world, x, y, z, dropItems);
            
            LOGGER.debug("removeBlock: ({}, {}, {}) dropItems={} - {}", x, y, z, dropItems, result ? "success" : "failed");
            return result;
        });
        
        // fillRegion(x1, y1, z1, x2, y2, z2, blockType)
        // Requirement 21.3: WHEN a script modifies terrain THEN the Runtime SHALL update chunk data and notify clients
        functions.put("fillRegion", (args, ctx) -> {
            if (args.size() < 7) {
                throw new IllegalArgumentException("fillRegion() requires 7 arguments: x1, y1, z1, x2, y2, z2, blockType");
            }
            
            int x1 = toInt(args.get(0));
            int y1 = toInt(args.get(1));
            int z1 = toInt(args.get(2));
            int x2 = toInt(args.get(3));
            int y2 = toInt(args.get(4));
            int z2 = toInt(args.get(5));
            String blockType = String.valueOf(args.get(6));
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("fillRegion: No world available");
                return 0;
            }
            
            int count = ru.mjkey.storykee.systems.world.WorldModifier.getInstance()
                .fillRegion(world, x1, y1, z1, x2, y2, z2, blockType);
            
            LOGGER.debug("fillRegion: {} from ({}, {}, {}) to ({}, {}, {}) - {} blocks changed", 
                blockType, x1, y1, z1, x2, y2, z2, count);
            return count;
        });
        
        // placeStructure(x, y, z, structureId)
        // Requirement 21.4: WHEN a script spawns structures THEN the Runtime SHALL load them from schematic files in assets
        functions.put("placeStructure", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("placeStructure() requires 4 arguments: x, y, z, structureId");
            }
            
            int x = toInt(args.get(0));
            int y = toInt(args.get(1));
            int z = toInt(args.get(2));
            String structureId = String.valueOf(args.get(3));
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("placeStructure: No world available");
                return false;
            }
            
            boolean result = ru.mjkey.storykee.systems.world.StructureLoader.getInstance()
                .placeStructure(world, new net.minecraft.core.BlockPos(x, y, z), structureId);
            
            LOGGER.debug("placeStructure: {} at ({}, {}, {}) - {}", structureId, x, y, z, result ? "success" : "failed");
            return result;
        });
        
        // clearRegion(x1, y1, z1, x2, y2, z2)
        functions.put("clearRegion", (args, ctx) -> {
            if (args.size() < 6) {
                throw new IllegalArgumentException("clearRegion() requires 6 arguments: x1, y1, z1, x2, y2, z2");
            }
            
            int x1 = toInt(args.get(0));
            int y1 = toInt(args.get(1));
            int z1 = toInt(args.get(2));
            int x2 = toInt(args.get(3));
            int y2 = toInt(args.get(4));
            int z2 = toInt(args.get(5));
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("clearRegion: No world available");
                return 0;
            }
            
            int count = ru.mjkey.storykee.systems.world.WorldModifier.getInstance()
                .clearRegion(world, new net.minecraft.core.BlockPos(x1, y1, z1), 
                            new net.minecraft.core.BlockPos(x2, y2, z2));
            
            LOGGER.debug("clearRegion: from ({}, {}, {}) to ({}, {}, {}) - {} blocks cleared", 
                x1, y1, z1, x2, y2, z2, count);
            return count;
        });
        
        // replaceRegion(x1, y1, z1, x2, y2, z2, oldBlockType, newBlockType)
        functions.put("replaceRegion", (args, ctx) -> {
            if (args.size() < 8) {
                throw new IllegalArgumentException("replaceRegion() requires 8 arguments: x1, y1, z1, x2, y2, z2, oldBlockType, newBlockType");
            }
            
            int x1 = toInt(args.get(0));
            int y1 = toInt(args.get(1));
            int z1 = toInt(args.get(2));
            int x2 = toInt(args.get(3));
            int y2 = toInt(args.get(4));
            int z2 = toInt(args.get(5));
            String oldBlockType = String.valueOf(args.get(6));
            String newBlockType = String.valueOf(args.get(7));
            
            net.minecraft.server.level.ServerLevel world = ru.mjkey.storykee.systems.world.WorldModifier.getInstance().getOverworld();
            if (world == null) {
                LOGGER.warn("replaceRegion: No world available");
                return 0;
            }
            
            int count = ru.mjkey.storykee.systems.world.WorldModifier.getInstance()
                .replaceRegion(world, new net.minecraft.core.BlockPos(x1, y1, z1), 
                              new net.minecraft.core.BlockPos(x2, y2, z2), oldBlockType, newBlockType);
            
            LOGGER.debug("replaceRegion: {} -> {} from ({}, {}, {}) to ({}, {}, {}) - {} blocks replaced", 
                oldBlockType, newBlockType, x1, y1, z1, x2, y2, z2, count);
            return count;
        });
        
        // spawnParticle(particleType, x, y, z, properties)
        functions.put("spawnParticle", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("spawnParticle() requires at least 4 arguments: particleType, x, y, z");
            }
            
            String particleType = String.valueOf(args.get(0));
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = args.size() > 4 ? (Map<String, Object>) args.get(4) : new HashMap<>();
            
            LOGGER.info("spawnParticle called: {} at ({}, {}, {})", particleType, x, y, z);
            // TODO: Implement particle spawning (task 21.2)
            LOGGER.warn("Particle system not yet implemented");
            
            return null;
        });
        
        // playSound(soundId, x, y, z, volume, pitch)
        // Requirement 13.1: Play sounds from assets/sounds directory
        // Requirement 13.2: Apply volume, pitch, and position parameters
        functions.put("playSound", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("playSound() requires at least 4 arguments: soundId, x, y, z");
            }
            
            String soundId = String.valueOf(args.get(0));
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            float volume = args.size() > 4 ? (float) toDouble(args.get(4)) : 1.0f;
            float pitch = args.size() > 5 ? (float) toDouble(args.get(5)) : 1.0f;
            
            ru.mjkey.storykee.systems.audio.AudioManager.getInstance()
                .playSound(soundId, new net.minecraft.world.phys.Vec3(x, y, z), volume, pitch);
            
            LOGGER.debug("playSound: {} at ({}, {}, {}) volume={} pitch={}", 
                soundId, x, y, z, volume, pitch);
            
            return null;
        });
        
        // playSoundForPlayer(player, soundId, volume, pitch)
        // Requirement 13.1: Play sounds for specific player
        functions.put("playSoundForPlayer", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("playSoundForPlayer() requires at least 2 arguments: player, soundId");
            }
            
            Object player = args.get(0);
            String soundId = String.valueOf(args.get(1));
            float volume = args.size() > 2 ? (float) toDouble(args.get(2)) : 1.0f;
            float pitch = args.size() > 3 ? (float) toDouble(args.get(3)) : 1.0f;
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                ru.mjkey.storykee.systems.audio.AudioManager.getInstance()
                    .playSoundForPlayer(playerId, soundId, volume, pitch);
                return true;
            } else {
                LOGGER.warn("playSoundForPlayer: Invalid player argument type");
                return false;
            }
        });
        
        // playMusic(player, musicId, loop, fadeInSeconds)
        // Requirement 13.3: Fade out previous music and fade in new track
        functions.put("playMusic", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("playMusic() requires at least 2 arguments: player, musicId");
            }
            
            Object player = args.get(0);
            String musicId = String.valueOf(args.get(1));
            boolean loop = args.size() > 2 ? Boolean.parseBoolean(String.valueOf(args.get(2))) : false;
            float fadeInSeconds = args.size() > 3 ? (float) toDouble(args.get(3)) : 0.0f;
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                ru.mjkey.storykee.systems.audio.AudioManager.getInstance()
                    .playMusic(playerId, musicId, loop, fadeInSeconds);
                return true;
            } else {
                LOGGER.warn("playMusic: Invalid player argument type");
                return false;
            }
        });
        
        // stopMusic(player, fadeOutSeconds)
        // Requirement 13.4: Halt playback immediately or with fade
        functions.put("stopMusic", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("stopMusic() requires at least 1 argument: player");
            }
            
            Object player = args.get(0);
            float fadeOutSeconds = args.size() > 1 ? (float) toDouble(args.get(1)) : 0.0f;
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                ru.mjkey.storykee.systems.audio.AudioManager.getInstance()
                    .stopMusic(playerId, fadeOutSeconds);
                return true;
            } else {
                LOGGER.warn("stopMusic: Invalid player argument type");
                return false;
            }
        });
        
        // setMasterVolume(volume)
        // Requirement 13.4: Volume control
        functions.put("setMasterVolume", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("setMasterVolume() requires 1 argument: volume");
            }
            
            float volume = (float) toDouble(args.get(0));
            ru.mjkey.storykee.systems.audio.AudioManager.getInstance().setMasterVolume(volume);
            return null;
        });
        
        // setSoundVolume(volume)
        // Requirement 13.4: Volume control
        functions.put("setSoundVolume", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("setSoundVolume() requires 1 argument: volume");
            }
            
            float volume = (float) toDouble(args.get(0));
            ru.mjkey.storykee.systems.audio.AudioManager.getInstance().setSoundVolume(volume);
            return null;
        });
        
        // setMusicVolume(volume)
        // Requirement 13.4: Volume control
        functions.put("setMusicVolume", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("setMusicVolume() requires 1 argument: volume");
            }
            
            float volume = (float) toDouble(args.get(0));
            ru.mjkey.storykee.systems.audio.AudioManager.getInstance().setMusicVolume(volume);
            return null;
        });
        
        // getMasterVolume()
        functions.put("getMasterVolume", (args, ctx) -> {
            return ru.mjkey.storykee.systems.audio.AudioManager.getInstance().getMasterVolume();
        });
        
        // getSoundVolume()
        functions.put("getSoundVolume", (args, ctx) -> {
            return ru.mjkey.storykee.systems.audio.AudioManager.getInstance().getSoundVolume();
        });
        
        // getMusicVolume()
        functions.put("getMusicVolume", (args, ctx) -> {
            return ru.mjkey.storykee.systems.audio.AudioManager.getInstance().getMusicVolume();
        });
        
        // isMusicPlaying(player)
        functions.put("isMusicPlaying", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("isMusicPlaying() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.audio.AudioManager.getInstance()
                    .isMusicPlaying(playerId);
            }
            return false;
        });
        
        // stopAllAudio()
        functions.put("stopAllAudio", (args, ctx) -> {
            ru.mjkey.storykee.systems.audio.AudioManager.getInstance().stopAllAudio();
            return null;
        });
    }
    
    // ===== Player Functions =====
    
    private void registerPlayerFunctions() {
        // teleport(player, x, y, z, dimension)
        functions.put("teleport", (args, ctx) -> {
            if (args.size() < 4) {
                throw new IllegalArgumentException("teleport() requires at least 4 arguments: player, x, y, z");
            }
            
            Object player = args.get(0);
            double x = toDouble(args.get(1));
            double y = toDouble(args.get(2));
            double z = toDouble(args.get(3));
            String dimension = args.size() > 4 ? String.valueOf(args.get(4)) : "minecraft:overworld";
            
            LOGGER.info("teleport called: player {} to ({}, {}, {}) in {}", player, x, y, z, dimension);
            // TODO: Implement teleportation (task 21.5)
            LOGGER.warn("Teleportation system not yet implemented");
            
            return null;
        });
        
        // giveItem(player, itemId, count, properties)
        // Requirement 22.1: WHEN a script gives an item THEN the Runtime SHALL add it to the player's inventory
        // Requirement 22.4: WHEN a script creates a custom item THEN the Runtime SHALL apply NBT data, lore, and enchantments
        // Requirement 22.5: WHEN inventory is full THEN the Runtime SHALL drop excess items at the player's location
        functions.put("giveItem", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("giveItem() requires at least 3 arguments: player, itemId, count");
            }
            
            Object player = args.get(0);
            String itemId = String.valueOf(args.get(1));
            int count = toInt(args.get(2));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = args.size() > 3 ? (Map<String, Object>) args.get(3) : null;
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                int given = ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .giveItem(playerId, itemId, count, properties);
                LOGGER.info("giveItem: Gave {} x{} to player {} (requested: {})", itemId, given, playerId, count);
                return given;
            } else {
                LOGGER.warn("giveItem: Invalid player argument type");
                return 0;
            }
        });
        
        // removeItem(player, itemId, count)
        // Requirement 22.2: WHEN a script removes an item THEN the Runtime SHALL search the inventory and remove the specified quantity
        functions.put("removeItem", (args, ctx) -> {
            if (args.size() < 3) {
                throw new IllegalArgumentException("removeItem() requires 3 arguments: player, itemId, count");
            }
            
            Object player = args.get(0);
            String itemId = String.valueOf(args.get(1));
            int count = toInt(args.get(2));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                int removed = ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .removeItem(playerId, itemId, count);
                LOGGER.info("removeItem: Removed {} x{} from player {} (requested: {})", itemId, removed, playerId, count);
                return removed;
            } else {
                LOGGER.warn("removeItem: Invalid player argument type");
                return 0;
            }
        });
        
        // hasItem(player, itemId, count)
        // Requirement 22.3: WHEN a script checks for an item THEN the Runtime SHALL return the quantity found in the inventory
        functions.put("hasItem", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("hasItem() requires at least 2 arguments: player, itemId");
            }
            
            Object player = args.get(0);
            String itemId = String.valueOf(args.get(1));
            int count = args.size() > 2 ? toInt(args.get(2)) : 1;
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                boolean hasItem = ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .hasItem(playerId, itemId, count);
                LOGGER.debug("hasItem: Player {} has {} x{}: {}", playerId, count, itemId, hasItem);
                return hasItem;
            } else {
                LOGGER.warn("hasItem: Invalid player argument type");
                return false;
            }
        });
        
        // countItem(player, itemId)
        // Requirement 22.3: WHEN a script checks for an item THEN the Runtime SHALL return the quantity found in the inventory
        functions.put("countItem", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("countItem() requires 2 arguments: player, itemId");
            }
            
            Object player = args.get(0);
            String itemId = String.valueOf(args.get(1));
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                int count = ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .countItem(playerId, itemId);
                LOGGER.debug("countItem: Player {} has {} x{}", playerId, count, itemId);
                return count;
            } else {
                LOGGER.warn("countItem: Invalid player argument type");
                return 0;
            }
        });
        
        // clearInventory(player)
        functions.put("clearInventory", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("clearInventory() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                int cleared = ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .clearInventory(playerId);
                LOGGER.info("clearInventory: Cleared {} items from player {}", cleared, playerId);
                return cleared;
            } else {
                LOGGER.warn("clearInventory: Invalid player argument type");
                return 0;
            }
        });
        
        // isInventoryFull(player)
        // Requirement 22.5: Check if inventory is full before giving items
        functions.put("isInventoryFull", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("isInventoryFull() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .isInventoryFull(playerId);
            } else {
                LOGGER.warn("isInventoryFull: Invalid player argument type");
                return false;
            }
        });
        
        // getEmptySlotCount(player)
        functions.put("getEmptySlotCount", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("getEmptySlotCount() requires 1 argument: player");
            }
            
            Object player = args.get(0);
            
            UUID playerId = extractPlayerId(player);
            if (playerId != null) {
                return ru.mjkey.storykee.systems.inventory.InventoryManager.getInstance()
                    .getEmptySlotCount(playerId);
            } else {
                LOGGER.warn("getEmptySlotCount: Invalid player argument type");
                return 0;
            }
        });
        
        // setHealth(player, health)
        functions.put("setHealth", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("setHealth() requires 2 arguments: player, health");
            }
            
            Object player = args.get(0);
            double health = toDouble(args.get(1));
            
            LOGGER.info("setHealth called: {} for player {}", health, player);
            // TODO: Implement health setting (task 21.7)
            LOGGER.warn("Health system not yet implemented");
            
            return null;
        });
        
        // giveExperience(player, amount)
        functions.put("giveExperience", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("giveExperience() requires 2 arguments: player, amount");
            }
            
            Object player = args.get(0);
            int amount = toInt(args.get(1));
            
            LOGGER.info("giveExperience called: {} for player {}", amount, player);
            // TODO: Implement experience giving (task 21.8)
            LOGGER.warn("Experience system not yet implemented");
            
            return null;
        });
    }
    
    // ===== Timer Functions =====
    
    private void registerTimerFunctions() {
        // scheduleDelayed(delayTicks, callback, ...args)
        // Requirement 14.1: WHEN a script schedules a delayed action THEN the Runtime SHALL execute it after the specified time
        functions.put("scheduleDelayed", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("scheduleDelayed() requires at least 2 arguments: delayTicks, callback");
            }
            
            long delayTicks = toLong(args.get(0));
            Object callback = args.get(1);
            
            // Collect additional arguments to pass to the callback
            List<Object> callbackArgs = args.size() > 2 ? args.subList(2, args.size()) : Collections.emptyList();
            
            // Create a runnable from the callback with arguments
            Runnable task = createTaskFromCallbackWithArgs(callback, callbackArgs, ctx);
            
            UUID timerId = ru.mjkey.storykee.systems.timer.TimerManager.getInstance()
                .scheduleDelayed(task, delayTicks);
            
            if (timerId != null) {
                LOGGER.debug("scheduleDelayed: scheduled task {} with delay {} ticks", timerId, delayTicks);
                return timerId.toString();
            } else {
                LOGGER.warn("scheduleDelayed: failed to schedule task");
                return null;
            }
        });
        
        // scheduleRepeating(intervalTicks, callback, ...args)
        // Requirement 14.2: WHEN a script creates a repeating timer THEN the Runtime SHALL execute the action at each interval
        functions.put("scheduleRepeating", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("scheduleRepeating() requires at least 2 arguments: intervalTicks, callback");
            }
            
            long intervalTicks = toLong(args.get(0));
            Object callback = args.get(1);
            long initialDelay = args.size() > 2 && args.get(2) instanceof Number ? toLong(args.get(2)) : 0;
            
            // Create a runnable from the callback
            Runnable task = createTaskFromCallback(callback, ctx);
            
            UUID timerId = ru.mjkey.storykee.systems.timer.TimerManager.getInstance()
                .scheduleRepeating(task, intervalTicks, initialDelay);
            
            if (timerId != null) {
                LOGGER.debug("scheduleRepeating: scheduled task {} with interval {} ticks", timerId, intervalTicks);
                return timerId.toString();
            } else {
                LOGGER.warn("scheduleRepeating: failed to schedule task");
                return null;
            }
        });
        
        // cancelTimer(timerId)
        // Requirement 14.3: WHEN a timer is cancelled THEN the Runtime SHALL prevent future executions
        functions.put("cancelTimer", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("cancelTimer() requires 1 argument: timerId");
            }
            
            String timerIdStr = String.valueOf(args.get(0));
            
            try {
                UUID timerId = UUID.fromString(timerIdStr);
                boolean cancelled = ru.mjkey.storykee.systems.timer.TimerManager.getInstance()
                    .cancel(timerId);
                
                LOGGER.debug("cancelTimer: {} - {}", timerIdStr, cancelled ? "cancelled" : "not found");
                return cancelled;
            } catch (IllegalArgumentException e) {
                LOGGER.warn("cancelTimer: invalid timer ID format: {}", timerIdStr);
                return false;
            }
        });
        
        // pauseTimers() - pauses all story timers
        // Requirement 14.4: WHEN the game is paused THEN the Runtime SHALL pause all story timers
        functions.put("pauseTimers", (args, ctx) -> {
            ru.mjkey.storykee.systems.timer.TimerManager.getInstance().pauseAll();
            LOGGER.debug("pauseTimers: all timers paused");
            return null;
        });
        
        // resumeTimers() - resumes all story timers
        // Requirement 14.4: WHEN the game is paused THEN the Runtime SHALL pause all story timers
        functions.put("resumeTimers", (args, ctx) -> {
            ru.mjkey.storykee.systems.timer.TimerManager.getInstance().resumeAll();
            LOGGER.debug("resumeTimers: all timers resumed");
            return null;
        });
        
        // hasTimer(timerId) - checks if a timer exists
        functions.put("hasTimer", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("hasTimer() requires 1 argument: timerId");
            }
            
            String timerIdStr = String.valueOf(args.get(0));
            
            try {
                UUID timerId = UUID.fromString(timerIdStr);
                return ru.mjkey.storykee.systems.timer.TimerManager.getInstance()
                    .hasTimer(timerId);
            } catch (IllegalArgumentException e) {
                return false;
            }
        });
        
        // getRemainingTicks(timerId) - gets remaining ticks until timer executes
        functions.put("getRemainingTicks", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("getRemainingTicks() requires 1 argument: timerId");
            }
            
            String timerIdStr = String.valueOf(args.get(0));
            
            try {
                UUID timerId = UUID.fromString(timerIdStr);
                return ru.mjkey.storykee.systems.timer.TimerManager.getInstance()
                    .getRemainingTicks(timerId);
            } catch (IllegalArgumentException e) {
                return -1L;
            }
        });
    }
    
    // ===== Animation Functions (Requirements: 10.2, 10.3) =====
    
    private void registerAnimationFunctions() {
        // playAnimation(npcId, animationId, loop, callback)
        // Requirement 10.2: WHEN a script plays an animation THEN the Runtime SHALL apply it to the target entity
        functions.put("playAnimation", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("playAnimation() requires at least 2 arguments: npcId, animationId");
            }
            
            String npcId = String.valueOf(args.get(0));
            String animationId = String.valueOf(args.get(1));
            boolean loop = args.size() > 2 && Boolean.parseBoolean(String.valueOf(args.get(2)));
            
            // Get callback if provided
            Runnable callback = null;
            if (args.size() > 3 && args.get(3) != null) {
                callback = createTaskFromCallback(args.get(3), ctx);
            }
            
            StoryNPC npc = NPCManager.getInstance().getNPC(npcId);
            if (npc == null) {
                LOGGER.warn("playAnimation: NPC '{}' not found", npcId);
                return false;
            }
            
            boolean started = npc.playAnimation(animationId, loop, callback);
            
            if (started) {
                LOGGER.info("playAnimation: Started '{}' on NPC '{}' (loop={})", animationId, npcId, loop);
                
                // Broadcast animation to clients
                NPCManager.getInstance().broadcastNPCAnimation(npcId, animationId, loop);
            } else {
                LOGGER.warn("playAnimation: Failed to start '{}' on NPC '{}'", animationId, npcId);
            }
            
            return started;
        });
        
        // stopAnimation(npcId)
        // Requirement 10.4: WHEN an animation is interrupted THEN the Runtime SHALL stop it and restore the default state
        functions.put("stopAnimation", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("stopAnimation() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            
            StoryNPC npc = NPCManager.getInstance().getNPC(npcId);
            if (npc == null) {
                LOGGER.warn("stopAnimation: NPC '{}' not found", npcId);
                return false;
            }
            
            if (npc.isAnimating()) {
                npc.stopAnimation();
                LOGGER.info("stopAnimation: Stopped animation on NPC '{}'", npcId);
                
                // Broadcast stop to clients
                NPCManager.getInstance().broadcastNPCAnimationStop(npcId);
                return true;
            }
            
            return false;
        });
        
        // isAnimating(npcId)
        functions.put("isAnimating", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("isAnimating() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            
            StoryNPC npc = NPCManager.getInstance().getNPC(npcId);
            if (npc == null) {
                return false;
            }
            
            return npc.isAnimating();
        });
        
        // registerAnimation(animationId, frames) - for custom animations
        // Requirement 10.1: WHEN an animation is defined THEN the Runtime SHALL load the animation data from assets
        functions.put("registerAnimation", (args, ctx) -> {
            if (args.size() < 2) {
                throw new IllegalArgumentException("registerAnimation() requires 2 arguments: animationId, frames");
            }
            
            String animationId = String.valueOf(args.get(0));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> framesList = (List<Map<String, Object>>) args.get(1);
            
            // Optional parameters
            float duration = args.size() > 2 ? (float) toDouble(args.get(2)) : 1.0f;
            boolean loop = args.size() > 3 && Boolean.parseBoolean(String.valueOf(args.get(3)));
            
            // Create animation
            ru.mjkey.storykee.systems.animation.Animation animation = 
                new ru.mjkey.storykee.systems.animation.Animation(animationId, duration, loop);
            
            // Add frames
            int frameNum = 0;
            for (Map<String, Object> frameData : framesList) {
                float timestamp = frameData.containsKey("time") ? 
                    (float) toDouble(frameData.get("time")) : 
                    (frameNum * duration / Math.max(1, framesList.size() - 1));
                
                ru.mjkey.storykee.systems.animation.AnimationFrame frame = 
                    new ru.mjkey.storykee.systems.animation.AnimationFrame(frameNum, timestamp);
                
                // Parse bone transforms
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> bones = 
                    (Map<String, Map<String, Object>>) frameData.get("bones");
                
                if (bones != null) {
                    for (Map.Entry<String, Map<String, Object>> boneEntry : bones.entrySet()) {
                        String boneName = boneEntry.getKey();
                        Map<String, Object> transformData = boneEntry.getValue();
                        
                        float pitch = transformData.containsKey("pitch") ? 
                            (float) toDouble(transformData.get("pitch")) : 0f;
                        float yaw = transformData.containsKey("yaw") ? 
                            (float) toDouble(transformData.get("yaw")) : 0f;
                        float roll = transformData.containsKey("roll") ? 
                            (float) toDouble(transformData.get("roll")) : 0f;
                        
                        ru.mjkey.storykee.systems.animation.Transform transform = 
                            ru.mjkey.storykee.systems.animation.Transform.fromRotation(pitch, yaw, roll);
                        
                        frame.setBoneTransform(boneName, transform);
                    }
                }
                
                animation.addFrame(frame);
                frameNum++;
            }
            
            // Register the animation
            boolean registered = ru.mjkey.storykee.systems.animation.AnimationManager.getInstance()
                .registerAnimation(animation);
            
            if (registered) {
                LOGGER.info("registerAnimation: Registered custom animation '{}'", animationId);
            } else {
                LOGGER.warn("registerAnimation: Failed to register '{}' (may already exist)", animationId);
            }
            
            return registered;
        });
        
        // getAnimationList() - returns list of available animation IDs
        functions.put("getAnimationList", (args, ctx) -> {
            return new ArrayList<>(
                ru.mjkey.storykee.systems.animation.AnimationManager.getInstance().getAnimationIds()
            );
        });
        
        // getCurrentAnimation(npcId) - returns current animation ID or null
        functions.put("getCurrentAnimation", (args, ctx) -> {
            if (args.isEmpty()) {
                throw new IllegalArgumentException("getCurrentAnimation() requires 1 argument: npcId");
            }
            
            String npcId = String.valueOf(args.get(0));
            
            StoryNPC npc = NPCManager.getInstance().getNPC(npcId);
            if (npc == null) {
                return null;
            }
            
            return npc.getCurrentAnimationId();
        });
    }
    
    /**
     * Creates a Runnable task from a callback object.
     * The callback can be a Runnable, a function name string, or a script block.
     */
    private Runnable createTaskFromCallback(Object callback, ExecutionContext ctx) {
        return createTaskFromCallbackWithArgs(callback, Collections.emptyList(), ctx);
    }
    
    /**
     * Creates a Runnable task from a callback object with arguments.
     * The callback can be a UserFunction, a function name string, or a Runnable.
     */
    private Runnable createTaskFromCallbackWithArgs(Object callback, List<Object> args, ExecutionContext ctx) {
        if (callback instanceof Runnable) {
            return (Runnable) callback;
        } else if (callback instanceof String) {
            // Callback is a function name - call it when the timer fires
            String functionName = (String) callback;
            return () -> {
                try {
                    if (hasFunction(functionName)) {
                        call(functionName, args, ctx);
                    } else {
                        LOGGER.warn("Timer callback function not found: {}", functionName);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error executing timer callback {}: {}", functionName, e.getMessage(), e);
                }
            };
        } else if (callback instanceof ASTInterpreter.ScriptCallable) {
            // UserFunction from script - call it with a new interpreter
            ASTInterpreter.ScriptCallable userFunc = (ASTInterpreter.ScriptCallable) callback;
            return () -> {
                try {
                    ASTInterpreter interpreter = new ASTInterpreter();
                    userFunc.call(args, interpreter);
                    LOGGER.debug("Executed UserFunction callback with {} args", args.size());
                } catch (Exception e) {
                    LOGGER.error("Error executing UserFunction callback: {}", e.getMessage(), e);
                }
            };
        } else {
            // For other types, log a warning and return a no-op
            LOGGER.warn("Unsupported callback type: {}", callback != null ? callback.getClass().getSimpleName() : "null");
            return () -> LOGGER.warn("Timer callback not executed - unsupported type");
        }
    }
    
    // ===== Helper Methods =====
    
    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert to number: " + value);
        }
    }
    
    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert to integer: " + value);
        }
    }
    
    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert to long: " + value);
        }
    }
    
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = String.valueOf(value).toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
}
