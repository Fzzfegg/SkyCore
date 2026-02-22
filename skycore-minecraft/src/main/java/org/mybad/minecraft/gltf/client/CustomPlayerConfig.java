package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.core.data.DataMaterial;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joml.Vector4f;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomPlayerConfig {
    @JsonProperty("name")
    public String name;

    @JsonProperty("modelPath")
    public String modelPath;

    @JsonProperty("texturePath")
    public String texturePath;

    @JsonProperty("modelScale")
    public float modelScale = 1.0f;

    @JsonProperty("fps")
    public int fps = 24;

    @JsonProperty("blendDuration")
    public double blendDuration = 0.2;

    @JsonProperty("animations")
    public HashMap<String, AnimationConfig> animations = new HashMap<>();

    @JsonProperty("materials")
    public HashMap<String, MaterialOverride> materials = new HashMap<>();

    @JsonProperty("renderOffset")
    public OffsetConfig renderOffset = new OffsetConfig();

    @JsonProperty("bloom")
    public BloomConfig bloom = new BloomConfig();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnimationConfig {
        @JsonProperty("startTime")
        public double startTime = 0.0;

        @JsonProperty("endTime")
        public double endTime = 1.0;

        @JsonProperty("speed")
        public double speed = 1.0;

        @JsonProperty("blendDuration")
        public Double blendDuration;

        @JsonProperty("loop")
        public Boolean loop = Boolean.TRUE;

        @JsonProperty("holdLastFrame")
        public Boolean holdLastFrame = Boolean.FALSE;

        public double getStartFrame(double fps) {
            return startTime;
        }

        public double getEndFrame(double fps) {
            return endTime;
        }

        public double getAnimationSpeed(double fps) {
            double durationSeconds = getDuration(fps);
            if (durationSeconds <= 0) {
                double safeFps = Math.max(fps, 1.0);
                durationSeconds = 1.0 / safeFps;
            }
            return speed / durationSeconds;
        }

        public double getDuration(double fps) {
            double frameSpan = getEndFrame(fps) - getStartFrame(fps);
            if (frameSpan <= 0) {
                return 0.0;
            }
            return frameSpan / fps;
        }

        public float getBlendDurationSeconds(double defaultDuration) {
            double value = blendDuration != null ? blendDuration : defaultDuration;
            if (value < 0) {
                value = 0;
            }
            return (float) value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MaterialOverride {
        @JsonProperty("baseColorFactor")
        public Vector4f baseColorFactor;

        @JsonProperty("metallicFactor")
        public Float metallicFactor;

        @JsonProperty("roughnessFactor")
        public Float roughnessFactor;

        @JsonProperty("baseColorTexture")
        public String baseColorTexture;

        @JsonProperty("metallicRoughnessTexture")
        public String metallicRoughnessTexture;

        @JsonProperty("normalTexture")
        public String normalTexture;

        @JsonProperty("occlusionTexture")
        public String occlusionTexture;

        @JsonProperty("alphaMode")
        public String alphaMode;

        @JsonProperty("alphaCutoff")
        public Float alphaCutoff;

        @JsonProperty("depthOffset")
        public Float depthOffset;

        public void applyTo(org.mybad.minecraft.gltf.core.data.DataMaterial material) {
            if (material == null) {
                return;
            }
            if (baseColorFactor != null) {
                material.baseColorFactor.set(baseColorFactor);
            }
            if (metallicFactor != null) {
                material.metallicFactor = metallicFactor;
            }
            if (roughnessFactor != null) {
                material.roughnessFactor = roughnessFactor;
            }
            if (baseColorTexture != null) {
                material.setBaseColorTexturePath(baseColorTexture);
            }
            if (metallicRoughnessTexture != null) {
                material.metallicRoughnessTexture = metallicRoughnessTexture;
            }
            if (normalTexture != null) {
                material.normalTexture = normalTexture;
            }
            if (occlusionTexture != null) {
                material.occlusionTexture = occlusionTexture;
            }
            if (alphaMode != null) {
                try {
                    material.alphaMode = org.mybad.minecraft.gltf.core.data.DataMaterial.AlphaMode.valueOf(alphaMode.toUpperCase());
                    material.updateTranslucencyFromAlphaMode();
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (alphaCutoff != null) {
                material.alphaCutoff = alphaCutoff;
            }
            if (depthOffset != null) {
                material.depthOffset = depthOffset;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OffsetConfig {
        @JsonProperty("position")
        public List<Float> position = Collections.emptyList();

        @JsonProperty("rotation")
        public List<Float> rotation = Collections.emptyList();

        @JsonProperty("scale")
        public Float scale;

        public float getPosition(int index, float fallback) {
            if (position == null || position.size() <= index) {
                return fallback;
            }
            Float value = position.get(index);
            return value != null ? value : fallback;
        }

        public float getRotation(int index, float fallback) {
            if (rotation == null || rotation.size() <= index) {
                return fallback;
            }
            Float value = rotation.get(index);
            return value != null ? value : fallback;
        }

        public float getScale(float fallback) {
            return scale != null ? scale : fallback;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BloomConfig {
        @JsonProperty("enabled")
        public boolean enabled = false;

        @JsonProperty("intensity")
        public float intensity = 1.0f;

        @JsonProperty("downscale")
        public int downscale = 2;

        @JsonProperty("blurPasses")
        public int blurPasses = 2;
    }

    public AnimationConfig getAnimation(String name) {
        return animations.get(name);
    }

    public boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }

    public void addAnimation(String name, AnimationConfig config) {
        animations.put(name, config);
    }

    public void removeAnimation(String name) {
        animations.remove(name);
    }

    public MaterialOverride getMaterialOverride(String materialName) {
        return materials.get(materialName);
    }

    public MaterialOverride getDefaultOverride() {
        return materials.get("default");
    }

    public MaterialOverride getFirstOverride() {
        return materials.isEmpty() ? null : materials.values().iterator().next();
    }

    public boolean hasMaterialOverride(String materialName) {
        return materials.containsKey(materialName);
    }

    public void addMaterialOverride(String materialName, MaterialOverride override) {
        materials.put(materialName, override);
    }

    public void removeMaterialOverride(String materialName) {
        materials.remove(materialName);
    }
}
