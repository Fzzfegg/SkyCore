package org.mybad.minecraft.config;

/**
 * 实体模型映射配置
 * 定义实体名字与模型/动画/纹理的对应关系
 */
public class EntityModelMapping {
    /** 实体自定义名字 */
    private String name;

    /** 模型文件路径 (如: skycore/models/zombie.geo.json) */
    private String model;

    /** 动画文件路径 (如: skycore/animations/zombie.animation.json) */
    private String animation;

    /** 纹理文件路径 (如: skycore/textures/zombie.png) */
    private String texture;

    public EntityModelMapping() {}

    public EntityModelMapping(String name, String model, String animation, String texture) {
        this.name = name;
        this.model = model;
        this.animation = animation;
        this.texture = texture;
    }

    // Getters
    public String getName() { return name; }
    public String getModel() { return model; }
    public String getAnimation() { return animation; }
    public String getTexture() { return texture; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setModel(String model) { this.model = model; }
    public void setAnimation(String animation) { this.animation = animation; }
    public void setTexture(String texture) { this.texture = texture; }

    @Override
    public String toString() {
        return "EntityModelMapping{" +
                "name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", animation='" + animation + '\'' +
                ", texture='" + texture + '\'' +
                '}';
    }
}
