package org.mybad.minecraft.gltf.client.decoration;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.client.CustomPlayerConfig;
import org.mybad.minecraft.gltf.client.CustomPlayerManager;
import org.mybad.minecraft.gltf.core.data.DataMaterial;
import org.mybad.minecraft.gltf.core.data.GltfRenderModel;
import org.mybad.minecraft.gltf.core.data.OverlayRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Lightweight renderer for static decoration anchors (e.g. skulls) that display a GLTF profile.
 */
final class DecorationInstance {

    private CustomPlayerConfig config;
    private GltfRenderModel renderModel;
    private ResourceLocation baseTexture;
    private final java.util.List<PulseOverrideCommand> pendingPulseOverrides = new java.util.ArrayList<>();
    private final java.util.List<ColorPulseOverrideCommand> pendingColorPulseOverrides = new java.util.ArrayList<>();
    private final OverlayRenderContext overlayRenderContext = new OverlayRenderContext();

    private String activeClip;
    private float clipPhase;
    private long lastSampleTimeNanos = -1L;

    boolean isBoundTo(CustomPlayerConfig candidate) {
        return config == candidate && renderModel != null;
    }

    void bindConfiguration(CustomPlayerConfig config) {
        if (config == null) {
            unbind();
            return;
        }
        if (isBoundTo(config)) {
            return;
        }
        unbind();
        this.config = config;
        this.baseTexture = safeTexture(config.texturePath);
        this.activeClip = null;
        this.clipPhase = 0.0f;
        this.lastSampleTimeNanos = -1L;
        loadModel();
        reapplyPendingPulseOverrides();
    }

    void unbind() {
        if (renderModel != null) {
            renderModel.cleanup();
            renderModel = null;
        }
        config = null;
        baseTexture = null;
        activeClip = null;
        clipPhase = 0.0f;
        lastSampleTimeNanos = -1L;
    }

    private void loadModel() {
        if (config == null) {
            return;
        }
        try {
            // 使用非缓存加载，避免不同装饰配置互相污染材质
            renderModel = CustomPlayerManager.loadModelFresh(config.modelPath);
            if (renderModel != null) {
                renderModel.setGlobalScale(config.modelScale);
                renderModel.setDefaultTexture(baseTexture);
                applyMaterialOverrides();
                reapplyPendingPulseOverrides();
            } else {
                if (CustomPlayerManager.shouldLogMissingModel(config.modelPath)) {
                    GltfLog.LOGGER.warn("Failed to load decoration model: {}", config.modelPath);
                }
            }
        } catch (Exception e) {
            if (CustomPlayerManager.shouldLogMissingModel(config.modelPath)) {
                GltfLog.LOGGER.error("Error loading decoration model {}", config.modelPath, e);
            }
        }
    }

