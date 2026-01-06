package org.mybad.minecraft.render;

import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.minecraft.render.ModelBlendMode;

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
        ResourceLocation blendTexture = null;
        if (mapping.getBlendTexture() != null && !mapping.getBlendTexture().isEmpty()) {
            blendTexture = cacheManager.resolveResourceLocation(mapping.getBlendTexture());
        }

        BedrockModelHandle handle = BedrockModelHandle.create(
            model,
            animation,
            texture,
            emissiveTexture,
            bloomTexture,
            blendTexture,
            mapping.isEnableCull(),
            mapping.getModel(),
            cacheManager.getGeometryCache()
        );
        if (handle != null) {
            handle.setBlendMode(parseBlendMode(mapping.getBlendMode()));
            handle.setBlendColor(mapping.getBlendColor());
        }
        return handle;
    }

    private static ModelBlendMode parseBlendMode(String raw) {
        if (raw == null || raw.isEmpty()) {
            return ModelBlendMode.ALPHA;
        }
        String lower = raw.trim().toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("add")) {
            return ModelBlendMode.ADD;
        }
        return ModelBlendMode.ALPHA;
    }

}
