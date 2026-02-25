package org.mybad.minecraft.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.skycoreproto.SkyCoreProto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuidanceService {

    private static final GuidanceService INSTANCE = new GuidanceService();
    private static final double MIN_SEGMENT_LENGTH = 1.0E-4D;

    public static GuidanceService getInstance() {
        return INSTANCE;
    }

    private final Map<String, SegmentInstance> segments = new LinkedHashMap<>();
    private final GuidanceEventHandler eventHandler = new GuidanceEventHandler(this);
    private long lastUpdateNanos = -1L;

    private GuidanceService() {
    }

    public GuidanceEventHandler getEventHandler() {
        return eventHandler;
    }

    public synchronized void handleSync(SkyCoreProto.GuidanceSync proto) {
        segments.values().forEach(SegmentInstance::dispose);
        segments.clear();
        if (proto == null || proto.getSegmentsCount() == 0) {
            SkyCoreMod.LOGGER.info("[Guidance] 已清空远程路径段。");
            lastUpdateNanos = -1L;
            return;
        }
        for (SkyCoreProto.GuidanceSegment segmentProto : proto.getSegmentsList()) {
            SegmentInstance instance = SegmentInstance.fromProto(segmentProto);
            if (instance != null) {
                segments.put(instance.getId(), instance);
            }
        }
        lastUpdateNanos = -1L;
        SkyCoreMod.LOGGER.info("[Guidance] 已更新 {} 条路径 (version={}).", segments.size(), proto.getVersion());
    }

    void renderWorld(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.getRenderManager() == null) {
            return;
        }
        ResourceCacheManager cacheManager = SkyCoreMod.getResourceCacheManagerInstance();
        if (cacheManager == null) {
            clearSegments();
            return;
        }
        List<SegmentInstance> snapshot;
        synchronized (this) {
            if (segments.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(segments.values());
        }
        Vec3d playerEyes = mc.player.getPositionEyes(partialTicks);
        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;
        float cameraYaw = MathHelper.wrapDegrees(mc.getRenderManager().playerViewY);
        float cameraPitch = mc.getRenderManager().playerViewX;

        long now = System.nanoTime();
        double deltaSeconds = 0.0d;
        if (lastUpdateNanos > 0L) {
            deltaSeconds = (now - lastUpdateNanos) / 1_000_000_000d;
        }
        lastUpdateNanos = now;
        if (deltaSeconds < 0d) {
            deltaSeconds = 0d;
        }

        for (SegmentInstance instance : snapshot) {
            instance.advance(deltaSeconds);
            instance.render(cacheManager, cameraX, cameraY, cameraZ, cameraYaw, cameraPitch, partialTicks);
        }
    }

    synchronized void clearSegments() {
        segments.values().forEach(SegmentInstance::dispose);
        segments.clear();
        lastUpdateNanos = -1L;
    }

    public void reload() {
        clearSegments();
    }

    private static final class SegmentInstance {
        private final String id;
        private final Vec3d start;
        private final Vec3d direction;
        private final double length;
        private final float speed;
        private final float spacing;
        private final String mappingName;
        private final Float scaleOverride;
        private final boolean faceCamera;
        private final float facingYaw;
        private final int instanceCount;
        private final double spacingStep;

        private BedrockModelHandle handle;
        private float mappingScale = 1.0f;
        private boolean mappingMissingLogged = false;
        private double progress = 0.0d;

        static SegmentInstance fromProto(SkyCoreProto.GuidanceSegment proto) {
            String id = proto.getId().trim();
            String mapping = proto.getMapping().trim();
            if (id.isEmpty() || mapping.isEmpty()) {
                return null;
            }
            Vec3d start = new Vec3d(proto.getStartX(), proto.getStartY(), proto.getStartZ());
            Vec3d end = new Vec3d(proto.getEndX(), proto.getEndY(), proto.getEndZ());
            Vec3d dir = end.subtract(start);
            double length = Math.sqrt(dir.x * dir.x + dir.y * dir.y + dir.z * dir.z);
            Vec3d normalized = length > MIN_SEGMENT_LENGTH ? dir.scale(1.0d / length) : Vec3d.ZERO;
            float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0d);
            float speed = proto.getSpeed();
            if (speed < 0f) {
                speed = 0f;
            }
            float spacing = proto.getSpacing();
            if (spacing < 0f) {
                spacing = 0f;
            }
            double spacingStep = spacing > 0f ? spacing : 0d;
            int instances = spacing > 0f && length > MIN_SEGMENT_LENGTH
                ? Math.max(1, (int) Math.ceil(length / spacing))
                : 1;
            Float overrideScale = proto.hasModelScale() && proto.getModelScale() > 0f
                ? proto.getModelScale()
                : null;
            boolean faceCamera = proto.hasFaceCamera() && proto.getFaceCamera();
            return new SegmentInstance(id, mapping, start, normalized, length, speed, spacing, spacingStep, instances, overrideScale, faceCamera, yaw);
        }

        private SegmentInstance(String id,
                                String mappingName,
                                Vec3d start,
                                Vec3d direction,
                                double length,
                                float speed,
                                float spacing,
                                double spacingStep,
                                int instanceCount,
                                Float scaleOverride,
                                boolean faceCamera,
                                float facingYaw) {
            this.id = id;
            this.mappingName = mappingName;
            this.start = start;
            this.direction = direction;
            this.length = length;
            this.speed = speed;
            this.spacing = spacing;
            this.spacingStep = spacingStep;
            this.instanceCount = instanceCount;
            this.scaleOverride = scaleOverride;
            this.faceCamera = faceCamera;
            this.facingYaw = facingYaw;
        }

        String getId() {
            return id;
        }

        void advance(double deltaSeconds) {
            if (deltaSeconds <= 0d || speed <= 0f || length <= MIN_SEGMENT_LENGTH) {
                return;
            }
            double delta = deltaSeconds * speed;
            progress = (progress + delta) % length;
            if (progress < 0d) {
                progress += length;
            }
        }

        void render(ResourceCacheManager cacheManager,
                    double cameraX,
                    double cameraY,
                    double cameraZ,
                    float cameraYaw,
                    float cameraPitch,
                    float partialTicks) {
            if (!ensureHandle(cacheManager)) {
                return;
            }
            handle.updateAnimations();
            float resolvedScale = scaleOverride != null && scaleOverride > 0f ? scaleOverride : mappingScale;
            handle.setModelScale(resolvedScale);
            double effectiveSpacing = length > MIN_SEGMENT_LENGTH && instanceCount > 0
                ? length / instanceCount
                : length;
            for (int i = 0; i < instanceCount; i++) {
                Vec3d position = computePosition(i, effectiveSpacing);
                double renderX = position.x - cameraX;
                double renderY = position.y - cameraY;
                double renderZ = position.z - cameraZ;
                GlStateManager.pushMatrix();
                GlStateManager.enableDepth();
                GlStateManager.depthMask(true);
                if (faceCamera) {
                    handle.renderBillboard(renderX, renderY, renderZ, cameraYaw, cameraPitch, partialTicks);
                } else {
                    handle.renderBlock(renderX, renderY, renderZ, facingYaw, partialTicks);
                }
                GlStateManager.depthMask(true);
                GlStateManager.enableDepth();
                GlStateManager.popMatrix();
            }
        }

        private Vec3d computePosition(int index, double effectiveSpacing) {
            if (start == null) {
                return Vec3d.ZERO;
            }
            Vec3d offset = start;
            if (direction == null) {
                return offset;
            }
            if (length <= MIN_SEGMENT_LENGTH || direction == Vec3d.ZERO) {
                return offset;
            }
            double base = progress + index * effectiveSpacing;
            double along = base % length;
            if (along < 0d) {
                along += length;
            }
            return start.add(direction.scale(along));
        }

        private boolean ensureHandle(ResourceCacheManager cacheManager) {
            if (handle != null) {
                return true;
            }
            EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
            if (mapping == null) {
                if (!mappingMissingLogged) {
                    SkyCoreMod.LOGGER.warn("[Guidance] 映射 {} 未找到（段 {}）", mappingName, id);
                    mappingMissingLogged = true;
                }
                return false;
            }
            BedrockModelHandle created = ModelHandleFactory.create(cacheManager, mapping);
            if (created == null) {
                if (!mappingMissingLogged) {
                    SkyCoreMod.LOGGER.warn("[Guidance] 映射 {} 初始化失败（段 {}）", mappingName, id);
                    mappingMissingLogged = true;
                }
                return false;
            }
            applyMappingProperties(created, mapping);
            mappingScale = mapping.getModelScale() > 0f ? mapping.getModelScale() : 1.0f;
            mappingMissingLogged = false;
            handle = created;
            return true;
        }

        void dispose() {
            if (handle != null) {
                handle.dispose();
                handle = null;
            }
        }

        private void applyMappingProperties(BedrockModelHandle target, EntityModelMapping mapping) {
            target.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
            target.setEmissiveStrength(mapping.getEmissiveStrength());
            target.setBloomStrength(mapping.getBloomStrength());
            target.setBloomColor(mapping.getBloomColor());
            target.setBloomPasses(mapping.getBloomPasses());
            target.setBloomScaleStep(mapping.getBloomScaleStep());
            target.setBloomDownscale(mapping.getBloomDownscale());
            target.setBloomOffset(mapping.getBloomOffset());
            target.setModelScale(mapping.getModelScale());
            target.setModelOffset(mapping.getOffsetX(), mapping.getOffsetY(), mapping.getOffsetZ(), mapping.getOffsetMode());
            target.setRenderHurtTint(mapping.isRenderHurtTint());
            target.setHurtTint(mapping.getHurtTint());
        }
    }
}
