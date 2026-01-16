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

/**
 * Collects trail clips during entity rendering and draws them in RenderWorldLast.
 */
public final class WeaponTrailRenderer {
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
        GlStateManager.enableAlpha();
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
        GlStateManager.disableAlpha();
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
        Vec3d previous = null;
        double traveled = 0.0;
        double prevCenterX = 0.0;
        double prevCenterY = 0.0;
        double prevCenterZ = 0.0;
        boolean hasPrevCenter = false;
        for (WeaponTrailClip.TrailSample sample : clip.getSamples()) {
            float currentU;
            if (stretchUv) {
                double centerX = (sample.start.x + sample.end.x) * 0.5;
                double centerY = (sample.start.y + sample.end.y) * 0.5;
                double centerZ = (sample.start.z + sample.end.z) * 0.5;
                if (hasPrevCenter) {
                    double dx = centerX - prevCenterX;
                    double dy = centerY - prevCenterY;
                    double dz = centerZ - prevCenterZ;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (Double.isFinite(len)) {
                        traveled += len;
                    }
                }
                currentU = baseOffset;
                if (totalLength > 0.0) {
                    currentU += (float) ((traveled / totalLength) * uvScale);
                }
                prevCenterX = centerX;
                prevCenterY = centerY;
                prevCenterZ = centerZ;
                hasPrevCenter = true;
            } else {
                if (previous != null) {
                    double len = previous.distanceTo(sample.start);
                    if (Double.isFinite(len)) {
                        tiledU += len * uvScale;
                    }
                }
                currentU = tiledU;
            }
            addVertex(buffer, sample.start, camX, camY, camZ, currentU, 0f, clip, sample);
            addVertex(buffer, sample.end, camX, camY, camZ, currentU, 1f, clip, sample);
            previous = sample.start;
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
                           WeaponTrailClip.TrailSample sample) {
        float alpha = clip.computeAlpha(sample);
        buffer.pos(position.x - camX, position.y - camY, position.z - camZ)
            .tex(u, v)
            .color(clip.getColorR(), clip.getColorG(), clip.getColorB(), alpha)
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

}
