package ru.mjkey.storykee.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge between Fabric events and the Storykee event system.
 * Registers listeners for Minecraft events and forwards them to EventManager.
 */
public class FabricEventBridge {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricEventBridge.class);
    private static FabricEventBridge instance;
    private final EventManager eventManager;
    
    private FabricEventBridge() {
        this.eventManager = EventManager.getInstance();
    }
    
    public static FabricEventBridge getInstance() {
        if (instance == null) {
            instance = new FabricEventBridge();
        }
        return instance;
    }
    
    /**
     * Register all Fabric event listeners.
     * Should be called during mod initialization.
     */
    public void registerAll() {
        LOGGER.info("Registering Fabric event listeners for Storykee");
        
        registerPlayerEvents();
        registerBlockEvents();
        registerEntityEvents();
        registerWorldEvents();
        
        LOGGER.info("Fabric event listeners registered successfully");
    }
    
    /**
     * Register player-related events.
     */
    private void registerPlayerEvents() {
        // Player join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            EventData data = new EventData("playerJoin");
            // Pass UUID as player identifier for script use
            data.set("player", player.getUUID());
            data.set("playerName", player.getName().getString());
            data.set("playerId", player.getStringUUID());
            data.set("playerEntity", player); // Keep entity reference for internal use
            // Fire both event name formats for compatibility
            eventManager.fireEvent("playerJoin", data);
            eventManager.fireEvent("onPlayerJoin", data);
        });
        
        // Player leave
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            EventData data = new EventData("playerLeave");
            data.set("player", player.getUUID());
            data.set("playerName", player.getName().getString());
            data.set("playerId", player.getStringUUID());
            data.set("playerEntity", player);
            eventManager.fireEvent("playerLeave", data);
            eventManager.fireEvent("onPlayerLeave", data);
        });
        
        // Player death
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                EventData data = new EventData("onPlayerDeath");
                data.set("player", player);
                data.set("playerName", player.getName().getString());
                data.set("playerId", player.getStringUUID());
                data.set("damageSource", damageSource.getMsgId());
                eventManager.fireEvent("onPlayerDeath", data);
            }
        });
        
        // Player respawn
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            EventData data = new EventData("onPlayerRespawn");
            data.set("player", newPlayer);
            data.set("playerName", newPlayer.getName().getString());
            data.set("playerId", newPlayer.getStringUUID());
            data.set("alive", alive);
            eventManager.fireEvent("onPlayerRespawn", data);
        });
        
        LOGGER.debug("Player events registered");
    }
    
    /**
     * Register block-related events.
     */
    private void registerBlockEvents() {
        // Block interact
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            EventData data = new EventData("onBlockInteract");
            data.set("player", player);
            data.set("world", world);
            data.set("hand", hand.toString());
            data.set("blockPos", hitResult.getBlockPos());
            data.set("direction", hitResult.getDirection().toString());
            
            eventManager.fireEvent("onBlockInteract", data);
            
            // If the event was cancelled, prevent the interaction
            return data.isCancelled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        
        LOGGER.debug("Block events registered");
    }
    
    /**
     * Register entity-related events.
     */
    private void registerEntityEvents() {
        // Player attack entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            EventData data = new EventData("onPlayerAttack");
            data.set("player", player);
            data.set("world", world);
            data.set("hand", hand.toString());
            data.set("target", entity);
            data.set("targetType", entity.getType().toString());
            
            eventManager.fireEvent("onPlayerAttack", data);
            
            // If the event was cancelled, prevent the attack
            return data.isCancelled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });
        
        // Entity death (already handled in player events for players)
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof Player)) {
                EventData data = new EventData("onEntityDeath");
                data.set("entity", entity);
                data.set("entityType", entity.getType().toString());
                data.set("damageSource", damageSource.getMsgId());
                data.set("position", entity.position());
                eventManager.fireEvent("onEntityDeath", data);
            }
        });
        
        LOGGER.debug("Entity events registered");
    }
    
    /**
     * Register world-related events.
     */
    private void registerWorldEvents() {
        // World load
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            EventData data = new EventData("onWorldLoad");
            data.set("server", server);
            eventManager.fireEvent("onWorldLoad", data);
        });
        
        // World unload
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EventData data = new EventData("onWorldUnload");
            data.set("server", server);
            eventManager.fireEvent("onWorldUnload", data);
        });
        
        LOGGER.debug("World events registered");
    }
}
