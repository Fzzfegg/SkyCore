package org.mybad.minecraft.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.List;

final class WaypointOverlayRenderer {

    private final Minecraft minecraft = Minecraft.getMinecraft();

    void renderOverlay(Waypoint waypoint,
                       WaypointStyleDefinition style,
                       double renderX,
                       double renderY,
                       double renderZ,
                       double distance) {
        WaypointStyleDefinition.Overlay overlay = style.getOverlay();
        if (overlay == null || !overlay.isEnabled()) {
            return;
        }
        FontRenderer font = minecraft.fontRenderer;
        if (font == null) {
            return;
        }
        double cappedDistance = Math.min(distance, 512.0d);

        GlStateManager.pushMatrix();
        GlStateManager.translate(renderX, renderY + overlay.getVerticalOffset(), renderZ);
        GlStateManager.rotate(-minecraft.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(minecraft.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-0.016666668F, -0.016666668F, 0.016666668F);
        float referenceDistance = style.getMaxVisibleDistance();
        float scale = overlay.scaleWithDistance()
            ? overlay.scaleForDistance(cappedDistance, referenceDistance)
            : overlay.scaleForDistance(0, referenceDistance);
        GlStateManager.scale(scale, scale, scale);

        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        int gap = 2;
        java.util.List<LayerRenderData> layers = buildLayers(overlay, waypoint, cappedDistance, font);
        if (layers.isEmpty()) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            return;
        }
        int maxWidth = 0;
        int totalHeight = Math.max(0, (layers.size() - 1) * gap);
        for (LayerRenderData data : layers) {
            maxWidth = Math.max(maxWidth, data.width);
            totalHeight += data.height;
        }
        int yCursor = -totalHeight / 2;
        for (LayerRenderData data : layers) {
            int boxLeft = -maxWidth / 2;
            int boxRight = maxWidth / 2;
            int boxTop = yCursor;
            int boxBottom = yCursor + data.height;
            if ((data.backgroundColor >>> 24) != 0) {
                drawQuad(boxLeft, boxTop, boxRight, boxBottom, data.backgroundColor);
            }
            if (data.type == WaypointStyleDefinition.OverlayLayer.Type.TEXT) {
                int textX = boxLeft + (maxWidth - data.contentWidth) / 2;
                int textY = boxTop + MathHelper.floor(data.layer.getPadding());
                font.drawString(data.text, textX, textY, data.layer.getTextColor(), false);
            } else {
                int iconWidth = data.contentWidth;
                int iconHeight = data.contentHeight;
                int iconX = boxLeft + (maxWidth - iconWidth) / 2;
                int iconY = boxTop + MathHelper.floor(data.layer.getPadding());
                GlStateManager.color(1f, 1f, 1f, 1f);
                drawIcon(data.layer.getIcon(), iconX, iconY, iconWidth, iconHeight);
            }
            yCursor += data.height + gap;
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private java.util.List<LayerRenderData> buildLayers(WaypointStyleDefinition.Overlay overlay,
                                                        Waypoint waypoint,
                                                        double distance,
                                                        FontRenderer font) {
        java.util.List<LayerRenderData> result = new java.util.ArrayList<>();
        for (WaypointStyleDefinition.OverlayLayer layer : overlay.getLayers()) {
            if (layer.getType() == WaypointStyleDefinition.OverlayLayer.Type.TEXT) {
                String text = formatText(layer.getText(), waypoint, distance);
                int textWidth = font.getStringWidth(text);
                int width = MathHelper.floor(textWidth + layer.getPadding() * 2);
                int height = MathHelper.floor(font.FONT_HEIGHT + layer.getPadding() * 2);
                result.add(new LayerRenderData(layer, text, textWidth, font.FONT_HEIGHT, width, height));
            } else if (layer.getIcon() != null) {
                int iconWidth = layer.getIconWidth() > 0 ? MathHelper.floor(layer.getIconWidth()) : font.FONT_HEIGHT;
                int iconHeight = layer.getIconHeight() > 0 ? MathHelper.floor(layer.getIconHeight()) : font.FONT_HEIGHT;
                int width = MathHelper.floor(iconWidth + layer.getPadding() * 2);
                int height = MathHelper.floor(iconHeight + layer.getPadding() * 2);
                result.add(new LayerRenderData(layer, "", iconWidth, iconHeight, width, height));
            }
        }
        return result;
    }

    private String formatText(String template, Waypoint waypoint, double distance) {
        if (template == null || template.isEmpty()) {
            return waypoint.getId() + "  " + String.format("%.1fm", distance);
        }
        String formatted = template.replace("{distance}", String.format("%.1f", distance));
        formatted = formatted.replace("{id}", waypoint.getId());
        formatted = formatted.replace("{order}", Integer.toString(waypoint.getOrder()));
        return formatted;
    }

    private void drawQuad(int left, int top, int right, int bottom, int color) {
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        Tessellator tess = Tessellator.getInstance();
        GlStateManager.color(r, g, b, a);
        tess.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        tess.getBuffer().pos(left, bottom, 0).endVertex();
        tess.getBuffer().pos(right, bottom, 0).endVertex();
        tess.getBuffer().pos(right, top, 0).endVertex();
        tess.getBuffer().pos(left, top, 0).endVertex();
        tess.draw();
    }

    private void drawIcon(ResourceLocation icon, int x, int y, int width, int height) {
        minecraft.getTextureManager().bindTexture(icon);
        GlStateManager.color(1f, 1f, 1f, 1f);
        float u0 = 0f;
        float u1 = 1f;
        float v0 = 0f;
        float v1 = 1f;
        Tessellator tess = Tessellator.getInstance();
        tess.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        tess.getBuffer().pos(x, y + height, 0).tex(u0, v1).endVertex();
        tess.getBuffer().pos(x + width, y + height, 0).tex(u1, v1).endVertex();
        tess.getBuffer().pos(x + width, y, 0).tex(u1, v0).endVertex();
        tess.getBuffer().pos(x, y, 0).tex(u0, v0).endVertex();
        tess.draw();
    }

    private static final class LayerRenderData {
        final WaypointStyleDefinition.OverlayLayer layer;
        final WaypointStyleDefinition.OverlayLayer.Type type;
        final String text;
        final int contentWidth;
        final int contentHeight;
        final int width;
        final int height;
        final int backgroundColor;

        LayerRenderData(WaypointStyleDefinition.OverlayLayer layer,
                        String text,
                        int contentWidth,
                        int contentHeight,
                        int width,
                        int height) {
            this.layer = layer;
            this.type = layer.getType();
            this.text = text;
            this.contentWidth = contentWidth;
            this.contentHeight = contentHeight;
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
            this.backgroundColor = layer.getBackgroundColor();
        }
    }
}
