package org.mybad.core.legacy;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.*;
import org.mybad.core.legacy.animation.AnimationBlender;
import org.mybad.core.legacy.resource.*;
import org.mybad.core.legacy.utils.*;
import org.mybad.core.parsing.AnimationParser;
import org.mybad.core.parsing.ModelParser;

/**
 * SkyCore 外观类
 * 提供简化的API供用户使用
 * 整合所有主要功能
 */
public class SkyCoreFacade {
    private static SkyCoreFacade instance;

    private ResourceCache modelCache;
    private ResourceCache animationCache;
    private ModelParser modelParser;
    private AnimationParser animationParser;

    private static final long DEFAULT_CACHE_SIZE = 100 * 1024 * 1024;  // 100MB

    private SkyCoreFacade() {
        this.modelCache = new ResourceCache(DEFAULT_CACHE_SIZE);
        this.animationCache = new ResourceCache(DEFAULT_CACHE_SIZE);
        this.modelParser = new ModelParser();
        this.animationParser = new AnimationParser();
    }

    /**
     * 获取单例实例
     */
    public static synchronized SkyCoreFacade getInstance() {
        if (instance == null) {
            instance = new SkyCoreFacade();
        }
        return instance;
    }

    // ===== 模型操作 =====

    /**
     * 加载模型
     */
    public Model loadModel(String modelId, String jsonContent) throws Exception {
        ModelResource resource = new ModelResource(modelId, jsonContent);
        resource.load();  // 加载并解析模型
        modelCache.put(modelId, resource);
        Model model = resource.getModel();

        return model;
    }

    /**
     * 获取已加载的模型
     */
    public Model getModel(String modelId) throws Exception {
        Resource resource = modelCache.get(modelId);
        if (resource instanceof ModelResource) {
            return ((ModelResource) resource).getModel();
        }
        return null;
    }

    /**
     * 卸载模型
     */
    public void unloadModel(String modelId) {
        modelCache.remove(modelId);
    }

    // ===== 动画操作 =====

    /**
     * 加载动画
     */
    public Animation loadAnimation(String animationId, String jsonContent) throws Exception {
        AnimationResource resource = new AnimationResource(animationId, jsonContent);
        resource.load();  // 加载并解析动画
        animationCache.put(animationId, resource);
        Animation animation = resource.getAnimation();

        return animation;
    }

    /**
     * 创建动画播放器
     */
    public AnimationPlayer createAnimationPlayer(Animation animation) {
        return new AnimationPlayer(animation);
    }

    /**
     * 创建动画混合器
     */
    public AnimationBlender createAnimationBlender(Model model) {
        return new AnimationBlender(model);
    }

    // ===== 工具方法 =====

    /**
     * 创建定位器管理器
     */
    public LocatorManager createLocatorManager(Model model) {
        return new LocatorManager(model);
    }

    /**
     * 获取资源缓存统计
     */
    public CacheStats getModelCacheStats() {
        ResourceCache.CacheStats stats = modelCache.getStats();
        return new CacheStats(stats.resourceCount, stats.usedSize, stats.maxSize, stats.freeSize);
    }

    /**
     * 获取动画缓存统计
     */
    public CacheStats getAnimationCacheStats() {
        ResourceCache.CacheStats stats = animationCache.getStats();
        return new CacheStats(stats.resourceCount, stats.usedSize, stats.maxSize, stats.freeSize);
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCaches() {
        modelCache.clear();
        animationCache.clear();
    }

    /**
     * 获取SkyCore版本
     */
    public String getVersion() {
        return SkyCore.getVersion();
    }

    /**
     * 关闭SkyCore（释放资源）
     */
    public void shutdown() {
        clearAllCaches();
    }

    // ===== 内部类 =====

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final int resourceCount;
        public final long usedSize;
        public final long maxSize;
        public final long freeSize;

        public CacheStats(int resourceCount, long usedSize, long maxSize, long freeSize) {
            this.resourceCount = resourceCount;
            this.usedSize = usedSize;
            this.maxSize = maxSize;
            this.freeSize = freeSize;
        }

        @Override
        public String toString() {
            return String.format("缓存统计: 资源数=%d, 已用=%dMB, 总容量=%dMB, 剩余=%dMB",
                resourceCount,
                usedSize / (1024 * 1024),
                maxSize / (1024 * 1024),
                freeSize / (1024 * 1024)
            );
        }
    }
}
