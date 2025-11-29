package ru.mjkey.storykee.runtime.optimization;

import java.util.concurrent.ConcurrentHashMap;

/**
 * String interning utility for reducing memory usage.
 * Commonly used strings (like variable names, function names) are interned
 * to avoid duplicate string allocations.
 */
public class StringInterner {
    
    private static final StringInterner INSTANCE = new StringInterner();
    
    private final ConcurrentHashMap<String, String> internedStrings;
    private final int maxSize;
    
    // Statistics
    private long lookups = 0;
    private long hits = 0;
    
    private StringInterner() {
        this(10000);
    }
    
    private StringInterner(int maxSize) {
        this.internedStrings = new ConcurrentHashMap<>();
        this.maxSize = maxSize;
    }
    
    public static StringInterner getInstance() {
        return INSTANCE;
    }
    
    /**
     * Interns a string, returning a canonical representation.
     * If the string is already interned, returns the existing instance.
     * 
     * @param str The string to intern
     * @return The interned string
     */
    public String intern(String str) {
        if (str == null) return null;
        
        lookups++;
        
        // Check if already interned
        String existing = internedStrings.get(str);
        if (existing != null) {
            hits++;
            return existing;
        }
        
        // Don't intern if at capacity (to prevent memory issues)
        if (internedStrings.size() >= maxSize) {
            return str;
        }
        
        // Intern the string
        internedStrings.putIfAbsent(str, str);
        return internedStrings.get(str);
    }
    
    /**
     * Interns a string only if it's short enough.
     * Long strings are not worth interning.
     * 
     * @param str The string to potentially intern
     * @param maxLength Maximum length to intern
     * @return The interned string or original if too long
     */
    public String internIfShort(String str, int maxLength) {
        if (str == null || str.length() > maxLength) {
            return str;
        }
        return intern(str);
    }
    
    /**
     * Clears all interned strings.
     */
    public void clear() {
        internedStrings.clear();
    }
    
    /**
     * Gets the number of interned strings.
     */
    public int size() {
        return internedStrings.size();
    }
    
    /**
     * Gets interning statistics.
     */
    public InternerStats getStats() {
        return new InternerStats(lookups, hits, internedStrings.size(), maxSize);
    }
    
    /**
     * Interning statistics.
     */
    public record InternerStats(long lookups, long hits, int size, int maxSize) {
        public double hitRate() {
            return lookups == 0 ? 0.0 : (double) hits / lookups;
        }
        
        @Override
        public String toString() {
            return String.format("InternerStats[lookups=%d, hits=%d, hitRate=%.2f%%, size=%d/%d]",
                    lookups, hits, hitRate() * 100, size, maxSize);
        }
    }
}
