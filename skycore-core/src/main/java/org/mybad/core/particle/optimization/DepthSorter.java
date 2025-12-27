package org.mybad.core.particle.optimization;

import org.mybad.core.particle.Particle;
import java.util.*;

/**
 * 深度排序器 - 优化透明粒子的渲染顺序
 * 使用增量排序和分组策略
 */
public class DepthSorter {

    private float cameraX = 0;
    private float cameraY = 0;
    private float cameraZ = 0;

    // 缓存上一帧的排序结果
    private List<Particle> lastSortedParticles;
    private boolean needsResort = true;

    // 排序策略
    private SortStrategy sortStrategy = SortStrategy.BUCKET;
    private int bucketCount = 16; // 深度分组数

    // 性能统计
    private long lastSortTime = 0;
    private int sortCalls = 0;

    public DepthSorter() {
        this.lastSortedParticles = new ArrayList<>();
    }

    /**
     * 设置相机位置
     */
    public void setCameraPosition(float x, float y, float z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
        this.needsResort = true;
    }

    /**
     * 排序粒子
     */
    public List<Particle> sort(List<Particle> particles) {
        long startTime = System.nanoTime();
        sortCalls++;

        List<Particle> sorted;

        switch (sortStrategy) {
            case BUBBLE:
                sorted = bubbleSort(particles);
                break;
            case BUCKET:
                sorted = bucketSort(particles);
                break;
            case INCREMENTAL:
                sorted = incrementalSort(particles);
                break;
            default:
                sorted = new ArrayList<>(particles);
        }

        lastSortedParticles = sorted;
        needsResort = false;

        // 性能统计
        long endTime = System.nanoTime();
        lastSortTime = (endTime - startTime) / 1_000_000; // 毫秒

        return sorted;
    }

    /**
     * 冒泡排序（最简单，适合小数据集）
     */
    private List<Particle> bubbleSort(List<Particle> particles) {
        List<Particle> sorted = new ArrayList<>(particles);

        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i + 1; j < sorted.size(); j++) {
                float d1 = getDepth(sorted.get(i));
                float d2 = getDepth(sorted.get(j));

                if (d1 < d2) { // 从远到近
                    Particle temp = sorted.get(i);
                    sorted.set(i, sorted.get(j));
                    sorted.set(j, temp);
                }
            }
        }

        return sorted;
    }

    /**
     * 分桶排序（优化性能）
     */
    private List<Particle> bucketSort(List<Particle> particles) {
        if (particles.isEmpty()) {
            return particles;
        }

        // 计算深度范围
        float minDepth = Float.MAX_VALUE;
        float maxDepth = Float.MIN_VALUE;

        for (Particle p : particles) {
            float depth = getDepth(p);
            minDepth = Math.min(minDepth, depth);
            maxDepth = Math.max(maxDepth, depth);
        }

        // 创建桶
        List<List<Particle>> buckets = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            buckets.add(new ArrayList<>());
        }

        // 将粒子分配到桶中
        float range = maxDepth - minDepth;
        if (range < 0.001f) range = 1.0f;

        for (Particle p : particles) {
            float depth = getDepth(p);
            int bucketIndex = (int) ((depth - minDepth) / range * (bucketCount - 1));
            bucketIndex = Math.max(0, Math.min(bucketCount - 1, bucketIndex));
            buckets.get(bucketIndex).add(p);
        }

        // 合并桶
        List<Particle> sorted = new ArrayList<>();
        for (int i = bucketCount - 1; i >= 0; i--) {
            sorted.addAll(buckets.get(i));
        }

        return sorted;
    }

    /**
     * 增量排序（使用上一帧的结果）
     */
    private List<Particle> incrementalSort(List<Particle> particles) {
        if (lastSortedParticles.isEmpty()) {
            return bucketSort(particles); // 第一次使用分桶排序
        }

        // 保留上一帧的顺序，只重新排序改变了的粒子
        List<Particle> sorted = new ArrayList<>(lastSortedParticles);

        // 简单的插入排序用于调整位置
        for (Particle p : particles) {
            if (!sorted.contains(p)) {
                // 新粒子，找到合适的位置插入
                float pDepth = getDepth(p);
                int insertIndex = sorted.size();

                for (int i = 0; i < sorted.size(); i++) {
                    if (getDepth(sorted.get(i)) < pDepth) {
                        insertIndex = i;
                        break;
                    }
                }

                sorted.add(insertIndex, p);
            }
        }

        // 移除死亡的粒子
        sorted.removeIf(p -> !p.isAlive());

        return sorted;
    }

    /**
     * 计算粒子深度
     */
    private float getDepth(Particle p) {
        float dx = p.getPositionX() - cameraX;
        float dy = p.getPositionY() - cameraY;
        float dz = p.getPositionZ() - cameraZ;
        return dx * dx + dy * dy + dz * dz; // 使用平方距离（避免sqrt）
    }

    /**
     * 设置排序策略
     */
    public void setSortStrategy(SortStrategy strategy) {
        this.sortStrategy = strategy;
        this.needsResort = true;
    }

    /**
     * 设置分桶数
     */
    public void setBucketCount(int count) {
        this.bucketCount = Math.max(4, count);
    }

    /**
     * 获取排序统计
     */
    public String getStats() {
        return String.format("DepthSorter [Strategy: %s, LastTime: %dms, Calls: %d]",
                sortStrategy, lastSortTime, sortCalls);
    }

    // Getters
    public long getLastSortTime() { return lastSortTime; }
    public int getSortCalls() { return sortCalls; }

    /**
     * 排序策略
     */
    public enum SortStrategy {
        BUBBLE,        // 冒泡排序
        BUCKET,        // 分桶排序（推荐）
        INCREMENTAL    // 增量排序
    }

    @Override
    public String toString() {
        return getStats();
    }
}
