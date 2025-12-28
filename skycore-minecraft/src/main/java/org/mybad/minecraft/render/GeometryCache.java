package org.mybad.minecraft.render;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class GeometryCache {
    private final Map<Key, SharedGeometry> cache = new HashMap<>();
    private long hitCount;
    private long missCount;
    private long buildCount;

    public synchronized SharedGeometry acquire(Key key, Supplier<SharedGeometry> builder) {
        SharedGeometry geometry = cache.get(key);
        if (geometry != null && !geometry.isDestroyed()) {
            hitCount++;
            geometry.retain();
            return geometry;
        }
        missCount++;
        geometry = builder.get();
        if (geometry != null) {
            buildCount++;
            cache.put(key, geometry);
        }
        return geometry;
    }

    public synchronized void release(Key key, SharedGeometry geometry) {
        if (geometry == null) {
            return;
        }
        SharedGeometry cached = cache.get(key);
        boolean destroyed = geometry.release();
        if (destroyed && cached == geometry) {
            cache.remove(key);
        }
    }

    public synchronized void clear() {
        for (SharedGeometry geometry : cache.values()) {
            geometry.forceDestroy();
        }
        cache.clear();
        hitCount = 0;
        missCount = 0;
        buildCount = 0;
        GpuSkinningShader.releaseAll();
    }

    public synchronized Stats getStats() {
        return new Stats(cache.size(), hitCount, missCount, buildCount);
    }

    public static Key key(String modelId, int textureWidth, int textureHeight) {
        return new Key(modelId, textureWidth, textureHeight);
    }

    public static final class Key {
        private final String modelId;
        private final int textureWidth;
        private final int textureHeight;

        private Key(String modelId, int textureWidth, int textureHeight) {
            this.modelId = modelId;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            Key other = (Key) obj;
            return textureWidth == other.textureWidth
                && textureHeight == other.textureHeight
                && modelId.equals(other.modelId);
        }

        @Override
        public int hashCode() {
            int result = modelId.hashCode();
            result = 31 * result + textureWidth;
            result = 31 * result + textureHeight;
            return result;
        }
    }

    public static final class Stats {
        public final int entryCount;
        public final long hits;
        public final long misses;
        public final long builds;

        private Stats(int entryCount, long hits, long misses, long builds) {
            this.entryCount = entryCount;
            this.hits = hits;
            this.misses = misses;
            this.builds = builds;
        }

        @Override
        public String toString() {
            return "GeometryCache: entries=" + entryCount
                + ", hits=" + hits
                + ", misses=" + misses
                + ", builds=" + builds;
        }
    }
}
