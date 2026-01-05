package org.mybad.minecraft.resource;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.geometry.GeometryCache;

import java.util.Map;

/**
 * 资源加载器
 * 负责从 assets 目录加载模型、动画文件
 * 支持缓存以提高性能
 */
@SideOnly(Side.CLIENT)
public class ResourceLoader {

    private final ModelResourceCache modelCache;
    private final AnimationResourceCache animationCache;
    private final ParticleResourceCache particleCache;
    private final ResourcePathResolver pathResolver;

    private final GeometryCache geometryCache;

    public ResourceLoader() {
        this.modelCache = new ModelResourceCache(this);
        this.animationCache = new AnimationResourceCache(this);
        this.particleCache = new ParticleResourceCache(this);
        this.pathResolver = new ResourcePathResolver();
        this.geometryCache = new GeometryCache();
    }

    /**
     * 加载模型
     * @param path 模型路径 (如: skycore/models/zombie.geo.json)
     * @return Model 对象，如果加载失败返回 null
     */
    public Model loadModel(String path) {
        return modelCache.loadModel(path);
    }

    /**
     * 加载动画
     * @param path 动画路径 (如: skycore/animations/zombie.animation.json)
     * @return Animation 对象，如果加载失败返回 null
     */
    public Animation loadAnimation(String path) {
        return animationCache.loadAnimation(path);
    }

    public Animation loadAnimation(String path, String clipName) {
        return animationCache.loadAnimation(path, clipName);
    }

    /**
     * 解析资源路径为 ResourceLocation
     *
     * 支持的格式：
     * 1. "models/zombie.geo.json"          → skycore:models/zombie.geo.json (默认命名空间)
     * 2. "skycore:models/zombie.geo.json"  → skycore:models/zombie.geo.json (Minecraft 标准格式)
     * 3. "minecraft:textures/entity/pig.png" → minecraft:textures/entity/pig.png (其他命名空间)
     */
    public ResourceLocation resolveResourceLocation(String path) {
        return pathResolver.resolveResourceLocation(path);
    }

    String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        return resolveResourceLocation(path).toString();
    }
    /**
     * 从 assets 加载资源文件为字符串
     */
    String readResourceAsString(String path) {
        return pathResolver.readResourceAsString(path);
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        modelCache.clear();
        animationCache.clear();
        particleCache.clear();
        geometryCache.clear();
        SkyCoreMod.LOGGER.info("[SkyCore] 资源缓存已清空");
    }

    public void clearModelCache() {
        modelCache.clear();
    }

    public void clearAnimationCache() {
        animationCache.clear();
    }

    public void clearParticleCache() {
        particleCache.clear();
    }

    public void clearGeometryCache() {
        geometryCache.clear();
    }

    public GeometryCache getGeometryCache() {
        return geometryCache;
    }

    /**
     * 从缓存中移除指定模型
     */
    public void invalidateModel(String path) {
        modelCache.invalidateModel(path);
    }

    /**
     * 从缓存中移除指定动画
     */
    public void invalidateAnimation(String path) {
        animationCache.invalidateAnimation(path);
    }

    /**
     * 获取缓存的模型数量
     */
    public int getCachedModelCount() {
        return modelCache.getCachedModelCount();
    }

    /**
     * 获取缓存的动画数量
     */
    public int getCachedAnimationCount() {
        return animationCache.getCachedAnimationCount();
    }

    public Map<String, Animation> loadAnimationSet(String path) {
        return animationCache.loadAnimationSet(path);
    }

    public ParticleData loadParticle(String path) {
        return particleCache.loadParticle(path);
    }

    // ========== 新增：文件系统资源加载 ==========
    
    public void invalidateParticle(String path) {
        particleCache.invalidateParticle(path);
    }

    public int getCachedParticleCount() {
        return particleCache.getCachedParticleCount();
    }
}
