package org.mybad.core.animation;

import org.mybad.core.data.RootBone;

/**
 * 根运动 - 管理根骨骼的动画驱动的变换
 * 根据动画数据驱动模型的整体移动和旋转
 * 用于角色移动、瞄准等动画效果
 */
public class RootMotion {

    private RootBone rootBone;
    private Animation rootAnimation;

    // 运动跟踪
    private float previousPositionX;
    private float previousPositionY;
    private float previousPositionZ;

    private float previousRotationX;
    private float previousRotationY;
    private float previousRotationZ;

    // 运动增量
    private float deltaPositionX;
    private float deltaPositionY;
    private float deltaPositionZ;

    private float deltaRotationX;
    private float deltaRotationY;
    private float deltaRotationZ;

    // 配置
    private boolean enabled;
    private boolean applyPosition = true;
    private boolean applyRotation = true;
    private float positionScale = 1.0f;
    private float rotationScale = 1.0f;

    // 约束
    private float maxLinearVelocity = 10.0f;
    private float maxAngularVelocity = 5.0f;

    public RootMotion(RootBone rootBone) {
        this.rootBone = rootBone;
        this.enabled = false;
        this.previousPositionX = 0;
        this.previousPositionY = 0;
        this.previousPositionZ = 0;
        this.previousRotationX = 0;
        this.previousRotationY = 0;
        this.previousRotationZ = 0;
        this.deltaPositionX = 0;
        this.deltaPositionY = 0;
        this.deltaPositionZ = 0;
        this.deltaRotationX = 0;
        this.deltaRotationY = 0;
        this.deltaRotationZ = 0;
    }

    /**
     * 启用根运动
     */
    public void enable() {
        this.enabled = true;
        this.rootBone.enableRootMotion(true);
    }

    /**
     * 禁用根运动
     */
    public void disable() {
        this.enabled = false;
        this.rootBone.enableRootMotion(false);
    }

    /**
     * 是否启用根运动
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 关联动画
     */
    public void setRootAnimation(Animation animation) {
        this.rootAnimation = animation;
        if (animation != null) {
            // 记录初始位置
            resetPreviousState();
        }
    }

    /**
     * 重置上一帧的状态
     */
    private void resetPreviousState() {
        this.previousPositionX = rootBone.getGlobalPositionX();
        this.previousPositionY = rootBone.getGlobalPositionY();
        this.previousPositionZ = rootBone.getGlobalPositionZ();
        this.previousRotationX = rootBone.getGlobalRotationX();
        this.previousRotationY = rootBone.getGlobalRotationY();
        this.previousRotationZ = rootBone.getGlobalRotationZ();
    }

    /**
     * 更新根运动
     */
    public void update(float deltaTime) {
        if (!enabled || rootBone == null) {
            return;
        }

        // 计算位置增量
        if (applyPosition) {
            deltaPositionX = (rootBone.getGlobalPositionX() - previousPositionX) * positionScale;
            deltaPositionY = (rootBone.getGlobalPositionY() - previousPositionY) * positionScale;
            deltaPositionZ = (rootBone.getGlobalPositionZ() - previousPositionZ) * positionScale;

            // 应用速度约束
            float linearVelocity = (float) Math.sqrt(deltaPositionX * deltaPositionX + deltaPositionY * deltaPositionY + deltaPositionZ * deltaPositionZ) / deltaTime;
            if (linearVelocity > maxLinearVelocity) {
                float scale = maxLinearVelocity / linearVelocity;
                deltaPositionX *= scale;
                deltaPositionY *= scale;
                deltaPositionZ *= scale;
            }
        }

        // 计算旋转增量
        if (applyRotation) {
            deltaRotationX = (rootBone.getGlobalRotationX() - previousRotationX) * rotationScale;
            deltaRotationY = (rootBone.getGlobalRotationY() - previousRotationY) * rotationScale;
            deltaRotationZ = (rootBone.getGlobalRotationZ() - previousRotationZ) * rotationScale;

            // 应用角速度约束
            float angularVelocity = (float) Math.sqrt(deltaRotationX * deltaRotationX + deltaRotationY * deltaRotationY + deltaRotationZ * deltaRotationZ) / deltaTime;
            if (angularVelocity > maxAngularVelocity) {
                float scale = maxAngularVelocity / angularVelocity;
                deltaRotationX *= scale;
                deltaRotationY *= scale;
                deltaRotationZ *= scale;
            }
        }

        // 更新上一帧状态
        resetPreviousState();
    }

    /**
     * 应用根运动到模型
     */
    public void applyMotion() {
        if (!enabled || rootBone == null) {
            return;
        }

        if (applyPosition) {
            rootBone.translateGlobal(deltaPositionX, deltaPositionY, deltaPositionZ);
        }

        if (applyRotation) {
            rootBone.rotateGlobal(deltaRotationX, deltaRotationY, deltaRotationZ);
        }
    }

    /**
     * 设置应用位置变换
     */
    public void setApplyPosition(boolean apply) {
        this.applyPosition = apply;
    }

    /**
     * 设置应用旋转变换
     */
    public void setApplyRotation(boolean apply) {
        this.applyRotation = apply;
    }

    /**
     * 设置位置缩放系数
     */
    public void setPositionScale(float scale) {
        this.positionScale = scale;
    }

    /**
     * 设置旋转缩放系数
     */
    public void setRotationScale(float scale) {
        this.rotationScale = scale;
    }

    /**
     * 设置最大线性速度
     */
    public void setMaxLinearVelocity(float velocity) {
        this.maxLinearVelocity = velocity;
    }

    /**
     * 设置最大角速度
     */
    public void setMaxAngularVelocity(float velocity) {
        this.maxAngularVelocity = velocity;
    }

    /**
     * 获取位置增量
     */
    public float[] getDeltaPosition() {
        return new float[]{deltaPositionX, deltaPositionY, deltaPositionZ};
    }

    /**
     * 获取旋转增量
     */
    public float[] getDeltaRotation() {
        return new float[]{deltaRotationX, deltaRotationY, deltaRotationZ};
    }

    /**
     * 重置增量
     */
    public void reset() {
        deltaPositionX = 0;
        deltaPositionY = 0;
        deltaPositionZ = 0;
        deltaRotationX = 0;
        deltaRotationY = 0;
        deltaRotationZ = 0;
        resetPreviousState();
    }

    /**
     * 获取运动摘要
     */
    public String getMotionInfo() {
        return String.format("RootMotion [Enabled: %b, DeltaPos: (%.2f, %.2f, %.2f), DeltaRot: (%.2f, %.2f, %.2f)]",
                enabled, deltaPositionX, deltaPositionY, deltaPositionZ,
                deltaRotationX, deltaRotationY, deltaRotationZ);
    }

    // Getters
    public RootBone getRootBone() { return rootBone; }
    public Animation getRootAnimation() { return rootAnimation; }
    public boolean isApplyingPosition() { return applyPosition; }
    public boolean isApplyingRotation() { return applyRotation; }
    public float getPositionScale() { return positionScale; }
    public float getRotationScale() { return rotationScale; }

    @Override
    public String toString() {
        return getMotionInfo();
    }
}
