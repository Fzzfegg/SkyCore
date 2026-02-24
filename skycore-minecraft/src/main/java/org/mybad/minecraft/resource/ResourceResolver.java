package org.mybad.minecraft.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.resource.PathObfuscator;
import org.mybad.minecraft.SkyCoreMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

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

    public enum ResourceType {
        MODEL,
        ANIMATION,
        PARTICLE,
        GENERIC
    }

    static final class ResourceLookup {
        private final Path jsonPath;
        private final Path binaryPath;
        private final org.mybad.core.binary.BinaryResourceType binaryType;

        ResourceLookup(Path jsonPath, Path binaryPath, org.mybad.core.binary.BinaryResourceType binaryType) {
            this.jsonPath = jsonPath;
            this.binaryPath = binaryPath;
            this.binaryType = binaryType;
        }

        public Path getJsonPath() {
            return jsonPath;
        }

        public Path getBinaryPath() {
            return binaryPath;
        }

        public boolean hasBinary() {
            return binaryPath != null;
        }

        public org.mybad.core.binary.BinaryResourceType getBinaryType() {
            return binaryType;
        }
    }

    /**
     * 从资源包加载资源文件为字符串
     */
    public String readResourceAsString(String path) {
        byte[] bytes = readResourceBytes(path);
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readResourceBytes(String path) {
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
        boolean hasExplicitNamespace = false;
        if (colonIndex > 0) {
            namespace = trimmed.substring(0, colonIndex);
            trimmed = trimmed.substring(colonIndex + 1);
            hasExplicitNamespace = true;
        }
        String normalized = trimmed.replace('\\', '/');
        if (!isObfuscatedRelative(normalized)) {
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (!hasExplicitNamespace) {
                if (lower.endsWith(".geo.json") || lower.endsWith(".glb")) {
                    if (!lower.startsWith("models/")) {
                        normalized = "models/" + normalized;
                    }
                } else if (lower.endsWith(".animation.json") || lower.endsWith(".anim.json")) {
                    if (!lower.startsWith("models/")) {
                        normalized = "models/" + normalized;
                    }
                } else if (lower.endsWith(".json")) {
                    if (!lower.startsWith("particles/")) {
                        normalized = "particles/" + normalized;
                    }
                } else if (lower.endsWith(".png")) {
                    if (!lower.startsWith("models/") && !lower.startsWith("textures/")) {
                        normalized = "models/" + normalized;
                    }
                }
            }
        }
        if (namespace != null) {
            return namespace + ":" + normalized;
        }
        return normalized;
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
        Path namespaceRoot = packRoot.resolve(location.getNamespace());
        Path direct = namespaceRoot.resolve(location.getPath());
        if (Files.exists(direct)) {
            return direct;
        }
        return resolveCaseInsensitive(namespaceRoot, location.getPath());
    }

    ResourceLookup lookup(String rawPath, ResourceType type) {
        Path jsonPath = locateResourcePath(rawPath);
        Path binaryPath = type == ResourceType.PARTICLE ? null : locateBinaryPath(rawPath, type);
        return new ResourceLookup(jsonPath, binaryPath, mapBinaryType(type));
    }

    private Path locateBinaryPath(String rawPath, ResourceType type) {
        Path packRoot = resolvePackRoot();
        if (packRoot == null || rawPath == null) {
            return null;
        }
        String normalized = normalizeKnownPrefixes(rawPath.trim());
        ResourceLocation location = resolveResourceLocationInternal(normalized);
        String relative = location.getPath().replace('\\', '/');
        String binaryRelative = toBinaryRelative(relative, type);
        if (binaryRelative == null) {
            return null;
        }
        String namespace = location.getNamespace();
        if (isObfuscatedRelative(relative)) {
            Path physical = resolvePhysicalObfuscated(packRoot, namespace, binaryRelative);
            if (physical != null) {
                return physical;
            }
        }
        String logicalBinary = namespace + ":" + binaryRelative;
        Path obfuscated = resolveObfuscatedBinary(packRoot, logicalBinary);
        if (obfuscated != null) {
            return obfuscated;
        }
        return resolveDevBinary(packRoot, logicalBinary);
    }

    private org.mybad.core.binary.BinaryResourceType mapBinaryType(ResourceType type) {
        switch (type) {
            case MODEL:
                return org.mybad.core.binary.BinaryResourceType.MODEL;
            case ANIMATION:
                return org.mybad.core.binary.BinaryResourceType.ANIMATION;
            case PARTICLE:
                return org.mybad.core.binary.BinaryResourceType.PARTICLE;
            default:
                return org.mybad.core.binary.BinaryResourceType.UNKNOWN;
        }
    }

    Path resolveBinaryPath(String rawPath, ResourceType type) {
        ResourceLookup lookup = lookup(rawPath, type);
        return lookup.getBinaryPath();
    }

    Path resolveJsonPath(String rawPath) {
        return locateResourcePath(rawPath);
    }

    private Path resolveCaseInsensitive(Path base, String relative) {
        if (base == null || relative == null || relative.isEmpty()) {
            return null;
        }
        String[] parts = relative.split("/");
        Path current = base;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            Path exact = current.resolve(part);
            if (Files.exists(exact)) {
                current = exact;
                continue;
            }
            Path matched = findCaseInsensitiveEntry(current, part);
            if (matched == null) {
                return null;
            }
            current = matched;
        }
        return current;
    }

    private Path findCaseInsensitiveEntry(Path directory, String targetName) {
        if (directory == null || targetName == null) {
            return null;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path child : stream) {
                String childName = child.getFileName().toString();
                if (childName.equalsIgnoreCase(targetName)) {
                    return child;
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private Path resolveObfuscatedBinary(Path packRoot, String logicalPath) {
        if (packRoot == null) {
            return null;
        }
        String obfuscated = PathObfuscator.toPhysical(logicalPath, PathObfuscator.Mode.PROD);
        Path direct = packRoot.resolve(obfuscated);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path enc = packRoot.resolve(obfuscated + ".enc");
        if (Files.isRegularFile(enc)) {
            return enc;
        }
        return null;
    }

    private Path resolveDevBinary(Path packRoot, String logicalPath) {
        if (packRoot == null) {
            return null;
        }
        String devRelative = PathObfuscator.toPhysical(logicalPath, PathObfuscator.Mode.DEV);
        Path direct = packRoot.resolve(devRelative);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path encrypted = packRoot.resolve(devRelative + ".enc");
        if (Files.isRegularFile(encrypted)) {
            return encrypted;
        }
        Path insensitive = resolveCaseInsensitive(packRoot, devRelative);
        if (insensitive != null && Files.isRegularFile(insensitive)) {
            return insensitive;
        }
        Path insensitiveEnc = resolveCaseInsensitive(packRoot, devRelative + ".enc");
        if (insensitiveEnc != null && Files.isRegularFile(insensitiveEnc)) {
            return insensitiveEnc;
        }
        return null;
    }

    private String toBinaryRelative(String relative, ResourceType type) {
        if (relative == null) {
            return null;
        }
        String normalized = relative.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        switch (type) {
            case MODEL:
                if (lower.endsWith(".geo.json")) {
                    return normalized.substring(0, normalized.length() - ".geo.json".length()) + ".skm";
                }
                if (lower.endsWith(".glb")) {
                    return normalized.substring(0, normalized.length() - ".glb".length()) + ".skm";
                }
                break;
            case ANIMATION:
                if (lower.endsWith(".animation.json")) {
                    return normalized.substring(0, normalized.length() - ".animation.json".length()) + ".ska";
                }
                if (lower.endsWith(".anim.json")) {
                    return normalized.substring(0, normalized.length() - ".anim.json".length()) + ".ska";
                }
                break;
            case PARTICLE:
                return null;
            default:
                break;
        }
        return null;
    }

    private Path resolvePhysicalObfuscated(Path packRoot, String namespace, String binaryRelative) {
        if (packRoot == null) {
            return null;
        }
        Path direct = packRoot.resolve(binaryRelative);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path encrypted = packRoot.resolve(binaryRelative + ".enc");
        if (Files.isRegularFile(encrypted)) {
            return encrypted;
        }
        if (namespace != null && !namespace.isEmpty()) {
            Path namespaced = packRoot.resolve(namespace).resolve(binaryRelative);
            if (Files.isRegularFile(namespaced)) {
                return namespaced;
            }
            Path namespacedEnc = packRoot.resolve(namespace).resolve(binaryRelative + ".enc");
            if (Files.isRegularFile(namespacedEnc)) {
                return namespacedEnc;
            }
        }
        return null;
    }

    private boolean isObfuscatedRelative(String path) {
        if (path == null) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        return normalized.startsWith("obf/");
    }
}
