package org.mybad.minecraft.particle.render;

import org.mybad.bedrockparticle.particle.component.ParticleAppearanceBillboardComponent;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceLightingComponent;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceTintingComponent;
import org.mybad.bedrockparticle.particle.render.QuadRenderProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.mybad.minecraft.particle.runtime.ActiveEmitter;
import org.mybad.minecraft.particle.runtime.ActiveParticle;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.lwjgl.opengl.GL11;

public final class ParticleRenderer {
    private final ParticleAppearanceBillboardComponent billboard;
    private final ParticleAppearanceTintingComponent tint;
    private final ParticleAppearanceLightingComponent lighting;
    private final ResourceLocation texture;
    private final ResourceLocation emissiveTexture;
    private final float emissiveStrength;
    private final BedrockParticleSystem.BlendMode blendMode;
    private final QuadRenderProperties renderProps;
    private final float[] tempDir;
    private final float[] tempAxisX;
    private final float[] tempAxisY;
    private final float[] tempAxisZ;
    private final Quaternionf tempQuat;
    private final Vector3f tempVecX;
    private final Vector3f tempVecY;
    private final Vector3f tempVecZ;
    private final Vector3f tempVecA;
    private final Vector3f tempVecB;

    public ParticleRenderer(ParticleAppearanceBillboardComponent billboard,
                     ParticleAppearanceTintingComponent tint,
                     ParticleAppearanceLightingComponent lighting,
                     ResourceLocation texture,
                     ResourceLocation emissiveTexture,
                     float emissiveStrength,
                     BedrockParticleSystem.BlendMode blendMode) {
        this.billboard = billboard;
        this.tint = tint;
        this.lighting = lighting;
        this.texture = texture;
        this.emissiveTexture = emissiveTexture;
        this.emissiveStrength = emissiveStrength;
        this.blendMode = blendMode;
        this.renderProps = new QuadRenderProperties();
        this.tempDir = new float[3];
        this.tempAxisX = new float[3];
        this.tempAxisY = new float[3];
        this.tempAxisZ = new float[3];
        this.tempQuat = new Quaternionf();
        this.tempVecX = new Vector3f();
        this.tempVecY = new Vector3f();
        this.tempVecZ = new Vector3f();
        this.tempVecA = new Vector3f();
        this.tempVecB = new Vector3f();
    }

