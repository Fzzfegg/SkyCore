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
    private final float[] tmpPosition = new float[3];
    private final float[] tmpRotation = new float[3];
    private final float[] tmpScale = new float[3];
    private final float[] tmpTarget = new float[3];
    private final float[] tmpQuatA = new float[4];
    private final float[] tmpQuatB = new float[4];
    private final float[] tmpQuatOut = new float[4];
    private final float[] tmpEuler = new float[3];
    private final Map<String, FrameCursor> frameCursors = new HashMap<>();
    private final Map<String, RotationState> rotationStates = new HashMap<>();
    private final Map<String, ModelBone> boneCache = new HashMap<>();
    private Model cachedModel;
    private float lastSampleTime = Float.NaN;

    private static final class FrameCursor {
        int positionIndex = -1;
        int rotationIndex = -1;
        int scaleIndex = -1;

        void reset() {
            positionIndex = -1;
            rotationIndex = -1;
            scaleIndex = -1;
        }
    }

    private static final class RotationState {
        final float[] last = new float[]{Float.NaN, Float.NaN, Float.NaN};

        void reset() {
            last[0] = Float.NaN;
            last[1] = Float.NaN;
            last[2] = Float.NaN;
        }
    }

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
        if (animation == null || !state.shouldApply()) {
            return;
        }

        if (cachedModel != model) {
            cachedModel = model;
            boneCache.clear();
            rotationStates.clear();
        }

        weight = Math.max(0, Math.min(1, weight));  // 限制权重范围

        float currentTime = state.getCurrentTime();
        Map<String, Animation.BoneAnimation> boneAnims = animation.getBoneAnimations();

        boolean hasLastSample = !Float.isNaN(lastSampleTime);
        boolean forward = hasLastSample && currentTime >= lastSampleTime;
        if (!hasLastSample) {
            resetFrameCursors();
        } else if (!forward) {
            resetFrameCursors();
            resetRotationStates();
        }
        lastSampleTime = currentTime;

        for (Map.Entry<String, Animation.BoneAnimation> entry : boneAnims.entrySet()) {
            String boneName = entry.getKey();
            Animation.BoneAnimation boneAnim = entry.getValue();
            ModelBone bone = boneCache.get(boneName);
            if (bone == null && !boneCache.containsKey(boneName)) {
                bone = model.getBone(boneName);
                boneCache.put(boneName, bone);
            }
            if (bone == null) {
                continue;
            }
            FrameCursor cursor = frameCursors.get(boneName);
            if (cursor == null) {
                cursor = new FrameCursor();
                frameCursors.put(boneName, cursor);
            }
            float[] basePosition = bone.getBindPosition();
            float[] baseRotation = bone.getBindRotation();
            float[] baseScale = bone.getBindSize();

            // 应用位置动画
            if (!boneAnim.positionFrames.isEmpty()) {
                cursor.positionIndex = interpolateFrames(boneAnim.positionFrames, currentTime, tmpPosition, cursor.positionIndex, forward);
                tmpTarget[0] = basePosition[0] + tmpPosition[0];
                tmpTarget[1] = basePosition[1] + tmpPosition[1];
                tmpTarget[2] = basePosition[2] + tmpPosition[2];
                applyBlendedTransform(bone.getPosition(), tmpTarget, weight);
            }

            // 应用旋转动画
            if (!boneAnim.rotationFrames.isEmpty()) {
                cursor.rotationIndex = interpolateRotationFrames(boneAnim.rotationFrames, currentTime, tmpRotation, cursor.rotationIndex, forward);
                applyRotationUnwrap(boneName, tmpRotation);
                tmpTarget[0] = baseRotation[0] + tmpRotation[0];
                tmpTarget[1] = baseRotation[1] + tmpRotation[1];
                tmpTarget[2] = baseRotation[2] + tmpRotation[2];
                applyBlendedRotation(bone.getRotation(), tmpTarget, weight);
            }

            // 应用缩放动画
            if (!boneAnim.scaleFrames.isEmpty()) {
                cursor.scaleIndex = interpolateFrames(boneAnim.scaleFrames, currentTime, tmpScale, cursor.scaleIndex, forward);
                tmpTarget[0] = baseScale[0] * tmpScale[0];
                tmpTarget[1] = baseScale[1] * tmpScale[1];
                tmpTarget[2] = baseScale[2] * tmpScale[2];
                applyBlendedTransform(bone.getSize(), tmpTarget, weight);
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
    private int interpolateFrames(List<Animation.KeyFrame> frames, float currentTime, float[] out, int lastIndex, boolean forward) {
        if (frames.isEmpty()) {
            setVec(out, 0f, 0f, 0f);
            return lastIndex;
        }

        int afterIndex = forward ? lowerBoundFrom(frames, currentTime, lastIndex) : lowerBound(frames, currentTime);
        Animation.KeyFrame after = afterIndex < frames.size() ? frames.get(afterIndex) : null;
        int beforeIndex = afterIndex - 1;
        Animation.KeyFrame before = beforeIndex >= 0 ? frames.get(beforeIndex) : null;

        // 处理边界情况
        if (before == null && after != null) {
            // 在第一个关键帧之前
            float[] v = getNextValue(after);
            setVec(out, v[0], v[1], v[2]);
            return afterIndex;
        }

        if (before != null && after == null) {
            // 在最后一个关键帧之后
            float[] v = getPrevValue(before);
            setVec(out, v[0], v[1], v[2]);
            return afterIndex;
        }

        if (before == null && after == null) {
            setVec(out, 0f, 0f, 0f);
            return afterIndex;
        }

        if (after != null && after.timestamp == currentTime) {
            float[] v = getNextValue(after);
            setVec(out, v[0], v[1], v[2]);
            return afterIndex;
        }

        // 计算插值系数
        float frameDuration = after.timestamp - before.timestamp;
        float timeOffset = currentTime - before.timestamp;
        float t = frameDuration > 0 ? timeOffset / frameDuration : 0;

        // 应用插值
        Interpolation interpolation = before.interpolation;
        String mode = interpolation != null ? interpolation.getName() : "linear";

        float[] start = getPrevValue(before);
        float[] end = getNextValue(after);

        if ("step".equalsIgnoreCase(mode)) {
            setVec(out, start[0], start[1], start[2]);
            return afterIndex;
        }

        if ("catmullrom".equalsIgnoreCase(mode)) {
            Animation.KeyFrame prev = beforeIndex > 0 ? frames.get(beforeIndex - 1) : before;
            Animation.KeyFrame next = (afterIndex + 1) < frames.size() ? frames.get(afterIndex + 1) : after;

            float[] p0v = getPrevValue(prev);
            float[] p1v = start;
            float[] p2v = end;
            float[] p3v = getNextValue(next);
            for (int i = 0; i < 3; i++) {
                out[i] = cubicHermite(p0v[i], p1v[i], p2v[i], p3v[i], t);
            }
            return afterIndex;
        }

        if ("bezier".equalsIgnoreCase(mode)) {
            float[] p0 = before.value;
            float[] p3 = after.value;
            float[] p1 = before.post != null ? before.post : p0;
            float[] p2 = after.pre != null ? after.pre : p3;
            cubicBezier(p0, p1, p2, p3, t, out);
            return afterIndex;
        }

        float interpolated = interpolation != null ? interpolation.interpolate(t) : t;

        // 计算最终值
        for (int i = 0; i < 3; i++) {
            out[i] = start[i] + (end[i] - start[i]) * interpolated;
        }
        return afterIndex;
    }

    private int interpolateRotationFrames(List<Animation.KeyFrame> frames, float currentTime, float[] out, int lastIndex, boolean forward) {
        if (frames.isEmpty()) {
            setVec(out, 0f, 0f, 0f);
            return lastIndex;
        }

        int afterIndex = forward ? lowerBoundFrom(frames, currentTime, lastIndex) : lowerBound(frames, currentTime);
        Animation.KeyFrame after = afterIndex < frames.size() ? frames.get(afterIndex) : null;
        int beforeIndex = afterIndex - 1;
        Animation.KeyFrame before = beforeIndex >= 0 ? frames.get(beforeIndex) : null;

        if (before == null && after != null) {
            float[] v = getNextValue(after);
            setVec(out, v[0], v[1], v[2]);
            return afterIndex;
        }

        if (before != null && after == null) {
            float[] v = getPrevValue(before);
            setVec(out, v[0], v[1], v[2]);
            return afterIndex;
        }

        if (before == null) {
            setVec(out, 0f, 0f, 0f);
            return afterIndex;
        }

        if (after != null && after.timestamp == currentTime) {
            float[] v = getNextValue(after);
            setVec(out, v[0], v[1], v[2]);
            return afterIndex;
        }

        float frameDuration = after.timestamp - before.timestamp;
        float timeOffset = currentTime - before.timestamp;
        float t = frameDuration > 0 ? timeOffset / frameDuration : 0;

        Interpolation interpolation = before.interpolation;
        String mode = interpolation != null ? interpolation.getName() : "linear";

        float[] start = getPrevValue(before);
        float[] end = getNextValue(after);

        if ("step".equalsIgnoreCase(mode)) {
            setVec(out, start[0], start[1], start[2]);
            return afterIndex;
        }

        if ("catmullrom".equalsIgnoreCase(mode)) {
            Animation.KeyFrame prev = beforeIndex > 0 ? frames.get(beforeIndex - 1) : before;
            Animation.KeyFrame next = (afterIndex + 1) < frames.size() ? frames.get(afterIndex + 1) : after;

            float[] p0v = getPrevValue(prev);
            float[] p1v = start;
            float[] p2v = end;
            float[] p3v = getNextValue(next);
            for (int i = 0; i < 3; i++) {
                float p1 = p1v[i];
                float p2 = p1 + shortestAngleDelta(p1, p2v[i]);
                float p0 = p1 + shortestAngleDelta(p1, p0v[i]);
                float p3 = p2 + shortestAngleDelta(p2, p3v[i]);
                out[i] = cubicHermite(p0, p1, p2, p3, t);
            }
            return afterIndex;
        }

        if ("bezier".equalsIgnoreCase(mode)) {
            for (int i = 0; i < 3; i++) {
                float p0 = before.value[i];
                float p3 = p0 + shortestAngleDelta(p0, after.value[i]);
                float p1 = before.post != null ? before.post[i] : p0;
                float p2 = after.pre != null ? after.pre[i] : after.value[i];
                float p1u = p0 + shortestAngleDelta(p0, p1);
                float p2u = p3 + shortestAngleDelta(p3, p2);
                out[i] = cubicBezier1D(p0, p1u, p2u, p3, t);
            }
            return afterIndex;
        }

        float interpolated = interpolation != null ? interpolation.interpolate(t) : t;
        applyQuaternionInterpolation(start, end, interpolated, out);
        return afterIndex;
    }

    private float[] getPrevValue(Animation.KeyFrame frame) {
        return frame.post != null ? frame.post : frame.value;
    }

    private float[] getNextValue(Animation.KeyFrame frame) {
        return frame.pre != null ? frame.pre : frame.value;
    }

    private float shortestAngleDelta(float from, float to) {
        float delta = (to - from) % 360f;
        if (delta > 180f) {
            delta -= 360f;
        } else if (delta < -180f) {
            delta += 360f;
        }
        return delta;
    }

    private void applyQuaternionInterpolation(float[] startEuler, float[] endEuler, float t, float[] out) {
        eulerToQuaternion(startEuler, tmpQuatA);
        eulerToQuaternion(endEuler, tmpQuatB);
        slerpQuaternion(tmpQuatA, tmpQuatB, t, tmpQuatOut);
        quaternionToEuler(tmpQuatOut, tmpEuler);
        setVec(out, tmpEuler[0], tmpEuler[1], tmpEuler[2]);
    }

    private void eulerToQuaternion(float[] euler, float[] out) {
        float x = (float) Math.toRadians(euler[0]);
        float y = (float) Math.toRadians(euler[1]);
        float z = (float) Math.toRadians(euler[2]);

        float cx = (float) Math.cos(x * 0.5f);
        float sx = (float) Math.sin(x * 0.5f);
        float cy = (float) Math.cos(y * 0.5f);
        float sy = (float) Math.sin(y * 0.5f);
        float cz = (float) Math.cos(z * 0.5f);
        float sz = (float) Math.sin(z * 0.5f);

        out[0] = sx * cy * cz - cx * sy * sz;
        out[1] = cx * sy * cz + sx * cy * sz;
        out[2] = cx * cy * sz - sx * sy * cz;
        out[3] = cx * cy * cz + sx * sy * sz;
    }

    private void quaternionToEuler(float[] quat, float[] out) {
        float x = quat[0];
        float y = quat[1];
        float z = quat[2];
        float w = quat[3];

        float sinr_cosp = 2 * (w * x + y * z);
        float cosr_cosp = 1 - 2 * (x * x + y * y);
        out[0] = (float) Math.toDegrees(Math.atan2(sinr_cosp, cosr_cosp));

        float sinp = 2 * (w * y - z * x);
        if (Math.abs(sinp) >= 1) {
            out[1] = (float) Math.copySign(90, sinp);
        } else {
            out[1] = (float) Math.toDegrees(Math.asin(sinp));
        }

        float siny_cosp = 2 * (w * z + x * y);
        float cosy_cosp = 1 - 2 * (y * y + z * z);
        out[2] = (float) Math.toDegrees(Math.atan2(siny_cosp, cosy_cosp));
    }

    private void slerpQuaternion(float[] qa, float[] qb, float t, float[] out) {
        float dot = qa[0] * qb[0] + qa[1] * qb[1] + qa[2] * qb[2] + qa[3] * qb[3];
        float[] q2 = qb;
        if (dot < 0f) {
            dot = -dot;
            q2 = tmpQuatB;
            q2[0] = -qb[0];
            q2[1] = -qb[1];
            q2[2] = -qb[2];
            q2[3] = -qb[3];
        }
        if (dot > 0.9995f) {
            out[0] = qa[0] + (q2[0] - qa[0]) * t;
            out[1] = qa[1] + (q2[1] - qa[1]) * t;
            out[2] = qa[2] + (q2[2] - qa[2]) * t;
            out[3] = qa[3] + (q2[3] - qa[3]) * t;
            normalizeQuaternion(out);
            return;
        }

        double theta0 = Math.acos(Math.max(-1.0f, Math.min(1.0f, dot)));
        double sinTheta0 = Math.sin(theta0);
        double theta = theta0 * t;
        double sinTheta = Math.sin(theta);

        double s0 = Math.cos(theta) - dot * sinTheta / sinTheta0;
        double s1 = sinTheta / sinTheta0;

        out[0] = (float) (s0 * qa[0] + s1 * q2[0]);
        out[1] = (float) (s0 * qa[1] + s1 * q2[1]);
        out[2] = (float) (s0 * qa[2] + s1 * q2[2]);
        out[3] = (float) (s0 * qa[3] + s1 * q2[3]);
        normalizeQuaternion(out);
    }

    private void normalizeQuaternion(float[] q) {
        float len = (float) Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        if (len == 0f) {
            q[0] = q[1] = q[2] = 0f;
            q[3] = 1f;
            return;
        }
        float inv = 1.0f / len;
        q[0] *= inv;
        q[1] *= inv;
        q[2] *= inv;
        q[3] *= inv;
    }

    private float cubicBezier1D(float p0, float p1, float p2, float p3, float t) {
        float u = 1.0f - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;
        return uuu * p0 + 3f * uu * t * p1 + 3f * u * tt * p2 + ttt * p3;
    }

    private void cubicBezier(float[] p0, float[] p1, float[] p2, float[] p3, float t, float[] out) {
        float u = 1.0f - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;

        for (int i = 0; i < 3; i++) {
            out[i] =
                uuu * p0[i] +
                3f * uu * t * p1[i] +
                3f * u * tt * p2[i] +
                ttt * p3[i];
        }
    }

    private void setVec(float[] out, float x, float y, float z) {
        if (out == null || out.length < 3) {
            return;
        }
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }

    private void resetFrameCursors() {
        for (FrameCursor cursor : frameCursors.values()) {
            cursor.reset();
        }
    }

    private void resetRotationStates() {
        for (RotationState state : rotationStates.values()) {
            state.reset();
        }
    }

    private void applyRotationUnwrap(String boneName, float[] rotation) {
        if (rotation == null || rotation.length < 3) {
            return;
        }
        RotationState state = rotationStates.computeIfAbsent(boneName, key -> new RotationState());
        for (int i = 0; i < 3; i++) {
            float value = rotation[i];
            float last = state.last[i];
            if (Float.isNaN(last)) {
                state.last[i] = value;
                continue;
            }
            float delta = value - last;
            delta = wrapDelta(delta);
            value = last + delta;
            rotation[i] = value;
            state.last[i] = value;
        }
    }

    private float wrapDelta(float delta) {
        delta = delta % 360f;
        if (delta > 180f) {
            delta -= 360f;
        } else if (delta < -180f) {
            delta += 360f;
        }
        return delta;
    }

    private int lowerBoundFrom(List<Animation.KeyFrame> frames, float time, int lastIndex) {
        int size = frames.size();
        if (size == 0) {
            return 0;
        }
        if (lastIndex < 0) {
            return lowerBound(frames, time);
        }
        if (lastIndex >= size) {
            if (time >= frames.get(size - 1).timestamp) {
                return size;
            }
            return lowerBound(frames, time);
        }
        if (frames.get(lastIndex).timestamp >= time) {
            return lastIndex;
        }
        int i = lastIndex + 1;
        while (i < size && frames.get(i).timestamp < time) {
            i++;
        }
        return i;
    }

    private int lowerBound(List<Animation.KeyFrame> frames, float time) {
        int low = 0;
        int high = frames.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (frames.get(mid).timestamp < time) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return low;
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

    private void applyBlendedRotation(float[] original, float[] animated, float weight) {
        if (weight <= 0f) {
            return;
        }
        if (weight >= 0.999f) {
            setVec(original, animated[0], animated[1], animated[2]);
            return;
        }
        eulerToQuaternion(original, tmpQuatA);
        eulerToQuaternion(animated, tmpQuatB);
        slerpQuaternion(tmpQuatA, tmpQuatB, weight, tmpQuatOut);
        quaternionToEuler(tmpQuatOut, tmpEuler);
        setVec(original, tmpEuler[0], tmpEuler[1], tmpEuler[2]);
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
