package org.mybad.core.particle;

import java.util.*;

/**
 * 发射器 - 粒子的发射源
 * 控制粒子的生成速率、初始参数、生命周期等
 */
public class Emitter {

    private String emitterId;
    private String emitterName;

    // 位置和朝向
    private float positionX;
    private float positionY;
    private float positionZ;

    private float rotationX;
    private float rotationY;
    private float rotationZ;

    // 发射参数
    private float emissionRate;         // 每秒发射粒子数
    private int burstSize;              // 爆发数量
    private float burstInterval;        // 爆发间隔（秒）

    // 粒子初始参数范围
    private float minLifetime = 1.0f;
    private float maxLifetime = 2.0f;

    private float minSpeedX = -1.0f;
    private float maxSpeedX = 1.0f;
    private float minSpeedY = 0.0f;
    private float maxSpeedY = 2.0f;
    private float minSpeedZ = -1.0f;
    private float maxSpeedZ = 1.0f;

    private float minScale = 1.0f;
    private float maxScale = 1.0f;

    // 发射形状
    private EmitterShape shape = EmitterShape.POINT;
    private float shapeSize = 1.0f;     // 形状大小参数

    // 状态
    private boolean active = true;
    private float burstTimer = 0;
    private float emissionAccumulator = 0;
    private int particlesEmitted = 0;

    // 持续时间
    private float lifetime = -1.0f;     // -1 = 无限制
    private float currentTime = 0;
    private boolean looping = false;

    // 组件和修饰器
    private List<Component> components;
    private Map<String, Object> customData;

    public Emitter(String emitterId, String emitterName) {
        this.emitterId = emitterId;
        this.emitterName = emitterName;
        this.components = new ArrayList<>();
        this.customData = new HashMap<>();
    }

    /**
     * 更新发射器
     */
    public void update(float deltaTime) {
        if (!active) {
            return;
        }

        currentTime += deltaTime;

        // 检查生命周期
        if (lifetime > 0 && currentTime >= lifetime) {
            if (looping) {
                currentTime = 0;
            } else {
                active = false;
                return;
            }
        }

        // 更新爆发计时器
        if (burstSize > 0 && burstInterval > 0) {
            burstTimer += deltaTime;
            if (burstTimer >= burstInterval) {
                burstTimer = 0;
                // 爆发产生粒子的信号会在ParticleSystem处理
            }
        }

        // 更新持续发射计时器
        emissionAccumulator += emissionRate * deltaTime;

        // 更新所有组件
        for (Component component : components) {
            component.update(this, deltaTime);
        }
    }

    /**
     * 计算此帧应生成的粒子数
     */
    public int getParticlesToEmit() {
        int count = (int) emissionAccumulator;
        emissionAccumulator -= count;
        return count;
    }

    /**
     * 是否应该爆发
     */
    public boolean shouldBurst() {
        return burstTimer <= 0 && burstSize > 0;
    }

    /**
     * 添加组件
     */
    public void addComponent(Component component) {
        if (component != null) {
            components.add(component);
        }
    }

    /**
     * 获取所有组件
     */
    public List<Component> getComponents() {
        return new ArrayList<>(components);
    }

    /**
     * 设置自定义数据
     */
    public void setCustomData(String key, Object value) {
        customData.put(key, value);
    }

    /**
     * 获取自定义数据
     */
    public Object getCustomData(String key) {
        return customData.get(key);
    }

    /**
     * 设置位置
     */
    public void setPosition(float x, float y, float z) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
    }

    /**
     * 设置旋转
     */
    public void setRotation(float rx, float ry, float rz) {
        this.rotationX = rx;
        this.rotationY = ry;
        this.rotationZ = rz;
    }

    /**
     * 设置初始速度范围
     */
    public void setSpeedRange(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        this.minSpeedX = minX;
        this.maxSpeedX = maxX;
        this.minSpeedY = minY;
        this.maxSpeedY = maxY;
        this.minSpeedZ = minZ;
        this.maxSpeedZ = maxZ;
    }

    /**
     * 设置生命周期范围
     */
    public void setLifetimeRange(float min, float max) {
        this.minLifetime = min;
        this.maxLifetime = max;
    }

    /**
     * 设置缩放范围
     */
    public void setScaleRange(float min, float max) {
        this.minScale = min;
        this.maxScale = max;
    }

    /**
     * 重置发射器
     */
    public void reset() {
        currentTime = 0;
        burstTimer = 0;
        emissionAccumulator = 0;
        particlesEmitted = 0;
    }

    /**
     * 启动发射
     */
    public void start() {
        active = true;
        reset();
    }

    /**
     * 停止发射
     */
    public void stop() {
        active = false;
    }

    // Getters and Setters
    public String getEmitterId() { return emitterId; }
    public String getEmitterName() { return emitterName; }

    public float getPositionX() { return positionX; }
    public float getPositionY() { return positionY; }
    public float getPositionZ() { return positionZ; }

    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }

    public float getEmissionRate() { return emissionRate; }
    public void setEmissionRate(float rate) { this.emissionRate = rate; }

    public int getBurstSize() { return burstSize; }
    public void setBurstSize(int size) { this.burstSize = size; }

    public float getBurstInterval() { return burstInterval; }
    public void setBurstInterval(float interval) { this.burstInterval = interval; }

    public float getMinLifetime() { return minLifetime; }
    public float getMaxLifetime() { return maxLifetime; }

    public float getMinSpeedX() { return minSpeedX; }
    public float getMaxSpeedX() { return maxSpeedX; }
    public float getMinSpeedY() { return minSpeedY; }
    public float getMaxSpeedY() { return maxSpeedY; }
    public float getMinSpeedZ() { return minSpeedZ; }
    public float getMaxSpeedZ() { return maxSpeedZ; }

    public float getMinScale() { return minScale; }
    public float getMaxScale() { return maxScale; }

    public EmitterShape getShape() { return shape; }
    public void setShape(EmitterShape shape) { this.shape = shape; }

    public float getShapeSize() { return shapeSize; }
    public void setShapeSize(float size) { this.shapeSize = size; }

    public boolean isActive() { return active; }

    public float getLifetime() { return lifetime; }
    public void setLifetime(float lifetime) { this.lifetime = lifetime; }

    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }

    public int getParticlesEmitted() { return particlesEmitted; }
    public void incrementParticlesEmitted(int count) { this.particlesEmitted += count; }
    public void setParticlesEmitted(int count) { this.particlesEmitted = count; }

    @Override
    public String toString() {
        return String.format("Emitter [%s (%s), Position: (%.2f, %.2f, %.2f), Rate: %.2f/s, Active: %b]",
                emitterId, emitterName, positionX, positionY, positionZ, emissionRate, active);
    }

    /**
     * 发射器形状
     */
    public enum EmitterShape {
        POINT,              // 点发射
        DISC,               // 圆盘
        BOX,                // 盒子
        SPHERE,             // 球体
        CYLINDER,           // 圆柱
        ENTITY_AABB         // 实体包围盒
    }

    /**
     * 组件接口
     */
    public interface Component {
        void update(Emitter emitter, float deltaTime);
    }
}
