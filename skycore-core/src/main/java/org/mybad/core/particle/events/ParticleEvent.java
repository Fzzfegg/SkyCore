package org.mybad.core.particle.events;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.ParticleEffect;

/**
 * 粒子事件基类 - 所有粒子事件的基础
 */
public abstract class ParticleEvent {

    protected String eventId;
    protected String eventName;
    protected long timestamp;
    protected ParticleEffect effect;
    protected Particle particle;

    public ParticleEvent(String eventId, String eventName, ParticleEffect effect) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.effect = effect;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 获取事件ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 获取事件名称
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * 获取事件时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取相关粒子效果
     */
    public ParticleEffect getEffect() {
        return effect;
    }

    /**
     * 获取相关粒子
     */
    public Particle getParticle() {
        return particle;
    }

    /**
     * 设置相关粒子
     */
    public void setParticle(Particle particle) {
        this.particle = particle;
    }

    @Override
    public String toString() {
        return String.format("ParticleEvent [%s, Name: %s, Time: %d]",
                eventId, eventName, timestamp);
    }
}
