package ru.mjkey.storykee.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.parser.ParseException;
import ru.mjkey.storykee.parser.StorykeeParserFacade;
import ru.mjkey.storykee.parser.ast.statement.ProgramNode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Main runtime environment for Storykee scripts.
 * Manages script loading, execution, and integration with Minecraft.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.5
 */
public class StorykeeRuntime {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorykeeRuntime.class);
    private static final String SCRIPT_EXTENSION = ".skee";
    
    private static StorykeeRuntime instance;
    
    private final Path storykeeDirectory;
    private final Path scriptsDirectory;
    private final Path assetsDirectory;
    private final Path dataDirectory;
    
    // Loaded scripts cache: scriptId -> parsed AST
    private final Map<String, LoadedScript> loadedScripts = new ConcurrentHashMap<>();
    
    // Parser facade for parsing scripts
    private final StorykeeParserFacade parserFacade = new StorykeeParserFacade();
    
    // Variable manager for player and global variables
    private ru.mjkey.storykee.runtime.variables.VariableManager variableManager;
    
    private boolean initialized = false;
    
    private StorykeeRuntime(Path minecraftDirectory) {
        this.storykeeDirectory = minecraftDirectory.resolve("itsmystory");
        this.scriptsDirectory = storykeeDirectory.resolve("scripts");
        this.assetsDirectory = storykeeDirectory.resolve("assets");
        this.dataDirectory = storykeeDirectory.resolve("data");
    }
    
    public static StorykeeRuntime getInstance(Path minecraftDirectory) {
        if (instance == null) {
            instance = new StorykeeRuntime(minecraftDirectory);
        }
        return instance;
    }
    
    public static StorykeeRuntime getInstance() {
        if (instance == null) {
            throw new IllegalStateException("StorykeeRuntime not initialized. Call getInstance(Path) first.");
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
     * Initializes the runtime environment.
     * Creates necessary directories if they don't exist.
     * Scans and loads all .skee scripts.
     * 
     * Requirements: 1.1, 1.2
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("StorykeeRuntime already initialized");
            return;
        }
        
        try {
            // Create main directory (Requirement 1.1)
            if (!Files.exists(storykeeDirectory)) {
                Files.createDirectories(storykeeDirectory);
                LOGGER.info("Created Storykee directory: {}", storykeeDirectory);
            }
            
            // Create scripts directory (Requirement 1.2)
            if (!Files.exists(scriptsDirectory)) {
                Files.createDirectories(scriptsDirectory);
                LOGGER.info("Created scripts directory: {}", scriptsDirectory);
            }
            
            // Create assets directory with subdirectories (Requirement 1.2, 1.5)
            createAssetDirectories();
            
            // Create data directory with subdirectories (Requirement 1.2)
            createDataDirectories();
            
            // Initialize variable manager
            this.variableManager = new ru.mjkey.storykee.runtime.variables.VariableManager(dataDirectory);
            variableManager.loadAll();
            
            // Scan and load all scripts (Requirement 1.3)
            scanAndLoadScripts();
            
            initialized = true;
            LOGGER.info("StorykeeRuntime initialized successfully. Loaded {} scripts.", loadedScripts.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize StorykeeRuntime", e);
            throw new RuntimeException("Failed to initialize StorykeeRuntime", e);
        }
    }
    
    /**
     * Creates asset directories with subdirectories.
     * Requirement 1.5: Provides paths to textures, sounds, and models subdirectories.
     */
    private void createAssetDirectories() throws IOException {
        Path texturesDir = assetsDirectory.resolve("textures");
        Path modelsDir = assetsDirectory.resolve("models");
        Path soundsDir = assetsDirectory.resolve("sounds");
        Path langDir = assetsDirectory.resolve("lang");
        Path schematicsDir = assetsDirectory.resolve("schematics");
        
        createDirectoryIfNotExists(texturesDir);
        createDirectoryIfNotExists(modelsDir);
        createDirectoryIfNotExists(soundsDir);
        createDirectoryIfNotExists(langDir);
        createDirectoryIfNotExists(schematicsDir);
    }
    
    /**
     * Creates data directories with subdirectories.
     */
    private void createDataDirectories() throws IOException {
        Path playerVarsDir = dataDirectory.resolve("player_variables");
        createDirectoryIfNotExists(playerVarsDir);
    }
    
    private void createDirectoryIfNotExists(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            LOGGER.info("Created directory: {}", dir);
        }
    }
    
    /**
     * Scans the scripts directory for all .skee files and loads them.
     * Requirement 1.3: Identifies all files with .skee extension in the scripts directory.
     * 
     * @return List of discovered script paths
     */
    public List<Path> scanForScripts() {
        List<Path> scriptPaths = new ArrayList<>();
        
        if (!Files.exists(scriptsDirectory)) {
            LOGGER.warn("Scripts directory does not exist: {}", scriptsDirectory);
            return scriptPaths;
        }
        
        try {
            Files.walkFileTree(scriptsDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (isScriptFile(file)) {
                        scriptPaths.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    LOGGER.warn("Failed to access file: {}", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("Error scanning scripts directory", e);
        }
        
        LOGGER.info("Found {} script files", scriptPaths.size());
        return scriptPaths;
    }
    
    /**
     * Checks if a file is a Storykee script file.
     * Requirement 1.3: Identifies files with .skee extension.
     */
    public boolean isScriptFile(Path file) {
        return file != null && 
               Files.isRegularFile(file) && 
               file.getFileName().toString().toLowerCase().endsWith(SCRIPT_EXTENSION);
    }
    
    /**
     * Scans and loads all scripts from the scripts directory.
     */
    private void scanAndLoadScripts() {
        List<Path> scriptPaths = scanForScripts();
        
        for (Path scriptPath : scriptPaths) {
            try {
                loadScript(scriptPath);
            } catch (Exception e) {
                LOGGER.error("Failed to load script: {}", scriptPath, e);
            }
        }
    }
    
    /**
     * Loads a single script from the given path.
     * 
     * @param scriptPath Path to the script file
     * @return The loaded script, or null if loading failed
     */
    public LoadedScript loadScript(Path scriptPath) {
        String scriptId = getScriptId(scriptPath);
        
        try {
            String source = Files.readString(scriptPath);
            ProgramNode ast = parserFacade.parse(source, scriptPath.getFileName().toString());
            
            LoadedScript loadedScript = new LoadedScript(
                scriptId,
                scriptPath,
                ast,
                System.currentTimeMillis()
            );
            
            loadedScripts.put(scriptId, loadedScript);
            LOGGER.info("Loaded script: {} from {}", scriptId, scriptPath);
            
            // Execute the script to register dialogues, quests, event handlers, etc.
            executeScript(loadedScript);
            
            return loadedScript;
            
        } catch (ParseException e) {
            LOGGER.error("Parse error in script {}: {}", scriptPath, e.getMessage());
            return null;
        } catch (IOException e) {
            LOGGER.error("IO error loading script {}: {}", scriptPath, e.getMessage());
            return null;
        }
    }
    
    /**
     * Executes a loaded script to register its declarations (dialogues, quests, event handlers).
     * 
     * @param script The loaded script to execute
     */
    public void executeScript(LoadedScript script) {
        if (script == null || script.getAst() == null) {
            return;
        }
        
        try {
            ru.mjkey.storykee.runtime.executor.ASTInterpreter interpreter = getInterpreter();
            ru.mjkey.storykee.runtime.context.ExecutionContext context = 
                new ru.mjkey.storykee.runtime.context.ExecutionContext(script.getScriptId());
            
            interpreter.visitProgram(script.getAst(), context);
            LOGGER.info("Executed script: {} - registered declarations", script.getScriptId());
            
        } catch (Exception e) {
            LOGGER.error("Error executing script {}: {}", script.getScriptId(), e.getMessage(), e);
        }
    }
    
    /**
     * Unloads a script by its ID.
     * 
     * @param scriptId The script ID to unload
     */
    public void unloadScript(String scriptId) {
        LoadedScript removed = loadedScripts.remove(scriptId);
        if (removed != null) {
            LOGGER.info("Unloaded script: {}", scriptId);
        }
    }
    
    /**
     * Gets a loaded script by its ID.
     * 
     * @param scriptId The script ID
     * @return The loaded script, or null if not found
     */
    public LoadedScript getScript(String scriptId) {
        return loadedScripts.get(scriptId);
    }
    
    /**
     * Gets all loaded scripts.
     * 
     * @return Unmodifiable map of loaded scripts
     */
    public Map<String, LoadedScript> getAllScripts() {
        return Collections.unmodifiableMap(loadedScripts);
    }
    
    /**
     * Generates a script ID from its path.
     * The ID is relative to the scripts directory.
     */
    public String getScriptId(Path scriptPath) {
        Path relativePath = scriptsDirectory.relativize(scriptPath);
        String pathStr = relativePath.toString().replace('\\', '/');
        
        // Remove .skee extension
        if (pathStr.toLowerCase().endsWith(SCRIPT_EXTENSION)) {
            pathStr = pathStr.substring(0, pathStr.length() - SCRIPT_EXTENSION.length());
        }
        
        return pathStr;
    }
    
    /**
     * Gets the path for a script ID.
     */
    public Path getScriptPath(String scriptId) {
        return scriptsDirectory.resolve(scriptId + SCRIPT_EXTENSION);
    }
    
    // Asset path accessors (Requirement 1.5)
    
    public Path getTexturesDirectory() {
        return assetsDirectory.resolve("textures");
    }
    
    public Path getModelsDirectory() {
        return assetsDirectory.resolve("models");
    }
    
    public Path getSoundsDirectory() {
        return assetsDirectory.resolve("sounds");
    }
    
    public Path getLangDirectory() {
        return assetsDirectory.resolve("lang");
    }
    
    public Path getSchematicsDirectory() {
        return assetsDirectory.resolve("schematics");
    }
    
    public Path getPlayerVariablesDirectory() {
        return dataDirectory.resolve("player_variables");
    }
    
    public Path getStorykeeDirectory() {
        return storykeeDirectory;
    }
    
    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }
    
    public Path getAssetsDirectory() {
        return assetsDirectory;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the variable manager for player and global variables.
     */
    public ru.mjkey.storykee.runtime.variables.VariableManager getVariableManager() {
        return variableManager;
    }
    
    /**
     * Gets or creates an AST interpreter for script execution.
     */
    public ru.mjkey.storykee.runtime.executor.ASTInterpreter getInterpreter() {
        return new ru.mjkey.storykee.runtime.executor.ASTInterpreter();
    }
    
    /**
     * Executes all loaded scripts for a specific player.
     * Called when player presses the start story keybinding.
     * 
     * @param player The player to execute scripts for
     */
    public void executeForPlayer(net.minecraft.server.level.ServerPlayer player) {
        if (!initialized) {
            LOGGER.warn("Cannot execute scripts - runtime not initialized");
            return;
        }
        
        if (loadedScripts.isEmpty()) {
            LOGGER.info("No scripts loaded to execute");
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eНет загруженных историй"));
            return;
        }
        
        LOGGER.info("Executing {} scripts for player {}", loadedScripts.size(), player.getName().getString());
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a▶ Запуск истории..."));
        
        for (LoadedScript script : loadedScripts.values()) {
            try {
                LOGGER.info("Executing script: {}", script.getScriptId());
                
                // Create execution context for this player
                ru.mjkey.storykee.runtime.context.ExecutionContext context = 
                    new ru.mjkey.storykee.runtime.context.ExecutionContext(script.getScriptId(), player.getUUID());
                
                // Execute the script AST
                ru.mjkey.storykee.runtime.executor.ASTInterpreter interpreter = 
                    new ru.mjkey.storykee.runtime.executor.ASTInterpreter();
                interpreter.visitProgram(script.getAst(), context);
                
                LOGGER.info("Script {} executed successfully", script.getScriptId());
                
            } catch (Exception e) {
                LOGGER.error("Error executing script {}: {}", script.getScriptId(), e.getMessage(), e);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cОшибка выполнения скрипта: " + e.getMessage()));
            }
        }
        
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a✓ История запущена!"));
        
        // Fire playerJoin event for this player since they're already in the game
        // This triggers the on playerJoin handlers that were just registered
        firePlayerJoinEvent(player);
    }
    
    /**
     * Fires the playerJoin event for a player.
     * Called after scripts are executed to trigger initial setup.
     */
    private void firePlayerJoinEvent(net.minecraft.server.level.ServerPlayer player) {
        ru.mjkey.storykee.events.EventData data = new ru.mjkey.storykee.events.EventData("playerJoin");
        data.set("player", player.getUUID());
        data.set("playerName", player.getName().getString());
        data.set("playerId", player.getStringUUID());
        data.set("playerEntity", player);
        
        LOGGER.info("Firing playerJoin event for player {}", player.getName().getString());
        ru.mjkey.storykee.events.EventManager.getInstance().fireEvent("playerJoin", data);
    }
    
    /**
     * Represents a loaded script with its AST and metadata.
     */
    public static class LoadedScript {
        private final String scriptId;
        private final Path path;
        private final ProgramNode ast;
        private final long loadedAt;
        
        public LoadedScript(String scriptId, Path path, ProgramNode ast, long loadedAt) {
            this.scriptId = scriptId;
            this.path = path;
            this.ast = ast;
            this.loadedAt = loadedAt;
        }
        
        public String getScriptId() {
            return scriptId;
        }
        
        public Path getPath() {
            return path;
        }
        
        public ProgramNode getAst() {
            return ast;
        }
        
        public long getLoadedAt() {
            return loadedAt;
        }
    }
}
