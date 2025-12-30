package org.mybad.core.animation;

import java.util.*;

/**
 * 动画数据容器
 * 包含完整的骨骼动画关键帧信息
 */
public class Animation {
    public enum LoopMode {
        ONCE,
        LOOP,
        HOLD_ON_LAST_FRAME
    }

    private String name;
    private float length;           // 动画长度（秒）
    private LoopMode loopMode;      // 循环模式
    private boolean overridePreviousAnimation; // 是否覆盖之前动画
    private Map<String, BoneAnimation> boneAnimations;
    private float speed = 1.0f;     // 播放速度倍数
    private final List<Event> particleEvents = new ArrayList<>();
    private final List<Event> soundEvents = new ArrayList<>();

    public Animation(String name) {
        this.name = name;
        this.length = 0;
        this.loopMode = LoopMode.ONCE;
        this.overridePreviousAnimation = false;
        this.boneAnimations = new HashMap<>();
    }

    public Animation(String name, float length) {
        this.name = name;
        this.length = length;
        this.loopMode = LoopMode.ONCE;
        this.overridePreviousAnimation = false;
        this.boneAnimations = new HashMap<>();
    }

    /**
     * 添加骨骼动画
     */
    public void addBoneAnimation(String boneName, BoneAnimation boneAnim) {
        boneAnimations.put(boneName, boneAnim);
    }

    /**
     * 获取骨骼动画
     */
    public BoneAnimation getBoneAnimation(String boneName) {
        return boneAnimations.get(boneName);
    }

    /**
     * 检查是否包含特定骨骼的动画
     */
    public boolean hasBoneAnimation(String boneName) {
        return boneAnimations.containsKey(boneName);
    }

    // Getters & Setters
    public String getName() { return name; }
    public float getLength() { return length; }
    public boolean isLoop() { return loopMode == LoopMode.LOOP; }
    public boolean isHoldOnLastFrame() { return loopMode == LoopMode.HOLD_ON_LAST_FRAME; }
    public LoopMode getLoopMode() { return loopMode; }
    public boolean isOverridePreviousAnimation() { return overridePreviousAnimation; }
    public Map<String, BoneAnimation> getBoneAnimations() { return boneAnimations; }
    public float getSpeed() { return speed; }
    public List<Event> getParticleEvents() { return particleEvents; }
    public List<Event> getSoundEvents() { return soundEvents; }

    public void setLength(float length) { this.length = length; }
    public void setLoop(boolean loop) { this.loopMode = loop ? LoopMode.LOOP : LoopMode.ONCE; }
    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode != null ? loopMode : LoopMode.ONCE;
    }
    public void setOverridePreviousAnimation(boolean overridePreviousAnimation) {
        this.overridePreviousAnimation = overridePreviousAnimation;
    }
    public void setSpeed(float speed) { this.speed = Math.max(0, speed); }

    public void addParticleEvent(float timestamp, String effect, String locator) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        particleEvents.add(new Event(Event.Type.PARTICLE, timestamp, effect, locator));
        particleEvents.sort(Comparator.comparingDouble(Event::getTimestamp));
    }

    public void addSoundEvent(float timestamp, String effect, String locator) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        soundEvents.add(new Event(Event.Type.SOUND, timestamp, effect, locator));
        soundEvents.sort(Comparator.comparingDouble(Event::getTimestamp));
    }

    /**
     * 骨骼动画数据
     */
    public static class BoneAnimation {
        public String boneName;
        public List<KeyFrame> positionFrames;
        public List<KeyFrame> rotationFrames;
        public List<KeyFrame> scaleFrames;

        public BoneAnimation(String boneName) {
            this.boneName = boneName;
            this.positionFrames = new ArrayList<>();
            this.rotationFrames = new ArrayList<>();
            this.scaleFrames = new ArrayList<>();
        }
    }

    /**
     * 关键帧数据
     */
    public static class KeyFrame {
        public float timestamp;         // 关键帧时间点
        public float[] value;           // 值 [x, y, z]
        public Interpolation interpolation;  // 插值方式
        public float[] post;            // easing出切线（可选）
        public float[] pre;             // easing入切线（可选）

        public KeyFrame(float timestamp, float[] value) {
            this.timestamp = timestamp;
            this.value = value;
            this.interpolation = new InterpolationImpl.Linear();
        }

        public KeyFrame(float timestamp, float[] value, String interpolationMode) {
            this.timestamp = timestamp;
            this.value = value;
            this.interpolation = InterpolationImpl.getInstance(interpolationMode);
        }
    }

    /**
     * 动画事件（声音/粒子）
     */
    public static final class Event {
        public enum Type {
            PARTICLE,
            SOUND
        }

        private final Type type;
        private final float timestamp;
        private final String effect;
        private final String locator;

        public Event(Type type, float timestamp, String effect, String locator) {
            this.type = type;
            this.timestamp = timestamp;
            this.effect = effect;
            this.locator = locator;
        }

        public Type getType() { return type; }
        public float getTimestamp() { return timestamp; }
        public String getEffect() { return effect; }
        public String getLocator() { return locator; }
    }
}
