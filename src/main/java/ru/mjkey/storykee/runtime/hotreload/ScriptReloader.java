package ru.mjkey.storykee.runtime.hotreload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ParseException;
import ru.mjkey.storykee.parser.StorykeeParserFacade;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;
import ru.mjkey.storykee.runtime.StorykeeRuntime;
import ru.mjkey.storykee.runtime.StorykeeRuntime.LoadedScript;
import ru.mjkey.storykee.runtime.executor.ScriptExecutor;
import ru.mjkey.storykee.runtime.hotreload.FileWatcher.FileChangeEvent;
import ru.mjkey.storykee.runtime.hotreload.FileWatcher.FileChangeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Handles script reloading when files change.
 * Preserves player variables and quest states during reload.
 * 
 * Requirements: 16.2, 16.3, 16.4, 16.5
 * - Parses and validates new script version
 * - Replaces old script with new one on success
 * - Keeps old version and logs error on failure
 * - Preserves player variables and quest states
 */
public class ScriptReloader {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptReloader.class);
    
    private static ScriptReloader instance;
    
    private final StorykeeParserFacade parserFacade = new StorykeeParserFacade();
    private final List<Consumer<ReloadEvent>> listeners = new CopyOnWriteArrayList<>();
    
    private boolean initialized = false;
    
    private ScriptReloader() {
    }
    
    public static ScriptReloader getInstance() {
        if (instance == null) {
            instance = new ScriptReloader();
        }
        return instance;
    }
    
    /**
     * Resets the singleton instance (for testing purposes).
     */
    public static void resetInstance() {
        instance = null;
    }
    
    /**
     * Initializes the script reloader and connects it to the file watcher.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("ScriptReloader already initialized");
            return;
        }
        
        // Register as listener for file changes
        FileWatcher.getInstance().addListener(this::handleFileChange);
        
        initialized = true;
        LOGGER.info("ScriptReloader initialized");
    }
    
    /**
     * Shuts down the script reloader.
     */
    public void shutdown() {
        FileWatcher.getInstance().removeListener(this::handleFileChange);
        initialized = false;
        LOGGER.info("ScriptReloader shutdown");
    }

    /**
     * Handles file change events from the FileWatcher.
     */
    private void handleFileChange(FileChangeEvent event) {
        Path path = event.getPath();
        FileChangeType changeType = event.getChangeType();
        
        LOGGER.debug("Handling file change: {} - {}", changeType, path);
        
        try {
            switch (changeType) {
                case CREATED:
                    handleScriptCreated(path);
                    break;
                case MODIFIED:
                    handleScriptModified(path);
                    break;
                case DELETED:
                    handleScriptDeleted(path);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("Error handling file change for {}: {}", path, e.getMessage(), e);
            notifyListeners(new ReloadEvent(path, ReloadStatus.FAILED, e.getMessage()));
        }
    }
    
    /**
     * Handles a newly created script file.
     * Requirement 16.2: Parses and validates the new script.
     */
    private void handleScriptCreated(Path path) {
        LOGGER.info("New script detected: {}", path);
        
        StorykeeRuntime runtime = StorykeeRuntime.getInstance();
        String scriptId = runtime.getScriptId(path);
        
        // Check if script already exists (shouldn't happen, but be safe)
        if (runtime.getScript(scriptId) != null) {
            LOGGER.warn("Script {} already loaded, treating as modification", scriptId);
            handleScriptModified(path);
            return;
        }
        
        // Load the new script
        LoadedScript loadedScript = runtime.loadScript(path);
        
        if (loadedScript != null) {
            LOGGER.info("Successfully loaded new script: {}", scriptId);
            notifyListeners(new ReloadEvent(path, ReloadStatus.SUCCESS, "Script loaded successfully"));
        } else {
            LOGGER.error("Failed to load new script: {}", scriptId);
            notifyListeners(new ReloadEvent(path, ReloadStatus.FAILED, "Failed to parse script"));
        }
    }
    
    /**
     * Handles a modified script file.
     * Requirements: 16.2, 16.3, 16.4, 16.5
     * - Parses and validates the new version
     * - Replaces old script on success
     * - Keeps old version on failure
     * - Preserves player variables and quest states
     */
    private void handleScriptModified(Path path) {
        LOGGER.info("Script modification detected: {}", path);
        
        StorykeeRuntime runtime = StorykeeRuntime.getInstance();
        String scriptId = runtime.getScriptId(path);
        
        // Get the old script (if exists)
        LoadedScript oldScript = runtime.getScript(scriptId);
        
        // Stop the script if it's currently running
        ScriptExecutor executor = ScriptExecutor.getInstance();
        if (executor.isRunning(scriptId)) {
            LOGGER.info("Stopping running script before reload: {}", scriptId);
            executor.stop(scriptId);
        }
        
        // Try to parse the new version
        // Requirement 16.2: Parse and validate new version
        try {
            String source = Files.readString(path);
            ProgramNode newAst = parserFacade.parse(source, path.getFileName().toString());
            
            // Parsing succeeded - replace the old script
            // Requirement 16.3: Replace old script with new one
            LoadedScript newScript = new LoadedScript(
                scriptId,
                path,
                newAst,
                System.currentTimeMillis()
            );
            
            // Note: Player variables and quest states are managed separately
            // by VariableManager and QuestManager, so they are automatically preserved
            // Requirement 16.5: Preserve player variables and quest states
            
            // Update the script in runtime
            runtime.unloadScript(scriptId);
            runtime.loadScript(path);
            
            LOGGER.info("Successfully reloaded script: {}", scriptId);
            notifyListeners(new ReloadEvent(path, ReloadStatus.SUCCESS, "Script reloaded successfully"));
            
        } catch (ParseException e) {
            // Requirement 16.4: Keep old version and log error on failure
            LOGGER.error("Parse error reloading script {}: {}", scriptId, e.getMessage());
            LOGGER.error("Keeping old version of script: {}", scriptId);
            notifyListeners(new ReloadEvent(path, ReloadStatus.FAILED, 
                "Parse error: " + e.getMessage()));
            
        } catch (IOException e) {
            // Requirement 16.4: Keep old version and log error on failure
            LOGGER.error("IO error reloading script {}: {}", scriptId, e.getMessage());
            LOGGER.error("Keeping old version of script: {}", scriptId);
            notifyListeners(new ReloadEvent(path, ReloadStatus.FAILED, 
                "IO error: " + e.getMessage()));
        }
    }
    
    /**
     * Handles a deleted script file.
     */
    private void handleScriptDeleted(Path path) {
        LOGGER.info("Script deletion detected: {}", path);
        
        StorykeeRuntime runtime = StorykeeRuntime.getInstance();
        String scriptId = runtime.getScriptId(path);
        
        // Stop the script if it's currently running
        ScriptExecutor executor = ScriptExecutor.getInstance();
        if (executor.isRunning(scriptId)) {
            LOGGER.info("Stopping running script before unload: {}", scriptId);
            executor.stop(scriptId);
        }
        
        // Unload the script
        runtime.unloadScript(scriptId);
        
        LOGGER.info("Unloaded deleted script: {}", scriptId);
        notifyListeners(new ReloadEvent(path, ReloadStatus.UNLOADED, "Script unloaded"));
    }
    
    /**
     * Manually triggers a reload of a specific script.
     * 
     * @param scriptId The ID of the script to reload
     * @return true if reload was successful, false otherwise
     */
    public boolean reloadScript(String scriptId) {
        StorykeeRuntime runtime = StorykeeRuntime.getInstance();
        Path scriptPath = runtime.getScriptPath(scriptId);
        
        if (!Files.exists(scriptPath)) {
            LOGGER.error("Script file not found: {}", scriptPath);
            return false;
        }
        
        handleScriptModified(scriptPath);
        
        // Check if the script was successfully reloaded
        LoadedScript script = runtime.getScript(scriptId);
        return script != null;
    }
    
    /**
     * Reloads all scripts.
     * 
     * @return The number of scripts successfully reloaded
     */
    public int reloadAllScripts() {
        LOGGER.info("Reloading all scripts...");
        
        StorykeeRuntime runtime = StorykeeRuntime.getInstance();
        List<Path> scriptPaths = runtime.scanForScripts();
        
        int successCount = 0;
        for (Path path : scriptPaths) {
            String scriptId = runtime.getScriptId(path);
            if (reloadScript(scriptId)) {
                successCount++;
            }
        }
        
        LOGGER.info("Reloaded {}/{} scripts", successCount, scriptPaths.size());
        return successCount;
    }
    
    /**
     * Registers a reload event listener.
     * 
     * @param listener The listener to add
     */
    public void addListener(Consumer<ReloadEvent> listener) {
        listeners.add(listener);
    }
    
    /**
     * Removes a reload event listener.
     * 
     * @param listener The listener to remove
     */
    public void removeListener(Consumer<ReloadEvent> listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifies all listeners of a reload event.
     */
    private void notifyListeners(ReloadEvent event) {
        for (Consumer<ReloadEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in reload event listener", e);
            }
        }
    }
    
    /**
     * Checks if the reloader is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Represents a script reload event.
     */
    public static class ReloadEvent {
        private final Path path;
        private final ReloadStatus status;
        private final String message;
        private final long timestamp;
        
        public ReloadEvent(Path path, ReloadStatus status, String message) {
            this.path = path;
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public Path getPath() {
            return path;
        }
        
        public ReloadStatus getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "ReloadEvent{" +
                   "path=" + path +
                   ", status=" + status +
                   ", message='" + message + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
    
    /**
     * Status of a script reload operation.
     */
    public enum ReloadStatus {
        SUCCESS,
        FAILED,
        UNLOADED
    }
}