package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.core.data.DataMaterial;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;
import org.mybad.minecraft.gltf.core.data.OverlayRenderContext;
import org.mybad.minecraft.gltf.client.network.RemoteAnimationController;
import org.mybad.minecraft.gltf.client.network.RemoteAnimationState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumHandSide;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;

public class CustomPlayerInstance implements AttachmentManager.Host {
    private CustomPlayerConfig config;
    private GltfRenderModel renderModel;
    private final AttachmentManager attachmentManager = new AttachmentManager(this);
    private final OverlayRenderContext overlayRenderContext = new OverlayRenderContext();

    private float idleTime = 0.0f;
    private float walkTime = 0.0f;
    private float sprintTime = 0.0f;
    private float sneakTime = 0.0f;

    private float lastTickTime = -1f;

    private static final float DEFAULT_BLEND_DURATION_SECONDS = 0.2f;

    private String activeClip;
    private String previousClip;
    private boolean blending = false;
    private float blendTimer = 0.0f;
    private float blendDurationSeconds = DEFAULT_BLEND_DURATION_SECONDS;
    private float blendSourceTimeSeconds = 0.0f;
    private boolean holdLastFrame = false;

    private final MovementBlendTree movementBlendTree = new MovementBlendTree();
    private PoseSignature lastPoseSignature = PoseSignature.invalid();
    private ResourceLocation baseTexture;
    private final PulseOverrideManager pulseOverrideManager = new PulseOverrideManager(new PulseOverrideManager.Target() {
        @Override
        public boolean applyAlpha(PulseOverrideManager.PulseOverrideCommand command, long now) {
            return CustomPlayerInstance.this.applyPulseOverride(command, now);
        }

        @Override
        public boolean applyColor(PulseOverrideManager.ColorPulseOverrideCommand command, long now) {
            return CustomPlayerInstance.this.applyColorPulseOverride(command, now);
        }
    });

    public void bindConfiguration(CustomPlayerConfig config) {
        unbindModel();
        this.config = config;
        this.baseTexture = safeTexture(config != null ? config.texturePath : null);
        if (config != null && GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.debug("CustomPlayerInstance[{}] binding profile {} (modelPath={})",
                Integer.toHexString(System.identityHashCode(this)), config.name, config.modelPath);
        }
        resetAnimationState();
        loadModel();
        attachmentManager.reload(config);
        reapplyPendingPulseOverrides();
    }

    public void applyOverlayPulseOverride(String materialName, String overlayId,
                                          DataMaterial.OverlayLayer.PulseSettings pulse,
                                          long durationMs) {
        pulseOverrideManager.applyAlphaOverride(materialName, overlayId, pulse, durationMs, command -> {
            if (command == null || !GltfLog.LOGGER.isDebugEnabled()) {
                return;
            }
            GltfLog.LOGGER.debug("Pulse override queued (material={}, overlay={}) for player {}",
                command.getMaterialName(), command.getOverlayId(), config != null ? config.name : "<unbound>");
        });
    }

