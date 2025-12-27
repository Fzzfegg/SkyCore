package org.mybad.core.data;

/**
 * 预计算的顶点数据
 * 包含位置和纹理坐标
 */
public class ModelVertex {
    /** 顶点位置 (已转换为 Minecraft 单位，即除以16) */
    public final float x, y, z;

    /** 纹理坐标 (已归一化到 0-1) */
    public final float u, v;

    public ModelVertex(float x, float y, float z, float u, float v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }

    /**
     * 创建顶点（自动转换坐标）
     * @param x Bedrock 坐标 X
     * @param y Bedrock 坐标 Y
     * @param z Bedrock 坐标 Z
     * @param u 纹理 U (像素)
     * @param v 纹理 V (像素)
     * @param texWidth 纹理宽度
     * @param texHeight 纹理高度
     */
    public static ModelVertex create(float x, float y, float z, float u, float v, int texWidth, int texHeight) {
        return new ModelVertex(
            x / 16.0f,
            y / 16.0f,
            z / 16.0f,
            u / texWidth,
            v / texHeight
        );
    }

    @Override
    public String toString() {
        return String.format("Vertex(%.3f, %.3f, %.3f, uv=%.3f, %.3f)", x, y, z, u, v);
    }
}
