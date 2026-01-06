package org.mybad.minecraft.resource;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Virtual resource pack that serves files from SkyCore root.
 * Layout: <root>/<namespace>/<path>
 * Serves resources directly from SkyCore root.
 */
public final class ConfigResourcePack implements IResourcePack {
    private final Path root;
    private final String packName;

    public ConfigResourcePack(Path root) {
        this.root = root;
        this.packName = "SkyCore Config Pack";
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        Path file = resolvePath(location);
        if (file == null || !Files.isRegularFile(file)) {
            throw new FileNotFoundException(location.toString());
        }
        // no per-file debug logging here
        return Files.newInputStream(file);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        Path file = resolvePath(location);
        return file != null && Files.isRegularFile(file);
    }

    @Override
    public Set<String> getResourceDomains() {
        if (!Files.isDirectory(root)) {
            return Collections.emptySet();
        }
        Set<String> domains = new HashSet<>();
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
        Path base = root.resolve(location.getNamespace());
        return base.resolve(location.getPath().replace('/', File.separatorChar));
    }

}
