package org.mybad.minecraft.render.entity.events;

import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.trail.TrailAction;
import org.mybad.minecraft.render.trail.TrailAxis;
import org.mybad.minecraft.render.trail.TrailBlendMode;

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
    
    static TrailParams parseTrail(String effect) {
        if (effect == null) {
            return null;
        }
        TrailParams params = new TrailParams();
        params.rawEffect = effect;
        String[] parts = effect.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                if (params.id == null) {
                    params.id = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            switch (key) {
                case "id":
                    params.id = value;
                    break;
                case "action":
                    params.action = TrailAction.fromString(value);
                    break;
                case "locator":
                    params.locatorStart = value;
                    break;
                case "locator2":
                    params.locatorEnd = value;
                    break;
                case "texture":
                    params.texture = parseTexture(value);
                    break;
                case "sample":
                    params.sampleInterval = parseFloat(value, params.sampleInterval);
                    break;
                case "lifetime":
                    params.lifetime = parseFloat(value, params.lifetime);
                    break;
                case "max_samples":
                    params.maxSamples = parseInt(value, params.maxSamples);
                    break;
                case "blend":
                    params.blendMode = TrailBlendMode.fromString(value);
                    break;
                case "color":
                    parseColor(value, params);
                    break;
                case "width":
                case "thickness":
                    params.width = parseFloat(value, params.width);
                    break;
                case "alpha":
                    params.alpha = clamp01(parseFloat(value, params.alpha));
                    break;
                case "uv_speed":
                    params.uvSpeed = parseFloat(value, params.uvSpeed);
                    break;
                case "segments":
                    params.segments = Math.max(1, parseInt(value, params.segments));
                    break;
                case "axis":
                    params.axis = TrailAxis.fromString(value);
                    break;
                case "uv_mode":
                    params.stretchUv = parseUvStretch(value, params.stretchUv);
                    break;
                case "bloom":
                    params.enableBloom = parseBoolean(value, params.enableBloom);
                    System.out.println("解析成功: " + params.enableBloom);
                    break;
                default:
                    break;
            }
        }
        if (params.id == null && params.texture != null) {
            params.id = params.texture.toString();
        }
        if (params.texture == null && params.id != null) {
            params.texture = inferTexture(params.id);
        }
        if (params.id != null) {
            params.id = params.id.trim();
        }
        return params;
    }
    
    private static ResourceLocation parseTexture(String raw) {
        if (raw.indexOf(':') >= 0) {
            return new ResourceLocation(raw);
        }
        int slash = raw.indexOf('/');
        if (slash > 0) {
            String namespace = raw.substring(0, slash);
            if ("minecraft".equals(namespace) || SkyCoreMod.MOD_ID.equals(namespace)) {
                String path = raw.substring(slash + 1);
                return new ResourceLocation(namespace, path);
            }
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, raw);
    }
    
    private static ResourceLocation inferTexture(String effectId) {
        String sanitized = effectId.replace(':', '_').replace('/', '_');
        return new ResourceLocation(SkyCoreMod.MOD_ID, "textures/trails/" + sanitized + ".png");
    }
    
    private static float parseFloat(String raw, float fallback) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
    
    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
    
    private static void parseColor(String value, TrailParams params) {
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.contains(",")) {
            String[] components = value.split(",");
            if (components.length >= 3) {
                params.colorR = clamp01(parseFloat(components[0], params.colorR));
                params.colorG = clamp01(parseFloat(components[1], params.colorG));
                params.colorB = clamp01(parseFloat(components[2], params.colorB));
            }
            return;
        }
        if (value.length() == 6 || value.length() == 8) {
            try {
                int color = (int) Long.parseLong(value, 16);
                if (value.length() == 8) {
                    params.alpha = clamp01(((color >> 24) & 0xFF) / 255f);
                }
                params.colorR = ((color >> 16) & 0xFF) / 255f;
                params.colorG = ((color >> 8) & 0xFF) / 255f;
                params.colorB = (color & 0xFF) / 255f;
            } catch (NumberFormatException ignored) {
            }
        }
    }
    
    private static float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    private static boolean parseUvStretch(String raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim().toLowerCase();
        if ("stretch".equals(value)) {
            return true;
        }
        if ("tile".equals(value)) {
            return false;
        }
        return fallback;
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim().toLowerCase();
        if (value.isEmpty()) {
            return fallback;
        }
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        return fallback;
    }

    public static final class TrailParams {
        public String id;
        public String rawEffect;
        public TrailAction action = TrailAction.START;
        public String locatorStart;
        public String locatorEnd;
        public ResourceLocation texture;
        public float lifetime = 0.25f;
        public float sampleInterval = 0.02f;
        public int maxSamples = 32;
        public TrailBlendMode blendMode = TrailBlendMode.ADD;
        public float colorR = 1.0f;
        public float colorG = 1.0f;
        public float colorB = 1.0f;
        public float alpha = 1.0f;
        public float uvSpeed = 1.0f;
        public float width = 0.3f;
        public int segments = 4;
        public TrailAxis axis = TrailAxis.Z;
        public boolean stretchUv = true;
        public boolean enableBloom = false;
    }
}
