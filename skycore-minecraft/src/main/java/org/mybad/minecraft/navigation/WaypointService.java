package org.mybad.minecraft.navigation;

import org.mybad.skycoreproto.SkyCoreProto;

import javax.annotation.Nullable;

@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
public final class WaypointService {

    private static final WaypointService INSTANCE = new WaypointService();

    public static WaypointService getInstance() {
        return INSTANCE;
    }

    private final WaypointManager manager = new WaypointManager();
    private final WaypointStyleRegistry styleRegistry = new WaypointStyleRegistry();
    private final WaypointRenderer renderer = new WaypointRenderer(manager, styleRegistry);
    private final WaypointEventHandler eventHandler = new WaypointEventHandler(this);

    private WaypointService() {
    }

    public WaypointEventHandler getEventHandler() {
        return eventHandler;
    }

    public void handleWaypointUpdate(@Nullable SkyCoreProto.WaypointTarget proto) {
        Waypoint waypoint = Waypoint.fromProto(proto);
        if (waypoint != null) {
            manager.upsert(waypoint);
        }
    }

    public void handleWaypointRemove(@Nullable SkyCoreProto.WaypointRemove proto) {
        if (proto == null || proto.getId().isEmpty()) {
            return;
        }
        manager.remove(proto.getId());
    }

    public void handleStyleConfig(@Nullable SkyCoreProto.NavigationStyleConfig config) {
        styleRegistry.applyRemoteConfig(config);
        renderer.invalidateAll();
    }

    void renderWorld(float partialTicks) {
        renderer.render(partialTicks);
    }

    public void clearWaypoints() {
        manager.clear();
        renderer.clearAnchors();
    }

    public void reload() {
        styleRegistry.reloadFromDisk();
        clearWaypoints();
    }
}
