package org.mybad.minecraft.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.mybad.core.particle.Particle;
import org.mybad.core.particle.ParticleEffect;
import org.mybad.core.particle.render.ParticleRenderer;
import org.mybad.core.particle.render.RenderState;
import org.mybad.minecraft.resource.ResourceLoader;

/**
 * 简易 Minecraft 粒子渲染器
 * 仅实现基础 billboard 渲染
 */
public class MinecraftParticleRenderer implements ParticleRenderer.ParticleProcessor {
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation("textures/particle/particles.png");

    private final ResourceLoader resourceLoader;
    private ResourceLocation currentTexture;
    private float cameraX;
    private float cameraY;
    private float cameraZ;

    public MinecraftParticleRenderer(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setCameraPosition(float x, float y, float z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
    }

    public void begin() {
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }

    public void end() {
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        currentTexture = null;
    }

    @Override
    public void process(Particle particle, RenderState state) {
        if (particle == null || !particle.isAlive()) {
            return;
        }

        ResourceLocation texture = resolveTexture(particle.getEffect());
        if (texture == null) {
            return;
        }
        if (currentTexture == null || !currentTexture.equals(texture)) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            currentTexture = texture;
        }

        double x = particle.getPositionX() - cameraX;
        double y = particle.getPositionY() - cameraY;
        double z = particle.getPositionZ() - cameraZ;

        float scale = particle.getScaleX();
        if (scale <= 0f) {
            return;
        }
        float half = scale * 0.5f;

        float r = particle.getColorR();
        float g = particle.getColorG();
        float b = particle.getColorB();
        float a = particle.getColorA();

        float yaw = Minecraft.getMinecraft().getRenderManager().playerViewY;
        float pitch = Minecraft.getMinecraft().getRenderManager().playerViewX;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-yaw, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(pitch, 1.0f, 0.0f, 0.0f);

        float rotZ = particle.getRotationZ();
        if (rotZ != 0f) {
            GlStateManager.rotate(rotZ, 0.0f, 0.0f, 1.0f);
        }

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-half, -half, 0).tex(0, 1).color(r, g, b, a).endVertex();
        buffer.pos(half, -half, 0).tex(1, 1).color(r, g, b, a).endVertex();
        buffer.pos(half, half, 0).tex(1, 0).color(r, g, b, a).endVertex();
        buffer.pos(-half, half, 0).tex(0, 0).color(r, g, b, a).endVertex();
        Tessellator.getInstance().draw();
        GlStateManager.popMatrix();
    }

    private ResourceLocation resolveTexture(ParticleEffect effect) {
        if (effect == null) {
            return DEFAULT_TEXTURE;
        }
        String textureFile = effect.getTextureFile();
        if (textureFile == null || textureFile.isEmpty()) {
            return DEFAULT_TEXTURE;
        }
        return resourceLoader.getResourceLocation(textureFile);
    }
}
