package org.mybad.minecraft.gltf.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;

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
}
