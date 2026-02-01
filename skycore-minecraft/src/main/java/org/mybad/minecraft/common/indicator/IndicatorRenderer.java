package org.mybad.minecraft.common.indicator;

import net.minecraft.client.shader.ShaderManager;

public class IndicatorRenderer extends IndicatorRenderer3 {
    private double radius;
    private float facingNormalized = -1.0f;

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getRadiusForDebug() {
        return radius;
    }

    public void setFacingDegrees(double degrees) {
        if (degrees <= 0.0d || Double.isNaN(degrees)) {
            this.facingNormalized = -1.0f;
            return;
        }
        double normalized = (180.0d - degrees) / 180.0d;
        if (!Double.isFinite(normalized)) {
            this.facingNormalized = -1.0f;
            return;
        }
        if (normalized < -1.0d) {
            normalized = -1.0d;
        } else if (normalized > 1.0d) {
            normalized = 1.0d;
        }
        this.facingNormalized = (float) normalized;
    }

    @Override
    public void renderScaled(float scale) {
        IndicatorRendererEvent.CIRCLE_SHADER.getShaderUniformOrDefault("angle").set(this.facingNormalized);
        IndicatorRendererEvent.CIRCLE_SHADER.useShader();
        IndicatorRendererEvent.renderParticle((float) (this.radius * scale));
        IndicatorRendererEvent.CIRCLE_SHADER.endShader();
    }

    @Override
    public ShaderManager getShader() {
        return IndicatorRendererEvent.CIRCLE_SHADER;
    }
}
