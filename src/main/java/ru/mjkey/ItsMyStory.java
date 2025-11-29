package ru.mjkey;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import ru.mjkey.storykee.events.FabricEventBridge;
import ru.mjkey.storykee.network.StoryNetworkManager;
import ru.mjkey.storykee.runtime.StorykeeRuntime;
import ru.mjkey.storykee.runtime.async.AsyncExecutor;
import ru.mjkey.storykee.runtime.async.MinecraftThreadBridge;
import ru.mjkey.storykee.runtime.hotreload.FileWatcher;
import ru.mjkey.storykee.runtime.hotreload.ScriptReloader;
import ru.mjkey.storykee.systems.dialogue.DialogueServerHandler;
import ru.mjkey.storykee.systems.npc.NPCEntityRegistry;
import ru.mjkey.storykee.systems.boss.BossEntityRegistry;
import ru.mjkey.storykee.systems.boss.BossManager;
import ru.mjkey.storykee.systems.audio.AudioManager;
import ru.mjkey.storykee.systems.timer.TimerManager;
import ru.mjkey.storykee.systems.world.WorldModifier;
import ru.mjkey.storykee.systems.inventory.InventoryManager;
import ru.mjkey.storykee.systems.camera.CinematicCameraManager;
import ru.mjkey.storykee.systems.particle.ParticleManager;
import ru.mjkey.storykee.systems.party.PartyManager;
import ru.mjkey.storykee.systems.scoreboard.ScoreboardManager;
import ru.mjkey.storykee.systems.teleport.TeleportManager;
import ru.mjkey.storykee.systems.effect.EffectManager;
import ru.mjkey.storykee.systems.health.HealthManager;
import ru.mjkey.storykee.systems.experience.ExperienceManager;
import ru.mjkey.storykee.systems.detection.DetectionManager;
import ru.mjkey.storykee.systems.hologram.HologramManager;
import ru.mjkey.storykee.systems.region.RegionManager;
import ru.mjkey.storykee.systems.npc.NPCAnimations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ItsMyStory implements ModInitializer {
	public static final String MOD_ID = "itsmystory";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing It's My Story mod...");
		
		// Register NPC entities (Requirements: 6.1, 6.2)
		NPCEntityRegistry.register();
		
		// Register standard NPC animations (Requirements: 10.1, 10.2)
		NPCAnimations.registerAll();
		
		// Register Boss entities (Requirements: 18.1, 18.2)
		BossEntityRegistry.register();
		
		// Register dialogue server handlers (Requirements: 7.1, 7.3, 7.4)
		DialogueServerHandler.getInstance().register();
		
		// Register story network manager (Requirements: 31.1, 31.2, 31.3, 31.4, 31.5)
		StoryNetworkManager.getInstance().register();
		
		// Initialize StorykeeRuntime with Minecraft game directory
		// Requirements: 1.1, 1.2, 1.3
		Path gameDir = FabricLoader.getInstance().getGameDir();
		StorykeeRuntime runtime = StorykeeRuntime.getInstance(gameDir);
		runtime.initialize();
		
		// Register Fabric event listeners for Storykee event system
		FabricEventBridge.getInstance().registerAll();
		
		// Register server lifecycle events
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LOGGER.info("Server started, initializing MinecraftThreadBridge");
			MinecraftThreadBridge.getInstance().initialize(server);
			
			// Initialize StoryNetworkManager (Requirements 31.1, 31.2, 31.3, 31.4, 31.5)
			StoryNetworkManager.getInstance().initialize(server);
			
			// Initialize TimerManager (Requirements 14.1, 14.2)
			TimerManager.getInstance().initialize(server);
			
			// Initialize AudioManager (Requirements 13.1, 13.2, 13.3, 13.4)
			AudioManager.getInstance().initialize(server);
			
			// Initialize WorldModifier (Requirements 21.1, 21.2, 21.3, 21.4, 21.5)
			WorldModifier.getInstance().setServer(server);
			
			// Initialize InventoryManager (Requirements 22.1, 22.2, 22.3, 22.4, 22.5)
			InventoryManager.getInstance().setServer(server);
			
			// Initialize BossManager (Requirements 18.1, 18.5)
			BossManager.getInstance().initialize(server);
			
			// Initialize CinematicCameraManager (Requirements 19.1, 19.2, 19.3, 19.4, 19.5)
			CinematicCameraManager.getInstance().initialize(server);
			
			// Initialize ParticleManager (Requirements 20.1, 20.2, 20.3, 20.4, 20.5)
			ParticleManager.getInstance().initialize(server);
			
			// Initialize PartyManager (Requirements 23.1, 23.2, 23.3, 23.4, 23.5)
			PartyManager.getInstance().initialize(server);
			
			// Initialize ScoreboardManager (Requirements 24.1, 24.2, 24.3, 24.4, 24.5)
			ScoreboardManager.getInstance().initialize(server);
			
			// Initialize TeleportManager (Requirements 43.1, 43.2, 43.3, 43.4, 43.5)
			TeleportManager.getInstance().initialize(server);
			
			// Initialize EffectManager (Requirements 44.1, 44.2, 44.3, 44.4, 44.5)
			EffectManager.getInstance().initialize(server);
			
			// Initialize HealthManager (Requirements 45.1, 45.2, 45.3, 45.4, 45.5)
			HealthManager.getInstance().initialize(server);
			
			// Initialize ExperienceManager (Requirements 46.1, 46.2, 46.3, 46.4, 46.5)
			ExperienceManager.getInstance().initialize(server);
			
			// Initialize DetectionManager (Requirements 47.1, 47.2, 47.3, 47.4, 47.5)
			DetectionManager.getInstance().initialize(server);
			
			// Initialize HologramManager (Requirements 48.1, 48.2, 48.3, 48.4, 48.5)
			HologramManager.getInstance().initialize(server);
			
			// Initialize RegionManager (Requirements 49.1, 49.2, 49.3, 49.4, 49.5)
			RegionManager.getInstance().initialize(server);
			
			// Initialize script reloader (Requirements 16.2-16.5)
			ScriptReloader.getInstance().initialize();
			
			// Start file watcher for hot reload (Requirement 1.4, 16.1)
			FileWatcher fileWatcher = FileWatcher.getInstance();
			fileWatcher.addWatchPath(runtime.getScriptsDirectory());
			fileWatcher.start();
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server stopping, shutting down async systems");
			
			// Shutdown StoryNetworkManager (Requirements 31.1, 31.2, 31.3, 31.4, 31.5)
			StoryNetworkManager.getInstance().shutdown();
			
			// Stop file watcher and script reloader
			FileWatcher.getInstance().stop();
			ScriptReloader.getInstance().shutdown();
			
			// Shutdown TimerManager (Requirements 14.1, 14.2, 14.3)
			TimerManager.getInstance().shutdown();
			
			// Shutdown AudioManager (Requirements 13.1, 13.2, 13.3, 13.4)
			AudioManager.getInstance().shutdown();
			
			// Shutdown WorldModifier (Requirements 21.1, 21.2, 21.3, 21.4, 21.5)
			WorldModifier.getInstance().shutdown();
			
			// Shutdown BossManager (Requirements 18.1, 18.5)
			BossManager.getInstance().shutdown();
			
			// Shutdown CinematicCameraManager (Requirements 19.1, 19.2, 19.3, 19.4, 19.5)
			CinematicCameraManager.getInstance().shutdown();
			
			// Shutdown ParticleManager (Requirements 20.1, 20.2, 20.3, 20.4, 20.5)
			ParticleManager.getInstance().shutdown();
			
			// Shutdown PartyManager (Requirements 23.1, 23.2, 23.3, 23.4, 23.5)
			PartyManager.getInstance().shutdown();
			
			// Shutdown ScoreboardManager (Requirements 24.1, 24.2, 24.3, 24.4, 24.5)
			ScoreboardManager.getInstance().shutdown();
			
			// Shutdown TeleportManager (Requirements 43.1, 43.2, 43.3, 43.4, 43.5)
			TeleportManager.getInstance().shutdown();
			
			// Shutdown EffectManager (Requirements 44.1, 44.2, 44.3, 44.4, 44.5)
			EffectManager.getInstance().shutdown();
			
			// Shutdown HealthManager (Requirements 45.1, 45.2, 45.3, 45.4, 45.5)
			HealthManager.getInstance().shutdown();
			
			// Shutdown ExperienceManager (Requirements 46.1, 46.2, 46.3, 46.4, 46.5)
			ExperienceManager.getInstance().shutdown();
			
			// Shutdown DetectionManager (Requirements 47.1, 47.2, 47.3, 47.4, 47.5)
			DetectionManager.getInstance().shutdown();
			
			// Shutdown HologramManager (Requirements 48.1, 48.2, 48.3, 48.4, 48.5)
			HologramManager.getInstance().shutdown();
			
			// Shutdown RegionManager (Requirements 49.1, 49.2, 49.3, 49.4, 49.5)
			RegionManager.getInstance().shutdown();
			
			MinecraftThreadBridge.getInstance().shutdown();
			AsyncExecutor.getInstance().shutdown();
		});
		
		// Register tick event to process queued tasks and timers
		// Requirements: 14.1, 14.2 - Timer tick processing
		// Requirements: 19.1, 19.2 - Cutscene camera updates
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MinecraftThreadBridge.getInstance().tick();
			TimerManager.getInstance().tick();
			CinematicCameraManager.getInstance().tick();
			ParticleManager.getInstance().tick();
			DetectionManager.getInstance().tick();
			RegionManager.getInstance().tick();
		});
		
		LOGGER.info("It's My Story mod initialized successfully");
	}
}