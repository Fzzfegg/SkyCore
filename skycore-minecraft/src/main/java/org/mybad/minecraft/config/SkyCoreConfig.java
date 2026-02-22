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

    /** 原始映射表（用于枚举/调试） */
    private final Map<String, EntityModelMapping> mappings = new ConcurrentHashMap<>();
    /** 大小写无关查找索引 */
    private final Map<String, EntityModelMapping> mappingLookup = new ConcurrentHashMap<>();

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

    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * 加载配置文件
     */
    public void load() {
        mappings.clear();
        mappingLookup.clear();
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
        EntityModelMapping exact = mappingLookup.get(entityName);
        if (exact != null) {
            return exact;
        }
        String lower = entityName.toLowerCase(java.util.Locale.ROOT);
        EntityModelMapping normalized = mappingLookup.get(lower);
        if (normalized != null) {
            return normalized;
        }
        for (Map.Entry<String, EntityModelMapping> entry : mappingLookup.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(entityName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Collection<EntityModelMapping> getAllMappings() {
        return Collections.unmodifiableCollection(mappings.values());
    }
    public synchronized void applyRemoteMappings(Map<String, EntityModelMapping> newMappings) {
        mappings.clear();
        mappingLookup.clear();
        if (newMappings != null) {
            for (Map.Entry<String, EntityModelMapping> entry : newMappings.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                mappings.put(entry.getKey(), entry.getValue());
                mappingLookup.put(entry.getKey(), entry.getValue());
                mappingLookup.put(entry.getKey().toLowerCase(java.util.Locale.ROOT), entry.getValue());
            }
        }
    }

    
}
