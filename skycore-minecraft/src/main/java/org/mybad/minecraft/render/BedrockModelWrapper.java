package org.mybad.minecraft.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.core.data.Model;
import org.mybad.minecraft.render.geometry.GeometryCache;
import org.mybad.minecraft.render.geometry.ModelGeometryBuilder;
import org.mybad.minecraft.render.skinning.SkinningPipeline;
import org.mybad.minecraft.render.transform.LocatorResolver;
import org.mybad.minecraft.render.transform.LocatorTransform;

import java.util.List;

/**
 * Bedrock 模型包装器
 * 参考 Chameleon 的 ChameleonRenderer 和 HammerAnimations 的 BedrockModelWrapper
 *
 * 特性：
 * - Chameleon 风格的骨骼变换顺序
 * - 预计算顶点（模型加载时生成四边形）
 * - 批量渲染
 */
@SideOnly(Side.CLIENT)
public class BedrockModelWrapper {

    /** 模型数据 */
    private final Model model;

    /** 动画控制 */
    private final ModelAnimationController animationController;

    /** 纹理位置 */
    private final ResourceLocation texture;
    private final ResourceLocation emissiveTexture;
    private final ResourceLocation bloomTexture;
    private final ResourceLocation blendTexture;
    private float emissiveStrength = 1.0f;
    private float bloomStrength = 0.0f;
    private int bloomRadius = 8;
    private int bloomDownsample = 2;
    private float bloomThreshold = 0.0f;
    private int bloomPasses = 1;
    private float bloomSpread = 1.0f;
    private boolean bloomUseDepth = true;
    private boolean renderHurtTint = true;
    private ModelBlendMode blendMode = ModelBlendMode.ALPHA;
    private float blendR = 1.0f;
    private float blendG = 1.0f;
    private float blendB = 1.0f;
    private float blendA = 1.0f;
    private float hurtTintR = 1.0f;
    private float hurtTintG = 0.3f;
    private float hurtTintB = 0.3f;
    private float hurtTintA = 1.0f;

    /** 纹理尺寸 */
    private final int textureWidth;
    private final int textureHeight;

    private float modelScale = 1.0f;
    private final ModelGeometryBuilder geometryBuilder;
    private final SkinningPipeline skinningPipeline;
    private final ModelRenderPipeline renderPipeline;
    private final String modelId;

    /** 是否启用背面剔除 */
    private final boolean enableCull;

    BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture) {
        this(model, animation, texture, null, true, null, null);
    }

    BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull) {
        this(model, animation, texture, null, enableCull, null, null);
    }

    BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull, String modelId) {
        this(model, animation, texture, null, enableCull, modelId, null);
    }

    BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, ResourceLocation emissiveTexture, boolean enableCull, String modelId, GeometryCache geometryCache) {
        this(ModelWrapperFactory.build(model, animation, texture, emissiveTexture, null, null, enableCull, modelId, geometryCache));
    }

    BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, ResourceLocation emissiveTexture, ResourceLocation bloomTexture, boolean enableCull, String modelId, GeometryCache geometryCache) {
        this(ModelWrapperFactory.build(model, animation, texture, emissiveTexture, bloomTexture, null, enableCull, modelId, geometryCache));
    }

    BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, ResourceLocation emissiveTexture, ResourceLocation bloomTexture, ResourceLocation blendTexture, boolean enableCull, String modelId, GeometryCache geometryCache) {
        this(ModelWrapperFactory.build(model, animation, texture, emissiveTexture, bloomTexture, blendTexture, enableCull, modelId, geometryCache));
    }

    BedrockModelWrapper(ModelWrapperFactory.BuildData data) {
        this(data.model, data.animationController, data.texture, data.emissiveTexture, data.bloomTexture, data.blendTexture, data.enableCull,
            data.textureWidth, data.textureHeight, data.geometryBuilder, data.skinningPipeline, data.modelId);
    }

    BedrockModelWrapper(Model model,
                        ModelAnimationController animationController,
                        ResourceLocation texture,
                        ResourceLocation emissiveTexture,
                        ResourceLocation bloomTexture,
                        ResourceLocation blendTexture,
                        boolean enableCull,
                        int textureWidth,
                        int textureHeight,
                        ModelGeometryBuilder geometryBuilder,
                        SkinningPipeline skinningPipeline,
                        String modelId) {
        this.enableCull = enableCull;
        this.model = model;
        this.animationController = animationController != null ? animationController : new ModelAnimationController(null);
        this.texture = texture;
        this.emissiveTexture = emissiveTexture;
        this.bloomTexture = bloomTexture;
        this.blendTexture = blendTexture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.geometryBuilder = geometryBuilder;
        this.skinningPipeline = skinningPipeline;
        this.modelId = modelId;
        this.renderPipeline = new ModelRenderPipeline();
    }

    /**
     * 渲染模型
     */
    void render(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 更新动画并应用到模型
        animationController.updateAndApply(model);
        renderPipeline.render(
            entity,
            x, y, z,
            entityYaw, partialTicks,
            modelScale,
            enableCull,
            texture,
            emissiveTexture,
            emissiveStrength,
            bloomTexture,
            bloomStrength,
            bloomRadius,
            bloomDownsample,
            bloomThreshold,
            bloomPasses,
            bloomSpread,
            bloomUseDepth,
            renderHurtTint,
            hurtTintR,
            hurtTintG,
            hurtTintB,
            hurtTintA,
            blendTexture,
            blendMode,
            blendR,
            blendG,
            blendB,
            blendA,
            skinningPipeline
        );
    }

    /**
     * 设置动画
     */
    void setAnimation(Animation animation) {
        animationController.setAnimation(animation);
    }

    /**
     * 重新开始当前动画
     */
    void restartAnimation() {
        animationController.restartAnimation();
    }

    /**
     * 获取动画播放器
     */
    AnimationPlayer getAnimationPlayer() {
        return animationController.getAnimationPlayer();
    }

    AnimationPlayer getActiveAnimationPlayer() {
        return animationController.getActiveAnimationPlayer();
    }

    void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        animationController.setOverlayStates(states);
    }

    void clearOverlayStates() {
        animationController.clearOverlayStates();
    }

    /**
     * 获取模型
     */
    Model getModel() {
        return model;
    }

    float[] resolveLocatorPosition(String locatorName) {
        return LocatorResolver.resolveLocatorPosition(model, locatorName);
    }

    boolean resolveLocatorTransform(String locatorName, LocatorTransform out) {
        return LocatorResolver.resolveLocatorTransform(model, locatorName, out);
    }

    void setPrimaryFadeDuration(float seconds) {
        animationController.setPrimaryFadeDuration(seconds);
    }

    void setEmissiveStrength(float strength) {
        if (Float.isNaN(strength)) {
            return;
        }
        if (strength < 0f) {
            strength = 0f;
        } else if (strength > 1f) {
            strength = 1f;
        }
        this.emissiveStrength = strength;
    }

    void setBloomStrength(float strength) {
        this.bloomStrength = strength;
    }

    void setBloomRadius(int radius) {
        this.bloomRadius = radius;
    }

    void setBloomDownsample(int downsample) {
        this.bloomDownsample = downsample;
    }

    void setBloomThreshold(float threshold) {
        this.bloomThreshold = threshold;
    }

    void setBloomPasses(int passes) {
        this.bloomPasses = passes;
    }

    void setBloomSpread(float spread) {
        this.bloomSpread = spread;
    }

    void setBloomUseDepth(boolean useDepth) {
        this.bloomUseDepth = useDepth;
    }

    void setRenderHurtTint(boolean renderHurtTint) {
        this.renderHurtTint = renderHurtTint;
    }

    void setBlendMode(ModelBlendMode mode) {
        if (mode != null) {
            this.blendMode = mode;
        }
    }

    void setBlendColor(float[] color) {
        if (color == null || color.length < 4) {
            return;
        }
        blendR = clamp01(color[0]);
        blendG = clamp01(color[1]);
        blendB = clamp01(color[2]);
        blendA = clamp01(color[3]);
    }

    void setHurtTint(float[] hurtTint) {
        if (hurtTint == null || hurtTint.length < 4) {
            return;
        }
        hurtTintR = clamp01(hurtTint[0]);
        hurtTintG = clamp01(hurtTint[1]);
        hurtTintB = clamp01(hurtTint[2]);
        hurtTintA = clamp01(hurtTint[3]);
    }

    private float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    void setModelScale(float scale) {
        if (Float.isNaN(scale) || scale <= 0f) {
            return;
        }
        this.modelScale = scale;
    }

    float getModelScale() {
        return modelScale;
    }

    void dispose() {
        skinningPipeline.dispose();
        animationController.dispose();
        ModelWrapperFactory.releaseModelInstance(modelId, model);
    }

    public static void clearSharedResources() {
        ModelWrapperFactory.clearSharedResources();
    }

    /**
     * 获取纹理
     */
    ResourceLocation getTexture() {
        return texture;
    }

    /**
     * 重新生成四边形
     */
    void regenerateQuads() {
        geometryBuilder.regenerateQuads();
    }

}