    private void applyMaterialOverrides() {
        if (renderModel == null || config == null || config.materials.isEmpty()) {
            return;
        }
        try {
            if (renderModel.geoModel != null && renderModel.geoModel.materials != null) {
                renderModel.geoModel.materials.values().forEach(material -> {
                    CustomPlayerConfig.MaterialOverride override = config.getMaterialOverride(material.name);
                    if (override == null) {
                        override = config.getDefaultOverride();
                    }
                    if (override == null) {
                        override = config.getFirstOverride();
                    }
                    if (override != null) {
                        override.applyTo(material);
                    }
                });
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error applying decoration material overrides", e);
        }
    }

    boolean render(BlockPos blockPos, double worldX, double worldY, double worldZ,
                   double relX, double relY, double relZ,
                   float yawDegrees, float pitchDegrees,
                   float scaleMultiplier, float partialTicks,
                   @Nullable String requestedClip) {
        if (config == null || renderModel == null) {
            return false;
        }
        purgeExpiredPulseOverrides();
        updateAnimation(requestedClip);

        boolean matrixPushed = false;
        boolean shadeAdjusted = false;
        try {
            if (baseTexture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(baseTexture);
            }

            GlStateManager.pushMatrix();
            matrixPushed = true;
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            shadeAdjusted = true;

            GlStateManager.translate(relX, relY, relZ);
            if (yawDegrees != 0.0f) {
                GlStateManager.rotate(yawDegrees, 0.0f, -1.0f, 0.0f);
            }
            if (pitchDegrees != 0.0f) {
                GlStateManager.rotate(pitchDegrees, 1.0f, 0.0f, 0.0f);
            }
            if (scaleMultiplier != 1.0f) {
                GlStateManager.scale(scaleMultiplier, scaleMultiplier, scaleMultiplier);
            }

            applyRenderOffset();
            prepareOverlayContextForDecoration(blockPos, worldX, worldY, worldZ, yawDegrees, pitchDegrees, scaleMultiplier, partialTicks);

            renderModel.renderAll();
            logGlError("decoration.render " + (config != null ? config.name : "null"));
            return true;
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error rendering decoration instance", e);
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

    private void prepareOverlayContextForDecoration(BlockPos blockPos, double worldX, double worldY, double worldZ,
                                                    float yawDegrees, float pitchDegrees, float scaleMultiplier,
                                                    float partialTicks) {
        if (renderModel == null) {
            return;
        }
        overlayRenderContext.reset();
        overlayRenderContext.setHoverBlock(false);
        overlayRenderContext.setHoverModel(false);
        overlayRenderContext.setHoveredNodes(Collections.emptySet());
        renderModel.setOverlayContext(overlayRenderContext);
    }

    @Nullable
    private Vec3d sampleNodeWorldPosition(String nodeName, double worldX, double worldY, double worldZ,
                                          float yawDegrees, float pitchDegrees, float scaleMultiplier) {
        if (renderModel == null) {
            return null;
        }
        return renderModel.sampleNodeWorldPosition(nodeName, worldX, worldY, worldZ, yawDegrees, pitchDegrees, scaleMultiplier);
    }

    private void updateAnimation(@Nullable String requestedClip) {
        if (config == null || renderModel == null) {
            return;
        }
        String desiredClip = resolveClip(requestedClip);
        if (desiredClip == null) {
            return;
        }
        if (!desiredClip.equals(activeClip)) {
            activeClip = desiredClip;
            clipPhase = 0.0f;
        }
        CustomPlayerConfig.AnimationConfig anim = config.getAnimation(activeClip);
        if (anim == null) {
            return;
        }
        float delta = sampleDeltaSeconds();
        clipPhase = advancePhase(clipPhase, anim, delta);
        float targetSeconds = toAnimationSeconds(anim, clipPhase);
        renderModel.updateAnimation(targetSeconds, true);
    }

    @Nullable
    private String resolveClip(@Nullable String requestedClip) {
        if (config == null) {
            return null;
        }
        if (requestedClip != null && config.hasAnimation(requestedClip)) {
            return requestedClip;
        }
        if (config.hasAnimation("idle")) {
            return "idle";
        }
        if (activeClip != null && config.hasAnimation(activeClip)) {
            return activeClip;
        }
        return config.animations.keySet().stream().findFirst().orElse(null);
    }

    private float sampleDeltaSeconds() {
        long now = System.nanoTime();
        if (lastSampleTimeNanos < 0L) {
            lastSampleTimeNanos = now;
            return 0.0f;
        }
        long delta = now - lastSampleTimeNanos;
        lastSampleTimeNanos = now;
        return delta / 1_000_000_000.0f;
    }

    private void applyRenderOffset() {
        if (config == null || config.renderOffset == null) {
            return;
        }
        CustomPlayerConfig.OffsetConfig offset = config.renderOffset;
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

    private float advancePhase(float current, CustomPlayerConfig.AnimationConfig anim, float deltaTime) {
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

    void applyOverlayPulseOverride(String materialName, String overlayId,
                                   DataMaterial.OverlayLayer.PulseSettings pulse, long durationMs) {
        PulseOverrideCommand command = PulseOverrideCommand.create(materialName, overlayId, pulse, durationMs);
        if (command == null) {
            return;
        }
        purgeExpiredPulseOverrides();
        pendingPulseOverrides.removeIf(existing -> existing.matches(command));
        pendingPulseOverrides.add(command);
        long now = System.currentTimeMillis();
        boolean applied = applyPulseOverride(command, now);
        if (command.isExpired(now)) {
            pendingPulseOverrides.remove(command);
        } else if (!applied && GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.debug("Decoration pulse queued (material={}, overlay={})",
                command.getMaterialName(), command.getOverlayId());
        }
    }

    void applyOverlayColorPulseOverride(String materialName, String overlayId,
                                        DataMaterial.OverlayLayer.ColorPulseSettings pulse, long durationMs) {
        ColorPulseOverrideCommand command = ColorPulseOverrideCommand.create(materialName, overlayId, pulse, durationMs);
        if (command == null) {
            return;
        }
        purgeExpiredPulseOverrides();
        pendingColorPulseOverrides.removeIf(existing -> existing.matches(command));
        pendingColorPulseOverrides.add(command);
        long now = System.currentTimeMillis();
        boolean applied = applyColorPulseOverride(command, now);
        if (command.isExpired(now)) {
            pendingColorPulseOverrides.remove(command);
        } else if (!applied && GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.debug("Decoration color pulse queued (material={}, overlay={})",
                command.getMaterialName(), command.getOverlayId());
        }
    }

    private void reapplyPendingPulseOverrides() {
        reapplyPendingAlphaPulseOverrides();
        reapplyPendingColorPulseOverrides();
    }

    private void reapplyPendingAlphaPulseOverrides() {
        if (pendingPulseOverrides.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        java.util.Iterator<PulseOverrideCommand> iterator = pendingPulseOverrides.iterator();
        while (iterator.hasNext()) {
            PulseOverrideCommand command = iterator.next();
            if (command == null || command.isExpired(now)) {
                iterator.remove();
                continue;
            }
            applyPulseOverride(command, now);
        }
    }

    private void reapplyPendingColorPulseOverrides() {
        if (pendingColorPulseOverrides.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        java.util.Iterator<ColorPulseOverrideCommand> iterator = pendingColorPulseOverrides.iterator();
        while (iterator.hasNext()) {
            ColorPulseOverrideCommand command = iterator.next();
            if (command == null || command.isExpired(now)) {
                iterator.remove();
                continue;
            }
            applyColorPulseOverride(command, now);
        }
    }

    private void purgeExpiredPulseOverrides() {
        long now = System.currentTimeMillis();
        if (!pendingPulseOverrides.isEmpty()) {
            pendingPulseOverrides.removeIf(command -> command == null || command.isExpired(now));
        }
        if (!pendingColorPulseOverrides.isEmpty()) {
            pendingColorPulseOverrides.removeIf(command -> command == null || command.isExpired(now));
        }
    }

    private boolean applyPulseOverride(@Nullable PulseOverrideCommand command, long now) {
        if (renderModel == null || command == null || command.isExpired(now)) {
            return false;
        }
        return command.apply(renderModel, now);
    }

    private boolean applyColorPulseOverride(@Nullable ColorPulseOverrideCommand command, long now) {
        if (renderModel == null || command == null || command.isExpired(now)) {
            return false;
        }
        return command.apply(renderModel, now);
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

    private void logGlError(String stage) {
        int error;
        boolean logged = false;
        while ((error = GL11.glGetError()) != GL11.GL_NO_ERROR) {
            logged = true;
            GltfLog.LOGGER.error("GL error 0x{} @ {} (decoration={}, modelInstance={})",
                Integer.toHexString(error),
                stage,
                config != null ? config.name : "null",
                renderModel != null ? renderModel.getInstanceId() : -1);
        }
        if (!logged && GltfLog.LOGGER.isDebugEnabled()) {
            GltfLog.LOGGER.debug("GL OK @ {} (decoration={})", stage,
                config != null ? config.name : "null");
        }
    }

    private static final class PulseOverrideCommand {
        private final String materialName;
        private final String overlayId;
        private final DataMaterial.OverlayLayer.PulseSettings pulse;
        private final long expireAtMs;

        private PulseOverrideCommand(String materialName, String overlayId,
                                     DataMaterial.OverlayLayer.PulseSettings pulse, long expireAtMs) {
            this.materialName = materialName;
            this.overlayId = overlayId;
            this.pulse = pulse;
            this.expireAtMs = expireAtMs;
        }

        static PulseOverrideCommand create(String materialName, String overlayId,
                                           DataMaterial.OverlayLayer.PulseSettings pulse, long durationMs) {
            if (materialName == null || overlayId == null || pulse == null) {
                return null;
            }
            String trimmedMaterial = materialName.trim();
            String trimmedOverlay = overlayId.trim();
            if (trimmedMaterial.isEmpty() || trimmedOverlay.isEmpty()) {
                return null;
            }
            DataMaterial.OverlayLayer.PulseSettings copy = new DataMaterial.OverlayLayer.PulseSettings();
            copy.copyFrom(pulse);
            long expireAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0L;
            return new PulseOverrideCommand(trimmedMaterial, trimmedOverlay, copy, expireAt);
        }

        boolean matches(PulseOverrideCommand other) {
            if (other == null) {
                return false;
            }
            return materialName.equals(other.materialName) && overlayId.equals(other.overlayId);
        }

        boolean isExpired(long now) {
            return expireAtMs > 0 && now >= expireAtMs;
        }

        boolean apply(@Nullable GltfRenderModel model, long now) {
            if (model == null || isExpired(now)) {
                return false;
            }
            long remaining = expireAtMs > 0 ? Math.max(0L, expireAtMs - now) : 0L;
            model.applyOverlayPulseOverride(materialName, overlayId, pulse, remaining);
            return true;
        }

        String getMaterialName() {
            return materialName;
        }

        String getOverlayId() {
            return overlayId;
        }
    }

    private static final class ColorPulseOverrideCommand {
        private final String materialName;
        private final String overlayId;
        private final DataMaterial.OverlayLayer.ColorPulseSettings pulse;
        private final long expireAtMs;

        private ColorPulseOverrideCommand(String materialName, String overlayId,
                                          DataMaterial.OverlayLayer.ColorPulseSettings pulse, long expireAtMs) {
            this.materialName = materialName;
            this.overlayId = overlayId;
            this.pulse = pulse;
            this.expireAtMs = expireAtMs;
        }

        static ColorPulseOverrideCommand create(String materialName, String overlayId,
                                                DataMaterial.OverlayLayer.ColorPulseSettings pulse, long durationMs) {
            if (materialName == null || overlayId == null || pulse == null) {
                return null;
            }
            String trimmedMaterial = materialName.trim();
            String trimmedOverlay = overlayId.trim();
            if (trimmedMaterial.isEmpty() || trimmedOverlay.isEmpty()) {
                return null;
            }
            DataMaterial.OverlayLayer.ColorPulseSettings copy = new DataMaterial.OverlayLayer.ColorPulseSettings();
            copy.copyFrom(pulse);
            long expireAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0L;
            return new ColorPulseOverrideCommand(trimmedMaterial, trimmedOverlay, copy, expireAt);
        }

        boolean matches(ColorPulseOverrideCommand other) {
            if (other == null) {
                return false;
            }
            return materialName.equals(other.materialName) && overlayId.equals(other.overlayId);
        }

        boolean isExpired(long now) {
            return expireAtMs > 0 && now >= expireAtMs;
        }

        boolean apply(@Nullable GltfRenderModel model, long now) {
            if (model == null || isExpired(now)) {
                return false;
            }
            long remaining = expireAtMs > 0 ? Math.max(0L, expireAtMs - now) : 0L;
            model.applyOverlayColorPulseOverride(materialName, overlayId, pulse, remaining);
            return true;
        }

        String getMaterialName() {
            return materialName;
        }

        String getOverlayId() {
            return overlayId;
        }
    }
}
