package org.mybad.minecraft.resource;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryPayloadCipher;
import org.mybad.core.binary.BinaryPayloadCipherRegistry;
import org.mybad.core.binary.BinaryResourceIO;
import org.mybad.core.binary.BinaryResourceType;
import org.mybad.core.binary.SkycoreBinaryArchive;
import org.mybad.core.binary.texture.TextureBinarySerializer;
import org.mybad.core.binary.audio.AudioBinarySerializer;
import org.mybad.core.resource.PathObfuscator;
import org.mybad.minecraft.SkyCoreMod;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Virtual resource pack that serves files from SkyCore root.
 * Layout: &lt;root&gt;/&lt;namespace&gt;/&lt;path&gt;
 */
public final class ConfigResourcePack implements IResourcePack {
    private final Path root;
    private final String packName;
    private final BinaryPayloadCipherRegistry cipherRegistry;
    private final TextureBinarySerializer textureSerializer = new TextureBinarySerializer();
    private final AudioBinarySerializer audioSerializer = new AudioBinarySerializer();

    public ConfigResourcePack(Path root, BinaryPayloadCipherRegistry cipherRegistry) {
        this.root = root;
        this.packName = "SkyCore Config Pack";
        this.cipherRegistry = cipherRegistry;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        Path direct = resolvePath(location);
        if (direct != null && Files.isRegularFile(direct)) {
            return Files.newInputStream(direct);
        }
        if (isTextureResource(location)) {
            byte[] decoded = decodeBinaryTexture(location);
            if (decoded != null) {
                return new ByteArrayInputStream(decoded);
            }
        } else if (isAudioResource(location)) {
            byte[] decoded = decodeBinaryAudio(location);
            if (decoded != null) {
                return new ByteArrayInputStream(decoded);
            }
        }
        throw new FileNotFoundException(location.toString());
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        Path direct = resolvePath(location);
        if (direct != null && Files.isRegularFile(direct)) {
            return true;
        }
        if (isTextureResource(location)) {
            return resolveBinaryPath(location, ".skt") != null;
        }
        if (isAudioResource(location)) {
            return resolveBinaryPath(location, ".sko") != null;
        }
        return false;
    }

    @Override
    public Set<String> getResourceDomains() {
        if (!Files.isDirectory(root)) {
            return Collections.singleton(SkyCoreMod.MOD_ID);
        }
        Set<String> domains = new HashSet<>();
        domains.add(SkyCoreMod.MOD_ID);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    domains.add(child.getFileName().toString());
                }
            }
        } catch (IOException ignored) {
        }
        return domains;
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String metadataSectionName) {
        return null;
    }

    @Nullable
    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return packName;
    }

    private Path resolvePath(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        Path namespaceRoot = root.resolve(location.getNamespace());
        String normalized = location.getPath().replace('/', File.separatorChar);
        Path direct = namespaceRoot.resolve(normalized);
        if (Files.exists(direct)) {
            return direct;
        }
        return resolveCaseInsensitive(namespaceRoot, location.getPath());
    }

    private boolean isTextureResource(ResourceLocation location) {
        if (location == null) {
            return false;
        }
        String path = location.getPath().toLowerCase(Locale.ROOT);
        return path.endsWith(".png");
    }

    private boolean isAudioResource(ResourceLocation location) {
        if (location == null) {
            return false;
        }
        String path = location.getPath().toLowerCase(Locale.ROOT);
        return path.endsWith(".ogg");
    }

    private byte[] decodeBinaryTexture(ResourceLocation location) throws IOException {
        return decodeBinaryPayload(location, ".skt", BinaryResourceType.TEXTURE, textureSerializer::read);
    }

    private byte[] decodeBinaryAudio(ResourceLocation location) throws IOException {
        return decodeBinaryPayload(location, ".sko", BinaryResourceType.AUDIO, audioSerializer::read);
    }

    private BinaryPayloadCipher resolveCipher(int flags) {
        if (cipherRegistry != null) {
            return cipherRegistry.resolve(flags);
        }
        return BinaryPayloadCipher.NO_OP;
    }

    private Path resolveBinaryPath(ResourceLocation location, String extension) {
        if (location == null) {
            return null;
        }
        String relative = location.getPath().replace('\\', '/');
        int dot = relative.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String binaryRelative = relative.substring(0, dot) + extension;
        if (isObfuscatedRelative(relative)) {
            Path physical = resolvePhysicalBinary(location.getNamespace(), binaryRelative);
            if (physical != null) {
                return physical;
            }
        }
        String logical = location.getNamespace() + ":" + binaryRelative;
        Path obfuscated = resolveObfuscatedBinary(root, logical);
        if (obfuscated != null) {
            return obfuscated;
        }
        return resolveDevBinary(root, logical);
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

    private byte[] decodeBinaryPayload(ResourceLocation location,
                                       String extension,
                                       BinaryResourceType expectedType,
                                       PayloadReader readerFn) throws IOException {
        Path binary = resolveBinaryPath(location, extension);
        if (binary == null) {
            return null;
        }
        byte[] data = Files.readAllBytes(binary);
        try {
            SkycoreBinaryArchive archive = BinaryResourceIO.read(data, this::resolveCipher);
            if (archive.getHeader().getType() != expectedType) {
                throw new IOException("Unexpected binary type for " + expectedType + ": " + archive.getHeader().getType());
            }
            BinaryDataReader reader = new BinaryDataReader(archive.getPayload());
            return readerFn.read(reader);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Failed to decrypt binary resource " + location, ex);
        }
    }

    @FunctionalInterface
    private interface PayloadReader {
        byte[] read(BinaryDataReader reader) throws IOException;
    }

    private Path resolvePhysicalBinary(String namespace, String relative) {
        Path direct = root.resolve(relative);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path encrypted = root.resolve(relative + ".enc");
        if (Files.isRegularFile(encrypted)) {
            return encrypted;
        }
        if (namespace != null && !namespace.isEmpty()) {
            Path namespaced = root.resolve(namespace).resolve(relative);
            if (Files.isRegularFile(namespaced)) {
                return namespaced;
            }
            Path namespacedEnc = root.resolve(namespace).resolve(relative + ".enc");
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

    private Path resolveObfuscatedBinary(Path base, String logicalPath) {
        if (base == null) {
            return null;
        }
        String physical = PathObfuscator.toPhysical(logicalPath, PathObfuscator.Mode.PROD);
        Path direct = base.resolve(physical);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path encrypted = base.resolve(physical + ".enc");
        if (Files.isRegularFile(encrypted)) {
            return encrypted;
        }
        return null;
    }

    private Path resolveDevBinary(Path base, String logicalPath) {
        if (base == null) {
            return null;
        }
        String relative = PathObfuscator.toPhysical(logicalPath, PathObfuscator.Mode.DEV);
        Path direct = base.resolve(relative);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path encrypted = base.resolve(relative + ".enc");
        if (Files.isRegularFile(encrypted)) {
            return encrypted;
        }
        Path insensitive = resolveCaseInsensitive(base, relative);
        if (insensitive != null && Files.isRegularFile(insensitive)) {
            return insensitive;
        }
        Path insensitiveEnc = resolveCaseInsensitive(base, relative + ".enc");
        if (insensitiveEnc != null && Files.isRegularFile(insensitiveEnc)) {
            return insensitiveEnc;
        }
        return null;
    }
}
