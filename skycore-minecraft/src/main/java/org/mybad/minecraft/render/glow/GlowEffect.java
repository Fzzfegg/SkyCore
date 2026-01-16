package org.mybad.minecraft.render.glow;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

/**
 * Performs the blur + composite steps for glow rendering.
 */
public final class GlowEffect {
    private static final Logger LOGGER = LogManager.getLogger("SkyCore-GlowEffect");
    private static final float MIN_STRENGTH = 0.05f;
    private static final float BLUR_STEP = 1.0f;

    private Framebuffer blurTempA;
    private Framebuffer blurTempB;
    private Framebuffer compositeBuffer;

    private float baseBrightness = 0.08f;
    private float highThreshold = 0.65f;
    private float lowThreshold = 0.05f;

    public Framebuffer render(Framebuffer highlightFbo,
                              Framebuffer sceneFbo,
                              GlowFrameParams params) {
        if (highlightFbo == null || sceneFbo == null) {
            return null;
        }
        if (!GlowShaderManager.INSTANCE.allowed()) {
            return null;
        }
        GlowShaderManager.INSTANCE.init();
        ensureBlurBuffers(highlightFbo.framebufferWidth, highlightFbo.framebufferHeight);
        ensureCompositeBuffer(sceneFbo.framebufferWidth, sceneFbo.framebufferHeight);
        if (blurTempA == null || blurTempB == null || compositeBuffer == null) {
            return null;
        }

        bindTexture(highlightFbo);
        GlowShaderManager.INSTANCE.renderFullscreen(blurTempA, GlowShaderManager.INSTANCE.getProgramBlur(), program -> {
            GlowShaderManager.INSTANCE.setUniform1i(program, "originalTexture", 0);
            GlowShaderManager.INSTANCE.setUniform2f(program, "u_resolution",
                    (float) highlightFbo.framebufferWidth, (float) highlightFbo.framebufferHeight);
            GlowShaderManager.INSTANCE.setUniform2f(program, "blurDir", 0.0f, BLUR_STEP);
        });

        bindTexture(blurTempA);
        GlowShaderManager.INSTANCE.renderFullscreen(blurTempB, GlowShaderManager.INSTANCE.getProgramBlur(), program -> {
            GlowShaderManager.INSTANCE.setUniform1i(program, "originalTexture", 0);
            GlowShaderManager.INSTANCE.setUniform2f(program, "u_resolution",
                    (float) blurTempA.framebufferWidth, (float) blurTempA.framebufferHeight);
            GlowShaderManager.INSTANCE.setUniform2f(program, "blurDir", BLUR_STEP, 0.0f);
        });

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFbo.framebufferTexture);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTempB.framebufferTexture);

        GlowShaderManager.INSTANCE.renderFullscreen(compositeBuffer, GlowShaderManager.INSTANCE.getProgramBloom(), program -> {
            float strength = params != null ? Math.max(MIN_STRENGTH, params.getStrength()) : 1.0f;
            boolean tint = params != null && params.isUseTint();
            float tintR = tint ? params.getTintR() : 1.0f;
            float tintG = tint ? params.getTintG() : 1.0f;
            float tintB = tint ? params.getTintB() : 1.0f;
            GlowShaderManager.INSTANCE.setUniform1i(program, "buffer_a", 0);
            GlowShaderManager.INSTANCE.setUniform1i(program, "buffer_b", 1);
            GlowShaderManager.INSTANCE.setUniform1f(program, "intensive", strength);
            GlowShaderManager.INSTANCE.setUniform1f(program, "base", baseBrightness);
            GlowShaderManager.INSTANCE.setUniform1f(program, "threshold_up", highThreshold);
            GlowShaderManager.INSTANCE.setUniform1f(program, "threshold_down", lowThreshold);
            GlowShaderManager.INSTANCE.setUniform1f(program, "use_tint", tint ? 1.0f : 0.0f);
            GlowShaderManager.INSTANCE.setUniform3f(program, "tint_color", tintR, tintG, tintB);
        });

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return compositeBuffer;
    }

    public void cleanup() {
        dispose(blurTempA);
        dispose(blurTempB);
        dispose(compositeBuffer);
        blurTempA = null;
        blurTempB = null;
        compositeBuffer = null;
    }

    private void bindTexture(Framebuffer framebuffer) {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
    }

    private void ensureBlurBuffers(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (blurTempA != null && blurTempA.framebufferWidth == width && blurTempA.framebufferHeight == height) {
            return;
        }
        dispose(blurTempA);
        dispose(blurTempB);
        blurTempA = createFramebuffer(width, height, false);
        blurTempB = createFramebuffer(width, height, false);
    }

    private void ensureCompositeBuffer(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (compositeBuffer != null && compositeBuffer.framebufferWidth == width && compositeBuffer.framebufferHeight == height) {
            return;
        }
        dispose(compositeBuffer);
        compositeBuffer = createFramebuffer(width, height, false);
    }

    private Framebuffer createFramebuffer(int width, int height, boolean useDepth) {
        Framebuffer framebuffer = new Framebuffer(width, height, useDepth);
        framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
        return framebuffer;
    }

    private void dispose(Framebuffer framebuffer) {
        if (framebuffer != null) {
            framebuffer.deleteFramebuffer();
        }
    }

    public float getBaseBrightness() {
        return baseBrightness;
    }

    public void setBaseBrightness(float baseBrightness) {
        this.baseBrightness = baseBrightness;
    }

    public float getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(float highThreshold) {
        this.highThreshold = highThreshold;
    }

    public float getLowThreshold() {
        return lowThreshold;
    }

    public void setLowThreshold(float lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

}
