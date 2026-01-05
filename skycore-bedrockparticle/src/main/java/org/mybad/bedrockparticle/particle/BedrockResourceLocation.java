package org.mybad.bedrockparticle.particle;

import org.jetbrains.annotations.Nullable;

/**
 * Lightweight ResourceLocation (namespace:path).
 * Used for particle parsing without Minecraft classes.
 */
public final class BedrockResourceLocation {
    private final String namespace;
    private final String path;

    public BedrockResourceLocation(String location) {
        String trimmed = location == null ? "" : location.trim();
        int idx = trimmed.indexOf(':');
        if (idx >= 0) {
            this.namespace = emptyToDefault(trimmed.substring(0, idx));
            this.path = normalizePath(trimmed.substring(idx + 1));
        } else {
            this.namespace = "skycore";
            this.path = normalizePath(trimmed);
        }
        if (this.path.isEmpty()) {
            throw new IllegalArgumentException("Invalid ResourceLocation: " + location);
        }
    }

    public BedrockResourceLocation(String namespace, String path) {
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

    public static BedrockResourceLocation parse(String location) {
        return new BedrockResourceLocation(location);
    }

    @Nullable
    public static BedrockResourceLocation tryParse(String location) {
        try {
            return new BedrockResourceLocation(location);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String emptyToDefault(String namespace) {
        String ns = namespace == null ? "" : namespace.trim();
        return ns.isEmpty() ? "skycore" : ns;
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
        if (!(o instanceof BedrockResourceLocation)) {
            return false;
        }
        BedrockResourceLocation that = (BedrockResourceLocation) o;
        return namespace.equals(that.namespace) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
