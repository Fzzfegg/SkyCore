package org.mybad.core.data;

import org.mybad.core.constraint.Constraint;

import java.util.*;

/**
 * 基岩模型骨骼
 * 代表模型树结构中的一个节点
 * 包含位置、旋转、缩放和立方体等信息
 */
public class ModelBone {
    private String name;
    private ModelBone parent;
    private List<ModelBone> children;

    // 变换属性
    private float[] pivot;      // 旋转点 [x, y, z]
    private float[] position;   // 相对位置 [x, y, z]
    private float[] rotation;   // 旋转角度 [x, y, z] 单位：度数
    private float[] size;       // 骨骼大小/缩放 [x, y, z]
    private boolean hasPositionOverride;
    private float[] bindPosition;
    private float[] bindRotation;
    private float[] bindSize;

    // 内容
    private List<ModelCube> cubes;
    private List<Constraint> constraints;

    // 标志
    private boolean mirror;     // 镜像标志
    private boolean neverRender; // 不渲染标志
    private boolean reset;      // 重置标志

    public ModelBone(String name) {
        this.name = name;
        this.children = new ArrayList<>();
        this.cubes = new ArrayList<>();
        this.constraints = new ArrayList<>();

        // 初始化变换属性为默认值
        this.pivot = new float[]{0, 0, 0};
        this.position = new float[]{0, 0, 0};
        this.rotation = new float[]{0, 0, 0};
        this.size = new float[]{1, 1, 1};
        this.hasPositionOverride = false;
        this.bindPosition = new float[]{0, 0, 0};
        this.bindRotation = new float[]{0, 0, 0};
        this.bindSize = new float[]{1, 1, 1};

        this.mirror = false;
        this.neverRender = false;
        this.reset = false;
    }

    /**
     * 添加子骨骼
     */
    public void addChild(ModelBone child) {
        children.add(child);
        child.parent = this;
    }

    /**
     * 添加立方体
     */
    public void addCube(ModelCube cube) {
        cubes.add(cube);
    }

    /**
     * 添加约束
     */
    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    /**
     * 获取所有后代骨骼（深度优先）
     */
    public List<ModelBone> getAllDescendants() {
        List<ModelBone> result = new ArrayList<>();
        addDescendantsRecursively(this, result);
        return result;
    }

    private void addDescendantsRecursively(ModelBone bone, List<ModelBone> result) {
        for (ModelBone child : bone.children) {
            result.add(child);
            addDescendantsRecursively(child, result);
        }
    }

    // Getters
    public String getName() { return name; }
    public ModelBone getParent() { return parent; }
    public List<ModelBone> getChildren() { return children; }
    public float[] getPivot() { return pivot; }
    public float getPivotX() { return pivot[0]; }
    public float getPivotY() { return pivot[1]; }
    public float getPivotZ() { return pivot[2]; }
    public float[] getPosition() { return position; }
    public boolean hasPositionOverride() { return hasPositionOverride; }
    public float[] getRotation() { return rotation; }
    public float[] getSize() { return size; }
    public float[] getBindPosition() { return bindPosition; }
    public float[] getBindRotation() { return bindRotation; }
    public float[] getBindSize() { return bindSize; }
    public List<ModelCube> getCubes() { return cubes; }
    public List<Constraint> getConstraints() { return constraints; }
    public boolean isMirror() { return mirror; }
    public boolean isNeverRender() { return neverRender; }
    public boolean isReset() { return reset; }

    // Setters
    public void setPivot(float x, float y, float z) {
        this.pivot[0] = x;
        this.pivot[1] = y;
        this.pivot[2] = z;
    }

    public void setPivot(float[] pivot) {
        this.pivot = pivot;
    }

    public void setPosition(float x, float y, float z) {
        this.position[0] = x;
        this.position[1] = y;
        this.position[2] = z;
    }

    public void setPosition(float[] position) {
        this.position = position;
    }

    public void setHasPositionOverride(boolean override) {
        this.hasPositionOverride = override;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation[0] = x;
        this.rotation[1] = y;
        this.rotation[2] = z;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }

    public void setSize(float x, float y, float z) {
        this.size[0] = x;
        this.size[1] = y;
        this.size[2] = z;
    }

    public void setSize(float[] size) {
        this.size = size;
    }

    public void setMirror(boolean mirror) { this.mirror = mirror; }
    public void setNeverRender(boolean neverRender) { this.neverRender = neverRender; }
    public void setReset(boolean reset) { this.reset = reset; }

    public void captureBindPose() {
        copyVec3(position, bindPosition);
        copyVec3(rotation, bindRotation);
        copyVec3(size, bindSize);
    }

    public void resetToBindPose() {
        copyVec3(bindPosition, position);
        copyVec3(bindRotation, rotation);
        copyVec3(bindSize, size);
    }

    public void copyBindPoseFrom(ModelBone other) {
        if (other == null) {
            return;
        }
        copyVec3(other.bindPosition, this.bindPosition);
        copyVec3(other.bindRotation, this.bindRotation);
        copyVec3(other.bindSize, this.bindSize);
    }

    private void copyVec3(float[] src, float[] dst) {
        if (src == null || dst == null || src.length < 3 || dst.length < 3) {
            return;
        }
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
    }
}
