package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.client.network.RemoteAnimationController;
import org.mybad.minecraft.gltf.client.network.RemoteAnimationState;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Renderer instance for non-player GLTF models (bosses, NPCs, decorations).
 */
public class CustomEntityInstance {

    private static final float DEFAULT_BLEND_DURATION_SECONDS = 0.2f;

    private CustomPlayerConfig config;
    private GltfRenderModel renderModel;

    private float idleTime;
    private float walkTime;
    private float sprintTime;
    private float sneakTime;

    private float lastTickTime = -1f;

    private String activeClip;
    private String previousClip;
    private boolean blending;
    private float blendTimer;
    private float blendDurationSeconds = DEFAULT_BLEND_DURATION_SECONDS;
    private float blendSourceTimeSeconds;

    private final MovementBlendTree movementBlendTree = new MovementBlendTree();
    private PoseSignature lastPoseSignature = PoseSignature.invalid();
    private ResourceLocation baseTexture;

    public boolean isBoundTo(CustomPlayerConfig candidate) {
        return this.config == candidate && renderModel != null;
    }

    @Nullable
    public CustomPlayerConfig getConfig() {
        return config;
    }

    public void bindConfiguration(CustomPlayerConfig config) {
        if (config == null) {
            unbindModel();
            return;
        }
        if (isBoundTo(config)) {
            return;
        }
        unbindModel();
        this.config = config;
        this.baseTexture = safeTexture(config.texturePath);
        resetAnimationState();
        loadModel();
    }

    public void unbindModel() {
        if (renderModel != null) {
            renderModel.cleanup();
            renderModel = null;
        }
        config = null;
        baseTexture = null;
        lastPoseSignature = PoseSignature.invalid();
    }

    private void resetAnimationState() {
        idleTime = 0.0f;
        walkTime = 0.0f;
        sprintTime = 0.0f;
        sneakTime = 0.0f;
        lastTickTime = -1f;
        activeClip = null;
        previousClip = null;
        blending = false;
        blendTimer = 0.0f;
        blendDurationSeconds = DEFAULT_BLEND_DURATION_SECONDS;
        blendSourceTimeSeconds = 0.0f;
        lastPoseSignature = PoseSignature.invalid();
    }

