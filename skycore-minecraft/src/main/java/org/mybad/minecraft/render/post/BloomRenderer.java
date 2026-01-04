package org.mybad.minecraft.render.post;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
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
    private int uBlitTex;

    private boolean usedThisFrame;
    private final GlStateSnapshot state = new GlStateSnapshot();
    private long lastTick = Long.MIN_VALUE;
    private float lastPartial = Float.NaN;
    private int savedLightX;
    private int savedLightY;

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
    }

    public void renderBloomMask(Entity entity,
                                float partialTicks,
                                ResourceLocation bloomTexture,
                                float bloomStrength,
                                int lightX,
                                int lightY,
                                SkinningPipeline skinningPipeline,
                                ResourceLocation baseTexture) {
        if (bloomTexture == null || bloomStrength <= 0f || skinningPipeline == null) {
            return;
        }
        beginFrame(entity, partialTicks);
        ensureBuffers();
        if (maskFbo == null) {
            return;
        }

        usedThisFrame = true;

        state.capture();
        Framebuffer main = Minecraft.getMinecraft().getFramebuffer();
        try {
        maskFbo.bindFramebuffer(true);

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

        int radius = 8;

        // Blur X
        blurPing.bindFramebuffer(true);
        GL11.glViewport(0, 0, width, height);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        useBlur(maskFbo.framebufferTexture, 1f, 0f, radius);
        drawFullscreenQuad();

        // Blur Y
        blurPong.bindFramebuffer(true);
        GL11.glViewport(0, 0, width, height);
        useBlur(blurPing.framebufferTexture, 0f, 1f, radius);
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
        int w = mc.displayWidth;
        int h = mc.displayHeight;
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
        uBlurTex = GL20.glGetUniformLocation(blurProgram, "DiffuseSampler");
        uBlurDir = GL20.glGetUniformLocation(blurProgram, "BlurDir");
        uBlurSize = GL20.glGetUniformLocation(blurProgram, "OutSize");
        uBlurRadius = GL20.glGetUniformLocation(blurProgram, "Radius");
    }

    private void useBlit(int textureId) {
        GL20.glUseProgram(blitProgram);
        GL20.glUniform1i(uBlitTex, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    private void useBlur(int textureId, float dirX, float dirY, int radius) {
        GL20.glUseProgram(blurProgram);
        GL20.glUniform1i(uBlurTex, 0);
        GL20.glUniform2f(uBlurDir, dirX, dirY);
        GL20.glUniform2f(uBlurSize, (float) width, (float) height);
        GL20.glUniform1i(uBlurRadius, radius);
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
            + "in vec2 v_uv;\n"
            + "out vec4 fragColor;\n"
            + "void main(){\n"
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
            + "in vec2 v_uv;\n"
            + "out vec4 fragColor;\n"
            + "float gaussianPdf(in float x, in float sigma) {\n"
            + "  return 0.39894 * exp(-0.5 * x * x/( sigma * sigma))/sigma;\n"
            + "}\n"
            + "void main(){\n"
            + "  vec2 invSize = 1.0 / OutSize;\n"
            + "  float fSigma = float(Radius);\n"
            + "  float weightSum = gaussianPdf(0.0, fSigma);\n"
            + "  vec3 diffuseSum = texture(DiffuseSampler, v_uv).rgb * weightSum;\n"
            + "  for(int i = 1; i < Radius; i++){\n"
            + "    float x = float(i);\n"
            + "    float w = gaussianPdf(x, fSigma);\n"
            + "    vec2 uvOffset = BlurDir * invSize * x;\n"
            + "    vec3 s1 = texture(DiffuseSampler, v_uv + uvOffset).rgb;\n"
            + "    vec3 s2 = texture(DiffuseSampler, v_uv - uvOffset).rgb;\n"
            + "    diffuseSum += (s1 + s2) * w;\n"
            + "    weightSum += 2.0 * w;\n"
            + "  }\n"
            + "  fragColor = vec4(diffuseSum/weightSum, 1.0);\n"
            + "}\n";
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

            GlStateManager.color(r, g, b, a);
            GL11.glViewport(vpX, vpY, vpW, vpH);

            net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTex0);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, boundTex1);
            net.minecraft.client.renderer.OpenGlHelper.setActiveTexture(activeTex);
        }
    }


}
