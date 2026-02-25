package org.mybad.minecraft.navigation;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.mybad.skycoreproto.SkyCoreProto;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class WaypointStyleDefinition {

    private static final float DEFAULT_BASE_SCALE = 1.0f;
    private static final float DEFAULT_MIN_SCALE = 0.85f;
    private static final float DEFAULT_MAX_SCALE = 1.25f;
    private static final float DEFAULT_MAX_DISTANCE = 128f;

    private final String id;
    private final String mapping;
    private final ResourceLocation hudIcon;
    private final int color;
    private final float baseScale;
    private final float minScale;
    private final float maxScale;
    private final float maxVisibleDistance;
    private final boolean faceCamera;
    private final Overlay overlay;

    private WaypointStyleDefinition(String id,
                                   String mapping,
                                   ResourceLocation hudIcon,
                                   int color,
                                   float baseScale,
                                   float minScale,
                                   float maxScale,
                                   float maxVisibleDistance,
                                   boolean faceCamera,
                                   Overlay overlay) {
        this.id = id;
        this.mapping = mapping;
        this.hudIcon = hudIcon;
        this.color = color;
        this.baseScale = baseScale;
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.maxVisibleDistance = maxVisibleDistance;
        this.faceCamera = faceCamera;
        this.overlay = overlay;
    }

    static WaypointStyleDefinition defaultStyle() {
        return new WaypointStyleDefinition(
            "default",
            "nav.default",
            null,
            0x00E0FFFF,
            DEFAULT_BASE_SCALE,
            DEFAULT_MIN_SCALE,
            DEFAULT_MAX_SCALE,
            DEFAULT_MAX_DISTANCE,
            true,
            Overlay.disabled()
        );
    }

    static @Nullable WaypointStyleDefinition fromProto(SkyCoreProto.NavigationStyle proto) {
        if (proto == null || proto.getId().isEmpty()) {
            return null;
        }
        String mapping = proto.getMapping().isEmpty() ? "nav.default" : proto.getMapping();
        ResourceLocation icon = parseIcon(proto.getHudIcon());
        int color = proto.getColor() == 0 ? 0x00E0FFFF : proto.getColor();
        float baseScale = proto.getBaseScale() > 0f ? proto.getBaseScale() : DEFAULT_BASE_SCALE;
        float minScale = proto.getMinScale() > 0f ? proto.getMinScale() : DEFAULT_MIN_SCALE;
        float maxScale = proto.getMaxScale() > 0f ? proto.getMaxScale() : DEFAULT_MAX_SCALE;
        if (minScale > maxScale) {
            float tmp = minScale;
            minScale = maxScale;
            maxScale = tmp;
        }
        float maxDistance = proto.getMaxVisibleDistance() > 0f ? proto.getMaxVisibleDistance() : DEFAULT_MAX_DISTANCE;
        boolean faceCamera = proto.getFaceCamera();
        Overlay overlay = Overlay.fromProto(proto.hasOverlay() ? proto.getOverlay() : null);
        return new WaypointStyleDefinition(
            proto.getId(),
            mapping,
            icon,
            color,
            baseScale,
            minScale,
            maxScale,
            maxDistance,
            faceCamera,
            overlay
        );
    }

    private static ResourceLocation parseIcon(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    String getId() {
        return id;
    }

    String getMapping() {
        return mapping;
    }

    @Nullable ResourceLocation getHudIcon() {
        return hudIcon;
    }

    int getColor() {
        return 0xFF000000 | color;
    }

    boolean isFaceCamera() {
        return faceCamera;
    }

    float getMaxVisibleDistance() {
        return maxVisibleDistance;
    }

    Overlay getOverlay() {
        return overlay;
    }

    float scaleForDistance(double distance) {
        float span = Math.max(0f, maxScale - minScale);
        float denom = maxVisibleDistance <= 0f ? 1f : maxVisibleDistance;
        float progress = (float) MathHelper.clamp(distance / denom, 0.0d, 1.0d);
        float gradient = minScale + span * progress;
        return baseScale * gradient;
    }

    static final class Overlay {
        private static final float DEFAULT_OFFSET = 2.4f;
        private static final float DEFAULT_MIN_SCALE = 1.0f;
        private static final float DEFAULT_MAX_SCALE = 1.0f;

        private final boolean enabled;
        private final float verticalOffset;
        private final boolean scaleWithDistance;
        private final float minScale;
        private final float maxScale;
        private final List<OverlayLayer> layers;

        private Overlay(boolean enabled,
                        float verticalOffset,
                        boolean scaleWithDistance,
                        float minScale,
                        float maxScale,
                        List<OverlayLayer> layers) {
            this.enabled = enabled;
            this.verticalOffset = verticalOffset;
            this.scaleWithDistance = scaleWithDistance;
            this.minScale = minScale;
            this.maxScale = maxScale;
            this.layers = layers;
        }

        static Overlay disabled() {
            return new Overlay(false, DEFAULT_OFFSET, false, DEFAULT_MIN_SCALE, DEFAULT_MAX_SCALE, Collections.emptyList());
        }

        static Overlay fromProto(@Nullable SkyCoreProto.NavigationStyleOverlay proto) {
            if (proto == null || !proto.getEnabled()) {
                return disabled();
            }
            List<OverlayLayer> layers = new ArrayList<>();
            for (SkyCoreProto.NavigationOverlayLayer layerProto : proto.getLayersList()) {
                OverlayLayer layer = OverlayLayer.fromProto(layerProto);
                if (layer != null) {
                    layers.add(layer);
                }
            }
            if (layers.isEmpty()) {
                return disabled();
            }
            float offset = proto.getVerticalOffset() != 0f ? proto.getVerticalOffset() : DEFAULT_OFFSET;
            boolean scaleDist = proto.getScaleWithDistance();
            float minScale = proto.getMinScale() > 0f ? proto.getMinScale() : DEFAULT_MIN_SCALE;
            float maxScale = proto.getMaxScale() > 0f ? proto.getMaxScale() : DEFAULT_MAX_SCALE;
            if (minScale > maxScale) {
                float tmp = minScale;
                minScale = maxScale;
                maxScale = tmp;
            }
            return new Overlay(true, offset, scaleDist, minScale, maxScale, Collections.unmodifiableList(layers));
        }

        boolean isEnabled() {
            return enabled;
        }

        float getVerticalOffset() {
            return verticalOffset;
        }

        boolean scaleWithDistance() {
            return scaleWithDistance;
        }

        float scaleForDistance(double distance, double maxDistance) {
            if (!scaleWithDistance || maxDistance <= 0f) {
                return maxScale;
            }
            float span = Math.max(0f, maxScale - minScale);
            float progress = (float) MathHelper.clamp(distance / maxDistance, 0.0d, 1.0d);
            return minScale + span * progress;
        }

        List<OverlayLayer> getLayers() {
            return layers;
        }
    }

    static final class OverlayLayer {

        enum Type {
            IMAGE,
            TEXT
        }

        private static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
        private static final int DEFAULT_BG_COLOR = 0x00000000;
        private static final float DEFAULT_PADDING = 4f;

        private final Type type;
        private final ResourceLocation icon;
        private final float iconWidth;
        private final float iconHeight;
        private final String text;
        private final int textColor;
        private final int backgroundColor;
        private final float padding;

        private OverlayLayer(Type type,
                             ResourceLocation icon,
                             float iconWidth,
                             float iconHeight,
                             String text,
                             int textColor,
                             int backgroundColor,
                             float padding) {
            this.type = type;
            this.icon = icon;
            this.iconWidth = iconWidth;
            this.iconHeight = iconHeight;
            this.text = text;
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
            this.padding = padding;
        }

        static @Nullable OverlayLayer fromProto(SkyCoreProto.NavigationOverlayLayer proto) {
            Type type = proto.getType() == SkyCoreProto.NavigationOverlayLayer.LayerType.IMAGE ? Type.IMAGE : Type.TEXT;
            ResourceLocation icon = parseIcon(proto.getIcon());
            float iconWidth = proto.getIconWidth() > 0f ? proto.getIconWidth() : 0f;
            float iconHeight = proto.getIconHeight() > 0f ? proto.getIconHeight() : 0f;
            String text = proto.getText();
            int textColor = proto.getTextColor() == 0 ? DEFAULT_TEXT_COLOR : (proto.getTextColor() | 0xFF000000);
            int bgColor = proto.getBackgroundColor() == 0 ? DEFAULT_BG_COLOR : proto.getBackgroundColor();
            float padding = proto.getPadding() > 0f ? proto.getPadding() : DEFAULT_PADDING;
            if (type == Type.IMAGE && (icon == null)) {
                return null;
            }
            if (type == Type.TEXT && (text == null || text.isEmpty())) {
                return null;
            }
            return new OverlayLayer(type, icon, iconWidth, iconHeight, text, textColor, bgColor, padding);
        }

        Type getType() {
            return type;
        }

        @Nullable ResourceLocation getIcon() {
            return icon;
        }

        float getIconWidth() {
            return iconWidth;
        }

        float getIconHeight() {
            return iconHeight;
        }

        String getText() {
            return text;
        }

        int getTextColor() {
            return textColor;
        }

        int getBackgroundColor() {
            return backgroundColor;
        }

        float getPadding() {
            return padding;
        }
    }
}
