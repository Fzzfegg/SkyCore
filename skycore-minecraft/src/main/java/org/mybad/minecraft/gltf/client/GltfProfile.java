package org.mybad.minecraft.gltf.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Immutable profile definition for GLTF entity rendering.
 */
public final class GltfProfile {

    private final String name;
    private final String modelPath;
    private final String texturePath;
    private final float modelScale;
    private final int fps;
    private final double blendDuration;
    private final String hash;
    private final long version;
    private final Map<String, AnimationClip> animations;

    public GltfProfile(String name,
                       String modelPath,
                       @Nullable String texturePath,
                       float modelScale,
                       int fps,
                       double blendDuration,
                       @Nullable String hash,
                       long version,
                       Map<String, AnimationClip> animations) {
        this.name = name;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
        this.modelScale = modelScale > 0f ? modelScale : 1.0f;
        this.fps = fps > 0 ? fps : 24;
        this.blendDuration = blendDuration > 0 ? blendDuration : 0.2;
        this.hash = hash != null ? hash : "";
        this.version = version;
        if (animations == null || animations.isEmpty()) {
            this.animations = Collections.emptyMap();
        } else {
            this.animations = Collections.unmodifiableMap(new HashMap<>(animations));
        }
    }

    public String getName() {
        return name;
    }

    public String getModelPath() {
        return modelPath;
    }

    @Nullable
    public String getTexturePath() {
        return texturePath;
    }

    public float getModelScale() {
        return modelScale;
    }

    public int getFps() {
        return fps;
    }

    public double getBlendDuration() {
        return blendDuration;
    }

    public String getHash() {
        return hash;
    }

    public long getVersion() {
        return version;
    }

    public Map<String, AnimationClip> getAnimations() {
        return animations;
    }

    public boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }

    @Nullable
    public AnimationClip getAnimation(String name) {
        return animations.get(name);
    }

    public static final class AnimationClip {
        private final double startTime;
        private final double endTime;
        private final double speed;
        private final Double blendDuration;
        private final Boolean loop;
        private final Boolean holdLastFrame;

        public AnimationClip(double startTime,
                             double endTime,
                             double speed,
                             @Nullable Double blendDuration,
                             @Nullable Boolean loop,
                             @Nullable Boolean holdLastFrame) {
            this.startTime = startTime;
            this.endTime = endTime <= startTime ? startTime : endTime;
            this.speed = speed > 0 ? speed : 1.0;
            this.blendDuration = blendDuration;
            this.loop = loop;
            this.holdLastFrame = holdLastFrame;
        }

        public double getStartFrame() {
            return startTime;
        }

        public double getEndFrame() {
            return endTime;
        }

        public double getAnimationSpeed(int fps) {
            double durationSeconds = getDurationSeconds(fps);
            if (durationSeconds <= 0) {
                double safeFps = Math.max(fps, 1);
                durationSeconds = 1.0 / safeFps;
            }
            return speed / durationSeconds;
        }

        public double getDurationSeconds(int fps) {
            double frameSpan = endTime - startTime;
            if (frameSpan <= 0) {
                return 0.0;
            }
            return frameSpan / Math.max(fps, 1);
        }

        public float getBlendDurationSeconds(double defaultDuration) {
            double value = blendDuration != null ? blendDuration : defaultDuration;
            if (value < 0) {
                value = 0;
            }
            return (float) value;
        }

        public boolean isLoop() {
            return loop != null ? loop : Boolean.TRUE;
        }

        public boolean shouldHoldLastFrame() {
            return holdLastFrame != null ? holdLastFrame : Boolean.FALSE;
        }

        public boolean resolveLoop(boolean fallback) {
            return loop != null ? loop : fallback;
        }

        public boolean resolveHoldLastFrame(boolean fallback) {
            return holdLastFrame != null ? holdLastFrame : fallback;
        }
    }
}
