package org.mybad.minecraft.gltf.core.data;

import org.mybad.minecraft.gltf.GltfLog;
import net.minecraft.util.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;

public class DataMaterial {
    public String name;
    public boolean isTranslucent;

    public float depthOffset = 0;

    public Vector4f baseColorFactor = new Vector4f(1f, 1f, 1f, 1f);
    public float metallicFactor = 1.0f;
    public float roughnessFactor = 1.0f;

    public String baseColorTexture;
    public String metallicRoughnessTexture;
    public String normalTexture;
    public String occlusionTexture;

    private transient ResourceLocation baseColorResource;
    private transient String cachedBaseColorPath;

    public java.util.List<OverlayLayer> overlays = java.util.Collections.emptyList();

    public enum BlendMode {
        ALPHA,
        ADD,
        MULTIPLY
    }

    public static class OverlayLayer {
        public String id = "";
        public String texturePath;
        public Vector4f color = new Vector4f(1f, 1f, 1f, 1f);
        public BlendMode blendMode = BlendMode.ALPHA;
        public boolean useTextureAlpha = true;
        public Float alphaCutoff = null;
        // bloom options (per overlay)
        public boolean bloomEnabled = false;
        public float bloomIntensity = 1.0f;
        public int bloomDownscale = 2;
        public int bloomPasses = 2;
        public PulseSettings pulse = new PulseSettings();
        public ColorPulseSettings colorPulse = new ColorPulseSettings();
        public java.util.List<TriggerCondition> triggers = DEFAULT_ALWAYS_TRIGGER;
        public TriggerMatchMode triggerMatchMode = TriggerMatchMode.ANY;
        public java.util.List<HoverZone> hoverZones = java.util.Collections.emptyList();

        public static final class PulseSettings {
            public boolean enabled = false;
            public float speed = 1.0f;       // cycles per second
            public float minAlpha = 0.0f;
            public float maxAlpha = 1.0f;
            public float phaseOffset = 0.0f; // cycles (0-1)
            public PulseWave wave = PulseWave.SINE;

            public PulseSettings() {
            }

            public PulseSettings(PulseSettings other) {
                copyFrom(other);
            }

            public void copyFrom(PulseSettings other) {
                if (other == null) {
                    enabled = false;
                    speed = 1.0f;
                    minAlpha = 0.0f;
                    maxAlpha = 1.0f;
                    phaseOffset = 0.0f;
                    wave = PulseWave.SINE;
                    return;
                }
                this.enabled = other.enabled;
                this.speed = other.speed;
                this.minAlpha = other.minAlpha;
                this.maxAlpha = other.maxAlpha;
                this.phaseOffset = other.phaseOffset;
                this.wave = other.wave;
            }

            public PulseSettings copy() {
                PulseSettings copy = new PulseSettings();
                copy.copyFrom(this);
                return copy;
            }
        }

        public enum PulseWave {
            SINE,
            TRIANGLE,
            SAW,
            STEP;

            public static PulseWave fromString(String value) {
                if (value == null || value.isEmpty()) {
                    return SINE;
                }
                try {
                    return PulseWave.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return SINE;
                }
            }
        }

        public static final class ColorPulseSettings {
            public boolean enabled = false;
            public float speed = 1.0f;
            public float phaseOffset = 0.0f;
            public PulseWave wave = PulseWave.SINE;
            public ColorPulseMode mode = ColorPulseMode.LERP;
            public boolean clampAlpha = true;
            public final Vector4f colorA = new Vector4f(1f, 1f, 1f, 1f);
            public final Vector4f colorB = new Vector4f(1f, 1f, 1f, 1f);

            public ColorPulseSettings() {
            }

            public ColorPulseSettings(ColorPulseSettings other) {
                copyFrom(other);
            }

            public void copyFrom(ColorPulseSettings other) {
                if (other == null) {
                    enabled = false;
                    speed = 1.0f;
                    phaseOffset = 0.0f;
                    wave = PulseWave.SINE;
                    mode = ColorPulseMode.LERP;
                    clampAlpha = true;
                    colorA.set(1f, 1f, 1f, 1f);
                    colorB.set(1f, 1f, 1f, 1f);
                    return;
                }
                enabled = other.enabled;
                speed = other.speed;
                phaseOffset = other.phaseOffset;
                wave = other.wave;
                mode = other.mode;
                clampAlpha = other.clampAlpha;
                colorA.set(other.colorA);
                colorB.set(other.colorB);
            }