    public void render(ActiveParticle particle, Minecraft mc, double camX, double camY, double camZ, float partialTicks) {
        float width = 1.0f;
        float height = 1.0f;
        if (billboard != null && billboard.size() != null && billboard.size().length >= 2) {
            width = particle.getEnvironment().safeResolve(billboard.size()[0]);
            height = particle.getEnvironment().safeResolve(billboard.size()[1]);
        }
        // Match Bedrock/Pollen quad sizing (base quad is 2x2 units).
        width *= 2.0f;
        height *= 2.0f;
        if (width <= 0.0f || height <= 0.0f) {
            return;
        }
        renderProps.setWidth(width);
        renderProps.setHeight(height);
        renderProps.setUV(0.0f, 0.0f, 1.0f, 1.0f);
        if (billboard != null) {
            billboard.textureSetter().setUV(particle, particle.getEnvironment(), renderProps);
        }
        double px = particle.getPrevX() + (particle.getX() - particle.getPrevX()) * partialTicks;
        double py = particle.getPrevY() + (particle.getY() - particle.getPrevY()) * partialTicks;
        double pz = particle.getPrevZ() + (particle.getZ() - particle.getPrevZ()) * partialTicks;
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        if (tint != null) {
            r = clamp01(tint.red().get(particle, particle.getEnvironment()));
            g = clamp01(tint.green().get(particle, particle.getEnvironment()));
            b = clamp01(tint.blue().get(particle, particle.getEnvironment()));
            a = clamp01(tint.alpha().get(particle, particle.getEnvironment()));
        }
        renderProps.setColor(r, g, b, a);

        applyBlendMode();
        GlStateManager.pushMatrix();
        GlStateManager.translate(px - camX, py - camY, pz - camZ);
        GlStateManager.translate(0.0, 0.01, 0.0);
        float yaw = mc.getRenderManager().playerViewY;
        float pitch = mc.getRenderManager().playerViewX;
        boolean oriented = false;
        boolean directionMode = false;
        if (billboard != null && billboard.cameraMode() != null) {
            switch (billboard.cameraMode()) {
                case ROTATE_Y:
                    GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                    oriented = true;
                    break;
                case ROTATE_XYZ:
                    GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                    oriented = true;
                    break;
                case LOOK_AT_XYZ:
                    oriented = applyLookAt(particle, camX, camY, camZ, px, py, pz, false);
                    break;
                case LOOK_AT_Y:
                    oriented = applyLookAt(particle, camX, camY, camZ, px, py, pz, true);
                    break;
                case EMITTER_TRANSFORM_XY:
                case EMITTER_TRANSFORM_XZ:
                case EMITTER_TRANSFORM_YZ:
                    oriented = applyEmitterTransform(particle, billboard.cameraMode());
                    break;
                case LOOKAT_DIRECTION:
                case DIRECTION_X:
                case DIRECTION_Y:
                case DIRECTION_Z:
                    directionMode = true;
                    oriented = applyDirectionFacing(particle, billboard.cameraMode(), camX, camY, camZ, px, py, pz);
                    break;
                default:
                    break;
            }
        }
        if (!oriented) {
            if (directionMode) {
                GlStateManager.popMatrix();
                resetBlendMode();
                return;
            }
            GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        }
        int prevLightX = (int) OpenGlHelper.lastBrightnessX;
        int prevLightY = (int) OpenGlHelper.lastBrightnessY;
        int lightX;
        int lightY;
        if (lighting != null) {
            int packed = particle.resolvePackedLight(px, py, pz);
            lightX = packed & 0xFFFF;
            lightY = (packed >> 16) & 0xFFFF;
            renderProps.setPackedLight(packed);
        } else {
            lightX = 240;
            lightY = 240;
            renderProps.setPackedLight((lightY << 16) | lightX);
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        float renderRoll = particle.getPrevRoll() + (particle.getRoll() - particle.getPrevRoll()) * partialTicks;
        if (renderRoll != 0.0f) {
            GlStateManager.rotate(renderRoll, 0.0F, 0.0F, 1.0F);
        }
        mc.getTextureManager().bindTexture(texture);

        float halfW = renderProps.getWidth() * 0.5f;
        float halfH = renderProps.getHeight() * 0.5f;
        float u0 = renderProps.getUMin();
        float v0 = renderProps.getVMin();
        float u1 = renderProps.getUMax();
        float v1 = renderProps.getVMax();

        int cr = toColor(renderProps.getRed());
        int cg = toColor(renderProps.getGreen());
        int cb = toColor(renderProps.getBlue());
        int ca = toColor(renderProps.getAlpha());

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-halfW, -halfH, 0.0).tex(u0, v1).color(cr, cg, cb, ca).endVertex();
        buffer.pos(halfW, -halfH, 0.0).tex(u1, v1).color(cr, cg, cb, ca).endVertex();
        buffer.pos(halfW, halfH, 0.0).tex(u1, v0).color(cr, cg, cb, ca).endVertex();
        buffer.pos(-halfW, halfH, 0.0).tex(u0, v0).color(cr, cg, cb, ca).endVertex();
        Tessellator.getInstance().draw();
        if (emissiveTexture != null && emissiveStrength > 0.0f) {
            renderEmissivePass(mc, halfW, halfH, u0, v0, u1, v1, cr, cg, cb, ca, prevLightX, prevLightY);
        } else {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) prevLightX, (float) prevLightY);
        }

        GlStateManager.popMatrix();
        resetBlendMode();
    }

    private void renderEmissivePass(Minecraft mc,
                                    float halfW, float halfH,
                                    float u0, float v0, float u1, float v1,
                                    int cr, int cg, int cb, int ca,
                                    int prevLightX, int prevLightY) {
        mc.getTextureManager().bindTexture(emissiveTexture);
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

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-halfW, -halfH, 0.0).tex(u0, v1).color(cr, cg, cb, ca).endVertex();
        buffer.pos(halfW, -halfH, 0.0).tex(u1, v1).color(cr, cg, cb, ca).endVertex();
        buffer.pos(halfW, halfH, 0.0).tex(u1, v0).color(cr, cg, cb, ca).endVertex();
        buffer.pos(-halfW, halfH, 0.0).tex(u0, v0).color(cr, cg, cb, ca).endVertex();
        Tessellator.getInstance().draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) prevLightX, (float) prevLightY);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        mc.getTextureManager().bindTexture(texture);
    }

    private boolean applyLookAt(ActiveParticle particle, double camX, double camY, double camZ,
                                double px, double py, double pz, boolean yOnly) {
        float dx = (float) (camX - px);
        float dy = (float) (camY - py);
        float dz = (float) (camZ - pz);
        if (yOnly) {
            dy = 0.0f;
        }
        tempDir[0] = dx;
        tempDir[1] = dy;
        tempDir[2] = dz;
        if (tempDir[0] == 0.0f && tempDir[1] == 0.0f && tempDir[2] == 0.0f) {
            return false;
        }
        return applyDirectionAsNormal(tempDir);
    }

    private boolean applyDirectionFacing(ActiveParticle particle, ParticleAppearanceBillboardComponent.FaceCameraMode mode,
                                         double camX, double camY, double camZ,
                                         double px, double py, double pz) {
        if (!resolveFacingDirection(particle, tempDir)) {
            return false;
        }
        float dx = tempDir[0];
        float dy = tempDir[1];
        float dz = tempDir[2];
        float lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq <= 1.0e-6f) {
            return false;
        }
        switch (mode) {
            case DIRECTION_X:
                return applyDirectionX(dx, dy, dz);
            case DIRECTION_Y:
                return applyDirectionY(dx, dy, dz);
            case DIRECTION_Z:
                return applyDirectionZ(dx, dy, dz);
            case LOOKAT_DIRECTION:
            default:
                return applyLookAtDirection(dx, dy, dz, camX, camY, camZ, px, py, pz);
        }
    }

    private boolean applyDirectionX(float dx, float dy, float dz) {
        float yawDeg = getDirectionYawDeg(dx, dy, dz);
        float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
        tempQuat.identity();
        tempQuat.rotateY((float) Math.toRadians(yawDeg));
        tempQuat.rotateX((float) Math.toRadians(pitchDeg));
        tempQuat.rotateY((float) Math.toRadians(90.0f));
        tempQuat.rotateZ((float) Math.toRadians(90.0f));
        return applyRotationFromQuaternion();
    }

    private boolean applyDirectionY(float dx, float dy, float dz) {
        float yawDeg = getDirectionYawDeg(dx, dy, dz);
        float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
        tempQuat.identity();
        tempQuat.rotateY((float) Math.toRadians(yawDeg));
        tempQuat.rotateX((float) Math.toRadians(pitchDeg + 90.0f));
        tempQuat.rotateZ((float) Math.toRadians(90.0f));
        return applyRotationFromQuaternion();
    }

    private boolean applyDirectionZ(float dx, float dy, float dz) {
        float yawDeg = getDirectionYawDeg(dx, dy, dz);
        float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
        tempQuat.identity();
        tempQuat.rotateY((float) Math.toRadians(yawDeg));
        tempQuat.rotateX((float) Math.toRadians(pitchDeg));
        tempQuat.rotateZ((float) Math.toRadians(90.0f));
        return applyRotationFromQuaternion();
    }

    private boolean applyLookAtDirection(float dx, float dy, float dz,
                                         double camX, double camY, double camZ,
                                         double px, double py, double pz) {
        // Blockbuster/Snowstorm-like: align to direction, then rotate around local Y to face camera.
        float yawDeg = getDirectionYawDeg(dx, dy, dz);
        float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
        tempQuat.identity();
        tempQuat.rotateY((float) Math.toRadians(yawDeg));
        tempQuat.rotateX((float) Math.toRadians(pitchDeg + 90.0f));

        // rotated normal
        tempVecZ.set(0.0f, 0.0f, 1.0f);
        tempQuat.transform(tempVecZ);

        // camera direction projected onto plane (direction is the plane normal)
        tempVecA.set((float) (camX - px), (float) (camY - py), (float) (camZ - pz));
        tempVecB.set(dx, dy, dz);
        if (tempVecB.lengthSquared() <= 1.0e-6f) {
            return false;
        }
        tempVecB.normalize();
        float dot = tempVecA.dot(tempVecB);
        tempVecA.sub(tempVecB.x * dot, tempVecB.y * dot, tempVecB.z * dot);
        if (tempVecA.lengthSquared() <= 1.0e-6f) {
            return false;
        }
        tempVecA.normalize();

        tempVecX.set(tempVecA).cross(tempVecZ);
        float angle = tempVecA.angle(tempVecZ);
        float sign = tempVecX.dot(tempVecB);
        float finalRot = (float) -Math.copySign(angle, sign);
        tempQuat.rotateY(finalRot);
        tempQuat.rotateZ((float) Math.toRadians(90.0f));

        return applyRotationFromQuaternion();
    }

    private float getDirectionYawDeg(float dx, float dy, float dz) {
        double yaw = Math.atan2(-dx, dz);
        return (float) -Math.toDegrees(yaw);
    }

    private float getDirectionPitchDeg(float dx, float dy, float dz) {
        double pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
        return (float) -Math.toDegrees(pitch);
    }

    private boolean applyRotationFromQuaternion() {
        tempVecX.set(1.0f, 0.0f, 0.0f);
        tempVecY.set(0.0f, 1.0f, 0.0f);
        tempVecZ.set(0.0f, 0.0f, 1.0f);
        tempQuat.transform(tempVecX);
        tempQuat.transform(tempVecY);
        tempQuat.transform(tempVecZ);
        tempAxisX[0] = tempVecX.x;
        tempAxisX[1] = tempVecX.y;
        tempAxisX[2] = tempVecX.z;
        tempAxisY[0] = tempVecY.x;
        tempAxisY[1] = tempVecY.y;
        tempAxisY[2] = tempVecY.z;
        tempAxisZ[0] = tempVecZ.x;
        tempAxisZ[1] = tempVecZ.y;
        tempAxisZ[2] = tempVecZ.z;
        BedrockParticleSystem.normalize(tempAxisX);
        BedrockParticleSystem.normalize(tempAxisY);
        BedrockParticleSystem.normalize(tempAxisZ);
        BedrockParticleSystem.multMatrix(tempAxisX, tempAxisY, tempAxisZ);
        return true;
    }

    private boolean applyEmitterTransform(ActiveParticle particle, ParticleAppearanceBillboardComponent.FaceCameraMode mode) {
        ActiveEmitter emitter = particle.getEmitter();
        if (emitter == null) {
            return false;
        }
        float[] ex = emitter.getBasisX();
        float[] ey = emitter.getBasisY();
        float[] ez = emitter.getBasisZ();
        if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_XZ) {
            tempAxisX[0] = ex[0];
            tempAxisX[1] = ex[1];
            tempAxisX[2] = ex[2];
            tempAxisY[0] = ez[0];
            tempAxisY[1] = ez[1];
            tempAxisY[2] = ez[2];
            tempAxisZ[0] = -ey[0];
            tempAxisZ[1] = -ey[1];
            tempAxisZ[2] = -ey[2];
        } else if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_YZ) {
            tempAxisX[0] = -ez[0];
            tempAxisX[1] = -ez[1];
            tempAxisX[2] = -ez[2];
            tempAxisY[0] = ey[0];
            tempAxisY[1] = ey[1];
            tempAxisY[2] = ey[2];
            tempAxisZ[0] = ex[0];
            tempAxisZ[1] = ex[1];
            tempAxisZ[2] = ex[2];
        } else if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_XY) {
            tempAxisX[0] = ex[0];
            tempAxisX[1] = ex[1];
            tempAxisX[2] = ex[2];
            tempAxisY[0] = ey[0];
            tempAxisY[1] = ey[1];
            tempAxisY[2] = ey[2];
            tempAxisZ[0] = ez[0];
            tempAxisZ[1] = ez[1];
            tempAxisZ[2] = ez[2];
        } else {
            tempAxisX[0] = ex[0];
            tempAxisX[1] = ex[1];
            tempAxisX[2] = ex[2];
            tempAxisY[0] = ey[0];
            tempAxisY[1] = ey[1];
            tempAxisY[2] = ey[2];
            tempAxisZ[0] = ez[0];
            tempAxisZ[1] = ez[1];
            tempAxisZ[2] = ez[2];
        }
        BedrockParticleSystem.normalize(tempAxisX);
        BedrockParticleSystem.normalize(tempAxisY);
        BedrockParticleSystem.normalize(tempAxisZ);
        BedrockParticleSystem.multMatrix(tempAxisX, tempAxisY, tempAxisZ);
        return true;
    }

    private boolean resolveFacingDirection(ActiveParticle particle, float[] out) {
        if (billboard != null && billboard.customDirection() != null) {
            out[0] = particle.getEnvironment().safeResolve(billboard.customDirection()[0]);
            out[1] = particle.getEnvironment().safeResolve(billboard.customDirection()[1]);
            out[2] = particle.getEnvironment().safeResolve(billboard.customDirection()[2]);
        } else {
            float speed = (float) Math.sqrt(particle.getVx() * particle.getVx()
                + particle.getVy() * particle.getVy()
                + particle.getVz() * particle.getVz());
            float threshold = billboard != null ? billboard.minSpeedThreshold() : 0.0f;
            if (speed <= threshold) {
                return false;
            }
            out[0] = (float) particle.getVx();
            out[1] = (float) particle.getVy();
            out[2] = (float) particle.getVz();
        }
        if (out[0] == 0.0f && out[1] == 0.0f && out[2] == 0.0f) {
            return false;
        }
        BedrockParticleSystem.normalize(out);
        return true;
    }

    private boolean applyDirectionAsNormal(float[] direction) {
        tempAxisZ[0] = direction[0];
        tempAxisZ[1] = direction[1];
        tempAxisZ[2] = direction[2];
        float[] up = tempAxisY;
        up[0] = 0.0f;
        up[1] = 1.0f;
        up[2] = 0.0f;
        if (Math.abs(BedrockParticleSystem.dot(tempAxisZ, up)) > 0.99f) {
            up[0] = 1.0f;
            up[1] = 0.0f;
            up[2] = 0.0f;
        }
        BedrockParticleSystem.cross(up, tempAxisZ, tempAxisX);
        BedrockParticleSystem.normalize(tempAxisX);
        BedrockParticleSystem.cross(tempAxisZ, tempAxisX, tempAxisY);
        BedrockParticleSystem.normalize(tempAxisY);
        BedrockParticleSystem.normalize(tempAxisZ);
        BedrockParticleSystem.multMatrix(tempAxisX, tempAxisY, tempAxisZ);
        return true;
    }

    private int toColor(float value) {
        return Math.min(255, Math.max(0, (int) (value * 255.0f)));
    }

    private float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private void applyBlendMode() {
        if (blendMode == BedrockParticleSystem.BlendMode.OPAQUE) {
            GlStateManager.disableBlend();
            return;
        }
        GlStateManager.enableBlend();
        if (blendMode == BedrockParticleSystem.BlendMode.ADD) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }

    private void resetBlendMode() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
}
