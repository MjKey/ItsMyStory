package ru.mjkey.storykee.systems.boss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BossPhase class.
 * 
 * Requirements: 18.4
 */
class BossPhaseTest {
    
    private BossPhase phase;
    
    @BeforeEach
    void setUp() {
        phase = new BossPhase("phase1", 0.75f);
    }
    
    @Test
    void testPhaseCreation() {
        assertEquals("phase1", phase.getId());
        assertEquals(0.75f, phase.getHealthThreshold(), 0.0001f);
        assertTrue(phase.getAbilities().isEmpty());
        assertTrue(phase.getProperties().isEmpty());
    }
    
    @Test
    void testHealthThresholdClamping() {
        BossPhase lowPhase = new BossPhase("low", -0.5f);
        BossPhase highPhase = new BossPhase("high", 1.5f);
        
        assertEquals(0.0f, lowPhase.getHealthThreshold(), 0.0001f);
        assertEquals(1.0f, highPhase.getHealthThreshold(), 0.0001f);
    }
    
    @Test
    void testAddAbility() {
        BossAbility ability1 = new BossAbility("fireball", "Fireball", 60, null);
        BossAbility ability2 = new BossAbility("slam", "Ground Slam", 100, null);
        
        phase.addAbility(ability1).addAbility(ability2);
        
        List<BossAbility> abilities = phase.getAbilities();
        assertEquals(2, abilities.size());
        assertEquals("fireball", abilities.get(0).getId());
        assertEquals("slam", abilities.get(1).getId());
    }
    
    @Test
    void testAbilitiesListIsCopy() {
        phase.addAbility(new BossAbility("test", "Test", 0, null));
        
        List<BossAbility> abilities = phase.getAbilities();
        abilities.clear();
        
        // Original should be unchanged
        assertEquals(1, phase.getAbilities().size());
    }
    
    @Test
    void testSetProperty() {
        phase.setProperty("damage_multiplier", 1.5)
             .setProperty("speed_boost", true)
             .setProperty("name", "Enraged");
        
        assertEquals(1.5, (double) phase.getProperty("damage_multiplier"), 0.0001);
        assertEquals(true, phase.getProperty("speed_boost"));
        assertEquals("Enraged", phase.getProperty("name"));
    }
    
    @Test
    void testPropertiesMapIsCopy() {
        phase.setProperty("key", "value");
        
        Map<String, Object> props = phase.getProperties();
        props.clear();
        
        // Original should be unchanged
        assertEquals("value", phase.getProperty("key"));
    }
    
    @Test
    void testShouldActivate() {
        // Requirement 18.4: Phase transitions based on health thresholds
        // Phase activates when health drops below threshold (0.75)
        
        assertFalse(phase.shouldActivate(1.0f));  // Full health
        assertFalse(phase.shouldActivate(0.8f));  // Above threshold
        assertTrue(phase.shouldActivate(0.75f));  // At threshold
        assertTrue(phase.shouldActivate(0.5f));   // Below threshold
        assertTrue(phase.shouldActivate(0.0f));   // Dead
    }
    
    @Test
    void testMultiplePhases() {
        BossPhase phase1 = new BossPhase("phase1", 1.0f);   // Always active
        BossPhase phase2 = new BossPhase("phase2", 0.66f); // Below 66%
        BossPhase phase3 = new BossPhase("phase3", 0.33f); // Below 33%
        
        // At 80% health
        assertTrue(phase1.shouldActivate(0.8f));
        assertFalse(phase2.shouldActivate(0.8f));
        assertFalse(phase3.shouldActivate(0.8f));
        
        // At 50% health
        assertTrue(phase1.shouldActivate(0.5f));
        assertTrue(phase2.shouldActivate(0.5f));
        assertFalse(phase3.shouldActivate(0.5f));
        
        // At 20% health
        assertTrue(phase1.shouldActivate(0.2f));
        assertTrue(phase2.shouldActivate(0.2f));
        assertTrue(phase3.shouldActivate(0.2f));
    }
    
    @Test
    void testToString() {
        phase.addAbility(new BossAbility("test", "Test", 0, null));
        
        String str = phase.toString();
        
        assertTrue(str.contains("phase1"));
        assertTrue(str.contains("0.75"));
        assertTrue(str.contains("1")); // abilities count
    }
}
