package org.mybad.minecraft.common.indicator;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.ShaderManager;

public class IndicatorRenderer2 extends IndicatorRenderer3 {
    private double baseWidth;
    private IndicatorValue distanceValue;

    public void setBaseWidth(double width) {
        this.baseWidth = width;
    }

    public void setDistanceValue(IndicatorValue value) {
        this.distanceValue = value;
    }

    @Override
    public void renderScaled(float scale) {
        double halfLength = this.distanceValue.computeValue();
        double halfWidth = this.baseWidth;
        if (halfLength == 0.0d || halfWidth == 0.0d) {
            return;
        }
        double actualLength = Math.abs(halfLength) * 2.0d;
        double actualWidth = Math.abs(halfWidth) * 2.0d;
        double maxExtent = Math.max(actualLength, actualWidth);
        if (maxExtent <= 0.0d) {
            return;
        }

        float forwardOffset = (float) (actualLength * 0.5d);
        float baseScaleX = (float) (actualWidth / (2.0d * maxExtent));
        float baseScaleZ = (float) (actualLength / (2.0d * maxExtent));
        float widthPulse = Math.max(0.15f, Math.min(scale, 1.0f));
        float appliedScaleX = baseScaleX * widthPulse;
        if (Math.abs(appliedScaleX) < 1.0E-6f || Math.abs(baseScaleZ) < 1.0E-6f) {
            return;
        }

        GlStateManager.translate(0.0f, 0.0f, forwardOffset);
        GlStateManager.scale(appliedScaleX, 1.0f, baseScaleZ);
        IndicatorRendererEvent.RECTANGLE_SHADER.getShaderUniformOrDefault("width").set(1.0f);
        IndicatorRendererEvent.RECTANGLE_SHADER.getShaderUniformOrDefault("height").set(1.0f);
        IndicatorRendererEvent.RECTANGLE_SHADER.useShader();
        IndicatorRendererEvent.renderParticle((float) maxExtent);
        IndicatorRendererEvent.RECTANGLE_SHADER.endShader();
        GlStateManager.scale(1.0f / appliedScaleX, 1.0f, 1.0f / baseScaleZ);
        GlStateManager.translate(0.0f, 0.0f, -forwardOffset);
    }

    @Override
    public ShaderManager getShader() {
        return IndicatorRendererEvent.RECTANGLE_SHADER;
    }
}
