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
import org.lwjgl.opengl.GL11;

/**
 * Renderer instance for non-player GLTF models (bosses, NPCs, decorations).
 */
public class CustomEntityInstance {

    private static final float DEFAULT_BLEND_DURATION_SECONDS = 0.2f;

    private GltfProfile config;
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
    private ResourceLocation baseTexture;

    public boolean isBoundTo(GltfProfile candidate) {
        return this.config == candidate && renderModel != null;
    }

    @Nullable
    public GltfProfile getProfile() {
        return config;
    }

    public void bindConfiguration(GltfProfile config) {
        if (config == null) {
            unbindModel();
            return;
        }
        if (isBoundTo(config)) {
            return;
        }
        unbindModel();
        this.config = config;
        this.baseTexture = safeTexture(config.getTexturePath());
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
    }

    private void loadModel() {
        if (config == null || config.getModelPath() == null || config.getModelPath().isEmpty()) {
            return;
        }
        try {
            // 使用非缓存加载，避免不同配置互相污染材质
            renderModel = CustomPlayerManager.loadModelFresh(config.getModelPath());
            if (renderModel != null) {
                renderModel.setGlobalScale(config.getModelScale());
                renderModel.setDefaultTexture(baseTexture);
                GltfLog.LOGGER.debug("Bound model to entity instance: {}", config.getModelPath());
            } else {
                GltfLog.LOGGER.warn("Failed to load model for entity instance: {}", config.getModelPath());
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error binding model for entity instance: {}", config.getModelPath(), e);
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
            renderModel.updateAnimationBlended(blendSourceTimeSeconds, targetClipTime, weight, true);
            if (weight >= 0.999f) {
                finishBlend();
                float finalClipTime = sampleClipTime(activeClip);
                renderModel.updateAnimation(finalClipTime, true);
            }
        } else {
            renderModel.updateAnimation(targetClipTime, true);
        }
    }

    private boolean applyRemoteAnimation(RemoteAnimationState state) {
        if (config == null || renderModel == null) {
            return false;
        }
        GltfProfile.AnimationClip anim = config.getAnimation(state.getClipId());
        if (anim == null) {
            return false;
        }
        boolean loop = anim.resolveLoop(state.shouldLoop());
        boolean hold = anim.resolveHoldLastFrame(state.shouldHoldLastFrame());
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
        GltfProfile.AnimationClip anim = config.getAnimation(clipName);
        if (anim == null) {
            return 0.0f;
        }
        float phase = getAnimationPhase(clipName);
        return toAnimationSeconds(anim, phase);
    }

    private float getBlendDurationForClip(@Nullable String clipName) {
        double defaultDuration = config != null ? config.getBlendDuration() : DEFAULT_BLEND_DURATION_SECONDS;
        if (config != null && clipName != null) {
            GltfProfile.AnimationClip anim = config.getAnimation(clipName);
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

        GltfProfile.AnimationClip idleAnim = config.getAnimation("idle");
        if (!state.moving && !state.sneaking) {
            idleTime = advancePhase(idleTime, idleAnim, deltaTime);
        } else {
            idleTime = 0.0f;
        }

        GltfProfile.AnimationClip walkAnim = config.getAnimation("walk");
        if (state.walking) {
            walkTime = advancePhase(walkTime, walkAnim, deltaTime);
        } else {
            walkTime = 0.0f;
        }

        GltfProfile.AnimationClip sprintAnim = config.getAnimation("sprint");
        if (state.sprinting) {
            sprintTime = advancePhase(sprintTime, sprintAnim, deltaTime);
            walkTime = 0.0f;
        } else {
            sprintTime = 0.0f;
        }

        GltfProfile.AnimationClip sneakAnim = config.getAnimation("sneak");
        if (state.sneaking) {
            sneakTime = advancePhase(sneakTime, sneakAnim, deltaTime);
        } else {
            sneakTime = 0.0f;
        }
    }

    private float advancePhase(float current, @Nullable GltfProfile.AnimationClip anim, float deltaTime) {
        if (anim == null || config == null) {
            return 0.0f;
        }
        boolean loop = anim.isLoop();
        boolean hold = anim.shouldHoldLastFrame();
        current += (float) (anim.getAnimationSpeed(config.getFps()) * deltaTime);
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

    private float toAnimationSeconds(GltfProfile.AnimationClip anim, float phase) {
        if (anim == null || config == null || config.getFps() <= 0) {
            return 0.0f;
        }
        double startFrame = anim.getStartFrame();
        double endFrame = anim.getEndFrame();
        double frameSpan = endFrame - startFrame;

        double frameValue = startFrame;
        if (frameSpan > 0.0) {
            frameValue += phase * frameSpan;
        }
        double fps = Math.max(config.getFps(), 1);
        return (float) (frameValue / fps);
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

    public List<String> getMaterialNames() {
        if (renderModel == null || renderModel.geoModel == null || renderModel.geoModel.materials == null) {
            return Collections.emptyList();
        }
        return sortedCopy(renderModel.geoModel.materials.keySet());
    }

    public List<String> getAnimationNames() {
        if (config == null || config.getAnimations().isEmpty()) {
            return Collections.emptyList();
        }
        return sortedCopy(config.getAnimations().keySet());
    }

    public List<String> getBoneNames() {
        if (renderModel == null || renderModel.geoModel == null || renderModel.geoModel.nodes == null) {
            return Collections.emptyList();
        }
        return sortedCopy(renderModel.geoModel.nodes.keySet());
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
