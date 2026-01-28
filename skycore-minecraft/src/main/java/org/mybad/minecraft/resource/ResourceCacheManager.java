package org.mybad.minecraft.resource;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.core.animation.Animation;
import org.mybad.core.binary.BinaryPayloadCipher;
import org.mybad.core.binary.BinaryPayloadCipherRegistry;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.geometry.GeometryCache;

import java.util.Map;

/**
 * 资源缓存管理器
 * 负责模型、动画、粒子缓存与几何缓存
 */
@SideOnly(Side.CLIENT)
public class ResourceCacheManager {

    private final ResourceResolver resolver;
    private final BinaryPayloadCipherRegistry cipherRegistry;
    private final ModelResourceCache modelCache;
    private final AnimationResourceCache animationCache;
    private final ParticleResourceCache particleCache;
    private final GeometryCache geometryCache;

    public ResourceCacheManager() {
        this(null);
    }

    public ResourceCacheManager(BinaryPayloadCipherRegistry registry) {
        this.cipherRegistry = registry != null ? registry : BinaryPayloadCipherRegistry.withDefaults();
        this.resolver = new ResourceResolver();
        this.modelCache = new ModelResourceCache(resolver, cipherRegistry);
        this.animationCache = new AnimationResourceCache(resolver, cipherRegistry);
        this.particleCache = new ParticleResourceCache(resolver, cipherRegistry);
        this.geometryCache = new GeometryCache();
    }

    public void installBinaryCipher(BinaryPayloadCipher cipher) {
        if (cipherRegistry != null) {
            cipherRegistry.setActiveCipher(cipher);
        }
    }

    public BinaryPayloadCipherRegistry getCipherRegistry() {
        return cipherRegistry;
    }

    public ResourceResolver getResolver() {
        return resolver;
    }

    public ResourceLocation resolveResourceLocation(String path) {
        return resolver.resolveResourceLocation(path);
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

    public Map<String, Animation> loadAnimationSet(String path) {
        return animationCache.loadAnimationSet(path);
    }

    public ParticleData loadParticle(String path) {
        return particleCache.loadParticle(path);
    }

    public GeometryCache getGeometryCache() {
        return geometryCache;
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

    public void invalidateParticle(String path) {
        particleCache.invalidateParticle(path);
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

    public int getCachedParticleCount() {
        return particleCache.getCachedParticleCount();
    }
}
