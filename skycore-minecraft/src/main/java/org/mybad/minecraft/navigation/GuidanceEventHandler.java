package org.mybad.minecraft.navigation;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

final class GuidanceEventHandler {

    private final GuidanceService service;

    GuidanceEventHandler(GuidanceService service) {
        this.service = service;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        service.renderWorld(event.getPartialTicks());
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            service.clearSegments();
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() != null && event.getWorld().isRemote) {
            service.clearSegments();
        }
    }

    @SubscribeEvent
    public void onClientConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        service.clearSegments();
    }

    @SubscribeEvent
    public void onClientDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        service.clearSegments();
    }
}
