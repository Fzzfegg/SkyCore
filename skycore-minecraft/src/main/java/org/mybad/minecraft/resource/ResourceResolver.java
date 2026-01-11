package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves resource locations and normalizes resource paths.
 */
@SideOnly(Side.CLIENT)
public final class ResourceResolver {

    /**
     * 解析资源路径为 ResourceLocation
     *
     * 支持的格式：
     * 1. "models/zombie.geo.json"            -> skycore:models/zombie.geo.json (默认命名空间)
     * 2. "skycore:models/zombie.geo.json"    -> skycore:models/zombie.geo.json (Minecraft 标准格式)
     * 3. "minecraft:textures/entity/pig.png" -> minecraft:textures/entity/pig.png (其他命名空间)
     */
    public ResourceLocation resolveResourceLocation(String path) {
        if (path == null) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return resolveResourceLocationInternal(trimmed);
        }
        String normalized = normalizeKnownPrefixes(trimmed);
        return resolveResourceLocationInternal(normalized);
    }

    String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String withPrefix = normalizeKnownPrefixes(trimmed);
        return resolveResourceLocation(withPrefix).toString();
    }

    /**
     * 从资源包加载资源文件为字符串
     */
    String readResourceAsString(String path) {
        byte[] bytes = readResourceBytes(path);
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    byte[] readResourceBytes(String path) {
        Path filePath = locateResourcePath(path);
        if (filePath == null) {
            return null;
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            return null;
        }
    }

    public boolean prefetchBinary(String path) {
        Path filePath = locateResourcePath(path);
        if (filePath == null || !Files.isRegularFile(filePath)) {
            return false;
        }
        try (java.io.InputStream input = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            while (input.read(buffer) != -1) {
                // discard
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private String normalizeKnownPrefixes(String path) {
        String trimmed = path;
        int colonIndex = trimmed.indexOf(':');
        String namespace = null;
        if (colonIndex > 0) {
            namespace = trimmed.substring(0, colonIndex);
            trimmed = trimmed.substring(colonIndex + 1);
        }
        String lower = trimmed.toLowerCase();
        if (lower.endsWith(".geo.json")) {
            if (!lower.startsWith("models/")) {
                trimmed = "models/" + trimmed;
            }
        } else if (lower.endsWith(".animation.json") || lower.endsWith(".anim.json")) {
            if (!lower.startsWith("models/")) {
                trimmed = "models/" + trimmed;
            }
        } else if (lower.endsWith(".json")) {
            if (!lower.startsWith("particles/")) {
                trimmed = "particles/" + trimmed;
            }
        } else if (lower.endsWith(".png")) {
            if (!lower.startsWith("models/") && !lower.startsWith("textures/")) {
                trimmed = "models/" + trimmed;
            }
        }
        if (namespace != null) {
            return namespace + ":" + trimmed;
        }
        return trimmed;
    }

    private ResourceLocation resolveResourceLocationInternal(String path) {
        int colonIndex = path.indexOf(':');
        if (colonIndex > 0) {
            String namespace = path.substring(0, colonIndex);
            String resourcePath = path.substring(colonIndex + 1);
            return new ResourceLocation(namespace, resourcePath);
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, path);
    }

    /**
     * 从 resourcepacks/SkyCore/<namespace>/<path> 目录读取
     * 支持路径: models/player.geo.json, animations/walk.animation.json 等
     */
    private Path resolvePackRoot() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameDir == null) {
            return null;
        }
        return mc.gameDir.toPath().resolve("resourcepacks").resolve("SkyCore");
    }

    private Path locateResourcePath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }
        String normalized = normalizeKnownPrefixes(rawPath.trim());
        ResourceLocation location = resolveResourceLocationInternal(normalized);
        Path packRoot = resolvePackRoot();
        if (packRoot == null) {
            return null;
        }
        return packRoot.resolve(location.getNamespace()).resolve(location.getPath());
    }
}
