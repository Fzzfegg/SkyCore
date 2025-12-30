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

    public void setLength(float length) { this.length = length; }
    public void setLoop(boolean loop) { this.loopMode = loop ? LoopMode.LOOP : LoopMode.ONCE; }
    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode != null ? loopMode : LoopMode.ONCE;
    }
    public void setOverridePreviousAnimation(boolean overridePreviousAnimation) {
        this.overridePreviousAnimation = overridePreviousAnimation;
    }
    public void setSpeed(float speed) { this.speed = Math.max(0, speed); }

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
}
