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
    private final float[] tmpQuatC = new float[4];
    private final float[] tmpQuatD = new float[4];
    private final float[] tmpQuatE = new float[4];
    private final float[] tmpEuler = new float[3];
    private final float[] tmpCatmullP0 = new float[3];
    private final float[] tmpCatmullP3 = new float[3];
    private final float[] tmpCatmullTimes = new float[4];
    private final Map<String, FrameCursor> frameCursors = new HashMap<>();
    private final IdentityHashMap<float[], float[]> quaternionCache = new IdentityHashMap<>();
    private final Map<String, ModelBone> boneCache = new HashMap<>();
    private Model cachedModel;
    private float lastSampleTime = Float.NaN;
    private static final float[] IDENTITY_QUATERNION = new float[]{0f, 0f, 0f, 1f};
    private static final float EPSILON = 1e-6f;
    private static final float[] ZERO_VECTOR = new float[]{0f, 0f, 0f};

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
            float[] actualStart = getFrameValue(before);
            float[] actualEnd = getFrameValue(after);
            resolveCatmullContext(frames, beforeIndex, afterIndex, actualStart, actualEnd,
                tmpCatmullP0, tmpCatmullP3, tmpCatmullTimes);
            catmullInterpolate(tmpCatmullP0, actualStart, actualEnd, tmpCatmullP3, tmpCatmullTimes, t, out);
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
            quaternionToEuler(getQuaternion(start), tmpEuler);
            setVec(out, tmpEuler[0], tmpEuler[1], tmpEuler[2]);
            return afterIndex;
        }

        boolean requiresEuler = requiresEulerInterpolation(start, end);

        if ("catmullrom".equalsIgnoreCase(mode)) {
            float[] actualStart = getFrameValue(before);
            float[] actualEnd = getFrameValue(after);
            resolveCatmullContext(frames, beforeIndex, afterIndex, actualStart, actualEnd,
                tmpCatmullP0, tmpCatmullP3, tmpCatmullTimes);
            System.out.println(String.format(
                "Rotation timing: current=%s, before=%s, after=%s, duration=%s",
                currentTime, before.timestamp, after.timestamp, frameDuration));
            System.out.println(String.format(
                "Catmull rotation frames: requiresEuler=%s, t=%s, p0=%s, p1=%s, p2=%s, p3=%s",
                requiresEuler, t,
                Arrays.toString(tmpCatmullP0), Arrays.toString(actualStart),
                Arrays.toString(actualEnd), Arrays.toString(tmpCatmullP3)));
            catmullInterpolate(tmpCatmullP0, actualStart, actualEnd, tmpCatmullP3, tmpCatmullTimes, t, out);
            return afterIndex;
        }

        if ("bezier".equalsIgnoreCase(mode)) {
            if (requiresEuler) {
                float[] p0 = before.value;
                float[] p1 = before.post != null ? before.post : p0;
                float[] p2 = after.pre != null ? after.pre : after.value;
                float[] p3 = after.value;
                cubicBezier(p0, p1, p2, p3, t, out);
                return afterIndex;
            }
            float[] q0 = getQuaternion(before.value);
            float[] q1 = getQuaternion(before.post != null ? before.post : before.value);
            float[] q2 = getQuaternion(after.pre != null ? after.pre : after.value);
            float[] q3 = getQuaternion(after.value);
            quaternionBezier(q0, q1, q2, q3, t, tmpQuatOut);
            quaternionToEuler(tmpQuatOut, tmpEuler);
            setVec(out, tmpEuler[0], tmpEuler[1], tmpEuler[2]);
            return afterIndex;
        }

        float interpolated = interpolation != null ? interpolation.interpolate(t) : t;
        if (requiresEuler) {
            for (int i = 0; i < 3; i++) {
                out[i] = start[i] + (end[i] - start[i]) * interpolated;
            }
            return afterIndex;
        }

        float[] startQuat = getQuaternion(start);
        float[] endQuat = getQuaternion(end);
        slerpQuaternion(startQuat, endQuat, interpolated, tmpQuatOut);
        quaternionToEuler(tmpQuatOut, tmpEuler);
        setVec(out, tmpEuler[0], tmpEuler[1], tmpEuler[2]);
        return afterIndex;
    }

    private float[] getPrevValue(Animation.KeyFrame frame) {
        return frame.post != null ? frame.post : frame.value;
    }

    private float[] getNextValue(Animation.KeyFrame frame) {
        return frame.pre != null ? frame.pre : frame.value;
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

    private void resolveCatmullContext(List<Animation.KeyFrame> frames, int beforeIndex, int afterIndex,
                                       float[] startValue, float[] endValue,
                                       float[] outP0, float[] outP3, float[] outTimes) {
        if (frames.isEmpty()) {
            Arrays.fill(outP0, 0f);
            Arrays.fill(outP3, 0f);
            Arrays.fill(outTimes, 0f);
            return;
        }
        Animation.KeyFrame before = frames.get(beforeIndex);
        Animation.KeyFrame after = frames.get(afterIndex);
        float t1 = before.timestamp;
        float t2 = after.timestamp;
        outTimes[1] = t1;
        outTimes[2] = t2;

        float defaultDt = t2 - t1;
        if (Math.abs(defaultDt) < EPSILON) {
            defaultDt = 1f;
        }

        boolean looped = animation != null && animation.getLoopMode() == Animation.LoopMode.LOOP && frames.size() > 1;
        float animationLength = animation != null ? animation.getLength() : 0f;

        Animation.KeyFrame prevFrame = beforeIndex > 0 ? frames.get(beforeIndex - 1) : null;
        if (prevFrame != null) {
            copyVec3Safe(getFrameValue(prevFrame), outP0);
            outTimes[0] = prevFrame.timestamp;
        } else if (looped) {
            Animation.KeyFrame loopPrev = frames.get(frames.size() - 1);
            copyVec3Safe(getFrameValue(loopPrev), outP0);
            float loopTime = loopPrev.timestamp;
            if (animationLength > EPSILON) {
                while (loopTime >= t1) {
                    loopTime -= animationLength;
                }
            } else {
                loopTime = t1 - defaultDt;
            }
            outTimes[0] = loopTime;
        } else {
            extrapolateEndpoint(startValue, endValue, outP0);
            outTimes[0] = t1 - defaultDt;
        }

        Animation.KeyFrame nextFrame = (afterIndex + 1) < frames.size() ? frames.get(afterIndex + 1) : null;
        if (nextFrame != null) {
            copyVec3Safe(getFrameValue(nextFrame), outP3);
            outTimes[3] = nextFrame.timestamp;
        } else if (looped) {
            Animation.KeyFrame loopNext = frames.get(0);
            copyVec3Safe(getFrameValue(loopNext), outP3);
            float loopTime = loopNext.timestamp;
            if (animationLength > EPSILON) {
                while (loopTime <= t2) {
                    loopTime += animationLength;
                }
            } else {
                loopTime = t2 + defaultDt;
            }
            outTimes[3] = loopTime;
        } else {
            extrapolateEndpoint(endValue, startValue, outP3);
            outTimes[3] = t2 + defaultDt;
        }
    }

    private void copyVec3Safe(float[] src, float[] dst) {
        if (dst == null) {
            return;
        }
        dst[0] = (src != null && src.length > 0) ? src[0] : 0f;
        dst[1] = (src != null && src.length > 1) ? src[1] : 0f;
        dst[2] = (src != null && src.length > 2) ? src[2] : 0f;
    }

    private void extrapolateEndpoint(float[] anchor, float[] reference, float[] out) {
        if (out == null) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            float anchorVal = (anchor != null && anchor.length > i) ? anchor[i] : 0f;
            float refVal = (reference != null && reference.length > i) ? reference[i] : anchorVal;
            out[i] = anchorVal + (anchorVal - refVal);
        }
    }

    private void catmullInterpolate(float[] p0, float[] p1, float[] p2, float[] p3,
                                    float[] times, float t, float[] out) {
        float t0 = times[0];
        float t1 = times[1];
        float t2 = times[2];
        float t3 = times[3];
        float dt01 = Math.max(EPSILON, t1 - t0);
        float dt12 = Math.max(EPSILON, t2 - t1);
        float dt23 = Math.max(EPSILON, t3 - t2);

        float t2Factor = dt12;
        float tt = Math.max(0f, Math.min(1f, t));
        float tt2 = tt * tt;
        float tt3 = tt2 * tt;
        float h00 = 2f * tt3 - 3f * tt2 + 1f;
        float h10 = tt3 - 2f * tt2 + tt;
        float h01 = -2f * tt3 + 3f * tt2;
        float h11 = tt3 - tt2;

        for (int i = 0; i < 3; i++) {
            float m1 = computeCatmullTangent(p0[i], p1[i], p2[i], dt01, dt12);
            float m2 = computeCatmullTangent(p1[i], p2[i], p3[i], dt12, dt23);
            out[i] = h00 * p1[i] + h10 * t2Factor * m1 + h01 * p2[i] + h11 * t2Factor * m2;
        }
    }

    private float computeCatmullTangent(float prev, float current, float next, float dtPrev, float dtNext) {
        float slopePrev = dtPrev > EPSILON ? (current - prev) / dtPrev : 0f;
        float slopeNext = dtNext > EPSILON ? (next - current) / dtNext : 0f;
        if (dtPrev <= EPSILON) {
            return slopeNext;
        }
        if (dtNext <= EPSILON) {
            return slopePrev;
        }
        float denom = dtPrev + dtNext;
        return denom > EPSILON ? (slopePrev * dtNext + slopeNext * dtPrev) / denom : 0f;
    }

    private float[] getFrameValue(Animation.KeyFrame frame) {
        if (frame == null || frame.value == null) {
            return ZERO_VECTOR;
        }
        return frame.value;
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
        if (requiresEulerInterpolation(original, animated)) {
            for (int i = 0; i < 3; i++) {
                original[i] = original[i] + (animated[i] - original[i]) * weight;
            }
            return;
        }
        eulerToQuaternion(original, tmpQuatA);
        eulerToQuaternion(animated, tmpQuatB);
        slerpQuaternion(tmpQuatA, tmpQuatB, weight, tmpQuatOut);
        quaternionToEuler(tmpQuatOut, tmpEuler);
        setVec(original, tmpEuler[0], tmpEuler[1], tmpEuler[2]);
    }

    private boolean requiresEulerInterpolation(float[] start, float[] end) {
        for (int i = 0; i < 3; i++) {
            if (Math.abs(end[i] - start[i]) > 180f + 1e-3f) {
                return true;
            }
        }
        return false;
    }

    private float[] getQuaternion(float[] euler) {
        if (euler == null || euler.length < 3) {
            return IDENTITY_QUATERNION;
        }
        return quaternionCache.computeIfAbsent(euler, key -> {
            float[] q = new float[4];
            eulerToQuaternion(key, q);
            return q;
        });
    }

    private void quaternionBezier(float[] q0, float[] q1, float[] q2, float[] q3, float t, float[] out) {
        slerpQuaternion(q0, q1, t, tmpQuatA);
        slerpQuaternion(q1, q2, t, tmpQuatB);
        slerpQuaternion(q2, q3, t, tmpQuatC);

        slerpQuaternion(tmpQuatA, tmpQuatB, t, tmpQuatD);
        slerpQuaternion(tmpQuatB, tmpQuatC, t, tmpQuatE);

        slerpQuaternion(tmpQuatD, tmpQuatE, t, out);
    }

    private void squad(float[] q0, float[] q1, float[] q2, float[] q3, float t, float[] out) {
        computeSquadControl(q0, q1, q2, tmpQuatC);
        computeSquadControl(q1, q2, q3, tmpQuatD);

        slerpQuaternion(q1, q2, t, tmpQuatA);
        slerpQuaternion(tmpQuatC, tmpQuatD, t, tmpQuatB);

        float slerpT = 2f * t * (1f - t);
        slerpQuaternion(tmpQuatA, tmpQuatB, slerpT, out);
    }

    private void computeSquadControl(float[] qm1, float[] q, float[] qp1, float[] out) {
        conjugateQuaternion(q, tmpQuatE);

        multiplyQuaternion(tmpQuatE, qm1, tmpQuatC);
        normalizeQuaternion(tmpQuatC);
        quaternionLog(tmpQuatC, tmpQuatC);

        multiplyQuaternion(tmpQuatE, qp1, tmpQuatD);
        normalizeQuaternion(tmpQuatD);
        quaternionLog(tmpQuatD, tmpQuatD);

        tmpQuatE[0] = -0.25f * (tmpQuatC[0] + tmpQuatD[0]);
        tmpQuatE[1] = -0.25f * (tmpQuatC[1] + tmpQuatD[1]);
        tmpQuatE[2] = -0.25f * (tmpQuatC[2] + tmpQuatD[2]);
        tmpQuatE[3] = 0f;

        quaternionExp(tmpQuatE, tmpQuatE);
        multiplyQuaternion(q, tmpQuatE, out);
        normalizeQuaternion(out);
    }

    private void multiplyQuaternion(float[] a, float[] b, float[] out) {
        float ax = a[0], ay = a[1], az = a[2], aw = a[3];
        float bx = b[0], by = b[1], bz = b[2], bw = b[3];
        out[0] = aw * bx + ax * bw + ay * bz - az * by;
        out[1] = aw * by - ax * bz + ay * bw + az * bx;
        out[2] = aw * bz + ax * by - ay * bx + az * bw;
        out[3] = aw * bw - ax * bx - ay * by - az * bz;
    }

    private void conjugateQuaternion(float[] q, float[] out) {
        out[0] = -q[0];
        out[1] = -q[1];
        out[2] = -q[2];
        out[3] = q[3];
    }

    private void quaternionLog(float[] q, float[] out) {
        float x = q[0];
        float y = q[1];
        float z = q[2];
        float w = q[3];
        float vecLength = (float) Math.sqrt(x * x + y * y + z * z);
        float angle = (float) Math.atan2(vecLength, w);
        if (vecLength > 1e-6f) {
            float coeff = angle / vecLength;
            out[0] = x * coeff;
            out[1] = y * coeff;
            out[2] = z * coeff;
        } else {
            out[0] = out[1] = out[2] = 0f;
        }
        out[3] = 0f;
    }

    private void quaternionExp(float[] q, float[] out) {
        float x = q[0];
        float y = q[1];
        float z = q[2];
        float theta = (float) Math.sqrt(x * x + y * y + z * z);
        float sinTheta = (float) Math.sin(theta);
        float cosTheta = (float) Math.cos(theta);
        float coeff = theta > 1e-6f ? sinTheta / theta : 1f;
        out[0] = x * coeff;
        out[1] = y * coeff;
        out[2] = z * coeff;
        out[3] = cosTheta;
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
