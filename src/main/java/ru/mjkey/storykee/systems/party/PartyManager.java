package ru.mjkey.storykee.systems.party;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages parties/teams in the Storykee system.
 * 
 * Requirements: 23.1, 23.2, 23.3, 23.4, 23.5
 */
public class PartyManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PartyManager.class);
    
    private static PartyManager instance;
    
    // Parties by ID
    private final Map<String, Party> parties;
    
    // Player to party mapping for quick lookup
    private final Map<UUID, String> playerParties;
    
    // Event listeners
    private final List<PartyEventListener> eventListeners;
    
    private MinecraftServer server;
    
    private PartyManager() {
        this.parties = new ConcurrentHashMap<>();
        this.playerParties = new ConcurrentHashMap<>();
        this.eventListeners = new ArrayList<>();
    }
    
    public static PartyManager getInstance() {
        if (instance == null) {
            instance = new PartyManager();
        }
        return instance;
    }
    
    /**
     * Initializes the party manager.
     */
    public void initialize(MinecraftServer server) {
        this.server = server;
        LOGGER.info("PartyManager initialized");
    }
    
    /**
     * Shuts down the party manager.
     */
    public void shutdown() {
        parties.clear();
        playerParties.clear();
        LOGGER.info("PartyManager shutdown");
    }
    
    // ==================== Party Creation ====================
    
    /**
     * Creates a new party.
     * Requirement 23.1: Initialize party with specified players
     * 
     * @param partyId Unique party ID
     * @param leaderId Leader player UUID
     * @return The created party, or null if failed
     */
    public Party createParty(String partyId, UUID leaderId) {
        if (partyId == null || leaderId == null) {
            return null;
        }
        
        if (parties.containsKey(partyId)) {
            LOGGER.warn("Party '{}' already exists", partyId);
            return null;
        }
        
        // Check if leader is already in a party
        if (playerParties.containsKey(leaderId)) {
            LOGGER.warn("Player {} is already in a party", leaderId);
            return null;
        }
        
        Party party = new Party(partyId, leaderId);
        parties.put(partyId, party);
        playerParties.put(leaderId, partyId);
        
        notifyPartyCreated(party);
        LOGGER.debug("Created party '{}' with leader {}", partyId, leaderId);
        
        return party;
    }
    
    /**
     * Creates a party with initial members.
     * 
     * @param partyId Unique party ID
     * @param leaderId Leader player UUID
     * @param memberIds Initial member UUIDs
     * @return The created party, or null if failed
     */
    public Party createParty(String partyId, UUID leaderId, Collection<UUID> memberIds) {
        Party party = createParty(partyId, leaderId);
        if (party == null) {
            return null;
        }
        
        for (UUID memberId : memberIds) {
            if (!memberId.equals(leaderId)) {
                addMember(partyId, memberId);
            }
        }
        
        return party;
    }
    
    /**
     * Disbands a party.
     * 
     * @param partyId Party ID
     * @return true if disbanded
     */
    public boolean disbandParty(String partyId) {
        Party party = parties.remove(partyId);
        if (party == null) {
            return false;
        }
        
        // Remove all player mappings
        for (UUID memberId : party.getMembers()) {
            playerParties.remove(memberId);
            notifyMemberLeft(party, memberId, true);
        }
        
        notifyPartyDisbanded(party);
        LOGGER.debug("Disbanded party '{}'", partyId);
        
        return true;
    }
    
    // ==================== Member Management ====================
    
    /**
     * Adds a player to a party.
     * Requirement 23.2: Add player to party member list
     * 
     * @param partyId Party ID
     * @param playerId Player UUID
     * @return true if added successfully
     */
    public boolean addMember(String partyId, UUID playerId) {
        Party party = parties.get(partyId);
        if (party == null) {
            LOGGER.warn("Party '{}' not found", partyId);
            return false;
        }
        
        // Check if player is already in a party
        if (playerParties.containsKey(playerId)) {
            LOGGER.warn("Player {} is already in a party", playerId);
            return false;
        }
        
        if (!party.addMember(playerId)) {
            return false;
        }
        
        playerParties.put(playerId, partyId);
        notifyMemberJoined(party, playerId);
        
        LOGGER.debug("Player {} joined party '{}'", playerId, partyId);
        return true;
    }
    
    /**
     * Removes a player from their party.
     * Requirement 23.3: Remove player and notify other members
     * 
     * @param playerId Player UUID
     * @return true if removed
     */
    public boolean removeMember(UUID playerId) {
        String partyId = playerParties.get(playerId);
        if (partyId == null) {
            return false;
        }
        
        Party party = parties.get(partyId);
        if (party == null) {
            playerParties.remove(playerId);
            return false;
        }
        
        // If leader leaves, disband party
        if (party.isLeader(playerId)) {
            return disbandParty(partyId);
        }
        
        if (!party.removeMember(playerId)) {
            return false;
        }
        
        playerParties.remove(playerId);
        notifyMemberLeft(party, playerId, false);
        
        LOGGER.debug("Player {} left party '{}'", playerId, partyId);
        return true;
    }
    
    // ==================== Query Methods ====================
    
    /**
     * Gets a party by ID.
     * 
     * @param partyId Party ID
     * @return The party, or null if not found
     */
    public Party getParty(String partyId) {
        return parties.get(partyId);
    }
    
    /**
     * Gets the party a player is in.
     * Requirement 23.4: Return list of active members
     * 
     * @param playerId Player UUID
     * @return The party, or null if not in a party
     */
    public Party getPlayerParty(UUID playerId) {
        String partyId = playerParties.get(playerId);
        return partyId != null ? parties.get(partyId) : null;
    }
    
    /**
     * Checks if a player is in a party.
     * 
     * @param playerId Player UUID
     * @return true if in a party
     */
    public boolean isInParty(UUID playerId) {
        return playerParties.containsKey(playerId);
    }
    
    /**
     * Checks if two players are in the same party.
     * 
     * @param player1 First player UUID
     * @param player2 Second player UUID
     * @return true if in same party
     */
    public boolean areInSameParty(UUID player1, UUID player2) {
        String party1 = playerParties.get(player1);
        String party2 = playerParties.get(player2);
        return party1 != null && party1.equals(party2);
    }
    
    /**
     * Gets all party IDs.
     * 
     * @return Set of party IDs
     */
    public Set<String> getPartyIds() {
        return Collections.unmodifiableSet(parties.keySet());
    }
    
    /**
     * Gets all parties.
     * 
     * @return Collection of parties
     */
    public Collection<Party> getAllParties() {
        return Collections.unmodifiableCollection(parties.values());
    }
    
    /**
     * Gets party members as ServerPlayer objects.
     * 
     * @param partyId Party ID
     * @return List of online party members
     */
    public List<ServerPlayer> getOnlineMembers(String partyId) {
        Party party = parties.get(partyId);
        if (party == null || server == null) {
            return Collections.emptyList();
        }
        
        List<ServerPlayer> online = new ArrayList<>();
        for (UUID memberId : party.getMembers()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                online.add(player);
            }
        }
        return online;
    }
    
    // ==================== Party Events ====================
    
    /**
     * Fires a party-wide event.
     * Requirement 23.5: Notify all party members
     * 
     * @param partyId Party ID
     * @param eventType Event type
     * @param data Event data
     */
    public void firePartyEvent(String partyId, String eventType, Map<String, Object> data) {
        Party party = parties.get(partyId);
        if (party == null) {
            return;
        }
        
        for (PartyEventListener listener : eventListeners) {
            try {
                listener.onPartyEvent(party, eventType, data);
            } catch (Exception e) {
                LOGGER.error("Error in party event listener: {}", e.getMessage());
            }
        }
    }
    
    // ==================== Event Listeners ====================
    
    public void addEventListener(PartyEventListener listener) {
        eventListeners.add(listener);
    }
    
    public void removeEventListener(PartyEventListener listener) {
        eventListeners.remove(listener);
    }
    
    private void notifyPartyCreated(Party party) {
        for (PartyEventListener listener : eventListeners) {
            try {
                listener.onPartyCreated(party);
            } catch (Exception e) {
                LOGGER.error("Error in party created listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyPartyDisbanded(Party party) {
        for (PartyEventListener listener : eventListeners) {
            try {
                listener.onPartyDisbanded(party);
            } catch (Exception e) {
                LOGGER.error("Error in party disbanded listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyMemberJoined(Party party, UUID playerId) {
        for (PartyEventListener listener : eventListeners) {
            try {
                listener.onMemberJoined(party, playerId);
            } catch (Exception e) {
                LOGGER.error("Error in member joined listener: {}", e.getMessage());
            }
        }
    }
    
    private void notifyMemberLeft(Party party, UUID playerId, boolean disbanded) {
        for (PartyEventListener listener : eventListeners) {
            try {
                listener.onMemberLeft(party, playerId, disbanded);
            } catch (Exception e) {
                LOGGER.error("Error in member left listener: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Listener interface for party events.
     */
    public interface PartyEventListener {
        default void onPartyCreated(Party party) {}
        default void onPartyDisbanded(Party party) {}
        default void onMemberJoined(Party party, UUID playerId) {}
        default void onMemberLeft(Party party, UUID playerId, boolean disbanded) {}
        default void onPartyEvent(Party party, String eventType, Map<String, Object> data) {}
    }
}
