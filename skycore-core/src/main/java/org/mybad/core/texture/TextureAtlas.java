package org.mybad.core.texture;

import java.util.*;

/**
 * 纹理图集 - 将多个小纹理组织到一个大纹理中
 * 用于优化渲染性能，减少纹理切换次数
 */
public class TextureAtlas {

    private String id;
    private String name;
    private int atlasWidth;
    private int atlasHeight;
    private Map<String, TextureRegion> regions;

    /**
     * 纹理区域 - 表示图集中的一个区域
     */
    public static class TextureRegion {
        public String name;
        public int x;      // 在图集中的 X 坐标
        public int y;      // 在图集中的 Y 坐标
        public int width;
        public int height;

        public TextureRegion(String name, int x, int y, int width, int height) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        /**
         * 获取标准化的 UV 坐标
         */
        public float[] getUV(int atlasWidth, int atlasHeight) {
            float u0 = x / (float) atlasWidth;
            float v0 = y / (float) atlasHeight;
            float u1 = (x + width) / (float) atlasWidth;
            float v1 = (y + height) / (float) atlasHeight;

            return new float[]{u0, v0, u1, v1};
        }
    }

    public TextureAtlas(String id, String name, int width, int height) {
        this.id = id;
        this.name = name;
        this.atlasWidth = width;
        this.atlasHeight = height;
        this.regions = new HashMap<>();
    }

    /**
     * 添加纹理区域
     */
    public void addRegion(String regionName, int x, int y, int width, int height) {
        TextureRegion region = new TextureRegion(regionName, x, y, width, height);
        regions.put(regionName, region);
    }

    /**
     * 获取纹理区域
     */
    public TextureRegion getRegion(String regionName) {
        return regions.get(regionName);
    }

    /**
     * 检查是否包含指定区域
     */
    public boolean hasRegion(String regionName) {
        return regions.containsKey(regionName);
    }

    /**
     * 获取所有区域
     */
    public Collection<TextureRegion> getAllRegions() {
        return regions.values();
    }

    /**
     * 获取图集宽度
     */
    public int getAtlasWidth() {
        return atlasWidth;
    }

    /**
     * 获取图集高度
     */
    public int getAtlasHeight() {
        return atlasHeight;
    }

    /**
     * 获取图集 ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取图集名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取区域数量
     */
    public int getRegionCount() {
        return regions.size();
    }

    /**
     * 计算图集的利用率
     */
    public float getUtilization() {
        long totalPixels = (long) atlasWidth * atlasHeight;
        long usedPixels = regions.values().stream()
                .mapToLong(r -> (long) r.width * r.height)
                .sum();
        return usedPixels / (float) totalPixels;
    }

    /**
     * 获取图集信息
     */
    @Override
    public String toString() {
        return String.format("TextureAtlas [%s, %dx%d, Regions: %d, Utilization: %.1f%%]",
                name, atlasWidth, atlasHeight, regions.size(), getUtilization() * 100);
    }
}
