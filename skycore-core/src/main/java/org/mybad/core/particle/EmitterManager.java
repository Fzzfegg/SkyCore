package org.mybad.core.particle;

import java.util.*;

/**
 * 发射器管理器 - 管理所有活跃的发射器
 * 负责发射器的创建、销毁、状态管理
 */
public class EmitterManager {

    private String managerId;
    private ParticleSystem parentSystem;

    private Map<String, Emitter> emitters;
    private Map<String, Integer> emitterPriority;  // 发射器优先级

    // 缓存
    private List<Emitter> sortedEmitters;
    private boolean sortDirty = true;

    // 统计
    private long emitterCreated = 0;
    private long emitterDestroyed = 0;

    public EmitterManager(String managerId, ParticleSystem parentSystem) {
        this.managerId = managerId;
        this.parentSystem = parentSystem;
        this.emitters = new HashMap<>();
        this.emitterPriority = new HashMap<>();
        this.sortedEmitters = new ArrayList<>();
    }

    /**
     * 创建并添加发射器
     */
    public Emitter createEmitter(String emitterId, String emitterName) {
        Emitter emitter = new Emitter(emitterId, emitterName);
        addEmitter(emitter);
        return emitter;
    }

    /**
     * 添加发射器
     */
    public void addEmitter(Emitter emitter) {
        if (emitter != null) {
            emitters.put(emitter.getEmitterId(), emitter);
            emitterPriority.put(emitter.getEmitterId(), 0);
            sortDirty = true;
            emitterCreated++;
        }
    }

    /**
     * 移除发射器
     */
    public void removeEmitter(String emitterId) {
        Emitter emitter = emitters.remove(emitterId);
        if (emitter != null) {
            emitterPriority.remove(emitterId);
            sortDirty = true;
            emitterDestroyed++;
        }
    }

    /**
     * 获取发射器
     */
    public Emitter getEmitter(String emitterId) {
        return emitters.get(emitterId);
    }

    /**
     * 获取所有发射器
     */
    public Collection<Emitter> getAllEmitters() {
        return new ArrayList<>(emitters.values());
    }

    /**
     * 获取排序的发射器列表（按优先级）
     */
    public List<Emitter> getSortedEmitters() {
        if (sortDirty) {
            sortedEmitters.clear();
            sortedEmitters.addAll(emitters.values());
            sortedEmitters.sort((e1, e2) -> {
                int p1 = emitterPriority.getOrDefault(e1.getEmitterId(), 0);
                int p2 = emitterPriority.getOrDefault(e2.getEmitterId(), 0);
                return Integer.compare(p2, p1); // 降序
            });
            sortDirty = false;
        }
        return new ArrayList<>(sortedEmitters);
    }

    /**
     * 设置发射器优先级
     */
    public void setEmitterPriority(String emitterId, int priority) {
        emitterPriority.put(emitterId, priority);
        sortDirty = true;
    }

    /**
     * 启动所有发射器
     */
    public void startAll() {
        for (Emitter emitter : emitters.values()) {
            emitter.start();
        }
    }

    /**
     * 停止所有发射器
     */
    public void stopAll() {
        for (Emitter emitter : emitters.values()) {
            emitter.stop();
        }
    }

    /**
     * 启动指定发射器
     */
    public void start(String emitterId) {
        Emitter emitter = getEmitter(emitterId);
        if (emitter != null) {
            emitter.start();
        }
    }

    /**
     * 停止指定发射器
     */
    public void stop(String emitterId) {
        Emitter emitter = getEmitter(emitterId);
        if (emitter != null) {
            emitter.stop();
        }
    }

    /**
     * 重置所有发射器
     */
    public void resetAll() {
        for (Emitter emitter : emitters.values()) {
            emitter.reset();
        }
    }

    /**
     * 更新所有发射器
     */
    public void updateAll(float deltaTime) {
        for (Emitter emitter : emitters.values()) {
            emitter.update(deltaTime);
        }
    }

    /**
     * 清空所有发射器
     */
    public void clear() {
        emitters.clear();
        emitterPriority.clear();
        sortedEmitters.clear();
        sortDirty = true;
    }

    /**
     * 获取活跃发射器数
     */
    public int getActiveEmitterCount() {
        int count = 0;
        for (Emitter emitter : emitters.values()) {
            if (emitter.isActive()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取总发射器数
     */
    public int getTotalEmitterCount() {
        return emitters.size();
    }

    /**
     * 获取管理器统计信息
     */
    public String getManagerStats() {
        return String.format("EmitterManager [%s, Total: %d, Active: %d, Created: %d, Destroyed: %d]",
                managerId, getTotalEmitterCount(), getActiveEmitterCount(), emitterCreated, emitterDestroyed);
    }

    // Getters
    public String getManagerId() { return managerId; }
    public ParticleSystem getParentSystem() { return parentSystem; }

    @Override
    public String toString() {
        return getManagerStats();
    }
}
