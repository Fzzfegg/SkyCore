package org.mybad.core.particle.events;

import org.mybad.core.particle.Emitter;
import org.mybad.core.particle.ParticleEffect;

/**
 * 发射器事件基类
 */
public abstract class EmitterEvent {

    protected String eventId;
    protected String eventName;
    protected long timestamp;
    protected Emitter emitter;
    protected ParticleEffect effect;

    public EmitterEvent(String eventId, String eventName, Emitter emitter, ParticleEffect effect) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.emitter = emitter;
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
     * 获取时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取发射器
     */
    public Emitter getEmitter() {
        return emitter;
    }

    /**
     * 获取粒子效果
     */
    public ParticleEffect getEffect() {
        return effect;
    }

    @Override
    public String toString() {
        return String.format("EmitterEvent [%s, Name: %s]",
                eventId, eventName);
    }

    /**
     * 发射器启动事件
     */
    public static class EmitterStartEvent extends EmitterEvent {
        public EmitterStartEvent(String eventId, Emitter emitter, ParticleEffect effect) {
            super(eventId, "EmitterStart", emitter, effect);
        }

        @Override
        public String toString() {
            return String.format("EmitterStartEvent [Emitter: %s, Rate: %.2f/s]",
                    emitter.getEmitterId(), emitter.getEmissionRate());
        }
    }

    /**
     * 发射器停止事件
     */
    public static class EmitterStopEvent extends EmitterEvent {
        private int particlesEmitted;

        public EmitterStopEvent(String eventId, Emitter emitter, ParticleEffect effect) {
            super(eventId, "EmitterStop", emitter, effect);
            this.particlesEmitted = emitter.getParticlesEmitted();
        }

        /**
         * 获取发射的粒子数
         */
        public int getParticlesEmitted() {
            return particlesEmitted;
        }

        @Override
        public String toString() {
            return String.format("EmitterStopEvent [Emitter: %s, Emitted: %d particles]",
                    emitter.getEmitterId(), particlesEmitted);
        }
    }

    /**
     * 发射器爆发事件
     */
    public static class EmitterBurstEvent extends EmitterEvent {
        private int burstCount;

        public EmitterBurstEvent(String eventId, Emitter emitter, ParticleEffect effect, int burstCount) {
            super(eventId, "EmitterBurst", emitter, effect);
            this.burstCount = burstCount;
        }

        /**
         * 获取爆发粒子数
         */
        public int getBurstCount() {
            return burstCount;
        }

        @Override
        public String toString() {
            return String.format("EmitterBurstEvent [Emitter: %s, BurstCount: %d]",
                    emitter.getEmitterId(), burstCount);
        }
    }
}
