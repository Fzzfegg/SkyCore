package org.mybad.core.particle;

import java.util.*;

/**
 * 粒子对象池 - 性能优化关键
 * 预先分配粒子对象，避免频繁的垃圾回收
 */
public class ParticlePool {

    private String poolId;
    private int initialSize;
    private int maxSize;

    private Queue<Particle> availableParticles;  // 可用粒子
    private Set<Particle> activeParticles;       // 活跃粒子

    // 统计信息
    private int totalCreated = 0;
    private int currentAllocations = 0;
    private long peakAllocations = 0;

    public ParticlePool(String poolId, int initialSize, int maxSize) {
        this.poolId = poolId;
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.availableParticles = new LinkedList<>();
        this.activeParticles = new HashSet<>();

        // 预分配粒子
        preallocate(initialSize);
    }

    /**
     * 预分配粒子对象
     */
    private void preallocate(int count) {
        for (int i = 0; i < count; i++) {
            String particleId = poolId + "_particle_" + totalCreated++;
            Particle particle = new Particle(particleId, null, 1.0f);
            availableParticles.offer(particle);
        }
    }

    /**
     * 获取粒子
     */
    public Particle acquire(ParticleEffect effect, float maxAge) {
        Particle particle;

        if (availableParticles.isEmpty()) {
            // 如果没有可用粒子，创建新的（如果没超过最大限制）
            if (currentAllocations >= maxSize) {
                return null; // 对象池满
            }

            String particleId = poolId + "_particle_" + totalCreated++;
            particle = new Particle(particleId, effect, maxAge);
        } else {
            // 从池中获取
            particle = availableParticles.poll();
            particle = new Particle(particle.getParticleId(), effect, maxAge);
        }

        activeParticles.add(particle);
        currentAllocations++;
        peakAllocations = Math.max(peakAllocations, currentAllocations);

        return particle;
    }

    /**
     * 释放粒子回池
     */
    public void release(Particle particle) {
        if (particle == null) {
            return;
        }

        activeParticles.remove(particle);
        particle.reset();
        availableParticles.offer(particle);
        currentAllocations--;
    }

    /**
     * 批量释放粒子
     */
    public void releaseAll(Collection<Particle> particles) {
        for (Particle particle : particles) {
            release(particle);
        }
    }

    /**
     * 清空对象池
     */
    public void clear() {
        activeParticles.clear();
        availableParticles.clear();
        currentAllocations = 0;
    }

    /**
     * 获取当前活跃粒子数
     */
    public int getActiveParticleCount() {
        return activeParticles.size();
    }

    /**
     * 获取可用粒子数
     */
    public int getAvailableParticleCount() {
        return availableParticles.size();
    }

    /**
     * 获取当前分配数
     */
    public int getCurrentAllocations() {
        return currentAllocations;
    }

    /**
     * 获取峰值分配数
     */
    public long getPeakAllocations() {
        return peakAllocations;
    }

    /**
     * 获取所有活跃粒子
     */
    public Collection<Particle> getActiveParticles() {
        return new ArrayList<>(activeParticles);
    }

    /**
     * 扩展池大小
     */
    public void expand(int additionalSize) {
        int newSize = Math.min(availableParticles.size() + additionalSize, maxSize);
        int toCreate = newSize - (availableParticles.size() + currentAllocations);

        if (toCreate > 0) {
            preallocate(toCreate);
        }
    }

    /**
     * 获取池状态信息
     */
    public String getPoolStats() {
        return String.format("ParticlePool [%s, Active: %d, Available: %d, Peak: %d, Max: %d]",
                poolId, getActiveParticleCount(), getAvailableParticleCount(), peakAllocations, maxSize);
    }

    // Getters
    public String getPoolId() { return poolId; }
    public int getMaxSize() { return maxSize; }
    public float getUtilization() {
        return (float) currentAllocations / maxSize;
    }

    @Override
    public String toString() {
        return getPoolStats();
    }
}
