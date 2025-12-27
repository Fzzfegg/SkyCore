package org.mybad.core.particle.render;

import java.util.*;

/**
 * 渲染状态 - 保存渲染过程中的状态信息
 */
public class RenderState {

    private String currentMaterial;
    private String currentTexture;

    // 渲染配置
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];

    // 光照和环境
    private float ambientLight = 0.5f;
    private float directionalLight = 1.0f;

    // 混合模式
    private BlendMode blendMode = BlendMode.ALPHA;

    // 统计
    private int drawCalls = 0;
    private int vertexCount = 0;

    public RenderState() {
        // 初始化矩阵为单位矩阵
        initializeMatrices();
    }

    /**
     * 初始化矩阵
     */
    private void initializeMatrices() {
        // 单位矩阵
        for (int i = 0; i < 16; i++) {
            viewMatrix[i] = 0;
            projectionMatrix[i] = 0;
        }
        for (int i = 0; i < 4; i++) {
            viewMatrix[i * 4 + i] = 1;
            projectionMatrix[i * 4 + i] = 1;
        }
    }

    /**
     * 设置视图矩阵
     */
    public void setViewMatrix(float[] matrix) {
        if (matrix.length == 16) {
            System.arraycopy(matrix, 0, viewMatrix, 0, 16);
        }
    }

    /**
     * 设置投影矩阵
     */
    public void setProjectionMatrix(float[] matrix) {
        if (matrix.length == 16) {
            System.arraycopy(matrix, 0, projectionMatrix, 0, 16);
        }
    }

    /**
     * 设置材质
     */
    public void setMaterial(String material) {
        if (!Objects.equals(currentMaterial, material)) {
            currentMaterial = material;
            drawCalls++;
        }
    }

    /**
     * 设置纹理
     */
    public void setTexture(String texture) {
        if (!Objects.equals(currentTexture, texture)) {
            currentTexture = texture;
            drawCalls++;
        }
    }

    /**
     * 设置混合模式
     */
    public void setBlendMode(BlendMode mode) {
        this.blendMode = mode;
    }

    /**
     * 增加顶点计数
     */
    public void addVertices(int count) {
        this.vertexCount += count;
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        drawCalls = 0;
        vertexCount = 0;
    }

    // Getters
    public String getCurrentMaterial() { return currentMaterial; }
    public String getCurrentTexture() { return currentTexture; }
    public float[] getViewMatrix() { return viewMatrix; }
    public float[] getProjectionMatrix() { return projectionMatrix; }
    public float getAmbientLight() { return ambientLight; }
    public float getDirectionalLight() { return directionalLight; }
    public BlendMode getBlendMode() { return blendMode; }
    public int getDrawCalls() { return drawCalls; }
    public int getVertexCount() { return vertexCount; }

    /**
     * 混合模式
     */
    public enum BlendMode {
        OPAQUE,
        ALPHA,
        ADD,
        MULTIPLY,
        SCREEN
    }

    @Override
    public String toString() {
        return String.format("RenderState [Material: %s, Texture: %s, DrawCalls: %d, Vertices: %d]",
                currentMaterial, currentTexture, drawCalls, vertexCount);
    }
}
