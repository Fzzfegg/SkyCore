package org.mybad.minecraft.navigation;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.resource.ResourceCacheManager;

import javax.annotation.Nullable;

final class WaypointAnchor {

    private final String id;
    private BedrockModelHandle handle;
    private TargetIndicatorInstance targetIndicatorHandle;
    private String mappingName;
    private float mappingScale = 1.0f;
    private float overlayBaseHeight = DEFAULT_OVERLAY_BASE_HEIGHT;
    private static final float DEFAULT_OVERLAY_BASE_HEIGHT = 2.4f;

    WaypointAnchor(String id) {
        this.id = id;
    }

    boolean ensureHandle(WaypointStyleDefinition style, ResourceCacheManager cacheManager) {
        if (style == null) {
            dispose();
            disposeTargetIndicator();
            return false;
        }
        boolean ok = ensureHandle(style.getMapping(), cacheManager);
        ensureTargetIndicator(style.getFootIndicator(), cacheManager);
        return ok;
    }

    boolean ensureHandle(String desiredMapping, ResourceCacheManager cacheManager) {
        if (cacheManager == null) {
            return false;
        }
        if (desiredMapping == null || desiredMapping.isEmpty()) {
            return false;
        }
        if (handle != null && desiredMapping.equals(mappingName)) {
            return true;
        }
        dispose();
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(desiredMapping);
        if (mapping == null) {
            SkyCoreMod.LOGGER.warn("[Waypoint] 映射 {} 未找到，无法渲染 {}", desiredMapping, id);
            return false;
        }
        BedrockModelHandle created = ModelHandleFactory.create(cacheManager, mapping);
        if (created == null) {
            SkyCoreMod.LOGGER.warn("[Waypoint] 映射 {} 初始化失败", desiredMapping);
            return false;
        }
        applyMappingProperties(created, mapping);
        mappingScale = mapping.getModelScale() > 0f ? mapping.getModelScale() : 1.0f;
        overlayBaseHeight = resolveOverlayBaseHeight(mapping);
        handle = created;
        mappingName = desiredMapping;
        return true;
    }

