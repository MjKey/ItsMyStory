package ru.mjkey.storykee.systems.npc;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.ItsMyStory;

/**
 * Registry for NPC-related entities.
 * Handles registration of the StoryNPC entity type with Minecraft.
 */
public class NPCEntityRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NPCEntityRegistry.class);
    
    private static final ResourceLocation STORY_NPC_ID = 
            ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, "story_npc");
    
    public static EntityType<StoryNPC> STORY_NPC;
    
    /**
     * Registers all NPC-related entities.
     * Should be called during mod initialization.
     */
    public static void register() {
        LOGGER.info("Registering NPC entities...");
        
        // Create the entity type with resource key
        STORY_NPC = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                STORY_NPC_ID,
                EntityType.Builder.<StoryNPC>of(StoryNPC::new, MobCategory.MISC)
                        .sized(0.6F, 1.8F)  // Same size as player
                        .clientTrackingRange(10)
                        .build(ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), STORY_NPC_ID))
        );
        
        // Register entity attributes
        FabricDefaultAttributeRegistry.register(STORY_NPC, StoryNPC.createAttributes());
        
        LOGGER.info("NPC entities registered successfully");
    }
}
