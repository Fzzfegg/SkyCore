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
    private RenderConfig renderConfig = new RenderConfig();

    public static final class RenderConfig {
        public float bloomStrength = 0.0f;
        public int bloomRadius = 8;
        public int bloomDownsample = 2;
        public float bloomThreshold = 0.0f;
    }

    private static final class ConfigFile {
        RenderConfig render = new RenderConfig();
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
        if (file.render != null) {
            renderConfig = file.render;
        } else {
            renderConfig = new RenderConfig();
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

    

    public RenderConfig getRenderConfig() {
        return renderConfig;
    }

    
}
