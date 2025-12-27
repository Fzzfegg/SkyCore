package org.mybad.core.particle.components;

import org.mybad.core.particle.Particle;
import org.mybad.core.particle.Component;

/**
 * 运动组件 - 控制粒子的基础运动
 * 应用速度和加速度
 */
public class MotionComponent extends Component {

    private float gravity = -9.8f;
    private float drag = 0.99f;  // 拖曳系数
    private boolean useGravity = true;

    public MotionComponent(String componentId, String componentName) {
        super(componentId, componentName);
    }

    @Override
    public void initialize() {
        // 初始化
    }

    @Override
    public void update(float deltaTime) {
        // 运动更新在粒子更新时处理
    }

    @Override
    public void apply(Particle particle) {
        if (!enabled || particle == null) {
            return;
        }

        // 应用重力
        if (useGravity) {
            float currentAccelY = particle.getAccelerationY();
            particle.setAcceleration(
                    particle.getAccelerationX(),
                    currentAccelY + gravity,
                    particle.getAccelerationZ()
            );
        }

        // 应用拖曳
        particle.setVelocity(
                particle.getVelocityX() * drag,
                particle.getVelocityY() * drag,
                particle.getVelocityZ() * drag
        );
    }

    /**
     * 设置重力
     */
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    /**
     * 设置拖曳系数
     */
    public void setDrag(float drag) {
        this.drag = Math.max(0, Math.min(1, drag));
    }

    /**
     * 设置是否使用重力
     */
    public void setUseGravity(boolean use) {
        this.useGravity = use;
    }

    // Getters
    public float getGravity() { return gravity; }
    public float getDrag() { return drag; }
    public boolean isUsingGravity() { return useGravity; }

    @Override
    public String toString() {
        return String.format("MotionComponent [%s, Gravity: %.2f, Drag: %.2f]",
                componentId, gravity, drag);
    }
}
