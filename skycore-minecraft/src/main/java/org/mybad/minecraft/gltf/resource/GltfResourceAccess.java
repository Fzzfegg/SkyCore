package org.mybad.minecraft.gltf.resource;

import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

public final class GltfResourceAccess {

    private static volatile GltfResourceLoader loader;

    private GltfResourceAccess() {
    }

    public static void install(GltfResourceLoader newLoader) {
        loader = newLoader;
    }

    public static void clear() {
        loader = null;
    }

    public static InputStream open(ResourceLocation location) throws IOException {
        GltfResourceLoader current = loader;
        if (current == null) {
            throw new IllegalStateException("GLTF resource loader not installed");
        }
        return current.open(location);
    }
}
