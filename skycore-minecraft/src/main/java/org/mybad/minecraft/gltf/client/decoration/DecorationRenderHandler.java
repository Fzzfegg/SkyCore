package org.mybad.minecraft.gltf.client.decoration;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Forwards render ticks to the decoration manager so skull-based GLTF props can be drawn.
 */
@SideOnly(Side.CLIENT)
public class DecorationRenderHandler {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        DecorationManager.render(event.getPartialTicks());
    }
}