    private void loadModel() {
        if (config == null || config.modelPath == null || config.modelPath.isEmpty()) {
            return;
        }
        try {
            // 使用非缓存加载，避免不同配置互相污染材质
            renderModel = CustomPlayerManager.loadModelFresh(config.modelPath);
            if (renderModel != null) {
                renderModel.setGlobalScale(config.modelScale);
                renderModel.setDefaultTexture(baseTexture);
                applyMaterialOverrides();
                GltfLog.LOGGER.debug("Bound model to entity instance: {}", config.modelPath);
            } else {
                GltfLog.LOGGER.warn("Failed to load model for entity instance: {}", config.modelPath);
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error binding model for entity instance: {}", config.modelPath, e);
        }
    }

    public boolean render(EntityLivingBase entity, double x, double y, double z,
                          float entityYaw, float partialTicks) {
        if (config == null || renderModel == null) {
            return false;
        }

        boolean matrixPushed = false;
        boolean shadeAdjusted = false;
        try {
            if (baseTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
            }

            updateAnimation(entity, partialTicks);

            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            shadeAdjusted = true;
            GlStateManager.translate(x, y, z);

            float yaw = entity.prevRenderYawOffset +
                (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
            GlStateManager.rotate(yaw, 0.0f, -1.0f, 0.0f);

            renderModel.renderAll();
            logGlError("entity.render " + (config != null ? config.modelPath : "null"));
            return true;
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering custom entity instance", e);
            return false;
        } finally {
            if (shadeAdjusted) {
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
            if (matrixPushed) {
                GlStateManager.popMatrix();
            }
        }
    }

    @Nullable
    private ResourceLocation safeTexture(@Nullable String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(path);
        } catch (Exception ex) {
            GltfLog.LOGGER.warn("Invalid texture path {}", path, ex);
            return null;
        }
    }

    private void updateAnimation(EntityLivingBase entity, float partialTicks) {
        if (config == null || renderModel == null) {
            return;
        }

        RemoteAnimationState remoteState = RemoteAnimationController.getState(entity.getUniqueID());
        if (remoteState != null) {
            if (applyRemoteAnimation(remoteState)) {
                return;
            }
            RemoteAnimationController.clear(entity.getUniqueID());
        }

        float deltaTime = sampleDeltaTimeFromTicks(entity, partialTicks);
        float previousClipTime = sampleClipTime(activeClip);

        MovementState movementState = MovementState.from(entity);
        String desiredClip = movementBlendTree.evaluate(movementState);

        updateAnimationStates(movementState, deltaTime);

        if (activeClip == null) {
            activeClip = desiredClip;
            blendDurationSeconds = getBlendDurationForClip(activeClip);
        }

        if (!desiredClip.equals(activeClip)) {
            beginBlend(desiredClip, previousClipTime);
        }

        float targetClipTime = sampleClipTime(activeClip);

        if (blending && previousClip != null) {
            blendTimer += deltaTime;
            float weight = computeBlendWeight();
            PoseSignature signature = PoseSignature.blended(previousClip, blendSourceTimeSeconds,
                activeClip, targetClipTime, weight);
            if (!signature.equals(lastPoseSignature)) {
                renderModel.updateAnimationBlended(blendSourceTimeSeconds, targetClipTime, weight, true);
                lastPoseSignature = signature;
            }
            if (weight >= 0.999f) {
                finishBlend();
                float finalClipTime = sampleClipTime(activeClip);
                PoseSignature finalSignature = PoseSignature.single(activeClip, finalClipTime);
                if (!finalSignature.equals(lastPoseSignature)) {
                    renderModel.updateAnimation(finalClipTime, true);
                }
                lastPoseSignature = finalSignature;
            }
        } else {
            PoseSignature signature = PoseSignature.single(activeClip, targetClipTime);
            if (!signature.equals(lastPoseSignature)) {
                renderModel.updateAnimation(targetClipTime, true);
                lastPoseSignature = signature;
            }
        }
    }

    private boolean applyRemoteAnimation(RemoteAnimationState state) {
        if (config == null || renderModel == null) {
            return false;
        }
        CustomPlayerConfig.AnimationConfig anim = config.getAnimation(state.getClipId());
        if (anim == null) {
            return false;
        }
        boolean loop = anim.loop != null ? anim.loop : state.shouldLoop();
        boolean hold = anim.holdLastFrame != null ? anim.holdLastFrame : state.shouldHoldLastFrame();
        float phase = state.phaseAt(System.currentTimeMillis(), loop, hold);
        if (!loop && !hold && phase >= 0.9999f) {
            RemoteAnimationController.clear(state.getSubjectId());
            return false;
        }
        float targetClipTime = toAnimationSeconds(anim, phase);
        renderModel.updateAnimation(targetClipTime, true);
        activeClip = state.getClipId();
        previousClip = null;
        blending = false;
        blendTimer = 0.0f;
        blendDurationSeconds = state.getBlendDuration();
        blendSourceTimeSeconds = targetClipTime;
        lastPoseSignature = PoseSignature.single(activeClip, targetClipTime);
        return true;
    }

    private void beginBlend(String nextClip, float sourceTimeSeconds) {
        previousClip = activeClip;
        activeClip = nextClip;
        blendSourceTimeSeconds = sourceTimeSeconds;
        blendDurationSeconds = getBlendDurationForClip(nextClip);
        blendTimer = 0.0f;
        blending = blendDurationSeconds > 0.0f && previousClip != null;
        if (!blending) {
            previousClip = null;
        }
    }

    private void finishBlend() {
        blending = false;
        previousClip = null;
        blendTimer = 0.0f;
    }

    private float sampleClipTime(@Nullable String clipName) {
        if (clipName == null || config == null) {
            return 0.0f;
        }
        CustomPlayerConfig.AnimationConfig anim = config.getAnimation(clipName);
        if (anim == null) {
            return 0.0f;
        }
        float phase = getAnimationPhase(clipName);
        return toAnimationSeconds(anim, phase);
    }

    private float getBlendDurationForClip(@Nullable String clipName) {
        double defaultDuration = config != null ? config.blendDuration : DEFAULT_BLEND_DURATION_SECONDS;
        if (config != null && clipName != null) {
            CustomPlayerConfig.AnimationConfig anim = config.getAnimation(clipName);
            if (anim != null) {
                return anim.getBlendDurationSeconds(defaultDuration);
            }
        }
        return (float) Math.max(0.0, defaultDuration);
    }

    private float computeBlendWeight() {
        float duration = Math.max(blendDurationSeconds, 1.0e-4f);
        float weight = blendTimer / duration;
        return weight > 1.0f ? 1.0f : weight;
    }

    private float getAnimationPhase(String clipName) {
        switch (clipName) {
            case "walk":
                return walkTime;
            case "sprint":
                return sprintTime;
            case "sneak":
                return sneakTime;
            case "idle":
            default:
                return idleTime;
        }
    }

    private float sampleDeltaTimeFromTicks(EntityLivingBase entity, float partialTicks) {
        float tickTime = entity.ticksExisted + partialTicks;

        if (tickTime < 0.0f) {
            tickTime = 0.0f;
        }
        if (lastTickTime < 0.0f || tickTime < lastTickTime) {
            lastTickTime = tickTime;
            return 0.0f;
        }
        float deltaTicks = tickTime - lastTickTime;
        lastTickTime = tickTime;
        return deltaTicks / 20.0f;
    }

    private void updateAnimationStates(MovementState state, float deltaTime) {
        if (config == null) {
            return;
        }

        CustomPlayerConfig.AnimationConfig idleAnim = config.getAnimation("idle");
        if (!state.moving && !state.sneaking) {
            idleTime = advancePhase(idleTime, idleAnim, deltaTime);
        } else {
            idleTime = 0.0f;
        }

        CustomPlayerConfig.AnimationConfig walkAnim = config.getAnimation("walk");
        if (state.walking) {
            walkTime = advancePhase(walkTime, walkAnim, deltaTime);
        } else {
            walkTime = 0.0f;
        }

        CustomPlayerConfig.AnimationConfig sprintAnim = config.getAnimation("sprint");
        if (state.sprinting) {
            sprintTime = advancePhase(sprintTime, sprintAnim, deltaTime);
            walkTime = 0.0f;
        } else {
            sprintTime = 0.0f;
        }

        CustomPlayerConfig.AnimationConfig sneakAnim = config.getAnimation("sneak");
        if (state.sneaking) {
            sneakTime = advancePhase(sneakTime, sneakAnim, deltaTime);
        } else {
            sneakTime = 0.0f;
        }
    }

    private float advancePhase(float current, @Nullable CustomPlayerConfig.AnimationConfig anim, float deltaTime) {
        if (anim == null || config == null) {
            return 0.0f;
        }
        boolean loop = anim.loop != null ? anim.loop : true;
        boolean hold = anim.holdLastFrame != null ? anim.holdLastFrame : false;
        current += (float) (anim.getAnimationSpeed(config.fps) * deltaTime);
        if (loop) {
            return wrapPhase(current);
        } else {
            if (current >= 1.0f) {
                return hold ? 1.0f : 1.0f;
            }
            if (current < 0.0f) {
                return 0.0f;
            }
            return current;
        }
    }

    private float wrapPhase(float value) {
        value = value % 1.0f;
        if (value < 0.0f) {
            value += 1.0f;
        }
        return value;
    }

    private float toAnimationSeconds(CustomPlayerConfig.AnimationConfig anim, float phase) {
        if (anim == null || config == null || config.fps <= 0) {
            return 0.0f;
        }
        double startFrame = anim.getStartFrame(config.fps);
        double endFrame = anim.getEndFrame(config.fps);
        double frameSpan = endFrame - startFrame;

        double frameValue = startFrame;
        if (frameSpan > 0.0) {
            frameValue += phase * frameSpan;
        }
        double fps = Math.max(config.fps, 1);
        return (float) (frameValue / fps);
    }

    private void applyMaterialOverrides() {
        if (config == null || renderModel == null || config.materials.isEmpty()) {
            return;
        }
        try {
            if (renderModel.geoModel != null && renderModel.geoModel.materials != null) {
                renderModel.geoModel.materials.values().forEach(material -> {
                    CustomPlayerConfig.MaterialOverride override = config.getMaterialOverride(material.name);
                    if (override != null) {
                        override.applyTo(material);
                    }
                });
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error applying entity material overrides", e);
        }
    }

    @Nullable
    public Vec3d sampleBoneWorldPosition(EntityLivingBase entity, String boneName, float partialTicks) {
        if (renderModel == null || boneName == null || boneName.isEmpty()) {
            return null;
        }
        if (renderModel.nodeStates == null || renderModel.nodeStates.isEmpty()) {
            return null;
        }
        GltfRenderModel.NodeState state = renderModel.nodeStates.get(boneName);
        if (state == null || state.mat == null) {
            return null;
        }

        Matrix4f matrix = new Matrix4f(state.mat);
        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);
        translation.mul(renderModel.getGlobalScale());

        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;

        float yaw = entity.prevRenderYawOffset +
            (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
        double yawRad = Math.toRadians(-yaw);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        double rotatedX = translation.x * cos - translation.z * sin;
        double rotatedZ = translation.x * sin + translation.z * cos;

        return new Vec3d(baseX + rotatedX, baseY + translation.y, baseZ + rotatedZ);
    }

    private static final class MovementState {
        final boolean moving;
        final boolean sneaking;
        final boolean sprinting;
        final boolean walking;

        private MovementState(boolean moving, boolean sneaking, boolean sprinting, boolean walking) {
            this.moving = moving;
            this.sneaking = sneaking;
            this.sprinting = sprinting;
            this.walking = walking;
        }

        static MovementState from(EntityLivingBase entity) {
            float distanceDelta = entity.distanceWalkedModified - entity.prevDistanceWalkedModified;
            boolean moving = Math.abs(distanceDelta) > 0.01f;
            boolean sprinting = moving && entity.isSprinting();
            boolean sneaking = entity.isSneaking() && !entity.isSprinting();
            boolean walking = moving && !sprinting && !sneaking;
            return new MovementState(moving, sneaking, sprinting, walking);
        }
    }

    private static final class MovementBlendTree {
        String evaluate(MovementState state) {
            if (state.sprinting) {
                return "sprint";
            }
            if (state.walking) {
                return "walk";
            }
            if (state.sneaking) {
                return "sneak";
            }
            return "idle";
        }
    }

    private static final class PoseSignature {
        private static final float EPSILON = 1.0e-4f;

        private final String fromClip;
        private final float fromTime;
        private final String toClip;
        private final float toTime;
        private final float weight;
        private final boolean blended;

        private PoseSignature(String fromClip, float fromTime, String toClip, float toTime, float weight,
                              boolean blended) {
            this.fromClip = fromClip;
            this.fromTime = fromTime;
            this.toClip = toClip;
            this.toTime = toTime;
            this.weight = weight;
            this.blended = blended;
        }

        static PoseSignature single(String clip, float time) {
            return new PoseSignature(null, 0.0f, clip, time, 1.0f, false);
        }

        static PoseSignature blended(String fromClip, float fromTime, String toClip, float toTime, float weight) {
            return new PoseSignature(fromClip, fromTime, toClip, toTime, weight, true);
        }

        static PoseSignature invalid() {
            return new PoseSignature(null, Float.NaN, null, Float.NaN, Float.NaN, false);
        }

        boolean equals(PoseSignature other) {
            if (other == null) {
                return false;
            }
            if (this.blended != other.blended) {
                return false;
            }
            if (this.blended) {
                return equalsClip(this.fromClip, other.fromClip)
                    && equalsClip(this.toClip, other.toClip)
                    && approximatelyEqual(this.fromTime, other.fromTime)
                    && approximatelyEqual(this.toTime, other.toTime)
                    && approximatelyEqual(this.weight, other.weight);
            }
            return equalsClip(this.toClip, other.toClip)
                && approximatelyEqual(this.toTime, other.toTime);
        }

        private boolean equalsClip(String a, String b) {
            if (a == null) {
                return b == null;
            }
            return a.equals(b);
        }

        private boolean approximatelyEqual(float a, float b) {
            if (Float.isNaN(a) || Float.isNaN(b)) {
                return false;
            }
            return Math.abs(a - b) <= EPSILON;
        }
    }

    public List<String> getMaterialNames() {
        if (renderModel == null || renderModel.geoModel == null || renderModel.geoModel.materials == null) {
            return Collections.emptyList();
        }
        return sortedCopy(renderModel.geoModel.materials.keySet());
    }

    public List<String> getAnimationNames() {
        if (config == null || config.animations == null || config.animations.isEmpty()) {
            return Collections.emptyList();
        }
        return sortedCopy(config.animations.keySet());
    }

    public List<String> getBoneNames() {
        if (renderModel == null || renderModel.geoModel == null || renderModel.geoModel.nodes == null) {
            return Collections.emptyList();
        }
        return sortedCopy(renderModel.geoModel.nodes.keySet());
    }

    private void logGlError(String stage) {
        int error;
        boolean logged = false;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            logged = true;
            GltfLog.LOGGER.error("GL error 0x{} @ {} (entityInstance={}, model={}, instanceId={})",
                Integer.toHexString(error),
                stage,
                Integer.toHexString(System.identityHashCode(this)),
                renderModel != null ? renderModel.getDebugSourceId() : "null",
                renderModel != null ? renderModel.getInstanceId() : -1);
        }
        if (!logged && GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.debug("GL OK @ {} (model={})", stage,
                renderModel != null ? renderModel.getDebugSourceId() : "null");
        }
    }

    private List<String> sortedCopy(Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> copy = new ArrayList<>(source);
        Collections.sort(copy);
        return copy;
    }
}
