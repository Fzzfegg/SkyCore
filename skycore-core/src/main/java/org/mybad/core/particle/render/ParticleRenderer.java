package org.mybad.core.particle.render;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.ParticleSystem;
import java.util.*;

/**
 * 粒子渲染驱动 - 管理粒子渲染过程
 * 支持多种渲染后端和优化策略
 */
public class ParticleRenderer {

    private String rendererId;
    private ParticleSystem particleSystem;

    private List<ParticleProcessor> processors;
    private RenderState renderState;

    // 优化策略
    private boolean useBatching = true;
    private boolean useDepthSort = true;
    private boolean useFrustumCulling = true;
    private float cullDistance = 64.0f;

    // 性能统计
    private long particlesRendered = 0;
    private float averageRenderTime = 0;

    public ParticleRenderer(String rendererId, ParticleSystem particleSystem) {
        this.rendererId = rendererId;
        this.particleSystem = particleSystem;
        this.processors = new ArrayList<>();
        this.renderState = new RenderState();
    }

    /**
     * 注册渲染处理器（访问者模式）
     */
    public void registerProcessor(ParticleProcessor processor) {
        if (processor != null) {
            processors.add(processor);
        }
    }

    /**
     * 渲染粒子系统
     */
    public void render(float cameraX, float cameraY, float cameraZ) {
        long startTime = System.nanoTime();

        if (particleSystem == null || !particleSystem.isRunning()) {
            return;
        }

        Collection<Particle> particles = particleSystem.getActiveParticles();

        // 视锥体剔除
        List<Particle> visibleParticles = new ArrayList<>(particles);
        if (useFrustumCulling) {
            visibleParticles = cullParticles(particles, cameraX, cameraY, cameraZ);
        }

        // 深度排序
        if (useDepthSort && !visibleParticles.isEmpty()) {
            visibleParticles = sortByDepth(visibleParticles, cameraX, cameraY, cameraZ);
        }

        // 批处理
        if (useBatching) {
            renderBatched(visibleParticles);
        } else {
            renderSingle(visibleParticles);
        }

        particlesRendered += visibleParticles.size();

        // 性能统计
        long endTime = System.nanoTime();
        float frameTime = (endTime - startTime) / 1_000_000.0f; // 毫秒
        averageRenderTime = averageRenderTime * 0.9f + frameTime * 0.1f;
    }

    /**
     * 视锥体剔除
     */
    private List<Particle> cullParticles(Collection<Particle> particles, float camX, float camY, float camZ) {
        List<Particle> visible = new ArrayList<>();

        for (Particle particle : particles) {
            if (!particle.isAlive()) {
                continue;
            }

            float dx = particle.getPositionX() - camX;
            float dy = particle.getPositionY() - camY;
            float dz = particle.getPositionZ() - camZ;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance <= cullDistance) {
                visible.add(particle);
            }
        }

        return visible;
    }

    /**
     * 按深度排序
     */
    private List<Particle> sortByDepth(List<Particle> particles, float camX, float camY, float camZ) {
        List<Particle> sorted = new ArrayList<>(particles);

        sorted.sort((p1, p2) -> {
            float d1 = distance(p1.getPositionX(), p1.getPositionY(), p1.getPositionZ(), camX, camY, camZ);
            float d2 = distance(p2.getPositionX(), p2.getPositionY(), p2.getPositionZ(), camX, camY, camZ);
            return Float.compare(d2, d1); // 从远到近
        });

        return sorted;
    }

    /**
     * 计算距离
     */
    private float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float dz = z1 - z2;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 批量渲染
     */
    private void renderBatched(List<Particle> particles) {
        // 按材质分组
        Map<String, List<Particle>> batches = new HashMap<>();

        for (Particle particle : particles) {
            String materialId = particle.getEffect() != null ?
                    particle.getEffect().getEffectId() : "default";

            batches.computeIfAbsent(materialId, k -> new ArrayList<>()).add(particle);
        }

        // 渲染每个批次
        for (List<Particle> batch : batches.values()) {
            renderBatch(batch);
        }
    }

    /**
     * 渲染单个批次
     */
    private void renderBatch(List<Particle> batch) {
        for (Particle particle : batch) {
            // 应用所有处理器（访问者模式）
            for (ParticleProcessor processor : processors) {
                processor.process(particle, renderState);
            }
        }
    }

    /**
     * 单粒子渲染
     */
    private void renderSingle(List<Particle> particles) {
        for (Particle particle : particles) {
            for (ParticleProcessor processor : processors) {
                processor.process(particle, renderState);
            }
        }
    }

    /**
     * 获取渲染统计
     */
    public String getRenderStats() {
        return String.format("ParticleRenderer [%s, Rendered: %d, AvgTime: %.2fms]",
                rendererId, particlesRendered, averageRenderTime);
    }

    // Setters
    public void setUseBatching(boolean use) { this.useBatching = use; }
    public void setUseDepthSort(boolean use) { this.useDepthSort = use; }
    public void setUseFrustumCulling(boolean use) { this.useFrustumCulling = use; }
    public void setCullDistance(float distance) { this.cullDistance = distance; }

    // Getters
    public String getRendererId() { return rendererId; }
    public float getAverageRenderTime() { return averageRenderTime; }
    public long getParticlesRendered() { return particlesRendered; }

    @Override
    public String toString() {
        return getRenderStats();
    }

    /**
     * 粒子处理器接口（访问者模式）
     */
    public interface ParticleProcessor {
        void process(Particle particle, RenderState state);
    }
}
