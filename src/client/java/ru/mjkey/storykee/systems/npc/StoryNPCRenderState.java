package ru.mjkey.storykee.systems.npc;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import ru.mjkey.storykee.systems.animation.AnimationFrame;

/**
 * Render state for StoryNPC entities.
 * Contains all the data needed to render the NPC.
 * 
 * In Minecraft 1.21.10, render states are used to decouple entity data
 * from the rendering process for better performance.
 * 
 * Requirement 6.2: Custom skin rendering and model support
 * Requirement 10.2: Animation playback support
 */
public class StoryNPCRenderState extends HumanoidRenderState {
    
    /** The skin URL or resource path for this NPC */
    public String skinUrl;
    
    /** Scale factor for rendering (default 1.0) */
    public float scale = 1.0F;
    
    /** Unique ResourceLocation for this NPC */
    public String npcId;
    
    /** Display name shown above the NPC */
    public String displayName;
    
    /** Whether to use slim arm model (Alex-style) */
    public boolean slimArms = false;
    
    /** Cached resolved texture location */
    public ResourceLocation resolvedTexture;
    
    /** Whether the NPC should glow (emissive rendering) */
    public boolean glowing = false;
    
    /** Custom tint color (ARGB format, -1 for no tint) */
    public int tintColor = -1;
    
    /** Transparency level (0.0 = invisible, 1.0 = fully opaque) */
    public float alpha = 1.0F;
    
    // Animation state (Requirements: 10.2, 10.4)
    
    /** Current animation ID being played, or null if not animating */
    public String currentAnimationId;
    
    /** Current animation time in seconds */
    public float animationTime;
    
    /** Current animation frame with bone transforms */
    public AnimationFrame currentAnimationFrame;
    
    /** Whether the NPC is currently animating */
    public boolean isAnimating = false;
    
    public StoryNPCRenderState() {
        super();
    }
}
