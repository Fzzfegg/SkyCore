package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.core.parsing.AnimationParser;
import org.mybad.core.parsing.ModelParser;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.GeometryCache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源加载器
 * 负责从 assets 目录加载模型、动画文件
 * 支持缓存以提高性能
 */
@SideOnly(Side.CLIENT)
public class ResourceLoader {

    /** 模型缓存: 路径 -> Model */
    private final Map<String, Model> modelCache;

    /** 动画缓存: 路径 -> Animation */
    private final Map<String, Animation> animationCache;

    /** 模型解析器 */
    private final ModelParser modelParser;

    /** 动画解析器 */
    private final AnimationParser animationParser;

    private final GeometryCache geometryCache;

    public ResourceLoader() {
        this.modelCache = new ConcurrentHashMap<>();
        this.animationCache = new ConcurrentHashMap<>();
        this.modelParser = new ModelParser();
        this.animationParser = new AnimationParser();
        this.geometryCache = new GeometryCache();
    }

    /**
     * 加载模型
     * @param path 模型路径 (如: skycore/models/zombie.geo.json)
     * @return Model 对象，如果加载失败返回 null
     */
    public Model loadModel(String path) {
        // 检查缓存
        if (modelCache.containsKey(path)) {
            return modelCache.get(path);
        }

        try {
            String jsonContent = loadResourceAsString(path);
            if (jsonContent == null) {
                SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型文件: {}", path);
                return null;
            }

            Model model = modelParser.parse(jsonContent);
            modelCache.put(path, model);
            return model;

        } catch (Exception e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 解析模型文件失败: {} - {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 加载动画
     * @param path 动画路径 (如: skycore/animations/zombie.animation.json)
     * @return Animation 对象，如果加载失败返回 null
     */
    public Animation loadAnimation(String path) {
        // 检查缓存
        if (animationCache.containsKey(path)) {
            return animationCache.get(path);
        }

        try {
            String jsonContent = loadResourceAsString(path);
            if (jsonContent == null) {
                SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载动画文件: {}", path);
                return null;
            }

            Animation animation = animationParser.parseToAnimation(jsonContent);
            animationCache.put(path, animation);
            return animation;

        } catch (Exception e) {
            SkyCoreMod.LOGGER.error("[SkyCore] 解析动画文件失败: {} - {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 获取纹理的 ResourceLocation
     * @param path 纹理路径
     * @return ResourceLocation
     */
    public ResourceLocation getTextureLocation(String path) {
        return parseResourceLocation(path);
    }

    /**
     * 解析资源路径为 ResourceLocation
     *
     * 支持的格式：
     * 1. "models/zombie.geo.json"          → skycore:models/zombie.geo.json (默认命名空间)
     * 2. "skycore:models/zombie.geo.json"  → skycore:models/zombie.geo.json (Minecraft 标准格式)
     * 3. "minecraft:textures/entity/pig.png" → minecraft:textures/entity/pig.png (其他命名空间)
     */
    private ResourceLocation parseResourceLocation(String path) {
        // 检查是否使用 Minecraft 标准格式 (namespace:path)
        int colonIndex = path.indexOf(':');
        if (colonIndex > 0) {
            String namespace = path.substring(0, colonIndex);
            String resourcePath = path.substring(colonIndex + 1);
            return new ResourceLocation(namespace, resourcePath);
        }

        // 默认使用 skycore 命名空间
        return new ResourceLocation(SkyCoreMod.MOD_ID, path);
    }

    /**
     * 从 assets 加载资源文件为字符串
     */
    private String loadResourceAsString(String path) {
        ResourceLocation location = parseResourceLocation(path);

        try {
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            try (InputStream is = resource.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 资源文件不存在: {} - {}", location, e.getMessage());
            return null;
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        modelCache.clear();
        animationCache.clear();
        geometryCache.clear();
        SkyCoreMod.LOGGER.info("[SkyCore] 资源缓存已清空");
    }

    public GeometryCache getGeometryCache() {
        return geometryCache;
    }

    /**
     * 从缓存中移除指定模型
     */
    public void invalidateModel(String path) {
        modelCache.remove(path);
    }

    /**
     * 从缓存中移除指定动画
     */
    public void invalidateAnimation(String path) {
        animationCache.remove(path);
    }

    /**
     * 获取缓存的模型数量
     */
    public int getCachedModelCount() {
        return modelCache.size();
    }

    /**
     * 获取缓存的动画数量
     */
    public int getCachedAnimationCount() {
        return animationCache.size();
    }
}
