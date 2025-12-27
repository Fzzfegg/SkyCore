package org.mybad.core.data;

import org.mybad.core.utils.TransformUtils;

/**
 * 根骨骼 - 模型的根节点
 * 管理整个模型的位置、旋转和缩放
 * 用于模型的全局变换和移动
 */
public class RootBone extends ModelBone {

    // 全局变换
    private float globalPositionX;
    private float globalPositionY;
    private float globalPositionZ;

    private float globalRotationX;
    private float globalRotationY;
    private float globalRotationZ;

    private float globalScaleX = 1.0f;
    private float globalScaleY = 1.0f;
    private float globalScaleZ = 1.0f;

    // 根运动相关
    private boolean enableRootMotion;
    private float rootMotionBlend;

    // 变换矩阵缓存
    private float[] transformMatrix;
    private boolean matrixDirty;

    public RootBone(String name) {
        super(name);
        this.globalPositionX = 0;
        this.globalPositionY = 0;
        this.globalPositionZ = 0;
        this.globalRotationX = 0;
        this.globalRotationY = 0;
        this.globalRotationZ = 0;
        this.enableRootMotion = false;
        this.rootMotionBlend = 1.0f;
        this.transformMatrix = new float[16];
        this.matrixDirty = true;
    }

    /**
     * 设置全局位置
     */
    public void setGlobalPosition(float x, float y, float z) {
        this.globalPositionX = x;
        this.globalPositionY = y;
        this.globalPositionZ = z;
        this.matrixDirty = true;
    }

    /**
     * 获取全局位置X
     */
    public float getGlobalPositionX() {
        return globalPositionX;
    }

    /**
     * 获取全局位置Y
     */
    public float getGlobalPositionY() {
        return globalPositionY;
    }

    /**
     * 获取全局位置Z
     */
    public float getGlobalPositionZ() {
        return globalPositionZ;
    }

    /**
     * 设置全局旋转
     */
    public void setGlobalRotation(float rotX, float rotY, float rotZ) {
        this.globalRotationX = rotX;
        this.globalRotationY = rotY;
        this.globalRotationZ = rotZ;
        this.matrixDirty = true;
    }

    /**
     * 获取全局旋转X
     */
    public float getGlobalRotationX() {
        return globalRotationX;
    }

    /**
     * 获取全局旋转Y
     */
    public float getGlobalRotationY() {
        return globalRotationY;
    }

    /**
     * 获取全局旋转Z
     */
    public float getGlobalRotationZ() {
        return globalRotationZ;
    }

    /**
     * 设置全局缩放
     */
    public void setGlobalScale(float scaleX, float scaleY, float scaleZ) {
        this.globalScaleX = scaleX;
        this.globalScaleY = scaleY;
        this.globalScaleZ = scaleZ;
        this.matrixDirty = true;
    }

    /**
     * 获取全局缩放X
     */
    public float getGlobalScaleX() {
        return globalScaleX;
    }

    /**
     * 获取全局缩放Y
     */
    public float getGlobalScaleY() {
        return globalScaleY;
    }

    /**
     * 获取全局缩放Z
     */
    public float getGlobalScaleZ() {
        return globalScaleZ;
    }

    /**
     * 启用根运动
     */
    public void enableRootMotion(boolean enable) {
        this.enableRootMotion = enable;
    }

    /**
     * 是否启用根运动
     */
    public boolean isRootMotionEnabled() {
        return enableRootMotion;
    }

    /**
     * 设置根运动混合权重
     */
    public void setRootMotionBlend(float blend) {
        this.rootMotionBlend = Math.max(0, Math.min(1, blend));
        this.matrixDirty = true;
    }

    /**
     * 获取根运动混合权重
     */
    public float getRootMotionBlend() {
        return rootMotionBlend;
    }

    /**
     * 平移根骨骼
     */
    public void translateGlobal(float dx, float dy, float dz) {
        this.globalPositionX += dx;
        this.globalPositionY += dy;
        this.globalPositionZ += dz;
        this.matrixDirty = true;
    }

    /**
     * 旋转根骨骼
     */
    public void rotateGlobal(float rotX, float rotY, float rotZ) {
        this.globalRotationX += rotX;
        this.globalRotationY += rotY;
        this.globalRotationZ += rotZ;
        this.matrixDirty = true;
    }

    /**
     * 缩放根骨骼
     */
    public void scaleGlobal(float scaleX, float scaleY, float scaleZ) {
        this.globalScaleX *= scaleX;
        this.globalScaleY *= scaleY;
        this.globalScaleZ *= scaleZ;
        this.matrixDirty = true;
    }

    /**
     * 获取变换矩阵（4x4）
     */
    public float[] getTransformMatrix() {
        if (matrixDirty) {
            updateTransformMatrix();
            matrixDirty = false;
        }
        return transformMatrix;
    }

    /**
     * 更新变换矩阵
     */
    private void updateTransformMatrix() {
        // 创建变换矩阵：平移 * 旋转 * 缩放
        TransformUtils.createMatrix(
                transformMatrix,
                globalPositionX,
                globalPositionY,
                globalPositionZ,
                globalRotationX,
                globalRotationY,
                globalRotationZ,
                globalScaleX,
                globalScaleY,
                globalScaleZ
        );
    }

    /**
     * 重置根骨骼
     */
    public void reset() {
        this.globalPositionX = 0;
        this.globalPositionY = 0;
        this.globalPositionZ = 0;
        this.globalRotationX = 0;
        this.globalRotationY = 0;
        this.globalRotationZ = 0;
        this.globalScaleX = 1.0f;
        this.globalScaleY = 1.0f;
        this.globalScaleZ = 1.0f;
        this.rootMotionBlend = 1.0f;
        this.matrixDirty = true;
    }

    @Override
    public String toString() {
        return String.format("RootBone [%s, Position: (%.2f, %.2f, %.2f), Rotation: (%.2f, %.2f, %.2f), RootMotion: %b]",
                getName(), globalPositionX, globalPositionY, globalPositionZ,
                globalRotationX, globalRotationY, globalRotationZ, enableRootMotion);
    }
}
