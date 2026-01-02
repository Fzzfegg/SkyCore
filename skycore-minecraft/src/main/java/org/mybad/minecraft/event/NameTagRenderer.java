package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;

final class NameTagRenderer {
    private NameTagRenderer() {
    }

    static boolean shouldRenderNameTag(EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == entity) {
            return false;
        }
        return entity.getAlwaysRenderNameTagForRender();
    }

    static void render(EntityLivingBase entity, double x, double y, double z) {
        if (entity == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager().renderEngine != null) {
            double yOffset = y + entity.height + 0.5;
            mc.getRenderManager().renderEntity(entity, x, yOffset, z, 0, 0, false);
        }
    }
}
