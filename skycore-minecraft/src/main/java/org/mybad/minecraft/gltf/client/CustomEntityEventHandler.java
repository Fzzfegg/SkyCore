package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import org.mybad.minecraft.gltf.client.network.RemoteEntityAppearanceRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class CustomEntityEventHandler {

    @SubscribeEvent
    public void onRenderLivingPre(RenderLivingEvent.Pre event) {
        if (event.getEntity() == null) {
            return;
        }
        EntityLivingBase entity = (EntityLivingBase) event.getEntity();
        if (entity instanceof EntityPlayer) {
            return;
        }
        if (!RemoteEntityAppearanceRegistry.hasAppearance(entity.getUniqueID())) {
            return;
        }
        try {
            boolean rendered = CustomEntityManager.renderCustomEntity(
                entity,
                event.getX(),
                event.getY(),
                event.getZ(),
                event.getEntity().rotationYaw,
                event.getPartialRenderTick()
            );
            if (rendered) {
                event.setCanceled(true);
            }
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error in entity render event", e);
            event.setCanceled(false);
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null) {
            return;
        }
        RemoteEntityAppearanceRegistry.pruneMissing(mc.world);
    }
}
