package org.mybad.core.constraint;

import org.mybad.core.data.ModelBone;

/**
 * 缩放约束
 * 约束目标骨骼的缩放跟随源骨骼
 */
public class ScaleConstraint extends Constraint {
    // 轴向选择
    private boolean constrainX;
    private boolean constrainY;
    private boolean constrainZ;

    // 约束范围
    private float minX, maxX;
    private float minY, maxY;
    private float minZ, maxZ;

    // 约束强度（0-1）
    private float strength;

    public ScaleConstraint(String name, String targetBone, String sourceBone) {
        super(name, targetBone, sourceBone);
        this.constrainX = true;
        this.constrainY = true;
        this.constrainZ = true;
        this.strength = 1.0f;
        this.minX = this.minY = this.minZ = 0.0f;
        this.maxX = this.maxY = this.maxZ = 2.0f;
    }

    @Override
    public void apply(ModelBone targetBone, ModelBone sourceBone) {
        if (targetBone == null || sourceBone == null) {
            return;
        }

        float[] sourceScale = sourceBone.getSize();
        float[] targetScale = targetBone.getSize();

        if (constrainX) {
            float value = sourceScale[0] * strength;
            targetScale[0] = Math.max(minX, Math.min(maxX, value));
        }

        if (constrainY) {
            float value = sourceScale[1] * strength;
            targetScale[1] = Math.max(minY, Math.min(maxY, value));
        }

        if (constrainZ) {
            float value = sourceScale[2] * strength;
            targetScale[2] = Math.max(minZ, Math.min(maxZ, value));
        }
    }

    // Setters
    public void setConstrainX(boolean value) { this.constrainX = value; }
    public void setConstrainY(boolean value) { this.constrainY = value; }
    public void setConstrainZ(boolean value) { this.constrainZ = value; }

    public void setRange(String axis, float min, float max) {
        switch (axis.toLowerCase()) {
            case "x":
                this.minX = min;
                this.maxX = max;
                break;
            case "y":
                this.minY = min;
                this.maxY = max;
                break;
            case "z":
                this.minZ = min;
                this.maxZ = max;
                break;
        }
    }

    public void setStrength(float strength) {
        this.strength = Math.max(0, Math.min(1, strength));
    }

    // Getters
    public boolean isConstrainX() { return constrainX; }
    public boolean isConstrainY() { return constrainY; }
    public boolean isConstrainZ() { return constrainZ; }
    public float getStrength() { return strength; }
}
