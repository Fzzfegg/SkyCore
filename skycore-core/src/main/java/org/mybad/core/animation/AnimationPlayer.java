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
                // TODO: 临时日志，排查骨骼名称不匹配
                System.out.println("[动画] 骨骼未找到: " + boneName);
                continue;
            }

            Animation.BoneAnimation boneAnim = boneAnims.get(boneName);
            float[] basePosition = bone.getBindPosition();
            float[] baseRotation = bone.getBindRotation();
            float[] baseScale = bone.getBindSize();

            // 应用位置动画
            if (!boneAnim.positionFrames.isEmpty()) {
                float[] position = interpolateFrames(boneAnim.positionFrames, currentTime);
                float[] target = new float[]{
                    basePosition[0] + position[0],
                    basePosition[1] + position[1],
                    basePosition[2] + position[2]
                };
                applyBlendedTransform(bone.getPosition(), target, weight);
            }

            // 应用旋转动画
            if (!boneAnim.rotationFrames.isEmpty()) {
                float[] rotation = interpolateFrames(boneAnim.rotationFrames, currentTime);
                // TODO: 临时日志，只打印一次（第一帧）
                if (currentTime < 0.01f && (boneName.contains("arm") || boneName.contains("hand"))) {
                    System.out.println("[动画] " + boneName + " 旋转: [" + rotation[0] + ", " + rotation[1] + ", " + rotation[2] + "]");
                }
                float[] target = new float[]{
                    baseRotation[0] + rotation[0],
                    baseRotation[1] + rotation[1],
                    baseRotation[2] + rotation[2]
                };
                applyBlendedTransform(bone.getRotation(), target, weight);
            }

            // 应用缩放动画
            if (!boneAnim.scaleFrames.isEmpty()) {
                float[] scale = interpolateFrames(boneAnim.scaleFrames, currentTime);
                float[] target = new float[]{
                    baseScale[0] * scale[0],
                    baseScale[1] * scale[1],
                    baseScale[2] * scale[2]
                };
                applyBlendedTransform(bone.getSize(), target, weight);
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
        int beforeIndex = -1;
        int afterIndex = -1;

        for (int i = 0; i < frames.size(); i++) {
            Animation.KeyFrame frame = frames.get(i);

            if (frame.timestamp <= currentTime) {
                before = frame;
                beforeIndex = i;
            }

            if (frame.timestamp >= currentTime) {
                after = frame;
                afterIndex = i;
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
        String mode = interpolation != null ? interpolation.getName() : "linear";

        if ("step".equalsIgnoreCase(mode)) {
            return new float[]{before.value[0], before.value[1], before.value[2]};
        }

        if ("catmullrom".equalsIgnoreCase(mode)) {
            Animation.KeyFrame prev = beforeIndex > 0 ? frames.get(beforeIndex - 1) : before;
            Animation.KeyFrame next = (afterIndex + 1) < frames.size() ? frames.get(afterIndex + 1) : after;

            float[] result = new float[3];
            for (int i = 0; i < 3; i++) {
                float p0 = prev.value[i];
                float p1 = before.value[i];
                float p2 = after.value[i];
                float p3 = next.value[i];
                result[i] = cubicHermite(p0, p1, p2, p3, t);
            }
            return result;
        }

        if ("bezier".equalsIgnoreCase(mode)) {
            float[] p0 = before.value;
            float[] p3 = after.value;
            float[] p1 = before.post != null ? before.post : p0;
            float[] p2 = after.pre != null ? after.pre : p3;
            return cubicBezier(p0, p1, p2, p3, t);
        }

        float interpolated = interpolation != null ? interpolation.interpolate(t) : t;

        // 计算最终值
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            result[i] = before.value[i] + (after.value[i] - before.value[i]) * interpolated;
        }

        return result;
    }

    private float[] cubicBezier(float[] p0, float[] p1, float[] p2, float[] p3, float t) {
        float u = 1.0f - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            result[i] =
                uuu * p0[i] +
                3f * uu * t * p1[i] +
                3f * u * tt * p2[i] +
                ttt * p3[i];
        }
        return result;
    }

    /**
     * Catmull-Rom/Hermite 曲线插值
     */
    private float cubicHermite(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1) +
            (-p0 + p2) * t +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
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
