package org.mybad.minecraft.navigation;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.event.world.WorldEvent;

final class WaypointEventHandler {

    private final WaypointService service;

    WaypointEventHandler(WaypointService service) {
        this.service = service;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        service.renderWorld(event.getPartialTicks());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            service.clearWaypoints();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            service.clearWaypoints();
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        service.clearWaypoints();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        service.clearWaypoints();
    }
}
