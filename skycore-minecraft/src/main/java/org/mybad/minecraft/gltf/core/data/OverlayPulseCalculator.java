package org.mybad.minecraft.gltf.core.data;

import org.mybad.minecraft.gltf.GltfLog;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.MathHelper;

final class OverlayPulseCalculator {
    private static final Set<String> PULSE_DEBUG_LOG = ConcurrentHashMap.newKeySet();

    private OverlayPulseCalculator() {}

    static float computeAlphaPulse(String overlayKey, DataMaterial.OverlayLayer.PulseSettings pulse, float timeSeconds) {
        if (pulse == null) {
            return 1.0f;
        }
        float speed = Math.max(0.0f, pulse.speed);
        float min = MathHelper.clamp(pulse.minAlpha, 0.0f, 1.0f);
        float max = MathHelper.clamp(pulse.maxAlpha, 0.0f, 1.0f);
        if (!pulse.enabled) {
            return 1.0f;
        }
        if (max < min) {
            float temp = max;
            max = min;
            min = temp;
        }
        float waveValue;
        if (speed > 0.0001f) {
            float cycle = speed * timeSeconds + pulse.phaseOffset;
            float frac = cycle - (float) Math.floor(cycle);
            switch (pulse.wave) {
                case TRIANGLE:
                    waveValue = 1.0f - Math.abs(frac * 2.0f - 1.0f);
                    break;
                case SAW:
                    waveValue = frac;
                    break;
                case STEP:
                    waveValue = frac >= 0.5f ? 1.0f : 0.0f;
                    break;
                case SINE:
                default:
                    waveValue = 0.5f * (MathHelper.sin((float) (Math.PI * 2.0) * cycle) + 1.0f);
                    break;
            }
        } else {
            waveValue = 1.0f;
        }
        return min + (max - min) * waveValue;
    }

    static float computeColorPulse(DataMaterial.OverlayLayer.ColorPulseSettings pulse, float timeSeconds) {
        if (pulse == null || !pulse.enabled) {
            return 1.0f;
        }
        float speed = Math.max(0.0f, pulse.speed);
        if (speed <= 0.0001f) {
            return 1.0f;
        }
        float cycle = speed * timeSeconds + pulse.phaseOffset;
        float frac = cycle - (float) Math.floor(cycle);
        switch (pulse.wave) {
            case TRIANGLE:
                return 1.0f - Math.abs(frac * 2.0f - 1.0f);
            case SAW:
                return frac;
            case STEP:
                return frac >= 0.5f ? 1.0f : 0.0f;
            case SINE:
            default:
                return 0.5f * (MathHelper.sin((float) (Math.PI * 2.0) * cycle) + 1.0f);
        }
    }

    static float clamp01(float value) {
        return MathHelper.clamp(value, 0.0f, 1.0f);
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static String buildOverlayKey(String materialName, String overlayId) {
        String matName = materialName != null ? materialName : "unknown";
        String overlay = overlayId != null && !overlayId.isEmpty() ? overlayId : "default";
        return matName + "|" + overlay;
    }
    
}
