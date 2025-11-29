package ru.mjkey.storykee.systems.party;

import java.util.*;

/**
 * Represents a party/team of players.
 * 
 * Requirements: 23.1, 23.2
 */
public class Party {
    
    private final String id;
    private final UUID leaderId;
    private final Set<UUID> members;
    private final Map<String, Object> customData;
    private final long createdAt;
    private int maxSize;
    
    public Party(String id, UUID leaderId) {
        this.id = id;
        this.leaderId = leaderId;
        this.members = new LinkedHashSet<>();
        this.members.add(leaderId);
        this.customData = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
        this.maxSize = 10;
    }
    
    public String getId() {
        return id;
    }
    
    public UUID getLeaderId() {
        return leaderId;
    }
    
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Adds a member to the party.
     * Requirement 23.2: Add player to party member list
     * 
     * @param playerId Player UUID
     * @return true if added successfully
     */
    public boolean addMember(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (members.size() >= maxSize) {
            return false;
        }
        return members.add(playerId);
    }
    
    /**
     * Removes a member from the party.
     * Requirement 23.3: Remove player from party
     * 
     * @param playerId Player UUID
     * @return true if removed
     */
    public boolean removeMember(UUID playerId) {
        if (playerId == null || playerId.equals(leaderId)) {
            return false; // Cannot remove leader
        }
        return members.remove(playerId);
    }
    
    /**
     * Checks if a player is a member.
     * 
     * @param playerId Player UUID
     * @return true if member
     */
    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }
    
    /**
     * Checks if a player is the leader.
     * 
     * @param playerId Player UUID
     * @return true if leader
     */
    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }
    
    /**
     * Gets custom data value.
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) customData.get(key);
    }
    
    /**
     * Sets custom data value.
     */
    public void setData(String key, Object value) {
        customData.put(key, value);
    }
    
    /**
     * Removes custom data value.
     */
    public void removeData(String key) {
        customData.remove(key);
    }
    
    /**
     * Checks if party has custom data.
     */
    public boolean hasData(String key) {
        return customData.containsKey(key);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Party party = (Party) o;
        return Objects.equals(id, party.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
