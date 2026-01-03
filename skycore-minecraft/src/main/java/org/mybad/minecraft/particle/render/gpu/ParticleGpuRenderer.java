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
    private final FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer viewProjBuffer = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer fogColorBuffer = BufferUtils.createFloatBuffer(16);
    private final float[] projTmp = new float[16];
    private final float[] modelTmp = new float[16];
    private final float[] viewProjTmp = new float[16];

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

        float[] cameraAxes = computeCameraAxes(mc);
        float rightX = cameraAxes[0];
        float rightY = cameraAxes[1];
        float rightZ = cameraAxes[2];
        float upX = cameraAxes[3];
        float upY = cameraAxes[4];
        float upZ = cameraAxes[5];
        float[] cameraOffset = extractCameraOffset(camX, camY, camZ);

        FogState fog = captureFogState();
        int lightmapId = getLightmapTextureId(mc);
        boolean lightmapAvailable = lightmapId != 0;

        setupRenderState();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);

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
        applyBlendMode(BedrockParticleSystem.BlendMode.ALPHA);
    }

    private void restoreRenderState() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
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
        projBuffer.clear();
        modelBuffer.clear();
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuffer);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
        projBuffer.rewind();
        modelBuffer.rewind();
        projBuffer.get(projTmp);
        modelBuffer.get(modelTmp);
        multiplyMat4(projTmp, modelTmp, viewProjTmp);
        viewProjBuffer.clear();
        viewProjBuffer.put(viewProjTmp).flip();
        return viewProjBuffer;
    }

    private float[] computeCameraAxes(Minecraft mc) {
        float yaw = 0.0f;
        float pitch = 0.0f;
        if (mc != null && mc.getRenderManager() != null) {
            yaw = mc.getRenderManager().playerViewY;
            pitch = mc.getRenderManager().playerViewX;
        } else {
            float[] axes = extractCameraAxesFromModelView();
            if (axes != null) {
                return axes;
            }
        }
        float yawRad = (float) Math.toRadians(-yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        float cosY = (float) Math.cos(yawRad);
        float sinY = (float) Math.sin(yawRad);
        float cosP = (float) Math.cos(pitchRad);
        float sinP = (float) Math.sin(pitchRad);

        float rightX = cosY;
        float rightY = 0.0f;
        float rightZ = -sinY;

        float upX = sinY * sinP;
        float upY = cosP;
        float upZ = cosY * sinP;

        return new float[]{rightX, rightY, rightZ, upX, upY, upZ};
    }

    private float[] extractCameraAxesFromModelView() {
        modelBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
        modelBuffer.rewind();
        modelBuffer.get(modelTmp);
        // Column-major: row0 = (m0, m4, m8), row1 = (m1, m5, m9)
        float rightX = modelTmp[0];
        float rightY = modelTmp[4];
        float rightZ = modelTmp[8];
        float upX = modelTmp[1];
        float upY = modelTmp[5];
        float upZ = modelTmp[9];
        float rightLen = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        float upLen = (float) Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        if (rightLen < 1.0e-6f || upLen < 1.0e-6f) {
            return null;
        }
        rightX /= rightLen;
        rightY /= rightLen;
        rightZ /= rightLen;
        upX /= upLen;
        upY /= upLen;
        upZ /= upLen;
        return new float[]{rightX, rightY, rightZ, upX, upY, upZ};
    }

    private float[] extractCameraOffset(double camX, double camY, double camZ) {
        modelBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelBuffer);
        modelBuffer.rewind();
        modelBuffer.get(modelTmp);
        float tx = modelTmp[12];
        float ty = modelTmp[13];
        float tz = modelTmp[14];
        double dx = tx + camX;
        double dy = ty + camY;
        double dz = tz + camZ;
        double diffSq = dx * dx + dy * dy + dz * dz;
        // If modelview translation already matches -camera, avoid double-subtracting.
        if (diffSq < 1.0e-2) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        return new float[]{(float) camX, (float) camY, (float) camZ};
    }

    private static void multiplyMat4(float[] a, float[] b, float[] out) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                out[col * 4 + row] = a[0 * 4 + row] * b[col * 4 + 0]
                    + a[1 * 4 + row] * b[col * 4 + 1]
                    + a[2 * 4 + row] * b[col * 4 + 2]
                    + a[3 * 4 + row] * b[col * 4 + 3];
            }
        }
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

    }

    private FogState captureFogState() {
        boolean enabled = GL11.glIsEnabled(GL11.GL_FOG);
        float start = GL11.glGetFloat(GL11.GL_FOG_START);
        float end = GL11.glGetFloat(GL11.GL_FOG_END);
        fogColorBuffer.clear();
        GL11.glGetFloat(GL11.GL_FOG_COLOR, fogColorBuffer);
        fogColorBuffer.rewind();
        float r = fogColorBuffer.get();
        float g = fogColorBuffer.get();
        float b = fogColorBuffer.get();
        return new FogState(enabled, r, g, b, start, end);
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
