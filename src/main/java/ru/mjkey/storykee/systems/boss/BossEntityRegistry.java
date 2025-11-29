package ru.mjkey.storykee.systems.boss;

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
 * Registry for Boss-related entities.
 * Handles registration of the StoryBoss entity type with Minecraft.
 * 
 * Requirements: 18.1 - Boss entity registration
 */
public class BossEntityRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BossEntityRegistry.class);
    
    private static final ResourceLocation STORY_BOSS_ID = 
            ResourceLocation.fromNamespaceAndPath(ItsMyStory.MOD_ID, "story_boss");
    
    public static EntityType<StoryBoss> STORY_BOSS;
    
    /**
     * Registers all Boss-related entities.
     * Should be called during mod initialization.
     */
    public static void register() {
        LOGGER.info("Registering Boss entities...");
        
        // Create the entity type with resource key
        STORY_BOSS = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                STORY_BOSS_ID,
                EntityType.Builder.<StoryBoss>of(StoryBoss::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)  // Slightly larger than player
                        .clientTrackingRange(32)  // Larger tracking range for bosses
                        .build(ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), STORY_BOSS_ID))
        );
        
        // Register entity attributes
        FabricDefaultAttributeRegistry.register(STORY_BOSS, StoryBoss.createAttributes());
        
        LOGGER.info("Boss entities registered successfully");
    }
}
