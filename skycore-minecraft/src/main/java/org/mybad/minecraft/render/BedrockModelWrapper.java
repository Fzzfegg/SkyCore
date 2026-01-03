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
class BedrockModelWrapper {

    /** 模型数据 */
    private final Model model;

    /** 动画控制 */
    private final ModelAnimationController animationController;

    /** 纹理位置 */
    private final ResourceLocation texture;
    private final ResourceLocation emissiveTexture;
    private float emissiveStrength = 1.0f;

    /** 纹理尺寸 */
    private final int textureWidth;
    private final int textureHeight;

    private float modelScale = 1.0f;
    private final ModelGeometryBuilder geometryBuilder;
    private final SkinningPipeline skinningPipeline;
    private final ModelRenderPipeline renderPipeline;

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
        this(ModelWrapperFactory.build(model, animation, texture, emissiveTexture, enableCull, modelId, geometryCache));
    }

    BedrockModelWrapper(ModelWrapperFactory.BuildData data) {
        this(data.model, data.animationController, data.texture, data.emissiveTexture, data.enableCull,
            data.textureWidth, data.textureHeight, data.geometryBuilder, data.skinningPipeline);
    }

    BedrockModelWrapper(Model model,
                        ModelAnimationController animationController,
                        ResourceLocation texture,
                        ResourceLocation emissiveTexture,
                        boolean enableCull,
                        int textureWidth,
                        int textureHeight,
                        ModelGeometryBuilder geometryBuilder,
                        SkinningPipeline skinningPipeline) {
        this.enableCull = enableCull;
        this.model = model;
        this.animationController = animationController != null ? animationController : new ModelAnimationController(null);
        this.texture = texture;
        this.emissiveTexture = emissiveTexture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.geometryBuilder = geometryBuilder;
        this.skinningPipeline = skinningPipeline;
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
    }

    static void clearSharedResources() {
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
