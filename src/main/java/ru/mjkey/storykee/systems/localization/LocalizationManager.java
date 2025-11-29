package ru.mjkey.storykee.systems.localization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mjkey.storykee.runtime.StorykeeRuntime;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages localization and translations for the Storykee system.
 * Handles language file loading, translation key resolution, fallback mechanism,
 * and language switching.
 * 
 * Requirements: 26.1, 26.2, 26.3, 26.4
 */
public class LocalizationManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalizationManager.class);
    
    // Pattern for translation keys: @{key.name} or @key.name
    private static final Pattern TRANSLATION_KEY_PATTERN = Pattern.compile("@\\{([^}]+)}|@([a-zA-Z0-9_.]+)");
    
    // Default language code
    public static final String DEFAULT_LANGUAGE = "en_us";
    
    // Language file extension
    private static final String LANG_FILE_EXTENSION = ".json";
    
    private static LocalizationManager instance;
    
    private final Gson gson;
    
    // Current language code
    private String currentLanguage;
    
    // Loaded language files: languageCode -> translations
    private final Map<String, Map<String, String>> loadedLanguages;
    
    // Available languages (discovered from files)
    private final Set<String> availableLanguages;
    
    // Listeners for language change events
    private final List<LanguageChangeListener> languageChangeListeners;
    
    // Cache for resolved translations
    private final Map<String, String> translationCache;

    private LocalizationManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.currentLanguage = DEFAULT_LANGUAGE;
        this.loadedLanguages = new ConcurrentHashMap<>();
        this.availableLanguages = ConcurrentHashMap.newKeySet();
        this.languageChangeListeners = new ArrayList<>();
        this.translationCache = new ConcurrentHashMap<>();
    }
    
    public static LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }
    
    public static void resetInstance() {
        instance = null;
    }
    
    // ===== Initialization =====
    
    public void initialize() {
        LOGGER.info("Initializing LocalizationManager...");
        scanAvailableLanguages();
        loadLanguage(DEFAULT_LANGUAGE);
        if (!currentLanguage.equals(DEFAULT_LANGUAGE)) {
            loadLanguage(currentLanguage);
        }
        LOGGER.info("LocalizationManager initialized. Available languages: {}", availableLanguages);
    }
    
    public void scanAvailableLanguages() {
        availableLanguages.clear();
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path langDir = runtime.getLangDirectory();
            
            if (!Files.exists(langDir)) {
                LOGGER.warn("Language directory does not exist: {}", langDir);
                availableLanguages.add(DEFAULT_LANGUAGE);
                return;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir, "*" + LANG_FILE_EXTENSION)) {
                for (Path file : stream) {
                    String fileName = file.getFileName().toString();
                    String langCode = fileName.substring(0, fileName.length() - LANG_FILE_EXTENSION.length());
                    availableLanguages.add(langCode.toLowerCase());
                }
            }
            
            Path scriptsDir = runtime.getScriptsDirectory();
            if (Files.exists(scriptsDir)) {
                Files.walk(scriptsDir, 3)
                    .filter(p -> p.getFileName().toString().equals("lang"))
                    .filter(Files::isDirectory)
                    .forEach(this::scanStoryLangDirectory);
            }
        } catch (Exception e) {
            LOGGER.error("Error scanning language files", e);
        }
        availableLanguages.add(DEFAULT_LANGUAGE);
    }
    
    private void scanStoryLangDirectory(Path langDir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(langDir, "*" + LANG_FILE_EXTENSION)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                String langCode = fileName.substring(0, fileName.length() - LANG_FILE_EXTENSION.length());
                availableLanguages.add(langCode.toLowerCase());
            }
        } catch (IOException e) {
            LOGGER.warn("Error scanning story lang directory: {}", langDir, e);
        }
    }

    // ===== Language Loading =====
    
    public boolean loadLanguage(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return false;
        }
        languageCode = languageCode.toLowerCase();
        if (loadedLanguages.containsKey(languageCode)) {
            return true;
        }
        
        Map<String, String> translations = new HashMap<>();
        try {
            StorykeeRuntime runtime = StorykeeRuntime.getInstance();
            Path mainLangFile = runtime.getLangDirectory().resolve(languageCode + LANG_FILE_EXTENSION);
            if (Files.exists(mainLangFile)) {
                loadLanguageFile(mainLangFile, translations);
            }
            
            Path scriptsDir = runtime.getScriptsDirectory();
            if (Files.exists(scriptsDir)) {
                String finalLangCode = languageCode;
                Files.walk(scriptsDir, 3)
                    .filter(p -> p.getFileName().toString().equals("lang"))
                    .filter(Files::isDirectory)
                    .forEach(langDir -> {
                        Path storyLangFile = langDir.resolve(finalLangCode + LANG_FILE_EXTENSION);
                        if (Files.exists(storyLangFile)) {
                            loadLanguageFile(storyLangFile, translations);
                        }
                    });
            }
            
            if (translations.isEmpty() && !languageCode.equals(DEFAULT_LANGUAGE)) {
                LOGGER.warn("No translations found for language: {}", languageCode);
                return false;
            }
            
            loadedLanguages.put(languageCode, translations);
            LOGGER.info("Loaded language '{}' with {} translations", languageCode, translations.size());
            return true;
        } catch (Exception e) {
            LOGGER.error("Error loading language: {}", languageCode, e);
            return false;
        }
    }
    
    private void loadLanguageFile(Path file, Map<String, String> translations) {
        try {
            String content = Files.readString(file);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> fileTranslations = gson.fromJson(content, type);
            if (fileTranslations != null) {
                translations.putAll(fileTranslations);
            }
        } catch (Exception e) {
            LOGGER.warn("Error loading language file: {}", file, e);
        }
    }
    
    public void reloadLanguages() {
        Set<String> languagesToReload = new HashSet<>(loadedLanguages.keySet());
        loadedLanguages.clear();
        translationCache.clear();
        for (String langCode : languagesToReload) {
            loadLanguage(langCode);
        }
    }

    // ===== Translation Resolution =====
    
    public String translate(String key) {
        if (key == null || key.isEmpty()) {
            return key;
        }
        String cacheKey = currentLanguage + ":" + key;
        String cached = translationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String translation = resolveTranslation(key);
        translationCache.put(cacheKey, translation);
        return translation;
    }
    
    public String translate(String key, Object... args) {
        String translation = translate(key);
        if (args == null || args.length == 0) {
            return translation;
        }
        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return translation;
    }
    
    private String resolveTranslation(String key) {
        Map<String, String> currentTranslations = loadedLanguages.get(currentLanguage);
        if (currentTranslations != null && currentTranslations.containsKey(key)) {
            return currentTranslations.get(key);
        }
        if (!currentLanguage.equals(DEFAULT_LANGUAGE)) {
            Map<String, String> defaultTranslations = loadedLanguages.get(DEFAULT_LANGUAGE);
            if (defaultTranslations != null && defaultTranslations.containsKey(key)) {
                return defaultTranslations.get(key);
            }
        }
        return key;
    }
    
    public boolean hasTranslation(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        Map<String, String> currentTranslations = loadedLanguages.get(currentLanguage);
        if (currentTranslations != null && currentTranslations.containsKey(key)) {
            return true;
        }
        if (!currentLanguage.equals(DEFAULT_LANGUAGE)) {
            Map<String, String> defaultTranslations = loadedLanguages.get(DEFAULT_LANGUAGE);
            return defaultTranslations != null && defaultTranslations.containsKey(key);
        }
        return false;
    }
    
    public String processTranslationKeys(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Matcher matcher = TRANSLATION_KEY_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String translation = translate(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(translation));
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    public boolean containsTranslationKeys(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return TRANSLATION_KEY_PATTERN.matcher(text).find();
    }

    // ===== Language Switching =====
    
    public boolean setLanguage(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return false;
        }
        languageCode = languageCode.toLowerCase();
        if (languageCode.equals(currentLanguage)) {
            return true;
        }
        if (!loadedLanguages.containsKey(languageCode)) {
            if (!loadLanguage(languageCode)) {
                return false;
            }
        }
        String oldLanguage = currentLanguage;
        currentLanguage = languageCode;
        translationCache.clear();
        fireLanguageChanged(oldLanguage, currentLanguage);
        return true;
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public Set<String> getAvailableLanguages() {
        return Collections.unmodifiableSet(availableLanguages);
    }
    
    public boolean isLanguageAvailable(String languageCode) {
        if (languageCode == null) {
            return false;
        }
        return availableLanguages.contains(languageCode.toLowerCase());
    }
    
    // ===== Event Listeners =====
    
    public void addLanguageChangeListener(LanguageChangeListener listener) {
        if (listener != null && !languageChangeListeners.contains(listener)) {
            languageChangeListeners.add(listener);
        }
    }
    
    public void removeLanguageChangeListener(LanguageChangeListener listener) {
        languageChangeListeners.remove(listener);
    }
    
    private void fireLanguageChanged(String oldLanguage, String newLanguage) {
        for (LanguageChangeListener listener : languageChangeListeners) {
            try {
                listener.onLanguageChanged(oldLanguage, newLanguage);
            } catch (Exception e) {
                LOGGER.error("Error in language change listener", e);
            }
        }
    }
    
    // ===== Utility Methods =====
    
    public Set<String> getTranslationKeys() {
        Set<String> keys = new HashSet<>();
        Map<String, String> currentTranslations = loadedLanguages.get(currentLanguage);
        if (currentTranslations != null) {
            keys.addAll(currentTranslations.keySet());
        }
        if (!currentLanguage.equals(DEFAULT_LANGUAGE)) {
            Map<String, String> defaultTranslations = loadedLanguages.get(DEFAULT_LANGUAGE);
            if (defaultTranslations != null) {
                keys.addAll(defaultTranslations.keySet());
            }
        }
        return keys;
    }
    
    public Map<String, String> getTranslations(String languageCode) {
        if (languageCode == null) {
            return Collections.emptyMap();
        }
        Map<String, String> translations = loadedLanguages.get(languageCode.toLowerCase());
        return translations != null ? Collections.unmodifiableMap(translations) : Collections.emptyMap();
    }
    
    public void addTranslation(String languageCode, String key, String value) {
        if (languageCode == null || key == null || value == null) {
            return;
        }
        languageCode = languageCode.toLowerCase();
        loadedLanguages.computeIfAbsent(languageCode, k -> new ConcurrentHashMap<>()).put(key, value);
        translationCache.remove(languageCode + ":" + key);
    }
    
    public void removeTranslation(String languageCode, String key) {
        if (languageCode == null || key == null) {
            return;
        }
        languageCode = languageCode.toLowerCase();
        Map<String, String> translations = loadedLanguages.get(languageCode);
        if (translations != null) {
            translations.remove(key);
            translationCache.remove(languageCode + ":" + key);
        }
    }
    
    public void clear() {
        loadedLanguages.clear();
        translationCache.clear();
        availableLanguages.clear();
        availableLanguages.add(DEFAULT_LANGUAGE);
        currentLanguage = DEFAULT_LANGUAGE;
    }
    
    public int getTranslationCount() {
        Map<String, String> translations = loadedLanguages.get(currentLanguage);
        return translations != null ? translations.size() : 0;
    }
    
    public interface LanguageChangeListener {
        void onLanguageChanged(String oldLanguage, String newLanguage);
    }
}
