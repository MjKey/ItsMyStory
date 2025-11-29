package ru.mjkey.storykee.systems.party;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Party class.
 * 
 * Requirements: 23.1, 23.2, 23.3
 */
class PartyTest {
    
    private UUID leaderId;
    private Party party;
    
    @BeforeEach
    void setUp() {
        leaderId = UUID.randomUUID();
        party = new Party("test-party", leaderId);
    }
    
    @Test
    void testPartyCreation() {
        // Requirement 23.1: Initialize party with specified players
        assertEquals("test-party", party.getId());
        assertEquals(leaderId, party.getLeaderId());
        assertTrue(party.isMember(leaderId));
        assertEquals(1, party.getMemberCount());
    }
    
    @Test
    void testAddMember() {
        // Requirement 23.2: Add player to party member list
        UUID memberId = UUID.randomUUID();
        
        assertTrue(party.addMember(memberId));
        assertTrue(party.isMember(memberId));
        assertEquals(2, party.getMemberCount());
    }
    
    @Test
    void testAddNullMember() {
        assertFalse(party.addMember(null));
        assertEquals(1, party.getMemberCount());
    }
    
    @Test
    void testAddDuplicateMember() {
        UUID memberId = UUID.randomUUID();
        
        assertTrue(party.addMember(memberId));
        assertFalse(party.addMember(memberId)); // Already a member
        assertEquals(2, party.getMemberCount());
    }
    
    @Test
    void testRemoveMember() {
        // Requirement 23.3: Remove player from party
        UUID memberId = UUID.randomUUID();
        party.addMember(memberId);
        
        assertTrue(party.removeMember(memberId));
        assertFalse(party.isMember(memberId));
        assertEquals(1, party.getMemberCount());
    }
    
    @Test
    void testCannotRemoveLeader() {
        // Leader cannot be removed
        assertFalse(party.removeMember(leaderId));
        assertTrue(party.isMember(leaderId));
    }
    
    @Test
    void testRemoveNullMember() {
        assertFalse(party.removeMember(null));
    }
    
    @Test
    void testMaxSize() {
        party.setMaxSize(3);
        
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        UUID member3 = UUID.randomUUID();
        
        assertTrue(party.addMember(member1));
        assertTrue(party.addMember(member2));
        assertFalse(party.addMember(member3)); // Exceeds max size
        
        assertEquals(3, party.getMemberCount());
    }
    
    @Test
    void testIsLeader() {
        assertTrue(party.isLeader(leaderId));
        assertFalse(party.isLeader(UUID.randomUUID()));
    }
    
    @Test
    void testCustomData() {
        party.setData("questProgress", 5);
        party.setData("name", "Heroes");
        
        assertTrue(party.hasData("questProgress"));
        assertEquals(5, (int) party.getData("questProgress"));
        assertEquals("Heroes", party.getData("name"));
        
        party.removeData("questProgress");
        assertFalse(party.hasData("questProgress"));
    }
    
    @Test
    void testEquality() {
        Party sameParty = new Party("test-party", UUID.randomUUID());
        Party differentParty = new Party("other-party", leaderId);
        
        assertEquals(party, sameParty); // Same ID
        assertNotEquals(party, differentParty);
    }
}
