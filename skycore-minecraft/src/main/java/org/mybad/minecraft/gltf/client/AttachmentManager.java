package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;
import org.mybad.minecraft.gltf.client.network.RemoteProfileRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

final class AttachmentManager {

    interface Host {
        @Nullable CustomPlayerConfig getConfig();
        @Nullable GltfRenderModel getBaseRenderModel();
        void applyMaterialOverrides(@Nullable GltfRenderModel target,
                                    @Nullable Map<String, CustomPlayerConfig.MaterialOverride> overrides);
        void requestPulseReapply();
        void invalidatePoseSignature();
        void applyOffset(@Nullable CustomPlayerConfig.OffsetConfig offset);
    }

    private final Host host;
    private final List<AttachmentInstance> attachments = new ArrayList<>();
    private final List<CustomPlayerConfig.AttachmentConfig> unresolved = new ArrayList<>();
    private final Set<String> missingRefs = new HashSet<>();

    AttachmentManager(Host host) {
        this.host = Objects.requireNonNull(host, "host");
    }

    void reload(@Nullable CustomPlayerConfig config) {
        reset();
        if (config == null || config.attachments == null || config.attachments.isEmpty()) {
            return;
        }
        for (CustomPlayerConfig.AttachmentConfig attachmentConfig : config.attachments) {
            if (attachmentConfig == null || !attachmentConfig.isValid()) {
                continue;
            }
            boolean loaded = loadAttachmentInstance(attachmentConfig);
            if (!loaded) {
                unresolved.add(attachmentConfig);
                if (attachmentConfig.profileRef != null) {
                    missingRefs.add(attachmentConfig.profileRef);
                }
            }
        }
        if (!attachments.isEmpty()) {
            host.invalidatePoseSignature();
        }
    }

    void reset() {
        cleanup();
        unresolved.clear();
        missingRefs.clear();
    }

    void cleanup() {
        if (attachments.isEmpty()) {
            return;
        }
        attachments.forEach(AttachmentInstance::cleanup);
        attachments.clear();
    }

    boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    boolean render(Minecraft mc) {
        if (attachments.isEmpty()) {
            return false;
        }
        boolean rendered = false;
        for (AttachmentInstance attachment : attachments) {
            rendered |= attachment.render(mc, host);
        }
        return rendered;
    }

    void applyPose(@Nullable CustomPlayerInstance.PoseSignature signature) {
        if (attachments.isEmpty() || signature == null) {
            return;
        }
        for (AttachmentInstance attachment : attachments) {
            attachment.applyPose(signature, host);
        }
    }

    void tickOverrides(float deltaSeconds) {
        if (attachments.isEmpty()) {
            return;
        }
        float clamped = Math.max(deltaSeconds, 0.0f);
        for (AttachmentInstance attachment : attachments) {
            attachment.tickOverride(clamped);
        }
    }

    boolean applyPulseOverride(@Nullable PulseOverrideManager.PulseOverrideCommand command, long now) {
        if (command == null || attachments.isEmpty()) {
            return false;
        }
        boolean applied = false;
        for (AttachmentInstance attachment : attachments) {
            applied |= attachment.applyPulseOverride(command, now);
        }
        return applied;
    }

    boolean applyColorPulseOverride(@Nullable PulseOverrideManager.ColorPulseOverrideCommand command, long now) {
        if (command == null || attachments.isEmpty()) {
            return false;
        }
        boolean applied = false;
        for (AttachmentInstance attachment : attachments) {
            applied |= attachment.applyColorPulseOverride(command, now);
        }
        return applied;
    }

    void onProfileAvailable(String profileId) {
        if (profileId == null || !missingRefs.contains(profileId)) {
            return;
        }
        boolean loaded = false;
        Iterator<CustomPlayerConfig.AttachmentConfig> iterator = unresolved.iterator();
        while (iterator.hasNext()) {
            CustomPlayerConfig.AttachmentConfig pending = iterator.next();
            if (pending != null && profileId.equals(pending.profileRef)) {
                if (loadAttachmentInstance(pending)) {
                    iterator.remove();
                    loaded = true;
                }
            }
        }
        rebuildMissingAttachmentRefs();
        if (loaded) {
            host.invalidatePoseSignature();
        }
    }

