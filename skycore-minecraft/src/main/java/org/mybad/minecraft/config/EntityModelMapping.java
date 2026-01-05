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

    /** 是否启用背面剔除 (默认: true) */
    private boolean enableCull = true;
    /** 模型缩放（默认: 1.0） */
    @SerializedName("scale")
    private float modelScale = 1.0f;
    /** 主动画切换淡入淡出时间（秒，默认: 0.12） */
    private float primaryFadeSeconds = 0.12f;

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
    public boolean isEnableCull() { return enableCull; }
    public float getModelScale() { return modelScale; }
    public float getPrimaryFadeSeconds() { return primaryFadeSeconds; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setModel(String model) { this.model = model; }
    public void setAnimation(String animation) { this.animation = animation; }
    public void setTexture(String texture) { this.texture = texture; }
    public void setEmissive(String emissive) { this.emissive = emissive; }
    public void setEmissiveStrength(float emissiveStrength) { this.emissiveStrength = emissiveStrength; }
    public void setBloom(String bloom) { this.bloom = bloom; }
    public void setEnableCull(boolean enableCull) { this.enableCull = enableCull; }
    public void setModelScale(float modelScale) { this.modelScale = modelScale; }
    public void setPrimaryFadeSeconds(float primaryFadeSeconds) { this.primaryFadeSeconds = primaryFadeSeconds; }

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
                ", enableCull=" + enableCull +
                ", modelScale=" + modelScale +
                ", primaryFadeSeconds=" + primaryFadeSeconds +
                '}';
    }
}
