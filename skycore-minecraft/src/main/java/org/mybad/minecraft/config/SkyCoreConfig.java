package org.mybad.minecraft.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.mybad.minecraft.SkyCoreMod;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SkyCore 配置管理器
 * 负责加载、保存和管理实体模型映射配置
 * 支持运行时 reload
 */
public class SkyCoreConfig {
    private static SkyCoreConfig instance;

    /** 配置文件路径 */
    private final Path configPath;

    /** 实体名字 -> 模型映射 */
    private final Map<String, EntityModelMapping> mappings;

    /** Gson 实例 */
    private final Gson gson;

    private SkyCoreConfig(Path configDir) {
        this.configPath = configDir.resolve("skycore.json");
        this.mappings = new ConcurrentHashMap<>();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * 初始化配置管理器
     * @param configDir Minecraft config 目录
     */
    public static void init(File configDir) {
        instance = new SkyCoreConfig(configDir.toPath());
        instance.load();
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

        if (!Files.exists(configPath)) {
            // 创建默认配置
            createDefaultConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();

            if (root.has("entities")) {
                JsonArray entities = root.getAsJsonArray("entities");
                Type listType = new TypeToken<List<EntityModelMapping>>(){}.getType();
                List<EntityModelMapping> list = gson.fromJson(entities, listType);

                for (EntityModelMapping mapping : list) {
                    if (mapping.getName() != null && !mapping.getName().isEmpty()) {
                        mappings.put(mapping.getName(), mapping);
                        SkyCoreMod.LOGGER.info("[SkyCore] 加载实体映射: {}", mapping.getName());
                    }
                }
            }

            SkyCoreMod.LOGGER.info("[SkyCore] 配置加载完成，共 {} 个实体映射", mappings.size());

        } catch (IOException e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 加载配置文件失败", e);
        } catch (JsonParseException e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 配置文件格式错误", e);
        }
    }

    /**
     * 重新加载配置（运行时 reload）
     */
    public void reload() {
        SkyCoreMod.LOGGER.info("[SkyCore] 重新加载配置...");
        load();
    }
    

    /**
     * 创建默认配置文件
     */
    private void createDefaultConfig() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject root = new JsonObject();
            JsonArray entities = new JsonArray();

            // 添加示例配置
            // 路径格式：直接写路径（默认 skycore 命名空间）或 "namespace:path" 格式
            JsonObject example = new JsonObject();
            example.addProperty("name", "ExampleEntity");
            example.addProperty("model", "models/example.geo.json");
            example.addProperty("animation", "animations/example.animation.json");
            example.addProperty("texture", "textures/example.png");
            example.addProperty("enableCull", true);  // 是否启用背面剔除，默认为 true
            example.addProperty("scale", 1.0f);  // 模型缩放
            example.addProperty("primaryFadeSeconds", 0.12f);  // 主动画切换淡入淡出时间（秒）
            entities.add(example);

            // 添加禁用背面剔除的示例（用于布料、旗子等需要双面渲染的模型）
            JsonObject example2 = new JsonObject();
            example2.addProperty("name", "BillboardEntity");
            example2.addProperty("model", "models/billboard.geo.json");
            example2.addProperty("animation", "animations/billboard.animation.json");
            example2.addProperty("texture", "textures/billboard.png");
            example2.addProperty("enableCull", false);  // 禁用背面剔除，显示双面
            example2.addProperty("scale", 1.0f);
            example2.addProperty("primaryFadeSeconds", 0.12f);
            entities.add(example2);

            root.add("entities", entities);

            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }

            SkyCoreMod.LOGGER.info("[SkyCore] 已创建默认配置文件: {}", configPath);

        } catch (IOException e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 创建默认配置文件失败", e);
        }
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

    /**
     * 检查是否存在指定名字的映射
     */
    public boolean hasMapping(String entityName) {
        return entityName != null && mappings.containsKey(entityName);
    }

    /**
     * 获取所有映射
     */
    public Collection<EntityModelMapping> getAllMappings() {
        return Collections.unmodifiableCollection(mappings.values());
    }

    /**
     * 获取映射数量
     */
    public int getMappingCount() {
        return mappings.size();
    }

    /**
     * 添加或更新映射
     */
    public void addMapping(EntityModelMapping mapping) {
        if (mapping != null && mapping.getName() != null) {
            mappings.put(mapping.getName(), mapping);
        }
    }

    /**
     * 移除映射
     */
    public void removeMapping(String entityName) {
        mappings.remove(entityName);
    }

    /**
     * 获取配置文件路径
     */
    public Path getConfigPath() {
        return configPath;
    }
}
