package org.mybad.minecraft.gltf.core.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * Encapsulates overlay override state so {@link GltfRenderModel} no longer needs to
 * manage the bookkeeping directly.
 */
final class OverlayOverrideRegistry {

    private final Map<String, OverlayOverrideState> overrides = new ConcurrentHashMap<>();

    void applyPulse(String materialName, String overlayId,
                    DataMaterial.OverlayLayer.PulseSettings pulse, long durationMs) {
        if (materialName == null || overlayId == null || pulse == null) {
            return;
        }
        String key = buildKey(materialName, overlayId);
        OverlayOverrideState state = overrides.computeIfAbsent(key, unused -> new OverlayOverrideState());
        state.setPulse(pulse, durationMs);
    }

    void clearPulse(String materialName, String overlayId) {
        String key = buildKey(materialName, overlayId);
        OverlayOverrideState state = overrides.get(key);
        if (state == null) {
            return;
        }
        state.clearPulse();
        if (state.isEmpty()) {
            overrides.remove(key);
        }
    }

    void applyColorPulse(String materialName, String overlayId,
                         DataMaterial.OverlayLayer.ColorPulseSettings pulse, long durationMs) {
        if (materialName == null || overlayId == null || pulse == null) {
            return;
        }
        String key = buildKey(materialName, overlayId);
        OverlayOverrideState state = overrides.computeIfAbsent(key, unused -> new OverlayOverrideState());
        state.setColorPulse(pulse, durationMs);
    }

    void clearColorPulse(String materialName, String overlayId) {
        String key = buildKey(materialName, overlayId);
        OverlayOverrideState state = overrides.get(key);
        if (state == null) {
            return;
        }
        state.clearColorPulse();
        if (state.isEmpty()) {
            overrides.remove(key);
        }
    }

    @Nullable
    DataMaterial.OverlayLayer.PulseSettings getActivePulse(String materialName, String overlayId, long now) {
        if (overlayId == null || overlayId.isEmpty()) {
            return null;
        }
        OverlayOverrideState state = overrides.get(buildKey(materialName, overlayId));
        if (state == null) {
            return null;
        }
        DataMaterial.OverlayLayer.PulseSettings active = state.getPulse(now);
        if (active == null && state.isEmpty()) {
            overrides.remove(buildKey(materialName, overlayId));
        }
        return active;
    }

    @Nullable
    DataMaterial.OverlayLayer.ColorPulseSettings getActiveColorPulse(String materialName, String overlayId, long now) {
        if (overlayId == null || overlayId.isEmpty()) {
            return null;
        }
        OverlayOverrideState state = overrides.get(buildKey(materialName, overlayId));
        if (state == null) {
            return null;
        }
        DataMaterial.OverlayLayer.ColorPulseSettings active = state.getColorPulse(now);
        if (active == null && state.isEmpty()) {
            overrides.remove(buildKey(materialName, overlayId));
        }
        return active;
    }

    void clearAll() {
        overrides.clear();
    }

    private String buildKey(String materialName, String overlayId) {
        String matName = materialName != null ? materialName : "unknown";
        String overlay = overlayId != null && !overlayId.isEmpty() ? overlayId : "default";
        return matName + "|" + overlay;
    }

    private static final class OverlayOverrideState {
        private final DataMaterial.OverlayLayer.PulseSettings pulse = new DataMaterial.OverlayLayer.PulseSettings();
        private final DataMaterial.OverlayLayer.ColorPulseSettings colorPulse = new DataMaterial.OverlayLayer.ColorPulseSettings();
        private long pulseExpireAtMs;
        private long colorExpireAtMs;
        private boolean pulseActive;
        private boolean colorPulseActive;

        void setPulse(DataMaterial.OverlayLayer.PulseSettings source, long durationMs) {
            pulse.copyFrom(source);
            pulseActive = true;
            pulseExpireAtMs = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0L;
        }

        void setColorPulse(DataMaterial.OverlayLayer.ColorPulseSettings source, long durationMs) {
            colorPulse.copyFrom(source);
            colorPulseActive = true;
            colorExpireAtMs = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0L;
        }

        @Nullable
        DataMaterial.OverlayLayer.PulseSettings getPulse(long now) {
            if (!pulseActive) {
                return null;
            }
            if (pulseExpireAtMs > 0 && now >= pulseExpireAtMs) {
                pulseActive = false;
                return null;
            }
            return pulse;
        }

        @Nullable
        DataMaterial.OverlayLayer.ColorPulseSettings getColorPulse(long now) {
            if (!colorPulseActive) {
                return null;
            }
            if (colorExpireAtMs > 0 && now >= colorExpireAtMs) {
                colorPulseActive = false;
                return null;
            }
            return colorPulse;
        }

        void clearPulse() {
            pulseActive = false;
        }

        void clearColorPulse() {
            colorPulseActive = false;
        }

        boolean isEmpty() {
            return !pulseActive && !colorPulseActive;
        }
    }
}
