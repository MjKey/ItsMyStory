package ru.mjkey.storykee.runtime.backup;

import java.nio.file.Path;

/**
 * Represents a corrupted file detected during corruption check.
 */
public class CorruptedFile {
    
    private final Path path;
    private final String reason;
    
    public CorruptedFile(Path path, String reason) {
        this.path = path;
        this.reason = reason;
    }
    
    public Path getPath() {
        return path;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return "CorruptedFile{path=" + path + ", reason=" + reason + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CorruptedFile that = (CorruptedFile) o;
        return path.equals(that.path);
    }
    
    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
