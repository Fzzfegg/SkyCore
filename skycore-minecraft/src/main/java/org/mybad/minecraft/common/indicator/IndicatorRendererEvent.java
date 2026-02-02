package org.mybad.minecraft.common.indicator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.mybad.skycoreproto.SkyCoreProto;

import java.util.*;
import java.util.Map.Entry;

public class IndicatorRendererEvent {
    public static IndicatorRendererEvent instance;
    public static boolean shadersInitialized;
    public static ShaderManager CIRCLE_SHADER;
    public static ShaderManager RECTANGLE_SHADER;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final RenderManager RENDER_MANAGER = mc.getRenderManager();
    public static Map<String, IndicatorRenderer3> ACTIVE_INDICATORS = new HashMap<>();

    public IndicatorRendererEvent() {
        instance = this;
    }

    public static void ensureShadersLoaded() {
        if (shadersInitialized) {return;}
        try {
            CIRCLE_SHADER = new ShaderManager(mc.getResourceManager(), "skycore:indicator/circle");
            RECTANGLE_SHADER = new ShaderManager(mc.getResourceManager(), "skycore:indicator/rectangle");
            shadersInitialized = true;
            org.mybad.minecraft.SkyCoreMod.LOGGER.info("[Indicator] shaders initialized");
        } catch (Exception ex) {
            org.mybad.minecraft.SkyCoreMod.LOGGER.error("[Indicator] failed to initialize shaders", ex);
            throw new RuntimeException("无法载入shader文件", ex);
        }
    }

    private static boolean isEntityReference(String value) {
        return value != null && value.split("-").length >= 5;
    }

    public static void applyIndicatorCommand(SkyCoreProto.IndicatorCommand packet) {
        if (packet == null) {
            return;
        }
        switch (packet.getAction()) {
            case REMOVE: 
                if (!packet.getId().isEmpty()) {
                    ACTIVE_INDICATORS.remove(packet.getId());
                }
                return;
            case CLEAR_ALL:
                ACTIVE_INDICATORS.clear();
                return;
            case UPSERT:
            default:
                break;
        }
        IndicatorRenderer3 renderer = null;
        if (packet.hasCircle()) {
            renderer = fromProto(packet.getCircle());
        } else if (packet.hasRectangle()) {
            renderer = fromProto(packet.getRectangle());
        } else if (packet.hasPolygon()) {
            renderer = fromProto(packet.getPolygon());
        }
        if (renderer == null) {
            org.mybad.minecraft.SkyCoreMod.LOGGER.warn("[Indicator] unsupported payload action={} id={}", packet.getAction(), packet.getId());
            return;
        }
        String id = packet.getId();
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        ACTIVE_INDICATORS.put(id, renderer);
        org.mybad.minecraft.SkyCoreMod.LOGGER.info("[Indicator] upsert id={} action={} mapSize={} lifetime={} radius={}",
            id, packet.getAction(), ACTIVE_INDICATORS.size(),
            renderer.lifetimeMs,
            renderer instanceof IndicatorRenderer ? ((IndicatorRenderer) renderer).getRadiusForDebug() : -1.0d);
    }

    public static Vec2f toPitchYaw(Vec3d a) {
        double x = a.x;
        double y = a.y;
        double z = a.z;
        if (x == 0.0d && z == 0.0d) {
            return new Vec2f(y > 0.0d ? -90f : 90f, 0.0f);
        }
        float pitch = (float) Math.toDegrees(Math.atan((-y) / Math.sqrt((x * x) + (z * z))));
        float yaw = (float) Math.toDegrees((Math.atan2(-x, z) + Math.PI * 2.0d) % (Math.PI * 2.0d));
        return new Vec2f(pitch, yaw);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (RENDER_MANAGER.renderViewEntity == null) {return;}
        if (ACTIVE_INDICATORS.isEmpty()) {return;}
        ensureShadersLoaded();

        Iterator<Entry<String, IndicatorRenderer3>> iter = ACTIVE_INDICATORS.entrySet().iterator();
        long now = System.currentTimeMillis();
        while (iter.hasNext()) {
            Entry<String, IndicatorRenderer3> entry = iter.next();
            IndicatorRenderer3 renderer = entry.getValue();
            if (now > renderer.startTimeMs + renderer.lifetimeMs) {
                iter.remove();
            } else {
                GlStateManager.pushMatrix();
                renderIndicator(renderer);
                GlStateManager.popMatrix();
            }
        }
    }

