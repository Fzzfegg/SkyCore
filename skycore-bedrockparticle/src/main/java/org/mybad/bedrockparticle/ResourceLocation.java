package org.mybad.bedrockparticle;

import org.jetbrains.annotations.Nullable;

/**
 * Lightweight ResourceLocation (namespace:path).
 * Used for particle parsing without Minecraft classes.
 */
public final class ResourceLocation {
    private final String namespace;
    private final String path;

    public ResourceLocation(String location) {
        String trimmed = location == null ? "" : location.trim();
        int idx = trimmed.indexOf(':');
        if (idx >= 0) {
            this.namespace = emptyToDefault(trimmed.substring(0, idx));
            this.path = normalizePath(trimmed.substring(idx + 1));
        } else {
            this.namespace = "minecraft";
            this.path = normalizePath(trimmed);
        }
        if (this.path.isEmpty()) {
            throw new IllegalArgumentException("Invalid ResourceLocation: " + location);
        }
    }

    public ResourceLocation(String namespace, String path) {
        String ns = emptyToDefault(namespace);
        String p = normalizePath(path);
        if (p.isEmpty()) {
            throw new IllegalArgumentException("Invalid ResourceLocation: " + namespace + ":" + path);
        }
        this.namespace = ns;
        this.path = p;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }

    public static ResourceLocation parse(String location) {
        return new ResourceLocation(location);
    }

    @Nullable
    public static ResourceLocation tryParse(String location) {
        try {
            return new ResourceLocation(location);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String emptyToDefault(String namespace) {
        String ns = namespace == null ? "" : namespace.trim();
        return ns.isEmpty() ? "minecraft" : ns;
    }

    private static String normalizePath(String path) {
        return path == null ? "" : path.trim();
    }

    @Override
    public String toString() {
        return namespace + ':' + path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceLocation)) {
            return false;
        }
        ResourceLocation that = (ResourceLocation) o;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
