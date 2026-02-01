package org.mybad.minecraft.common.indicator;


import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.ShaderManager;

import java.util.UUID;

public abstract class IndicatorRenderer3 {
    public static final int DEFAULT_GROW_DURATION_MS = 490;

    public final long startTimeMs = System.currentTimeMillis();
    public float colorR;
    public float colorG;
    public float colorB;
    public int lifetimeMs;
    private int growDurationMs = DEFAULT_GROW_DURATION_MS;
    public IndicatorValue xValue;
    public IndicatorValue yValue;
    public IndicatorValue zValue;
    public IndicatorValue yawValue;

    public abstract void renderScaled(float scale);

    public abstract ShaderManager getShader();

    public void bindToEntity(UUID uuid) {
        this.xValue.setEntityA(uuid);
        this.yValue.setEntityA(uuid);
        this.zValue.setEntityA(uuid);
    }

    public void setXValue(IndicatorValue value) {
        this.xValue = value;
    }

    public void setYValue(IndicatorValue value) {
        this.yValue = value;
    }

    public void setZValue(IndicatorValue value) {
        this.zValue = value;
    }

    public void setYawValue(IndicatorValue value) {
        this.yawValue = value;
    }

    public void setColorR(int r) {
        this.colorR = r / 255.0f;
    }

    public void setColorG(int g) {
        this.colorG = g / 255.0f;
    }

    public void setColorB(int b) {
        this.colorB = b / 255.0f;
    }

    public void setLifetime(int lifetimeMs) {
        this.lifetimeMs = lifetimeMs;
    }

    public void setGrowDurationMs(int durationMs) {
        if (durationMs <= 0) {
            this.growDurationMs = DEFAULT_GROW_DURATION_MS;
        } else {
            this.growDurationMs = durationMs;
        }
    }

    public int getGrowDurationMs() {
        return this.growDurationMs;
    }

    public void applyColor(float alpha) {
        getShader().getShaderUniformOrDefault("RColor").set(this.colorR, this.colorG, this.colorB, alpha);
    }

    public void applyTransform() {
        double camX = IndicatorRendererEvent.RENDER_MANAGER.viewerPosX;
        double camY = IndicatorRendererEvent.RENDER_MANAGER.viewerPosY;
        double camZ = IndicatorRendererEvent.RENDER_MANAGER.viewerPosZ;
        double worldX = this.xValue != null ? this.xValue.computeValue() : 0.0d;
        double worldY = this.yValue != null ? this.yValue.computeValue() : 0.0d;
        double worldZ = this.zValue != null ? this.zValue.computeValue() : 0.0d;
        double yaw = this.yawValue != null ? this.yawValue.computeValue() : 0.0d;

        double tx = worldX - camX;
        double ty = (worldY - camY) + 0.02d;
        double tz = worldZ - camZ;

        GlStateManager.translate(tx, ty, tz);
        GlStateManager.rotate((float) (-yaw), 0.0f, 1.0f, 0.0f);
    }
}
