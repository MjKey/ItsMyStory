package ru.mjkey.storykee.runtime.hotreload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Watches for file changes in the scripts directory using Java NIO WatchService.
 * Implements debouncing for rapid changes to avoid excessive reloads.
 * 
 * Requirements: 1.4, 16.1
 * - Detects file modifications within 2 seconds
 * - Supports debouncing for rapid changes
 */
public class FileWatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileWatcher.class);
    private static final String SCRIPT_EXTENSION = ".skee";
    
    // Debounce delay in milliseconds (wait for changes to settle)
    private static final long DEBOUNCE_DELAY_MS = 500;
    
    private static FileWatcher instance;
    
    private WatchService watchService;
    private final Map<WatchKey, Path> watchKeyToPath = new ConcurrentHashMap<>();
    private final Set<Path> watchedPaths = ConcurrentHashMap.newKeySet();
    
    // Debouncing: track pending changes and their scheduled reload time
    private final Map<Path, Long> pendingChanges = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor;
    
    // Listeners for file change events
    private final List<Consumer<FileChangeEvent>> listeners = new CopyOnWriteArrayList<>();
    
    // Watcher thread
    private Thread watcherThread;
    private volatile boolean running = false;
    
    private FileWatcher() {
        this.debounceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FileWatcher-Debounce");
            t.setDaemon(true);
            return t;
        });
    }
    
    public static FileWatcher getInstance() {
        if (instance == null) {
            instance = new FileWatcher();
        }
        return instance;
    }
    
    /**
     * Resets the singleton instance (for testing purposes).
     */
    public static void resetInstance() {
        if (instance != null) {
            instance.stop();
        }
        instance = null;
    }
    
    /**
     * Starts the file watcher.
     * Requirement 1.4: Detects file modifications.
     */
    public void start() {
        if (running) {
            LOGGER.warn("FileWatcher is already running");
            return;
        }
        
        try {
            watchService = FileSystems.getDefault().newWatchService();
            running = true;
            
            // Register all watched paths
            for (Path path : watchedPaths) {
                registerPath(path);
            }
            
            // Start watcher thread
            watcherThread = new Thread(this::watchLoop, "FileWatcher-Main");
            watcherThread.setDaemon(true);
            watcherThread.start();
            
            LOGGER.info("FileWatcher started");
            
        } catch (IOException e) {
            LOGGER.error("Failed to start FileWatcher", e);
            running = false;
        }
    }
    
    /**
     * Stops the file watcher.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Interrupt watcher thread
        if (watcherThread != null) {
            watcherThread.interrupt();
            try {
                watcherThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Close watch service
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                LOGGER.error("Error closing WatchService", e);
            }
        }
        
        // Shutdown debounce executor
        debounceExecutor.shutdown();
        try {
            if (!debounceExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                debounceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            debounceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        watchKeyToPath.clear();
        pendingChanges.clear();
        
        LOGGER.info("FileWatcher stopped");
    }
    
    /**
     * Adds a path to watch for changes.
     * Recursively watches all subdirectories.
     * 
     * @param path The directory path to watch
     */
    public void addWatchPath(Path path) {
        if (path == null || !Files.isDirectory(path)) {
            LOGGER.warn("Cannot watch path (not a directory): {}", path);
            return;
        }
        
        watchedPaths.add(path);
        
        if (running) {
            registerPath(path);
        }
        
        LOGGER.info("Added watch path: {}", path);
    }
    
    /**
     * Removes a path from watching.
     * 
     * @param path The directory path to stop watching
     */
    public void removeWatchPath(Path path) {
        watchedPaths.remove(path);
        
        // Cancel watch keys for this path and its subdirectories
        watchKeyToPath.entrySet().removeIf(entry -> {
            if (entry.getValue().startsWith(path)) {
                entry.getKey().cancel();
                return true;
            }
            return false;
        });
        
        LOGGER.info("Removed watch path: {}", path);
    }
    
    /**
     * Registers a file change listener.
     * 
     * @param listener The listener to add
     */
    public void addListener(Consumer<FileChangeEvent> listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a file change listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(Consumer<FileChangeEvent> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Registers a path and all its subdirectories with the watch service.
     */
    private void registerPath(Path path) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    try {
                        WatchKey key = dir.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                        watchKeyToPath.put(key, dir);
                        LOGGER.debug("Registered watch for directory: {}", dir);
                    } catch (IOException e) {
                        LOGGER.error("Failed to register watch for directory: {}", dir, e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to walk directory tree: {}", path, e);
        }
    }
    
    /**
     * Main watch loop that processes file system events.
     */
    private void watchLoop() {
        while (running) {
            try {
                // Wait for events (with timeout to allow checking running flag)
                WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);
                
                if (key == null) {
                    continue;
                }
                
                Path dir = watchKeyToPath.get(key);
                if (dir == null) {
                    key.cancel();
                    continue;
                }
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // Handle overflow
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        LOGGER.warn("WatchService overflow - some events may have been lost");
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();
                    Path fullPath = dir.resolve(fileName);
                    
                    // Handle new directories
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                        registerPath(fullPath);
                    }
                    
                    // Only process .skee files
                    if (isScriptFile(fullPath)) {
                        handleFileChange(fullPath, kind);
                    }
                }
                
                // Reset key to receive further events
                boolean valid = key.reset();
                if (!valid) {
                    watchKeyToPath.remove(key);
                    LOGGER.debug("Watch key invalidated for: {}", dir);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ClosedWatchServiceException e) {
                break;
            } catch (Exception e) {
                LOGGER.error("Error in watch loop", e);
            }
        }
    }
    
    /**
     * Handles a file change event with debouncing.
     * Requirement 16.1: Detects changes within 2 seconds.
     */
    private void handleFileChange(Path path, WatchEvent.Kind<?> kind) {
        FileChangeType changeType = getChangeType(kind);
        
        LOGGER.debug("File change detected: {} - {}", changeType, path);
        
        // For delete events, notify immediately (no debouncing needed)
        if (changeType == FileChangeType.DELETED) {
            notifyListeners(new FileChangeEvent(path, changeType));
            return;
        }
        
        // Debounce create and modify events
        long now = System.currentTimeMillis();
        pendingChanges.put(path, now);
        
        // Schedule debounced notification
        debounceExecutor.schedule(() -> {
            Long scheduledTime = pendingChanges.get(path);
            if (scheduledTime != null && scheduledTime == now) {
                pendingChanges.remove(path);
                notifyListeners(new FileChangeEvent(path, changeType));
            }
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Notifies all listeners of a file change event.
     */
    private void notifyListeners(FileChangeEvent event) {
        LOGGER.info("File change event: {} - {}", event.getChangeType(), event.getPath());
        
        for (Consumer<FileChangeEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in file change listener", e);
            }
        }
    }
    
    /**
     * Checks if a file is a Storykee script file.
     */
    private boolean isScriptFile(Path file) {
        return file != null && 
               file.getFileName().toString().toLowerCase().endsWith(SCRIPT_EXTENSION);
    }
    
    /**
     * Converts a WatchEvent kind to a FileChangeType.
     */
    private FileChangeType getChangeType(WatchEvent.Kind<?> kind) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            return FileChangeType.CREATED;
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            return FileChangeType.MODIFIED;
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            return FileChangeType.DELETED;
        }
        return FileChangeType.MODIFIED;
    }
    
    /**
     * Checks if the file watcher is running.
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Gets the number of watched directories.
     */
    public int getWatchedDirectoryCount() {
        return watchKeyToPath.size();
    }
    
    /**
     * Represents a file change event.
     */
    public static class FileChangeEvent {
        private final Path path;
        private final FileChangeType changeType;
        private final long timestamp;
        
        public FileChangeEvent(Path path, FileChangeType changeType) {
            this.path = path;
            this.changeType = changeType;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Path getPath() {
            return path;
        }
        
        public FileChangeType getChangeType() {
            return changeType;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "FileChangeEvent{" +
                   "path=" + path +
                   ", changeType=" + changeType +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
    
    /**
     * Types of file changes.
     */
    public enum FileChangeType {
        CREATED,
        MODIFIED,
        DELETED
    }
}
