package org.mybad.minecraft.particle.render.gpu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.mybad.minecraft.particle.runtime.ActiveParticle;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * SSBO + instanced 粒子渲染入口。
 */
public final class ParticleGpuRenderer {
    private static final Field LIGHTMAP_TEXTURE_FIELD = resolveLightmapField();

    private final ParticleBatcher batcher = new ParticleBatcher();
    private final ParticleSsboBuffer ssboBuffer = new ParticleSsboBuffer();
    private final ParticleQuadMesh quadMesh = new ParticleQuadMesh();
    private final ParticleShader shaderLit = new ParticleShader(true);
    private final ParticleShader shaderUnlit = new ParticleShader(false);

    private boolean ready;

    public boolean isAvailable() {
        return GpuParticleSupport.isGpuParticleAvailable();
    }

    public void render(List<ActiveParticle> particles,
                       Minecraft mc,
                       double camX,
                       double camY,
                       double camZ,
                       float partialTicks) {
        if (particles.isEmpty()) {
            return;
        }
        if (!isAvailable()) {
            return;
        }

        ensureReady();
        if (!ready) {
            return;
        }

        ParticleBatcher.Result result = batcher.build(particles, partialTicks);
        if (result.particleCount <= 0 || result.batches.isEmpty()) {
            return;
        }

        int particleBytes = result.particleBuffer.remaining() * Float.BYTES;
        int emitterBytes = result.emitterBuffer.remaining() * Float.BYTES;
        ssboBuffer.uploadParticles(result.particleBuffer, particleBytes);
        ssboBuffer.uploadEmitters(result.emitterBuffer, emitterBytes);

        FloatBuffer viewProj = buildViewProj();
        if (viewProj == null) {
            return;
        }

        float[] cameraAxes = extractCameraAxes();
        float rightX = cameraAxes[0];
        float rightY = cameraAxes[1];
        float rightZ = cameraAxes[2];
        float upX = cameraAxes[3];
        float upY = cameraAxes[4];
        float upZ = cameraAxes[5];
        float[] cameraOffset = extractCameraOffset(camX, camY, camZ);

        FogState fog = FogState.capture();
        int lightmapId = getLightmapTextureId(mc);
        boolean lightmapAvailable = lightmapId != 0;

        setupRenderState();

        ssboBuffer.bind();
        quadMesh.bind();

        ParticleShader currentShader = null;
        BedrockParticleSystem.BlendMode currentBlend = null;
        ResourceLocation currentTexture = null;

        for (ParticleBatcher.Batch batch : result.batches) {
            ParticleBatcher.BatchKey key = batch.key;
            ParticleShader shader = (key.lit && lightmapAvailable) ? shaderLit : shaderUnlit;
            if (shader != currentShader) {
                if (currentShader != null) {
                    currentShader.stop();
                }
                shader.use();
                shader.setViewProj(viewProj);
                shader.setCamera((float) camX, (float) camY, (float) camZ, rightX, rightY, rightZ, upX, upY, upZ);
                shader.setCameraOffset(cameraOffset[0], cameraOffset[1], cameraOffset[2]);
                shader.setFog(fog.r, fog.g, fog.b, fog.start, fog.end, fog.enabled);
                currentShader = shader;
            }

            shader.setInstanceOffset(batch.offset);

            if (key.blendMode != currentBlend) {
                applyBlendMode(key.blendMode);
                currentBlend = key.blendMode;
            }

            if (!key.texture.equals(currentTexture)) {
                mc.getTextureManager().bindTexture(key.texture);
                currentTexture = key.texture;
            }

            if (key.lit && lightmapAvailable) {
                OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
                GlStateManager.bindTexture(lightmapId);
                OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            }

            GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, 6, GL11.GL_UNSIGNED_INT, 0, batch.count);
        }

        if (currentShader != null) {
            currentShader.stop();
        }

        quadMesh.unbind();
        ssboBuffer.unbind();

