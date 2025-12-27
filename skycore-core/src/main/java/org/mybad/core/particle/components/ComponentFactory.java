package org.mybad.core.particle.components;

import org.mybad.core.particle.Component;
import java.util.*;

/**
 * 组件工厂 - 创建和管理粒子组件
 * 支持创建各种标准组件
 */
public class ComponentFactory {

    private static final Map<String, ComponentCreator> creators = new HashMap<>();

    static {
        // 注册标准组件创建器
        registerComponent("motion", (id, name) -> new MotionComponent(id, name));
        registerComponent("color", (id, name) -> new ColorComponent(id, name));
        registerComponent("scale", (id, name) -> new ScaleComponent(id, name));
        registerComponent("rotation", (id, name) -> new RotationComponent(id, name));
        registerComponent("lifetime", (id, name) -> new LifetimeComponent(id, name));
        registerComponent("initial_speed", (id, name) -> new InitialSpeedComponent(id, name));
        registerComponent("initial_spin", (id, name) -> new InitialSpinComponent(id, name));
        registerComponent("collision", (id, name) -> new CollisionComponent(id, name));
        registerComponent("sub_emitter", (id, name) -> new SubEmitterComponent(id, name));
        registerComponent("physics", (id, name) -> new PhysicsComponent(id, name));
        registerComponent("attractor", (id, name) -> new AttractorComponent(id, name));
        registerComponent("billboard", (id, name) -> new BillboardComponent(id, name));
        registerComponent("lighting", (id, name) -> new LightingComponent(id, name));
        registerComponent("texture", (id, name) -> new TextureComponent(id, name));
    }

    /**
     * 创建组件
     */
    public static Component createComponent(String componentType, String componentId, String componentName) {
        ComponentCreator creator = creators.get(componentType.toLowerCase());
        if (creator != null) {
            return creator.create(componentId, componentName);
        }
        return createCustomComponent(componentType, componentId, componentName);
    }

    /**
     * 创建自定义组件
     */
    private static Component createCustomComponent(String componentType, String componentId, String componentName) {
        // 返回通用组件
        return new CustomComponent(componentId, componentName, componentType);
    }

    /**
     * 注册自定义组件创建器
     */
    public static void registerComponent(String componentType, ComponentCreator creator) {
        creators.put(componentType.toLowerCase(), creator);
    }

    /**
     * 获取所有支持的组件类型
     */
    public static List<String> getSupportedComponentTypes() {
        return new ArrayList<>(creators.keySet());
    }

    /**
     * 组件创建器接口
     */
    public interface ComponentCreator {
        Component create(String componentId, String componentName);
    }

    /**
     * 通用组件 - 用于不支持的组件类型
     */
    public static class CustomComponent extends Component {
        private String componentType;

        public CustomComponent(String componentId, String componentName, String componentType) {
            super(componentId, componentName);
            this.componentType = componentType;
        }

        @Override
        public void initialize() {
            // 初始化
        }

        @Override
        public void update(float deltaTime) {
            // 更新
        }

        @Override
        public void apply(org.mybad.core.particle.Particle particle) {
            // 应用
        }

        public String getComponentType() { return componentType; }
    }
}
