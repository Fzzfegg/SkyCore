package org.mybad.minecraft.render;

import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.render.geometry.GeometryCache;
import org.mybad.minecraft.render.geometry.ModelGeometryBuilder;
import org.mybad.minecraft.render.skinning.SkinningPipeline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModelWrapperFactory {
    private static final GeometryCache FALLBACK_GEOMETRY_CACHE = new GeometryCache();
    private static final Map<String, Deque<Model>> MODEL_INSTANCE_POOLS = new ConcurrentHashMap<>();
    private static final int MAX_POOL_SIZE = 16;

    private ModelWrapperFactory() {
    }

    static BuildData build(Model model,
                           Animation animation,
                           ResourceLocation texture,
                           ResourceLocation emissiveTexture,
                           ResourceLocation bloomTexture,
                           ResourceLocation blendTexture,
                           boolean enableCull,
                           String modelId,
                           GeometryCache geometryCache) {
        String resolvedModelId = normalizeModelId(modelId, model);
        if (model == null) {
            return new BuildData(null, new ModelAnimationController(new AnimationBridge(animation)), texture, emissiveTexture, bloomTexture, blendTexture, enableCull,
                64, 64, null, null, resolvedModelId);
        }
        Model baseModel = model;
        Model instance = borrowModelInstance(baseModel, resolvedModelId);
        int[] texSize = resolveTextureSize(baseModel);
        int texWidth = texSize[0];
        int texHeight = texSize[1];
        GeometryCache resolvedCache = geometryCache != null ? geometryCache : FALLBACK_GEOMETRY_CACHE;
        GeometryCache.Key resolvedKey = GeometryCache.key(resolvedModelId, texWidth, texHeight);

        ModelAnimationController animationController = new ModelAnimationController(new AnimationBridge(animation));
        ModelGeometryBuilder geometryBuilder = new ModelGeometryBuilder(instance, texWidth, texHeight);
        geometryBuilder.generateAllQuads();
        SkinningPipeline skinningPipeline = new SkinningPipeline(instance, geometryBuilder, resolvedCache, resolvedKey);

        return new BuildData(instance, animationController, texture, emissiveTexture, bloomTexture, blendTexture, enableCull,
            texWidth, texHeight, geometryBuilder, skinningPipeline, resolvedModelId);
    }

    static void clearSharedResources() {
        FALLBACK_GEOMETRY_CACHE.clear();
        MODEL_INSTANCE_POOLS.clear();
    }

    private static int[] resolveTextureSize(Model model) {
        int texWidth = model.getTextureWidth();
        int texHeight = model.getTextureHeight();
        if (texWidth <= 0) {
            texWidth = 64;
        }
        if (texHeight <= 0) {
            texHeight = 64;
        }
        return new int[]{texWidth, texHeight};
    }

    private static String normalizeModelId(String modelId, Model model) {
        if (modelId != null && !modelId.isEmpty()) {
            return modelId;
        }
        String name = model != null ? model.getName() : null;
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "unknown";
    }

    static final class BuildData {
        final Model model;
        final ModelAnimationController animationController;
        final ResourceLocation texture;
        final ResourceLocation emissiveTexture;
        final ResourceLocation bloomTexture;
        final ResourceLocation blendTexture;
        final boolean enableCull;
        final int textureWidth;
        final int textureHeight;
        final ModelGeometryBuilder geometryBuilder;
        final SkinningPipeline skinningPipeline;
        final String modelId;

        private BuildData(Model model,
                          ModelAnimationController animationController,
                          ResourceLocation texture,
                          ResourceLocation emissiveTexture,
                          ResourceLocation bloomTexture,
                          ResourceLocation blendTexture,
                          boolean enableCull,
                          int textureWidth,
                          int textureHeight,
                          ModelGeometryBuilder geometryBuilder,
                          SkinningPipeline skinningPipeline,
                          String modelId) {
            this.model = model;
            this.animationController = animationController;
            this.texture = texture;
            this.emissiveTexture = emissiveTexture;
            this.bloomTexture = bloomTexture;
            this.blendTexture = blendTexture;
            this.enableCull = enableCull;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.geometryBuilder = geometryBuilder;
            this.skinningPipeline = skinningPipeline;
            this.modelId = modelId;
        }
    }

    private static Model borrowModelInstance(Model baseModel, String modelId) {
        if (baseModel == null) {
            return null;
        }
        Deque<Model> pool = MODEL_INSTANCE_POOLS.get(modelId);
        Model instance = pool != null ? pool.pollFirst() : null;
        if (instance == null) {
            instance = baseModel.createInstance();
        }
        return instance;
    }

    static void releaseModelInstance(String modelId, Model instance) {
        if (modelId == null || instance == null) {
            return;
        }
        instance.resetToBindPose();
        Deque<Model> pool = MODEL_INSTANCE_POOLS.computeIfAbsent(modelId, key -> new ArrayDeque<>());
        pool.offerFirst(instance);
        while (pool.size() > MAX_POOL_SIZE) {
            pool.pollLast();
        }
    }
}
