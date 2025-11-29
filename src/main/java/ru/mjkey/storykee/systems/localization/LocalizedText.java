package ru.mjkey.storykee.systems.localization;

import java.util.Objects;

/**
 * Represents text that can be either a literal string or a translation key.
 * Provides automatic resolution of translation keys when getting the display text.
 * 
 * Requirements: 26.1, 26.5
 */
public class LocalizedText {
    
    private final String value;
    private final boolean isTranslationKey;
    private final Object[] args;
    
    /**
     * Creates a LocalizedText from a literal string.
     * 
     * @param literal The literal text
     */
    private LocalizedText(String literal, boolean isKey, Object[] args) {
        this.value = literal != null ? literal : "";
        this.isTranslationKey = isKey;
        this.args = args;
    }
    
    /**
     * Creates a LocalizedText from a literal string.
     * 
     * @param literal The literal text
     * @return A LocalizedText instance
     */
    public static LocalizedText literal(String literal) {
        return new LocalizedText(literal, false, null);
    }
    
    /**
     * Creates a LocalizedText from a translation key.
     * 
     * @param key The translation key
     * @return A LocalizedText instance
     */
    public static LocalizedText translatable(String key) {
        return new LocalizedText(key, true, null);
    }
    
    /**
     * Creates a LocalizedText from a translation key with arguments.
     * 
     * @param key The translation key
     * @param args Arguments for substitution
     * @return A LocalizedText instance
     */
    public static LocalizedText translatable(String key, Object... args) {
        return new LocalizedText(key, true, args);
    }
    
    /**
     * Creates a LocalizedText by auto-detecting if the value is a translation key.
     * Values starting with @ are treated as translation keys.
     * 
     * @param value The value (literal or @key)
     * @return A LocalizedText instance
     */
    public static LocalizedText of(String value) {
        if (value == null || value.isEmpty()) {
            return literal(value);
        }
        
        // Check if it's a translation key format
        if (value.startsWith("@{") && value.endsWith("}")) {
            return translatable(value.substring(2, value.length() - 1));
        } else if (value.startsWith("@")) {
            return translatable(value.substring(1));
        }
        
        return literal(value);
    }
    
    /**
     * Gets the resolved text.
     * If this is a translation key, it will be resolved using LocalizationManager.
     * 
     * @return The resolved text
     */
    public String getText() {
        if (!isTranslationKey) {
            // Still process for embedded translation keys
            return LocalizationManager.getInstance().processTranslationKeys(value);
        }
        
        LocalizationManager manager = LocalizationManager.getInstance();
        if (args != null && args.length > 0) {
            return manager.translate(value, args);
        }
        return manager.translate(value);
    }
    
    /**
     * Gets the raw value (key or literal).
     * 
     * @return The raw value
     */
    public String getRawValue() {
        return value;
    }
    
    /**
     * Checks if this is a translation key.
     * 
     * @return true if this is a translation key
     */
    public boolean isTranslationKey() {
        return isTranslationKey;
    }
    
    /**
     * Gets the arguments for substitution.
     * 
     * @return The arguments, or null if none
     */
    public Object[] getArgs() {
        return args;
    }
    
    /**
     * Creates a copy with different arguments.
     * 
     * @param newArgs The new arguments
     * @return A new LocalizedText with the specified arguments
     */
    public LocalizedText withArgs(Object... newArgs) {
        return new LocalizedText(value, isTranslationKey, newArgs);
    }
    
    @Override
    public String toString() {
        return getText();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalizedText that = (LocalizedText) o;
        return isTranslationKey == that.isTranslationKey && 
               Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, isTranslationKey);
    }
}
