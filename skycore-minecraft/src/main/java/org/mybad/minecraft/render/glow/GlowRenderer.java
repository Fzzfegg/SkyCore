package org.mybad.minecraft.render.glow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.mybad.minecraft.render.skinning.SkinningPipeline;

import java.util.Arrays;

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
    private boolean depthSynced;
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
        System.out.println("触发 tint1 !");
        boolean tinted = applyTint(tint, entity);
        if (tinted) {
            GlStateManager.color(frameParams.getTintR(), frameParams.getTintG(), frameParams.getTintB(), alpha);
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
        int prevActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int prevTexture0 = captureTextureBinding(OpenGlHelper.defaultTexUnit);
        int prevTexture1 = captureTextureBinding(OpenGlHelper.lightmapTexUnit);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        Framebuffer result = effect.render(maskFbo, main, frameParams);
        if (result != null) {
            if (main != null) {
                main.bindFramebuffer(true);
                GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
            } else {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
                GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
            }
            GlStateManager.disableDepth();
            boolean blendWasEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            GlStateManager.disableBlend();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, result.framebufferTexture);
            GlowShaderManager.INSTANCE.renderFullscreen(main, GlowShaderManager.INSTANCE.getProgramImage(), program ->
                    GlowShaderManager.INSTANCE.setUniform1i(program, "colourTexture", 0)
            );
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            if (blendWasEnabled) {
                GlStateManager.enableBlend();
            }
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableDepth();
        }
        restoreTextureBinding(OpenGlHelper.defaultTexUnit, prevTexture0);
        restoreTextureBinding(OpenGlHelper.lightmapTexUnit, prevTexture1);
        GL13.glActiveTexture(prevActiveTexture);
        usedThisFrame = false;
        depthSynced = false;
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
        syncDepthFromMain();
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
        maskFbo = new Framebuffer(targetWidth, targetHeight, true);
        maskFbo.setFramebufferFilter(GL11.GL_LINEAR);
        maskWidth = targetWidth;
        maskHeight = targetHeight;
        clearMask();
        depthSynced = false;
    }

    private void clearMask() {
        if (maskFbo == null) {
            return;
        }
        maskFbo.bindFramebuffer(true);
        GL11.glViewport(0, 0, maskWidth, maskHeight);
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
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
        GlStateManager.enableDepth();
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

    private void syncDepthFromMain() {
        if (depthSynced || maskFbo == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        Framebuffer main = mc.getFramebuffer();
        if (main == null) {
            return;
        }
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, maskFbo.framebufferObject);
        GL30.glBlitFramebuffer(
                0, 0, main.framebufferWidth, main.framebufferHeight,
                0, 0, maskWidth, maskHeight,
                GL11.GL_DEPTH_BUFFER_BIT,
                GL11.GL_NEAREST
        );
        depthSynced = true;
        main.bindFramebuffer(true);
        GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
    }

    private int captureTextureBinding(int textureUnit) {
        OpenGlHelper.setActiveTexture(textureUnit);
        return GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    private void restoreTextureBinding(int textureUnit, int textureId) {
        OpenGlHelper.setActiveTexture(textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    private boolean applyTint(int[] tint, Entity entity) {
        System.out.println("Tint: " + Arrays.toString(tint));
        if (tint == null || tint.length < 3) {
            return false;
        }
        float r = clampColorComponent(tint[0]);
        float g = clampColorComponent(tint[1]);
        float b = clampColorComponent(tint[2]);
        if (r == 1.0f && g == 1.0f && b == 1.0f) {
            return false;
        }
        frameParams.setTint(r, g, b);
        if (entity != null) {
            System.out.printf("[GlowRenderer] tint set for %s -> (%.3f, %.3f, %.3f)%n", entity.getName(), r, g, b);
        } else {
            System.out.printf("[GlowRenderer] tint set -> (%.3f, %.3f, %.3f)%n", r, g, b);
        }
        return true;
    }

    private float clampColorComponent(int value) {
        float normalized = value / 255.0f;
        if (normalized < 0f) {
            return 0f;
        }
        if (normalized > 1f) {
            return 1f;
        }
        return normalized;
    }

    private static final float MIN_STRENGTH = 0.1f;
}
