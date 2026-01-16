package org.mybad.minecraft.render.glow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.mybad.minecraft.render.skinning.SkinningPipeline;

/**
 * Accumulates glow masks each frame and composites them using {@link GlowEffect}.
 */
public final class GlowRenderer {
    public static final GlowRenderer INSTANCE = new GlowRenderer();

    private final GlowEffect effect = new GlowEffect();
    private Framebuffer maskFbo;
    private int maskWidth;
    private int maskHeight;
    private int downsample = 2;

    private boolean usedThisFrame;
    private long lastTick = Long.MIN_VALUE;
    private float lastPartial = Float.NaN;
    private final GlowFrameParams frameParams = new GlowFrameParams();

    private GlowRenderer() {}

    public void setDownsample(int downsample) {
        if (downsample <= 0) {
            downsample = 1;
        }
        if (this.downsample != downsample) {
            this.downsample = downsample;
            disposeMask();
        }
    }

    public void renderSkinnedMask(Entity entity,
                                  float partialTicks,
                                  ResourceLocation glowTexture,
                                  float strength,
                                  int[] tint,
                                  SkinningPipeline pipeline,
                                  ResourceLocation baseTexture) {
        if (glowTexture == null || pipeline == null || strength <= 0f) {
            return;
        }
        Entity anchor = entity != null ? entity : Minecraft.getMinecraft().player;
        if (anchor == null) {
            return;
        }
        prepareFrame(anchor, partialTicks);
        if (maskFbo == null) {
            return;
        }
        usedThisFrame = true;
        frameParams.setStrength(Math.max(frameParams.getStrength(), strength));

        int savedLightX = (int) OpenGlHelper.lastBrightnessX;
        int savedLightY = (int) OpenGlHelper.lastBrightnessY;
        beginMaskPass();
        Minecraft.getMinecraft().getTextureManager().bindTexture(glowTexture);
        float alpha = Math.min(1.0f, Math.max(0.0f, strength));
        if (tint != null && tint.length >= 3) {
            float r = tint[0] / 255.0f;
            float g = tint[1] / 255.0f;
            float b = tint[2] / 255.0f;
            GlStateManager.color(r, g, b, alpha);
        } else {
            GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f);
        pipeline.draw();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, savedLightX, savedLightY);
        endMaskPass(baseTexture);
    }

    public void renderCustomMask(Entity entity,
                                 float partialTicks,
                                 float strength,
                                 Runnable drawCall) {
        if (drawCall == null || strength <= 0f) {
            return;
        }
        Entity anchor = entity != null ? entity : Minecraft.getMinecraft().player;
        if (anchor == null) {
            return;
        }
        prepareFrame(anchor, partialTicks);
        if (maskFbo == null) {
            return;
        }
        usedThisFrame = true;
        frameParams.setStrength(Math.max(frameParams.getStrength(), strength));
        beginMaskPass();
        GlStateManager.color(1.0f, 1.0f, 1.0f, Math.min(1.0f, Math.max(0.0f, strength)));
        drawCall.run();
        endMaskPass(null);
    }

    public void endFrame() {
        if (!usedThisFrame || maskFbo == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Framebuffer main = mc != null ? mc.getFramebuffer() : null;
        Framebuffer result = effect.render(maskFbo, main, frameParams);
        if (result != null) {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, result.framebufferTexture);
            GlowShaderManager.INSTANCE.renderFullscreen(main, GlowShaderManager.INSTANCE.getProgramImage(), program ->
                GlowShaderManager.INSTANCE.setUniform1i(program, "colourTexture", 0)
            );
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
        usedThisFrame = false;
        clearMask();
    }

    public void cleanup() {
        disposeMask();
        effect.cleanup();
    }

    private void prepareFrame(Entity entity, float partialTicks) {
        if (entity == null || entity.world == null) {
            return;
        }
        long tick = entity.world.getTotalWorldTime();
        if (tick != lastTick || Math.abs(lastPartial - partialTicks) > 1.0e-4f) {
            resetFrameState();
            lastTick = tick;
            lastPartial = partialTicks;
        }
        ensureMaskBuffer();
    }

    private void resetFrameState() {
        usedThisFrame = false;
        frameParams.setStrength(MIN_STRENGTH);
        frameParams.resetTint();
        clearMask();
    }

    private void ensureMaskBuffer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        int ds = Math.max(1, downsample);
        int targetWidth = Math.max(1, (mc.displayWidth + ds - 1) / ds);
        int targetHeight = Math.max(1, (mc.displayHeight + ds - 1) / ds);
        if (maskFbo != null && maskWidth == targetWidth && maskHeight == targetHeight) {
            return;
        }
        disposeMask();
        maskFbo = new Framebuffer(targetWidth, targetHeight, false);
        maskFbo.setFramebufferFilter(GL11.GL_LINEAR);
        maskWidth = targetWidth;
        maskHeight = targetHeight;
        clearMask();
    }

    private void clearMask() {
        if (maskFbo == null) {
            return;
        }
        maskFbo.bindFramebuffer(true);
        GL11.glViewport(0, 0, maskWidth, maskHeight);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        if (main != null) {
            main.bindFramebuffer(true);
            GL11.glViewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }

    private void disposeMask() {
        if (maskFbo != null) {
            maskFbo.deleteFramebuffer();
            maskFbo = null;
        }
    }

    private void beginMaskPass() {
        if (maskFbo == null) {
            return;
        }
        maskFbo.bindFramebuffer(true);
        GL11.glViewport(0, 0, maskWidth, maskHeight);
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
    }

    private void endMaskPass(ResourceLocation baseTexture) {
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        if (main != null) {
            main.bindFramebuffer(true);
            GL11.glViewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        if (baseTexture != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
        }
    }

    private static final float MIN_STRENGTH = 0.1f;
}
