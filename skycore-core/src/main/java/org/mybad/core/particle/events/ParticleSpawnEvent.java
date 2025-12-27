package org.mybad.core.particle.events;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.ParticleEffect;

/**
 * 粒子生成事件 - 粒子被创建时触发
 */
public class ParticleSpawnEvent extends ParticleEvent {

    private float initialPositionX;
    private float initialPositionY;
    private float initialPositionZ;

    private float initialVelocityX;
    private float initialVelocityY;
    private float initialVelocityZ;

    private float initialScale;

    public ParticleSpawnEvent(String eventId, Particle particle, ParticleEffect effect) {
        super(eventId, "ParticleSpawn", effect);
        this.particle = particle;

        // 记录初始状态
        this.initialPositionX = particle.getPositionX();
        this.initialPositionY = particle.getPositionY();
        this.initialPositionZ = particle.getPositionZ();

        this.initialVelocityX = particle.getVelocityX();
        this.initialVelocityY = particle.getVelocityY();
        this.initialVelocityZ = particle.getVelocityZ();

        this.initialScale = particle.getScaleX();
    }

    /**
     * 获取初始位置
     */
    public float[] getInitialPosition() {
        return new float[]{initialPositionX, initialPositionY, initialPositionZ};
    }

    /**
     * 获取初始速度
     */
    public float[] getInitialVelocity() {
        return new float[]{initialVelocityX, initialVelocityY, initialVelocityZ};
    }

    /**
     * 获取初始缩放
     */
    public float getInitialScale() {
        return initialScale;
    }

    @Override
    public String toString() {
        return String.format("ParticleSpawnEvent [Particle: %s, Position: (%.2f, %.2f, %.2f)]",
                particle.getParticleId(), initialPositionX, initialPositionY, initialPositionZ);
    }
}
