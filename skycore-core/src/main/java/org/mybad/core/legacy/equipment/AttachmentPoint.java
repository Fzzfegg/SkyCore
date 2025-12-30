package org.mybad.core.legacy.equipment;

import org.mybad.core.data.ModelBone;

/**
 * 附着点 - 在骨骼上附着装备的定义点
 * 定义装备在模型上的位置、旋转和缩放
 */
public class AttachmentPoint {

    private String pointName;
    private String boneName;
    private ModelBone bone;

    // 相对变换
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    private float rotationX;
    private float rotationY;
    private float rotationZ;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;

    // 配置
    private boolean active = true;
    private String category;              // 装备分类（如"weapon", "armor", "hat"）
    private float priority;               // 优先级（用于多个同类装备时排序）

    // 装备
    private Equipment equippedItem;

    public AttachmentPoint(String pointName, String boneName) {
        this.pointName = pointName;
        this.boneName = boneName;
    }

    public AttachmentPoint(String pointName, ModelBone bone) {
        this.pointName = pointName;
        this.bone = bone;
        this.boneName = bone.getName();
    }

    /**
     * 设置相对位置
     */
    public void setOffset(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
    }

    /**
     * 获取相对位置X
     */
    public float getOffsetX() {
        return offsetX;
    }

    /**
     * 获取相对位置Y
     */
    public float getOffsetY() {
        return offsetY;
    }

    /**
     * 获取相对位置Z
     */
    public float getOffsetZ() {
        return offsetZ;
    }

    /**
     * 设置相对旋转
     */
    public void setRotation(float rotX, float rotY, float rotZ) {
        this.rotationX = rotX;
        this.rotationY = rotY;
        this.rotationZ = rotZ;
    }

    /**
     * 获取相对旋转X
     */
    public float getRotationX() {
        return rotationX;
    }

    /**
     * 获取相对旋转Y
     */
    public float getRotationY() {
        return rotationY;
    }

    /**
     * 获取相对旋转Z
     */
    public float getRotationZ() {
        return rotationZ;
    }

    /**
     * 设置相对缩放
     */
    public void setScale(float scaleX, float scaleY, float scaleZ) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    /**
     * 获取相对缩放X
     */
    public float getScaleX() {
        return scaleX;
    }

    /**
     * 获取相对缩放Y
     */
    public float getScaleY() {
        return scaleY;
    }

    /**
     * 获取相对缩放Z
     */
    public float getScaleZ() {
        return scaleZ;
    }

    /**
     * 装备物品
     */
    public void equip(Equipment item) {
        this.equippedItem = item;
    }

    /**
     * 卸装物品
     */
    public Equipment unequip() {
        Equipment item = this.equippedItem;
        this.equippedItem = null;
        return item;
    }

    /**
     * 获取装备的物品
     */
    public Equipment getEquippedItem() {
        return equippedItem;
    }

    /**
     * 是否装备了物品
     */
    public boolean isEquipped() {
        return equippedItem != null;
    }

    /**
     * 设置附着点活跃状态
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * 是否活跃
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 设置装备分类
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 获取装备分类
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置优先级
     */
    public void setPriority(float priority) {
        this.priority = priority;
    }

    /**
     * 获取优先级
     */
    public float getPriority() {
        return priority;
    }

    /**
     * 关联骨骼
     */
    public void setBone(ModelBone bone) {
        this.bone = bone;
        if (bone != null) {
            this.boneName = bone.getName();
        }
    }

    /**
     * 获取关联骨骼
     */
    public ModelBone getBone() {
        return bone;
    }

    // Getters
    public String getPointName() { return pointName; }
    public String getBoneName() { return boneName; }

    @Override
    public String toString() {
        return String.format("AttachmentPoint [%s, Bone: %s, Equipped: %s, Active: %b]",
                pointName, boneName, equippedItem != null ? equippedItem.getName() : "none", active);
    }

    /**
     * 装备接口
     */
    public interface Equipment {
        String getName();
        void render(AttachmentPoint point);
    }
}
