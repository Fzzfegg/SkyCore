package org.mybad.minecraft.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

final class WaypointRenderer {

    private final WaypointManager manager;
    private final WaypointStyleRegistry styleRegistry;
    private final Map<String, WaypointAnchor> anchors = new HashMap<>();
    private FootIndicatorInstance footIndicatorHandle;
    private final WaypointOverlayRenderer overlayRenderer = new WaypointOverlayRenderer();
    private Vec3d lastFootBlockCenter;

    WaypointRenderer(WaypointManager manager, WaypointStyleRegistry styleRegistry) {
        this.manager = manager;
        this.styleRegistry = styleRegistry;
    }

    void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.getRenderManager() == null) {
            return;
        }
        ResourceCacheManager cacheManager = SkyCoreMod.getResourceCacheManagerInstance();
        if (cacheManager == null) {
            return;
        }
        Vec3d playerEyes = mc.player.getPositionEyes(partialTicks);
        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;
        Set<String> seen = new HashSet<>();
        for (Waypoint waypoint : manager.getOrderedWaypoints()) {
            if (!waypoint.isActive()) {
                continue;
            }
            WaypointStyleDefinition style = styleRegistry.resolve(waypoint.getStyleId());
            WaypointAnchor anchor = anchors.computeIfAbsent(waypoint.getId(), WaypointAnchor::new);
            if (!anchor.ensureHandle(style, cacheManager)) {
                continue;
            }
            seen.add(waypoint.getId());
            double distance = waypoint.distanceTo(playerEyes);
            double renderX = waypoint.getPosition().x - cameraX;
            double renderY = waypoint.getPosition().y - cameraY;
            double renderZ = waypoint.getPosition().z - cameraZ;
            float finalScale = style.scaleForDistance(distance);
            float overlayBaseHeight;
            float baseHeight = style.getOverlay().hasCustomBaseHeight()
                ? style.getOverlay().getBaseHeight()
                : anchor.getOverlayBaseHeight();
            overlayBaseHeight = Math.max(0f, baseHeight) * Math.max(0.01f, finalScale);
            float yaw = style.isFaceCamera()
                ? MathHelper.wrapDegrees(mc.getRenderManager().playerViewY)
                : 0f;
            float pitch = 0f;
            anchor.render(waypoint, style, renderX, renderY, renderZ, yaw, pitch, style.isFaceCamera(), finalScale, distance, partialTicks);
            overlayRenderer.renderOverlay(waypoint, style, renderX, renderY, renderZ, overlayBaseHeight, distance);
        }
        cleanupAnchors(seen);
        renderFootIndicator(mc, cacheManager, partialTicks, playerEyes, cameraX, cameraY, cameraZ);
    }

    void clearAnchors() {
        for (WaypointAnchor anchor : anchors.values()) {
            anchor.dispose();
        }
        anchors.clear();
        disposeFootIndicator();
    }

    void invalidateAll() {
        clearAnchors();
    }

    private void cleanupAnchors(Set<String> activeIds) {
        anchors.entrySet().removeIf(entry -> {
            if (!activeIds.contains(entry.getKey())) {
                entry.getValue().dispose();
                return true;
            }
            return false;
        });
    }

    private void renderFootIndicator(Minecraft mc,
                                     ResourceCacheManager cacheManager,
                                     float partialTicks,
                                     Vec3d playerEyes,
                                     double cameraX,
                                     double cameraY,
                                     double cameraZ) {
        if (mc.player == null || cacheManager == null) {
            disposeFootIndicator();
            return;
        }
        Waypoint tracked = manager.getCurrentTracked();
        if (tracked == null) {
            disposeFootIndicator();
            return;
        }
        WaypointStyleDefinition style = styleRegistry.resolve(tracked.getStyleId());
        WaypointStyleDefinition.FootIndicator indicator = style.getFootIndicator();
        if (indicator == null || !indicator.isEnabled()) {
            disposeFootIndicator();
            return;
        }
        ensureFootIndicatorHandle(indicator, cacheManager);
        if (footIndicatorHandle == null) {
            return;
        }
        Vec3d basePos = resolveFootBase(mc.player, indicator, partialTicks);
        double renderX = basePos.x - cameraX;
        double renderY = basePos.y - cameraY + indicator.getVerticalOffset();
        double renderZ = basePos.z - cameraZ;
        double distance = basePos.distanceTo(playerEyes);
        float yaw = indicator.isFaceTarget()
            ? computeYawTowards(basePos, tracked.getPosition())
            : MathHelper.wrapDegrees(mc.player.rotationYaw);
        footIndicatorHandle.render(renderX, renderY, renderZ, yaw, indicator.getScale(), partialTicks);
    }

    private void ensureFootIndicatorHandle(WaypointStyleDefinition.FootIndicator indicator,
                                           ResourceCacheManager cacheManager) {
        if (indicator == null || cacheManager == null) {
            disposeFootIndicator();
            return;
        }
        if (footIndicatorHandle != null && footIndicatorHandle.matches(indicator.getMapping())) {
            return;
        }
        disposeFootIndicator();
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(indicator.getMapping());
        if (mapping == null) {
            SkyCoreMod.LOGGER.warn("[Waypoint] Foot indicator mapping {} not found", indicator.getMapping());
            return;
        }
        BedrockModelHandle handle = ModelHandleFactory.create(cacheManager, mapping);
        if (handle == null) {
            SkyCoreMod.LOGGER.warn("[Waypoint] Foot indicator mapping {} init failed", indicator.getMapping());
            return;
        }
        applyMappingProperties(handle, mapping);
        footIndicatorHandle = new FootIndicatorInstance(indicator.getMapping(), handle, mapping.getModelScale());
    }

    private void disposeFootIndicator() {
        if (footIndicatorHandle != null) {
            footIndicatorHandle.dispose();
            footIndicatorHandle = null;
        }
        lastFootBlockCenter = null;
    }

    private Vec3d resolveFootBase(EntityPlayer player,
                                  WaypointStyleDefinition.FootIndicator indicator,
                                  float partialTicks) {
        double interpX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double interpY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double interpZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        Vec3d target;
        if (indicator.isStickToLastBlock()) {
            int blockX = MathHelper.floor(interpX);
            int blockZ = MathHelper.floor(interpZ);
            int blockY = MathHelper.floor(interpY - 0.51d);
            BlockPos below = new BlockPos(blockX, blockY, blockZ);
            target = new Vec3d(below.getX() + 0.5d, below.getY() + 1.0E-3d, below.getZ() + 0.5d);
        } else {
            target = new Vec3d(interpX, interpY - 0.01d, interpZ);
        }
        if (lastFootBlockCenter == null) {
            lastFootBlockCenter = target;
        } else {
            double smooth = indicator.isStickToLastBlock() ? 0.65d : 0.4d;
            lastFootBlockCenter = lastFootBlockCenter.add(target.subtract(lastFootBlockCenter).scale(smooth));
        }
        return lastFootBlockCenter;
    }

    private float computeYawTowards(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90.0d;
        return (float) MathHelper.wrapDegrees(angle);
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

    private static final class FootIndicatorInstance {
        private final String mappingName;
        private final BedrockModelHandle handle;
        private final float mappingScale;

        FootIndicatorInstance(String mappingName, BedrockModelHandle handle, float mappingScale) {
            this.mappingName = mappingName;
            this.handle = handle;
            this.mappingScale = mappingScale > 0f ? mappingScale : 1.0f;
        }

        boolean matches(String desired) {
            return mappingName.equals(desired);
        }

        void render(double renderX,
                    double renderY,
                    double renderZ,
                    float yaw,
                    float scale,
                    float partialTicks) {
            handle.updateAnimations();
            handle.setModelScale(mappingScale * scale);
            boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.pushMatrix();
            handle.renderBlock(renderX, renderY, renderZ, yaw, partialTicks);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(depthMask);
            if (!depthEnabled) {
                GlStateManager.disableDepth();
            }
        }

        void dispose() {
            handle.dispose();
        }
    }
}
