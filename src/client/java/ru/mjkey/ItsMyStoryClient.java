package ru.mjkey;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.network.ClientStoryStateManager;
import ru.mjkey.storykee.network.StoryNetworkClientHandler;
import ru.mjkey.storykee.systems.audio.AudioClientHandler;
import ru.mjkey.storykee.systems.boss.BossClientHandler;
import ru.mjkey.storykee.systems.dialogue.DialogueClientHandler;
import ru.mjkey.storykee.systems.gui.GUIClientHandler;
import ru.mjkey.storykee.systems.hud.HUDClientHandler;
import ru.mjkey.storykee.systems.hud.HUDRenderer;
import ru.mjkey.storykee.systems.npc.NPCClientRegistry;

public class ItsMyStoryClient implements ClientModInitializer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ItsMyStoryClient.class);
	
	// Keybinding to start/interact with story (K key)
	private static KeyMapping startStoryKey;
	
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		LOGGER.info("Initializing It's My Story client...");
		
		// Register NPC client components (renderers, models)
		// Requirement 6.2: Custom skin rendering and model support
		NPCClientRegistry.register();
		
		// Register dialogue client handlers
		// Requirement 7.1: Display dialogue interface to player
		DialogueClientHandler.register();
		
		// Register HUD client handler for network packets
		// Requirement 9.1, 9.2, 9.3: Custom HUD elements with network sync
		HUDClientHandler.register();
		
		// Register HUD renderer
		// Requirement 9.1, 9.2, 9.3: Custom HUD elements
		HUDRenderer.register();
		
		// Register GUI client handler for network packets
		// Requirement 9.4, 9.5: Custom GUI with network sync
		GUIClientHandler.register();
		
		// Register audio client handler
		// Requirement 13.1, 13.2, 13.3, 13.4: Audio playback with fading
		AudioClientHandler.register();
		
		// Register boss client handler
		// Requirement 18.2: Boss health bar display
		BossClientHandler.getInstance().register();
		
		// Register story network client handler
		// Requirements 31.1, 31.2, 31.3, 31.4: Multiplayer synchronization
		StoryNetworkClientHandler.register();
		
		// Initialize client story state manager
		// Requirements 31.3, 31.4: State synchronization
		ClientStoryStateManager.initialize();
		
		// Register keybinding for starting story (K key)
		registerKeybindings();
		
		LOGGER.info("It's My Story client initialized successfully");
	}
	
	private void registerKeybindings() {
		// Create keybinding for starting story using Fabric's KeyBindingHelper
		// In 1.21.10, KeyMapping requires a Category, so we use MISC
		startStoryKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.itsmystory.start_story",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_K,
			KeyMapping.Category.MISC
		));
		
		// Register tick event to check for key press
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (startStoryKey.consumeClick()) {
				if (client.player != null) {
					LOGGER.info("Start Story key pressed - triggering story execution");
					// Send packet to server to start story for this player
					StoryNetworkClientHandler.sendStartStoryRequest();
				}
			}
		});
		
		LOGGER.info("Registered keybinding: K to start story");
	}
}