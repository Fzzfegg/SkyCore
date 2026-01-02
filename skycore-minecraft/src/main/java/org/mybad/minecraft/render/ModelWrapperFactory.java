package org.mybad.minecraft.render;

import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;

final class ModelWrapperFactory {
    private static final GeometryCache FALLBACK_GEOMETRY_CACHE = new GeometryCache();

    private ModelWrapperFactory() {
    }

    static BuildData build(Model model,
                           Animation animation,
                           ResourceLocation texture,
                           ResourceLocation emissiveTexture,
                           boolean enableCull,
                           String modelId,
                           GeometryCache geometryCache) {
        if (model == null) {
            return new BuildData(null, new ModelAnimationController(new AnimationBridge(animation)), texture, emissiveTexture, enableCull,
                64, 64, null, null);
        }
        Model baseModel = model;
        Model instance = baseModel.createInstance();
        int[] texSize = resolveTextureSize(baseModel);
        int texWidth = texSize[0];
        int texHeight = texSize[1];
        GeometryCache resolvedCache = geometryCache != null ? geometryCache : FALLBACK_GEOMETRY_CACHE;
        GeometryCache.Key resolvedKey = GeometryCache.key(normalizeModelId(modelId, baseModel), texWidth, texHeight);

        ModelAnimationController animationController = new ModelAnimationController(new AnimationBridge(animation));
        ModelGeometryBuilder geometryBuilder = new ModelGeometryBuilder(instance, texWidth, texHeight);
        geometryBuilder.generateAllQuads();
        SkinningPipeline skinningPipeline = new SkinningPipeline(instance, geometryBuilder, resolvedCache, resolvedKey);

        return new BuildData(instance, animationController, texture, emissiveTexture, enableCull,
            texWidth, texHeight, geometryBuilder, skinningPipeline);
    }

    static void clearSharedResources() {
        FALLBACK_GEOMETRY_CACHE.clear();
    }

    private static int[] resolveTextureSize(Model model) {
        int texWidth = 64;
        int texHeight = 64;
        try {
            String tw = model.getTextureWidth();
            String th = model.getTextureHeight();
            if (tw != null && !tw.isEmpty()) {
                texWidth = Integer.parseInt(tw);
            }
            if (th != null && !th.isEmpty()) {
                texHeight = Integer.parseInt(th);
            }
        } catch (NumberFormatException ignored) {
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
        final boolean enableCull;
        final int textureWidth;
        final int textureHeight;
        final ModelGeometryBuilder geometryBuilder;
        final SkinningPipeline skinningPipeline;

        private BuildData(Model model,
                          ModelAnimationController animationController,
                          ResourceLocation texture,
                          ResourceLocation emissiveTexture,
                          boolean enableCull,
                          int textureWidth,
                          int textureHeight,
                          ModelGeometryBuilder geometryBuilder,
                          SkinningPipeline skinningPipeline) {
            this.model = model;
            this.animationController = animationController;
            this.texture = texture;
            this.emissiveTexture = emissiveTexture;
            this.enableCull = enableCull;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.geometryBuilder = geometryBuilder;
            this.skinningPipeline = skinningPipeline;
        }
    }
}
