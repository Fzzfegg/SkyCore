package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.SkyCoreMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves resource paths and loads raw content.
 * Priority: resourcepacks/SkyCore/<namespace>/<path> → Minecraft resource manager
 */
final class ResourcePathResolver {

    ResourceLocation resolveResourceLocation(String path) {
        int colonIndex = path.indexOf(':');
        if (colonIndex > 0) {
            String namespace = path.substring(0, colonIndex);
            String resourcePath = path.substring(colonIndex + 1);
            return new ResourceLocation(namespace, resourcePath);
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, path);
    }

    String readResourceAsString(String path) {
        String content = readFromPackRoot(path);
        return content;
    }

    /**
     * 从 resourcepacks/SkyCore/<namespace>/<path> 目录读取
     * 支持路径: models/player.geo.json, animations/walk.animation.json 等
     */
    private String readFromPackRoot(String path) {
        try {
            // 提取 namespace + 相对路径
            String cleanPath = path;
            String namespace = SkyCoreMod.MOD_ID;
            int colonIndex = path.indexOf(':');
            if (colonIndex > 0) {
                namespace = path.substring(0, colonIndex);
                cleanPath = path.substring(colonIndex + 1);
            }

            Path packRoot = resolvePackRoot();
            if (packRoot == null) {
                return null;
            }
            Path filePath = packRoot.resolve(namespace).resolve(cleanPath);
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // 文件不存在或读取失败，继续下一步
        }
        return null;
    }
    

    private Path resolvePackRoot() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameDir == null) {
            return null;
        }
        return mc.gameDir.toPath().resolve("resourcepacks").resolve("SkyCore");
    }
    
}
