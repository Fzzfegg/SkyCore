package org.mybad.minecraft.render;

import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.resource.ResourceCacheManager;

public final class ModelHandleFactory {
    private ModelHandleFactory() {
    }

    public static BedrockModelHandle create(ResourceCacheManager cacheManager, EntityModelMapping mapping) {
        if (cacheManager == null || mapping == null) {
            return null;
        }
        Model model = cacheManager.loadModel(mapping.getModel());
        if (model == null) {
            return null;
        }

        Animation animation = null;
        if (mapping.getAnimation() != null && !mapping.getAnimation().isEmpty()) {
            animation = cacheManager.loadAnimation(mapping.getAnimation());
        }

        ResourceLocation texture = cacheManager.resolveResourceLocation(mapping.getTexture());
        ResourceLocation emissiveTexture = null;
        if (mapping.getEmissive() != null && !mapping.getEmissive().isEmpty()) {
            emissiveTexture = cacheManager.resolveResourceLocation(mapping.getEmissive());
        }
        ResourceLocation bloomTexture = null;
        if (mapping.getBloom() != null && !mapping.getBloom().isEmpty()) {
            bloomTexture = cacheManager.resolveResourceLocation(mapping.getBloom());
        }

        return BedrockModelHandle.create(
            model,
            animation,
            texture,
            emissiveTexture,
            bloomTexture,
            mapping.isEnableCull(),
            mapping.getModel(),
            cacheManager.getGeometryCache()
        );
    }

}
