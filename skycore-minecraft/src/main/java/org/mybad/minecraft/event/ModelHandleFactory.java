package org.mybad.minecraft.event;

import net.minecraft.util.ResourceLocation;
import org.mybad.core.animation.Animation;
import org.mybad.core.data.Model;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.resource.ResourceLoader;

final class ModelHandleFactory {
    private ModelHandleFactory() {
    }

    static BedrockModelHandle create(ResourceLoader resourceLoader, EntityModelMapping mapping, String contextName) {
        if (resourceLoader == null || mapping == null) {
            return null;
        }
        Model model = resourceLoader.loadModel(mapping.getModel());
        if (model == null) {
            warnMissingModel(mapping.getModel(), contextName);
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

    private static void warnMissingModel(String modelPath, String contextName) {
        if (contextName == null || contextName.isEmpty()) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型: {}", modelPath);
        } else {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无法加载模型: {} for {}", modelPath, contextName);
        }
    }
}
