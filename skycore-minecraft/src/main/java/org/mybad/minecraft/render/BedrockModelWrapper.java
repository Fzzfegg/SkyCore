package org.mybad.minecraft.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.core.data.Model;

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
    private final AnimationBridge animationBridge;

    /** 纹理位置 */
    private final ResourceLocation texture;
    private final ResourceLocation emissiveTexture;
    private float emissiveStrength = 1.0f;

    /** 纹理尺寸 */
    private final int textureWidth;
    private final int textureHeight;

    private float modelScale = 1.0f;
    private static final GeometryCache FALLBACK_GEOMETRY_CACHE = new GeometryCache();
    private final ModelGeometryBuilder geometryBuilder;
    private final SkinningPipeline skinningPipeline;

    /** 是否启用背面剔除 */
    private final boolean enableCull;

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture) {
        this(model, animation, texture, null, true, null, null);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull) {
        this(model, animation, texture, null, enableCull, null, null);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, boolean enableCull, String modelId) {
        this(model, animation, texture, null, enableCull, modelId, null);
    }

    public BedrockModelWrapper(Model model, Animation animation, ResourceLocation texture, ResourceLocation emissiveTexture, boolean enableCull, String modelId, GeometryCache geometryCache) {
        this.enableCull = enableCull;
        Model baseModel = model;
        this.model = baseModel != null ? baseModel.createInstance() : null;
        this.texture = texture;
        this.emissiveTexture = emissiveTexture;

        // 获取纹理尺寸
        int texWidth = 64;
        int texHeight = 64;
        try {
            String tw = baseModel.getTextureWidth();
            String th = baseModel.getTextureHeight();
            if (tw != null && !tw.isEmpty()) {
                texWidth = Integer.parseInt(tw);
            }
            if (th != null && !th.isEmpty()) {
                texHeight = Integer.parseInt(th);
            }
        } catch (NumberFormatException ignored) {}

        this.textureWidth = texWidth;
        this.textureHeight = texHeight;
        GeometryCache resolvedCache = geometryCache != null ? geometryCache : FALLBACK_GEOMETRY_CACHE;
        GeometryCache.Key resolvedKey = GeometryCache.key(normalizeModelId(modelId, model), textureWidth, textureHeight);

        this.animationBridge = new AnimationBridge(animation);

        this.geometryBuilder = new ModelGeometryBuilder(this.model, textureWidth, textureHeight);
        // 预生成四边形
        this.geometryBuilder.generateAllQuads();

        this.skinningPipeline = new SkinningPipeline(this.model, geometryBuilder, resolvedCache, resolvedKey);
    }

    /**
     * 渲染模型
     */
    public void render(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        // 更新动画并应用到模型
        animationBridge.updateAndApply(model);

        // 绑定纹理
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        // 设置 OpenGL 状态 - 根据配置控制背面剔除
        if (enableCull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableColorMaterial();

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        if (modelScale != 1.0f) {
            GlStateManager.scale(modelScale, modelScale, modelScale);
        }


        if (entity != null) {
            GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        }

        // 获取实际光照值
        int lightX = (int) OpenGlHelper.lastBrightnessX;
        int lightY = (int) OpenGlHelper.lastBrightnessY;
        // 设置 lightmap 纹理坐标
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);

        boolean gpu = skinningPipeline.ensureGpuSkinningReady();
        if (!gpu) {
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            GlStateManager.disableRescaleNormal();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            return;
        }

        skinningPipeline.updateBoneMatrices();
        skinningPipeline.runSkinningPass();
        skinningPipeline.draw();

        if (emissiveTexture != null) {
            renderEmissivePass(lightX, lightY);
        }

        GlStateManager.popMatrix();

        // 恢复 OpenGL 状态
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
    }

    /**
     * 设置动画
     */
    public void setAnimation(Animation animation) {
        animationBridge.setAnimation(animation);
    }

    /**
     * 重新开始当前动画
     */
    public void restartAnimation() {
        animationBridge.restartAnimation();
    }

    /**
     * 获取动画播放器
     */
    public AnimationPlayer getAnimationPlayer() {
        return animationBridge.getAnimationPlayer();
    }

    public AnimationPlayer getActiveAnimationPlayer() {
        return animationBridge.getActiveAnimationPlayer();
    }

    public void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        animationBridge.setOverlayStates(states);
    }

    public void clearOverlayStates() {
        animationBridge.clearOverlayStates();
    }

    /**
     * 获取模型
     */
    public Model getModel() {
        return model;
    }

    public float[] getLocatorPosition(String locatorName) {
        return LocatorResolver.getLocatorPosition(model, locatorName);
    }

    public boolean getLocatorTransform(String locatorName, LocatorTransform out) {
        return LocatorResolver.getLocatorTransform(model, locatorName, out);
    }

    private void renderEmissivePass(int lightX, int lightY) {
        if (emissiveStrength <= 0f) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(emissiveTexture);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, emissiveStrength);
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);

        int fullBright = 240;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) fullBright, (float) fullBright);

        skinningPipeline.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    public void setPrimaryFadeDuration(float seconds) {
        animationBridge.setPrimaryFadeDuration(seconds);
    }

    public void setEmissiveStrength(float strength) {
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

    public void setModelScale(float scale) {
        if (Float.isNaN(scale) || scale <= 0f) {
            return;
        }
        this.modelScale = scale;
    }

    public float getModelScale() {
        return modelScale;
    }

    public void dispose() {
        skinningPipeline.dispose();
        animationBridge.dispose();
    }

    public static void clearSharedResources() {
        FALLBACK_GEOMETRY_CACHE.clear();
    }

    /**
     * 获取纹理
     */
    public ResourceLocation getTexture() {
        return texture;
    }

    /**
     * 重新生成四边形
     */
    public void regenerateQuads() {
        geometryBuilder.regenerateQuads();
    }

    private static String normalizeModelId(String modelId, Model model) {
        if (modelId != null && !modelId.isEmpty()) {
            return modelId;
        }
        String name = model != null ? model.getName() : null;
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "unknown";
    }

}
