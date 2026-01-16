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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.glow.GlowRenderer;


public final class WeaponTrailRenderer {
    private final List<WeaponTrailClip> queue = new ArrayList<>();
    private final List<WeaponTrailClip> bloomQueue = new ArrayList<>();
    private final Map<WeaponTrailClip, List<TrailVertex>> bloomVertexCache = new HashMap<>();
    private long lastBloomVertexLog;
    
    public void beginFrame() {
        queue.clear();
        bloomQueue.clear();
        bloomVertexCache.clear();
    }
    
    public void queueClip(WeaponTrailClip clip) {
        if (clip == null || !clip.hasRenderableGeometry()) {
            return;
        }
        queue.add(clip);
        if (clip.isBloomEnabled()) {
            bloomQueue.add(clip);
        }
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
            drawClip(mc, clip, camX, camY, camZ, false);
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        renderBloomPass(mc, partialTicks, camX, camY, camZ);
        queue.clear();
        bloomQueue.clear();
    }
    
    private void drawClip(Minecraft mc,
                          WeaponTrailClip clip,
                          double camX,
                          double camY,
                          double camZ,
                          boolean bloomPass) {
        ResourceLocation texture = clip.getTexture();
        if (!clip.hasRenderableGeometry()) {
            return;
        }
        if (!bloomPass) {
            if (clip.getBlendMode() == TrailBlendMode.ADD) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
        }
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(texture != null ? texture : TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
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
            
            addVertex(buffer, first, camX, camY, camZ, currentU, vFirst, clip, sample, bloomPass);
            addVertex(buffer, second, camX, camY, camZ, currentU, vSecond, clip, sample, bloomPass);
            
            prevCenterX = centerX;
            prevCenterY = centerY;
            prevCenterZ = centerZ;
            hasPrevCenter = true;
        }
        tessellator.draw();
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
                           boolean bloomPass) {
        float alpha = clip.computeAlpha(sample);
        buffer.pos(position.x - camX, position.y - camY, position.z - camZ)
                .tex(u, v)
                .color(clip.getColorR(), clip.getColorG(), clip.getColorB(), alpha)
                .endVertex();
        if (!bloomPass && clip.isBloomEnabled()) {
            bloomVertexCache
                    .computeIfAbsent(clip, key -> new ArrayList<>(clip.getSamples().size() * 2))
                    .add(new TrailVertex(position.x, position.y, position.z, u, v, alpha));
        }
    }
    
    private void renderBloomPass(Minecraft mc,
                                 float partialTicks,
                                 double camX,
                                 double camY,
                                 double camZ) {
        if (bloomQueue.isEmpty()) {
            return;
        }
        Entity view = mc.getRenderViewEntity();
        if (view == null) {
            view = mc.player;
        }
        if (view == null) {
            return;
        }
        Entity finalView = (Entity) view;
        for (WeaponTrailClip clip : bloomQueue) {
            if (!clip.isBloomEnabled()) {
                continue;
            }
            float strength = Math.max(clip.getBloomIntensity(), 0.05f);
            GlowRenderer.INSTANCE.renderCustomMask(finalView, partialTicks, strength, () -> {
                GlStateManager.disableCull();
                GlStateManager.enableTexture2D();
                drawBloomClip(mc, clip, camX, camY, camZ);
            });
        }
    }
    
    private boolean drawBloomClip(Minecraft mc,
                                  WeaponTrailClip clip,
                                  double camX,
                                  double camY,
                                  double camZ) {
        List<TrailVertex> vertices = bloomVertexCache.get(clip);
        if (vertices == null || vertices.isEmpty()) {
            logBloomMissingVertices(clip);
            return false;
        }
        ResourceLocation texture = clip.getTexture();
        mc.getTextureManager().bindTexture(texture != null ? texture : TextureMap.LOCATION_BLOCKS_TEXTURE);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (TrailVertex vertex : vertices) {
            buffer.pos(vertex.x - camX, vertex.y - camY, vertex.z - camZ)
                    .tex(vertex.u, vertex.v)
                    .color(clip.getColorR(), clip.getColorG(), clip.getColorB(), vertex.alpha)
                    .endVertex();
        }
        tessellator.draw();
        return true;
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
    
    private void logBloomMissingVertices(WeaponTrailClip clip) {
        long now = System.currentTimeMillis();
        if (now - lastBloomVertexLog < 1000L) {
            return;
        }
        lastBloomVertexLog = now;
        SkyCoreMod.LOGGER.info("[TrailBloom] Missing cached vertices for clip {}", clip.getId());
    }
    
    private static final class TrailVertex {
        final double x;
        final double y;
        final double z;
        final float u;
        final float v;
        final float alpha;
        
        TrailVertex(double x, double y, double z, float u, float v, float alpha) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
            this.alpha = alpha;
        }
    }
    
}
