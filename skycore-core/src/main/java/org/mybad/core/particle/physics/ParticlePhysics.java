package org.mybad.core.particle.physics;

import org.mybad.core.particle.Particle;

/**
 * 粒子物理计算 - 提供物理模拟相关计算
 * 包括碰撞、弹性、摩擦等
 */
public class ParticlePhysics {

    private float gravity = -9.8f;
    private float drag = 0.99f;
    private float airDensity = 1.225f;  // kg/m³
    private float windX = 0;
    private float windY = 0;
    private float windZ = 0;

    public ParticlePhysics() {
        // 默认物理参数
    }

    /**
     * 应用重力到粒子
     */
    public void applyGravity(Particle particle, float deltaTime) {
        float currentAccelY = particle.getAccelerationY();
        particle.setAcceleration(
                particle.getAccelerationX(),
                currentAccelY + gravity,
                particle.getAccelerationZ()
        );
    }

    /**
     * 应用空气阻力
     */
    public void applyDrag(Particle particle, float deltaTime) {
        particle.setVelocity(
                particle.getVelocityX() * drag,
                particle.getVelocityY() * drag,
                particle.getVelocityZ() * drag
        );
    }

    /**
     * 应用风力
     */
    public void applyWind(Particle particle, float deltaTime) {
        float scale = particle.getScaleX(); // 使用缩放作为风阻面积代理
        float windForce = 0.001f * scale;

        float newVelX = particle.getVelocityX() + windX * windForce * deltaTime;
        float newVelY = particle.getVelocityY() + windY * windForce * deltaTime;
        float newVelZ = particle.getVelocityZ() + windZ * windForce * deltaTime;

        particle.setVelocity(newVelX, newVelY, newVelZ);
    }

    /**
     * 应用吸引力
     */
    public void applyAttraction(Particle particle, float attractorX, float attractorY, float attractorZ, float force, float maxDistance) {
        float dx = attractorX - particle.getPositionX();
        float dy = attractorY - particle.getPositionY();
        float dz = attractorZ - particle.getPositionZ();

        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > 0.001f && distance <= maxDistance) {
            float strength = force / (distance * distance + 1);

            float velX = particle.getVelocityX() + (dx / distance) * strength;
            float velY = particle.getVelocityY() + (dy / distance) * strength;
            float velZ = particle.getVelocityZ() + (dz / distance) * strength;

            particle.setVelocity(velX, velY, velZ);
        }
    }

    /**
     * 处理碰撞反弹
     */
    public void handleBounce(Particle particle, float bounciness, float normalX, float normalY, float normalZ) {
        // 反射速度向量
        float velX = particle.getVelocityX();
        float velY = particle.getVelocityY();
        float velZ = particle.getVelocityZ();

        float dot = velX * normalX + velY * normalY + velZ * normalZ;
        float reflectX = (velX - 2 * dot * normalX) * bounciness;
        float reflectY = (velY - 2 * dot * normalY) * bounciness;
        float reflectZ = (velZ - 2 * dot * normalZ) * bounciness;

        particle.setVelocity(reflectX, reflectY, reflectZ);
    }

    /**
     * 限制速度
     */
    public void limitVelocity(Particle particle, float maxSpeed) {
        float velX = particle.getVelocityX();
        float velY = particle.getVelocityY();
        float velZ = particle.getVelocityZ();

        float speed = (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);

        if (speed > maxSpeed) {
            float scale = maxSpeed / speed;
            particle.setVelocity(velX * scale, velY * scale, velZ * scale);
        }
    }

    /**
     * 计算动能
     */
    public float getKineticEnergy(Particle particle, float mass) {
        float velX = particle.getVelocityX();
        float velY = particle.getVelocityY();
        float velZ = particle.getVelocityZ();

        float speed = (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);
        return 0.5f * mass * speed * speed;
    }

    /**
     * 计算速度
     */
    public float getVelocity(Particle particle) {
        float velX = particle.getVelocityX();
        float velY = particle.getVelocityY();
        float velZ = particle.getVelocityZ();

        return (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);
    }

    /**
     * 完整的物理更新
     */
    public void update(Particle particle, float deltaTime) {
        applyGravity(particle, deltaTime);
        applyDrag(particle, deltaTime);
        applyWind(particle, deltaTime);

        // 更新速度和位置
        particle.update(deltaTime);
    }

    // Setters
    public void setGravity(float gravity) { this.gravity = gravity; }
    public void setDrag(float drag) { this.drag = Math.max(0, Math.min(1, drag)); }
    public void setAirDensity(float density) { this.airDensity = density; }
    public void setWind(float x, float y, float z) { this.windX = x; this.windY = y; this.windZ = z; }

    // Getters
    public float getGravity() { return gravity; }
    public float getDrag() { return drag; }
    public float getAirDensity() { return airDensity; }

    /**
     * 获取物理统计
     */
    public String getPhysicsInfo() {
        return String.format("ParticlePhysics [Gravity: %.2f, Drag: %.2f, Wind: (%.2f, %.2f, %.2f)]",
                gravity, drag, windX, windY, windZ);
    }

    @Override
    public String toString() {
        return getPhysicsInfo();
    }
}
