package org.mybad.minecraft.render;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.minecraft.animation.EntityAnimationController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles animation playback, overlays, and cross-fading.
 */
final class AnimationBridge {
    private AnimationPlayer animationPlayer;
    private AnimationPlayer activePlayer;
    private AnimationPlayer previousPlayer;
    private float primaryFadeTime;
    private float primaryFadeDuration = 0.12f;
    private final Map<Animation, AnimationPlayer> overlayPlayers = new HashMap<>();
    private List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
    private long lastUpdateTime = System.currentTimeMillis();
    private PoseSnapshot lastPoseSnapshot;
    private PoseSnapshot transitionSnapshot;
    private float transitionTime;
    private float transitionDuration = 0.12f;
    private boolean inAutoTransition = false;
    private final float[] quatScratchA = new float[4];
    private final float[] quatScratchB = new float[4];
    private final float[] quatScratchOut = new float[4];
    private final float[] eulerScratch = new float[3];

    AnimationBridge(Animation animation) {
        if (animation != null) {
            animationPlayer = new AnimationPlayer(animation);
            animationPlayer.play();
            activePlayer = animationPlayer;
        }
    }

    void setPrimaryFadeDuration(float seconds) {
        if (Float.isNaN(seconds) || seconds < 0f) {
            return;
        }
        this.primaryFadeDuration = seconds;
        this.transitionDuration = seconds;
    }

    void setAnimation(Animation animation) {
        overlayPlayers.clear();
        overlayStates = Collections.emptyList();
        if (animation != null) {
            if (animationPlayer == null || animationPlayer.getAnimation() != animation) {
                animationPlayer = new AnimationPlayer(animation);
                animationPlayer.play();
            }
        } else {
            animationPlayer = null;
        }
        beginPrimaryTransition(animationPlayer);
    }

    void restartAnimation() {
        if (animationPlayer != null) {
            animationPlayer.restart();
        }
    }

    AnimationPlayer getAnimationPlayer() {
        return animationPlayer;
    }

    AnimationPlayer getActiveAnimationPlayer() {
        return activePlayer != null ? activePlayer : animationPlayer;
    }