    private void renderIndicator(IndicatorRenderer3 indicator) {
        long elapsed = System.currentTimeMillis() - indicator.startTimeMs;
        double alphaFactor;
        double scaleFactor;
        double pulse = 0.0d;

        int growDuration = indicator.getGrowDurationMs();

        // entrance (fade in), steady, and exit (fade out) regions
        if (growDuration > 0 && elapsed < growDuration) {
            double normalized = Math.min(1.0d, elapsed / (double) growDuration);
            scaleFactor = 0.3d + (normalized * 0.7d);
            alphaFactor = scaleFactor;
        } else if (elapsed < indicator.lifetimeMs - 300) {
            scaleFactor = 1.0d;
            int pulseInterval = indicator.getPulseIntervalMs();
            if (pulseInterval > 0) {
                long steadyElapsed = Math.max(0L, elapsed - Math.max(growDuration, 0));
                pulse = (steadyElapsed % pulseInterval) / (double) pulseInterval;
            } else {
                pulse = 0.0d;
            }
            alphaFactor = 1.0d;
        } else {
            alphaFactor = 1.0d - (((double) (elapsed - indicator.lifetimeMs) + 300.0d) / 300.0d);
            scaleFactor = 1.0d;
            pulse = 0.0d;
        }

        double eased = easeOutCosine(scaleFactor);

        ResourceLocation texture = indicator.getTexture();
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.enableBlend();
        GlStateManager.disableCull();
        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false); // 不写入深度，但参与测试
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        indicator.applyTransform();
        indicator.applyColor((float) alphaFactor); // shader uniform 只接受 float
        indicator.renderScaled((float) eased);       // renderScaled 也期望 float

        if (pulse > 0.0d) {
            indicator.applyColor((float) alphaFactor);
            indicator.renderScaled((float) pulse);
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.enableCull();
    }

    public static double easeOutCosine(double a) {
        return (-(Math.cos(Math.PI * a) - 1.0d)) / 2.0d;
    }