    public void applyOverlayColorPulseOverride(String materialName, String overlayId,
                                               DataMaterial.OverlayLayer.ColorPulseSettings pulse,
                                               long durationMs) {
        pulseOverrideManager.applyColorOverride(materialName, overlayId, pulse, durationMs, command -> {
            if (command == null || !GltfLog.LOGGER.isDebugEnabled()) {
                return;
            }
            GltfLog.LOGGER.debug("Color pulse override queued (material={}, overlay={}) for player {}",
                command.getMaterialName(), command.getOverlayId(), config != null ? config.name : "<unbound>");
        });
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
        if (config == null) {
            return;
        }

        try {
            // 每个 profile 独立的模型实例（材料不共享），使用 profile cache
            renderModel = CustomPlayerManager.getOrCreateProfileModel(config);
            if (renderModel != null) {
                renderModel.setGlobalScale(config.modelScale);
                renderModel.setDefaultTexture(baseTexture);
                applyMaterialOverrides(renderModel, config.materials);
                if (GltfLog.LOGGER.isDebugEnabled()) {
                    GltfLog.LOGGER.debug("CustomPlayerInstance[{}] bound model {} (instanceId={}, scale={})",
                        Integer.toHexString(System.identityHashCode(this)),
                        config.modelPath,
                        renderModel.getInstanceId(),
                        config.modelScale);
                }
            } else {
                GltfLog.LOGGER.warn("Failed to load model for player instance: " + config.modelPath);
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error binding model: " + config.modelPath, e);
        }
    }

    public void onAttachmentProfileAvailable(String profileId) {
        attachmentManager.onProfileAvailable(profileId);
    }

    @Nullable
    static ResourceLocation safeTexture(@Nullable String path) {
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

    public boolean render(EntityPlayer player, double x, double y, double z, float entityYaw, float partialTicks) {
        if (config == null) {
            return false;
        }
        purgeExpiredPulseOverrides();
        boolean hasModel = renderModel != null;
        boolean hasAttachments = attachmentManager.hasAttachments();
        if (!hasModel && !hasAttachments) {
            return false;
        }

        boolean matrixPushed = false;
        boolean shadeAdjusted = false;
        boolean renderedAnything = false;
        ResourceLocation baseTexture = this.baseTexture;
        try {
            if (baseTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
            }

            updateAnimation(player, partialTicks);

            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            shadeAdjusted = true;
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(player.prevRenderYawOffset +
                (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks, 0, -1, 0);

            applyOffset(config.renderOffset);

            if (renderModel != null) {
                prepareOverlayContextForPlayer(player, partialTicks);
                renderModel.renderAll();
                renderedAnything = true;
            }

            if (attachmentManager.render(Minecraft.getMinecraft())) {
                renderedAnything = true;
            }

            logGlError("player.render " + (config != null ? config.modelPath : "null"));

            return renderedAnything;

        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering player instance", e);
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

    public boolean renderFirstPerson(EntityPlayer player, EnumHandSide side, float partialTicks,
                                     CustomPlayerConfig.HandConfig handConfig) {
        if (config == null || renderModel == null || handConfig == null) {
            return false;
        }
        purgeExpiredPulseOverrides();
        boolean enabled = handConfig.isEnabled(true);
        if (!enabled) {
            return false;
        }

        boolean matrixPushed = false;
        boolean shadeAdjusted = false;
        boolean disabledCull = false;
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            if (baseTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
            }

            updateAnimation(player, partialTicks);

            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            shadeAdjusted = true;

            boolean shouldDisableCull = handConfig.shouldDisableCull(true);
            if (shouldDisableCull && cullWasEnabled) {
                GlStateManager.disableCull();
                disabledCull = true;
            }

            boolean mirror = handConfig.shouldMirror(side == EnumHandSide.LEFT);
            if (mirror) {
                GlStateManager.scale(-1.0f, 1.0f, 1.0f);
            }

            float posX = handConfig.getPosition(0, 0.0f);
            float posY = handConfig.getPosition(1, 0.0f);
            float posZ = handConfig.getPosition(2, 0.0f);
            GlStateManager.translate(posX, posY, posZ);

            float rotX = handConfig.getRotation(0, 0.0f);
            float rotY = handConfig.getRotation(1, 0.0f);
            float rotZ = handConfig.getRotation(2, 0.0f);
            if (rotZ != 0.0f) {
                GlStateManager.rotate(rotZ, 0.0f, 0.0f, 1.0f);
            }
            if (rotY != 0.0f) {
                GlStateManager.rotate(rotY, 0.0f, 1.0f, 0.0f);
            }
            if (rotX != 0.0f) {
                GlStateManager.rotate(rotX, 1.0f, 0.0f, 0.0f);
            }

            float scale = handConfig.getScale(1.0f);
            GlStateManager.scale(scale, scale, scale);

            applyOffset(config != null ? config.renderOffset : null);
            prepareOverlayContextForPlayer(player, partialTicks);

            Set<String> whitelist = handConfig.getVisibleNodes();
            Set<String> blacklist = handConfig.getHiddenNodes();
            if (!whitelist.isEmpty()) {
                renderModel.renderOnly(new HashSet<>(whitelist));
            } else if (!blacklist.isEmpty()) {
                renderModel.renderExcept(new HashSet<>(blacklist));
            } else {
                renderModel.renderAll();
            }

            if (handConfig.weapon != null && handConfig.weapon.isValid()) {
                renderWeapon(player, handConfig.weapon, mirror);
            }
            logGlError("player.renderFirstPerson " + (config != null ? config.modelPath : "null"));

            return true;
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering first-person player instance", e);
            return false;
        } finally {
            if (disabledCull && cullWasEnabled) {
                GlStateManager.enableCull();
            }
            if (shadeAdjusted) {
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
            if (matrixPushed) {
                GlStateManager.popMatrix();
            }
        }
    }

    @Override
    public void applyMaterialOverrides(@Nullable GltfRenderModel target,
                                       @Nullable Map<String, CustomPlayerConfig.MaterialOverride> overrides) {
        if (target == null || overrides == null || overrides.isEmpty()) {
            return;
        }
        try {
            if (target.geoModel != null && target.geoModel.materials != null) {
                target.geoModel.materials.values().forEach(material -> {
                    CustomPlayerConfig.MaterialOverride override = overrides.get(material.name);
                    if (override == null) {
                        override = overrides.get("default");
                    }
                    if (override == null && !overrides.isEmpty()) {
                        override = overrides.values().iterator().next();
                    }
                    if (override != null) {
                        override.applyTo(material);
                    }
                });
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error applying material overrides", e);
        }
    }

    @Override
    public void requestPulseReapply() {
        reapplyPendingPulseOverrides();
    }

    private void reapplyPendingPulseOverrides() {
        pulseOverrideManager.reapplyPending();
    }

    @Override
    public void invalidatePoseSignature() {
        lastPoseSignature = PoseSignature.invalid();
    }

    private boolean applyPulseOverride(@Nullable PulseOverrideManager.PulseOverrideCommand command, long now) {
        if (command == null || command.isExpired(now)) {
            return false;
        }
        boolean applied = false;
        if (renderModel != null) {
            applied |= command.apply(renderModel, now);
        }
        applied |= attachmentManager.applyPulseOverride(command, now);
        return applied;
    }

    private boolean applyColorPulseOverride(@Nullable PulseOverrideManager.ColorPulseOverrideCommand command, long now) {
        if (command == null || command.isExpired(now)) {
            return false;
        }
        boolean applied = false;
        if (renderModel != null) {
            applied |= command.apply(renderModel, now);
        }
        applied |= attachmentManager.applyColorPulseOverride(command, now);
        return applied;
    }

    private void purgeExpiredPulseOverrides() {
        pulseOverrideManager.purgeExpired();
    }

    private void applyPoseSignature(@Nullable PoseSignature signature) {
        if (signature == null) {
            return;
        }
        signature.applyTo(renderModel);
        attachmentManager.applyPose(signature);
        lastPoseSignature = signature;
    }

    private void updateAnimation(EntityPlayer player, float partialTicks) {
        if (config == null) {
            return;
        }
        if (renderModel == null && !attachmentManager.hasAttachments()) {
            return;
        }

        float deltaTime = sampleDeltaTimeFromTicks(player, partialTicks);
        attachmentManager.tickOverrides(deltaTime);

        RemoteAnimationState remoteState = RemoteAnimationController.getState(player.getUniqueID());
        if (remoteState != null) {
            if (applyRemoteAnimation(remoteState)) {
                return;
            }
            RemoteAnimationController.clear(player.getUniqueID());
        }

        float previousClipTime = sampleClipTime(activeClip);

        MovementState movementState = MovementState.from(player);
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
                applyPoseSignature(signature);
            }
            if (weight >= 0.999f) {
                finishBlend();
                float finalClipTime = sampleClipTime(activeClip);
                PoseSignature finalSignature = PoseSignature.single(activeClip, finalClipTime);
                if (!finalSignature.equals(lastPoseSignature)) {
                    applyPoseSignature(finalSignature);
                }
            }
        } else {
            PoseSignature signature = PoseSignature.single(activeClip, targetClipTime);
            if (!signature.equals(lastPoseSignature)) {
                applyPoseSignature(signature);
            }
        }
    }

    // Derive delta time from server tick progression (20 ticks per second baseline).
    private float sampleDeltaTimeFromTicks(EntityPlayer player, float partialTicks) {
        float tickTime = player.ticksExisted + partialTicks;

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
            CustomPlayerConfig.AnimationConfig activeSneakAnim = sneakAnim != null ? sneakAnim : idleAnim;
            sneakTime = advancePhase(sneakTime, activeSneakAnim, deltaTime);
        } else {
            sneakTime = 0.0f;
        }
    }

    private float sampleClipTime(String clipName) {
        if (config == null || clipName == null) {
            return 0.0f;
        }
        CustomPlayerConfig.AnimationConfig anim = config.getAnimation(clipName);
        return toAnimationSeconds(anim, getPhaseForClip(clipName));
    }

    private float getPhaseForClip(String clipName) {
        if (clipName == null) {
            return 0.0f;
        }
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

    private float getBlendDurationForClip(String clipName) {
        double defaultDuration = config != null ? config.blendDuration : DEFAULT_BLEND_DURATION_SECONDS;
        if (config != null) {
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

    private boolean applyRemoteAnimation(RemoteAnimationState state) {
        if (config == null) {
            return false;
        }
        if (renderModel == null && !attachmentManager.hasAttachments()) {
            return false;
        }
        CustomPlayerConfig.AnimationConfig anim = config.getAnimation(state.getClipId());
        if (anim == null) {
            return false;
        }
        boolean loop = anim.loop != null ? anim.loop : state.shouldLoop();
        boolean holdLast = anim.holdLastFrame != null ? anim.holdLastFrame : state.shouldHoldLastFrame();
        float phase = state.phaseAt(System.currentTimeMillis(), loop, holdLast);
        if (!loop && !holdLast && phase >= 0.9999f) {
            RemoteAnimationController.clear(state.getSubjectId());
            return false;
        }
        float targetClipTime = toAnimationSeconds(anim, phase);
        PoseSignature signature = PoseSignature.single(state.getClipId(), targetClipTime);
        applyPoseSignature(signature);
        activeClip = state.getClipId();
        previousClip = null;
        blending = false;
        blendTimer = 0.0f;
        blendDurationSeconds = state.getBlendDuration();
        blendSourceTimeSeconds = 0.0f;
        holdLastFrame = holdLast;
        if (!loop && holdLast) {
            // stop advancing further; phaseAt will clamp, and isExpired will be false
        }
        return true;
    }

    private void finishBlend() {
        blending = false;
        previousClip = null;
        blendTimer = 0.0f;
        blendSourceTimeSeconds = 0.0f;
        blendDurationSeconds = getBlendDurationForClip(activeClip);
        lastPoseSignature = PoseSignature.invalid();
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

        static MovementState from(EntityPlayer player) {
            float distanceDelta = player.distanceWalkedModified - player.prevDistanceWalkedModified;
            boolean moving = Math.abs(distanceDelta) > 0.01f;
            boolean sprinting = moving && player.isSprinting();
            boolean sneaking = player.isSneaking() && !player.isSprinting();
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

    private float advancePhase(float current, CustomPlayerConfig.AnimationConfig anim, float deltaTime) {
        if (anim == null || config == null) {
            return 0.0f;
        }

        boolean loop = anim.loop != null ? anim.loop : true;
        boolean hold = anim.holdLastFrame != null ? anim.holdLastFrame : false;
        current += (float)(anim.getAnimationSpeed(config.fps) * deltaTime);
        if (loop) {
            return wrapPhase(current);
        } else {
            if (current >= 1.0f) {
                return hold ? 1.0f : 1.0f; // non-loop: clamp to end
            }
            if (current < 0.0f) {
                return 0.0f;
            }
            return current;
        }
    }

    @Override
    public void applyOffset(@Nullable CustomPlayerConfig.OffsetConfig offset) {
        if (offset == null) {
            return;
        }
        float posX = offset.getPosition(0, 0.0f);
        float posY = offset.getPosition(1, 0.0f);
        float posZ = offset.getPosition(2, 0.0f);
        if (posX != 0.0f || posY != 0.0f || posZ != 0.0f) {
            GlStateManager.translate(posX, posY, posZ);
        }
        float rotX = offset.getRotation(0, 0.0f);
        float rotY = offset.getRotation(1, 0.0f);
        float rotZ = offset.getRotation(2, 0.0f);
        if (rotZ != 0.0f) {
            GlStateManager.rotate(rotZ, 0.0f, 0.0f, 1.0f);
        }
        if (rotY != 0.0f) {
            GlStateManager.rotate(rotY, 0.0f, 1.0f, 0.0f);
        }
        if (rotX != 0.0f) {
            GlStateManager.rotate(rotX, 1.0f, 0.0f, 0.0f);
        }
        float scale = offset.getScale(1.0f);
        if (scale != 1.0f) {
            GlStateManager.scale(scale, scale, scale);
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
        return (float)(frameValue / fps);
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

    private List<String> sortedCopy(java.util.Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> copy = new ArrayList<>(source);
        Collections.sort(copy);
        return copy;
    }

    @Override
    @Nullable
    public CustomPlayerConfig getConfig() {
        return config;
    }

    @Override
    @Nullable
    public GltfRenderModel getBaseRenderModel() {
        return renderModel;
    }

    public boolean hasValidModel() {
        return renderModel != null && renderModel.geoModel.loaded;
    }

    public void unbindModel() {
        if (renderModel != null) {
            if (GltfLog.LOGGER.isDebugEnabled()) {
                GltfLog.LOGGER.debug("CustomPlayerInstance[{}] releasing model instance {} (source={})",
                    Integer.toHexString(System.identityHashCode(this)),
                    renderModel.getInstanceId(),
                    renderModel.getDebugSourceId());
            }
            renderModel.cleanup();
            renderModel = null;
        }
        attachmentManager.reset();
        lastPoseSignature = PoseSignature.invalid();
        baseTexture = null;
    }

    private void renderWeapon(EntityPlayer player, CustomPlayerConfig.HandWeaponConfig weaponConfig, boolean parentMirror) {
        if (weaponConfig == null || !weaponConfig.isValid()) {
            return;
        }

        GltfRenderModel weaponModel = CustomPlayerManager.getOrLoadModel(weaponConfig.modelPath);
        if (weaponModel == null || weaponModel.geoModel == null || !weaponModel.geoModel.loaded) {
            return;
        }

        boolean matrixPushed = false;
        boolean shadeAdjusted = false;
        boolean disabledCull = false;
        boolean cullWasEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        ResourceLocation weaponTexture = safeTexture(weaponConfig.texturePath);
        ResourceLocation previousDefault = weaponModel.getDefaultTexture();
        try {
            if (weaponTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(weaponTexture);
            }
            weaponModel.setDefaultTexture(weaponTexture);

            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            shadeAdjusted = true;

            boolean shouldDisableCull = weaponConfig.shouldDisableCull(true);
            if (shouldDisableCull && cullWasEnabled) {
                GlStateManager.disableCull();
                disabledCull = true;
            }

            boolean weaponMirror = weaponConfig.shouldMirror(parentMirror);
            if (weaponMirror) {
                GlStateManager.scale(-1.0f, 1.0f, 1.0f);
            }

            float posX = weaponConfig.getPosition(0, 0.0f);
            float posY = weaponConfig.getPosition(1, 0.0f);
            float posZ = weaponConfig.getPosition(2, 0.0f);
            GlStateManager.translate(posX, posY, posZ);

            float rotX = weaponConfig.getRotation(0, 0.0f);
            float rotY = weaponConfig.getRotation(1, 0.0f);
            float rotZ = weaponConfig.getRotation(2, 0.0f);
            if (rotZ != 0.0f) {
                GlStateManager.rotate(rotZ, 0.0f, 0.0f, 1.0f);
            }
            if (rotY != 0.0f) {
                GlStateManager.rotate(rotY, 0.0f, 1.0f, 0.0f);
            }
            if (rotX != 0.0f) {
                GlStateManager.rotate(rotX, 1.0f, 0.0f, 0.0f);
            }

            float scale = weaponConfig.getScale(1.0f);
            GlStateManager.scale(scale, scale, scale);

            Set<String> whitelist = weaponConfig.getVisibleNodes();
            Set<String> blacklist = weaponConfig.getHiddenNodes();
            if (!whitelist.isEmpty()) {
                weaponModel.renderOnly(new HashSet<>(whitelist));
            } else if (!blacklist.isEmpty()) {
                weaponModel.renderExcept(new HashSet<>(blacklist));
            } else {
                weaponModel.renderAll();
            }
            logGlError("player.renderWeapon " + weaponConfig.modelPath);
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering first-person weapon", e);
        } finally {
            weaponModel.setDefaultTexture(previousDefault);
            if (disabledCull && cullWasEnabled) {
                GlStateManager.enableCull();
            }
            if (shadeAdjusted) {
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
            if (matrixPushed) {
                GlStateManager.popMatrix();
            }
        }
    }

    private void logGlError(String stage) {
        int error;
        boolean logged = false;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            logged = true;
            GltfLog.LOGGER.error("GL error 0x{} @ {} (playerInstance={}, model={}, instanceId={})",
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

    @Nullable
    public Vec3d sampleBoneWorldPosition(EntityPlayer player, String boneName, float partialTicks) {
        if (renderModel == null || boneName == null || boneName.isEmpty()) {
            return null;
        }
        
        double baseX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double baseY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double baseZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        return renderModel.sampleNodeWorldPosition(boneName, baseX, baseY, baseZ, yaw, 0.0f, 1.0f);
    }
    static final class PoseSignature {
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

        boolean isBlended() {
            return blended;
        }

        float getFromTime() {
            return fromTime;
        }

        float getToTime() {
            return toTime;
        }

        float getWeight() {
            return weight;
        }

        void applyTo(@Nullable GltfRenderModel model) {
            if (model == null) {
                return;
            }
            if (blended) {
                model.updateAnimationBlended(fromTime, toTime, weight, true);
            } else {
                model.updateAnimation(toTime, true);
            }
        }
    }

    private void prepareOverlayContextForPlayer(EntityPlayer player, float partialTicks) {
        if (renderModel == null) {
            return;
        }
        overlayRenderContext.reset();
        overlayRenderContext.setHoverModel(false);
        overlayRenderContext.setHoverBlock(false);
        overlayRenderContext.setHoveredNodes(Collections.emptySet());
        renderModel.setOverlayContext(overlayRenderContext);
    }
}
