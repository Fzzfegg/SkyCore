package org.mybad.core.texture;

import java.util.*;

/**
 * 纹理管理系统
 * 负责模型纹理的加载、缓存和管理
 * 支持纹理图集和个体纹理管理
 */
public class TextureManager {

    private final Map<String, TextureInfo> textures;
    private final Map<String, TextureAtlas> atlases;
    private final long maxCacheSize;
    private long currentCacheSize;

    /**
     * 纹理信息类
     */
    public static class TextureInfo {
        public String id;
        public String name;
        public int width;
        public int height;
        public byte[] data;
        public String format; // "PNG", "JPG", etc.
        public long loadTime;
        public long accessTime;

        public TextureInfo(String id, String name) {
            this.id = id;
            this.name = name;
            this.loadTime = System.currentTimeMillis();
            this.accessTime = loadTime;
        }

        public long getSize() {
            return data != null ? data.length : 0;
        }
    }

    public TextureManager(long maxCacheSize) {
        this.textures = new HashMap<>();
        this.atlases = new HashMap<>();
        this.maxCacheSize = maxCacheSize;
        this.currentCacheSize = 0;
    }

    /**
     * 注册纹理
     */
    public void registerTexture(String id, String name, int width, int height, byte[] data, String format) {
        TextureInfo info = new TextureInfo(id, name);
        info.width = width;
        info.height = height;
        info.data = data;
        info.format = format;

        long textureSize = info.getSize();
        if (currentCacheSize + textureSize > maxCacheSize) {
            evictLRU();
        }

        textures.put(id, info);
        currentCacheSize += textureSize;
    }

    /**
     * 获取纹理
     */
    public TextureInfo getTexture(String id) {
        TextureInfo info = textures.get(id);
        if (info != null) {
            info.accessTime = System.currentTimeMillis();
        }
        return info;
    }

    /**
     * 移除纹理
     */
    public void removeTexture(String id) {
        TextureInfo info = textures.remove(id);
        if (info != null) {
            currentCacheSize -= info.getSize();
        }
    }

    /**
     * LRU 缓存淘汰
     */
    private void evictLRU() {
        textures.values().stream()
                .min(Comparator.comparingLong(t -> t.accessTime))
                .ifPresent(info -> removeTexture(info.id));
    }

    /**
     * 注册纹理图集
     */
    public void registerAtlas(String id, TextureAtlas atlas) {
        atlases.put(id, atlas);
    }

    /**
     * 获取纹理图集
     */
    public TextureAtlas getAtlas(String id) {
        return atlases.get(id);
    }

    /**
     * 获取已注册的纹理数量
     */
    public int getTextureCount() {
        return textures.size();
    }

    /**
     * 获取缓存大小
     */
    public long getCacheSize() {
        return currentCacheSize;
    }

    /**
     * 清空所有纹理
     */
    public void clear() {
        textures.clear();
        atlases.clear();
        currentCacheSize = 0;
    }

    /**
     * 获取纹理统计信息
     */
    public String getStats() {
        return String.format("TextureManager [Textures: %d, CacheSize: %.2fMB/%.2fMB, Atlases: %d]",
                textures.size(),
                currentCacheSize / (1024.0 * 1024.0),
                maxCacheSize / (1024.0 * 1024.0),
                atlases.size());
    }
}
