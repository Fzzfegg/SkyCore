package org.mybad.minecraft.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.mybad.minecraft.render.skinning.SkinningPipeline;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Minimal bloom pipeline for model-only glow mask.
 * Passes: mask -> blurX -> blurY -> additive composite.
 */
public final class BloomRenderer {
    private static final BloomRenderer INSTANCE = new BloomRenderer();

    public static BloomRenderer get() {
        return INSTANCE;
    }

    private Framebuffer maskFbo;
    private Framebuffer blurPing;
    private Framebuffer blurPong;
    private int width;
    private int height;

    private int quadVao;
    private int quadVbo;

    private int blitProgram;
    private int blurProgram;
    private int uBlurTex;
    private int uBlurDir;
    private int uBlurSize;
    private int uBlurRadius;
    private int uBlurThreshold;
    private int uBlitTex;
    private int uBlitSceneDepth;
    private int uBlitMaskDepth;
    private int uBlitUseDepth;
    private int uBlitDepthBias;

    private boolean usedThisFrame;
    private final GlStateSnapshot state = new GlStateSnapshot();
    private long lastTick = Long.MIN_VALUE;
    private float lastPartial = Float.NaN;
    private int savedLightX;
    private int savedLightY;
    private boolean frameParamsLocked;
    private int frameRadius = 8;
    private float frameThreshold = 0.0f;
    private int downsample = 1;
    private int depthTexScene;
    private int depthTexMask;
    private int depthFboScene;
    private int depthFboMask;
    private int depthWidth;
    private int depthHeight;
    private boolean depthValid;

    private BloomRenderer() {
    }

    public void beginFrame(Entity entity, float partialTicks) {
        if (entity == null || entity.world == null) {
            return;
        }
        long tick = entity.world.getTotalWorldTime();
        if (tick == lastTick && Math.abs(partialTicks - lastPartial) < 1.0e-6f) {
            return;
        }
        lastTick = tick;
        lastPartial = partialTicks;

        ensureBuffers();
        if (maskFbo != null) {
            maskFbo.bindFramebuffer(true);
            GlStateManager.clearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            maskFbo.unbindFramebuffer();
        }
        usedThisFrame = false;
        frameParamsLocked = false;
        frameRadius = 0;
        frameThreshold = 1.0f;
    }

    public void renderBloomMask(Entity entity,
                                float partialTicks,
                                ResourceLocation bloomTexture,
                                float bloomStrength,
                                int bloomRadius,
                                int bloomDownsample,
                                float bloomThreshold,
                                int lightX,
                                int lightY,
                                SkinningPipeline skinningPipeline,
                                ResourceLocation baseTexture) {
        if (bloomTexture == null || bloomStrength <= 0f || skinningPipeline == null) {
            return;
        }
        beginFrame(entity, partialTicks);
        if (!frameParamsLocked) {
            if (bloomDownsample > 0) {
                int ds = Math.max(1, Math.min(bloomDownsample, 4));
                if (ds != downsample) {
                    downsample = ds;
                    // force reallocate on first call with new downsample
                    width = 0;
                    height = 0;
                }
            }
            frameParamsLocked = true;
        }
        if (bloomRadius > 0) {
            frameRadius = Math.max(frameRadius, Math.min(bloomRadius, 32));
        }
        if (bloomThreshold >= 0f) {
            frameThreshold = Math.min(frameThreshold, bloomThreshold);
        }
        ensureBuffers();
        if (maskFbo == null) {
            return;
        }

        usedThisFrame = true;

        state.capture();
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        try {
        maskFbo.bindFramebuffer(true);
        GL11.glViewport(0, 0, width, height);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.enableAlpha();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        savedLightX = (int) net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX;
        savedLightY = (int) net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
        int fullBright = 240;
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
            net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, (float) fullBright, (float) fullBright);

        net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
        Minecraft.getMinecraft().getTextureManager().bindTexture(bloomTexture);

        float remaining = bloomStrength;
        while (remaining > 0f) {
            float passStrength = Math.min(1.0f, remaining);
            GlStateManager.color(1.0f, 1.0f, 1.0f, passStrength);
            skinningPipeline.draw();
            remaining -= passStrength;
        }

        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
            net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, (float) savedLightX, (float) savedLightY);