    void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        if (states == null || states.isEmpty()) {
            overlayStates = Collections.emptyList();
            return;
        }
        overlayStates = new ArrayList<>(states);
    }

    void clearOverlayStates() {
        overlayStates = Collections.emptyList();
    }

    void dispose() {
        overlayPlayers.clear();
        overlayStates = Collections.emptyList();
        activePlayer = null;
        previousPlayer = null;
    }

    boolean updateAndApply(Model model) {
        update();
        return apply(model);
    }

    boolean update() {
        updateAnimation();
        return hasActiveAnimation();
    }

    boolean apply(Model model) {
        if (model == null) {
            return false;
        }
        if (!hasActiveAnimation()) {
            model.resetToBindPose();
            return false;
        }
        model.resetToBindPose();
        AnimationPlayer player = activePlayer;
        if (player != null) {
            player.apply(model);
        }
        if (inAutoTransition && transitionSnapshot != null) {
            float blend = transitionDuration > 0f ? Math.min(transitionTime / transitionDuration, 1f) : 1f;
            transitionSnapshot.blendToCurrent(blend, quatScratchA, quatScratchB, quatScratchOut, eulerScratch);
            if (blend >= 1f - 1.0e-4f) {
                inAutoTransition = false;
                transitionSnapshot = null;
            }
        } else {
            float previousWeight = getPrimaryFadeWeight();
            if (previousPlayer != null && previousWeight > 0f) {
                previousPlayer.apply(model, previousWeight);
            }
        }
        applyOverlays(model);
        storePose(model);
        return true;
    }

    private void updateAnimation() {
        if (animationPlayer == null && activePlayer == null && previousPlayer == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0F;
        lastUpdateTime = currentTime;

        if (deltaTime > 0.1F) {
            deltaTime = 0.1F;
        }

        AnimationPlayer desiredPlayer = null;
        if (animationPlayer != null) {
            animationPlayer.update(deltaTime);
            desiredPlayer = animationPlayer;
        }

        if (desiredPlayer != activePlayer) {
            beginPrimaryTransition(desiredPlayer);
        }

        if (previousPlayer != null) {
            previousPlayer.update(deltaTime);
            primaryFadeTime += deltaTime;
            if (primaryFadeTime >= primaryFadeDuration) {
                previousPlayer = null;
            }
        }

        if (inAutoTransition) {
            transitionTime += deltaTime;
            if (transitionDuration <= 0f || transitionTime >= transitionDuration) {
                inAutoTransition = false;
                transitionSnapshot = null;
            }
        }
    }

    private void applyOverlays(Model model) {
        if (overlayStates.isEmpty()) {
            return;
        }
        for (EntityAnimationController.OverlayState state : overlayStates) {
            if (state == null || state.weight <= 0f || state.animation == null) {
                continue;
            }
            AnimationPlayer player = overlayPlayers.get(state.animation);
            if (player == null) {
                player = new AnimationPlayer(state.animation);
                player.play();
                overlayPlayers.put(state.animation, player);
            }
            player.setCurrentTime(state.time);
            player.apply(model, state.weight);
        }
    }

    private void beginPrimaryTransition(AnimationPlayer next) {
        if (next == activePlayer) {
            return;
        }
        if (lastPoseSnapshot != null) {
            transitionSnapshot = new PoseSnapshot(lastPoseSnapshot);
            inAutoTransition = true;
            transitionTime = 0f;
            previousPlayer = null;
        } else if (primaryFadeDuration > 0f) {
            previousPlayer = activePlayer;
            primaryFadeTime = 0f;
            inAutoTransition = false;
            transitionSnapshot = null;
        } else {
            previousPlayer = null;
            primaryFadeTime = primaryFadeDuration;
            inAutoTransition = false;
            transitionSnapshot = null;
        }
        activePlayer = next;
    }

    private float getPrimaryFadeWeight() {
        if (previousPlayer == null || primaryFadeDuration <= 0f) {
            return 0f;
        }
        float t = primaryFadeTime / primaryFadeDuration;
        if (t >= 1f) {
            return 0f;
        }
        return 1f - t;
    }

    private boolean hasActiveAnimation() {
        return activePlayer != null || previousPlayer != null || !overlayStates.isEmpty();
    }

    private void storePose(Model model) {
        if (model == null) {
            return;
        }
        if (lastPoseSnapshot == null) {
            lastPoseSnapshot = new PoseSnapshot(model.getAllBones());
        } else {
            lastPoseSnapshot.capture();
        }
    }

    private static final class PoseSnapshot {
        private final ModelBone[] bones;
        private final float[][] positions;
        private final float[][] rotations;
        private final float[][] scales;

        PoseSnapshot(List<ModelBone> sourceBones) {
            this.bones = sourceBones.toArray(new ModelBone[0]);
            this.positions = new float[bones.length][3];
            this.rotations = new float[bones.length][3];
            this.scales = new float[bones.length][3];
            capture();
        }

        PoseSnapshot(PoseSnapshot other) {
            this.bones = other.bones;
            this.positions = deepCopy(other.positions);
            this.rotations = deepCopy(other.rotations);
            this.scales = deepCopy(other.scales);
        }

        void capture() {
            for (int i = 0; i < bones.length; i++) {
                ModelBone bone = bones[i];
                System.arraycopy(bone.getPosition(), 0, positions[i], 0, 3);
                System.arraycopy(bone.getRotation(), 0, rotations[i], 0, 3);
                System.arraycopy(bone.getSize(), 0, scales[i], 0, 3);
            }
        }

        void blendToCurrent(float t,
                            float[] quatA,
                            float[] quatB,
                            float[] quatOut,
                            float[] eulerScratch) {
            if (bones.length == 0) {
                return;
            }
            if (t <= 0f) {
                for (int i = 0; i < bones.length; i++) {
                    ModelBone bone = bones[i];
                    System.arraycopy(positions[i], 0, bone.getPosition(), 0, 3);
                    System.arraycopy(rotations[i], 0, bone.getRotation(), 0, 3);
                    System.arraycopy(scales[i], 0, bone.getSize(), 0, 3);
                }
                return;
            }
            if (t >= 1f) {
                return;
            }
            for (int i = 0; i < bones.length; i++) {
                ModelBone bone = bones[i];
                float[] pos = bone.getPosition();
                float[] startPos = positions[i];
                pos[0] = startPos[0] + (pos[0] - startPos[0]) * t;
                pos[1] = startPos[1] + (pos[1] - startPos[1]) * t;
                pos[2] = startPos[2] + (pos[2] - startPos[2]) * t;

                float[] targetRot = bone.getRotation();
                blendEuler(rotations[i], targetRot, t, quatA, quatB, quatOut, eulerScratch);
                targetRot[0] = eulerScratch[0];
                targetRot[1] = eulerScratch[1];
                targetRot[2] = eulerScratch[2];

                float[] size = bone.getSize();
                float[] startScale = scales[i];
                size[0] = startScale[0] + (size[0] - startScale[0]) * t;
                size[1] = startScale[1] + (size[1] - startScale[1]) * t;
                size[2] = startScale[2] + (size[2] - startScale[2]) * t;
            }
        }

        private static float[][] deepCopy(float[][] source) {
            float[][] result = new float[source.length][3];
            for (int i = 0; i < source.length; i++) {
                System.arraycopy(source[i], 0, result[i], 0, 3);
            }
            return result;
        }

        private static void blendEuler(float[] start,
                                       float[] end,
                                       float t,
                                       float[] quatA,
                                       float[] quatB,
                                       float[] quatOut,
                                       float[] eulerOut) {
            eulerToQuaternion(start, quatA);
            eulerToQuaternion(end, quatB);
            slerpQuaternion(quatA, quatB, t, quatOut);
            quaternionToEuler(quatOut, eulerOut);
        }

        private static void eulerToQuaternion(float[] euler, float[] out) {
            double yaw = Math.toRadians(euler[1]);
            double pitch = Math.toRadians(euler[0]);
            double roll = Math.toRadians(euler[2]);
            double cy = Math.cos(yaw * 0.5);
            double sy = Math.sin(yaw * 0.5);
            double cp = Math.cos(pitch * 0.5);
            double sp = Math.sin(pitch * 0.5);
            double cr = Math.cos(roll * 0.5);
            double sr = Math.sin(roll * 0.5);
            out[0] = (float) (sr * cp * cy - cr * sp * sy);
            out[1] = (float) (cr * sp * cy + sr * cp * sy);
            out[2] = (float) (cr * cp * sy - sr * sp * cy);
            out[3] = (float) (cr * cp * cy + sr * sp * sy);
            normalizeQuaternion(out);
        }

        private static void quaternionToEuler(float[] q, float[] out) {
            normalizeQuaternion(q);
            double sinrCosp = 2.0 * (q[3] * q[0] + q[1] * q[2]);
            double cosrCosp = 1.0 - 2.0 * (q[0] * q[0] + q[1] * q[1]);
            out[0] = (float) Math.toDegrees(Math.atan2(sinrCosp, cosrCosp));
            double sinp = 2.0 * (q[3] * q[1] - q[2] * q[0]);
            if (Math.abs(sinp) >= 1) {
                out[1] = (float) Math.toDegrees(Math.copySign(Math.PI / 2.0, sinp));
            } else {
                out[1] = (float) Math.toDegrees(Math.asin(sinp));
            }
            double sinyCosp = 2.0 * (q[3] * q[2] + q[0] * q[1]);
            double cosyCosp = 1.0 - 2.0 * (q[1] * q[1] + q[2] * q[2]);
            out[2] = (float) Math.toDegrees(Math.atan2(sinyCosp, cosyCosp));
        }

        private static void slerpQuaternion(float[] qa, float[] qb, float t, float[] out) {
            double cosHalfTheta = qa[0] * qb[0] + qa[1] * qb[1] + qa[2] * qb[2] + qa[3] * qb[3];
            if (cosHalfTheta < 0.0) {
                qb[0] = -qb[0];
                qb[1] = -qb[1];
                qb[2] = -qb[2];
                qb[3] = -qb[3];
                cosHalfTheta = -cosHalfTheta;
            }
            if (Math.abs(cosHalfTheta) >= 1.0) {
                out[0] = qa[0];
                out[1] = qa[1];
                out[2] = qa[2];
                out[3] = qa[3];
                return;
            }
            double halfTheta = Math.acos(cosHalfTheta);
            double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta * cosHalfTheta);
            if (Math.abs(sinHalfTheta) < 0.001) {
                out[0] = (qa[0] * 0.5f + qb[0] * 0.5f);
                out[1] = (qa[1] * 0.5f + qb[1] * 0.5f);
                out[2] = (qa[2] * 0.5f + qb[2] * 0.5f);
                out[3] = (qa[3] * 0.5f + qb[3] * 0.5f);
                return;
            }
            double ratioA = Math.sin((1 - t) * halfTheta) / sinHalfTheta;
            double ratioB = Math.sin(t * halfTheta) / sinHalfTheta;
            out[0] = (float) (qa[0] * ratioA + qb[0] * ratioB);
            out[1] = (float) (qa[1] * ratioA + qb[1] * ratioB);
            out[2] = (float) (qa[2] * ratioA + qb[2] * ratioB);
            out[3] = (float) (qa[3] * ratioA + qb[3] * ratioB);
        }

        private static void normalizeQuaternion(float[] q) {
            double len = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
            if (len == 0.0) {
                q[0] = q[1] = q[2] = 0f;
                q[3] = 1f;
                return;
            }
            float inv = (float) (1.0 / len);
            q[0] *= inv;
            q[1] *= inv;
            q[2] *= inv;
            q[3] *= inv;
        }
    }
}
