package org.mybad.minecraft.render.glow;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

/**
 * Performs the blur + composite steps for glow rendering.
 */
public final class GlowEffect {
    // 最低发光强度。即便模型给的 bloomStrength 很低，合成时也会至少用 0.25，防止光晕太弱看不见。
    private static final float MIN_STRENGTH = 0.3f;
    // 主高光（核心）模糊时的采样步长，单位是像素。值越大，核心光区越快扩散。
    private static final float CORE_BLUR_STEP = 3.0f;
    // 在第一次纵/横模糊之后，再追加的核心模糊迭代次数；越多，中心亮区越柔。
    private static final int EXTRA_CORE_BLUR_LOOPS = 1;
    // 给 halo 缓冲做超大半径模糊时的采样步长。越大，外圈“漫出去”得越宽。
    private static final float HALO_BLUR_STEP = 9f;
    // halo 模糊的迭代次数；越多，外圈越平滑、半径越大。
    private static final int HALO_BLUR_LOOPS = 2;
    // 合成阶段给 halo 贡献的亮度倍数，和 bloomStrength 相乘。调大就让外圈更亮。
    private static final float HALO_WEIGHT_MULTIPLIER = 3.2f;
    // 合成 shader 中 sampling halo 时使用的 UV 偏移尺度，决定我们向外取样多远来生成“超出原图”的光晕。
    private static final float HALO_OFFSET_SCALE = 8.5f;
    //  是合成阶段的基础增益，控制“全局发光底座”：
    private float baseBrightness = 0.48f;
    private float highThreshold = 0.0f;
    private float lowThreshold = 0.0f;

    
    private Framebuffer blurTempA;
    private Framebuffer blurTempB;
    private Framebuffer compositeBuffer;
    private Framebuffer haloBuffer;
    
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
        if (blurTempA == null || blurTempB == null || haloBuffer == null || compositeBuffer == null) {
            return null;
        }

        blurOnce(highlightFbo, blurTempA, 0.0f, CORE_BLUR_STEP);
        blurOnce(blurTempA, blurTempB, CORE_BLUR_STEP, 0.0f);
        for (int i = 0; i < EXTRA_CORE_BLUR_LOOPS; i++) {
            blurOnce(blurTempB, blurTempA, 0.0f, CORE_BLUR_STEP);
            blurOnce(blurTempA, blurTempB, CORE_BLUR_STEP, 0.0f);
        }
        prepareHaloBlur();

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneFbo.framebufferTexture);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, blurTempB.framebufferTexture);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, haloBuffer.framebufferTexture);

        GlowShaderManager.INSTANCE.renderFullscreen(compositeBuffer, GlowShaderManager.INSTANCE.getProgramBloom(), program -> {
            float strength = params != null ? Math.max(MIN_STRENGTH, params.getStrength()) : 1.0f;
            boolean tint = params != null && params.isUseTint();
            float tintR = tint ? params.getTintR() : 1.0f;
            float tintG = tint ? params.getTintG() : 1.0f;
            float tintB = tint ? params.getTintB() : 1.0f;
            GlowShaderManager.INSTANCE.setUniform1i(program, "buffer_a", 0);
            GlowShaderManager.INSTANCE.setUniform1i(program, "buffer_b", 1);
            GlowShaderManager.INSTANCE.setUniform1i(program, "buffer_c", 2);
            GlowShaderManager.INSTANCE.setUniform1f(program, "intensive", strength);
            GlowShaderManager.INSTANCE.setUniform1f(program, "base", baseBrightness);
            GlowShaderManager.INSTANCE.setUniform1f(program, "threshold_up", highThreshold);
            GlowShaderManager.INSTANCE.setUniform1f(program, "threshold_down", lowThreshold);
            GlowShaderManager.INSTANCE.setUniform1f(program, "use_tint", tint ? 1.0f : 0.0f);
            GlowShaderManager.INSTANCE.setUniform3f(program, "tint_color", tintR, tintG, tintB);
            float haloWeight = strength * HALO_WEIGHT_MULTIPLIER;
            GlowShaderManager.INSTANCE.setUniform1f(program, "halo_weight", haloWeight);
            float offX = HALO_OFFSET_SCALE / Math.max(1.0f, haloBuffer.framebufferWidth);
            float offY = HALO_OFFSET_SCALE / Math.max(1.0f, haloBuffer.framebufferHeight);
            GlowShaderManager.INSTANCE.setUniform2f(program, "halo_offset", offX, offY);
        });

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + 2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
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
        dispose(haloBuffer);
        blurTempA = null;
        blurTempB = null;
        compositeBuffer = null;
        haloBuffer = null;
    }

    private void bindTexture(Framebuffer framebuffer) {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, framebuffer.framebufferTexture);
    }

    private void ensureBlurBuffers(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        boolean match = blurTempA != null
                && blurTempA.framebufferWidth == width
                && blurTempA.framebufferHeight == height
                && haloBuffer != null
                && haloBuffer.framebufferWidth == width
                && haloBuffer.framebufferHeight == height;
        if (match) {
            return;
        }
        dispose(blurTempA);
        dispose(blurTempB);
        dispose(haloBuffer);
        blurTempA = createFramebuffer(width, height, false);
        blurTempB = createFramebuffer(width, height, false);
        haloBuffer = createFramebuffer(width, height, false);
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

    private void blurOnce(Framebuffer source, Framebuffer target, float dirX, float dirY) {
        if (source == null || target == null) {
            return;
        }
        bindTexture(source);
        GlowShaderManager.INSTANCE.renderFullscreen(target, GlowShaderManager.INSTANCE.getProgramBlur(), program -> {
            GlowShaderManager.INSTANCE.setUniform1i(program, "originalTexture", 0);
            GlowShaderManager.INSTANCE.setUniform2f(program, "u_resolution",
                    (float) source.framebufferWidth, (float) source.framebufferHeight);
            GlowShaderManager.INSTANCE.setUniform2f(program, "blurDir", dirX, dirY);
        });
    }

    private void prepareHaloBlur() {
        if (haloBuffer == null || blurTempA == null || blurTempB == null) {
            return;
        }
        copyFramebuffer(blurTempB, haloBuffer);
        Framebuffer ping = haloBuffer;
        Framebuffer pong = blurTempA;
        for (int i = 0; i < HALO_BLUR_LOOPS; i++) {
            blurOnce(ping, pong, 0.0f, HALO_BLUR_STEP);
            blurOnce(pong, ping, HALO_BLUR_STEP, 0.0f);
        }
    }

    private void copyFramebuffer(Framebuffer source, Framebuffer target) {
        if (source == null || target == null) {
            return;
        }
        bindTexture(source);
        GlowShaderManager.INSTANCE.renderFullscreen(target, GlowShaderManager.INSTANCE.getProgramImage(), program -> {
            GlowShaderManager.INSTANCE.setUniform1i(program, "colourTexture", 0);
        });
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