        GlStateManager.enableBlend();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);

        maskFbo.unbindFramebuffer();
        if (main != null) {
            main.bindFramebuffer(true);
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null) {
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        }
        } finally {
            state.restore();
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null) {
                Framebuffer fb = mc.getFramebuffer();
                if (fb != null) {
                    fb.bindFramebuffer(true);
                    GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
                }
            }
        }
    }

    public void renderParticleMask(Entity entity,
                                   float partialTicks,
                                   float bloomStrength,
                                   int bloomRadius,
                                   int bloomDownsample,
                                   float bloomThreshold,
                                   Runnable drawMask) {
        if (drawMask == null || bloomStrength <= 0f) {
            return;
        }
        beginFrame(entity, partialTicks);
        if (!frameParamsLocked) {
            if (bloomDownsample > 0) {
                int ds = Math.max(1, Math.min(bloomDownsample, 4));
                if (ds != downsample) {
                    downsample = ds;
                    width = 0;
                    height = 0;
                }
            }
            frameParamsLocked = true;
        }
        if (bloomRadius > 0) {
            frameRadius = Math.max(frameRadius, Math.min(bloomRadius, 32));
        }
        if (bloomThreshold >= 0f) {
            frameThreshold = Math.min(frameThreshold, bloomThreshold);
        }
        ensureBuffers();
        if (maskFbo == null) {
            return;
        }

        usedThisFrame = true;

        state.capture();
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        try {
            maskFbo.bindFramebuffer(true);
            GL11.glViewport(0, 0, width, height);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        float strength = Math.max(0f, bloomStrength);
        GlStateManager.color(1.0f, 1.0f, 1.0f, strength);
        drawMask.run();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

            maskFbo.unbindFramebuffer();
            if (main != null) {
                main.bindFramebuffer(true);
            }
        } finally {
            state.restore();
        }
    }

    public void endFrame() {
        if (!usedThisFrame) {
            return;
        }
        state.capture();
        try {
        ensureBuffers();
        if (maskFbo == null || blurPing == null || blurPong == null) {
            return;
        }
        ensureShaders();
        if (blurProgram == 0 || blitProgram == 0) {
            return;
        }
        ensureDepthTargets();
        captureDepths();

        int radius = frameRadius > 0 ? frameRadius : 8;
        float threshold = frameThreshold < 1.0f ? frameThreshold : 0.0f;

        // Blur X
        blurPing.bindFramebuffer(true);
        GL11.glViewport(0, 0, width, height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        useBlur(maskFbo.framebufferTexture, 1f, 0f, radius, threshold);
        drawFullscreenQuad();

        // Blur Y
        blurPong.bindFramebuffer(true);
        GL11.glViewport(0, 0, width, height);
        useBlur(blurPing.framebufferTexture, 0f, 1f, radius, threshold);
        drawFullscreenQuad();

        // Composite
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        if (main != null) {
            main.bindFramebuffer(true);
        } else {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
        GL11.glViewport(0, 0, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        useBlit(blurPong.framebufferTexture);
        drawFullscreenQuad();
        GL20.glUseProgram(0);

        } finally {
            state.restore();
            usedThisFrame = false;
        }
    }

    private void ensureBuffers() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        int ds = Math.max(1, downsample);
        int w = Math.max(1, (mc.displayWidth + ds - 1) / ds);
        int h = Math.max(1, (mc.displayHeight + ds - 1) / ds);
        if (w <= 0 || h <= 0) {
            return;
        }
        if (maskFbo != null && w == width && h == height) {
            return;
        }
        width = w;
        height = h;
        if (maskFbo != null) {
            maskFbo.deleteFramebuffer();
            blurPing.deleteFramebuffer();
            blurPong.deleteFramebuffer();
            deleteDepthTargets();
        }
        maskFbo = new Framebuffer(w, h, true);
        blurPing = new Framebuffer(w, h, false);
        blurPong = new Framebuffer(w, h, false);
        maskFbo.setFramebufferFilter(GL11.GL_LINEAR);
        blurPing.setFramebufferFilter(GL11.GL_LINEAR);
        blurPong.setFramebufferFilter(GL11.GL_LINEAR);
        ensureQuad();
    }

    private void ensureQuad() {
        if (quadVao != 0) {
            return;
        }
        quadVao = GL30.glGenVertexArrays();
        quadVbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(quadVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, quadVbo);

        float[] data = new float[]{
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        };
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        int stride = 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    private void ensureShaders() {
        if (blitProgram != 0 && blurProgram != 0) {
            return;
        }
        blitProgram = compileProgram(blitVertex(), blitFragment());
        blurProgram = compileProgram(blurVertex(), blurFragment());
        uBlitTex = GL20.glGetUniformLocation(blitProgram, "DiffuseSampler");
        uBlitSceneDepth = GL20.glGetUniformLocation(blitProgram, "SceneDepth");
        uBlitMaskDepth = GL20.glGetUniformLocation(blitProgram, "MaskDepth");
        uBlitUseDepth = GL20.glGetUniformLocation(blitProgram, "UseDepth");
        uBlitDepthBias = GL20.glGetUniformLocation(blitProgram, "DepthBias");
        uBlurTex = GL20.glGetUniformLocation(blurProgram, "DiffuseSampler");
        uBlurDir = GL20.glGetUniformLocation(blurProgram, "BlurDir");
        uBlurSize = GL20.glGetUniformLocation(blurProgram, "OutSize");
        uBlurRadius = GL20.glGetUniformLocation(blurProgram, "Radius");
        uBlurThreshold = GL20.glGetUniformLocation(blurProgram, "Threshold");
    }

    private void useBlit(int textureId) {
        GL20.glUseProgram(blitProgram);
        GL20.glUniform1i(uBlitTex, 0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        if (depthValid) {
            GL20.glUniform1i(uBlitUseDepth, 1);
            GL20.glUniform1f(uBlitDepthBias, 0.0005f);
            GL20.glUniform1i(uBlitSceneDepth, 1);
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexScene);
            GL20.glUniform1i(uBlitMaskDepth, 2);
            GL13.glActiveTexture(GL13.GL_TEXTURE2);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexMask);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
        } else {
            GL20.glUniform1i(uBlitUseDepth, 0);
        }
    }

    private void useBlur(int textureId, float dirX, float dirY, int radius, float threshold) {
        GL20.glUseProgram(blurProgram);
        GL20.glUniform1i(uBlurTex, 0);
        GL20.glUniform2f(uBlurDir, dirX, dirY);
        GL20.glUniform2f(uBlurSize, (float) width, (float) height);
        GL20.glUniform1i(uBlurRadius, radius);
        GL20.glUniform1f(uBlurThreshold, threshold);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    private void drawFullscreenQuad() {
        GL30.glBindVertexArray(quadVao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(0);
    }

    private int compileProgram(String vsSource, String fsSource) {
        int vs = compileShader(GL20.GL_VERTEX_SHADER, vsSource);
        int fs = compileShader(GL20.GL_FRAGMENT_SHADER, fsSource);
        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vs);
        GL20.glAttachShader(program, fs);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glBindAttribLocation(program, 1, "TexCoord");
        GL20.glLinkProgram(program);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return program;
    }

    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private String blitVertex() {
        return "#version 150\n"
            + "in vec2 Position;\n"
            + "in vec2 TexCoord;\n"
            + "out vec2 v_uv;\n"
            + "void main(){\n"
            + "  gl_Position = vec4(Position.xy, 0.0, 1.0);\n"
            + "  v_uv = TexCoord;\n"
            + "}\n";
    }

    private String blitFragment() {
        return "#version 150\n"
            + "uniform sampler2D DiffuseSampler;\n"
            + "uniform sampler2D SceneDepth;\n"
            + "uniform sampler2D MaskDepth;\n"
            + "uniform int UseDepth;\n"
            + "uniform float DepthBias;\n"
            + "in vec2 v_uv;\n"
            + "out vec4 fragColor;\n"
            + "void main(){\n"
            + "  if (UseDepth == 1) {\n"
            + "    float sceneD = texture(SceneDepth, v_uv).r;\n"
            + "    float maskD = texture(MaskDepth, v_uv).r;\n"
            + "    if (sceneD + DepthBias < maskD) discard;\n"
            + "  }\n"
            + "  fragColor = texture(DiffuseSampler, v_uv);\n"
            + "}\n";
    }

    private String blurVertex() {
        return blitVertex();
    }

    private String blurFragment() {
        return "#version 150\n"
            + "uniform sampler2D DiffuseSampler;\n"
            + "uniform vec2 OutSize;\n"
            + "uniform vec2 BlurDir;\n"
            + "uniform int Radius;\n"
            + "uniform float Threshold;\n"
            + "in vec2 v_uv;\n"
            + "out vec4 fragColor;\n"
            + "float gaussianPdf(in float x, in float sigma) {\n"
            + "  return 0.39894 * exp(-0.5 * x * x/( sigma * sigma))/sigma;\n"
            + "}\n"
            + "void main(){\n"
            + "  vec2 invSize = 1.0 / OutSize;\n"
            + "  float fSigma = float(Radius);\n"
            + "  float weightSum = gaussianPdf(0.0, fSigma);\n"
            + "  vec3 base = texture(DiffuseSampler, v_uv).rgb;\n"
            + "  base = max(base - vec3(Threshold), vec3(0.0));\n"
            + "  vec3 diffuseSum = base * weightSum;\n"
            + "  for(int i = 1; i < Radius; i++){\n"
            + "    float x = float(i);\n"
            + "    float w = gaussianPdf(x, fSigma);\n"
            + "    vec2 uvOffset = BlurDir * invSize * x;\n"
            + "    vec3 s1 = texture(DiffuseSampler, v_uv + uvOffset).rgb;\n"
            + "    vec3 s2 = texture(DiffuseSampler, v_uv - uvOffset).rgb;\n"
            + "    s1 = max(s1 - vec3(Threshold), vec3(0.0));\n"
            + "    s2 = max(s2 - vec3(Threshold), vec3(0.0));\n"
            + "    diffuseSum += (s1 + s2) * w;\n"
            + "    weightSum += 2.0 * w;\n"
            + "  }\n"
            + "  fragColor = vec4(diffuseSum/weightSum, 1.0);\n"
            + "}\n";
    }

    private void ensureDepthTargets() {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (depthTexScene != 0 && depthTexMask != 0 && depthWidth == width && depthHeight == height) {
            return;
        }
        deleteDepthTargets();
        depthTexScene = createDepthTexture(width, height);
        depthTexMask = createDepthTexture(width, height);
        depthFboScene = createDepthFbo(depthTexScene);
        depthFboMask = createDepthFbo(depthTexMask);
        depthWidth = width;
        depthHeight = height;
    }

    private int createDepthTexture(int w, int h) {
        int tex = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24, w, h, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return tex;
    }

    private int createDepthFbo(int depthTex) {
        int fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTex, 0);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        return fbo;
    }

    private void deleteDepthTargets() {
        if (depthTexScene != 0) {
            GL11.glDeleteTextures(depthTexScene);
            depthTexScene = 0;
        }
        if (depthTexMask != 0) {
            GL11.glDeleteTextures(depthTexMask);
            depthTexMask = 0;
        }
        if (depthFboScene != 0) {
            GL30.glDeleteFramebuffers(depthFboScene);
            depthFboScene = 0;
        }
        if (depthFboMask != 0) {
            GL30.glDeleteFramebuffers(depthFboMask);
            depthFboMask = 0;
        }
        depthWidth = 0;
        depthHeight = 0;
        depthValid = false;
    }

    private void captureDepths() {
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        if (main == null || maskFbo == null || depthFboScene == 0 || depthFboMask == 0) {
            depthValid = false;
            return;
        }
        if (main.framebufferObject == 0) {
            depthValid = false;
            return;
        }
        int mainW = Minecraft.getMinecraft().displayWidth;
        int mainH = Minecraft.getMinecraft().displayHeight;
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthFboScene);
        GL30.glBlitFramebuffer(0, 0, mainW, mainH, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, maskFbo.framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, depthFboMask);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        depthValid = true;
    }
    private static final class GlStateSnapshot {
        private final FloatBuffer colorBuf = BufferUtils.createFloatBuffer(16);
        private final IntBuffer viewportBuf = BufferUtils.createIntBuffer(16);
        private int activeTex;
        private int boundTex0;
        private int boundTex1;
        private int blendSrc;
        private int blendDst;
        private int depthFunc;
        private int alphaFunc;
        private float alphaRef;
        private boolean blend;
        private boolean depthTest;
        private boolean alphaTest;
        private boolean cull;
        private boolean lighting;
        private boolean colorMaterial;
        private boolean texture2d;
        private boolean depthMask;
        private boolean scissorTest;
        private int scissorX;
        private int scissorY;
        private int scissorW;
        private int scissorH;
        private float r;
        private float g;
        private float b;
        private float a;
        private int lightX;
        private int lightY;
        private int vpX;
        private int vpY;
        private int vpW;
        private int vpH;

        void capture() {
            blend = GL11.glIsEnabled(GL11.GL_BLEND);
            depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
            cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
            colorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
            texture2d = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            depthMask = GL11.glGetInteger(GL11.GL_DEPTH_WRITEMASK) != 0;
            scissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
            depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            alphaFunc = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
            alphaRef = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
            blendSrc = GL11.glGetInteger(GL11.GL_BLEND_SRC);
            blendDst = GL11.glGetInteger(GL11.GL_BLEND_DST);

            activeTex = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
            boundTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
            boundTex1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(activeTex);

            colorBuf.clear();
            GL11.glGetFloat(GL11.GL_CURRENT_COLOR, colorBuf);
            r = colorBuf.get(0);
            g = colorBuf.get(1);
            b = colorBuf.get(2);
            a = colorBuf.get(3);

            viewportBuf.clear();
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuf);
            vpX = viewportBuf.get(0);
            vpY = viewportBuf.get(1);
            vpW = viewportBuf.get(2);
            vpH = viewportBuf.get(3);
            if (scissorTest) {
                viewportBuf.clear();
                GL11.glGetInteger(GL11.GL_SCISSOR_BOX, viewportBuf);
                scissorX = viewportBuf.get(0);
                scissorY = viewportBuf.get(1);
                scissorW = viewportBuf.get(2);
                scissorH = viewportBuf.get(3);
            }

            lightX = (int) net.minecraft.client.renderer.OpenGlHelper.lastBrightnessX;
            lightY = (int) net.minecraft.client.renderer.OpenGlHelper.lastBrightnessY;
        }

        void restore() {
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            GlStateManager.blendFunc(blendSrc, blendDst);

            if (depthTest) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            GL11.glDepthFunc(depthFunc);
            GlStateManager.depthMask(depthMask);

            if (alphaTest) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            GL11.glAlphaFunc(alphaFunc, alphaRef);
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (colorMaterial) GlStateManager.enableColorMaterial(); else GlStateManager.disableColorMaterial();
            if (texture2d) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (scissorTest) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
            } else {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }

            GlStateManager.color(r, g, b, a);
            GL11.glViewport(vpX, vpY, vpW, vpH);

            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.defaultTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTex0);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTex1);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(activeTex);
        }
    }


}
