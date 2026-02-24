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
        double scaledLength = this.distanceValue.computeValue();
        double scaledWidth = this.baseWidth;
        if (scaledLength != 0.0d && scaledWidth != 0.0d) {
            float forwardOffset = (float) (scale * scaledLength * 0.5d);
            GlStateManager.translate(0.0f, 0.0f, forwardOffset);
            if (scaledLength > scaledWidth) {
                IndicatorRendererEvent.RECTANGLE_SHADER.getShaderUniformOrDefault("height").set(1.0f);
                IndicatorRendererEvent.RECTANGLE_SHADER.getShaderUniformOrDefault("width").set((float) (scaledWidth / scaledLength));
            } else {
                IndicatorRendererEvent.RECTANGLE_SHADER.getShaderUniformOrDefault("width").set(1.0f);
                IndicatorRendererEvent.RECTANGLE_SHADER.getShaderUniformOrDefault("height").set((float) (scaledLength / scaledWidth));
            }
            IndicatorRendererEvent.RECTANGLE_SHADER.useShader();
            IndicatorRendererEvent.renderParticle((float) (scale * Math.max(scaledLength, scaledWidth)));
            IndicatorRendererEvent.RECTANGLE_SHADER.endShader();
            GlStateManager.translate(0.0f, 0.0f, -forwardOffset);
        }
    }

    @Override
    public ShaderManager getShader() {
        return IndicatorRendererEvent.RECTANGLE_SHADER;
    }
}
