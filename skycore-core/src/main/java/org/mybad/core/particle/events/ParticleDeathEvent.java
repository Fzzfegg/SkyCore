package org.mybad.core.particle.events;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.ParticleEffect;

/**
 * 粒子死亡事件 - 粒子到达生命周期末尾时触发
 */
public class ParticleDeathEvent extends ParticleEvent {

    public enum DeathReason {
        AGE_EXPIRED,        // 生命周期结束
        OUT_OF_BOUNDS,      // 超出边界
        COLLISION,          // 碰撞
        MANUAL_KILL,        // 手动删除
        SYSTEM_CLEARED      // 系统清空
    }

    private DeathReason deathReason;
    private float finalPositionX;
    private float finalPositionY;
    private float finalPositionZ;
    private float lifespan;

    public ParticleDeathEvent(String eventId, Particle particle, ParticleEffect effect, DeathReason reason) {
        super(eventId, "ParticleDeath", effect);
        this.particle = particle;
        this.deathReason = reason;

        // 记录最终状态
        this.finalPositionX = particle.getPositionX();
        this.finalPositionY = particle.getPositionY();
        this.finalPositionZ = particle.getPositionZ();

        this.lifespan = particle.getAge();
    }

    /**
     * 获取死亡原因
     */
    public DeathReason getDeathReason() {
        return deathReason;
    }

    /**
     * 获取最终位置
     */
    public float[] getFinalPosition() {
        return new float[]{finalPositionX, finalPositionY, finalPositionZ};
    }

    /**
     * 获取生命周期持续时间
     */
    public float getLifespan() {
        return lifespan;
    }

    @Override
    public String toString() {
        return String.format("ParticleDeathEvent [Particle: %s, Reason: %s, Lifespan: %.2fs]",
                particle.getParticleId(), deathReason, lifespan);
    }
}
