package org.mybad.minecraft.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.resource.ResourceCacheManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class WaypointRenderer {

    private final WaypointManager manager;
    private final WaypointStyleRegistry styleRegistry;
    private final Map<String, WaypointAnchor> anchors = new HashMap<>();
    private final WaypointOverlayRenderer overlayRenderer = new WaypointOverlayRenderer();

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
            float yaw = style.isFaceCamera()
                ? MathHelper.wrapDegrees(mc.getRenderManager().playerViewY)
                : 0f;
            anchor.render(waypoint, style, renderX, renderY, renderZ, yaw, finalScale, distance, partialTicks);
            overlayRenderer.renderOverlay(waypoint, style, renderX, renderY, renderZ, distance);
        }
        cleanupAnchors(seen);
    }

    void clearAnchors() {
        for (WaypointAnchor anchor : anchors.values()) {
            anchor.dispose();
        }
        anchors.clear();
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

}
