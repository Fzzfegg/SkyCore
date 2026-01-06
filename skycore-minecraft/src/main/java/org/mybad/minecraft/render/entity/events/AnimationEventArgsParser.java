package org.mybad.minecraft.render.entity.events;

import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.SkyCoreMod;

public final class AnimationEventArgsParser {
    private AnimationEventArgsParser() {
    }

    static ParticleParams parseParticle(String effect) {
        if (effect == null) {
            return null;
        }
        String path = null;
        int count = 0;
        ParticleTargetMode mode = ParticleTargetMode.LOOK;
        float yawOffset = 0.0f;
        String[] parts = effect.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                if (path == null) {
                    path = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("effect".equals(key) || "particle".equals(key) || "path".equals(key)) {
                path = value;
            } else if ("count".equals(key) || "num".equals(key) || "amount".equals(key)) {
                try {
                    count = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            } else if ("mode".equals(key)) {
                mode = ParticleTargetMode.parse(value);
            } else if ("yaw".equals(key)) {
                try {
                    yawOffset = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (path == null) {
            path = effect.trim();
        }
        ParticleParams params = new ParticleParams();
        params.path = path;
        params.count = count;
        params.mode = mode;
        params.yawOffset = yawOffset;
        return params;
    }

    static SoundParams parseSound(String effect) {
        if (effect == null) {
            return null;
        }
        String soundId = null;
        float pitch = 1.0f;
        float volume = 1.0f;
        String[] parts = effect.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                if (soundId == null) {
                    soundId = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("sound".equals(key)) {
                soundId = value;
            } else if ("pitch".equals(key)) {
                try {
                    pitch = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
            } else if ("volume".equals(key)) {
                try {
                    volume = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (soundId == null || soundId.isEmpty()) {
            soundId = effect;
        }
        soundId = soundId.trim();
        if (soundId.endsWith(".ogg")) {
            soundId = soundId.substring(0, soundId.length() - 4);
        }
        ResourceLocation soundLocation = parseSoundLocation(soundId);
        if (volume > 1.0f) {
            volume = volume / 100.0f;
        }
        SoundParams params = new SoundParams();
        params.soundId = soundLocation;
        params.volume = Math.max(0f, volume);
        params.pitch = Math.max(0f, pitch);
        return params;
    }

    private static ResourceLocation parseSoundLocation(String soundId) {
        if (soundId.contains(":")) {
            return new ResourceLocation(soundId);
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, soundId);
    }

    enum ParticleTargetMode {
        LOOK,
        BODY,
        WORLD;

        static ParticleTargetMode parse(String raw) {
            if (raw == null || raw.isEmpty()) {
                return LOOK;
            }
            String value = raw.trim().toLowerCase();
            if ("body".equals(value)) {
                return BODY;
            }
            if ("world".equals(value)) {
                return WORLD;
            }
            return LOOK;
        }
    }

    static final class ParticleParams {
        String path;
        int count;
        ParticleTargetMode mode;
        float yawOffset;
    }

    static final class SoundParams {
        ResourceLocation soundId;
        float volume;
        float pitch;
    }
}
