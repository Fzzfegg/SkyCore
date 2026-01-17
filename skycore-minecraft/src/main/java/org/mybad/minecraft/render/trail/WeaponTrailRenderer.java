package org.mybad.minecraft.render.trail;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.texture.TextureMap;

import java.util.ArrayList;
import java.util.List;

import org.mybad.minecraft.SkyCoreMod;


public final class WeaponTrailRenderer {
    private static final int TRAIL_BLOOM_PASSES = 5;
    private static final float TRAIL_BLOOM_SCALE_STEP = 0.06f;
    private static final float TRAIL_BLOOM_DOWNSCALE = 1.0f;
    private final List<WeaponTrailClip> queue = new ArrayList<>();
    
    public void beginFrame() {
        queue.clear();
    }
    
    public void queueClip(WeaponTrailClip clip) {
        if (clip == null || !clip.hasRenderableGeometry()) {
            return;
        }
        queue.add(clip);
    }
    
    public void render(float partialTicks) {
        if (queue.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getRenderManager() == null) {
            queue.clear();
            return;
        }
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        for (WeaponTrailClip clip : queue) {
            drawClip(mc, clip, camX, camY, camZ);
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        queue.clear();
    }
    
    private void drawClip(Minecraft mc,
                          WeaponTrailClip clip,
                          double camX,
                          double camY,
                          double camZ) {
        ResourceLocation texture = clip.getTexture();
        if (!clip.hasRenderableGeometry()) {
            return;
        }
        if (clip.getBlendMode() == TrailBlendMode.ADD) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(texture != null ? texture : TextureMap.LOCATION_BLOCKS_TEXTURE);
        drawTrailStrip(clip, camX, camY, camZ, 1.0f, 1.0f);

        if (clip.isBloomEnabled()) {
            renderTrailBloom(mc, clip, camX, camY, camZ);
        }
    }
    
    private void drawTrailStrip(WeaponTrailClip clip,
                                double camX,
                                double camY,
                                double camZ,
                                float colorMultiplier,
                                float alphaMultiplier) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
        buildTrailVertices(buffer, clip, camX, camY, camZ, colorMultiplier, alphaMultiplier);
        tessellator.draw();
    }

    private void buildTrailVertices(BufferBuilder buffer,
                                    WeaponTrailClip clip,
                                    double camX,
                                    double camY,
                                    double camZ,
                                    float colorMultiplier,
                                    float alphaMultiplier) {
        boolean stretchUv = clip.isStretchUv();
        double totalLength = stretchUv ? computeTrailLength(clip) : 0.0;
        if (stretchUv && totalLength <= 1.0e-4) {
            stretchUv = false;
        }
        float baseOffset = clip.getUvOffset();
        float uvScale = clip.getUvSpeed();
        float tiledU = baseOffset;
        double traveled = 0.0;
        double prevCenterX = 0.0;
        double prevCenterY = 0.0;
        double prevCenterZ = 0.0;
        boolean hasPrevCenter = false;
        boolean lastFlip = false;
        int flipConfidence = 0;
        for (WeaponTrailClip.TrailSample sample : clip.getSamples()) {
            double centerX = (sample.start.x + sample.end.x) * 0.5;
            double centerY = (sample.start.y + sample.end.y) * 0.5;
            double centerZ = (sample.start.z + sample.end.z) * 0.5;
            Vec3d widthVec = sample.end.subtract(sample.start);
            boolean desiredFlip = shouldFlip(widthVec, centerX, centerY, centerZ,
                    hasPrevCenter, prevCenterX, prevCenterY, prevCenterZ,
                    camX, camY, camZ);
            if (desiredFlip != lastFlip) {
                flipConfidence++;
                if (flipConfidence >= 3) {
                    lastFlip = desiredFlip;
                    flipConfidence = 0;
                }
            } else {
                flipConfidence = 0;
            }
            Vec3d first = lastFlip ? sample.end : sample.start;
            Vec3d second = lastFlip ? sample.start : sample.end;
            float vFirst = lastFlip ? 1f : 0f;
            float vSecond = lastFlip ? 0f : 1f;

            if (hasPrevCenter) {
                double dx = centerX - prevCenterX;
                double dy = centerY - prevCenterY;
                double dz = centerZ - prevCenterZ;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (Double.isFinite(len) && len > 1.0e-5) {
                    traveled += len;
                    if (!stretchUv) {
                        tiledU += (float) (len * uvScale);
                    }
                }
            }
            float currentU;
            if (stretchUv) {
                float normalized = totalLength > 0.0 ? (float) (traveled / totalLength) : 0f;
                if (normalized > 1f) {
                    normalized = 1f;
                } else if (normalized < 0f) {
                    normalized = 0f;
                }
                currentU = baseOffset + normalized;
            } else {
                currentU = tiledU;
            }

            addVertex(buffer, first, camX, camY, camZ, currentU, vFirst, clip, sample, colorMultiplier, alphaMultiplier);
            addVertex(buffer, second, camX, camY, camZ, currentU, vSecond, clip, sample, colorMultiplier, alphaMultiplier);

            prevCenterX = centerX;
            prevCenterY = centerY;
            prevCenterZ = centerZ;
            hasPrevCenter = true;
        }
    }

