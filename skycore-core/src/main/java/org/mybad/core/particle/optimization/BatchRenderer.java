package org.mybad.core.particle.optimization;

import org.mybad.core.particle.Particle;
import java.util.*;

/**
 * 批处理渲染器 - 优化渲染性能
 * 减少DrawCall，提高批处理效率
 */
public class BatchRenderer {

    private String rendererId;

    // 批次管理
    private Map<String, ParticleBatch> batches;
    private int maxBatchSize = 1000;
    private int maxDrawCalls = 100;

    // 优化策略
    private BatchStrategy batchStrategy = BatchStrategy.MATERIAL;

    // 性能统计
    private int totalDrawCalls = 0;
    private int totalBatches = 0;
    private float averageBatchSize = 0;

    public BatchRenderer(String rendererId) {
        this.rendererId = rendererId;
        this.batches = new HashMap<>();
    }

    /**
     * 生成批处理
     */
    public List<ParticleBatch> generateBatches(List<Particle> particles) {
        batches.clear();
        totalBatches = 0;
        totalDrawCalls = 0;

        switch (batchStrategy) {
            case MATERIAL:
                return batchByMaterial(particles);
            case TEXTURE:
                return batchByTexture(particles);
            case DISTANCE:
                return batchByDistance(particles);
            default:
                return batchByMaterial(particles);
        }
    }

    /**
     * 按材质分组
     */
    private List<ParticleBatch> batchByMaterial(List<Particle> particles) {
        Map<String, List<Particle>> groups = new HashMap<>();

        for (Particle p : particles) {
            String materialId = p.getEffect() != null ?
                    p.getEffect().getEffectId() : "default";

            groups.computeIfAbsent(materialId, k -> new ArrayList<>()).add(p);
        }

        return createBatchesFromGroups(groups);
    }

    /**
     * 按纹理分组
     */
    private List<ParticleBatch> batchByTexture(List<Particle> particles) {
        Map<String, List<Particle>> groups = new HashMap<>();

        for (Particle p : particles) {
            String textureId = p.getEffect() != null ?
                    p.getEffect().getTextureFile() : "default";

            groups.computeIfAbsent(textureId, k -> new ArrayList<>()).add(p);
        }

        return createBatchesFromGroups(groups);
    }

    /**
     * 按距离分组（LOD）
     */
    private List<ParticleBatch> batchByDistance(List<Particle> particles) {
        Map<String, List<Particle>> groups = new HashMap<>();

        for (Particle p : particles) {
            // 简化LOD：根据缩放估算距离层级
            float scale = p.getScaleX();
            int lod = (int) (Math.log(Math.max(0.1f, scale)) * 5);
            lod = Math.max(0, Math.min(3, lod)); // 限制在0-3

            String lodKey = "lod_" + lod;
            groups.computeIfAbsent(lodKey, k -> new ArrayList<>()).add(p);
        }

        return createBatchesFromGroups(groups);
    }

    /**
     * 从分组创建批次
     */
    private List<ParticleBatch> createBatchesFromGroups(Map<String, List<Particle>> groups) {
        List<ParticleBatch> result = new ArrayList<>();

        for (Map.Entry<String, List<Particle>> entry : groups.entrySet()) {
            String groupId = entry.getKey();
            List<Particle> groupParticles = entry.getValue();

            // 如果超过最大批次大小，分割批次
            if (groupParticles.size() > maxBatchSize) {
                int batchCount = (int) Math.ceil((float) groupParticles.size() / maxBatchSize);
                for (int i = 0; i < batchCount; i++) {
                    int start = i * maxBatchSize;
                    int end = Math.min(start + maxBatchSize, groupParticles.size());
                    List<Particle> batchParticles = groupParticles.subList(start, end);

                    ParticleBatch batch = new ParticleBatch(
                            groupId + "_" + i,
                            new ArrayList<>(batchParticles)
                    );
                    result.add(batch);
                    totalBatches++;
                }
            } else {
                ParticleBatch batch = new ParticleBatch(groupId, new ArrayList<>(groupParticles));
                result.add(batch);
                totalBatches++;
            }
        }

        totalDrawCalls = result.size();

        // 计算平均批次大小
        if (!result.isEmpty()) {
            int totalParticles = result.stream().mapToInt(b -> b.getParticles().size()).sum();
            averageBatchSize = averageBatchSize * 0.9f + (totalParticles / (float) result.size()) * 0.1f;
        }

        return result;
    }

    /**
     * 合并相邻批次
     */
    public void mergeBatches(List<ParticleBatch> batches) {
        if (batches.size() <= 1) {
            return;
        }

        List<ParticleBatch> merged = new ArrayList<>();
        ParticleBatch currentBatch = null;

        for (ParticleBatch batch : batches) {
            if (currentBatch == null) {
                currentBatch = new ParticleBatch(batch.getBatchId(), new ArrayList<>(batch.getParticles()));
            } else if (currentBatch.getParticles().size() + batch.getParticles().size() <= maxBatchSize) {
                // 合并到当前批次
                currentBatch.getParticles().addAll(batch.getParticles());
            } else {
                // 保存当前批次，开始新的
                merged.add(currentBatch);
                currentBatch = new ParticleBatch(batch.getBatchId(), new ArrayList<>(batch.getParticles()));
            }
        }

        if (currentBatch != null) {
            merged.add(currentBatch);
        }

        batches.clear();
        batches.addAll(merged);
        totalDrawCalls = merged.size();
    }

    /**
     * 设置批处理策略
     */
    public void setBatchStrategy(BatchStrategy strategy) {
        this.batchStrategy = strategy;
    }

    /**
     * 设置最大批次大小
     */
    public void setMaxBatchSize(int size) {
        this.maxBatchSize = Math.max(1, size);
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format(
                "BatchRenderer [%s, Batches: %d, DrawCalls: %d, AvgSize: %.1f]",
                rendererId, totalBatches, totalDrawCalls, averageBatchSize);
    }

    // Getters
    public int getTotalBatches() { return totalBatches; }
    public int getTotalDrawCalls() { return totalDrawCalls; }
    public float getAverageBatchSize() { return averageBatchSize; }

    /**
     * 粒子批次
     */
    public static class ParticleBatch {
        private String batchId;
        private List<Particle> particles;

        public ParticleBatch(String batchId, List<Particle> particles) {
            this.batchId = batchId;
            this.particles = particles;
        }

        public String getBatchId() { return batchId; }
        public List<Particle> getParticles() { return particles; }
        public int getSize() { return particles.size(); }

        @Override
        public String toString() {
            return String.format("ParticleBatch [%s, Size: %d]", batchId, particles.size());
        }
    }

    /**
     * 批处理策略
     */
    public enum BatchStrategy {
        MATERIAL,      // 按材质分组
        TEXTURE,       // 按纹理分组
        DISTANCE       // 按距离分组（LOD）
    }

    @Override
    public String toString() {
        return getStats();
    }
}
