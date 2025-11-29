package ru.mjkey.storykee.parser.prettyprint;

/**
 * Options for controlling pretty printer output formatting.
 */
public class PrettyPrintOptions {
    
    private int indentSize = 4;
    private boolean useSpaces = true;
    private boolean insertNewlines = true;
    private int maxLineLength = 100;
    
    public PrettyPrintOptions() {
    }
    
    public int getIndentSize() {
        return indentSize;
    }
    
    public PrettyPrintOptions setIndentSize(int indentSize) {
        this.indentSize = indentSize;
        return this;
    }
    
    public boolean isUseSpaces() {
        return useSpaces;
    }
    
    public PrettyPrintOptions setUseSpaces(boolean useSpaces) {
        this.useSpaces = useSpaces;
        return this;
    }
    
    public boolean isInsertNewlines() {
        return insertNewlines;
    }
    
    public PrettyPrintOptions setInsertNewlines(boolean insertNewlines) {
        this.insertNewlines = insertNewlines;
        return this;
    }
    
    public int getMaxLineLength() {
        return maxLineLength;
    }
    
    public PrettyPrintOptions setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
        return this;
    }
    
    public String getIndentString() {
        if (useSpaces) {
            return " ".repeat(indentSize);
        } else {
            return "\t";
        }
    }
    
    public static PrettyPrintOptions defaults() {
        return new PrettyPrintOptions();
    }
}
