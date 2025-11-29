package ru.mjkey.storykee.systems.boss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BossAbility class.
 * 
 * Requirements: 18.3
 */
class BossAbilityTest {
    
    private AtomicInteger executeCount;
    private BossAbility ability;
    
    @BeforeEach
    void setUp() {
        executeCount = new AtomicInteger(0);
        ability = new BossAbility(
                "fireball",
                "Fireball",
                60, // 3 second cooldown
                boss -> executeCount.incrementAndGet()
        );
    }
    
    @Test
    void testAbilityCreation() {
        assertEquals("fireball", ability.getId());
        assertEquals("Fireball", ability.getName());
        assertEquals(60, ability.getCooldownTicks());
        assertTrue(ability.isReady());
        assertEquals(0, ability.getCurrentCooldown());
    }
    
    @Test
    void testTryUseSuccess() {
        // Requirement 18.3: Boss abilities execute associated script actions
        boolean used = ability.tryUse(null);
        
        assertTrue(used);
        assertEquals(1, executeCount.get());
        assertFalse(ability.isReady());
        assertEquals(60, ability.getCurrentCooldown());
    }
    
    @Test
    void testTryUseOnCooldown() {
        ability.tryUse(null);
        
        boolean usedAgain = ability.tryUse(null);
        
        assertFalse(usedAgain);
        assertEquals(1, executeCount.get()); // Only executed once
    }
    
    @Test
    void testTick() {
        ability.tryUse(null);
        assertEquals(60, ability.getCurrentCooldown());
        
        // Tick 30 times
        for (int i = 0; i < 30; i++) {
            ability.tick();
        }
        
        assertEquals(30, ability.getCurrentCooldown());
        assertFalse(ability.isReady());
        
        // Tick remaining 30 times
        for (int i = 0; i < 30; i++) {
            ability.tick();
        }
        
        assertEquals(0, ability.getCurrentCooldown());
        assertTrue(ability.isReady());
    }
    
    @Test
    void testResetCooldown() {
        ability.tryUse(null);
        assertFalse(ability.isReady());
        
        ability.resetCooldown();
        
        assertTrue(ability.isReady());
        assertEquals(0, ability.getCurrentCooldown());
    }
    
    @Test
    void testStartCooldown() {
        assertTrue(ability.isReady());
        
        ability.startCooldown();
        
        assertFalse(ability.isReady());
        assertEquals(60, ability.getCurrentCooldown());
    }
    
    @Test
    void testAbilityWithNullExecute() {
        BossAbility nullAbility = new BossAbility("passive", "Passive", 0, null);
        
        // Should not throw
        boolean used = nullAbility.tryUse(null);
        assertTrue(used);
    }
    
    @Test
    void testZeroCooldown() {
        BossAbility instant = new BossAbility("instant", "Instant", 0, boss -> executeCount.incrementAndGet());
        
        assertTrue(instant.tryUse(null));
        assertTrue(instant.isReady()); // Zero cooldown means always ready
        assertTrue(instant.tryUse(null));
        
        assertEquals(2, executeCount.get());
    }
    
    @Test
    void testToString() {
        String str = ability.toString();
        
        assertTrue(str.contains("fireball"));
        assertTrue(str.contains("Fireball"));
        assertTrue(str.contains("60"));
    }
}
