package org.mybad.minecraft.gltf.client;

import org.mybad.minecraft.gltf.GltfLog;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class CustomPlayerEventHandler {

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        try {
            CustomPlayerInstance instance = CustomPlayerManager.getPlayerInstance(event.getEntityPlayer().getName());
            CustomPlayerConfig config = instance.getConfig();
            if (config != null && config.renderMode == CustomPlayerConfig.RenderMode.OVERLAY) {
                return;
            }
            boolean rendered = CustomPlayerManager.renderCustomPlayer(
                event.getEntityPlayer(),
                event.getX(),
                event.getY(),
                event.getZ(),
                event.getEntityPlayer().rotationYaw,
                event.getPartialRenderTick()
            );

            event.setCanceled(rendered);

        } catch (Exception e) {
            GltfLog.LOGGER.error("Error in player render event", e);
            // 如果渲染失败，允许原版渲染继续
            event.setCanceled(false);
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        try {
            CustomPlayerInstance instance = CustomPlayerManager.getPlayerInstance(event.getEntityPlayer().getName());
            CustomPlayerConfig config = instance.getConfig();
            if (config == null || config.renderMode != CustomPlayerConfig.RenderMode.OVERLAY) {
                return;
            }
            CustomPlayerManager.renderCustomPlayer(
                event.getEntityPlayer(),
                event.getX(),
                event.getY(),
                event.getZ(),
                event.getEntityPlayer().rotationYaw,
                event.getPartialRenderTick()
            );
        } catch (Exception e) {
            GltfLog.LOGGER.error("Error in player post-render event", e);
        }
    }
}
