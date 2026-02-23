package org.mybad.minecraft.gltf.client.decoration;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.client.GltfProfile;
import org.mybad.minecraft.gltf.client.CustomPlayerManager;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

/**
 * Lightweight renderer for static decoration anchors (e.g. skulls) that display a GLTF profile.
 */
final class DecorationInstance {
    
    private GltfProfile config;
    private GltfRenderModel renderModel;
    private ResourceLocation baseTexture;
    private String activeClip;
    private float clipPhase;
    private long lastSampleTimeNanos = -1L;
    
    boolean isBoundTo(GltfProfile candidate) {
        return config == candidate && renderModel != null;
    }
    
    void bindConfiguration(GltfProfile config) {
        if (config == null) {
            unbind();
            return;
        }
        if (isBoundTo(config)) {
            return;
        }
        unbind();
        this.config = config;
        this.baseTexture = safeTexture(config.getTexturePath());
        this.activeClip = null;
        this.clipPhase = 0.0f;
        this.lastSampleTimeNanos = -1L;
        loadModel();
    }
    
    void unbind() {
        if (renderModel != null) {
            renderModel.cleanup();
            renderModel = null;
        }
        config = null;
        baseTexture = null;
        activeClip = null;
        clipPhase = 0.0f;
        lastSampleTimeNanos = -1L;
    }
    
    private void loadModel() {
        if (config == null) {
            return;
        }
        try {
            // 使用非缓存加载，避免不同装饰配置互相污染材质
            renderModel = CustomPlayerManager.loadModelFresh(config.getModelPath());
            if (renderModel != null) {
                renderModel.setGlobalScale(config.getModelScale());
                renderModel.setDefaultTexture(baseTexture);
            } else {
                if (CustomPlayerManager.shouldLogMissingModel(config.getModelPath())) {
                    GltfLog.LOGGER.warn("Failed to load decoration model: {}", config.getModelPath());
                }
            }
        } catch (Exception e) {
            if (CustomPlayerManager.shouldLogMissingModel(config.getModelPath())) {
                GltfLog.LOGGER.error("Error loading decoration model {}", config.getModelPath(), e);
            }
        }
    }
    
