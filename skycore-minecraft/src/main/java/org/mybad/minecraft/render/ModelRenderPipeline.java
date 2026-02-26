package org.mybad.minecraft.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.config.EntityModelMapping;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.mybad.minecraft.render.skinning.SkinningPipeline;
import org.mybad.minecraft.render.ModelBlendMode;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

final class ModelRenderPipeline {
    private static final int DEFAULT_BLOOM_PASSES = 5;
    private static final float DEFAULT_BLOOM_SCALE_STEP = 0.06f;
    private static final float DEFAULT_BLOOM_DOWNSCALE = 1.0f;

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
                int[] bloomColor,
                int bloomPasses,
                float bloomScaleStep,
                float bloomDownscale,
                float[] bloomOffset,
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
                float modelOffsetX,
                float modelOffsetY,
                float modelOffsetZ,
                int modelOffsetMode,
                SkinningPipeline skinningPipeline,
                boolean applyYaw,
                boolean billboardMode,
                float billboardPitch,
                boolean lightning,
                int packedLightOverride) {
        if (skinningPipeline == null) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        final float prevLightX = OpenGlHelper.lastBrightnessX;
        final float prevLightY = OpenGlHelper.lastBrightnessY;
        float baseLightX = prevLightX;
        float baseLightY = prevLightY;
        int resolvedPackedLight = -1;
        if (entity != null) {
            resolvedPackedLight = entity.getBrightnessForRender();
        } else if (packedLightOverride >= 0) {
            resolvedPackedLight = packedLightOverride;
        }
        if (lightning && resolvedPackedLight >= 0) {
            baseLightX = resolvedPackedLight & 0xFFFF;
            baseLightY = (resolvedPackedLight >> 16) & 0xFFFF;
        } else if (!lightning) {
            baseLightX = 240f;
            baseLightY = 240f;
        }
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, baseLightX, baseLightY);

        final boolean prevLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        final boolean prevColorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
        boolean appliedStandardLighting = false;

        if (enableCull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableTexture2D();
        if (lightning) {
            if (!prevLighting) {
                GlStateManager.enableLighting();
            }
            if (!prevColorMaterial) {
                GlStateManager.enableColorMaterial();
            }
            RenderHelper.enableStandardItemLighting();
            appliedStandardLighting = true;
        } else {
            GlStateManager.disableLighting();
            GlStateManager.disableColorMaterial();
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        applyModelOffset(modelOffsetX, modelOffsetY, modelOffsetZ, modelOffsetMode, entityYaw);
        if (modelScale != 1.0f) {
            GlStateManager.scale(modelScale, modelScale, modelScale);
        }

        if (applyYaw) {
            GlStateManager.rotate(180.0F - entityYaw, 0.0F, 1.0F, 0.0F);
        }
        if (billboardMode) {
            GlStateManager.rotate(billboardPitch, 1.0F, 0.0F, 0.0F);
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

        int lightX = (int) baseLightX;
        int lightY = (int) baseLightY;

        boolean gpu = skinningPipeline.ensureGpuSkinningReady();
        if (!gpu) {
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            GlStateManager.disableRescaleNormal();
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            if (appliedStandardLighting) {
                RenderHelper.disableStandardItemLighting();
            }
            restoreLightingState(prevLighting, prevColorMaterial);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightX, prevLightY);
            return;
        }

        skinningPipeline.updateBoneMatrices();
        skinningPipeline.runSkinningPass();
        skinningPipeline.draw();

        if (bloomStrength > 0f && bloomTexture != null) {
            int passes = bloomPasses > 0 ? bloomPasses : DEFAULT_BLOOM_PASSES;
            float scaleStep = bloomScaleStep > 0f ? bloomScaleStep : DEFAULT_BLOOM_SCALE_STEP;
            float downscale = bloomDownscale > 0f ? bloomDownscale : DEFAULT_BLOOM_DOWNSCALE;
            renderPseudoBloomPass(bloomTexture,
                bloomStrength,
                bloomColor,
                passes,
                scaleStep,
                downscale,
                bloomOffset,
                lightX,
                lightY,
                skinningPipeline,
                texture);
        }

        if (emissiveTexture != null) {
            renderEmissivePass(emissiveTexture, emissiveStrength, lightX, lightY, skinningPipeline, texture);
        }
        if (blendTexture != null && blendA > 0.0f) {
            renderBlendPass(blendTexture, blendMode, blendR, blendG, blendB, blendA, lightX, lightY, skinningPipeline, texture);
        }
        GlStateManager.popMatrix();

        if (appliedStandardLighting) {
            RenderHelper.disableStandardItemLighting();
        }
        GlStateManager.disableBlend();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        restoreLightingState(prevLighting, prevColorMaterial);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLightX, prevLightY);
    }

    private static void restoreLightingState(boolean prevLighting, boolean prevColorMaterial) {
        if (prevColorMaterial) {
            GlStateManager.enableColorMaterial();
        } else {
            GlStateManager.disableColorMaterial();
        }
        if (prevLighting) {
            GlStateManager.enableLighting();
        } else {
            GlStateManager.disableLighting();
        }
    }

    private void applyModelOffset(float offsetX, float offsetY, float offsetZ, int mode, float entityYaw) {
        if (offsetX == 0f && offsetY == 0f && offsetZ == 0f) {
            return;
        }
        if (mode == EntityModelMapping.OFFSET_MODE_LOCAL) {
            double yawRad = Math.toRadians(entityYaw);
            double sin = Math.sin(yawRad);
            double cos = Math.cos(yawRad);
            double worldX = (-cos * offsetX) + (-sin * offsetZ);
            double worldZ = (-sin * offsetX) + (cos * offsetZ);
            GlStateManager.translate(worldX, offsetY, worldZ);
            return;
        }
        GlStateManager.translate(offsetX, offsetY, offsetZ);
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
        pushDepthOffset();
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
        popDepthOffset();
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
        pushDepthOffset();

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
        popDepthOffset();
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

    private void renderPseudoBloomPass(ResourceLocation bloomTexture,
                                       float bloomStrength,
                                       int[] bloomColor,
                                       int bloomPasses,
                                       float bloomScaleStep,
                                       float bloomDownscale,
                                       float[] bloomOffset,
                                       int lightX,
                                       int lightY,
                                       SkinningPipeline skinningPipeline,
                                       ResourceLocation baseTexture) {
        float effectiveStrength = Math.max(0f, bloomStrength);
        if (effectiveStrength <= 0f || bloomTexture == null) {
            return;
        }

        float[] color = decodeBloomColor(bloomColor);
        float colorAlpha = color[3];
        if (colorAlpha > 0f) {
            effectiveStrength *= colorAlpha;
        }

        pushDepthOffset();
        Minecraft.getMinecraft().getTextureManager().bindTexture(bloomTexture);
        GlStateManager.disableLighting();
        GlStateManager.disableColorMaterial();
        boolean fogEnabled = GL11.glIsEnabled(GL11.GL_FOG);
        if (fogEnabled) {
            GlStateManager.disableFog();
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        GlStateManager.depthMask(false);
        GlStateManager.colorMask(true, true, true, false);

        int fullBright = 240;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, fullBright, fullBright);

        float offsetX = 0f;
        float offsetY = 0f;
        float offsetZ = 0f;
        boolean hasOffset = bloomOffset != null && bloomOffset.length >= 3;
        if (hasOffset) {
            offsetX = sanitizeOffsetComponent(bloomOffset[0]);
            offsetY = sanitizeOffsetComponent(bloomOffset[1]);
            offsetZ = sanitizeOffsetComponent(bloomOffset[2]);
            hasOffset = (offsetX != 0f || offsetY != 0f || offsetZ != 0f);
        }

        for (int pass = 0; pass < bloomPasses; pass++) {
            float passIntensity = effectiveStrength / (pass + 1f);
            if (passIntensity <= 0.0001f) {
                continue;
            }
            float scale = 1.0f + ((pass + 1f) * bloomScaleStep) / Math.max(0.001f, bloomDownscale);
            float r = clampColor(color[0] * passIntensity);
            float g = clampColor(color[1] * passIntensity);
            float b = clampColor(color[2] * passIntensity);
            GlStateManager.pushMatrix();
            if (hasOffset) {
                GlStateManager.translate(offsetX, offsetY, offsetZ);
            }
            GlStateManager.scale(scale, scale, scale);
            GlStateManager.color(r, g, b, 1.0f);
            skinningPipeline.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.depthMask(true);
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();
        if (fogEnabled) {
            GlStateManager.enableFog();
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
        Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
        popDepthOffset();
    }

    private float[] decodeBloomColor(int[] rgba) {
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;
        if (rgba != null) {
            if (rgba.length >= 3) {
                r = clampColorComponent(rgba[0]);
                g = clampColorComponent(rgba[1]);
                b = clampColorComponent(rgba[2]);
            }
            if (rgba.length >= 4) {
                a = clampColorComponent(rgba[3]);
            }
        }
        return new float[]{r, g, b, a};
    }

    private float clampColorComponent(int value) {
        if (value <= 0) {
            return 0f;
        }
        if (value >= 255) {
            return 1.0f;
        }
        return value / 255.0f;
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

    private float sanitizeOffsetComponent(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        return value;
    }

    private void pushDepthOffset() {
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1f, -10f);
    }

    private void popDepthOffset() {
        GlStateManager.doPolygonOffset(0f, 0f);
        GlStateManager.disablePolygonOffset();
    }

}
