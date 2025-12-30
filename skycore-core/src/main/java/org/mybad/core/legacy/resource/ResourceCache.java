package org.mybad.core.legacy.resource;

import java.util.*;

/**
 * 资源缓存管理器
 * 使用LRU（最近最少使用）缓存策略
 * 管理资源的加载、缓存和卸载
 */
public class ResourceCache {
    private static final long DEFAULT_MAX_SIZE = 100 * 1024 * 1024;  // 100MB

    private Map<String, Resource> resourceMap;
    private LinkedHashMap<String, Long> lruMap;  // LRU追踪
    private long maxSize;
    private long currentSize;

    public ResourceCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public ResourceCache(long maxSize) {
        this.resourceMap = new HashMap<>();
        this.lruMap = new LinkedHashMap<String, Long>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return false;  // 手动控制移除
            }
        };
        this.maxSize = maxSize;
        this.currentSize = 0;
    }

    /**
     * 获取或加载资源
     */
    public Resource get(String resourceId) throws Exception {
        if (resourceMap.containsKey(resourceId)) {
            Resource resource = resourceMap.get(resourceId);
            lruMap.put(resourceId, System.currentTimeMillis());
            return resource;
        }
        return null;
    }

    /**
     * 添加资源到缓存
     */
    public void put(String resourceId, Resource resource) throws Exception {
        if (!resource.isLoaded()) {
            resource.load();
        }

        // 检查是否需要清理空间
        long resourceSize = resource.getSize();
        while (currentSize + resourceSize > maxSize && !resourceMap.isEmpty()) {
            evictLRU();
        }

        // 添加到缓存
        if (resourceMap.containsKey(resourceId)) {
            currentSize -= resourceMap.get(resourceId).getSize();
        }

        resourceMap.put(resourceId, resource);
        lruMap.put(resourceId, System.currentTimeMillis());
        currentSize += resourceSize;
    }

    /**
     * 移除LRU资源
     */
    private void evictLRU() {
        if (lruMap.isEmpty()) {
            return;
        }

        // 获取第一个元素（最老的）
        String oldestId = lruMap.keySet().iterator().next();
        Resource resource = resourceMap.remove(oldestId);

        if (resource != null) {
            resource.unload();
            currentSize -= resource.getSize();
            lruMap.remove(oldestId);
        }
    }

    /**
     * 移除指定资源
     */
    public void remove(String resourceId) {
        if (resourceMap.containsKey(resourceId)) {
            Resource resource = resourceMap.remove(resourceId);
            resource.unload();
            currentSize -= resource.getSize();
            lruMap.remove(resourceId);
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        for (Resource resource : resourceMap.values()) {
            resource.unload();
        }
        resourceMap.clear();
        lruMap.clear();
        currentSize = 0;
    }

    /**
     * 检查资源是否在缓存中
     */
    public boolean contains(String resourceId) {
        return resourceMap.containsKey(resourceId);
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return new CacheStats(
            resourceMap.size(),
            currentSize,
            maxSize,
            (maxSize - currentSize)
        );
    }

    /**
     * 缓存统计信息
     */
    public static class CacheStats {
        public final int resourceCount;
        public final long usedSize;
        public final long maxSize;
        public final long freeSize;

        public CacheStats(int resourceCount, long usedSize, long maxSize, long freeSize) {
            this.resourceCount = resourceCount;
            this.usedSize = usedSize;
            this.maxSize = maxSize;
            this.freeSize = freeSize;
        }

        @Override
        public String toString() {
            return String.format("缓存统计: 资源数=%d, 已用=%dMB, 总容量=%dMB, 剩余=%dMB",
                resourceCount,
                usedSize / (1024 * 1024),
                maxSize / (1024 * 1024),
                freeSize / (1024 * 1024)
            );
        }
    }

    // Getters
    public long getMaxSize() { return maxSize; }
    public long getCurrentSize() { return currentSize; }
    public int getResourceCount() { return resourceMap.size(); }
}
