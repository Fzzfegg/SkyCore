package org.mybad.minecraft.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.entity.EntityRenderDispatcher;
import org.mybad.minecraft.render.skull.SkullModelManager;

@SideOnly(Side.CLIENT)
public final class DebugRenderOverlay {
    private static final double MAX_DISTANCE_SQ = 256.0 * 256.0;

    private DebugRenderOverlay() {}

    public static void render(RenderWorldLastEvent event, EntityRenderDispatcher dispatcher) {
        if (event == null || dispatcher == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        net.minecraft.entity.Entity camera = mc.getRenderViewEntity();
        if (camera == null) {
            return;
        }
        double partial = event.getPartialTicks();
        double camX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partial;
        double camY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partial;
        double camZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partial;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.glLineWidth(2.0F);
        
        if (DebugRenderController.shouldDrawEntityBoxes()) {
            drawEntityBoxes(dispatcher, camX, camY, camZ);
        }
        if (DebugRenderController.shouldDrawSkullAnchors()) {
            drawSkullAnchors(camX, camY, camZ);
        }

        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void drawEntityBoxes(EntityRenderDispatcher dispatcher, double camX, double camY, double camZ) {
        dispatcher.forEachWrapper((entity, entry) -> {
            if (entity == null || entity.isDead) {
                return;
            }
            double dx = entity.posX - camX;
            double dy = entity.posY + entity.height * 0.5 - camY;
            double dz = entity.posZ - camZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > MAX_DISTANCE_SQ) {
                return;
            }
            AxisAlignedBB vanilla = entity.getEntityBoundingBox().grow(0.01).offset(-camX, -camY, -camZ);
            RenderGlobal.drawSelectionBoundingBox(vanilla, 0.1F, 1.0F, 0.1F, 0.8F);

            EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(entry.mappingName);
            if (mapping == null || !mapping.hasCustomRenderBox()) {
                return;
            }
            AxisAlignedBB custom = buildCustomBoundingBox(entity, mapping);
            if (custom != null) {
                RenderGlobal.drawSelectionBoundingBox(custom.offset(-camX, -camY, -camZ), 1.0F, 0.4F, 0.1F, 0.9F);
            }
        });
    }

    private static AxisAlignedBB buildCustomBoundingBox(EntityLivingBase entity, EntityModelMapping mapping) {
        double width = mapping.getRenderBoxWidth();
        double height = mapping.getRenderBoxHeight();
        double depth = mapping.getRenderBoxDepth();
        if (width <= 0.0 || height <= 0.0 || depth <= 0.0) {
            return null;
        }
        double halfWidth = width * 0.5;
        double halfDepth = depth * 0.5;
        double minX = entity.posX - halfWidth;
        double minY = entity.posY;
        double minZ = entity.posZ - halfDepth;
        double maxX = entity.posX + halfWidth;
        double maxY = entity.posY + height;
        double maxZ = entity.posZ + halfDepth;
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void drawSkullAnchors(double camX, double camY, double camZ) {
        SkullModelManager.collectDebugInfo(info -> {
            if (info == null) {
                return;
            }
            double dx = info.getWorldX() - camX;
            double dy = info.getWorldY() - camY;
            double dz = info.getWorldZ() - camZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq > MAX_DISTANCE_SQ) {
                return;
            }
            AxisAlignedBB marker = new AxisAlignedBB(
                info.getWorldX() - 0.15, info.getWorldY() - 0.15, info.getWorldZ() - 0.15,
                info.getWorldX() + 0.15, info.getWorldY() + 0.15, info.getWorldZ() + 0.15
            );
            RenderGlobal.drawSelectionBoundingBox(marker.offset(-camX, -camY, -camZ), 0.2F, 0.6F, 1.0F, 0.9F);
        });
    }
}
