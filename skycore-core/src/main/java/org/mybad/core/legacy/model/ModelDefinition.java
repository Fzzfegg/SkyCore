package org.mybad.core.legacy.model;

import org.mybad.core.data.Model;
import java.util.*;

/**
 * 模型定义 - 模型的静态定义（共享的模型数据）
 * ModelDefinition 存储模型的原始结构，可被多个 ModelInstance 共享
 * 用于内存优化和实例复用
 */
public class ModelDefinition {

    private String id;
    private String name;
    private Model baseModel;
    private long creationTime;
    private int instanceCount;
    private Map<String, Object> metadata;

    public ModelDefinition(String id, String name, Model baseModel) {
        this.id = id;
        this.name = name;
        this.baseModel = baseModel;
        this.creationTime = System.currentTimeMillis();
        this.instanceCount = 0;
        this.metadata = new HashMap<>();
    }

    /**
     * 获取模型定义 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取模型名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取基础模型
     */
    public Model getBaseModel() {
        return baseModel;
    }

    /**
     * 创建一个新的模型实例
     */
    public ModelInstance createInstance(String instanceId) {
        ModelInstance instance = new ModelInstance(instanceId, this);
        instanceCount++;
        return instance;
    }

    /**
     * 获取基于此定义创建的实例数量
     */
    public int getInstanceCount() {
        return instanceCount;
    }

    /**
     * 减少实例计数（当实例被销毁时调用）
     */
    public void decrementInstanceCount() {
        if (instanceCount > 0) {
            instanceCount--;
        }
    }

    /**
     * 设置元数据
     */
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 检查元数据是否存在
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * 获取创建时间
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * 获取定义的总大小（估算）
     */
    public long getSize() {
        return baseModel.getAllBones().size() * 100; // 粗略估算
    }

    /**
     * 获取定义信息
     */
    @Override
    public String toString() {
        return String.format("ModelDefinition [%s, Bones: %d, Instances: %d]",
                name, baseModel.getAllBones().size(), instanceCount);
    }
}