    void render(Waypoint waypoint,
                WaypointStyleDefinition style,
                Vec3d anchorPos,
                Vec3d indicatorPos,
                double cameraX,
                double cameraY,
                double cameraZ,
                float yaw,
                float pitch,
                boolean faceCamera,
                float scale,
                double distance,
                float partialTicks,
                Vec3d playerEyes) {
        if (handle == null) {
            return;
        }
        handle.updateAnimations();
        handle.setModelScale(mappingScale * scale);
        double renderX = anchorPos.x - cameraX;
        double renderY = anchorPos.y - cameraY;
        double renderZ = anchorPos.z - cameraZ;
        handle.setPackedLightFromWorld(anchorPos.x, anchorPos.y, anchorPos.z);
        GlStateManager.pushMatrix();
        boolean close = distance <= 6.0d;
        if (close) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
        } else {
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.enableCull();
        }
        if (faceCamera) {
            handle.renderBillboard(renderX, renderY, renderZ, yaw, pitch, partialTicks);
        } else {
            handle.renderBlock(renderX, renderY, renderZ, yaw, partialTicks);
        }
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        if (!close) {
            GlStateManager.disableCull();
        }
        GlStateManager.popMatrix();
        if (targetIndicatorHandle != null) {
            WaypointStyleDefinition.FootIndicator indicator = style.getFootIndicator();
            float indicatorYaw = 0f;
            if (indicator != null && indicator.isFaceTarget() && playerEyes != null) {
                indicatorYaw = computeYawTowards(indicatorPos, playerEyes);
            }
            double indicatorX = indicatorPos.x - cameraX;
            double indicatorY = indicatorPos.y - cameraY;
            double indicatorZ = indicatorPos.z - cameraZ;
            targetIndicatorHandle.render(indicatorPos, indicatorX, indicatorY, indicatorZ, indicatorYaw, partialTicks);
        }
    }

    float getOverlayBaseHeight() {
        return overlayBaseHeight;
    }

    void dispose() {
        if (handle != null) {
            handle.dispose();
            handle = null;
        }
        mappingName = null;
        disposeTargetIndicator();
    }

    private void applyMappingProperties(BedrockModelHandle target, EntityModelMapping mapping) {
        target.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
        target.setEmissiveStrength(mapping.getEmissiveStrength());
        target.setBloomStrength(mapping.getBloomStrength());
        target.setBloomColor(mapping.getBloomColor());
        target.setBloomPasses(mapping.getBloomPasses());
        target.setBloomScaleStep(mapping.getBloomScaleStep());
        target.setBloomDownscale(mapping.getBloomDownscale());
        target.setBloomOffset(mapping.getBloomOffset());
        target.setModelScale(mapping.getModelScale());
        target.setModelOffset(mapping.getOffsetX(), mapping.getOffsetY(), mapping.getOffsetZ(), mapping.getOffsetMode());
        target.setRenderHurtTint(mapping.isRenderHurtTint());
        target.setHurtTint(mapping.getHurtTint());
        target.setLightning(mapping.isLightning());
    }

    private float resolveOverlayBaseHeight(EntityModelMapping mapping) {
        if (mapping == null) {
            return DEFAULT_OVERLAY_BASE_HEIGHT;
        }
        if (mapping.getRenderBoxHeight() > 0f) {
            return mapping.getRenderBoxHeight();
        }
        float scale = mapping.getModelScale();
        if (scale <= 0f) {
            scale = 1.0f;
        }
        return Math.max(1.6f, DEFAULT_OVERLAY_BASE_HEIGHT * scale);
    }

    private void ensureTargetIndicator(WaypointStyleDefinition.FootIndicator indicator,
                                       ResourceCacheManager cacheManager) {
        if (indicator == null || !indicator.isEnabled()) {
            disposeTargetIndicator();
            return;
        }
        String desired = indicator.getTargetMapping();
        if (desired == null || desired.isEmpty() || cacheManager == null) {
            disposeTargetIndicator();
            return;
        }
        if (targetIndicatorHandle != null && targetIndicatorHandle.matches(desired)) {
            targetIndicatorHandle.setScale(indicator.getTargetScale());
            targetIndicatorHandle.setVerticalOffset(indicator.getTargetVerticalOffset());
            return;
        }
        disposeTargetIndicator();
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(desired);
        if (mapping == null) {
            SkyCoreMod.LOGGER.warn("[Waypoint] 目标指示器映射 {} 未找到", desired);
            return;
        }
        BedrockModelHandle handle = ModelHandleFactory.create(cacheManager, mapping);
        if (handle == null) {
            SkyCoreMod.LOGGER.warn("[Waypoint] 目标指示器映射 {} 初始化失败", desired);
            return;
        }
        applyMappingProperties(handle, mapping);
        float mappingScale = mapping.getModelScale() > 0f ? mapping.getModelScale() : 1.0f;
        targetIndicatorHandle = new TargetIndicatorInstance(
            desired,
            handle,
            mappingScale,
            indicator.getTargetScale(),
            indicator.getTargetVerticalOffset()
        );
    }

    private void disposeTargetIndicator() {
        if (targetIndicatorHandle != null) {
            targetIndicatorHandle.dispose();
            targetIndicatorHandle = null;
        }
    }

    private static final class TargetIndicatorInstance {
        private final String mappingName;
        private final BedrockModelHandle handle;
        private final float mappingScale;
        private float indicatorScale;
        private float verticalOffset;

        TargetIndicatorInstance(String mappingName,
                                BedrockModelHandle handle,
                                float mappingScale,
                                float indicatorScale,
                                float verticalOffset) {
            this.mappingName = mappingName;
            this.handle = handle;
            this.mappingScale = mappingScale;
            this.indicatorScale = indicatorScale > 0f ? indicatorScale : 1.0f;
            this.verticalOffset = verticalOffset;
        }

        boolean matches(String desired) {
            return mappingName.equals(desired);
        }

        void setScale(float scale) {
            this.indicatorScale = scale > 0f ? scale : 1.0f;
        }

        void setVerticalOffset(float offset) {
            this.verticalOffset = offset;
        }

        void render(Vec3d worldPos, double renderX, double renderY, double renderZ, float yaw, float partialTicks) {
            handle.updateAnimations();
            handle.setModelScale(mappingScale * indicatorScale);
            if (worldPos != null) {
                handle.setPackedLightFromWorld(worldPos.x, worldPos.y, worldPos.z);
            }
            GlStateManager.pushMatrix();
            handle.renderBlock(renderX, renderY + verticalOffset, renderZ, yaw, partialTicks);
            GlStateManager.popMatrix();
        }

        void dispose() {
            handle.dispose();
        }
    }

    private float computeYawTowards(Vec3d from, Vec3d to) {
        if (from == null || to == null) {
            return 0f;
        }
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0d;
        return (float) MathHelper.wrapDegrees(angle);
    }
}