    private void rebuildMissingAttachmentRefs() {
        missingRefs.clear();
        for (CustomPlayerConfig.AttachmentConfig pending : unresolved) {
            if (pending != null && pending.profileRef != null) {
                missingRefs.add(pending.profileRef);
            }
        }
    }

    private boolean loadAttachmentInstance(CustomPlayerConfig.AttachmentConfig attachmentConfig) {
        CustomPlayerConfig referenceProfile = RemoteProfileRegistry.getProfile(attachmentConfig.profileRef);
        if (referenceProfile == null || referenceProfile.modelPath == null || referenceProfile.modelPath.isEmpty()) {
            return false;
        }
        try {
            GltfRenderModel model = CustomPlayerManager.loadModelFresh(referenceProfile.modelPath);
            if (model == null) {
                return false;
            }
            model.setGlobalScale(referenceProfile.modelScale);
            host.applyMaterialOverrides(model, referenceProfile.materials);
            int fallbackFps = 24;
            CustomPlayerConfig ownerConfig = host.getConfig();
            if (ownerConfig != null && ownerConfig.fps > 0) {
                fallbackFps = ownerConfig.fps;
            }
            attachments.add(new AttachmentInstance(referenceProfile, attachmentConfig, model, fallbackFps));
            host.requestPulseReapply();
            return true;
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error loading attachment {}",
                attachmentConfig.profileRef != null ? attachmentConfig.profileRef : "<unknown>", e);
            return false;
        }
    }

    private static final class AttachmentInstance {
        private final CustomPlayerConfig referenceConfig;
        private final CustomPlayerConfig.AttachmentConfig attachmentConfig;
        private final GltfRenderModel renderModel;
        private final ResourceLocation texture;
        private final String animationOverride;
        private final CustomPlayerConfig.AnimationConfig overrideClip;
        private final int overrideFps;
        private float overridePhase = 0.0f;

        private AttachmentInstance(CustomPlayerConfig referenceConfig,
                                   CustomPlayerConfig.AttachmentConfig attachmentConfig,
                                   GltfRenderModel renderModel,
                                   int fallbackFps) {
            this.referenceConfig = referenceConfig;
            this.attachmentConfig = attachmentConfig;
            this.renderModel = renderModel;
            this.texture = CustomPlayerInstance.safeTexture(
                referenceConfig != null ? referenceConfig.texturePath : null);
            if (this.renderModel != null) {
                this.renderModel.setDefaultTexture(this.texture);
            }
            this.animationOverride = resolveAnimationOverride(attachmentConfig);
            this.overrideClip = resolveOverrideClip(referenceConfig, this.animationOverride);
            if (referenceConfig != null && referenceConfig.fps > 0) {
                this.overrideFps = referenceConfig.fps;
            } else {
                this.overrideFps = Math.max(fallbackFps, 1);
            }
            if (hasOverrideAnimation()) {
                tickOverride(0.0f);
            }
        }

        private String resolveAnimationOverride(@Nullable CustomPlayerConfig.AttachmentConfig config) {
            if (config == null || config.animationOverride == null) {
                return null;
            }
            String trimmed = config.animationOverride.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        private CustomPlayerConfig.AnimationConfig resolveOverrideClip(@Nullable CustomPlayerConfig reference,
                                                                       @Nullable String overrideName) {
            if (reference == null || overrideName == null) {
                return null;
            }
            CustomPlayerConfig.AnimationConfig clip = reference.getAnimation(overrideName);
            if (clip == null) {
                String refName = reference.name != null ? reference.name : "<unknown>";
                String attachmentName = attachmentConfig != null ? attachmentConfig.profileRef : "<null>";
                GltfLog.LOGGER.warn(
                    "Attachment {} requested animation '{}' but profile {} has no matching clip",
                    attachmentName, overrideName, refName);
            }
            return clip;
        }

        private boolean hasOverrideAnimation() {
            return overrideClip != null;
        }

        boolean render(Minecraft mc, Host host) {
            if (renderModel == null) {
                return false;
            }
            boolean rendered = false;
            GlStateManager.pushMatrix();
            try {
                if (referenceConfig != null) {
                    host.applyOffset(referenceConfig.renderOffset);
                }
                if (attachmentConfig != null) {
                    host.applyOffset(attachmentConfig.renderOffset);
                }
                if (texture != null) {
                    mc.getTextureManager().bindTexture(texture);
                }
                renderModel.renderAll();
                rendered = true;
            } catch (Exception ex) {
                String refName = attachmentConfig != null ? attachmentConfig.profileRef : "<null>";
                GltfLog.LOGGER.error("Error rendering attachment {}", refName, ex);
            } finally {
                GlStateManager.popMatrix();
            }
            return rendered;
        }

        void applyPose(CustomPlayerInstance.PoseSignature signature, Host host) {
            if (renderModel == null || signature == null) {
                return;
            }
            if (hasOverrideAnimation()) {
                return;
            }
            boolean appliedSignature = false;
            if (hasAnimationTracks(renderModel)) {
                signature.applyTo(renderModel);
                appliedSignature = true;
            }
            if (!appliedSignature) {
                GltfRenderModel baseModel = host.getBaseRenderModel();
                if (baseModel != null) {
                    renderModel.loadAnimation(baseModel, true);
                }
            }
        }

        private boolean hasAnimationTracks(@Nullable GltfRenderModel model) {
            if (model == null || model.geoModel == null || model.geoModel.animations == null) {
                return false;
            }
            return !model.geoModel.animations.isEmpty();
        }

        boolean applyPulseOverride(@Nullable PulseOverrideManager.PulseOverrideCommand command, long now) {
            if (renderModel == null || command == null) {
                return false;
            }
            return command.apply(renderModel, now);
        }

        boolean applyColorPulseOverride(@Nullable PulseOverrideManager.ColorPulseOverrideCommand command, long now) {
            if (renderModel == null || command == null) {
                return false;
            }
            return command.apply(renderModel, now);
        }

        void tickOverride(float deltaSeconds) {
            if (!hasOverrideAnimation() || renderModel == null || overrideClip == null) {
                return;
            }
            overridePhase = advanceOverridePhase(overridePhase, deltaSeconds);
            float animationSeconds = toOverrideAnimationSeconds(overrideClip, overridePhase);
            renderModel.updateAnimation(animationSeconds, true);
        }

        private float advanceOverridePhase(float current, float deltaSeconds) {
            if (overrideClip == null) {
                return current;
            }
            double fps = Math.max(overrideFps, 1);
            double phaseDelta = overrideClip.getAnimationSpeed(fps) * deltaSeconds;
            current += (float) phaseDelta;
            boolean loop = overrideClip.loop != null ? overrideClip.loop : true;
            boolean hold = overrideClip.holdLastFrame != null ? overrideClip.holdLastFrame : false;
            if (loop) {
                return wrapPhase(current);
            }
            if (current >= 1.0f) {
                return hold ? 1.0f : 1.0f;
            }
            if (current < 0.0f) {
                return 0.0f;
            }
            return current;
        }

        private float toOverrideAnimationSeconds(@Nullable CustomPlayerConfig.AnimationConfig anim, float phase) {
            if (anim == null) {
                return 0.0f;
            }
            int fps = Math.max(overrideFps, 1);
            double startFrame = anim.getStartFrame(fps);
            double endFrame = anim.getEndFrame(fps);
            double frameSpan = endFrame - startFrame;
            double frameValue = startFrame;
            if (frameSpan > 0.0) {
                frameValue += phase * frameSpan;
            }
            return (float) (frameValue / Math.max(fps, 1));
        }

        void cleanup() {
            if (renderModel != null) {
                try {
                    renderModel.cleanup();
                } catch (Exception ignored) {
                }
            }
        }

        private float wrapPhase(float value) {
            value = value % 1.0f;
            if (value < 0.0f) {
                value += 1.0f;
            }
            return value;
        }
    }
}
