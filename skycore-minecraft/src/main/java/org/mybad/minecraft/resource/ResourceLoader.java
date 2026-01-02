package org.mybad.minecraft.resource;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import gg.moonflower.pinwheel.particle.ParticleData;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.GeometryCache;

import java.util.Map;

/**
 * 资源加载器
 * 负责从 assets 目录加载模型、动画文件
 * 支持缓存以提高性能
 */
@SideOnly(Side.CLIENT)
public class ResourceLoader {

    private final ModelResourceLoader modelLoader;
    private final AnimationResourceLoader animationLoader;
    private final ParticleResourceLoader particleLoader;
    private final ResourcePathResolver pathResolver;

    private final GeometryCache geometryCache;

    public ResourceLoader() {
        this.modelLoader = new ModelResourceLoader(this);
        this.animationLoader = new AnimationResourceLoader(this);
        this.particleLoader = new ParticleResourceLoader(this);
        this.pathResolver = new ResourcePathResolver();
        this.geometryCache = new GeometryCache();
    }

    /**
     * 加载模型
     * @param path 模型路径 (如: skycore/models/zombie.geo.json)
     * @return Model 对象，如果加载失败返回 null
     */
    public Model loadModel(String path) {
        return modelLoader.loadModel(path);
    }

    /**
     * 加载动画
     * @param path 动画路径 (如: skycore/animations/zombie.animation.json)
     * @return Animation 对象，如果加载失败返回 null
     */
    public Animation loadAnimation(String path) {
        return animationLoader.loadAnimation(path);
    }

    public Animation loadAnimation(String path, String clipName) {
        return animationLoader.loadAnimation(path, clipName);
    }

    /**
     * 获取纹理的 ResourceLocation
     * @param path 纹理路径
     * @return ResourceLocation
     */
    public ResourceLocation getTextureLocation(String path) {
        return parseResourceLocation(path);
    }

    public ResourceLocation getResourceLocation(String path) {
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
    ResourceLocation parseResourceLocation(String path) {
        return pathResolver.resolve(path);
    }

    /**
     * 从 assets 加载资源文件为字符串
     */
    String loadResourceAsString(String path) {
        return pathResolver.loadAsString(path);
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        modelLoader.clear();
        animationLoader.clear();
        particleLoader.clear();
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
        modelLoader.invalidateModel(path);
    }

    /**
     * 从缓存中移除指定动画
     */
    public void invalidateAnimation(String path) {
        animationLoader.invalidateAnimation(path);
    }

    /**
     * 获取缓存的模型数量
     */
    public int getCachedModelCount() {
        return modelLoader.getCachedModelCount();
    }

    /**
     * 获取缓存的动画数量
     */
    public int getCachedAnimationCount() {
        return animationLoader.getCachedAnimationCount();
    }

    public Map<String, Animation> loadAnimationSet(String path) {
        return animationLoader.loadAnimationSet(path);
    }

    public ParticleData loadParticle(String path) {
        return particleLoader.loadParticle(path);
    }

    public void invalidateParticle(String path) {
        particleLoader.invalidateParticle(path);
    }

    public int getCachedParticleCount() {
        return particleLoader.getCachedParticleCount();
    }
}
