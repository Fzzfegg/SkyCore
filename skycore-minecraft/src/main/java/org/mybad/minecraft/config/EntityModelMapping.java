package org.mybad.minecraft.config;

import com.google.gson.annotations.SerializedName;

/**
 * 实体模型映射配置
 * 定义实体名字与模型/动画/纹理的对应关系
 */
public class EntityModelMapping {
    public static final int OFFSET_MODE_WORLD = 0;
    public static final int OFFSET_MODE_LOCAL = 1;
    /** 实体自定义名字 */
    private String name;

    /** 模型文件路径 (如: skycore/models/zombie.geo.json) */
    private String model;
    /** 远程 GLTF profile 标识（引用客户端缓存的 profile） */
    @SerializedName("gltfProfile")
    private String gltfProfileId;

    /** 动画文件路径 (如: skycore/animations/zombie.animation.json) */
    private String animation;

    /** 纹理文件路径 (如: skycore/textures/zombie.png) */
    private String texture;

    /** 发光贴图路径 (如: skycore/textures/zombie_emissive.png) */
    @SerializedName(value = "emissive")
    private String emissive;
    /** 发光强度 [0,1]，默认 1.0 */
    private float emissiveStrength = 1.0f;

    /** 泛光遮罩贴图路径 (如: skycore/textures/zombie_bloom.png) */
    @SerializedName(value = "bloom")
    private String bloom;
    /** 泛光颜色 (RGBA)，默认白色 */
    @SerializedName(value = "bloomColor")
    private int[] bloomColor;
    /** 泛光强度 */
    @SerializedName(value = "bloomStrength")
    private float bloomStrength = 0.0f;
    /** 泛光叠加圈数（伪 bloom pass 数） */
    @SerializedName(value = "bloomPasses")
    private int bloomPasses = 5;
    /** 每一圈的放大增量 */
    @SerializedName(value = "bloomScaleStep")
    private float bloomScaleStep = 0.06f;
    /** 伪 bloom 伪 downscale（越大越接近原分辨率） */
    @SerializedName(value = "bloomDownscale")
    private float bloomDownscale = 1.0f;
    /** 伪 bloom 静态偏移 (XYZ，单位: 格) */
    @SerializedName(value = "bloomOffset")
    private float[] bloomOffset;
    /** 叠色遮罩贴图路径 */
    @SerializedName(value = "blendTexture")
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
    /** 是否开启环境光渲染（默认: false） */
    @SerializedName(value = "lightning", alternate = {"lighting"})
    private boolean lightning = false;
    /** 自定义渲染包围盒宽度（XZ方向，单位格） */
    private float renderBoxWidth = 0f;
    /** 自定义渲染包围盒高度（单位格） */
    private float renderBoxHeight = 0f;
    /** 自定义渲染包围盒深度（Z方向，单位格） */
    private float renderBoxDepth = 0f;
    /** 模型渲染偏移（单位格） */
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float offsetZ = 0f;
    /** 偏移模式：0=world，1=local */
    private int offsetMode = OFFSET_MODE_WORLD;

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
    public String getGltfProfileId() { return gltfProfileId; }
    public String getAnimation() { return animation; }
    public String getTexture() { return texture; }
    public String getEmissive() { return emissive; }
    public float getEmissiveStrength() { return emissiveStrength; }
    public String getBloom() { return bloom; }
    public int[] getBloomColor() { return bloomColor; }
    public float getBloomStrength() { return bloomStrength; }
    public int getBloomPasses() { return bloomPasses; }
    public float getBloomScaleStep() { return bloomScaleStep; }
    public float getBloomDownscale() { return bloomDownscale; }
    public float[] getBloomOffset() { return bloomOffset; }
    public String getBlendTexture() { return blendTexture; }
    public String getBlendMode() { return blendMode; }
    public float[] getBlendColor() { return blendColor; }
    public boolean isEnableCull() { return enableCull; }
    public float getModelScale() { return modelScale; }
    public float getPrimaryFadeSeconds() { return primaryFadeSeconds; }
    public boolean isRenderHurtTint() { return renderHurtTint; }
    public float[] getHurtTint() { return hurtTint; }
    public boolean isRenderShadow() { return renderShadow; }
    public boolean isLightning() { return lightning; }
    public float getRenderBoxWidth() { return renderBoxWidth; }
    public float getRenderBoxHeight() { return renderBoxHeight; }
    public float getRenderBoxDepth() { return renderBoxDepth; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public int getOffsetMode() { return offsetMode; }
    public boolean hasModelOffset() {
        return Math.abs(offsetX) > 1.0E-4f || Math.abs(offsetY) > 1.0E-4f || Math.abs(offsetZ) > 1.0E-4f;
    }
    public boolean hasCustomRenderBox() {
        return renderBoxWidth > 0f && renderBoxHeight > 0f && renderBoxDepth > 0f;
    }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setModel(String model) { this.model = model; }
    public void setGltfProfileId(String gltfProfileId) { this.gltfProfileId = gltfProfileId; }
    public void setAnimation(String animation) { this.animation = animation; }
    public void setTexture(String texture) { this.texture = texture; }
    public void setEmissive(String emissive) { this.emissive = emissive; }
    public void setEmissiveStrength(float emissiveStrength) { this.emissiveStrength = emissiveStrength; }
    public void setBloom(String bloom) { this.bloom = bloom; }
    public void setBloomColor(int[] bloomColor) { this.bloomColor = bloomColor; }
    public void setBloomStrength(float bloomStrength) { this.bloomStrength = bloomStrength; }
    public void setBloomPasses(int bloomPasses) { this.bloomPasses = bloomPasses; }
    public void setBloomScaleStep(float bloomScaleStep) { this.bloomScaleStep = bloomScaleStep; }
    public void setBloomDownscale(float bloomDownscale) { this.bloomDownscale = bloomDownscale; }
    public void setBloomOffset(float[] bloomOffset) {
        if (bloomOffset == null || bloomOffset.length < 3) {
            this.bloomOffset = null;
            return;
        }
        this.bloomOffset = new float[]{bloomOffset[0], bloomOffset[1], bloomOffset[2]};
    }
    public void setBlendTexture(String blendTexture) { this.blendTexture = blendTexture; }
    public void setBlendMode(String blendMode) { this.blendMode = blendMode; }
    public void setBlendColor(float[] blendColor) { this.blendColor = blendColor; }
    public void setEnableCull(boolean enableCull) { this.enableCull = enableCull; }
    public void setModelScale(float modelScale) { this.modelScale = modelScale; }
    public void setPrimaryFadeSeconds(float primaryFadeSeconds) { this.primaryFadeSeconds = primaryFadeSeconds; }
    public void setRenderHurtTint(boolean renderHurtTint) { this.renderHurtTint = renderHurtTint; }
    public void setHurtTint(float[] hurtTint) { this.hurtTint = hurtTint; }
    public void setRenderShadow(boolean renderShadow) { this.renderShadow = renderShadow; }
    public void setLightning(boolean lightning) { this.lightning = lightning; }
    public void setRenderBoxWidth(float renderBoxWidth) { this.renderBoxWidth = renderBoxWidth; }
    public void setRenderBoxHeight(float renderBoxHeight) { this.renderBoxHeight = renderBoxHeight; }
    public void setRenderBoxDepth(float renderBoxDepth) { this.renderBoxDepth = renderBoxDepth; }
    public void setOffsetX(float offsetX) { this.offsetX = sanitizeFinite(offsetX); }
    public void setOffsetY(float offsetY) { this.offsetY = sanitizeFinite(offsetY); }
    public void setOffsetZ(float offsetZ) { this.offsetZ = sanitizeFinite(offsetZ); }
    public void setOffsetMode(int offsetMode) {
        this.offsetMode = offsetMode == OFFSET_MODE_LOCAL ? OFFSET_MODE_LOCAL : OFFSET_MODE_WORLD;
    }

    private float sanitizeFinite(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        return value;
    }


}
