package org.mybad.core.particle;

import java.util.*;

/**
 * 粒子系统 - 粒子系统的主要管理器
 * 负责粒子生命周期、发射器管理、渲染等
 */
public class ParticleSystem {

    private String systemId;
    private String systemName;

    // 资源管理
    private ParticlePool particlePool;
    private Map<String, ParticleEffect> effects;
    private Map<String, Emitter> activeEmitters;
    private Map<String, Space> spaces;

    // 粒子管理
    private Set<Particle> activeParticles;
    private List<Particle> particlesToKill;

    // 性能配置
    private int maxParticles = 10000;
    private boolean useObjectPool = true;
    private float updateRate = 60.0f;    // 更新频率（Hz）
    private float lastUpdateTime = 0;

    // 统计信息
    private long particlesCreated = 0;
    private long particlesKilled = 0;
    private float averageFrameTime = 0;

    // 事件监听
    private List<ParticleSystemListener> listeners;

    // 状态
    private boolean running = false;
    private float systemTime = 0;

    public ParticleSystem(String systemId, String systemName) {
        this.systemId = systemId;
        this.systemName = systemName;
        this.effects = new HashMap<>();
        this.activeEmitters = new HashMap<>();
        this.spaces = new HashMap<>();
        this.activeParticles = new HashSet<>();
        this.particlesToKill = new ArrayList<>();
        this.listeners = new ArrayList<>();

        // 初始化对象池
        if (useObjectPool) {
            this.particlePool = new ParticlePool(systemId + "_pool", 1000, maxParticles);
        }
    }

    /**
     * 注册粒子效果
     */
    public void registerEffect(ParticleEffect effect) {
        if (effect != null && effect.validate()) {
            effects.put(effect.getEffectId(), effect);
        }
    }

    /**
     * 获取粒子效果
     */
    public ParticleEffect getEffect(String effectId) {
        return effects.get(effectId);
    }

    /**
     * 播放粒子效果
     */
    public void playEffect(String effectId, float x, float y, float z) {
        ParticleEffect effect = getEffect(effectId);
        if (effect == null) {
            return;
        }

        // 播放所有发射器
        for (Emitter emitter : effect.getEmitters()) {
            Emitter newEmitter = new Emitter(emitter.getEmitterId() + "_" + System.nanoTime(),
                    emitter.getEmitterName());

            // 复制发射器配置
            newEmitter.setPosition(x + emitter.getPositionX(), y + emitter.getPositionY(), z + emitter.getPositionZ());
            newEmitter.setRotation(emitter.getRotationX(), emitter.getRotationY(), emitter.getRotationZ());
            newEmitter.setEmissionRate(emitter.getEmissionRate());
            newEmitter.setBurstSize(emitter.getBurstSize());
            newEmitter.setBurstInterval(emitter.getBurstInterval());
            newEmitter.setLifetimeRange(emitter.getMinLifetime(), emitter.getMaxLifetime());
            newEmitter.setSpeedRange(emitter.getMinSpeedX(), emitter.getMaxSpeedX(),
                    emitter.getMinSpeedY(), emitter.getMaxSpeedY(),
                    emitter.getMinSpeedZ(), emitter.getMaxSpeedZ());
            newEmitter.setScaleRange(emitter.getMinScale(), emitter.getMaxScale());
            newEmitter.setShape(emitter.getShape());
            newEmitter.setShapeSize(emitter.getShapeSize());

            addEmitter(newEmitter, effect);
        }
    }

    /**
     * 添加发射器
     */
    private void addEmitter(Emitter emitter, ParticleEffect effect) {
        activeEmitters.put(emitter.getEmitterId(), emitter);
        emitter.start();

        fireEmitterEvent("start", emitter);
    }

    /**
     * 移除发射器
     */
    public void removeEmitter(String emitterId) {
        Emitter emitter = activeEmitters.remove(emitterId);
        if (emitter != null) {
            fireEmitterEvent("stop", emitter);
        }
    }

    /**
     * 注册空间
     */
    public void registerSpace(Space space) {
        if (space != null) {
            spaces.put(space.getSpaceId(), space);
        }
    }

    /**
     * 获取空间
     */
    public Space getSpace(String spaceId) {
        return spaces.get(spaceId);
    }

    /**
     * 更新粒子系统
     */
    public void update(float deltaTime) {
        if (!running) {
            return;
        }

        systemTime += deltaTime;
        long startTime = System.nanoTime();

        // 更新所有发射器
        updateEmitters(deltaTime);

        // 生成新粒子
        emitParticles();

        // 更新粒子
        updateParticles(deltaTime);

        // 清理死粒子
        killDeadParticles();

        // 计算性能指标
        long endTime = System.nanoTime();
        float frameTime = (endTime - startTime) / 1_000_000.0f; // 转换为毫秒
        averageFrameTime = averageFrameTime * 0.95f + frameTime * 0.05f; // 移动平均
    }

    /**
     * 更新所有发射器
     */
    private void updateEmitters(float deltaTime) {
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Emitter> entry : activeEmitters.entrySet()) {
            Emitter emitter = entry.getValue();
            emitter.update(deltaTime);

            if (!emitter.isActive()) {
                toRemove.add(entry.getKey());
            }
        }

