package org.mybad.core.constraint;

import org.mybad.core.data.ModelBone;

/**
 * 骨骼约束基类
 * 支持多种约束类型：旋转、缩放、平移等
 */
public abstract class Constraint {
    protected String name;
    protected String targetBone;  // 受影响的骨骼
    protected String sourceBone;  // 源骨骼（参考骨骼）

    public Constraint(String name, String targetBone, String sourceBone) {
        this.name = name;
        this.targetBone = targetBone;
        this.sourceBone = sourceBone;
    }

    /**
     * 应用约束到目标骨骼
     * 在渲染/动画时调用
     */
    public abstract void apply(ModelBone targetBone, ModelBone sourceBone);

    // Getters
    public String getName() { return name; }
    public String getTargetBone() { return targetBone; }
    public String getSourceBone() { return sourceBone; }
}
