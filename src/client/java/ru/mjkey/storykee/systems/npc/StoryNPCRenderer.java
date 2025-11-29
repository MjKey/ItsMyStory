package ru.mjkey.storykee.systems.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.ItsMyStory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Renderer for StoryNPC entities.
 * Handles custom skin rendering and model display with support for:
 * - Custom skin textures from resource locations
 * - Skin caching for performance
 * - Scale transformations
 * - Slim (Alex) and wide (Steve) arm model variants
 * - Transparency and tint effects
 * 
 * Requirement 6.2: Implement custom skin rendering and model support
 */
public class StoryNPCRenderer extends HumanoidMobRenderer<StoryNPC, StoryNPCRenderState, StoryNPCModel> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StoryNPCRenderer.class);
    
    // Default NPC texture (Steve-like)
    private static final ResourceLocation DEFAULT_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, "textures/entity/npc/default.png");
    
    // Fallback to vanilla Steve texture if custom texture not found
    // In Minecraft 1.21+ player skins are in textures/entity/player/
    private static final ResourceLocation STEVE_TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    
    // Fallback to vanilla Alex texture for slim models
    private static final ResourceLocation ALEX_TEXTURE = 
            ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");
    
    // Cache for resolved texture locations to avoid repeated parsing
    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    
    // Maximum cache size to prevent memory issues
    private static final int MAX_CACHE_SIZE = 100;
    
    // Slim model for Alex-style NPCs
    private final StoryNPCModel slimModel;
    
    // Standard model for Steve-style NPCs
    private final StoryNPCModel standardModel;
    
    public StoryNPCRenderer(EntityRendererProvider.Context context) {
        super(context, new StoryNPCModel(context.bakeLayer(NPCModelLayers.STORY_NPC), false), 0.5F);
        this.standardModel = this.getModel();
        this.slimModel = new StoryNPCModel(context.bakeLayer(NPCModelLayers.STORY_NPC_SLIM), true);
    }
    
    /**
     * Applies scale transformation to the NPC.
     * Called by the parent renderer during the rendering process.
     */
    @Override
    protected void scale(StoryNPCRenderState state, PoseStack poseStack) {
        // Apply custom scale from NPC properties
        if (state.scale != 1.0F) {
            poseStack.scale(state.scale, state.scale, state.scale);
        }
        super.scale(state, poseStack);
    }
    
    @Override
    public ResourceLocation getTextureLocation(StoryNPCRenderState state) {
        // Use cached resolved texture if available
        if (state.resolvedTexture != null) {
            return state.resolvedTexture;
        }
        
        String skinUrl = state.skinUrl;
        
        // If a custom skin URL is set, try to resolve it
        if (skinUrl != null && !skinUrl.isEmpty()) {
            ResourceLocation resolved = resolveTexture(skinUrl, state.npcId);
            state.resolvedTexture = resolved;
            return resolved;
        }
        
        // Return appropriate default texture based on model type
        return state.slimArms ? ALEX_TEXTURE : STEVE_TEXTURE;
    }
    
    /**
     * Resolves a skin URL to a ResourceLocation.
     * Supports multiple formats:
     * - Full resource location: "modid:textures/entity/npc/skin.png"
     * - Short name: "custom_skin" -> "itsmystory:textures/entity/npc/custom_skin.png"
     * - Vanilla skins: "steve", "alex"
     * 
     * @param skinUrl The skin URL or name
     * @param npcId The NPC ID for logging purposes
     * @return The resolved ResourceLocation
     */
    private ResourceLocation resolveTexture(String skinUrl, String npcId) {
        // Check cache first
        ResourceLocation cached = TEXTURE_CACHE.get(skinUrl);
        if (cached != null) {
            return cached;
        }
        
        ResourceLocation resolved;
        
        try {
            // Handle special vanilla skin names
            if ("steve".equalsIgnoreCase(skinUrl)) {
                resolved = STEVE_TEXTURE;
            } else if ("alex".equalsIgnoreCase(skinUrl)) {
                resolved = ALEX_TEXTURE;
            } else if (skinUrl.contains(":")) {
                // Full resource location format
                resolved = ResourceLocation.parse(skinUrl);
            } else if (skinUrl.endsWith(".png")) {
                // Filename with extension - assume it's in the NPC textures folder
                resolved = ResourceLocation.fromNamespaceAndPath(
                        ItsMyStory.MOD_ID,
                        "textures/entity/npc/" + skinUrl
                );
            } else {
                // Short name - add path and extension
                resolved = ResourceLocation.fromNamespaceAndPath(
                        ItsMyStory.MOD_ID,
                        "textures/entity/npc/" + skinUrl + ".png"
                );
            }
            
            // Cache the result (with size limit)
            if (TEXTURE_CACHE.size() < MAX_CACHE_SIZE) {
                TEXTURE_CACHE.put(skinUrl, resolved);
            }
            
            return resolved;
            
        } catch (Exception e) {
            LOGGER.warn("Invalid skin URL for NPC {}: {} - using default", npcId, skinUrl);
            return STEVE_TEXTURE;
        }
    }
    
    @Override
    public StoryNPCRenderState createRenderState() {
        return new StoryNPCRenderState();
    }
    
    @Override
    public void extractRenderState(StoryNPC entity, StoryNPCRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        
        // Extract custom NPC data into render state
        state.npcId = entity.getNpcId();
        state.skinUrl = entity.getSkinUrl();
        state.displayName = entity.getCustomName() != null ? entity.getCustomName().getString() : "NPC";
        
        // Clear cached texture if skin URL changed
        String previousSkinUrl = state.skinUrl;
        if (previousSkinUrl != null && !previousSkinUrl.equals(entity.getSkinUrl())) {
            state.resolvedTexture = null;
        }
        
        // Extract scale from custom data
        Object scaleData = entity.getCustomData("scale");
        if (scaleData instanceof Number) {
            state.scale = ((Number) scaleData).floatValue();
        } else {
            state.scale = 1.0F;
        }
        
        // Extract slim arms setting from custom data
        Object slimData = entity.getCustomData("slim");
        if (slimData instanceof Boolean) {
            state.slimArms = (Boolean) slimData;
        } else if (slimData instanceof String) {
            state.slimArms = Boolean.parseBoolean((String) slimData);
        } else {
            state.slimArms = false;
        }
        
        // Extract glowing effect
        Object glowData = entity.getCustomData("glowing");
        if (glowData instanceof Boolean) {
            state.glowing = (Boolean) glowData;
        } else {
            state.glowing = false;
        }
        
        // Extract tint color
        Object tintData = entity.getCustomData("tint");
        if (tintData instanceof Number) {
            state.tintColor = ((Number) tintData).intValue();
        } else {
            state.tintColor = -1;
        }
        
        // Extract alpha/transparency
        Object alphaData = entity.getCustomData("alpha");
        if (alphaData instanceof Number) {
            state.alpha = Math.max(0.0F, Math.min(1.0F, ((Number) alphaData).floatValue()));
        } else {
            state.alpha = 1.0F;
        }
        
        // Extract animation state (Requirements: 10.2, 10.4)
        state.isAnimating = entity.isAnimating();
        state.currentAnimationId = entity.getCurrentAnimationId();
        state.animationTime = entity.getAnimationTime();
        state.currentAnimationFrame = entity.getCurrentAnimationFrame();
    }
    
    /**
     * Clears the texture cache.
     * Useful when reloading resources or when memory needs to be freed.
     */
    public static void clearTextureCache() {
        TEXTURE_CACHE.clear();
        LOGGER.debug("NPC texture cache cleared");
    }
    
    /**
     * Gets the current size of the texture cache.
     * 
     * @return The number of cached textures
     */
    public static int getTextureCacheSize() {
        return TEXTURE_CACHE.size();
    }
}