    private void addVertex(BufferBuilder buffer,
                           Vec3d position,
                           double camX,
                           double camY,
                           double camZ,
                           float u,
                           float v,
                           WeaponTrailClip clip,
                           WeaponTrailClip.TrailSample sample,
                           float colorMultiplier,
                           float alphaMultiplier) {
        float alpha = clip.computeAlpha(sample) * alphaMultiplier;
        if (alpha < 0f) {
            alpha = 0f;
        } else if (alpha > 1f) {
            alpha = 1f;
        }
        float r = clampColor(clip.getColorR() * colorMultiplier);
        float g = clampColor(clip.getColorG() * colorMultiplier);
        float b = clampColor(clip.getColorB() * colorMultiplier);
        buffer.pos(position.x - camX, position.y - camY, position.z - camZ)
                .tex(u, v)
                .color(r, g, b, alpha)
                .endVertex();
    }
    
    private static double computeTrailLength(WeaponTrailClip clip) {
        double total = 0.0;
        double prevCenterX = 0.0;
        double prevCenterY = 0.0;
        double prevCenterZ = 0.0;
        boolean hasPrev = false;
        for (WeaponTrailClip.TrailSample sample : clip.getSamples()) {
            double centerX = (sample.start.x + sample.end.x) * 0.5;
            double centerY = (sample.start.y + sample.end.y) * 0.5;
            double centerZ = (sample.start.z + sample.end.z) * 0.5;
            if (hasPrev) {
                double dx = centerX - prevCenterX;
                double dy = centerY - prevCenterY;
                double dz = centerZ - prevCenterZ;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (Double.isFinite(len)) {
                    total += len;
                }
            }
            prevCenterX = centerX;
            prevCenterY = centerY;
            prevCenterZ = centerZ;
            hasPrev = true;
        }
        return total;
    }

    private void renderTrailBloom(Minecraft mc,
                                  WeaponTrailClip clip,
                                  double camX,
                                  double camY,
                                  double camZ) {
        float bloomIntensity = clip.getBloomIntensity();
        if (bloomIntensity <= 0f) {
            return;
        }

        Vec3d centroid = computeClipCentroid(clip);
        float relX = (float) (centroid.x - camX);
        float relY = (float) (centroid.y - camY);
        float relZ = (float) (centroid.z - camZ);
        int passes = clip.getBloomPasses();
        if (passes <= 0) {
            passes = TRAIL_BLOOM_PASSES;
        }
        float scaleStep = clip.getBloomScaleStep() > 0f ? clip.getBloomScaleStep() : TRAIL_BLOOM_SCALE_STEP;
        float downscale = clip.getBloomDownscale() > 0f ? clip.getBloomDownscale() : TRAIL_BLOOM_DOWNSCALE;

        GlStateManager.pushMatrix();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.colorMask(true, true, true, false);

        ResourceLocation texture = clip.getTexture();
        mc.getTextureManager().bindTexture(texture != null ? texture : TextureMap.LOCATION_BLOCKS_TEXTURE);

        for (int pass = 0; pass < passes; pass++) {
            float scale = 1.0f + ((pass + 1f) * scaleStep) / Math.max(0.001f, downscale);
            float intensity = bloomIntensity / (pass + 1f);
            if (intensity <= 0.0005f) {
                continue;
            }

            GlStateManager.pushMatrix();
            GlStateManager.translate(relX, relY, relZ);
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.translate(-relX, -relY, -relZ);

            drawTrailStrip(clip, camX, camY, camZ, intensity, intensity);

            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.popMatrix();

        if (clip.getBlendMode() == TrailBlendMode.ADD) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }

    private Vec3d computeClipCentroid(WeaponTrailClip clip) {
        double sumX = 0.0;
        double sumY = 0.0;
        double sumZ = 0.0;
        int count = 0;
        for (WeaponTrailClip.TrailSample sample : clip.getSamples()) {
            double centerX = (sample.start.x + sample.end.x) * 0.5;
            double centerY = (sample.start.y + sample.end.y) * 0.5;
            double centerZ = (sample.start.z + sample.end.z) * 0.5;
            if (Double.isFinite(centerX) && Double.isFinite(centerY) && Double.isFinite(centerZ)) {
                sumX += centerX;
                sumY += centerY;
                sumZ += centerZ;
                count++;
            }
        }
        if (count == 0) {
            return new Vec3d(0.0, 0.0, 0.0);
        }
        return new Vec3d(sumX / count, sumY / count, sumZ / count);
    }

    private float clampColor(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 4f) {
            return 4f;
        }
        return value;
    }
    
    private static boolean shouldFlip(Vec3d widthVec,
                                      double centerX,
                                      double centerY,
                                      double centerZ,
                                      boolean hasPrevCenter,
                                      double prevCenterX,
                                      double prevCenterY,
                                      double prevCenterZ,
                                      double camX,
                                      double camY,
                                      double camZ) {
        if (!hasPrevCenter || widthVec == null || widthVec.lengthSquared() < 1.0e-6) {
            return false;
        }
        double tx = centerX - prevCenterX;
        double ty = centerY - prevCenterY;
        double tz = centerZ - prevCenterZ;
        double tangentLenSq = tx * tx + ty * ty + tz * tz;
        if (tangentLenSq < 1.0e-6) {
            return false;
        }
        double vx = centerX - camX;
        double vy = centerY - camY;
        double vz = centerZ - camZ;
        double viewLenSq = vx * vx + vy * vy + vz * vz;
        if (viewLenSq < 1.0e-6) {
            return false;
        }
        Vec3d tangent = new Vec3d(tx, ty, tz);
        Vec3d viewDir = new Vec3d(vx, vy, vz);
        Vec3d reference = tangent.crossProduct(viewDir);
        if (reference.lengthSquared() < 1.0e-7) {
            return false;
        }
        double orientation = reference.dotProduct(widthVec);
        return orientation < 0.0;
    }
    
}
