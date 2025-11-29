package ru.mjkey.storykee.systems.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Image GUI component.
 * Displays a texture/image on the GUI.
 * 
 * Requirements: 9.4
 */
public class ImageComponent extends AbstractGUIComponent {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageComponent.class);
    
    private ResourceLocation texture;
    private int u;
    private int v;
    private int textureWidth;
    private int textureHeight;
    private int regionWidth;
    private int regionHeight;
    
    public ImageComponent(String id, int x, int y, int width, int height, ResourceLocation texture) {
        this(id, x, y, width, height, texture, 0, 0, width, height, 256, 256);
    }
    
    public ImageComponent(String id, int x, int y, int width, int height, 
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
    }
    
    public static ImageComponent fromPath(String id, int x, int y, int width, int height, String texturePath) {
        try {
            ResourceLocation texture = ResourceLocation.parse(texturePath);
            return new ImageComponent(id, x, y, width, height, texture);
        } catch (Exception e) {
            LOGGER.error("Failed to create image component from path: {}", texturePath, e);
            return new ImageComponent(id, x, y, width, height, (ResourceLocation) null);
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!visible || texture == null) {
            return;
        }
        
        try {
            graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } catch (Exception e) {
            LOGGER.error("Error rendering image component '{}': {}", id, e.getMessage());
        }
    }
    
    public ResourceLocation getTexture() {
        return texture;
    }
    
    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }
    
    public void setTexture(String texturePath) {
        try {
            this.texture = ResourceLocation.parse(texturePath);
        } catch (Exception e) {
            LOGGER.error("Failed to set texture from path: {}", texturePath, e);
        }
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
    
    public void setTextureCoordinates(int u, int v) {
        this.u = u;
        this.v = v;
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
}
