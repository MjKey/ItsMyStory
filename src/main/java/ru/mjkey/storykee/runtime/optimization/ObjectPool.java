package ru.mjkey.storykee.runtime.optimization;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Generic object pool for reducing memory allocations.
 * Useful for frequently created/destroyed objects like execution contexts.
 * 
 * @param <T> The type of objects to pool
 */
public class ObjectPool<T> {
    
    private final Queue<T> pool;
    private final Supplier<T> factory;
    private final int maxSize;
    
    // Statistics
    private long borrows = 0;
    private long returns = 0;
    private long creates = 0;
    
    /**
     * Creates a new object pool.
     * 
     * @param factory Factory function to create new objects
     * @param maxSize Maximum number of objects to keep in the pool
     */
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.factory = factory;
        this.maxSize = maxSize;
    }
    
    /**
     * Borrows an object from the pool.
     * Creates a new one if the pool is empty.
     * 
     * @return An object from the pool or a new instance
     */
    public T borrow() {
        borrows++;
        T obj = pool.poll();
        if (obj == null) {
            creates++;
            return factory.get();
        }
        return obj;
    }
    
    /**
     * Returns an object to the pool.
     * If the pool is full, the object is discarded.
     * 
     * @param obj The object to return
     */
    public void release(T obj) {
        if (obj == null) return;
        
        returns++;
        if (pool.size() < maxSize) {
            pool.offer(obj);
        }
        // Otherwise, let GC handle it
    }
    
    /**
     * Gets the current size of the pool.
     */
    public int size() {
        return pool.size();
    }
    
    /**
     * Clears all objects from the pool.
     */
    public void clear() {
        pool.clear();
    }
    
    /**
     * Gets pool statistics.
     */
    public PoolStats getStats() {
        return new PoolStats(borrows, returns, creates, pool.size(), maxSize);
    }
    
    /**
     * Pool statistics.
     */
    public record PoolStats(long borrows, long returns, long creates, int currentSize, int maxSize) {
        public double reuseRate() {
            return borrows == 0 ? 0.0 : (double) (borrows - creates) / borrows;
        }
        
        @Override
        public String toString() {
            return String.format("PoolStats[borrows=%d, returns=%d, creates=%d, reuseRate=%.2f%%, size=%d/%d]",
                    borrows, returns, creates, reuseRate() * 100, currentSize, maxSize);
        }
    }
}
