package org.mybad.minecraft.config;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SkyCore 配置管理器
 * 负责加载、保存和管理实体模型映射配置
 * 支持运行时 reload
 */
public class SkyCoreConfig {
    private static SkyCoreConfig instance;

    /** 实体名字 -> 模型映射 */
    private final Map<String, EntityModelMapping> mappings = new ConcurrentHashMap<>();
    private RenderConfig renderConfig = new RenderConfig();

    public static final class RenderConfig {
        public float bloomStrength = 0.0f;
        public int bloomRadius = 8;
        public int bloomDownsample = 2;
        public float bloomThreshold = 0.0f;
        public int bloomPasses = 1;
        public float bloomSpread = 1.0f;
        public boolean bloomUseDepth = true;
    }

    private SkyCoreConfig(Path packRoot) {}

    /**
     * 初始化配置管理器
     * @param packRoot SkyCore 资源包根目录
     */
    public static void init(Path packRoot) {
        if (packRoot == null) {
            throw new IllegalStateException("SkyCoreConfig 初始化失败，packRoot 为空");
        }
        instance = new SkyCoreConfig(packRoot);
    }

    /**
     * 获取单例实例
     */
    public static SkyCoreConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SkyCoreConfig 未初始化，请先调用 init()");
        }
        return instance;
    }

    /**
     * 加载配置文件
     */
    public void load() {
        mappings.clear();
        renderConfig = new RenderConfig();
    }

    /**
     * 重新加载配置（运行时 reload）
     */
    public void reload() {
        // No-op in remote-managed mode
    }

 
    /**
     * 根据实体名字获取映射配置
     * @param entityName 实体的自定义名字
     * @return 映射配置，如果没有找到返回 null
     */
    public EntityModelMapping getMapping(String entityName) {
        if (entityName == null || entityName.isEmpty()) {
            return null;
        }
        return mappings.get(entityName);
    }

    public Collection<EntityModelMapping> getAllMappings() {
        return Collections.unmodifiableCollection(mappings.values());
    }
    

    public RenderConfig getRenderConfig() {
        return renderConfig;
    }

    public synchronized void applyRemoteMappings(Map<String, EntityModelMapping> newMappings) {
        mappings.clear();
        if (newMappings != null) {
            mappings.putAll(newMappings);
        }
    }

    public synchronized void applyRenderSettings(float bloomStrength,
                                                 int bloomRadius,
                                                 int bloomDownsample,
                                                 float bloomThreshold,
                                                 int bloomPasses) {
        RenderConfig updated = new RenderConfig();
        updated.bloomStrength = bloomStrength;
        updated.bloomRadius = bloomRadius;
        updated.bloomDownsample = bloomDownsample;
        updated.bloomThreshold = bloomThreshold;
        updated.bloomPasses = bloomPasses;
        updated.bloomSpread = renderConfig.bloomSpread;
        updated.bloomUseDepth = renderConfig.bloomUseDepth;
        renderConfig = updated;
    }

    
}
