package org.mybad.core.legacy.utils;

import org.mybad.core.data.*;
import org.mybad.core.render.CoreMatrixStack;
import java.util.*;

/**
 * 定位器管理器
 * 计算定位器在世界坐标系中的位置
 * 用于附着点、粒子生成点、声音源等
 */
public class LocatorManager {
    private Model model;
    private Map<String, WorldLocator> cachedLocators;
    private boolean needsUpdate;

    public LocatorManager(Model model) {
        this.model = model;
        this.cachedLocators = new HashMap<>();
        this.needsUpdate = true;
    }

    /**
     * 更新所有定位器的世界坐标
     */
    public void updateWorldCoordinates() {
        if (!needsUpdate) {
            return;
        }

        cachedLocators.clear();

        for (String locatorName : model.getLocators().keySet()) {
            ModelLocator locator = model.getLocator(locatorName);
            WorldLocator worldLoc = calculateWorldCoordinates(locator);
            cachedLocators.put(locatorName, worldLoc);
        }

        needsUpdate = false;
    }

    /**
     * 计算单个定位器的世界坐标
     */
    private WorldLocator calculateWorldCoordinates(ModelLocator locator) {
        WorldLocator result = new WorldLocator(locator.getName());

        // 获取定位器相对的骨骼
        String attachedBoneName = locator.getAttachedBone();
        if (attachedBoneName == null || attachedBoneName.isEmpty()) {
            // 附着到根节点
            float[] pos = locator.getPosition();
            result.position = new float[]{pos[0], pos[1], pos[2]};
            result.rotation = new float[]{0, 0, 0};
            return result;
        }

        ModelBone attachedBone = model.getBone(attachedBoneName);
        if (attachedBone == null) {
            // 骨骼不存在，返回本地坐标
            float[] pos = locator.getPosition();
            result.position = new float[]{pos[0], pos[1], pos[2]};
            result.rotation = locator.getRotation();
            return result;
        }

        // 计算骨骼的世界变换矩阵
        CoreMatrixStack matrixStack = new CoreMatrixStack();
        calculateBoneWorldTransform(attachedBone, matrixStack);

        // 应用定位器本地位置（复制数组，避免修改原始数据）
        float[] position = locator.getPosition();
        float[] transformed = new float[]{position[0], position[1], position[2]};
        matrixStack.transform(transformed);
        result.position = transformed;

        // 应用旋转
        float[] rotation = locator.getRotation();
        if (rotation != null) {
            result.rotation = rotation;
        }

        return result;
    }

    /**
     * 递归计算骨骼的世界变换
     */
    private void calculateBoneWorldTransform(ModelBone bone, CoreMatrixStack stack) {
        if (bone.getParent() != null) {
            calculateBoneWorldTransform(bone.getParent(), stack);
        }

        // 应用骨骼变换
        float[] pivot = bone.getPivot();
        stack.translate(pivot[0], pivot[1], pivot[2]);

        float[] rotation = bone.getRotation();
        if (rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0) {
            stack.rotateEuler(rotation[0], rotation[1], rotation[2]);
        }

        float[] size = bone.getSize();
        if (size[0] != 1 || size[1] != 1 || size[2] != 1) {
            stack.scale(size[0], size[1], size[2]);
        }

        stack.translate(-pivot[0], -pivot[1], -pivot[2]);

        float[] position = bone.getPosition();
        stack.translate(position[0], position[1], position[2]);
    }

    /**
     * 获取定位器的世界坐标
     */
    public WorldLocator getWorldLocator(String locatorName) {
        updateWorldCoordinates();
        return cachedLocators.get(locatorName);
    }

    /**
     * 获取所有定位器的世界坐标
     */
    public Map<String, WorldLocator> getAllWorldLocators() {
        updateWorldCoordinates();
        return new HashMap<>(cachedLocators);
    }

    /**
     * 标记需要更新
     */
    public void markDirty() {
        needsUpdate = true;
    }

    /**
     * 世界坐标定位器
     */
    public static class WorldLocator {
        public String name;
        public float[] position;
        public float[] rotation;

        public WorldLocator(String name) {
            this.name = name;
            this.position = new float[]{0, 0, 0};
            this.rotation = new float[]{0, 0, 0};
        }

        public float getX() { return position[0]; }
        public float getY() { return position[1]; }
        public float getZ() { return position[2]; }

        public float[] getPosition() {
            return new float[]{position[0], position[1], position[2]};
        }

        public float[] getRotation() {
            return new float[]{rotation[0], rotation[1], rotation[2]};
        }

        @Override
        public String toString() {
            return String.format("定位器[%s]: 位置=(%f, %f, %f), 旋转=(%f, %f, %f)",
                name, position[0], position[1], position[2],
                rotation[0], rotation[1], rotation[2]);
        }
    }
}