        restoreRenderState();
    }

    private void ensureReady() {
        if (ready) {
            return;
        }
        try {
            quadMesh.ensureCreated();
            ssboBuffer.ensureCreated();
            shaderLit.ensureProgram();
            shaderUnlit.ensureProgram();
            ready = true;
        } catch (Throwable t) {
            GpuParticleSupport.disable("shader/buffer init failed");
            ready = false;
        }
    }

    private void setupRenderState() {
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.enableAlpha();
        applyBlendMode(BedrockParticleSystem.BlendMode.ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    private void restoreRenderState() {
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }

    private void applyBlendMode(BedrockParticleSystem.BlendMode mode) {
        if (mode == BedrockParticleSystem.BlendMode.OPAQUE) {
            GlStateManager.disableBlend();
            return;
        }
        GlStateManager.enableBlend();
        if (mode == BedrockParticleSystem.BlendMode.ADD) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }

    private FloatBuffer buildViewProj() {
        FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuffer);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
        projBuffer.rewind();
        modelBuffer.rewind();
        float[] proj = new float[16];
        float[] model = new float[16];
        projBuffer.get(proj);
        modelBuffer.get(model);
        float[] viewProj = multiplyMat4(proj, model);
        FloatBuffer out = BufferUtils.createFloatBuffer(16);
        out.put(viewProj).flip();
        return out;
    }

    private float[] extractCameraAxes() {
        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
        modelBuffer.rewind();
        float[] m = new float[16];
        modelBuffer.get(m);
        // row 0/1 from column-major matrix
        float rx = m[0];
        float ry = m[4];
        float rz = m[8];
        float ux = m[1];
        float uy = m[5];
        float uz = m[9];
        float rLen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        float uLen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (rLen > 0.0f) {
            rx /= rLen;
            ry /= rLen;
            rz /= rLen;
        }
        if (uLen > 0.0f) {
            ux /= uLen;
            uy /= uLen;
            uz /= uLen;
        }
        return new float[]{rx, ry, rz, ux, uy, uz};
    }

    private float[] extractCameraOffset(double camX, double camY, double camZ) {
        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
        modelBuffer.rewind();
        float[] m = new float[16];
        modelBuffer.get(m);
        float tx = m[12];
        float ty = m[13];
        float tz = m[14];
        float mag = Math.abs(tx) + Math.abs(ty) + Math.abs(tz);
        if (mag > 1.0e-4f) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        return new float[]{(float) camX, (float) camY, (float) camZ};
    }

    private static float[] multiplyMat4(float[] a, float[] b) {
        float[] out = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                out[col * 4 + row] = a[0 * 4 + row] * b[col * 4 + 0]
                    + a[1 * 4 + row] * b[col * 4 + 1]
                    + a[2 * 4 + row] * b[col * 4 + 2]
                    + a[3 * 4 + row] * b[col * 4 + 3];
            }
        }
        return out;
    }

    private static final class FogState {
        final boolean enabled;
        final float r;
        final float g;
        final float b;
        final float start;
        final float end;

        private FogState(boolean enabled, float r, float g, float b, float start, float end) {
            this.enabled = enabled;
            this.r = r;
            this.g = g;
            this.b = b;
            this.start = start;
            this.end = end;
        }

        static FogState capture() {
            boolean enabled = GL11.glIsEnabled(GL11.GL_FOG);
            float start = GL11.glGetFloat(GL11.GL_FOG_START);
            float end = GL11.glGetFloat(GL11.GL_FOG_END);
            FloatBuffer fogColor = BufferUtils.createFloatBuffer(16);
            GL11.glGetFloat(GL11.GL_FOG_COLOR, fogColor);
            fogColor.rewind();
            float r = fogColor.get();
            float g = fogColor.get();
            float b = fogColor.get();
            return new FogState(enabled, r, g, b, start, end);
        }
    }

    private static Field resolveLightmapField() {
        try {
            return ReflectionHelper.findField(EntityRenderer.class, "lightmapTexture", "field_78513_d");
        } catch (Throwable t) {
            return null;
        }
    }

    private static int getLightmapTextureId(Minecraft mc) {
        if (mc == null || mc.entityRenderer == null) {
            return 0;
        }
        if (LIGHTMAP_TEXTURE_FIELD == null) {
            return 0;
        }
        try {
            Object value = LIGHTMAP_TEXTURE_FIELD.get(mc.entityRenderer);
            if (value instanceof DynamicTexture) {
                return ((DynamicTexture) value).getGlTextureId();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
