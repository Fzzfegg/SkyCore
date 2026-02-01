package org.mybad.minecraft.common.indicator;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.ShaderManager;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IndicatorRendererPolygon extends IndicatorRenderer3 {

    private final List<IndicatorVertex> vertices = new ArrayList<>();

    public void setVertices(List<IndicatorVertex> list) {
        this.vertices.clear();
        if (list != null) {
            this.vertices.addAll(list);
        }
    }

    public List<IndicatorVertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    @Override
    public void renderScaled(float scale) {
        if (vertices.size() < 3) {
            return;
        }
        ShaderManager shader = IndicatorRendererEvent.CIRCLE_SHADER;
        if (shader == null) {
            return;
        }
        double centerX = xValue != null ? xValue.computeValue() : 0.0d;
        double centerY = yValue != null ? yValue.computeValue() : 0.0d;
        double centerZ = zValue != null ? zValue.computeValue() : 0.0d;
        shader.getShaderUniformOrDefault("angle").set(0.0f);
        shader.useShader();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
        int count = vertices.size();
        for (int i = 0; i < count; i++) {
            IndicatorVertex vertex = vertices.get(i);
            double vx = (vertex.xValue.computeValue() - centerX) * scale;
            double vy = (vertex.yValue.computeValue() - centerY) * scale;
            double vz = (vertex.zValue.computeValue() - centerZ) * scale;
            float u = (float) i / (float) count;
            float v = u;
            buffer.pos(vx, vy, vz).tex(u, v).endVertex();
        }
        tessellator.draw();
        shader.endShader();
    }

    @Override
    public ShaderManager getShader() {
        return IndicatorRendererEvent.CIRCLE_SHADER;
    }

    public static final class IndicatorVertex {
        final IndicatorValue xValue;
        final IndicatorValue yValue;
        final IndicatorValue zValue;

        public IndicatorVertex(IndicatorValue xValue, IndicatorValue yValue, IndicatorValue zValue) {
            this.xValue = xValue;
            this.yValue = yValue;
            this.zValue = zValue;
        }
    }
}
