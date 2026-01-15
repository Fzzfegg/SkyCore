package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;

import java.util.HashSet;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class TextureReloadHelper {

    private TextureReloadHelper() {}

    public static void reloadSkyCoreTextures(ResourceCacheManager cacheManager) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || cacheManager == null) {
            return;
        }
        TextureManager textureManager = mc.getTextureManager();
        Set<ResourceLocation> targets = collectMappingTextures(cacheManager);
        if (targets.isEmpty()) {
            return;
        }
        targets.forEach(textureManager::deleteTexture);
        SkyCoreMod.LOGGER.info("[SkyCore] 已刷新 {} 个 SkyCore 纹理资源。", targets.size());
    }

    private static Set<ResourceLocation> collectMappingTextures(ResourceCacheManager cacheManager) {
        Set<ResourceLocation> set = new HashSet<>();
        SkyCoreConfig config = SkyCoreConfig.getInstance();
        if (config == null) {
            return set;
        }
        ResourceResolver resolver = cacheManager.getResolver();
        for (EntityModelMapping mapping : config.getAllMappings()) {
            addIfPresent(set, resolver, mapping.getTexture());
            addIfPresent(set, resolver, mapping.getEmissive());
            addIfPresent(set, resolver, mapping.getBloom());
            addIfPresent(set, resolver, mapping.getBlendTexture());
        }
        return set;
    }

    private static void addIfPresent(Set<ResourceLocation> set, ResourceResolver resolver, String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }
        ResourceLocation location = resolver.resolveResourceLocation(path);
        if (location != null) {
            set.add(location);
        }
    }
}