    boolean render(double worldX, double worldY, double worldZ,
                   double relX, double relY, double relZ,
                   float yawDegrees, float pitchDegrees,
                   float scaleMultiplier, float partialTicks,
                   @Nullable String requestedClip) {
        if (config == null || renderModel == null) {
            return false;
        }
        updateAnimation(requestedClip);
        
        boolean matrixPushed = false;
        boolean shadeAdjusted = false;
        boolean blendStateChanged = false;
        boolean lightingStateChanged = false;
        boolean cullStateChanged = false;
        float prevLightX = OpenGlHelper.lastBrightnessX;
        float prevLightY = OpenGlHelper.lastBrightnessY;
        try {
            if (baseTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            applyLighting(worldX, worldY, worldZ);
            if (GL11.glIsEnabled(GL11.GL_BLEND)) {
                GlStateManager.disableBlend();
                blendStateChanged = true;
            }
            if (!GL11.glIsEnabled(GL11.GL_LIGHTING)) {
                GlStateManager.enableLighting();
                RenderHelper.enableStandardItemLighting();
                lightingStateChanged = true;
            }
            if (GL11.glIsEnabled(GL11.GL_CULL_FACE)) {
                GlStateManager.disableCull();
                cullStateChanged = true;
            }
            
            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            shadeAdjusted = true;
            
            GlStateManager.translate(relX, relY, relZ);
            if (yawDegrees != 0.0f) {
                GlStateManager.rotate(yawDegrees, 0.0f, -1.0f, 0.0f);
            }
            if (pitchDegrees != 0.0f) {
                GlStateManager.rotate(pitchDegrees, 1.0f, 0.0f, 0.0f);
            }
            if (scaleMultiplier != 1.0f) {
                GlStateManager.scale(scaleMultiplier, scaleMultiplier, scaleMultiplier);
            }
            
            renderModel.renderAll();
            return true;
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering decoration instance", e);
            return false;
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightX, prevLightY);
            if (cullStateChanged) {
                GlStateManager.enableCull();
            }
            if (lightingStateChanged) {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
            }
            if (blendStateChanged) {
                GlStateManager.enableBlend();
            }
            if (shadeAdjusted) {
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
            if (matrixPushed) {
                GlStateManager.popMatrix();
            }
        }
    }

    private void applyLighting(double worldX, double worldY, double worldZ) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null) {
            return;
        }
        BlockPos pos = new BlockPos(
            MathHelper.floor(worldX),
            MathHelper.floor(worldY),
            MathHelper.floor(worldZ)
        );
        int combined = mc.world.getCombinedLight(pos, 0);
        int block = combined & 0xFFFF;
        int sky = (combined >> 16) & 0xFFFF;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) block, (float) sky);
    }
    
    private void updateAnimation(@Nullable String requestedClip) {
        if (config == null || renderModel == null) {
            return;
        }
        String desiredClip = resolveClip(requestedClip);
        if (desiredClip == null) {
            return;
        }
        if (!desiredClip.equals(activeClip)) {
            activeClip = desiredClip;
            clipPhase = 0.0f;
        }
        GltfProfile.AnimationClip anim = config.getAnimation(activeClip);
        if (anim == null) {
            return;
        }
        float delta = sampleDeltaSeconds();
        clipPhase = advancePhase(clipPhase, anim, delta);
        float targetSeconds = toAnimationSeconds(anim, clipPhase);
        renderModel.updateAnimation(targetSeconds, true);
    }
    
    @Nullable
    private String resolveClip(@Nullable String requestedClip) {
        if (config == null) {
            return null;
        }
        if (requestedClip != null && config.hasAnimation(requestedClip)) {
            return requestedClip;
        }
        if (config.hasAnimation("idle")) {
            return "idle";
        }
        if (activeClip != null && config.hasAnimation(activeClip)) {
            return activeClip;
        }
        return config.getAnimations().keySet().stream().findFirst().orElse(null);
    }
    
    private float sampleDeltaSeconds() {
        long now = System.nanoTime();
        if (lastSampleTimeNanos < 0L) {
            lastSampleTimeNanos = now;
            return 0.0f;
        }
        long delta = now - lastSampleTimeNanos;
        lastSampleTimeNanos = now;
        return delta / 1_000_000_000.0f;
    }
    
    private float advancePhase(float current, GltfProfile.AnimationClip anim, float deltaTime) {
        if (anim == null || config == null) {
            return 0.0f;
        }
        boolean loop = anim.isLoop();
        boolean hold = anim.shouldHoldLastFrame();
        current += (float) (anim.getAnimationSpeed(config.getFps()) * deltaTime);
        if (loop) {
            return wrapPhase(current);
        } else {
            if (current >= 1.0f) {
                return hold ? 1.0f : 1.0f;
            }
            if (current < 0.0f) {
                return 0.0f;
            }
            return current;
        }
    }
    
    @Nullable
    private ResourceLocation safeTexture(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(path);
        } catch (Exception ex) {
            GltfLog.LOGGER.warn("Invalid texture path {}", path, ex);
            return null;
        }
    }
    
    private float wrapPhase(float value) {
        value = value % 1.0f;
        if (value < 0.0f) {
            value += 1.0f;
        }
        return value;
    }
    
    private float toAnimationSeconds(GltfProfile.AnimationClip anim, float phase) {
        if (anim == null || config == null || config.getFps() <= 0) {
            return 0.0f;
        }
        double startFrame = anim.getStartFrame();
        double endFrame = anim.getEndFrame();
        double frameSpan = endFrame - startFrame;
        double frameValue = startFrame;
        if (frameSpan > 0.0) {
            frameValue += phase * frameSpan;
        }
        double fps = Math.max(config.getFps(), 1);
        return (float) (frameValue / fps);
    }
}
