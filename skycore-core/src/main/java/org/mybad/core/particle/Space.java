package org.mybad.core.particle;

import java.util.*;

/**
 * 粒子空间 - 粒子运行的环境
 * 定义碰撞、物理、边界等环境参数
 */
public class Space {

    private String spaceId;
    private String spaceName;

    // 环境参数
    private float gravityX = 0;
    private float gravityY = -9.8f;
    private float gravityZ = 0;

    private float airResistance = 0.98f;  // 空气阻力系数

    // 边界
    private float minX = Float.NEGATIVE_INFINITY;
    private float maxX = Float.POSITIVE_INFINITY;
    private float minY = Float.NEGATIVE_INFINITY;
    private float maxY = Float.POSITIVE_INFINITY;
    private float minZ = Float.NEGATIVE_INFINITY;
    private float maxZ = Float.POSITIVE_INFINITY;

    // 碰撞配置
    private boolean enableCollision = false;
    private CollisionBehavior collisionBehavior = CollisionBehavior.DESTROY;

    // 环境对象
    private List<SpaceObject> objects;
    private Map<String, SpaceObject> objectMap;

    public Space(String spaceId, String spaceName) {
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.objects = new ArrayList<>();
        this.objectMap = new HashMap<>();
    }

    /**
     * 添加环境对象（如碰撞体）
     */
    public void addObject(SpaceObject obj) {
        if (obj != null) {
            objects.add(obj);
            objectMap.put(obj.getObjectId(), obj);
        }
    }

    /**
     * 移除环境对象
     */
    public void removeObject(String objectId) {
        SpaceObject obj = objectMap.remove(objectId);
        if (obj != null) {
            objects.remove(obj);
        }
    }

    /**
     * 获取环境对象
     */
    public SpaceObject getObject(String objectId) {
        return objectMap.get(objectId);
    }

    /**
     * 设置重力
     */
    public void setGravity(float gx, float gy, float gz) {
        this.gravityX = gx;
        this.gravityY = gy;
        this.gravityZ = gz;
    }

    /**
     * 设置空气阻力
     */
    public void setAirResistance(float resistance) {
        this.airResistance = Math.max(0, Math.min(1, resistance));
    }

    /**
     * 设置边界
     */
    public void setBounds(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    /**
     * 设置碰撞检测
     */
    public void setEnableCollision(boolean enable) {
        this.enableCollision = enable;
    }

    /**
     * 检查粒子是否在边界内
     */
    public boolean isInBounds(Particle particle) {
        float x = particle.getPositionX();
        float y = particle.getPositionY();
        float z = particle.getPositionZ();

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    /**
     * 应用环境影响
     */
    public void applyEnvironment(Particle particle, float deltaTime) {
        // 应用重力
        float accelerationY = particle.getAccelerationY() + gravityY;
        particle.setAcceleration(
                particle.getAccelerationX(),
                accelerationY,
                particle.getAccelerationZ()
        );

        // 应用空气阻力
        particle.setVelocity(
                particle.getVelocityX() * airResistance,
                particle.getVelocityY() * airResistance,
                particle.getVelocityZ() * airResistance
        );

        // 检查边界
        if (!isInBounds(particle)) {
            particle.setAlive(false);
        }
    }

    /**
     * 检查碰撞
     */
    public boolean checkCollision(Particle particle) {
        if (!enableCollision) {
            return false;
        }

        for (SpaceObject obj : objects) {
            if (obj.collidesWith(particle)) {
                return true;
            }
        }
        return false;
    }

    // Getters
    public String getSpaceId() { return spaceId; }
    public String getSpaceName() { return spaceName; }
    public float getGravityX() { return gravityX; }
    public float getGravityY() { return gravityY; }
    public float getGravityZ() { return gravityZ; }
    public float getAirResistance() { return airResistance; }
    public boolean isCollisionEnabled() { return enableCollision; }
    public CollisionBehavior getCollisionBehavior() { return collisionBehavior; }

    @Override
    public String toString() {
        return String.format("Space [%s (%s), Gravity: (%.2f, %.2f, %.2f), Objects: %d]",
                spaceId, spaceName, gravityX, gravityY, gravityZ, objects.size());
    }

    /**
     * 空间对象接口（用于碰撞检测）
     */
    public interface SpaceObject {
        String getObjectId();
        boolean collidesWith(Particle particle);
    }

    /**
     * 碰撞行为
     */
    public enum CollisionBehavior {
        DESTROY,        // 销毁粒子
        BOUNCE,         // 反弹
        STICK,          // 粘附
        PASS_THROUGH    // 穿过
    }
}
