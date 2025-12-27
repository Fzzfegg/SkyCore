package org.mybad.core.particle;

import java.util.*;

/**
 * 粒子材质 - 定义粒子的外观和渲染方式
 */
public class ParticleMaterial {

    private String materialId;
    private String materialName;

    // 纹理
    private String textureFile;
    private int textureWidth = 256;
    private int textureHeight = 256;

    // UV映射
    private float uvOffsetX = 0;
    private float uvOffsetY = 0;
    private float uvScaleX = 1.0f;
    private float uvScaleY = 1.0f;

    // 渲染模式
    private BlendMode blendMode = BlendMode.ADD;
    private boolean doubleSided = false;
    private boolean faceCameraX = true;  // Billboard - X轴朝向相机
    private boolean faceCameraY = true;  // Billboard - Y轴朝向相机
    private boolean faceCameraZ = true;  // Billboard - Z轴朝向相机

    // 颜色和透明度
    private float defaultColorR = 1.0f;
    private float defaultColorG = 1.0f;
    private float defaultColorB = 1.0f;
    private float defaultColorA = 1.0f;

    // 动画
    private int spriteColumns = 1;      // 精灵图列数
    private int spriteRows = 1;         // 精灵图行数
    private float animationSpeed = 1.0f; // 动画速度
    private boolean animateByAge = false; // 根据粒子年龄动画

    // 特殊效果
    private boolean enableGlow = false;
    private float glowIntensity = 1.0f;
    private boolean enableDistortion = false;
    private float distortionAmount = 0.0f;

    // 性能配置
    private boolean castShadow = false;
    private boolean receiveShadow = false;
    private float depthBias = 0.0f;

    // 元数据
    private Map<String, String> metadata;
    private long creationTime;

    public ParticleMaterial(String materialId, String materialName) {
        this.materialId = materialId;
        this.materialName = materialName;
        this.metadata = new HashMap<>();
        this.creationTime = System.currentTimeMillis();
    }

    /**
     * 设置元数据
     */
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 获取元数据
     */
    public String getMetadata(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    /**
     * 设置纹理
     */
    public void setTexture(String textureFile, int width, int height) {
        this.textureFile = textureFile;
        this.textureWidth = width;
        this.textureHeight = height;
    }

    /**
     * 设置UV偏移和缩放
     */
    public void setUVTransform(float offsetX, float offsetY, float scaleX, float scaleY) {
        this.uvOffsetX = offsetX;
        this.uvOffsetY = offsetY;
        this.uvScaleX = scaleX;
        this.uvScaleY = scaleY;
    }

    /**
     * 设置默认颜色
     */
    public void setDefaultColor(float r, float g, float b, float a) {
        this.defaultColorR = r;
        this.defaultColorG = g;
        this.defaultColorB = b;
        this.defaultColorA = a;
    }

    /**
     * 设置精灵图动画
     */
    public void setSpriteAnimation(int columns, int rows, float speed, boolean byAge) {
        this.spriteColumns = columns;
        this.spriteRows = rows;
        this.animationSpeed = speed;
        this.animateByAge = byAge;
    }

    /**
     * 获取精灵图帧数
     */
    public int getSpriteFrameCount() {
        return spriteColumns * spriteRows;
    }

    /**
     * 设置Billboard模式
     */
    public void setBillboardMode(boolean faceX, boolean faceY, boolean faceZ) {
        this.faceCameraX = faceX;
        this.faceCameraY = faceY;
        this.faceCameraZ = faceZ;
    }

    /**
     * 设置混合模式
     */
    public void setBlendMode(BlendMode mode) {
        this.blendMode = mode;
    }

    /**
     * 验证材质
     */
    public boolean validate() {
        return materialId != null && !materialId.isEmpty() &&
               textureFile != null && !textureFile.isEmpty() &&
               textureWidth > 0 && textureHeight > 0;
    }

    /**
     * 获取材质信息
     */
    public String getMaterialInfo() {
        return String.format("ParticleMaterial [%s (%s), Texture: %s (%dx%d), Blend: %s]",
                materialId, materialName, textureFile, textureWidth, textureHeight, blendMode);
    }

    // Getters
    public String getMaterialId() { return materialId; }
    public String getMaterialName() { return materialName; }
    public String getTextureFile() { return textureFile; }
    public int getTextureWidth() { return textureWidth; }
    public int getTextureHeight() { return textureHeight; }
    public BlendMode getBlendMode() { return blendMode; }
    public boolean isDoubleSided() { return doubleSided; }
    public boolean isFaceCameraX() { return faceCameraX; }
    public boolean isFaceCameraY() { return faceCameraY; }
    public boolean isFaceCameraZ() { return faceCameraZ; }
    public float getDefaultColorR() { return defaultColorR; }
    public float getDefaultColorG() { return defaultColorG; }
    public float getDefaultColorB() { return defaultColorB; }
    public float getDefaultColorA() { return defaultColorA; }
    public int getSpriteColumns() { return spriteColumns; }
    public int getSpriteRows() { return spriteRows; }
    public float getAnimationSpeed() { return animationSpeed; }
    public boolean isAnimateByAge() { return animateByAge; }
    public boolean isGlowEnabled() { return enableGlow; }
    public float getGlowIntensity() { return glowIntensity; }
    public boolean isDistortionEnabled() { return enableDistortion; }
    public float getDistortionAmount() { return distortionAmount; }
    public long getCreationTime() { return creationTime; }

    @Override
    public String toString() {
        return getMaterialInfo();
    }

    /**
     * 混合模式
     */
    public enum BlendMode {
        OPAQUE,         // 不透明
        ALPHA,          // Alpha混合
        ADD,            // 加法混合（发光效果）
        MULTIPLY,       // 乘法混合
        SCREEN          // 屏幕混合
    }
}