            public ColorPulseSettings copy() {
                ColorPulseSettings copy = new ColorPulseSettings();
                copy.copyFrom(this);
                return copy;
            }
        }

        public enum ColorPulseMode {
            LERP,
            MULTIPLY;

            public static ColorPulseMode fromString(String value) {
                if (value == null || value.isEmpty()) {
                    return LERP;
                }
                try {
                    return ColorPulseMode.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return LERP;
                }
            }
        }

        public enum TriggerMatchMode {
            ANY,
            ALL;

            public static TriggerMatchMode fromString(String value) {
                if (value == null || value.isEmpty()) {
                    return ANY;
                }
                try {
                    return TriggerMatchMode.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return ANY;
                }
            }
        }

        public static final class TriggerCondition {
            public TriggerType type = TriggerType.ALWAYS;
            public String node;

            public static TriggerCondition always() {
                TriggerCondition condition = new TriggerCondition();
                condition.type = TriggerType.ALWAYS;
                return condition;
            }

            public TriggerCondition copy() {
                TriggerCondition copy = new TriggerCondition();
                copy.type = this.type;
                copy.node = this.node;
                return copy;
            }
        }

        public enum TriggerType {
            ALWAYS,
            HOVER_MODEL,
            HOVER_BLOCK,
            HOVER_NODE;

            public static TriggerType fromString(String raw) {
                if (raw == null || raw.isEmpty()) {
                    return ALWAYS;
                }
                String normalized = raw.trim().toUpperCase();
                switch (normalized) {
                    case "HOVER_MODEL":
                        return HOVER_MODEL;
                    case "HOVER_BLOCK":
                        return HOVER_BLOCK;
                    case "HOVER_NODE":
                        return HOVER_NODE;
                    case "ALWAYS":
                    default:
                        return ALWAYS;
                }
            }
        }

        public static final class HoverZone {
            public String node;
            public HoverZoneShape shape = HoverZoneShape.SPHERE;
            public float radius = 0.25f;
            public final Vector3f halfExtents = new Vector3f(0.25f, 0.25f, 0.25f);

            public HoverZone copy() {
                HoverZone zone = new HoverZone();
                zone.node = this.node;
                zone.shape = this.shape;
                zone.radius = this.radius;
                zone.halfExtents.set(this.halfExtents);
                return zone;
            }
        }

        public enum HoverZoneShape {
            SPHERE,
            BOX;

            public static HoverZoneShape fromString(String value) {
                if (value == null || value.isEmpty()) {
                    return SPHERE;
                }
                try {
                    return HoverZoneShape.valueOf(value.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return SPHERE;
                }
            }
        }

        private static final java.util.List<TriggerCondition> DEFAULT_ALWAYS_TRIGGER =
            java.util.Collections.singletonList(TriggerCondition.always());
    }

    public enum AlphaMode {
        OPAQUE,
        MASK,
        BLEND
    }

    public AlphaMode alphaMode = AlphaMode.MASK;
    public float alphaCutoff = 0.5f;

    public void updateTranslucencyFromAlphaMode() {
        this.isTranslucent = (this.alphaMode == AlphaMode.BLEND);
    }

    public void setBaseColorTexturePath(String path) {
        this.baseColorTexture = path;
        this.cachedBaseColorPath = path;
        this.baseColorResource = buildResource(path);
    }

    public ResourceLocation getBaseColorTextureResource() {
        ensureBaseColorCached();
        return baseColorResource;
    }

    private void ensureBaseColorCached() {
        if (Objects.equals(cachedBaseColorPath, baseColorTexture)) {
            return;
        }
        this.cachedBaseColorPath = baseColorTexture;
        this.baseColorResource = buildResource(baseColorTexture);
    }

    private ResourceLocation buildResource(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(path);
        } catch (Exception e) {
            if (GltfLog.LOGGER.isDebugEnabled()) {
                GltfLog.LOGGER.debug("Invalid texture path '{}' for material '{}'", path, name);
            }
            return null;
        }
    }
}
