package ru.mjkey.storykee.systems.npc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.systems.animation.Animation;
import ru.mjkey.storykee.systems.animation.AnimationFrame;
import ru.mjkey.storykee.systems.animation.AnimationManager;
import ru.mjkey.storykee.systems.animation.Transform;

/**
 * Provides standard NPC animations for common actions.
 * These animations are registered automatically when the mod initializes.
 * 
 * Requirements: 10.1
 * 
 * Standard animations:
 * - wave: Waving hand greeting
 * - bow: Bowing in thanks/respect
 * - nod: Nodding in agreement
 * - shake_head: Shaking head in disagreement
 * - point: Pointing with arm
 * - talk: Talking gestures for dialogues
 */
public class NPCAnimations {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NPCAnimations.class);
    
    // Animation IDs
    public static final String ANIM_WAVE = "npc_wave";
    public static final String ANIM_BOW = "npc_bow";
    public static final String ANIM_NOD = "npc_nod";
    public static final String ANIM_SHAKE_HEAD = "npc_shake_head";
    public static final String ANIM_POINT = "npc_point";
    public static final String ANIM_TALK = "npc_talk";
    
    // Bone names (matching StoryNPCModel)
    private static final String HEAD = "head";
    private static final String BODY = "body";
    private static final String RIGHT_ARM = "right_arm";
    private static final String LEFT_ARM = "left_arm";
    private static final String RIGHT_LEG = "right_leg";
    private static final String LEFT_LEG = "left_leg";
    
    private static boolean registered = false;
    
    /**
     * Registers all standard NPC animations.
     * Should be called during mod initialization.
     */
    public static void registerAll() {
        if (registered) {
            LOGGER.debug("NPC animations already registered");
            return;
        }
        
        AnimationManager manager = AnimationManager.getInstance();
        
        // Register each standard animation
        manager.registerAnimation(createWaveAnimation());
        manager.registerAnimation(createBowAnimation());
        manager.registerAnimation(createNodAnimation());
        manager.registerAnimation(createShakeHeadAnimation());
        manager.registerAnimation(createPointAnimation());
        manager.registerAnimation(createTalkAnimation());
        
        registered = true;
        LOGGER.info("Registered {} standard NPC animations", 6);
    }
    
    /**
     * Creates the wave animation (waving hand greeting).
     * Duration: 1.5 seconds
     */
    private static Animation createWaveAnimation() {
        Animation anim = new Animation(ANIM_WAVE, 1.5f, false);
        
        // Frame 0: Start position (arm at side)
        AnimationFrame frame0 = new AnimationFrame(0, 0f);
        frame0.setBoneTransform(RIGHT_ARM, Transform.identity());
        anim.addFrame(frame0);
        
        // Frame 1: Arm raised (0.2s)
        AnimationFrame frame1 = new AnimationFrame(1, 0.2f);
        frame1.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-120f, 0f, -30f));
        anim.addFrame(frame1);
        
        // Frame 2: Wave right (0.4s)
        AnimationFrame frame2 = new AnimationFrame(2, 0.4f);
        frame2.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-120f, 0f, -10f));
        anim.addFrame(frame2);
        
        // Frame 3: Wave left (0.6s)
        AnimationFrame frame3 = new AnimationFrame(3, 0.6f);
        frame3.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-120f, 0f, -50f));
        anim.addFrame(frame3);
        
        // Frame 4: Wave right (0.8s)
        AnimationFrame frame4 = new AnimationFrame(4, 0.8f);
        frame4.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-120f, 0f, -10f));
        anim.addFrame(frame4);
        
        // Frame 5: Wave left (1.0s)
        AnimationFrame frame5 = new AnimationFrame(5, 1.0f);
        frame5.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-120f, 0f, -50f));
        anim.addFrame(frame5);
        
        // Frame 6: Return to rest (1.5s)
        AnimationFrame frame6 = new AnimationFrame(6, 1.5f);
        frame6.setBoneTransform(RIGHT_ARM, Transform.identity());
        anim.addFrame(frame6);
        
        return anim;
    }
    
    /**
     * Creates the bow animation (bowing in thanks/respect).
     * Duration: 2.0 seconds
     */
    private static Animation createBowAnimation() {
        Animation anim = new Animation(ANIM_BOW, 2.0f, false);
        
        // Frame 0: Standing straight
        AnimationFrame frame0 = new AnimationFrame(0, 0f);
        frame0.setBoneTransform(BODY, Transform.identity());
        frame0.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame0);
        
        // Frame 1: Start bowing (0.4s)
        AnimationFrame frame1 = new AnimationFrame(1, 0.4f);
        frame1.setBoneTransform(BODY, Transform.fromRotation(30f, 0f, 0f));
        frame1.setBoneTransform(HEAD, Transform.fromRotation(15f, 0f, 0f));
        anim.addFrame(frame1);
        
        // Frame 2: Full bow (0.8s)
        AnimationFrame frame2 = new AnimationFrame(2, 0.8f);
        frame2.setBoneTransform(BODY, Transform.fromRotation(45f, 0f, 0f));
        frame2.setBoneTransform(HEAD, Transform.fromRotation(20f, 0f, 0f));
        anim.addFrame(frame2);
        
        // Frame 3: Hold bow (1.2s)
        AnimationFrame frame3 = new AnimationFrame(3, 1.2f);
        frame3.setBoneTransform(BODY, Transform.fromRotation(45f, 0f, 0f));
        frame3.setBoneTransform(HEAD, Transform.fromRotation(20f, 0f, 0f));
        anim.addFrame(frame3);
        
        // Frame 4: Return to standing (2.0s)
        AnimationFrame frame4 = new AnimationFrame(4, 2.0f);
        frame4.setBoneTransform(BODY, Transform.identity());
        frame4.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame4);
        
        return anim;
    }
    
    /**
     * Creates the nod animation (nodding in agreement).
     * Duration: 0.8 seconds
     */
    private static Animation createNodAnimation() {
        Animation anim = new Animation(ANIM_NOD, 0.8f, false);
        
        // Frame 0: Head neutral
        AnimationFrame frame0 = new AnimationFrame(0, 0f);
        frame0.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame0);
        
        // Frame 1: Head down (0.15s)
        AnimationFrame frame1 = new AnimationFrame(1, 0.15f);
        frame1.setBoneTransform(HEAD, Transform.fromRotation(20f, 0f, 0f));
        anim.addFrame(frame1);
        
        // Frame 2: Head up (0.3s)
        AnimationFrame frame2 = new AnimationFrame(2, 0.3f);
        frame2.setBoneTransform(HEAD, Transform.fromRotation(-5f, 0f, 0f));
        anim.addFrame(frame2);
        
        // Frame 3: Head down again (0.45s)
        AnimationFrame frame3 = new AnimationFrame(3, 0.45f);
        frame3.setBoneTransform(HEAD, Transform.fromRotation(15f, 0f, 0f));
        anim.addFrame(frame3);
        
        // Frame 4: Return to neutral (0.8s)
        AnimationFrame frame4 = new AnimationFrame(4, 0.8f);
        frame4.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame4);
        
        return anim;
    }
    
    /**
     * Creates the shake head animation (shaking head in disagreement).
     * Duration: 1.0 seconds
     */
    private static Animation createShakeHeadAnimation() {
        Animation anim = new Animation(ANIM_SHAKE_HEAD, 1.0f, false);
        
        // Frame 0: Head neutral
        AnimationFrame frame0 = new AnimationFrame(0, 0f);
        frame0.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame0);
        
        // Frame 1: Head left (0.15s)
        AnimationFrame frame1 = new AnimationFrame(1, 0.15f);
        frame1.setBoneTransform(HEAD, Transform.fromRotation(0f, -25f, 0f));
        anim.addFrame(frame1);
        
        // Frame 2: Head right (0.35s)
        AnimationFrame frame2 = new AnimationFrame(2, 0.35f);
        frame2.setBoneTransform(HEAD, Transform.fromRotation(0f, 25f, 0f));
        anim.addFrame(frame2);
        
        // Frame 3: Head left (0.55s)
        AnimationFrame frame3 = new AnimationFrame(3, 0.55f);
        frame3.setBoneTransform(HEAD, Transform.fromRotation(0f, -20f, 0f));
        anim.addFrame(frame3);
        
        // Frame 4: Head right (0.75s)
        AnimationFrame frame4 = new AnimationFrame(4, 0.75f);
        frame4.setBoneTransform(HEAD, Transform.fromRotation(0f, 20f, 0f));
        anim.addFrame(frame4);
        
        // Frame 5: Return to neutral (1.0s)
        AnimationFrame frame5 = new AnimationFrame(5, 1.0f);
        frame5.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame5);
        
        return anim;
    }
    
    /**
     * Creates the point animation (pointing with arm).
     * Duration: 1.2 seconds
     */
    private static Animation createPointAnimation() {
        Animation anim = new Animation(ANIM_POINT, 1.2f, false);
        
        // Frame 0: Arm at side
        AnimationFrame frame0 = new AnimationFrame(0, 0f);
        frame0.setBoneTransform(RIGHT_ARM, Transform.identity());
        anim.addFrame(frame0);
        
        // Frame 1: Arm raising (0.3s)
        AnimationFrame frame1 = new AnimationFrame(1, 0.3f);
        frame1.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-70f, 0f, 0f));
        anim.addFrame(frame1);
        
        // Frame 2: Full point (0.5s)
        AnimationFrame frame2 = new AnimationFrame(2, 0.5f);
        frame2.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-90f, 0f, 0f));
        anim.addFrame(frame2);
        
        // Frame 3: Hold point (0.9s)
        AnimationFrame frame3 = new AnimationFrame(3, 0.9f);
        frame3.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-90f, 0f, 0f));
        anim.addFrame(frame3);
        
        // Frame 4: Return to rest (1.2s)
        AnimationFrame frame4 = new AnimationFrame(4, 1.2f);
        frame4.setBoneTransform(RIGHT_ARM, Transform.identity());
        anim.addFrame(frame4);
        
        return anim;
    }

    
    /**
     * Creates the talk animation (gesturing while talking).
     * Duration: 2.0 seconds, loops
     */
    private static Animation createTalkAnimation() {
        Animation anim = new Animation(ANIM_TALK, 2.0f, true);
        
        // Frame 0: Neutral position
        AnimationFrame frame0 = new AnimationFrame(0, 0f);
        frame0.setBoneTransform(RIGHT_ARM, Transform.identity());
        frame0.setBoneTransform(LEFT_ARM, Transform.identity());
        frame0.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame0);
        
        // Frame 1: Right arm gesture (0.3s)
        AnimationFrame frame1 = new AnimationFrame(1, 0.3f);
        frame1.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-30f, 0f, -15f));
        frame1.setBoneTransform(LEFT_ARM, Transform.fromRotation(-10f, 0f, 10f));
        frame1.setBoneTransform(HEAD, Transform.fromRotation(5f, 10f, 0f));
        anim.addFrame(frame1);
        
        // Frame 2: Both arms gesture (0.6s)
        AnimationFrame frame2 = new AnimationFrame(2, 0.6f);
        frame2.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-25f, 0f, -20f));
        frame2.setBoneTransform(LEFT_ARM, Transform.fromRotation(-25f, 0f, 20f));
        frame2.setBoneTransform(HEAD, Transform.fromRotation(0f, -5f, 0f));
        anim.addFrame(frame2);
        
        // Frame 3: Left arm gesture (0.9s)
        AnimationFrame frame3 = new AnimationFrame(3, 0.9f);
        frame3.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-10f, 0f, -10f));
        frame3.setBoneTransform(LEFT_ARM, Transform.fromRotation(-35f, 0f, 15f));
        frame3.setBoneTransform(HEAD, Transform.fromRotation(-5f, -10f, 0f));
        anim.addFrame(frame3);
        
        // Frame 4: Return to neutral (1.2s)
        AnimationFrame frame4 = new AnimationFrame(4, 1.2f);
        frame4.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-5f, 0f, -5f));
        frame4.setBoneTransform(LEFT_ARM, Transform.fromRotation(-5f, 0f, 5f));
        frame4.setBoneTransform(HEAD, Transform.fromRotation(0f, 5f, 0f));
        anim.addFrame(frame4);
        
        // Frame 5: Slight gesture (1.5s)
        AnimationFrame frame5 = new AnimationFrame(5, 1.5f);
        frame5.setBoneTransform(RIGHT_ARM, Transform.fromRotation(-20f, 0f, -10f));
        frame5.setBoneTransform(LEFT_ARM, Transform.fromRotation(-15f, 0f, 10f));
        frame5.setBoneTransform(HEAD, Transform.fromRotation(3f, -3f, 0f));
        anim.addFrame(frame5);
        
        // Frame 6: Back to neutral for loop (2.0s)
        AnimationFrame frame6 = new AnimationFrame(6, 2.0f);
        frame6.setBoneTransform(RIGHT_ARM, Transform.identity());
        frame6.setBoneTransform(LEFT_ARM, Transform.identity());
        frame6.setBoneTransform(HEAD, Transform.identity());
        anim.addFrame(frame6);
        
        return anim;
    }
    
    /**
     * Checks if standard animations are registered.
     * 
     * @return true if animations have been registered
     */
    public static boolean isRegistered() {
        return registered;
    }
    
    /**
     * Gets all standard animation IDs.
     * 
     * @return Array of animation IDs
     */
    public static String[] getAllAnimationIds() {
        return new String[] {
            ANIM_WAVE,
            ANIM_BOW,
            ANIM_NOD,
            ANIM_SHAKE_HEAD,
            ANIM_POINT,
            ANIM_TALK
        };
    }
}
