package org.mybad.core.equipment;

import java.util.*;

/**
 * 装备管理器 - 管理模型的所有附着点和装备
 * 负责装备的挂载、卸装、渲染等
 */
public class EquipmentManager {

    private String managerId;
    private Map<String, AttachmentPoint> attachmentPoints;
    private Map<String, List<AttachmentPoint>> categoryMap;
    private List<EquipmentListener> listeners;

    public EquipmentManager(String managerId) {
        this.managerId = managerId;
        this.attachmentPoints = new HashMap<>();
        this.categoryMap = new HashMap<>();
        this.listeners = new ArrayList<>();
    }

    /**
     * 注册附着点
     */
    public void registerAttachmentPoint(AttachmentPoint point) {
        if (point == null) {
            return;
        }

        attachmentPoints.put(point.getPointName(), point);

        // 按分类索引
        if (point.getCategory() != null) {
            categoryMap.computeIfAbsent(point.getCategory(), k -> new ArrayList<>()).add(point);
        }
    }

    /**
     * 注销附着点
     */
    public void unregisterAttachmentPoint(String pointName) {
        AttachmentPoint point = attachmentPoints.remove(pointName);
        if (point != null && point.getCategory() != null) {
            List<AttachmentPoint> categoryList = categoryMap.get(point.getCategory());
            if (categoryList != null) {
                categoryList.remove(point);
            }
        }
    }

    /**
     * 获取附着点
     */
    public AttachmentPoint getAttachmentPoint(String pointName) {
        return attachmentPoints.get(pointName);
    }

    /**
     * 获取指定分类的所有附着点
     */
    public List<AttachmentPoint> getAttachmentPointsByCategory(String category) {
        return categoryMap.getOrDefault(category, new ArrayList<>());
    }

    /**
     * 装备物品
     */
    public boolean equip(String pointName, AttachmentPoint.Equipment equipment) {
        AttachmentPoint point = getAttachmentPoint(pointName);
        if (point == null || !point.isActive()) {
            return false;
        }

        // 如果已有装备，先卸装
        if (point.isEquipped()) {
            point.unequip();
        }

        point.equip(equipment);
        fireEquipmentEvent("equip", pointName, equipment);
        return true;
    }

    /**
     * 卸装物品
     */
    public AttachmentPoint.Equipment unequip(String pointName) {
        AttachmentPoint point = getAttachmentPoint(pointName);
        if (point == null) {
            return null;
        }

        AttachmentPoint.Equipment equipment = point.unequip();
        if (equipment != null) {
            fireEquipmentEvent("unequip", pointName, equipment);
        }
        return equipment;
    }

    /**
     * 卸装指定分类的所有装备
     */
    public List<AttachmentPoint.Equipment> unequipCategory(String category) {
        List<AttachmentPoint.Equipment> unequipped = new ArrayList<>();
        List<AttachmentPoint> points = getAttachmentPointsByCategory(category);

        for (AttachmentPoint point : points) {
            if (point.isEquipped()) {
                AttachmentPoint.Equipment equipment = point.unequip();
                if (equipment != null) {
                    unequipped.add(equipment);
                    fireEquipmentEvent("unequip", point.getPointName(), equipment);
                }
            }
        }

        return unequipped;
    }

    /**
     * 检查是否装备了指定物品
     */
    public boolean isEquipped(String pointName) {
        AttachmentPoint point = getAttachmentPoint(pointName);
        return point != null && point.isEquipped();
    }

    /**
     * 获取装备的物品
     */
    public AttachmentPoint.Equipment getEquippedItem(String pointName) {
        AttachmentPoint point = getAttachmentPoint(pointName);
        return point != null ? point.getEquippedItem() : null;
    }

    /**
     * 渲染所有装备
     */
    public void renderEquipment() {
        for (AttachmentPoint point : attachmentPoints.values()) {
            if (point.isActive() && point.isEquipped()) {
                AttachmentPoint.Equipment equipment = point.getEquippedItem();
                if (equipment != null) {
                    equipment.render(point);
                }
            }
        }
    }

    /**
     * 渲染指定分类的装备
     */
    public void renderCategory(String category) {
        List<AttachmentPoint> points = getAttachmentPointsByCategory(category);
        points.sort(Comparator.comparingDouble(AttachmentPoint::getPriority));

        for (AttachmentPoint point : points) {
            if (point.isActive() && point.isEquipped()) {
                AttachmentPoint.Equipment equipment = point.getEquippedItem();
                if (equipment != null) {
                    equipment.render(point);
                }
            }
        }
    }

    /**
     * 启用/禁用附着点
     */
    public void setAttachmentPointActive(String pointName, boolean active) {
        AttachmentPoint point = getAttachmentPoint(pointName);
        if (point != null) {
            point.setActive(active);
        }
    }

    /**
     * 启用/禁用分类中的所有附着点
     */
    public void setCategoryActive(String category, boolean active) {
        List<AttachmentPoint> points = getAttachmentPointsByCategory(category);
        for (AttachmentPoint point : points) {
            point.setActive(active);
        }
    }

    /**
     * 获取装备统计信息
     */
    public EquipmentStats getStats() {
        EquipmentStats stats = new EquipmentStats();
        stats.totalPoints = attachmentPoints.size();
        stats.activePoints = 0;
        stats.equippedCount = 0;

        for (AttachmentPoint point : attachmentPoints.values()) {
            if (point.isActive()) {
                stats.activePoints++;
                if (point.isEquipped()) {
                    stats.equippedCount++;
                }
            }
        }

        stats.categories = new HashSet<>(categoryMap.keySet());
        return stats;
    }

    /**
     * 清空所有装备
     */
    public void clearAllEquipment() {
        for (AttachmentPoint point : attachmentPoints.values()) {
            if (point.isEquipped()) {
                point.unequip();
            }
        }
    }

    /**
     * 添加监听器
     */
    public void addListener(EquipmentListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除监听器
     */
    public void removeListener(EquipmentListener listener) {
        listeners.remove(listener);
    }

    /**
     * 触发装备事件
     */
    private void fireEquipmentEvent(String eventType, String pointName, AttachmentPoint.Equipment equipment) {
        for (EquipmentListener listener : listeners) {
            listener.onEquipmentChanged(this, eventType, pointName, equipment);
        }
    }

    /**
     * 获取管理器信息
     */
    public String getManagerInfo() {
        EquipmentStats stats = getStats();
        return String.format("EquipmentManager [%s, Points: %d/%d, Equipped: %d, Categories: %d]",
                managerId, stats.activePoints, stats.totalPoints, stats.equippedCount, stats.categories.size());
    }

    // Getters
    public String getManagerId() { return managerId; }
    public int getAttachmentPointCount() { return attachmentPoints.size(); }
    public Collection<AttachmentPoint> getAllAttachmentPoints() { return new ArrayList<>(attachmentPoints.values()); }

    @Override
    public String toString() {
        return getManagerInfo();
    }

    /**
     * 装备统计信息
     */
    public static class EquipmentStats {
        public int totalPoints;
        public int activePoints;
        public int equippedCount;
        public Set<String> categories;
    }

    /**
     * 装备事件监听器
     */
    public interface EquipmentListener {
        void onEquipmentChanged(EquipmentManager manager, String eventType, String pointName, AttachmentPoint.Equipment equipment);
    }
}
