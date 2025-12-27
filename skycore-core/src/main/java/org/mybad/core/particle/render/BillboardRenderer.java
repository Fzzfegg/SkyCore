package org.mybad.core.particle.render;

import org.mybad.core.particle.Particle;

/**
 * Billboard渲染处理器 - 实现Billboard粒子渲染
 * 粒子总是面向相机
 */
public class BillboardRenderer implements ParticleRenderer.ParticleProcessor {

    private float cameraX = 0;
    private float cameraY = 0;
    private float cameraZ = 0;

    private float upX = 0;
    private float upY = 1;
    private float upZ = 0;

    public BillboardRenderer() {
        // 默认构造
    }

    /**
     * 设置相机位置
     */
    public void setCameraPosition(float x, float y, float z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
    }

    /**
     * 设置向上向量
     */
    public void setUpVector(float x, float y, float z) {
        this.upX = x;
        this.upY = y;
        this.upZ = z;
    }

    @Override
    public void process(Particle particle, RenderState state) {
        if (particle == null || !particle.isAlive()) {
            return;
        }

        // 计算朝向相机的旋转
        float dx = cameraX - particle.getPositionX();
        float dy = cameraY - particle.getPositionY();
        float dz = cameraZ - particle.getPositionZ();

        // 归一化方向向量
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0.001f) {
            dx /= dist;
            dy /= dist;
            dz /= dist;

            // 计算旋转（简化版，实际应该计算四元数或矩阵）
            float rotX = (float) Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            float rotY = (float) Math.atan2(dx, dz);

            particle.setRotation(rotX, rotY, 0);
        }

        // 设置状态
        state.setMaterial(particle.getEffect() != null ? particle.getEffect().getEffectId() : "default");
        state.addVertices(4); // Billboard通常需要4个顶点（四边形）
    }

    /**
     * 获取渲染器信息
     */
    public String getInfo() {
        return String.format("BillboardRenderer [Camera: (%.2f, %.2f, %.2f)]",
                cameraX, cameraY, cameraZ);
    }

    @Override
    public String toString() {
        return getInfo();
    }
}
