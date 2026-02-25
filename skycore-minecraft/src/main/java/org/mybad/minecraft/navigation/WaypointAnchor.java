package org.mybad.minecraft.navigation;

import net.minecraft.client.renderer.GlStateManager;
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
    private String mappingName;
    private float mappingScale = 1.0f;

    WaypointAnchor(String id) {
        this.id = id;
    }

    boolean ensureHandle(WaypointStyleDefinition style, ResourceCacheManager cacheManager) {
        if (style == null || cacheManager == null) {
            return false;
        }
        String desiredMapping = style.getMapping();
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
        handle = created;
        mappingName = desiredMapping;
        return true;
    }

    void render(Waypoint waypoint,
                WaypointStyleDefinition style,
                double renderX,
                double renderY,
                double renderZ,
                float yaw,
                float scale,
                double distance,
                float partialTicks) {
        if (handle == null) {
            return;
        }
        handle.updateAnimations();
        handle.setModelScale(mappingScale * scale);
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
        handle.renderBlock(renderX, renderY, renderZ, yaw, partialTicks);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        if (!close) {
            GlStateManager.disableCull();
        }
        GlStateManager.popMatrix();
    }

    void dispose() {
        if (handle != null) {
            handle.dispose();
            handle = null;
        }
        mappingName = null;
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
    }
}
