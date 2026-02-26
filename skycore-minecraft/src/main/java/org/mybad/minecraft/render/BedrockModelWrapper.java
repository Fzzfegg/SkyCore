package org.mybad.minecraft.render;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.core.data.Model;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.geometry.GeometryCache;
import org.mybad.minecraft.render.geometry.ModelGeometryBuilder;
import org.mybad.minecraft.render.skinning.SkinningPipeline;
import org.mybad.minecraft.render.transform.LocatorResolver;
import org.mybad.minecraft.render.transform.LocatorTransform;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

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
    private int[] bloomColor;
    private int bloomPasses = 5;
    private float bloomScaleStep = 0.06f;
    private float bloomDownscale = 1.0f;
    private float[] bloomOffset;
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
    private float modelOffsetX = 0f;
    private float modelOffsetY = 0f;
    private float modelOffsetZ = 0f;
    private int modelOffsetMode = EntityModelMapping.OFFSET_MODE_WORLD;
    private boolean billboardMode = false;
    private float billboardPitch = 0f;
    private boolean lightning = false;

    /** 纹理尺寸 */
    private final int textureWidth;
    private final int textureHeight;

    private float modelScale = 1.0f;
    private final ModelGeometryBuilder geometryBuilder;
    private final SkinningPipeline skinningPipeline;
    private final ModelRenderPipeline renderPipeline;
    private boolean animationsDirty = true;
    private final String modelId;

    /** 是否启用背面剔除 */
    private final boolean enableCull;
    private int packedLightOverride = -1;

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

    String getModelId() {
        return modelId;
    }

    /**
     * 渲染模型
     */
    void render(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        renderInternal(entity, x, y, z, entityYaw, partialTicks, entity != null);
    }

    void renderBlock(double x, double y, double z, float yaw, float partialTicks) {
        renderInternal(null, x, y, z, yaw, partialTicks, true);
    }

    void renderBillboard(double x, double y, double z, float yaw, float pitch, float partialTicks) {
        billboardMode = true;
        billboardPitch = pitch;
        try {
            renderInternal(null, x, y, z, yaw, partialTicks, true);
        } finally {
            billboardMode = false;
            billboardPitch = 0f;
        }
    }

    private void renderInternal(@Nullable Entity entity,
                                double x, double y, double z,
                                float entityYaw, float partialTicks,
                                boolean applyYaw) {
        applyAnimationsIfNeeded();
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
            bloomColor,
            bloomPasses,
            bloomScaleStep,
            bloomDownscale,
            bloomOffset,
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
            modelOffsetX,
            modelOffsetY,
            modelOffsetZ,
            modelOffsetMode,
            skinningPipeline,
            applyYaw,
            billboardMode,
            billboardPitch,
            lightning,
            packedLightOverride
        );
        packedLightOverride = -1;
    }

    /**
     * 设置动画
     */
    void setAnimation(Animation animation) {
        animationController.setAnimation(animation);
        markAnimationsDirty();
    }

    /**
     * 重新开始当前动画
     */
    void restartAnimation() {
        animationController.restartAnimation();
        markAnimationsDirty();
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
        markAnimationsDirty();
    }

    void clearOverlayStates() {
        animationController.clearOverlayStates();
        markAnimationsDirty();
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

    void setBloomColor(int[] color) {
        this.bloomColor = color;
    }

    void setBloomPasses(int passes) {
        if (passes <= 0) {
            this.bloomPasses = 0;
        } else {
            this.bloomPasses = passes;
        }
    }

    void setBloomScaleStep(float step) {
        if (Float.isNaN(step) || step <= 0f) {
            return;
        }
        this.bloomScaleStep = step;
    }

    void setBloomDownscale(float downscale) {
        if (Float.isNaN(downscale) || downscale <= 0f) {
            return;
        }
        this.bloomDownscale = downscale;
    }

    void setBloomOffset(float[] offset) {
        if (offset == null || offset.length < 3) {
            this.bloomOffset = null;
            return;
        }
        float x = sanitizeOffsetComponent(offset[0]);
        float y = sanitizeOffsetComponent(offset[1]);
        float z = sanitizeOffsetComponent(offset[2]);
        if (x == 0f && y == 0f && z == 0f) {
            this.bloomOffset = null;
        } else {
            this.bloomOffset = new float[]{x, y, z};
        }
    }

    private float sanitizeOffsetComponent(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        return value;
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

    void setLightning(boolean lightning) {
        this.lightning = lightning;
    }

    void setPackedLightOverride(int packedLight) {
        if (packedLight < 0) {
            this.packedLightOverride = -1;
        } else {
            this.packedLightOverride = packedLight;
        }
    }

    void setPackedLightFromWorld(double worldX, double worldY, double worldZ) {
        int packed = sampleWorldLight(worldX, worldY, worldZ);
        setPackedLightOverride(packed);
    }

    private static int sampleWorldLight(double worldX, double worldY, double worldZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null) {
            return -1;
        }
        BlockPos pos = new BlockPos(worldX, worldY, worldZ);
        return mc.world.getCombinedLight(pos, 0);
    }

    void setModelScale(float scale) {
        if (Float.isNaN(scale) || scale <= 0f) {
            return;
        }
        this.modelScale = scale;
        markAnimationsDirty();
    }

    void setModelOffset(float x, float y, float z, int mode) {
        this.modelOffsetX = sanitizeOffsetComponent(x);
        this.modelOffsetY = sanitizeOffsetComponent(y);
        this.modelOffsetZ = sanitizeOffsetComponent(z);
        this.modelOffsetMode = mode == EntityModelMapping.OFFSET_MODE_LOCAL
            ? EntityModelMapping.OFFSET_MODE_LOCAL
            : EntityModelMapping.OFFSET_MODE_WORLD;
    }

    float getModelScale() {
        return modelScale;
    }

    void dispose() {
        skinningPipeline.dispose();
        animationController.dispose();
        ModelWrapperFactory.releaseModelInstance(modelId, model);
    }

    void updateAnimations() {
        if (animationController.update()) {
            animationsDirty = true;
        }
    }

    private void applyAnimationsIfNeeded() {
        if (animationsDirty) {
            animationController.apply(model);
            animationsDirty = false;
        }
    }

    private void markAnimationsDirty() {
        animationsDirty = true;
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
