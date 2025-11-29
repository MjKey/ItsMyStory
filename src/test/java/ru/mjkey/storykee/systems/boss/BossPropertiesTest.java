package ru.mjkey.storykee.systems.boss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BossProperties class.
 * 
 * Requirements: 18.1, 18.6
 */
class BossPropertiesTest {
    
    private static final double EPSILON = 0.0001;
    private BossProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new BossProperties();
    }
    
    @Test
    void testDefaultValues() {
        assertEquals("Boss", properties.getName());
        assertEquals(100.0, properties.getMaxHealth(), EPSILON);
        assertEquals(10.0, properties.getAttackDamage(), EPSILON);
        assertEquals(0.3, properties.getMovementSpeed(), EPSILON);
        assertEquals(32.0, properties.getFollowRange(), EPSILON);
        assertEquals(0.5, properties.getKnockbackResistance(), EPSILON);
    }
    
    @Test
    void testBuilderPattern() {
        // Requirement 18.1: Boss spawning with specified health, damage, and abilities
        BossProperties props = new BossProperties()
                .name("Dragon")
                .maxHealth(500.0)
                .attackDamage(25.0)
                .movementSpeed(0.5)
                .followRange(64.0)
                .knockbackResistance(0.8);
        
        assertEquals("Dragon", props.getName());
        assertEquals(500.0, props.getMaxHealth(), EPSILON);
        assertEquals(25.0, props.getAttackDamage(), EPSILON);
        assertEquals(0.5, props.getMovementSpeed(), EPSILON);
        assertEquals(64.0, props.getFollowRange(), EPSILON);
        assertEquals(0.8, props.getKnockbackResistance(), EPSILON);
    }
    
    @Test
    void testValueClamping() {
        BossProperties props = new BossProperties()
                .maxHealth(-100)      // Should clamp to 1
                .attackDamage(-10)    // Should clamp to 0
                .movementSpeed(-0.5)  // Should clamp to 0
                .followRange(0)       // Should clamp to 1
                .knockbackResistance(2.0); // Should clamp to 1
        
        assertEquals(1.0, props.getMaxHealth(), EPSILON);
        assertEquals(0.0, props.getAttackDamage(), EPSILON);
        assertEquals(0.0, props.getMovementSpeed(), EPSILON);
        assertEquals(1.0, props.getFollowRange(), EPSILON);
        assertEquals(1.0, props.getKnockbackResistance(), EPSILON);
    }
    
    @Test
    void testPosition() {
        properties.position(100, 64, 200);
        
        assertEquals(100, properties.getX(), EPSILON);
        assertEquals(64, properties.getY(), EPSILON);
        assertEquals(200, properties.getZ(), EPSILON);
    }
    
    @Test
    void testCombatArea() {
        // Requirement 18.6: Combat area restriction
        assertFalse(properties.hasCombatArea());
        
        properties.combatArea(100, 64, 200, 50);
        
        assertTrue(properties.hasCombatArea());
        assertEquals(100, properties.getCombatAreaCenter().x, EPSILON);
        assertEquals(64, properties.getCombatAreaCenter().y, EPSILON);
        assertEquals(200, properties.getCombatAreaCenter().z, EPSILON);
        assertEquals(50, properties.getCombatAreaRadius(), EPSILON);
    }
    
    @Test
    void testCombatAreaNegativeRadius() {
        properties.combatArea(0, 0, 0, -10);
        
        assertEquals(0, properties.getCombatAreaRadius(), EPSILON);
        assertFalse(properties.hasCombatArea());
    }
    
    @Test
    void testAddPhase() {
        BossPhase phase1 = new BossPhase("phase1", 1.0f);
        BossPhase phase2 = new BossPhase("phase2", 0.5f);
        
        properties.addPhase(phase1).addPhase(phase2);
        
        List<BossPhase> phases = properties.getPhases();
        assertEquals(2, phases.size());
        assertEquals("phase1", phases.get(0).getId());
        assertEquals("phase2", phases.get(1).getId());
    }
    
    @Test
    void testPhasesListIsCopy() {
        properties.addPhase(new BossPhase("test", 1.0f));
        
        List<BossPhase> phases = properties.getPhases();
        phases.clear();
        
        // Original should be unchanged
        assertEquals(1, properties.getPhases().size());
    }
    
    @Test
    void testCustomData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lootTable", "boss_loot");
        data.put("difficulty", 5);
        
        properties.customData(data);
        
        Map<String, Object> retrieved = properties.getCustomData();
        assertEquals("boss_loot", retrieved.get("lootTable"));
        assertEquals(5, retrieved.get("difficulty"));
    }
    
    @Test
    void testSetCustomData() {
        properties.setCustomData("key1", "value1")
                  .setCustomData("key2", 42);
        
        Map<String, Object> data = properties.getCustomData();
        assertEquals("value1", data.get("key1"));
        assertEquals(42, data.get("key2"));
    }
    
    @Test
    void testCustomDataMapIsCopy() {
        properties.setCustomData("key", "value");
        
        Map<String, Object> data = properties.getCustomData();
        data.clear();
        
        // Original should be unchanged
        assertEquals("value", properties.getCustomData().get("key"));
    }
    
    @Test
    void testBossBarSettings() {
        properties.bossBarColor(2).bossBarStyle(1);
        
        assertEquals(2, properties.getBossBarColor());
        assertEquals(1, properties.getBossBarStyle());
    }
    
    @Test
    void testToString() {
        properties.name("TestBoss").maxHealth(200);
        
        String str = properties.toString();
        
        assertTrue(str.contains("TestBoss"));
        assertTrue(str.contains("200"));
    }
}
