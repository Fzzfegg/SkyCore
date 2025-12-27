package org.mybad.core.particle;

import java.util.*;

/**
 * 粒子 - 粒子系统的基本单元
 * 存储粒子的位置、速度、生命周期等运行时数据
 */
public class Particle {

    private String particleId;
    private ParticleEffect effect;

    // 生命周期
    private float age;              // 当前年龄（秒）
    private float maxAge;           // 最大年龄（秒）
    private boolean alive;

    // 运动参数
    private float positionX;
    private float positionY;
    private float positionZ;

    private float velocityX;
    private float velocityY;
    private float velocityZ;

    private float accelerationX;
    private float accelerationY;
    private float accelerationZ;

    // 旋转和缩放
    private float rotationX;
    private float rotationY;
    private float rotationZ;

    private float rotationSpeedX;
    private float rotationSpeedY;
    private float rotationSpeedZ;

    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float scaleZ = 1.0f;

    // 颜色和透明度
    private float colorR = 1.0f;
    private float colorG = 1.0f;
    private float colorB = 1.0f;
    private float colorA = 1.0f;

    // 自定义属性
    private Map<String, Float> customData;

    public Particle(String particleId, ParticleEffect effect, float maxAge) {
        this.particleId = particleId;
        this.effect = effect;
        this.maxAge = maxAge;
        this.age = 0;
        this.alive = true;
        this.customData = new HashMap<>();
    }

    /**
     * 更新粒子
     */
    public void update(float deltaTime) {
        if (!alive) {
            return;
        }

        age += deltaTime;

        // 检查生命周期
        if (age >= maxAge) {
            alive = false;
            return;
        }

        // 更新速度（应用加速度）
        velocityX += accelerationX * deltaTime;
        velocityY += accelerationY * deltaTime;
        velocityZ += accelerationZ * deltaTime;

        // 更新位置（应用速度）
        positionX += velocityX * deltaTime;
        positionY += velocityY * deltaTime;
        positionZ += velocityZ * deltaTime;

        // 更新旋转
        rotationX += rotationSpeedX * deltaTime;
        rotationY += rotationSpeedY * deltaTime;
        rotationZ += rotationSpeedZ * deltaTime;
    }

    /**
     * 获取生命周期进度（0-1）
     */
    public float getProgress() {
        if (maxAge <= 0) {
            return 0;
        }
        return Math.min(1.0f, age / maxAge);
    }

    /**
     * 获取归一化的年龄（0-1）
     */
    public float getNormalizedAge() {
        return getProgress();
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
     * 设置速度
     */
    public void setVelocity(float vx, float vy, float vz) {
        this.velocityX = vx;
        this.velocityY = vy;
        this.velocityZ = vz;
    }

    /**
     * 设置加速度
     */
    public void setAcceleration(float ax, float ay, float az) {
        this.accelerationX = ax;
        this.accelerationY = ay;
        this.accelerationZ = az;
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
     * 设置旋转速度
     */
    public void setRotationSpeed(float rx, float ry, float rz) {
        this.rotationSpeedX = rx;
        this.rotationSpeedY = ry;
        this.rotationSpeedZ = rz;
    }

    /**
     * 设置缩放
     */
    public void setScale(float sx, float sy, float sz) {
        this.scaleX = sx;
        this.scaleY = sy;
        this.scaleZ = sz;
    }

    /**
     * 设置颜色
     */
    public void setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
    }

    /**
     * 设置自定义数据
     */
    public void setCustomData(String key, float value) {
        customData.put(key, value);
    }

    /**
     * 获取自定义数据
     */
    public float getCustomData(String key, float defaultValue) {
        return customData.getOrDefault(key, defaultValue);
    }

    /**
     * 设置最大年龄
     */
    public void setMaxAge(float maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * 重置粒子
     */
    public void reset() {
        age = 0;
        alive = true;
        positionX = positionY = positionZ = 0;
        velocityX = velocityY = velocityZ = 0;
        accelerationX = accelerationY = accelerationZ = 0;
        rotationX = rotationY = rotationZ = 0;
        rotationSpeedX = rotationSpeedY = rotationSpeedZ = 0;
        scaleX = scaleY = scaleZ = 1.0f;
        colorR = colorG = colorB = colorA = 1.0f;
        customData.clear();
    }

    // Getters
    public String getParticleId() { return particleId; }
    public ParticleEffect getEffect() { return effect; }
    public float getAge() { return age; }
    public float getMaxAge() { return maxAge; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public float getPositionX() { return positionX; }
    public float getPositionY() { return positionY; }
    public float getPositionZ() { return positionZ; }

    public float getVelocityX() { return velocityX; }
    public float getVelocityY() { return velocityY; }
    public float getVelocityZ() { return velocityZ; }

    public float getAccelerationX() { return accelerationX; }
    public float getAccelerationY() { return accelerationY; }
    public float getAccelerationZ() { return accelerationZ; }

    public float getRotationX() { return rotationX; }
    public float getRotationY() { return rotationY; }
    public float getRotationZ() { return rotationZ; }

    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }

    public float getColorR() { return colorR; }
    public float getColorG() { return colorG; }
    public float getColorB() { return colorB; }
    public float getColorA() { return colorA; }

    @Override
    public String toString() {
        return String.format("Particle [%s, Age: %.2f/%.2f, Pos: (%.2f, %.2f, %.2f), Alive: %b]",
                particleId, age, maxAge, positionX, positionY, positionZ, alive);
    }
}
