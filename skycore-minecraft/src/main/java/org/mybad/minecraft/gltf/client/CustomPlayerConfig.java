package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.core.data.DataMaterial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    @JsonProperty("renderMode")
    public RenderMode renderMode = RenderMode.REPLACE;

    @JsonProperty("fps")
    public int fps = 24;

    @JsonProperty("blendDuration")
    public double blendDuration = 0.2;

    @JsonProperty("animations")
    public HashMap<String, AnimationConfig> animations = new HashMap<>();

    @JsonProperty("materials")
    public HashMap<String, MaterialOverride> materials = new HashMap<>();

    @JsonProperty("attachments")
    public List<AttachmentConfig> attachments = new ArrayList<>();

    @JsonProperty("firstPerson")
    public FirstPersonConfig firstPerson;

    @JsonProperty("renderOffset")
    public OffsetConfig renderOffset = new OffsetConfig();

    @JsonProperty("bloom")
    public BloomConfig bloom = new BloomConfig();

    public enum RenderMode {
        REPLACE,
        OVERLAY
    }

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

        @JsonProperty("overlays")
        public List<OverlayConfig> overlays = Collections.emptyList();

        private static final List<DataMaterial.OverlayLayer.TriggerCondition> DEFAULT_TRIGGER_CONDITIONS =
            Collections.singletonList(DataMaterial.OverlayLayer.TriggerCondition.always());

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
            if (overlays != null && !overlays.isEmpty()) {
                java.util.List<org.mybad.minecraft.gltf.core.data.DataMaterial.OverlayLayer> layers = new java.util.ArrayList<>();
                int overlayIndex = 0;
                for (OverlayConfig ov : overlays) {
                    if (ov == null || ov.texturePath == null || ov.texturePath.isEmpty()) {
                        continue;
                    }
                    org.mybad.minecraft.gltf.core.data.DataMaterial.OverlayLayer layer = new org.mybad.minecraft.gltf.core.data.DataMaterial.OverlayLayer();
                    layer.id = deriveOverlayId(ov, layer.texturePath, overlayIndex);
                    layer.texturePath = ov.texturePath;
                    if (ov.color != null && ov.color.size() >= 3) {
                        float r = ov.color.get(0) != null ? ov.color.get(0) : 1f;
                        float g = ov.color.get(1) != null ? ov.color.get(1) : 1f;
                        float b = ov.color.get(2) != null ? ov.color.get(2) : 1f;
                        float a = ov.color.size() > 3 && ov.color.get(3) != null ? ov.color.get(3) : 1f;
                        layer.color.set(r, g, b, a);
                    }
                    if (ov.blendMode != null) {
                        try {
                            layer.blendMode = org.mybad.minecraft.gltf.core.data.DataMaterial.BlendMode.valueOf(ov.blendMode.toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (ov.useTextureAlpha != null) {
                        layer.useTextureAlpha = ov.useTextureAlpha;
                    }
                    if (ov.alphaCutoff != null) {
                        layer.alphaCutoff = ov.alphaCutoff;
                    }
                    if (ov.bloomEnabled != null) {
                        layer.bloomEnabled = ov.bloomEnabled;
                    }
                    if (ov.bloomIntensity != null) {
                        layer.bloomIntensity = ov.bloomIntensity;
                    }
                    if (ov.bloomDownscale != null) {
                        layer.bloomDownscale = Math.max(1, ov.bloomDownscale);
                    }
                    if (ov.bloomPasses != null) {
                        layer.bloomPasses = Math.max(0, ov.bloomPasses);
                    }
                    if (ov.pulse != null) {
                        if (ov.pulse.enabled != null) {
                            layer.pulse.enabled = ov.pulse.enabled;
                        }
                        if (ov.pulse.speed != null) {
                            layer.pulse.speed = Math.max(0.0f, ov.pulse.speed);
                        }
                        if (ov.pulse.minAlpha != null) {
                            layer.pulse.minAlpha = ov.pulse.minAlpha;
                        }
                        if (ov.pulse.maxAlpha != null) {
                            layer.pulse.maxAlpha = ov.pulse.maxAlpha;
                        }
                        if (ov.pulse.phaseOffset != null) {
                            layer.pulse.phaseOffset = ov.pulse.phaseOffset;
                        }
                        if (ov.pulse.wave != null) {
                            layer.pulse.wave = DataMaterial.OverlayLayer.PulseWave.fromString(ov.pulse.wave);
                        }
                    }
                    if (ov.colorPulse != null) {
                        DataMaterial.OverlayLayer.ColorPulseSettings colorPulse = layer.colorPulse;
                        if (ov.colorPulse.enabled != null) {
                            colorPulse.enabled = ov.colorPulse.enabled;
                        }
                        if (ov.colorPulse.speed != null) {
                            colorPulse.speed = Math.max(0.0f, ov.colorPulse.speed);
                        }
                        if (ov.colorPulse.phaseOffset != null) {
                            colorPulse.phaseOffset = ov.colorPulse.phaseOffset;
                        }
                        if (ov.colorPulse.wave != null) {
                            colorPulse.wave = DataMaterial.OverlayLayer.PulseWave.fromString(ov.colorPulse.wave);
                        }
                        if (ov.colorPulse.mode != null) {
                            colorPulse.mode = DataMaterial.OverlayLayer.ColorPulseMode.fromString(ov.colorPulse.mode);
                        }
                        if (ov.colorPulse.clampAlpha != null) {
                            colorPulse.clampAlpha = ov.colorPulse.clampAlpha;
                        }
                        if (ov.colorPulse.colorA != null && ov.colorPulse.colorA.size() >= 3) {
                            float r = ov.colorPulse.colorA.get(0) != null ? ov.colorPulse.colorA.get(0) : 1f;
                            float g = ov.colorPulse.colorA.get(1) != null ? ov.colorPulse.colorA.get(1) : 1f;
                            float b = ov.colorPulse.colorA.get(2) != null ? ov.colorPulse.colorA.get(2) : 1f;
                            float a = ov.colorPulse.colorA.size() > 3 && ov.colorPulse.colorA.get(3) != null
                                ? ov.colorPulse.colorA.get(3) : 1f;
                            colorPulse.colorA.set(r, g, b, a);
                        }
                        if (ov.colorPulse.colorB != null && ov.colorPulse.colorB.size() >= 3) {
                            float r = ov.colorPulse.colorB.get(0) != null ? ov.colorPulse.colorB.get(0) : 1f;
                            float g = ov.colorPulse.colorB.get(1) != null ? ov.colorPulse.colorB.get(1) : 1f;
                            float b = ov.colorPulse.colorB.get(2) != null ? ov.colorPulse.colorB.get(2) : 1f;
                            float a = ov.colorPulse.colorB.size() > 3 && ov.colorPulse.colorB.get(3) != null
                                ? ov.colorPulse.colorB.get(3) : 1f;
                            colorPulse.colorB.set(r, g, b, a);
                        }
                    }
                    List<DataMaterial.OverlayLayer.TriggerCondition> triggerConditions = buildTriggerConditions(ov.triggers);
                    if (!triggerConditions.isEmpty()) {
                        layer.triggers = triggerConditions;
                    } else {
                        layer.triggers = DEFAULT_TRIGGER_CONDITIONS;
                    }
                    if (ov.hitMode != null) {
                        layer.triggerMatchMode = DataMaterial.OverlayLayer.TriggerMatchMode.fromString(ov.hitMode);
                    }
                    List<DataMaterial.OverlayLayer.HoverZone> hoverZones = buildHoverZones(ov.hoverZones);
                    if (!hoverZones.isEmpty()) {
                        layer.hoverZones = hoverZones;
                    }
                    layers.add(layer);
                    overlayIndex++;
                }
                material.overlays = layers;
            }
        }

        private static List<DataMaterial.OverlayLayer.TriggerCondition> buildTriggerConditions(List<String> triggerTokens) {
            if (triggerTokens == null || triggerTokens.isEmpty()) {
                return Collections.emptyList();
            }
            List<DataMaterial.OverlayLayer.TriggerCondition> result = new ArrayList<>();
            for (String raw : triggerTokens) {
                if (raw == null) {
                    continue;
                }
                String token = raw.trim();
                if (token.isEmpty()) {
                    continue;
                }
                String keyword = token;
                String node = null;
                int separator = token.indexOf(':');
                if (separator >= 0) {
                    keyword = token.substring(0, separator);
                    node = token.substring(separator + 1).trim();
                    if (node.isEmpty()) {
                        node = null;
                    }
                }
                DataMaterial.OverlayLayer.TriggerType type = DataMaterial.OverlayLayer.TriggerType.fromString(keyword);
                if (type == DataMaterial.OverlayLayer.TriggerType.HOVER_NODE && (node == null || node.isEmpty())) {
                    continue;
                }
                DataMaterial.OverlayLayer.TriggerCondition condition = new DataMaterial.OverlayLayer.TriggerCondition();
                condition.type = type;
                condition.node = node;
                result.add(condition);
            }
            return result;
        }

        private static List<DataMaterial.OverlayLayer.HoverZone> buildHoverZones(List<HoverZoneConfig> configs) {
            if (configs == null || configs.isEmpty()) {
                return Collections.emptyList();
            }
            List<DataMaterial.OverlayLayer.HoverZone> zones = new ArrayList<>();
            for (HoverZoneConfig config : configs) {
                if (config == null || config.node == null || config.node.isEmpty()) {
                    continue;
                }
                DataMaterial.OverlayLayer.HoverZone zone = new DataMaterial.OverlayLayer.HoverZone();
                zone.node = config.node;
                if (config.shape != null) {
                    zone.shape = DataMaterial.OverlayLayer.HoverZoneShape.fromString(config.shape);
                }
                if (config.radius != null && config.radius > 0f) {
                    zone.radius = config.radius;
                }
                if (config.halfExtents != null && !config.halfExtents.isEmpty()) {
                    zone.halfExtents.set(
                        getComponent(config.halfExtents, 0, zone.halfExtents.x),
                        getComponent(config.halfExtents, 1, zone.halfExtents.y),
                        getComponent(config.halfExtents, 2, zone.halfExtents.z)
                    );
                }
                zones.add(zone);
            }
            return zones;
        }

        private static float getComponent(List<Float> values, int index, float fallback) {
            if (values == null || values.size() <= index) {
                return fallback;
            }
            Float v = values.get(index);
            return v != null ? v : fallback;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverlayConfig {
        @JsonProperty("id")
        public String id;

        @JsonProperty("texturePath")
        public String texturePath;

        @JsonProperty("color")
        public List<Float> color;

        @JsonProperty("blendMode")
        public String blendMode;

        @JsonProperty("useTextureAlpha")
        public Boolean useTextureAlpha;

        @JsonProperty("alphaCutoff")
        public Float alphaCutoff;

        @JsonProperty("bloomEnabled")
        public Boolean bloomEnabled;

        @JsonProperty("bloomIntensity")
        public Float bloomIntensity;

        @JsonProperty("bloomDownscale")
        public Integer bloomDownscale;

        @JsonProperty("bloomPasses")
        public Integer bloomPasses;

        @JsonProperty("pulse")
        public PulseConfig pulse;

        @JsonProperty("colorPulse")
        public ColorPulseConfig colorPulse;

        @JsonProperty("triggers")
        public List<String> triggers = Collections.emptyList();

        @JsonProperty("hitMode")
        public String hitMode;

        @JsonProperty("hoverZones")
        public List<HoverZoneConfig> hoverZones = Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PulseConfig {
        @JsonProperty("enabled")
        public Boolean enabled;

        @JsonProperty("speed")
        public Float speed;

        @JsonProperty("minAlpha")
        public Float minAlpha;

        @JsonProperty("maxAlpha")
        public Float maxAlpha;

        @JsonProperty("phaseOffset")
        public Float phaseOffset;

        @JsonProperty("wave")
        public String wave;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ColorPulseConfig {
        @JsonProperty("enabled")
        public Boolean enabled;

        @JsonProperty("speed")
        public Float speed;

        @JsonProperty("phaseOffset")
        public Float phaseOffset;

        @JsonProperty("wave")
        public String wave;

        @JsonProperty("mode")
        public String mode;

        @JsonProperty("clampAlpha")
        public Boolean clampAlpha;

        @JsonProperty("colorA")
        public List<Float> colorA;

        @JsonProperty("colorB")
        public List<Float> colorB;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HoverZoneConfig {
        @JsonProperty("node")
        public String node;

        @JsonProperty("shape")
        public String shape;

        @JsonProperty("radius")
        public Float radius;

        @JsonProperty("halfExtents")
        public List<Float> halfExtents = Collections.emptyList();
    }

    private static String deriveOverlayId(OverlayConfig config, String texturePath, int index) {
        if (config != null && config.id != null && !config.id.isEmpty()) {
            return config.id;
        }
        if (texturePath != null && !texturePath.isEmpty()) {
            return texturePath + "#" + index;
        }
        return "overlay#" + index;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentConfig {
        @JsonProperty("ref")
        public String profileRef;

        @JsonProperty("renderOffset")
        public OffsetConfig renderOffset;

        @JsonProperty("animationOverride")
        public String animationOverride;

        public boolean isValid() {
            return profileRef != null && !profileRef.isEmpty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FirstPersonConfig {
        @JsonProperty("enabled")
        public boolean enabled = false;

        @JsonProperty("leftHand")
        public HandConfig leftHand = new HandConfig();

        @JsonProperty("rightHand")
        public HandConfig rightHand = new HandConfig();

        public HandConfig getForSide(net.minecraft.util.EnumHandSide side) {
            return side == net.minecraft.util.EnumHandSide.LEFT ? leftHand : rightHand;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HandConfig {
        @JsonProperty("enabled")
        public Boolean enabled = Boolean.FALSE;

        @JsonProperty("position")
        public List<Float> position = Collections.emptyList();

        @JsonProperty("rotation")
        public List<Float> rotation = Collections.emptyList();

        @JsonProperty("scale")
        public Float scale;

        @JsonProperty("visibleNodes")
        public List<String> visibleNodes = Collections.emptyList();

        @JsonProperty("hiddenNodes")
        public List<String> hiddenNodes = Collections.emptyList();

        @JsonProperty("mirror")
        public Boolean mirror;

        @JsonProperty("disableCull")
        public Boolean disableCull;

        @JsonProperty("weapon")
        public HandWeaponConfig weapon;

        public boolean isEnabled(boolean defaultValue) {
            return enabled == null ? defaultValue : enabled;
        }

        public float getScale(float fallback) {
            return scale != null ? scale : fallback;
        }

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

        public Set<String> getVisibleNodes() {
            if (visibleNodes == null || visibleNodes.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(visibleNodes);
        }

        public Set<String> getHiddenNodes() {
            if (hiddenNodes == null || hiddenNodes.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(hiddenNodes);
        }

        public boolean shouldMirror(boolean fallback) {
            return mirror != null ? mirror : fallback;
        }

        public boolean shouldDisableCull(boolean fallback) {
            return disableCull != null ? disableCull : fallback;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HandWeaponConfig {
        @JsonProperty("modelPath")
        public String modelPath;

        @JsonProperty("texturePath")
        public String texturePath;

        @JsonProperty("position")
        public List<Float> position = Collections.emptyList();

        @JsonProperty("rotation")
        public List<Float> rotation = Collections.emptyList();

        @JsonProperty("scale")
        public Float scale;

        @JsonProperty("visibleNodes")
        public List<String> visibleNodes = Collections.emptyList();

        @JsonProperty("hiddenNodes")
        public List<String> hiddenNodes = Collections.emptyList();

        @JsonProperty("mirror")
        public Boolean mirror;

        @JsonProperty("disableCull")
        public Boolean disableCull;

        public boolean isValid() {
            return modelPath != null && !modelPath.isEmpty();
        }

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

        public Set<String> getVisibleNodes() {
            if (visibleNodes == null || visibleNodes.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(visibleNodes);
        }

        public Set<String> getHiddenNodes() {
            if (hiddenNodes == null || hiddenNodes.isEmpty()) {
                return Collections.emptySet();
            }
            return new HashSet<>(hiddenNodes);
        }

        public boolean shouldMirror(boolean fallback) {
            return mirror != null ? mirror : fallback;
        }

        public boolean shouldDisableCull(boolean fallback) {
            return disableCull != null ? disableCull : fallback;
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
