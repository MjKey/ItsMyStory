package ru.mjkey.storykee.systems.npc;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import ru.mjkey.storykee.systems.animation.AnimationFrame;
import ru.mjkey.storykee.systems.animation.Transform;

/**
 * Model for StoryNPC entities.
 * Uses a humanoid model similar to player model with support for
 * both wide (Steve) and slim (Alex) arm variants.
 * 
 * Requirement 6.2: Custom model support
 * Requirement 10.2, 10.4: Animation playback with frame interpolation
 */
public class StoryNPCModel extends HumanoidModel<StoryNPCRenderState> {
    
    private final boolean slim;
    
    // Bone name constants for animation
    public static final String BONE_HEAD = "head";
    public static final String BONE_BODY = "body";
    public static final String BONE_RIGHT_ARM = "right_arm";
    public static final String BONE_LEFT_ARM = "left_arm";
    public static final String BONE_RIGHT_LEG = "right_leg";
    public static final String BONE_LEFT_LEG = "left_leg";
    
    public StoryNPCModel(ModelPart root) {
        this(root, false);
    }
    
    public StoryNPCModel(ModelPart root, boolean slim) {
        super(root);
        this.slim = slim;
    }
    
    /**
     * Creates the layer definition for the NPC model with wide arms (Steve-style).
     * Uses standard player model mesh to ensure all required parts (head, hat, body, arms, legs) are present.
     */
    public static LayerDefinition createBodyLayer() {
        return createBodyLayer(false);
    }
    
    /**
     * Creates the layer definition for the NPC model with slim arms (Alex-style).
     */
    public static LayerDefinition createSlimBodyLayer() {
        return createBodyLayer(true);
    }
    
    /**
     * Creates the layer definition for the NPC model.
     * Uses PlayerModel.createMesh() to ensure all required parts for HumanoidModel are present.
     * 
     * @param slim Whether to use slim arm model (Alex-style)
     * @return The layer definition for the model
     */
    public static LayerDefinition createBodyLayer(boolean slim) {
        // Use PlayerModel's mesh creation to ensure all required parts are present
        // This includes: head, hat, body, right_arm, left_arm, right_leg, left_leg
        MeshDefinition meshDefinition = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        return LayerDefinition.create(meshDefinition, 64, 64);
    }
    
    /**
     * Returns whether this model uses slim arms.
     */
    public boolean isSlim() {
        return slim;
    }
    
    @Override
    public void setupAnim(StoryNPCRenderState state) {
        super.setupAnim(state);
        
        // Apply animation frame if NPC is animating (Requirements: 10.2, 10.4)
        if (state.isAnimating && state.currentAnimationFrame != null) {
            applyFrameToModel(state.currentAnimationFrame);
        }
    }
    
    /**
     * Applies an animation frame to the model parts.
     * Transforms are applied as rotations in radians.
     * 
     * @param frame The animation frame to apply
     */
    private void applyFrameToModel(AnimationFrame frame) {
        // Apply head transform
        if (frame.hasBoneTransform(BONE_HEAD)) {
            Transform transform = frame.getBoneTransform(BONE_HEAD);
            applyTransformToModelPart(this.head, transform);
        }
        
        // Apply body transform
        if (frame.hasBoneTransform(BONE_BODY)) {
            Transform transform = frame.getBoneTransform(BONE_BODY);
            applyTransformToModelPart(this.body, transform);
        }
        
        // Apply right arm transform
        if (frame.hasBoneTransform(BONE_RIGHT_ARM)) {
            Transform transform = frame.getBoneTransform(BONE_RIGHT_ARM);
            applyTransformToModelPart(this.rightArm, transform);
        }
        
        // Apply left arm transform
        if (frame.hasBoneTransform(BONE_LEFT_ARM)) {
            Transform transform = frame.getBoneTransform(BONE_LEFT_ARM);
            applyTransformToModelPart(this.leftArm, transform);
        }
        
        // Apply right leg transform
        if (frame.hasBoneTransform(BONE_RIGHT_LEG)) {
            Transform transform = frame.getBoneTransform(BONE_RIGHT_LEG);
            applyTransformToModelPart(this.rightLeg, transform);
        }
        
        // Apply left leg transform
        if (frame.hasBoneTransform(BONE_LEFT_LEG)) {
            Transform transform = frame.getBoneTransform(BONE_LEFT_LEG);
            applyTransformToModelPart(this.leftLeg, transform);
        }
    }
    
    /**
     * Applies a transform to a model part.
     * Rotation values in the transform are in degrees and converted to radians.
     * 
     * @param part The model part to transform
     * @param transform The transform to apply
     */
    private void applyTransformToModelPart(ModelPart part, Transform transform) {
        // Convert degrees to radians for rotation
        float pitchRad = (float) Math.toRadians(transform.getPitch());
        float yawRad = (float) Math.toRadians(transform.getYaw());
        float rollRad = (float) Math.toRadians(transform.getRoll());
        
        // Add rotation to existing rotation (animations are additive)
        part.xRot += pitchRad;
        part.yRot += yawRad;
        part.zRot += rollRad;
        
        // Apply position offset (scaled down for model space)
        part.x += transform.getX();
        part.y += transform.getY();
        part.z += transform.getZ();
    }
}
