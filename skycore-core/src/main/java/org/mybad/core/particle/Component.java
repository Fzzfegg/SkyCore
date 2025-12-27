package org.mybad.core.particle;

/**
 * 组件基类 - 粒子系统的可重用组件
 * 提供扩展粒子行为的基础接口
 * Phase 2及以后会实现具体的组件（外观、运动、生命周期等）
 */
public abstract class Component {

    protected String componentId;
    protected String componentName;
    protected boolean enabled = true;

    public Component(String componentId, String componentName) {
        this.componentId = componentId;
        this.componentName = componentName;
    }

    /**
     * 初始化组件
     */
    public void initialize() {
        // 子类可以覆盖
    }

    /**
     * 更新组件
     */
    public abstract void update(float deltaTime);

    /**
     * 应用组件效果到粒子
     */
    public abstract void apply(Particle particle);

    /**
     * 清理资源
     */
    public void dispose() {
        // 子类可以覆盖
    }

    /**
     * 启用/禁用组件
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 检查组件是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 重置组件
     */
    public void reset() {
        // 子类可以覆盖
    }

    // Getters
    public String getComponentId() { return componentId; }
    public String getComponentName() { return componentName; }

    @Override
    public String toString() {
        return String.format("Component [%s (%s), Enabled: %b]",
                componentId, componentName, enabled);
    }
}
