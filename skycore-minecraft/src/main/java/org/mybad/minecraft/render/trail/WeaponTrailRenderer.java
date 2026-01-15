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
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        for (WeaponTrailClip clip : queue) {
            drawClip(mc, clip, camX, camY, camZ);
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
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
        if (clip.isSolidColor()) {
            GlStateManager.disableTexture2D();
        } else {
            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(texture != null ? texture : TextureMap.LOCATION_BLOCKS_TEXTURE);
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_TEX_COLOR);
        float u = clip.getUvOffset();
        Vec3d previous = null;
        for (WeaponTrailClip.TrailSample sample : clip.getSamples()) {
            if (previous != null) {
                double len = previous.distanceTo(sample.start);
                if (Double.isFinite(len)) {
                    u += len * clip.getUvSpeed();
                }
            }
            addVertex(buffer, sample.start, camX, camY, camZ, u, 0f, clip, sample);
            addVertex(buffer, sample.end, camX, camY, camZ, u, 1f, clip, sample);
            previous = sample.start;
        }
        tessellator.draw();
        if (clip.isSolidColor()) {
            GlStateManager.enableTexture2D();
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
                           WeaponTrailClip.TrailSample sample) {
        float alpha = clip.computeAlpha(sample);
        buffer.pos(position.x - camX, position.y - camY, position.z - camZ)
            .tex(u, v)
            .color(clip.getColorR(), clip.getColorG(), clip.getColorB(), alpha)
            .endVertex();
    }

}
