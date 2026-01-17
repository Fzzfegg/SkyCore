package org.mybad.minecraft.config;

import com.google.gson.annotations.SerializedName;

/**
 * 实体模型映射配置
 * 定义实体名字与模型/动画/纹理的对应关系
 */
public class EntityModelMapping {
    /** 实体自定义名字 */
    private String name;

    /** 模型文件路径 (如: skycore/models/zombie.geo.json) */
    private String model;

    /** 动画文件路径 (如: skycore/animations/zombie.animation.json) */
    private String animation;

    /** 纹理文件路径 (如: skycore/textures/zombie.png) */
    private String texture;

    /** 发光贴图路径 (如: skycore/textures/zombie_emissive.png) */
    @SerializedName(value = "emissive", alternate = {"emissiveTexture"})
    private String emissive;
    /** 发光强度 [0,1]，默认 1.0 */
    private float emissiveStrength = 1.0f;

    /** 泛光遮罩贴图路径 (如: skycore/textures/zombie_bloom.png) */
    @SerializedName(value = "bloom", alternate = {"bloomTexture"})
    private String bloom;
    /** 泛光颜色 (RGBA)，默认白色 */
    @SerializedName(value = "bloomColor", alternate = {"bloom_color"})
    private int[] bloomColor;
    /** 泛光强度 */
    @SerializedName(value = "bloomStrength", alternate = {"bloom_strength"})
    private float bloomStrength = 1.0f;
    /** 叠色遮罩贴图路径 */
    @SerializedName(value = "blendTexture", alternate = {"blend"})
    private String blendTexture;
    /** 叠色模式: alpha | add */
    private String blendMode;
    /** 叠色颜色 RGBA */
    private float[] blendColor;

    /** 是否启用背面剔除 (默认: true) */
    private boolean enableCull = true;
    /** 模型缩放（默认: 1.0） */
    @SerializedName("scale")
    private float modelScale = 1.0f;
    /** 主动画切换淡入淡出时间（秒，默认: 0.12） */
    private float primaryFadeSeconds = 0.12f;
    /** 是否渲染原版受击红色（默认: true） */
    private boolean renderHurtTint = true;
    /** 受击颜色 RGBA（可选，默认使用内置红色） */
    private float[] hurtTint;
    /** 是否渲染原版实体阴影（默认: true） */
    private boolean renderShadow = true;
    /** 自定义渲染包围盒宽度（XZ方向，单位格） */
    private float renderBoxWidth = 0f;
    /** 自定义渲染包围盒高度（单位格） */
    private float renderBoxHeight = 0f;
    /** 自定义渲染包围盒深度（Z方向，单位格） */
    private float renderBoxDepth = 0f;

    public EntityModelMapping() {}

    public EntityModelMapping(String name, String model, String animation, String texture) {
        this.name = name;
        this.model = model;
        this.animation = animation;
        this.texture = texture;
    }

    // Getters
    public String getName() { return name; }
    public String getModel() { return model; }
    public String getAnimation() { return animation; }
    public String getTexture() { return texture; }
    public String getEmissive() { return emissive; }
    public float getEmissiveStrength() { return emissiveStrength; }
    public String getBloom() { return bloom; }
    public int[] getBloomColor() { return bloomColor; }
    public float getBloomStrength() { return bloomStrength; }
    public String getBlendTexture() { return blendTexture; }
    public String getBlendMode() { return blendMode; }
    public float[] getBlendColor() { return blendColor; }
    public boolean isEnableCull() { return enableCull; }
    public float getModelScale() { return modelScale; }
    public float getPrimaryFadeSeconds() { return primaryFadeSeconds; }
    public boolean isRenderHurtTint() { return renderHurtTint; }
    public float[] getHurtTint() { return hurtTint; }
    public boolean isRenderShadow() { return renderShadow; }
    public float getRenderBoxWidth() { return renderBoxWidth; }
    public float getRenderBoxHeight() { return renderBoxHeight; }
    public float getRenderBoxDepth() { return renderBoxDepth; }
    public boolean hasCustomRenderBox() {
        return renderBoxWidth > 0f && renderBoxHeight > 0f && renderBoxDepth > 0f;
    }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setModel(String model) { this.model = model; }
    public void setAnimation(String animation) { this.animation = animation; }
    public void setTexture(String texture) { this.texture = texture; }
    public void setEmissive(String emissive) { this.emissive = emissive; }
    public void setEmissiveStrength(float emissiveStrength) { this.emissiveStrength = emissiveStrength; }
    public void setBloom(String bloom) { this.bloom = bloom; }
    public void setBloomColor(int[] bloomColor) { this.bloomColor = bloomColor; }
    public void setBloomStrength(float bloomStrength) { this.bloomStrength = bloomStrength; }
    public void setBlendTexture(String blendTexture) { this.blendTexture = blendTexture; }
    public void setBlendMode(String blendMode) { this.blendMode = blendMode; }
    public void setBlendColor(float[] blendColor) { this.blendColor = blendColor; }
    public void setEnableCull(boolean enableCull) { this.enableCull = enableCull; }
    public void setModelScale(float modelScale) { this.modelScale = modelScale; }
    public void setPrimaryFadeSeconds(float primaryFadeSeconds) { this.primaryFadeSeconds = primaryFadeSeconds; }
    public void setRenderHurtTint(boolean renderHurtTint) { this.renderHurtTint = renderHurtTint; }
    public void setHurtTint(float[] hurtTint) { this.hurtTint = hurtTint; }
    public void setRenderShadow(boolean renderShadow) { this.renderShadow = renderShadow; }
    public void setRenderBoxWidth(float renderBoxWidth) { this.renderBoxWidth = renderBoxWidth; }
    public void setRenderBoxHeight(float renderBoxHeight) { this.renderBoxHeight = renderBoxHeight; }
    public void setRenderBoxDepth(float renderBoxDepth) { this.renderBoxDepth = renderBoxDepth; }

    @Override
    public String toString() {
        return "EntityModelMapping{" +
                "name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", animation='" + animation + '\'' +
                ", texture='" + texture + '\'' +
                ", emissive='" + emissive + '\'' +
                ", emissiveStrength=" + emissiveStrength +
                ", bloom='" + bloom + '\'' +
                ", bloomColor=" + (bloomColor == null ? "null" : java.util.Arrays.toString(bloomColor)) +
                ", bloomStrength=" + bloomStrength +
                ", blendTexture='" + blendTexture + '\'' +
                ", blendMode='" + blendMode + '\'' +
                ", blendColor=" + (blendColor == null ? "null" : java.util.Arrays.toString(blendColor)) +
                ", enableCull=" + enableCull +
                ", modelScale=" + modelScale +
                ", primaryFadeSeconds=" + primaryFadeSeconds +
                ", renderHurtTint=" + renderHurtTint +
                ", hurtTint=" + (hurtTint == null ? "null" : java.util.Arrays.toString(hurtTint)) +
                ", renderShadow=" + renderShadow +
                ", renderBoxWidth=" + renderBoxWidth +
                ", renderBoxHeight=" + renderBoxHeight +
                ", renderBoxDepth=" + renderBoxDepth +
                '}';
    }
}
