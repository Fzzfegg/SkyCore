package org.mybad.core.data;

import org.mybad.core.constraint.Constraint;

import java.util.*;

/**
 * 基岩格式模型容器
 * 包含骨骼树、定位器和约束等完整模型信息
 */
public class Model {
    private String name;
    private String textureWidth;
    private String textureHeight;

    private List<ModelBone> bones;
    private Map<String, ModelBone> boneMap;  // 快速查找
    private Map<String, ModelLocator> locators;
    private List<Constraint> constraints;

    public Model(String name) {
        this.name = name;
        this.bones = new ArrayList<>();
        this.boneMap = new HashMap<>();
        this.locators = new HashMap<>();
        this.constraints = new ArrayList<>();
    }

    /**
     * 添加骨骼
     */
    public void addBone(ModelBone bone) {
        bones.add(bone);
        boneMap.put(bone.getName(), bone);
    }

    /**
     * 根据名称查找骨骼
     */
    public ModelBone getBone(String name) {
        return boneMap.get(name);
    }

    /**
     * 添加定位器
     */
    public void addLocator(String name, ModelLocator locator) {
        locators.put(name, locator);
    }

    /**
     * 获取定位器
     */
    public ModelLocator getLocator(String name) {
        return locators.get(name);
    }

    /**
     * 添加约束
     */
    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    /**
     * 重置所有骨骼到绑定姿态
     */
    public void resetToBindPose() {
        for (ModelBone bone : bones) {
            bone.resetToBindPose();
        }
    }

    // Getters
    public String getName() { return name; }
    public String getTextureWidth() { return textureWidth; }
    public String getTextureHeight() { return textureHeight; }
    public List<ModelBone> getBones() { return bones; }
    public List<ModelBone> getAllBones() { return bones; }
    public Map<String, ModelLocator> getLocators() { return locators; }
    public List<Constraint> getConstraints() { return constraints; }

    // Setters
    public void setTextureWidth(String width) { this.textureWidth = width; }
    public void setTextureHeight(String height) { this.textureHeight = height; }
}
