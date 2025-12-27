package org.mybad.core.data;

import java.util.*;

/**
 * UV映射信息
 * 支持两种格式：
 * 1. 简单格式 (Box UV)：[u, v] 坐标，根据立方体尺寸自动计算各面UV
 * 2. Per-face格式：每个面单独指定UV映射
 *
 * Bedrock Box UV 布局：
 *          ┌──────┬──────┐
 *          │  up  │ down │   ← v = 0
 *          ├──────┼──────┼──────┬──────┐
 *          │ east │north │ west │south │   ← v = depth
 *          └──────┴──────┴──────┴──────┘   ← v = depth + height
 *          ↑      ↑      ↑      ↑      ↑
 *          0      d    d+w    d+w+d  d+w+d+w
 */
public class UVMapping {
    // 简单格式的基础坐标
    private float[] boxUV;  // [u, v]

    // Per-face格式: 每个面的 UV 区域 [u1, v1, u2, v2]
    public float[] north;  // [u1, v1, u2, v2]
    public float[] south;
    public float[] east;
    public float[] west;
    public float[] up;
    public float[] down;

    // 是否是 per-face 格式
    private boolean isPerFace;

    /**
     * 创建简单UV映射 (Box UV)
     */
    public UVMapping(float u, float v) {
        this.boxUV = new float[]{u, v};
        this.isPerFace = false;
    }

    /**
     * 创建简单UV映射 (Box UV) - int 版本
     */
    public UVMapping(int u, int v) {
        this((float) u, (float) v);
    }

    /**
     * 创建Per-face UV映射
     */
    public UVMapping() {
        this.isPerFace = true;
        this.boxUV = null;
    }

    /**
     * 复制构造函数
     */
    public UVMapping(UVMapping other) {
        this.isPerFace = other.isPerFace;
        if (other.boxUV != null) {
            this.boxUV = new float[]{other.boxUV[0], other.boxUV[1]};
        }
        if (other.north != null) this.north = other.north.clone();
        if (other.south != null) this.south = other.south.clone();
        if (other.east != null) this.east = other.east.clone();
        if (other.west != null) this.west = other.west.clone();
        if (other.up != null) this.up = other.up.clone();
        if (other.down != null) this.down = other.down.clone();
    }

    /**
     * 根据立方体尺寸设置 Box UV
     * 这是 Bedrock 标准的 Box UV 布局
     *
     * @param width  立方体宽度 (X)
     * @param height 立方体高度 (Y)
     * @param depth  立方体深度 (Z)
     * @param mirror 是否镜像
     */
    public void setupBoxUV(float width, float height, float depth, boolean mirror) {
        if (boxUV == null) {
            return;  // 不是 Box UV 模式
        }

        float u = boxUV[0];
        float v = boxUV[1];

        // 取整（Bedrock 使用整数像素）
        float w = (float) Math.floor(width);
        float h = (float) Math.floor(height);
        float d = (float) Math.floor(depth);

        // North face (前面, Z-)
        float nMinX = u + d;
        float nMinY = v + d;
        float nMaxX = nMinX + w;
        float nMaxY = nMinY + h;

        if (mirror) {
            float tmp = nMaxX;
            nMaxX = nMinX;
            nMinX = tmp;
        }
        this.north = new float[]{nMinX, nMinY, nMaxX, nMaxY};

        // East face (右面, X+)
        float eMinX = u;
        float eMinY = v + d;
        float eMaxX = eMinX + d;
        float eMaxY = eMinY + h;

        if (mirror) {
            // 镜像时 east 和 west 交换位置
            eMinX = u + d + w;
            eMaxX = eMinX + d;
            float tmp = eMinX;
            eMinX = eMaxX;
            eMaxX = tmp;
        }
        this.east = new float[]{eMinX, eMinY, eMaxX, eMaxY};

        // South face (后面, Z+)
        float sMinX = u + d * 2 + w;
        float sMinY = v + d;
        float sMaxX = sMinX + w;
        float sMaxY = sMinY + h;

        if (mirror) {
            float tmp = sMaxX;
            sMaxX = sMinX;
            sMinX = tmp;
        }
        this.south = new float[]{sMinX, sMinY, sMaxX, sMaxY};

        // West face (左面, X-)
        float wMinX = u + d + w;
        float wMinY = v + d;
        float wMaxX = wMinX + d;
        float wMaxY = wMinY + h;

        if (mirror) {
            // 镜像时 west 使用原 east 的位置
            wMinX = u;
            wMaxX = wMinX + d;
            float tmp = wMinX;
            wMinX = wMaxX;
            wMaxX = tmp;
        }
        this.west = new float[]{wMinX, wMinY, wMaxX, wMaxY};

        // Up face (上面, Y+)
        float uMinX = u + d;
        float uMinY = v;
        float uMaxX = uMinX + w;
        float uMaxY = uMinY + d;

        if (mirror) {
            float tmp = uMaxX;
            uMaxX = uMinX;
            uMinX = tmp;
        }
        this.up = new float[]{uMinX, uMinY, uMaxX, uMaxY};

        // Down face (下面, Y-)
        float dMinX = u + d + w;
        float dMinY = v + d;  // 注意：down 的 v2 < v1
        float dMaxX = dMinX + w;
        float dMaxY = v;

        if (mirror) {
            float tmp = dMaxX;
            dMaxX = dMinX;
            dMinX = tmp;
        }
        this.down = new float[]{dMinX, dMinY, dMaxX, dMaxY};

        // 设置为 per-face 模式（因为已经计算好了各面）
        this.isPerFace = true;
    }

    /**
     * 设置单个面的 UV（Per-face 模式）
     */
    public void setFaceUV(String face, float u, float v, float uSize, float vSize) {
        float[] uv = new float[]{u, v, u + uSize, v + vSize};
        switch (face.toLowerCase()) {
            case "north": this.north = uv; break;
            case "south": this.south = uv; break;
            case "east": this.east = uv; break;
            case "west": this.west = uv; break;
            case "up": this.up = uv; break;
            case "down": this.down = uv; break;
        }
        this.isPerFace = true;
    }

    /**
     * 设置单个面的 UV - int 版本
     */
    public void setFaceUV(String face, int u, int v, int uSize, int vSize) {
        setFaceUV(face, (float) u, (float) v, (float) uSize, (float) vSize);
    }

    // Getters
    public boolean isPerFace() { return isPerFace; }
    public float[] getBoxUV() { return boxUV; }

    public float[] getNorth() { return north; }
    public float[] getSouth() { return south; }
    public float[] getEast() { return east; }
    public float[] getWest() { return west; }
    public float[] getUp() { return up; }
    public float[] getDown() { return down; }

    /**
     * 获取指定面的 UV
     * @return [u1, v1, u2, v2] 或 null
     */
    public float[] getFaceUV(String face) {
        switch (face.toLowerCase()) {
            case "north": return north;
            case "south": return south;
            case "east": return east;
            case "west": return west;
            case "up": return up;
            case "down": return down;
            default: return null;
        }
    }

    /**
     * 检查是否有指定面的 UV
     */
    public boolean hasFace(String face) {
        return getFaceUV(face) != null;
    }
}
