package ru.mjkey.storykee.systems.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HUD element that displays an image/texture on the screen.
 * Supports texture coordinates for sprite sheets and tinting.
 * 
 * Requirements: 9.1, 9.2
 */
public class ImageHUDElement extends HUDElement {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageHUDElement.class);
    
    private ResourceLocation texture;
    private int u;
    private int v;
    private int textureWidth;
    private int textureHeight;
    private int regionWidth;
    private int regionHeight;
    private int tintColor;
    private float alpha;
    
    /**
     * Creates a new image HUD element with a full texture.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Display width
     * @param height Display height
     * @param texture Texture resource location
     */
    public ImageHUDElement(String id, int x, int y, int width, int height, ResourceLocation texture) {
        this(id, x, y, width, height, texture, 0, 0, width, height, width, height);
    }
    
    /**
     * Creates a new image HUD element with texture region support.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Display width
     * @param height Display height
     * @param texture Texture resource location
     * @param u Texture U coordinate (x offset in texture)
     * @param v Texture V coordinate (y offset in texture)
     * @param regionWidth Width of the texture region
     * @param regionHeight Height of the texture region
     * @param textureWidth Total texture width
     * @param textureHeight Total texture height
     */
    public ImageHUDElement(String id, int x, int y, int width, int height, 
                           ResourceLocation texture, int u, int v, 
                           int regionWidth, int regionHeight,
                           int textureWidth, int textureHeight) {
        super(id, x, y, width, height);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.tintColor = 0xFFFFFFFF; // No tint (white)
        this.alpha = 1.0f;
    }
    
    /**
     * Creates an image HUD element from a texture path string.
     * 
     * @param id Unique ResourceLocation
     * @param x X position
     * @param y Y position
     * @param width Display width
     * @param height Display height
     * @param texturePath Texture path (e.g., "itsmystory:textures/hud/icon.png")
     */
    public static ImageHUDElement fromPath(String id, int x, int y, int width, int height, String texturePath) {
        ResourceLocation location = parseTexturePath(texturePath);
        return new ImageHUDElement(id, x, y, width, height, location);
    }
    
    /**
     * Parses a texture path string into a ResourceLocation.
     */
    private static ResourceLocation parseTexturePath(String path) {
        if (path == null || path.isEmpty()) {
            return ResourceLocation.fromNamespaceAndPath("itsmystory", "textures/hud/missing.png");
        }
        
        // Check if path contains namespace
        if (path.contains(":")) {
            String[] parts = path.split(":", 2);
            return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
        }
        
        // Default to itsmystory namespace
        return ResourceLocation.fromNamespaceAndPath("itsmystory", "textures/hud/" + path);
    }
    
    @Override
    public void render(GuiGraphics graphics, float tickDelta) {
        if (!visible || texture == null) {
            return;
        }
        
        try {
            // Render the texture
            // Note: Color tinting would require lower-level rendering APIs
            // For now, we render the texture as-is
            graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } catch (Exception e) {
            LOGGER.warn("Failed to render image HUD element '{}': {}", id, e.getMessage());
        }
    }
    
    // Getters and setters
    
    public ResourceLocation getTexture() {
        return texture;
    }
    
    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }
    
    public void setTexture(String texturePath) {
        this.texture = parseTexturePath(texturePath);
    }
    
    public int getU() {
        return u;
    }
    
    public void setU(int u) {
        this.u = u;
    }
    
    public int getV() {
        return v;
    }
    
    public void setV(int v) {
        this.v = v;
    }
    
    public void setTextureCoords(int u, int v) {
        this.u = u;
        this.v = v;
    }
    
    public int getTextureWidth() {
        return textureWidth;
    }
    
    public void setTextureWidth(int textureWidth) {
        this.textureWidth = textureWidth;
    }
    
    public int getTextureHeight() {
        return textureHeight;
    }
    
    public void setTextureHeight(int textureHeight) {
        this.textureHeight = textureHeight;
    }
    
    public void setTextureSize(int textureWidth, int textureHeight) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }
    
    public int getRegionWidth() {
        return regionWidth;
    }
    
    public void setRegionWidth(int regionWidth) {
        this.regionWidth = regionWidth;
    }
    
    public int getRegionHeight() {
        return regionHeight;
    }
    
    public void setRegionHeight(int regionHeight) {
        this.regionHeight = regionHeight;
    }
    
    public void setRegionSize(int regionWidth, int regionHeight) {
        this.regionWidth = regionWidth;
        this.regionHeight = regionHeight;
    }
    
    public int getTintColor() {
        return tintColor;
    }
    
    public void setTintColor(int tintColor) {
        this.tintColor = tintColor;
    }
    
    public float getAlpha() {
        return alpha;
    }
    
    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }
    
    @Override
    public String toString() {
        return "ImageHUDElement{id='" + id + "', texture=" + texture + ", x=" + x + ", y=" + y + 
               ", width=" + width + ", height=" + height + ", visible=" + visible + "}";
    }
}
