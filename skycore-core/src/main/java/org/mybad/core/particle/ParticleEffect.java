package org.mybad.core.particle;

import java.util.*;

/**
 * 粒子效果 - 粒子效果的定义和配置
 * 定义粒子的行为、发射方式、外观等
 */
public class ParticleEffect {

    private String effectId;
    private String effectName;
    private String description;

    // 发射器配置
    private List<Emitter> emitters;
    private Map<String, Emitter> emitterMap;

    // 效果参数
    private float lifetime;              // 效果总时长
    private int maxParticles;            // 最大粒子数
    private String textureFile;          // 纹理文件路径
    private String spaceType;            // 空间类型（world, local, etc）

    // 元数据
    private Map<String, String> metadata;
    private long creationTime;
    private String creator;

    // 性能配置
    private float updateRate = 60.0f;    // 更新频率（Hz）
    private boolean optimizeWithDistance = true;  // 距离优化
    private float maxRenderDistance = 64.0f;

    public ParticleEffect(String effectId, String effectName) {
        this.effectId = effectId;
        this.effectName = effectName;
        this.emitters = new ArrayList<>();
        this.emitterMap = new HashMap<>();
        this.metadata = new HashMap<>();
        this.creationTime = System.currentTimeMillis();
        this.lifetime = 10.0f;
        this.maxParticles = 1000;
    }

    /**
     * 添加发射器
     */
    public void addEmitter(Emitter emitter) {
        if (emitter != null) {
            emitters.add(emitter);
            emitterMap.put(emitter.getEmitterId(), emitter);
        }
    }

    /**
     * 获取发射器
     */
    public Emitter getEmitter(String emitterId) {
        return emitterMap.get(emitterId);
    }

    /**
     * 获取所有发射器
     */
    public List<Emitter> getEmitters() {
        return new ArrayList<>(emitters);
    }

    /**
     * 移除发射器
     */
    public void removeEmitter(String emitterId) {
        Emitter emitter = emitterMap.remove(emitterId);
        if (emitter != null) {
            emitters.remove(emitter);
        }
    }

    /**
     * 获取发射器数量
     */
    public int getEmitterCount() {
        return emitters.size();
    }

    /**
     * 设置元数据
     */
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * 获取元数据
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 获取元数据
     */
    public String getMetadata(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    /**
     * 检查是否包含元数据
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * 验证效果配置
     */
    public boolean validate() {
        if (effectId == null || effectId.isEmpty()) {
            return false;
        }
        if (emitters.isEmpty()) {
            return false;
        }
        if (maxParticles <= 0) {
            return false;
        }
        return true;
    }

    /**
     * 获取效果信息
     */
    public String getEffectInfo() {
        return String.format("ParticleEffect [%s (%s), Emitters: %d, MaxParticles: %d, Lifetime: %.2fs]",
                effectId, effectName, emitters.size(), maxParticles, lifetime);
    }

    // Setters and Getters
    public void setCreator(String creator) { this.creator = creator; }
    public void setDescription(String description) { this.description = description; }
    public void setLifetime(float lifetime) { this.lifetime = lifetime; }
    public void setMaxParticles(int maxParticles) { this.maxParticles = maxParticles; }
    public void setTextureFile(String textureFile) { this.textureFile = textureFile; }
    public void setSpaceType(String spaceType) { this.spaceType = spaceType; }
    public void setUpdateRate(float rate) { this.updateRate = rate; }
    public void setOptimizeWithDistance(boolean optimize) { this.optimizeWithDistance = optimize; }
    public void setMaxRenderDistance(float distance) { this.maxRenderDistance = distance; }

    public String getEffectId() { return effectId; }
    public String getEffectName() { return effectName; }
    public String getDescription() { return description; }
    public float getLifetime() { return lifetime; }
    public int getMaxParticles() { return maxParticles; }
    public String getTextureFile() { return textureFile; }
    public String getSpaceType() { return spaceType; }
    public float getUpdateRate() { return updateRate; }
    public boolean isOptimizeWithDistance() { return optimizeWithDistance; }
    public float getMaxRenderDistance() { return maxRenderDistance; }
    public String getCreator() { return creator; }
    public long getCreationTime() { return creationTime; }

    @Override
    public String toString() {
        return getEffectInfo();
    }
}
