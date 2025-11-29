package ru.mjkey.storykee.systems.npc;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import ru.mjkey.ItsMyStory;

/**
 * Model layer definitions for NPC entities.
 * 
 * Requirement 6.2: Custom model support with wide and slim arm variants.
 */
public class NPCModelLayers {
    
    /** Standard NPC model with wide arms (Steve-style) */
    public static final ModelLayerLocation STORY_NPC = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, "story_npc"),
            "main"
    );
    
    /** NPC model with slim arms (Alex-style) */
    public static final ModelLayerLocation STORY_NPC_SLIM = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, "story_npc"),
            "slim"
    );
}