    public static void renderParticle(float a) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buffer = tess.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-a, 0.0d, a).tex(0.0d, 1.0d).endVertex();
        buffer.pos(a, 0.0d, a).tex(1.0d, 1.0d).endVertex();
        buffer.pos(a, 0.0d, -a).tex(1.0d, 0.0d).endVertex();
        buffer.pos(-a, 0.0d, -a).tex(0.0d, 0.0d).endVertex();
        tess.draw();
    }

    private enum Axis {X, Y, Z}

    private static IndicatorValue createCoordinateValue(String raw, Axis axis) {
        if (isEntityReference(raw)) {
            String[] parts = raw.split(",");
            double offset = parts.length == 2 ? Double.parseDouble(parts[1]) : 0.0d;
            IndicatorValue value = new IndicatorValue(offset, (entity, ignored) -> interpolatePosition(entity, axis));
            value.setEntityA(UUID.fromString(parts[0]));
            return value;
        }
        return new IndicatorValue(Double.parseDouble(raw), null);
    }

    private static IndicatorValue createCircleYawValue(String raw) {
        if (!isEntityReference(raw)) {
            return new IndicatorValue(Double.parseDouble(raw), null);
        }
        String[] parts = raw.split(",");
        double offset = parts.length == 2 ? Double.parseDouble(parts[1]) : 0.0d;
        IndicatorValue value = new IndicatorValue(offset,
                (entity, ignored) -> (double) (entity.prevRotationYaw + ((entity.rotationYaw - entity.prevRotationYaw) * mc.getRenderPartialTicks())));
        value.setEntityA(UUID.fromString(parts[0]));
        return value;
    }

    private static IndicatorValue createRectangleDirectionValue(String raw) {
        if (!isEntityReference(raw)) {
            return new IndicatorValue(Double.parseDouble(raw), null);
        }
        String[] parts = raw.split(",");
        double baseValue = Double.parseDouble(parts[parts.length - 1]);

        if (parts.length >= 2 && isEntityReference(parts[1])) {
            IndicatorValue value = new IndicatorValue(baseValue, (entityA, entityB) -> {
                Vec3d target = interpolateXZ(entityB);
                Vec3d origin = interpolateXZ(entityA);
                Vec3d diff = target.subtract(origin).normalize();
                return (double) toPitchYaw(diff).y;
            });
            value.setEntityA(UUID.fromString(parts[0]));
            value.setEntityB(UUID.fromString(parts[1]));
            return value;
        }

        if (!isEntityReference(parts[0])) {
            return null;
        }

        IndicatorValue value = new IndicatorValue(baseValue,
                (entity, ignored) -> (double) (entity.prevRotationYaw + ((entity.rotationYaw - entity.prevRotationYaw) * mc.getRenderPartialTicks())));
        value.setEntityA(UUID.fromString(parts[0]));
        return value;
    }

    private static IndicatorValue createRectangleDistanceValue(String raw) {
        if (!isEntityReference(raw)) {
            return new IndicatorValue(Double.parseDouble(raw), null);
        }
        String[] parts = raw.split(",");
        double base = Double.parseDouble(parts[parts.length - 1]);
        IndicatorValue value = new IndicatorValue(base, (entityA, entityB) -> {
            Vec3d aPos = interpolateXZ(entityA);
            Vec3d bPos = interpolateXZ(entityB);
            return aPos.distanceTo(bPos) + 1.0d;
        });
        value.setEntityA(UUID.fromString(parts[0]));
        if (parts.length > 1) {
            value.setEntityB(UUID.fromString(parts[1]));
        }
        return value;
    }

    private static Vec3d interpolateXZ(Entity entity) {
        double x = interpolatePosition(entity, Axis.X);
        double z = interpolatePosition(entity, Axis.Z);
        return new Vec3d(x, 0.0d, z);
    }

    private static double interpolatePosition(Entity entity, Axis axis) {
        double partial = mc.getRenderPartialTicks();
        switch (axis) {
            case X:
                return entity.lastTickPosX + ((entity.posX - entity.lastTickPosX) * partial);
            case Y:
                return entity.lastTickPosY + ((entity.posY - entity.lastTickPosY) * partial);
            case Z:
            default:
                return entity.lastTickPosZ + ((entity.posZ - entity.lastTickPosZ) * partial);
        }
    }

    private static IndicatorRenderer fromProto(SkyCoreProto.CircleIndicator proto) {
        if (proto == null) {
            return null;
        }
        IndicatorRenderer indicator = new IndicatorRenderer();
        indicator.setXValue(createCoordinateValue(proto.getX(), Axis.X));
        indicator.setYValue(createCoordinateValue(proto.getY(), Axis.Y));
        indicator.setZValue(createCoordinateValue(proto.getZ(), Axis.Z));
        indicator.setYawValue(createDirectionValue(proto.getYaw()));
        indicator.setLifetime(Math.max(proto.getLifetimeMs(), 1));
        indicator.setFacingDegrees(proto.getFacingDegrees());
        indicator.setRadius(proto.getRadius());
        indicator.setGrowDurationMs(proto.getGrowDurationMs());
        if (!proto.getTexture().isEmpty()) {
            indicator.setTexture(parseTexture(proto.getTexture()));
            indicator.setTextureMaskEnabled(true);
        } else {
            indicator.setTexture(IndicatorRenderer3.DEFAULT_TEXTURE);
            indicator.setTextureMaskEnabled(false);
        }
        indicator.setPulseIntervalMs(proto.getPulseIntervalMs());
        applyColor(indicator, proto.getColor());
        return indicator;
    }

    private static IndicatorRenderer2 fromProto(SkyCoreProto.RectangleIndicator proto) {
        if (proto == null) {
            return null;
        }
        IndicatorRenderer2 indicator = new IndicatorRenderer2();
        indicator.setXValue(createCoordinateValue(proto.getX(), Axis.X));
        indicator.setYValue(createCoordinateValue(proto.getY(), Axis.Y));
        indicator.setZValue(createCoordinateValue(proto.getZ(), Axis.Z));
        indicator.setYawValue(createDirectionValue(proto.getYaw()));
        indicator.setDistanceValue(createDistanceValue(proto.getDistance()));
        indicator.setBaseWidth(proto.getBaseWidth());
        indicator.setLifetime(Math.max(proto.getLifetimeMs(), 1));
        applyColor(indicator, proto.getColor());
        return indicator;
    }

    private static IndicatorRendererPolygon fromProto(SkyCoreProto.PolygonIndicator proto) {
        if (proto == null || proto.getVerticesCount() < 3) {
            return null;
        }
        IndicatorRendererPolygon indicator = new IndicatorRendererPolygon();
        indicator.setXValue(createCoordinateValue(proto.getX(), Axis.X));
        indicator.setYValue(createCoordinateValue(proto.getY(), Axis.Y));
        indicator.setZValue(createCoordinateValue(proto.getZ(), Axis.Z));
        indicator.setYawValue(createDirectionValue(proto.getYaw()));
        indicator.setVertices(createVertexList(proto.getVerticesList()));
        indicator.setLifetime(Math.max(proto.getLifetimeMs(), 1));
        applyColor(indicator, proto.getColor());
        return indicator;
    }

    private static List<IndicatorRendererPolygon.IndicatorVertex> createVertexList(
        java.util.List<SkyCoreProto.IndicatorVertex> vertices
    ) {
        if (vertices == null || vertices.isEmpty()) {
            return Collections.emptyList();
        }
        List<IndicatorRendererPolygon.IndicatorVertex> result = new ArrayList<>();
        for (SkyCoreProto.IndicatorVertex vertex : vertices) {
            IndicatorValue x = createCoordinateValue(vertex.getX(), Axis.X);
            IndicatorValue y = createCoordinateValue(vertex.getY(), Axis.Y);
            IndicatorValue z = createCoordinateValue(vertex.getZ(), Axis.Z);
            result.add(new IndicatorRendererPolygon.IndicatorVertex(
                x != null ? x : new IndicatorValue(0.0d, null),
                y != null ? y : new IndicatorValue(0.0d, null),
                z != null ? z : new IndicatorValue(0.0d, null)
            ));
        }
        return result;
    }

    private static IndicatorValue createCoordinateValue(SkyCoreProto.IndicatorValue proto, Axis axis) {
        if (proto == null) {
            return new IndicatorValue(0.0d, null);
        }
        IndicatorValue value;
        if (!proto.getEntityUuid().isEmpty()) {
            value = new IndicatorValue(proto.getBase(), (entity, ignored) -> interpolatePosition(entity, axis));
            setEntityReferences(value, proto.getEntityUuid(), proto.getTargetUuid());
        } else {
            value = new IndicatorValue(proto.getBase(), null);
        }
        return value;
    }

    private static IndicatorValue createDirectionValue(SkyCoreProto.IndicatorValue proto) {
        if (proto == null) {
            return new IndicatorValue(0.0d, null);
        }
        if (!proto.getEntityUuid().isEmpty() && !proto.getTargetUuid().isEmpty()) {
            IndicatorValue value = new IndicatorValue(proto.getBase(), (entityA, entityB) -> {
                if (entityA == null || entityB == null) {
                    return proto.getBase();
                }
                Vec3d target = interpolateXZ(entityB);
                Vec3d origin = interpolateXZ(entityA);
                Vec3d diff = target.subtract(origin).normalize();
                return (double) toPitchYaw(diff).y;
            });
            setEntityReferences(value, proto.getEntityUuid(), proto.getTargetUuid());
            return value;
        }
        if (!proto.getEntityUuid().isEmpty()) {
            IndicatorValue value = new IndicatorValue(proto.getBase(),
                (entity, ignored) -> (double) (entity.prevRotationYaw
                    + ((entity.rotationYaw - entity.prevRotationYaw) * mc.getRenderPartialTicks())));
            setEntityReferences(value, proto.getEntityUuid(), null);
            return value;
        }
        return new IndicatorValue(proto.getBase(), null);
    }

    private static IndicatorValue createDistanceValue(SkyCoreProto.IndicatorValue proto) {
        if (proto == null) {
            return new IndicatorValue(0.0d, null);
        }
        if (!proto.getEntityUuid().isEmpty() && !proto.getTargetUuid().isEmpty()) {
            IndicatorValue value = new IndicatorValue(proto.getBase(), (entityA, entityB) -> {
                if (entityA == null || entityB == null) {
                    return proto.getBase();
                }
                Vec3d aPos = interpolateXZ(entityA);
                Vec3d bPos = interpolateXZ(entityB);
                return aPos.distanceTo(bPos);
            });
            setEntityReferences(value, proto.getEntityUuid(), proto.getTargetUuid());
            return value;
        }
        return new IndicatorValue(proto.getBase(), null);
    }

    private static void applyColor(IndicatorRenderer3 indicator, SkyCoreProto.IndicatorColor color) {
        int r = 255;
        int g = 255;
        int b = 255;
        if (color != null) {
            r = color.getR();
            g = color.getG();
            b = color.getB();
        }
        indicator.setColorR(r);
        indicator.setColorG(g);
        indicator.setColorB(b);
    }

    private static void setEntityReferences(IndicatorValue value, String entityA, String entityB) {
        if (value == null) {
            return;
        }
        if (entityA != null && !entityA.isEmpty()) {
            try {
                value.setEntityA(UUID.fromString(entityA));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (entityB != null && !entityB.isEmpty()) {
            try {
                value.setEntityB(UUID.fromString(entityB));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static ResourceLocation parseTexture(String raw) {
        if (raw == null || raw.isEmpty()) {
            return IndicatorRenderer3.DEFAULT_TEXTURE;
        }
        try {
            if (raw.contains(":")) {
                return new ResourceLocation(raw);
            }
            return new ResourceLocation(org.mybad.minecraft.SkyCoreMod.MOD_ID, raw);
        } catch (Exception ex) {
            org.mybad.minecraft.SkyCoreMod.LOGGER.warn("[Indicator] 无法解析纹理路径 {}，已回退默认。", raw);
            return IndicatorRenderer3.DEFAULT_TEXTURE;
        }
    }
}
