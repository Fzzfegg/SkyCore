package org.mybad.minecraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.mybad.minecraft.SkyCoreMod;

import java.io.*;
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

    private static final class ConfigFile {
        List<EntityModelMapping> entities = new ArrayList<>();
    }

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

        ConfigFile file = readConfigFile();
        if (file == null) {
            createDefaultConfig();
            return;
        }
        if (file.entities != null) {
            for (EntityModelMapping mapping : file.entities) {
                if (mapping != null && mapping.getName() != null && !mapping.getName().isEmpty()) {
                    mappings.put(mapping.getName(), mapping);
                    SkyCoreMod.LOGGER.info("[SkyCore] 加载实体映射: {}", mapping.getName());
                }
            }
        }
        SkyCoreMod.LOGGER.info("[SkyCore] 配置加载完成，共 {} 个实体映射", mappings.size());
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
        ConfigFile file = new ConfigFile();
        file.entities.add(buildExampleMapping());
        file.entities.add(buildBillboardExampleMapping());
        if (writeConfigFile(file)) {
            SkyCoreMod.LOGGER.info("[SkyCore] 已创建默认配置文件: {}", configPath);
        }
    }

    private ConfigFile readConfigFile() {
        if (!Files.exists(configPath)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, ConfigFile.class);
        } catch (IOException e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 加载配置文件失败", e);
        } catch (JsonParseException e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 配置文件格式错误", e);
        }
        return null;
    }

    private boolean writeConfigFile(ConfigFile file) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                gson.toJson(file, writer);
            }
            return true;
        } catch (IOException e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 创建默认配置文件失败", e);
            return false;
        }
    }

    private EntityModelMapping buildExampleMapping() {
        EntityModelMapping example = new EntityModelMapping();
        example.setName("ExampleEntity");
        example.setModel("models/example.geo.json");
        example.setAnimation("animations/example.animation.json");
        example.setTexture("textures/example.png");
        example.setEmissive("textures/example_emissive.png");
        example.setEmissiveStrength(1.0f);
        example.setBloom("textures/example_bloom.png");
        example.setBloomStrength(0.0f);
        example.setBloomRadius(8);
        example.setBloomDownsample(2);
        example.setBloomThreshold(0.0f);
        example.setEnableCull(true);
        example.setModelScale(1.0f);
        example.setPrimaryFadeSeconds(0.12f);
        return example;
    }

    private EntityModelMapping buildBillboardExampleMapping() {
        EntityModelMapping example = new EntityModelMapping();
        example.setName("BillboardEntity");
        example.setModel("models/billboard.geo.json");
        example.setAnimation("animations/billboard.animation.json");
        example.setTexture("textures/billboard.png");
        example.setEmissive("textures/billboard_emissive.png");
        example.setEmissiveStrength(1.0f);
        example.setBloom("textures/billboard_bloom.png");
        example.setBloomStrength(0.0f);
        example.setBloomRadius(8);
        example.setBloomDownsample(2);
        example.setBloomThreshold(0.0f);
        example.setEnableCull(false);
        example.setModelScale(1.0f);
        example.setPrimaryFadeSeconds(0.12f);
        return example;
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
