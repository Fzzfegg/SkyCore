package org.mybad.minecraft.gltf.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.resource.ResourceResolver;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Resolves GLTF resources through SkyCore's {@link ResourceResolver} and
 * falls back to Minecraft's resource manager when necessary.
 */
public final class GltfResourceLoader {

    private final ResourceResolver resolver;

    public GltfResourceLoader(ResourceResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver);
    }

    public InputStream open(ResourceLocation location) throws IOException {
        if (location == null) {
            throw new FileNotFoundException("GLTF resource location is null");
        }
        String logicalPath = location.getNamespace() + ":" + location.getPath();
        byte[] bytes = resolver.readResourceBytes(logicalPath);
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }
        return Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream();
    }
}
