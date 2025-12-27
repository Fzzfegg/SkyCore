package org.mybad.core.particle.components;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.Component;

/**
 * 旋转组件 - 控制粒子旋转
 */
public class RotationComponent extends Component {

    private float rotationSpeedX = 0;
    private float rotationSpeedY = 0;
    private float rotationSpeedZ = 0;

    private float initialRotationX = 0;
    private float initialRotationY = 0;
    private float initialRotationZ = 0;

    public RotationComponent(String componentId, String componentName) {
        super(componentId, componentName);
    }

    @Override
    public void initialize() {
        // 初始化
    }

    @Override
    public void update(float deltaTime) {
        // 更新在apply中处理
    }

    @Override
    public void apply(Particle particle) {
        if (!enabled || particle == null) {
            return;
        }

        float age = particle.getAge();

        float rotX = initialRotationX + rotationSpeedX * age;
        float rotY = initialRotationY + rotationSpeedY * age;
        float rotZ = initialRotationZ + rotationSpeedZ * age;

        particle.setRotation(rotX, rotY, rotZ);
    }

    /**
     * 设置旋转速度
     */
    public void setRotationSpeed(float speedX, float speedY, float speedZ) {
        this.rotationSpeedX = speedX;
        this.rotationSpeedY = speedY;
        this.rotationSpeedZ = speedZ;
    }

    /**
     * 设置初始旋转
     */
    public void setInitialRotation(float rotX, float rotY, float rotZ) {
        this.initialRotationX = rotX;
        this.initialRotationY = rotY;
        this.initialRotationZ = rotZ;
    }

    // Getters
    public float getRotationSpeedX() { return rotationSpeedX; }
    public float getRotationSpeedY() { return rotationSpeedY; }
    public float getRotationSpeedZ() { return rotationSpeedZ; }

    @Override
    public String toString() {
        return String.format("RotationComponent [%s, Speed: (%.2f, %.2f, %.2f)]",
                componentId, rotationSpeedX, rotationSpeedY, rotationSpeedZ);
    }
}
