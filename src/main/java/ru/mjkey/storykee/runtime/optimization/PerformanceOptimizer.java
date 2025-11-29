package ru.mjkey.storykee.runtime.optimization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;
import ru.mjkey.storykee.runtime.context.ExecutionContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Central performance optimization manager for Storykee runtime.
 * Provides caching, pooling, and other optimizations.
 */
public class PerformanceOptimizer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceOptimizer.class);
    
    private static PerformanceOptimizer instance;
    
    private final ScriptCache scriptCache;
    private final ObjectPool<ExecutionContext> contextPool;
    
    // Configuration
    private boolean cachingEnabled = true;
    private boolean poolingEnabled = true;
    
    private PerformanceOptimizer() {
        this.scriptCache = new ScriptCache(100);
        this.contextPool = new ObjectPool<>(
            () -> new ExecutionContext("pooled-" + UUID.randomUUID(), null),
            50
        );
    }
    
    public static synchronized PerformanceOptimizer getInstance() {
        if (instance == null) {
            instance = new PerformanceOptimizer();
        }
        return instance;
    }
    
    /**
     * Gets a cached AST or returns null if not cached.
     */
    public ProgramNode getCachedAST(String scriptId, String scriptContent) {
        if (!cachingEnabled) return null;
        
        String hash = computeHash(scriptContent);
        return scriptCache.get(scriptId, hash);
    }
    
    /**
     * Caches a parsed AST.
     */
    public void cacheAST(String scriptId, String scriptContent, ProgramNode ast) {
        if (!cachingEnabled) return;
        
        String hash = computeHash(scriptContent);
        scriptCache.put(scriptId, hash, ast);
    }
    
    /**
     * Invalidates a cached script.
     */
    public void invalidateScript(String scriptId) {
        scriptCache.invalidate(scriptId);
    }
    
    /**
     * Borrows an execution context from the pool.
     * Note: The context should be properly initialized before use.
     */
    public ExecutionContext borrowContext() {
        if (!poolingEnabled) {
            return new ExecutionContext("temp-" + UUID.randomUUID(), null);
        }
        return contextPool.borrow();
    }
    
    /**
     * Returns an execution context to the pool.
     */
    public void releaseContext(ExecutionContext context) {
        if (!poolingEnabled) return;
        // Note: In a real implementation, we'd want to reset the context state
        contextPool.release(context);
    }
    
    /**
     * Computes a hash of the script content for cache validation.
     */
    private String computeHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return String.valueOf(content.hashCode());
        }
    }
    
    /**
     * Enables or disables caching.
     */
    public void setCachingEnabled(boolean enabled) {
        this.cachingEnabled = enabled;
        if (!enabled) {
            scriptCache.clear();
        }
    }
    
    /**
     * Enables or disables object pooling.
     */
    public void setPoolingEnabled(boolean enabled) {
        this.poolingEnabled = enabled;
        if (!enabled) {
            contextPool.clear();
        }
    }
    
    /**
     * Clears all caches and pools.
     */
    public void clearAll() {
        scriptCache.clear();
        contextPool.clear();
        LOGGER.info("All caches and pools cleared");
    }
    
    /**
     * Gets performance statistics.
     */
    public PerformanceStats getStats() {
        return new PerformanceStats(
            scriptCache.getStats(),
            contextPool.getStats()
        );
    }
    
    /**
     * Logs current performance statistics.
     */
    public void logStats() {
        PerformanceStats stats = getStats();
        LOGGER.info("Performance Statistics:");
        LOGGER.info("  Script Cache: {}", stats.cacheStats());
        LOGGER.info("  Context Pool: {}", stats.poolStats());
    }
    
    /**
     * Combined performance statistics.
     */
    public record PerformanceStats(
        ScriptCache.CacheStats cacheStats,
        ObjectPool.PoolStats poolStats
    ) {}
}
