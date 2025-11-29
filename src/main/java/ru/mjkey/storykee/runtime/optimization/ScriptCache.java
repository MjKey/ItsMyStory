package ru.mjkey.storykee.runtime.optimization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LRU cache for parsed AST nodes.
 * Avoids re-parsing scripts that haven't changed.
 */
public class ScriptCache {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptCache.class);
    
    private static final int DEFAULT_MAX_SIZE = 100;
    
    private final int maxSize;
    private final Map<String, CacheEntry> cache;
    
    // Statistics
    private long hits = 0;
    private long misses = 0;
    
    public ScriptCache() {
        this(DEFAULT_MAX_SIZE);
    }
    
    public ScriptCache(int maxSize) {
        this.maxSize = maxSize;
        // Use LinkedHashMap with access order for LRU behavior
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets a cached AST if available and not stale.
     * 
     * @param scriptId The script identifier
     * @param contentHash Hash of the script content
     * @return The cached AST or null if not found/stale
     */
    public ProgramNode get(String scriptId, String contentHash) {
        CacheEntry entry = cache.get(scriptId);
        
        if (entry == null) {
            misses++;
            return null;
        }
        
        // Check if content has changed
        if (!entry.contentHash.equals(contentHash)) {
            misses++;
            cache.remove(scriptId);
            return null;
        }
        
        hits++;
        entry.lastAccess = System.currentTimeMillis();
        return entry.ast;
    }
    
    /**
     * Stores a parsed AST in the cache.
     * 
     * @param scriptId The script identifier
     * @param contentHash Hash of the script content
     * @param ast The parsed AST
     */
    public void put(String scriptId, String contentHash, ProgramNode ast) {
        // Evict oldest entries if at capacity
        if (cache.size() >= maxSize) {
            evictOldest();
        }
        
        cache.put(scriptId, new CacheEntry(contentHash, ast));
    }
    
    /**
     * Invalidates a specific script in the cache.
     */
    public void invalidate(String scriptId) {
        cache.remove(scriptId);
    }
    
    /**
     * Clears the entire cache.
     */
    public void clear() {
        cache.clear();
        LOGGER.info("Script cache cleared");
    }
    
    /**
     * Gets cache statistics.
     */
    public CacheStats getStats() {
        return new CacheStats(hits, misses, cache.size(), maxSize);
    }
    
    /**
     * Evicts the oldest (least recently accessed) entry.
     */
    private void evictOldest() {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().lastAccess < oldestTime) {
                oldestTime = entry.getValue().lastAccess;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            LOGGER.debug("Evicted script from cache: {}", oldestKey);
        }
    }
    
    /**
     * Cache entry holding the AST and metadata.
     */
    private static class CacheEntry {
        final String contentHash;
        final ProgramNode ast;
        final long createdAt;
        long lastAccess;
        
        CacheEntry(String contentHash, ProgramNode ast) {
            this.contentHash = contentHash;
            this.ast = ast;
            this.createdAt = System.currentTimeMillis();
            this.lastAccess = this.createdAt;
        }
    }
    
    /**
     * Cache statistics.
     */
    public record CacheStats(long hits, long misses, int size, int maxSize) {
        public double hitRate() {
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats[hits=%d, misses=%d, hitRate=%.2f%%, size=%d/%d]",
                    hits, misses, hitRate() * 100, size, maxSize);
        }
    }
}
