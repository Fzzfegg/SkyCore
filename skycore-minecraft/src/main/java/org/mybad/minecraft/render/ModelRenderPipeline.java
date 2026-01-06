package org.mybad.minecraft.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.mybad.minecraft.render.skinning.SkinningPipeline;
import org.mybad.minecraft.render.ModelBlendMode;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

final class ModelRenderPipeline {
    void render(Entity entity,
                double x, double y, double z,
                float entityYaw, float partialTicks,
                float modelScale,
                boolean enableCull,
                ResourceLocation texture,
                ResourceLocation emissiveTexture,
                float emissiveStrength,
                ResourceLocation bloomTexture,
                float bloomStrength,
                int bloomRadius,
                int bloomDownsample,
                float bloomThreshold,
                int bloomPasses,
                float bloomSpread,
                boolean bloomUseDepth,
                boolean renderHurtTint,
                float hurtTintR,
                float hurtTintG,
                float hurtTintB,
                float hurtTintA,
                ResourceLocation blendTexture,
                ModelBlendMode blendMode,
                float blendR,
                float blendG,
                float blendB,
                float blendA,
                SkinningPipeline skinningPipeline) {
        if (skinningPipeline == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        if (enableCull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.enableColorMaterial();

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        if (modelScale != 1.0f) {
            GlStateManager.scale(modelScale, modelScale, modelScale);
        }

        if (entity != null) {
            GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        }

        if (renderHurtTint && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.hurtTime > 0 || living.deathTime > 0) {
                GlStateManager.color(hurtTintR, hurtTintG, hurtTintB, hurtTintA);
            } else {
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            }
        } else {
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }

        int lightX = (int) OpenGlHelper.lastBrightnessX;
        int lightY = (int) OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);

        boolean gpu = skinningPipeline.ensureGpuSkinningReady();
        if (!gpu) {
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            GlStateManager.disableRescaleNormal();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            return;
        }

        skinningPipeline.updateBoneMatrices();
        skinningPipeline.runSkinningPass();
        skinningPipeline.draw();

        if (emissiveTexture != null) {
            renderEmissivePass(emissiveTexture, emissiveStrength, lightX, lightY, skinningPipeline, texture);
        }
        if (blendTexture != null && blendA > 0.0f) {
            renderBlendPass(blendTexture, blendMode, blendR, blendG, blendB, blendA, lightX, lightY, skinningPipeline, texture);
        }
        if (bloomTexture != null) {
            BloomRenderer.get().renderBloomMask(entity, partialTicks, bloomTexture, bloomStrength, bloomRadius, bloomDownsample, bloomThreshold, bloomPasses, bloomSpread, bloomUseDepth, lightX, lightY, skinningPipeline, texture);
        }

        GlStateManager.popMatrix();

        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
    }

    private void renderEmissivePass(ResourceLocation emissiveTexture,
                                    float emissiveStrength,
                                    int lightX,
                                    int lightY,
                                    SkinningPipeline skinningPipeline,
                                    ResourceLocation baseTexture) {
        if (emissiveStrength <= 0f) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(emissiveTexture);
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

        skinningPipeline.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
    }

    private void renderBlendPass(ResourceLocation blendTexture,
                                 ModelBlendMode blendMode,
                                 float blendR,
                                 float blendG,
                                 float blendB,
                                 float blendA,
                                 int lightX,
                                 int lightY,
                                 SkinningPipeline skinningPipeline,
                                 ResourceLocation baseTexture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(blendTexture);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);

        int fullBright = 240;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) fullBright, (float) fullBright);

        applyMaskTextureEnv(blendR, blendG, blendB, blendA);
        applyBlendMode(blendMode);
        skinningPipeline.draw();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        resetMaskTextureEnv();
        GlStateManager.disableAlpha();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
    }

    private void applyBlendMode(ModelBlendMode mode) {
        if (mode == ModelBlendMode.ADD) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        } else {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
    }

    private static final FloatBuffer MASK_COLOR = BufferUtils.createFloatBuffer(4);

    private void applyMaskTextureEnv(float r, float g, float b, float a) {
        MASK_COLOR.clear();
        MASK_COLOR.put(r).put(g).put(b).put(a).flip();
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, MASK_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_CONSTANT);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
    }

    private void resetMaskTextureEnv() {
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        MASK_COLOR.clear();
        MASK_COLOR.put(1.0f).put(1.0f).put(1.0f).put(1.0f).flip();
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, MASK_COLOR);
    }

    // bloom pass moved to BloomRenderer
}
