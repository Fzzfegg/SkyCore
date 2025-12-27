package org.mybad.core.animation;

import org.mybad.core.data.*;
import java.util.*;

/**
 * 动画播放器
 * 负责计算和应用动画到模型骨骼
 *
 * 流程：
 * 1. 获取当前时间的关键帧
 * 2. 计算插值系数
 * 3. 应用插值到骨骼变换
 * 4. 支持多层混合（通过权重）
 */
public class AnimationPlayer {
    private Animation animation;
    private AnimationState state;

    public AnimationPlayer(Animation animation) {
        this.animation = animation;
        this.state = new AnimationState(animation);
    }

    /**
     * 更新动画状态
     */
    public void update(float deltaTime) {
        state.update(deltaTime);
    }

    /**
     * 应用动画到模型
     * @param model 模型
     * @param weight 权重（用于混合）[0, 1]
     */
    public void apply(Model model, float weight) {
        if (!state.isPlaying() || animation == null) {
            return;
        }

        weight = Math.max(0, Math.min(1, weight));  // 限制权重范围

        float currentTime = state.getCurrentTime();
        Map<String, Animation.BoneAnimation> boneAnims = animation.getBoneAnimations();

        for (String boneName : boneAnims.keySet()) {
            ModelBone bone = model.getBone(boneName);
            if (bone == null) {
                continue;
            }

            Animation.BoneAnimation boneAnim = boneAnims.get(boneName);

            // 应用位置动画
            if (!boneAnim.positionFrames.isEmpty()) {
                float[] position = interpolateFrames(boneAnim.positionFrames, currentTime);
                applyBlendedTransform(bone.getPosition(), position, weight);
            }

            // 应用旋转动画
            if (!boneAnim.rotationFrames.isEmpty()) {
                float[] rotation = interpolateFrames(boneAnim.rotationFrames, currentTime);
                applyBlendedTransform(bone.getRotation(), rotation, weight);
            }

            // 应用缩放动画
            if (!boneAnim.scaleFrames.isEmpty()) {
                float[] scale = interpolateFrames(boneAnim.scaleFrames, currentTime);
                applyBlendedTransform(bone.getSize(), scale, weight);
            }
        }
    }

    /**
     * 应用动画到模型（完整权重）
     */
    public void apply(Model model) {
        apply(model, 1.0f);
    }

    /**
     * 计算给定时间的插值值
     */
    private float[] interpolateFrames(List<Animation.KeyFrame> frames, float currentTime) {
        if (frames.isEmpty()) {
            return new float[]{0, 0, 0};
        }

        // 找到当前时间前后的关键帧
        Animation.KeyFrame before = null;
        Animation.KeyFrame after = null;

        for (int i = 0; i < frames.size(); i++) {
            Animation.KeyFrame frame = frames.get(i);

            if (frame.timestamp <= currentTime) {
                before = frame;
            }

            if (frame.timestamp >= currentTime) {
                after = frame;
                break;
            }
        }

        // 处理边界情况
        if (before == null && after != null) {
            // 在第一个关键帧之前
            return new float[]{after.value[0], after.value[1], after.value[2]};
        }

        if (before != null && after == null) {
            // 在最后一个关键帧之后
            return new float[]{before.value[0], before.value[1], before.value[2]};
        }

        if (before == null && after == null) {
            return new float[]{0, 0, 0};
        }

        // 两个关键帧之间的插值
        if (before == after) {
            // 恰好在关键帧上
            return new float[]{before.value[0], before.value[1], before.value[2]};
        }

        // 计算插值系数
        float frameDuration = after.timestamp - before.timestamp;
        float timeOffset = currentTime - before.timestamp;
        float t = frameDuration > 0 ? timeOffset / frameDuration : 0;

        // 应用插值
        Interpolation interpolation = before.interpolation;
        float interpolated = interpolation.interpolate(t);

        // 计算最终值
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            result[i] = before.value[i] + (after.value[i] - before.value[i]) * interpolated;
        }

        return result;
    }

    /**
     * 应用混合变换
     * blendedValue = originalValue + (animatedValue - originalValue) * weight
     */
    private void applyBlendedTransform(float[] original, float[] animated, float weight) {
        for (int i = 0; i < 3; i++) {
            original[i] = original[i] + (animated[i] - original[i]) * weight;
        }
    }

    /**
     * 获取动画状态
     */
    public AnimationState getState() {
        return state;
    }

    /**
     * 播放动画
     */
    public void play() {
        state.play();
    }

    /**
     * 暂停动画
     */
    public void pause() {
        state.pause();
    }

    /**
     * 继续播放
     */
    public void resume() {
        state.resume();
    }

    /**
     * 停止动画
     */
    public void stop() {
        state.stop();
    }

    /**
     * 重新开始动画
     */
    public void restart() {
        state.restart();
    }

    /**
     * 设置播放速度
     */
    public void setSpeed(float speed) {
        animation.setSpeed(speed);
    }

    /**
     * 设置当前时间
     */
    public void setCurrentTime(float time) {
        state.setCurrentTime(time);
    }

    /**
     * 获取动画
     */
    public Animation getAnimation() {
        return animation;
    }

    /**
     * 检查动画是否播放完成
     */
    public boolean isFinished() {
        return state.isFinished();
    }

    /**
     * 获取当前进度（0-1）
     */
    public float getProgress() {
        return state.getProgress();
    }

    /**
     * 检查动画是否正在播放
     */
    public boolean isPlaying() {
        return state.isPlaying();
    }

    /**
     * 检查动画是否已停止
     */
    public boolean isStopped() {
        return state.isStopped();
    }
}