        // 移除不活跃的发射器
        for (String emitterId : toRemove) {
            removeEmitter(emitterId);
        }
    }

    /**
     * 生成新粒子
     */
    private void emitParticles() {
        for (Emitter emitter : activeEmitters.values()) {
            // 持续发射
            int toEmit = emitter.getParticlesToEmit();
            for (int i = 0; i < toEmit && activeParticles.size() < maxParticles; i++) {
                createParticle(emitter);
            }

            // 爆发发射
            if (emitter.shouldBurst()) {
                for (int i = 0; i < emitter.getBurstSize() && activeParticles.size() < maxParticles; i++) {
                    createParticle(emitter);
                }
            }
        }
    }

    /**
     * 创建单个粒子
     */
    private void createParticle(Emitter emitter) {
        if (activeParticles.size() >= maxParticles) {
            return;
        }

        float lifetime = emitter.getMinLifetime() +
                (float) Math.random() * (emitter.getMaxLifetime() - emitter.getMinLifetime());

        Particle particle;
        if (useObjectPool) {
            particle = particlePool.acquire(null, lifetime);
        } else {
            particle = new Particle("particle_" + particlesCreated++, null, lifetime);
        }

        if (particle == null) {
            return;
        }

        // 设置初始位置
        particle.setPosition(emitter.getPositionX(), emitter.getPositionY(), emitter.getPositionZ());

        // 设置初始速度
        float vx = emitter.getMinSpeedX() + (float) Math.random() * (emitter.getMaxSpeedX() - emitter.getMinSpeedX());
        float vy = emitter.getMinSpeedY() + (float) Math.random() * (emitter.getMaxSpeedY() - emitter.getMinSpeedY());
        float vz = emitter.getMinSpeedZ() + (float) Math.random() * (emitter.getMaxSpeedZ() - emitter.getMinSpeedZ());
        particle.setVelocity(vx, vy, vz);

        // 设置初始缩放
        float scale = emitter.getMinScale() + (float) Math.random() * (emitter.getMaxScale() - emitter.getMinScale());
        particle.setScale(scale, scale, scale);

        activeParticles.add(particle);
        emitter.incrementParticlesEmitted(1);
        particlesCreated++;

        fireParticleEvent("spawn", particle);
    }

    /**
     * 更新所有粒子
     */
    private void updateParticles(float deltaTime) {
        for (Particle particle : activeParticles) {
            particle.update(deltaTime);

            // 应用环境影响
            Space space = getSpace("default");
            if (space != null) {
                space.applyEnvironment(particle, deltaTime);
            }

            // 检查碰撞
            if (space != null && space.isCollisionEnabled() && space.checkCollision(particle)) {
                particle.setAlive(false);
            }

            // 检查是否死亡
            if (!particle.isAlive()) {
                particlesToKill.add(particle);
            }
        }
    }

    /**
     * 清理死粒子
     */
    private void killDeadParticles() {
        for (Particle particle : particlesToKill) {
            activeParticles.remove(particle);

            if (useObjectPool) {
                particlePool.release(particle);
            }

            particlesKilled++;
            fireParticleEvent("death", particle);
        }
        particlesToKill.clear();
    }

    /**
     * 启动系统
     */
    public void start() {
        running = true;
        systemTime = 0;
    }

    /**
     * 停止系统
     */
    public void stop() {
        running = false;
    }

    /**
     * 清空所有粒子
     */
    public void clear() {
        activeParticles.clear();
        activeEmitters.clear();
        if (useObjectPool) {
            particlePool.clear();
        }
        particlesToKill.clear();
    }

    /**
     * 添加监听器
     */
    public void addListener(ParticleSystemListener listener) {
        listeners.add(listener);
    }

    /**
     * 移除监听器
     */
    public void removeListener(ParticleSystemListener listener) {
        listeners.remove(listener);
    }

    /**
     * 触发粒子事件
     */
    private void fireParticleEvent(String eventType, Particle particle) {
        for (ParticleSystemListener listener : listeners) {
            listener.onParticleEvent(this, eventType, particle);
        }
    }

    /**
     * 触发发射器事件
     */
    private void fireEmitterEvent(String eventType, Emitter emitter) {
        for (ParticleSystemListener listener : listeners) {
            listener.onEmitterEvent(this, eventType, emitter);
        }
    }

    /**
     * 获取系统统计信息
     */
    public String getSystemStats() {
        return String.format(
                "ParticleSystem [%s, Active: %d, Created: %d, Killed: %d, Emitters: %d, AvgFrameTime: %.2fms]",
                systemId, activeParticles.size(), particlesCreated, particlesKilled,
                activeEmitters.size(), averageFrameTime);
    }

    // Getters
    public String getSystemId() { return systemId; }
    public String getSystemName() { return systemName; }
    public int getActiveParticleCount() { return activeParticles.size(); }
    public Collection<Particle> getActiveParticles() { return activeParticles; }
    public int getEmitterCount() { return activeEmitters.size(); }
    public int getEffectCount() { return effects.size(); }
    public float getSystemTime() { return systemTime; }
    public float getAverageFrameTime() { return averageFrameTime; }
    public boolean isRunning() { return running; }
    public ParticlePool getParticlePool() { return particlePool; }

    @Override
    public String toString() {
        return getSystemStats();
    }

    /**
     * 粒子系统监听器
     */
    public interface ParticleSystemListener {
        void onParticleEvent(ParticleSystem system, String eventType, Particle particle);
        void onEmitterEvent(ParticleSystem system, String eventType, Emitter emitter);
    }
}
