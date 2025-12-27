package org.mybad.core.particle;

import java.util.*;
import java.io.*;

/**
 * 粒子库 - 粒子效果的库管理系统
 * 支持加载、保存、热重载等功能
 * Blockbuster风格的设计
 */
public class ParticleLibrary {

    private String libraryId;
    private String libraryPath;

    private Map<String, ParticleEffect> effects;
    private Map<String, ParticleMaterial> materials;
    private Map<String, Long> fileModifyTimes;

    // 预设和工厂
    private Map<String, ParticleEffectPreset> presets;

    // 缓存
    private boolean autoReload = true;
    private long lastReloadTime = 0;
    private float reloadCheckInterval = 2.0f;  // 秒

    // 统计
    private long effectsLoaded = 0;
    private long effectsSaved = 0;

    public ParticleLibrary(String libraryId, String libraryPath) {
        this.libraryId = libraryId;
        this.libraryPath = libraryPath;
        this.effects = new HashMap<>();
        this.materials = new HashMap<>();
        this.fileModifyTimes = new HashMap<>();
        this.presets = new HashMap<>();
    }

    /**
     * 注册粒子效果
     */
    public void registerEffect(ParticleEffect effect) {
        if (effect != null && effect.validate()) {
            effects.put(effect.getEffectId(), effect);
        }
    }

    /**
     * 获取粒子效果
     */
    public ParticleEffect getEffect(String effectId) {
        return effects.get(effectId);
    }

    /**
     * 获取所有效果
     */
    public Collection<ParticleEffect> getAllEffects() {
        return new ArrayList<>(effects.values());
    }

    /**
     * 移除效果
     */
    public void removeEffect(String effectId) {
        effects.remove(effectId);
    }

    /**
     * 注册材质
     */
    public void registerMaterial(ParticleMaterial material) {
        if (material != null && material.validate()) {
            materials.put(material.getMaterialId(), material);
        }
    }

    /**
     * 获取材质
     */
    public ParticleMaterial getMaterial(String materialId) {
        return materials.get(materialId);
    }

    /**
     * 注册预设
     */
    public void registerPreset(ParticleEffectPreset preset) {
        if (preset != null) {
            presets.put(preset.getPresetId(), preset);
        }
    }

    /**
     * 从预设创建效果
     */
    public ParticleEffect createFromPreset(String presetId, String newEffectId) {
        ParticleEffectPreset preset = presets.get(presetId);
        if (preset == null) {
            return null;
        }

        return preset.createEffect(newEffectId);
    }

    /**
     * 从文件加载效果
     */
    public ParticleEffect loadEffectFromFile(String filePath, String effectId) throws IOException {
        File file = new File(libraryPath, filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        try {
            String jsonContent = readFileContent(file);
            ParticleEffect effect = ParticleParser.parseFromJson(jsonContent, effectId, filePath);

            // 记录文件修改时间
            fileModifyTimes.put(effectId, file.lastModified());

            effectsLoaded++;
            return effect;

        } catch (ParticleParser.ParseException e) {
            throw new IOException("Failed to parse particle effect from file: " + filePath, e);
        }
    }

    /**
     * 保存效果到文件
     */
    public void saveEffectToFile(ParticleEffect effect, String filePath) throws IOException {
        File file = new File(libraryPath, filePath);
        file.getParentFile().mkdirs();

        String jsonContent = effectToJson(effect);
        writeFileContent(file, jsonContent);

        fileModifyTimes.put(effect.getEffectId(), file.lastModified());
        effectsSaved++;
    }

    /**
     * 检查文件是否已修改
     */
    public boolean hasFileChanged(String effectId) {
        Long lastTime = fileModifyTimes.get(effectId);
        if (lastTime == null) {
            return false;
        }

        // 实际应检查文件系统中的修改时间
        return false;
    }

    /**
     * 热重载所有效果
     */
    public void hotReload() throws IOException {
        for (String effectId : new HashSet<>(effects.keySet())) {
            if (hasFileChanged(effectId)) {
                // 重新加载效果
                // 注意：这是简化实现
            }
        }
        lastReloadTime = System.currentTimeMillis();
    }

    /**
     * 清空库
     */
    public void clear() {
        effects.clear();
        materials.clear();
        presets.clear();
        fileModifyTimes.clear();
    }

    /**
     * 获取库信息
     */
    public String getLibraryInfo() {
        return String.format("ParticleLibrary [%s, Effects: %d, Materials: %d, Presets: %d]",
                libraryId, effects.size(), materials.size(), presets.size());
    }

    // 辅助方法
    private String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private void writeFileContent(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }

    /**
     * 转换效果为JSON字符串
     */
    private String effectToJson(ParticleEffect effect) {
        // 简化的JSON序列化
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"format_version\": \"1.10.0\",\n");
        json.append("  \"particle_effect\": {\n");
        json.append("    \"description\": {\n");
        json.append("      \"identifier\": \"").append(effect.getEffectId()).append("\",\n");
        json.append("      \"basic_render_parameters\": {\n");
        json.append("        \"material\": \"particles_alpha\",\n");
        json.append("        \"texture\": \"").append(effect.getTextureFile() != null ? effect.getTextureFile() : "").append("\"\n");
        json.append("      }\n");
        json.append("    },\n");
        json.append("    \"components\": {}\n");
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }

    // Getters
    public String getLibraryId() { return libraryId; }
    public String getLibraryPath() { return libraryPath; }
    public int getEffectCount() { return effects.size(); }
    public int getMaterialCount() { return materials.size(); }
    public long getEffectsLoaded() { return effectsLoaded; }
    public long getEffectsSaved() { return effectsSaved; }

    @Override
    public String toString() {
        return getLibraryInfo();
    }

    /**
     * 粒子效果预设
     */
    public static class ParticleEffectPreset {
        private String presetId;
        private String presetName;
        private ParticleEffect templateEffect;

        public ParticleEffectPreset(String presetId, String presetName, ParticleEffect templateEffect) {
            this.presetId = presetId;
            this.presetName = presetName;
            this.templateEffect = templateEffect;
        }

        /**
         * 从预设创建新效果
         */
        public ParticleEffect createEffect(String newEffectId) {
            // 深拷贝模板效果
            ParticleEffect effect = new ParticleEffect(newEffectId, templateEffect.getEffectName());
            effect.setLifetime(templateEffect.getLifetime());
            effect.setMaxParticles(templateEffect.getMaxParticles());
            effect.setTextureFile(templateEffect.getTextureFile());
            effect.setSpaceType(templateEffect.getSpaceType());

            // 复制发射器
            for (Emitter emitter : templateEffect.getEmitters()) {
                Emitter newEmitter = new Emitter(emitter.getEmitterId(), emitter.getEmitterName());
                newEmitter.setEmissionRate(emitter.getEmissionRate());
                newEmitter.setLifetimeRange(emitter.getMinLifetime(), emitter.getMaxLifetime());
                newEmitter.setSpeedRange(emitter.getMinSpeedX(), emitter.getMaxSpeedX(),
                        emitter.getMinSpeedY(), emitter.getMaxSpeedY(),
                        emitter.getMinSpeedZ(), emitter.getMaxSpeedZ());
                newEmitter.setScaleRange(emitter.getMinScale(), emitter.getMaxScale());
                newEmitter.setShape(emitter.getShape());
                newEmitter.setShapeSize(emitter.getShapeSize());

                effect.addEmitter(newEmitter);
            }

            return effect;
        }

        public String getPresetId() { return presetId; }
        public String getPresetName() { return presetName; }
    }
}
