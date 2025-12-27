package org.mybad.core.data;

/**
 * 基岩模型定位器
 * 代表模型中的特定位置，通常用于：
 * - 附着点（武器、工具、配饰）
 * - 粒子生成点
 * - 声音源位置
 * - 动画事件触发位置
 */
public class ModelLocator {
    private String name;
    private float[] position;       // 相对于附着骨骼的位置 [x, y, z]
    private String attachedBone;    // 附着的骨骼名称
    private float[] rotation;       // 定位器旋转（可选）[x, y, z]
    private boolean visible;        // 是否在编辑器中可见

    public ModelLocator(String name) {
        this.name = name;
        this.position = new float[]{0, 0, 0};
        this.rotation = new float[]{0, 0, 0};
        this.visible = true;
    }

    public ModelLocator(String name, float x, float y, float z) {
        this.name = name;
        this.position = new float[]{x, y, z};
        this.rotation = new float[]{0, 0, 0};
        this.visible = true;
    }

    /**
     * 获取世界坐标（需要通过骨骼变换计算）
     * 这里只返回相对位置，实际世界坐标需要在渲染时计算
     */
    public float[] getRelativePosition() {
        return position;
    }

    // Getters
    public String getName() { return name; }
    public float[] getPosition() { return position; }
    public String getAttachedBone() { return attachedBone; }
    public float[] getRotation() { return rotation; }
    public boolean isVisible() { return visible; }

    // Setters
    public void setPosition(float x, float y, float z) {
        this.position[0] = x;
        this.position[1] = y;
        this.position[2] = z;
    }

    public void setPosition(float[] position) {
        this.position = position;
    }

    public void setAttachedBone(String boneName) {
        this.attachedBone = boneName;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation[0] = x;
        this.rotation[1] = y;
        this.rotation[2] = z;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
