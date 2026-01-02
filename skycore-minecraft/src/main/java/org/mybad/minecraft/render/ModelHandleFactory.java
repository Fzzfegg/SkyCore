package org.mybad.minecraft.render;

import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.resource.ResourceLoader;

public final class ModelHandleFactory {
    private ModelHandleFactory() {
    }

    public static BedrockModelHandle create(ResourceLoader resourceLoader, EntityModelMapping mapping) {
        if (resourceLoader == null || mapping == null) {
            return null;
        }
        Model model = resourceLoader.loadModel(mapping.getModel());
        if (model == null) {
            return null;
        }

        Animation animation = null;
        if (mapping.getAnimation() != null && !mapping.getAnimation().isEmpty()) {
            animation = resourceLoader.loadAnimation(mapping.getAnimation());
        }

        ResourceLocation texture = resourceLoader.getTextureLocation(mapping.getTexture());
        ResourceLocation emissiveTexture = null;
        if (mapping.getEmissive() != null && !mapping.getEmissive().isEmpty()) {
            emissiveTexture = resourceLoader.getTextureLocation(mapping.getEmissive());
        }

        return BedrockModelHandle.create(
            model,
            animation,
            texture,
            emissiveTexture,
            mapping.isEnableCull(),
            mapping.getModel(),
            resourceLoader.getGeometryCache()
        );
    }

}
