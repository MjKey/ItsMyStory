package ru.mjkey.storykee.systems.armorstand;

import net.minecraft.core.Rotations;

/**
 * Represents a pose configuration for an armor stand.
 * 
 * Requirements: 56.2
 */
public class ArmorStandPose {
    
    public static final ArmorStandPose DEFAULT = new ArmorStandPose();
    
    public static final ArmorStandPose SALUTE = new ArmorStandPose()
        .withRightArm(new Rotations(-10, 0, -100));
    
    public static final ArmorStandPose WAVE = new ArmorStandPose()
        .withRightArm(new Rotations(-100, 0, -10));
    
    public static final ArmorStandPose POINTING = new ArmorStandPose()
        .withRightArm(new Rotations(-90, 0, 0));
    
    public static final ArmorStandPose CROSSED_ARMS = new ArmorStandPose()
        .withLeftArm(new Rotations(-20, 20, 0))
        .withRightArm(new Rotations(-20, -20, 0));
    
    public static final ArmorStandPose ZOMBIE = new ArmorStandPose()
        .withLeftArm(new Rotations(-90, 0, 0))
        .withRightArm(new Rotations(-90, 0, 0));
    
    private Rotations head;
    private Rotations body;
    private Rotations leftArm;
    private Rotations rightArm;
    private Rotations leftLeg;
    private Rotations rightLeg;
    
    public ArmorStandPose() {
        // Default pose - all zeros
    }
    
    public ArmorStandPose(Rotations head, Rotations body, Rotations leftArm, 
                          Rotations rightArm, Rotations leftLeg, Rotations rightLeg) {
        this.head = head;
        this.body = body;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }
    
    public Rotations getHead() { return head; }
    public Rotations getBody() { return body; }
    public Rotations getLeftArm() { return leftArm; }
    public Rotations getRightArm() { return rightArm; }
    public Rotations getLeftLeg() { return leftLeg; }
    public Rotations getRightLeg() { return rightLeg; }
    
    public ArmorStandPose withHead(Rotations head) {
        this.head = head;
        return this;
    }
    
    public ArmorStandPose withBody(Rotations body) {
        this.body = body;
        return this;
    }
    
    public ArmorStandPose withLeftArm(Rotations leftArm) {
        this.leftArm = leftArm;
        return this;
    }
    
    public ArmorStandPose withRightArm(Rotations rightArm) {
        this.rightArm = rightArm;
        return this;
    }
    
    public ArmorStandPose withLeftLeg(Rotations leftLeg) {
        this.leftLeg = leftLeg;
        return this;
    }
    
    public ArmorStandPose withRightLeg(Rotations rightLeg) {
        this.rightLeg = rightLeg;
        return this;
    }
    
    /**
     * Creates a pose from rotation values.
     */
    public static ArmorStandPose fromValues(
            float headX, float headY, float headZ,
            float bodyX, float bodyY, float bodyZ,
            float leftArmX, float leftArmY, float leftArmZ,
            float rightArmX, float rightArmY, float rightArmZ,
            float leftLegX, float leftLegY, float leftLegZ,
            float rightLegX, float rightLegY, float rightLegZ) {
        return new ArmorStandPose(
            new Rotations(headX, headY, headZ),
            new Rotations(bodyX, bodyY, bodyZ),
            new Rotations(leftArmX, leftArmY, leftArmZ),
            new Rotations(rightArmX, rightArmY, rightArmZ),
            new Rotations(leftLegX, leftLegY, leftLegZ),
            new Rotations(rightLegX, rightLegY, rightLegZ)
        );
    }
    
    /**
     * Creates a copy of this pose.
     */
    public ArmorStandPose copy() {
        return new ArmorStandPose(head, body, leftArm, rightArm, leftLeg, rightLeg);
    }
}
