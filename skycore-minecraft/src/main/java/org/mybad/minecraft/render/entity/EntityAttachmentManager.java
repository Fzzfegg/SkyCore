package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.config.SkyCoreConfig;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.ModelHandleFactory;
import org.mybad.minecraft.render.entity.events.AnimationEventContext;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.render.entity.events.AnimationEventState;
import org.mybad.minecraft.render.entity.events.AnimationEventTarget;
import org.mybad.minecraft.render.entity.events.OverlayEventCursorCache;
import org.mybad.minecraft.render.trail.WeaponTrailController;
import org.mybad.minecraft.render.trail.WeaponTrailRenderer;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.skycoreproto.SkyCoreProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client-side fake attachments that follow real entities.
 */
public final class EntityAttachmentManager {

    private final ResourceCacheManager cacheManager;
    private final Map<UUID, List<AttachmentInstance>> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, List<WorldAttachmentInstance>> worldAttachments = new ConcurrentHashMap<>();

    EntityAttachmentManager(ResourceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void spawnAttachment(SkyCoreProto.EntityAttachment packet) {
        if (packet == null || packet.getAttachmentId().isEmpty()) {
            return;
        }
        UUID targetUuid = parseUuid(packet.getTargetEntityUuid());
        if (targetUuid == null) {
            return;
        }
        String mappingName = packet.getMappingName();
        if (mappingName == null || mappingName.isEmpty()) {
            return;
        }
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        if (mapping == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 附加实体映射不存在: {}", mappingName);
            return;
        }
        BedrockModelHandle handle = ModelHandleFactory.create(cacheManager, mapping);
        if (handle == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 附加实体模型构建失败: {}", mappingName);
            return;
        }
        applyMappingProperties(handle, mapping);
        if (packet.hasScale()) {
            handle.setModelScale(packet.getScale());
        }
        if (packet.hasAnimation()) {
            Animation clip = cacheManager.loadAnimation(mapping.getAnimation(), packet.getAnimation());
            SkyCoreMod.LOGGER.info("[SkyCore] 附加实体动画 mapping={} clip={} loaded={}",
                mapping.getAnimation(),
                packet.getAnimation(),
                clip != null);
            if (clip != null) {
                handle.setAnimation(clip);
                handle.restartAnimation();
            }
        }
        Vec3d offset = parseOffset(packet.getOffsetList());
        boolean followPos = !packet.hasFollowPosition() || packet.getFollowPosition();
        boolean followRot = !packet.hasFollowRotation() || packet.getFollowRotation();
        int lifetime = packet.hasLifetimeTicks() ? packet.getLifetimeTicks() : -1;
        float yawOffset = packet.hasYawOffset() ? packet.getYawOffset() : 0f;

        if (packet.getWorldSpace()) {
            spawnWorldAttachment(packet, targetUuid, handle, offset);
            return;
        }

        AttachmentInstance instance = new AttachmentInstance(
            packet.getAttachmentId(),
            targetUuid,
            handle,
            offset,
            followPos,
            followRot,
            yawOffset,
            lifetime
        );

        removeAttachment(targetUuid, instance.id);
        List<AttachmentInstance> list = attachments.computeIfAbsent(targetUuid,
            key -> Collections.synchronizedList(new ArrayList<>()));
        list.add(instance);
        SkyCoreMod.LOGGER.info("[SkyCore] 已创建附加实体 {} -> {} (followPos={}, followRot={}, lifetime={})",
            packet.getAttachmentId(), mappingName, followPos, followRot, lifetime);
    }

    public void removeAttachment(UUID targetUuid, String attachmentId) {
        if (targetUuid == null) {
            return;
        }
        List<AttachmentInstance> list = attachments.get(targetUuid);
        if (list == null) {
            return;
        }
        synchronized (list) {
            Iterator<AttachmentInstance> iterator = list.iterator();
            while (iterator.hasNext()) {
                AttachmentInstance instance = iterator.next();
                if (attachmentId == null || attachmentId.isEmpty() || attachmentId.equals(instance.id)) {
                    instance.dispose();
                    iterator.remove();
                }
            }
        }
        if (list.isEmpty()) {
            attachments.remove(targetUuid);
        }
        removeWorldAttachment(targetUuid, attachmentId);
    }

    private void removeWorldAttachment(UUID targetUuid, String attachmentId) {
        if (targetUuid == null) {
            return;
        }
        List<WorldAttachmentInstance> list = worldAttachments.get(targetUuid);
        if (list == null) {
            return;
        }
        synchronized (list) {
            Iterator<WorldAttachmentInstance> iterator = list.iterator();
            while (iterator.hasNext()) {
                WorldAttachmentInstance instance = iterator.next();
                if (attachmentId == null || attachmentId.isEmpty() || attachmentId.equals(instance.id)) {
                    instance.dispose();
                    iterator.remove();
                }
            }
        }
        if (list.isEmpty()) {
            worldAttachments.remove(targetUuid);
        }
    }

    private void tickWorldAttachments() {
        if (worldAttachments.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<UUID, List<WorldAttachmentInstance>>> it = worldAttachments.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, List<WorldAttachmentInstance>> entry = it.next();
            List<WorldAttachmentInstance> list = entry.getValue();
            synchronized (list) {
                Iterator<WorldAttachmentInstance> iter = list.iterator();
                while (iter.hasNext()) {
                    WorldAttachmentInstance instance = iter.next();
                    if (instance.tick()) {
                        instance.dispose();
                        iter.remove();
                    }
                }
            }
            if (list.isEmpty()) {
                it.remove();
            }
        }
    }

    void clear() {
        attachments.values().forEach(list -> {
            synchronized (list) {
                for (AttachmentInstance instance : list) {
                    instance.dispose();
                }
                list.clear();
            }
        });
        attachments.clear();
        worldAttachments.values().forEach(list -> {
            synchronized (list) {
                for (WorldAttachmentInstance instance : list) {
                    instance.dispose();
                }
                list.clear();
            }
        });
        worldAttachments.clear();
    }

    void tick() {
        if (!attachments.isEmpty()) {
            for (Iterator<Map.Entry<UUID, List<AttachmentInstance>>> it = attachments.entrySet().iterator(); it.hasNext();) {
                Map.Entry<UUID, List<AttachmentInstance>> entry = it.next();
                List<AttachmentInstance> list = entry.getValue();
                synchronized (list) {
                    Iterator<AttachmentInstance> iter = list.iterator();
                    while (iter.hasNext()) {
                        AttachmentInstance instance = iter.next();
                        if (instance.tick()) {
                            instance.dispose();
                            iter.remove();
                        }
                    }
                }
                if (list.isEmpty()) {
                    it.remove();
                }
            }
        }
        tickWorldAttachments();
    }

    public void renderAttachments(EntityLivingBase entity,
                                  double baseX, double baseY, double baseZ,
                                  float entityYaw, float partialTicks,
                                  AnimationEventDispatcher dispatcher,
                                  WeaponTrailRenderer trailRenderer) {
        if (entity == null) {
            return;
        }
        List<AttachmentInstance> list = attachments.get(entity.getUniqueID());
        if (list == null || list.isEmpty()) {
            return;
        }
        double cameraX = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double cameraY = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double cameraZ = Minecraft.getMinecraft().getRenderManager().viewerPosZ;

        synchronized (list) {
            for (AttachmentInstance instance : list) {
                instance.wrapper.updateAnimations();
                Vec3d rotatedOffset = instance.computeOffset(entityYaw);

                double renderX;
                double renderY;
                double renderZ;
                if (instance.followPosition) {
                    renderX = baseX + rotatedOffset.x;
                    renderY = baseY + rotatedOffset.y;
                    renderZ = baseZ + rotatedOffset.z;
                } else {
                    instance.ensureFixedPosition(entity, partialTicks, rotatedOffset);
                    renderX = instance.fixedX - cameraX;
                    renderY = instance.fixedY - cameraY;
                    renderZ = instance.fixedZ - cameraZ;
                }
                float yaw = instance.resolveYaw(entityYaw);
                instance.wrapper.render(entity, renderX, renderY, renderZ, yaw, partialTicks);
                if (dispatcher != null) {
                    dispatcher.dispatchAnimationEvents(entity, instance, null, instance.wrapper, partialTicks);
                }
                if (trailRenderer != null) {
                    instance.trailController.update(entity, instance.wrapper, partialTicks);
                    instance.trailController.forEachRenderable(trailRenderer::queueClip);
                }
            }
        }
    }

    public void renderWorldAttachments(float partialTicks,
                                       AnimationEventDispatcher dispatcher,
                                       WeaponTrailRenderer trailRenderer) {
        if (worldAttachments.isEmpty()) {
            return;
        }
        double cameraX = Minecraft.getMinecraft().getRenderManager().viewerPosX;
        double cameraY = Minecraft.getMinecraft().getRenderManager().viewerPosY;
        double cameraZ = Minecraft.getMinecraft().getRenderManager().viewerPosZ;

        for (Map.Entry<UUID, List<WorldAttachmentInstance>> entry : worldAttachments.entrySet()) {
            List<WorldAttachmentInstance> list = entry.getValue();
            if (list == null || list.isEmpty()) {
                continue;
            }
            synchronized (list) {
                for (WorldAttachmentInstance instance : list) {
                    instance.wrapper.updateAnimations();
                    double renderX = instance.posX - cameraX;
                    double renderY = instance.posY - cameraY;
                    double renderZ = instance.posZ - cameraZ;
                    instance.wrapper.setPackedLightFromWorld(instance.posX, instance.posY, instance.posZ);
                    instance.wrapper.renderBlock(renderX, renderY, renderZ, instance.yaw, partialTicks);
                    if (dispatcher != null) {
                        dispatcher.dispatchAnimationEvents(null, instance, instance, instance.wrapper, partialTicks);
                    }
                    if (trailRenderer != null) {
                        instance.trailController.update(null, instance.wrapper, partialTicks, instance.yaw);
                        instance.trailController.forEachRenderable(trailRenderer::queueClip);
                    }
                }
            }
        }
    }

    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    public boolean hasAttachments(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        List<AttachmentInstance> list = attachments.get(uuid);
        return list != null && !list.isEmpty();
    }

    public java.util.List<UUID> snapshotOwners() {
        return new java.util.ArrayList<>(attachments.keySet());
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Vec3d parseOffset(List<Float> values) {
        double x = values.size() > 0 ? values.get(0) : 0d;
        double y = values.size() > 1 ? values.get(1) : 0d;
        double z = values.size() > 2 ? values.get(2) : 0d;
        return new Vec3d(x, y, z);
    }

    private void applyMappingProperties(BedrockModelHandle handle, EntityModelMapping mapping) {
        handle.setPrimaryFadeDuration(mapping.getPrimaryFadeSeconds());
        handle.setEmissiveStrength(mapping.getEmissiveStrength());
        handle.setBloomStrength(mapping.getBloomStrength());
        handle.setBloomColor(mapping.getBloomColor());
        handle.setBloomPasses(mapping.getBloomPasses());
        handle.setBloomScaleStep(mapping.getBloomScaleStep());
        handle.setBloomDownscale(mapping.getBloomDownscale());
        handle.setBloomOffset(mapping.getBloomOffset());
        handle.setModelScale(mapping.getModelScale());
        handle.setModelOffset(mapping.getOffsetX(), mapping.getOffsetY(), mapping.getOffsetZ(), mapping.getOffsetMode());
        handle.setRenderHurtTint(mapping.isRenderHurtTint());
        handle.setHurtTint(mapping.getHurtTint());
        handle.setLightning(mapping.isLightning());
    }

    private void spawnWorldAttachment(SkyCoreProto.EntityAttachment packet,
                                      UUID targetUuid,
                                      BedrockModelHandle handle,
                                      Vec3d offset) {
        double baseX = packet.hasWorldX() ? packet.getWorldX() : 0d;
        double baseY = packet.hasWorldY() ? packet.getWorldY() : 0d;
        double baseZ = packet.hasWorldZ() ? packet.getWorldZ() : 0d;
        double posX = baseX + offset.x;
        double posY = baseY + offset.y;
        double posZ = baseZ + offset.z;
        float yaw = packet.hasWorldYaw() ? packet.getWorldYaw() : (packet.hasYawOffset() ? packet.getYawOffset() : 0f);
        int lifetime = packet.hasLifetimeTicks() ? packet.getLifetimeTicks() : -1;

        WorldAttachmentInstance instance = new WorldAttachmentInstance(
            packet.getAttachmentId(),
            targetUuid,
            handle,
            posX,
            posY,
            posZ,
            yaw,
            lifetime
        );
        removeWorldAttachment(targetUuid, instance.id);
        List<WorldAttachmentInstance> list = worldAttachments.computeIfAbsent(
            targetUuid,
            key -> Collections.synchronizedList(new ArrayList<>())
        );
        list.add(instance);
        SkyCoreMod.LOGGER.info("[SkyCore] 已创建世界附加实体 {} -> {} (pos={}, {}, {}, lifetime={})",
            packet.getAttachmentId(),
            packet.getMappingName(),
            posX,
            posY,
            posZ,
            lifetime);
    }

    private static final class AttachmentInstance implements AnimationEventContext {
        final String id;
        final UUID targetUuid;
        final BedrockModelHandle wrapper;
        final Vec3d offset;
        final boolean followPosition;
        final boolean followRotation;
        final float yawOffset;
        int remainingTicks;
        double fixedX;
        double fixedY;
        double fixedZ;
        boolean fixedPositionInitialized;
        float fixedYaw;
        boolean fixedYawInitialized;
        final AnimationEventState animationState = new AnimationEventState();
        final OverlayEventCursorCache overlayCursorCache = new OverlayEventCursorCache();
        final WeaponTrailController trailController = new WeaponTrailController();

        AttachmentInstance(String id,
                           UUID targetUuid,
                           BedrockModelHandle wrapper,
                           Vec3d offset,
                           boolean followPosition,
                           boolean followRotation,
                           float yawOffset,
                           int lifetimeTicks) {
            this.id = id;
            this.targetUuid = targetUuid;
            this.wrapper = wrapper;
            this.offset = offset;
            this.followPosition = followPosition;
            this.followRotation = followRotation;
            this.yawOffset = yawOffset;
            this.remainingTicks = lifetimeTicks <= 0 ? -1 : lifetimeTicks;
        }

        boolean tick() {
            if (remainingTicks < 0) {
                return false;
            }
            remainingTicks--;
            return remainingTicks <= 0;
        }

        Vec3d computeOffset(float entityYaw) {
            double yawRad = Math.toRadians(entityYaw);
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            double x = offset.x * cos - offset.z * sin;
            double z = offset.x * sin + offset.z * cos;
            return new Vec3d(x, offset.y, z);
        }

        void ensureFixedPosition(EntityLivingBase entity, float partialTicks, Vec3d rotatedOffset) {
            if (fixedPositionInitialized || entity == null) {
                return;
            }
            double interpolatedX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double interpolatedY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
            double interpolatedZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            this.fixedX = interpolatedX + rotatedOffset.x;
            this.fixedY = interpolatedY + rotatedOffset.y;
            this.fixedZ = interpolatedZ + rotatedOffset.z;
            this.fixedPositionInitialized = true;
        }

        float resolveYaw(float entityYaw) {
            if (followRotation) {
                return entityYaw + yawOffset;
            }
            if (!fixedYawInitialized) {
                fixedYaw = entityYaw + yawOffset;
                fixedYawInitialized = true;
            }
            return fixedYaw;
        }

        void dispose() {
            trailController.clear();
            wrapper.dispose();
        }

        @Override
        public AnimationEventState getPrimaryEventState() {
            return animationState;
        }

        @Override
        public List<org.mybad.minecraft.animation.EntityAnimationController.OverlayState> getOverlayStates() {
            return Collections.emptyList();
        }

        @Override
        public OverlayEventCursorCache getOverlayCursorCache() {
            return overlayCursorCache;
        }

        @Override
        public WeaponTrailController getTrailController() {
            return trailController;
        }
    }

    private static final class WorldAttachmentInstance implements AnimationEventContext, AnimationEventTarget {
        final String id;
        final UUID targetUuid;
        final BedrockModelHandle wrapper;
        final double posX;
        final double posY;
        final double posZ;
        final float yaw;
        int remainingTicks;
        final AnimationEventState animationState = new AnimationEventState();
        final OverlayEventCursorCache overlayCursorCache = new OverlayEventCursorCache();
        final WeaponTrailController trailController = new WeaponTrailController();

        WorldAttachmentInstance(String id,
                                UUID targetUuid,
                                BedrockModelHandle wrapper,
                                double posX,
                                double posY,
                                double posZ,
                                float yaw,
                                int lifetimeTicks) {
            this.id = id;
            this.targetUuid = targetUuid;
            this.wrapper = wrapper;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.yaw = yaw;
            this.remainingTicks = lifetimeTicks <= 0 ? -1 : lifetimeTicks;
        }

        boolean disposed;

        boolean tick() {
            if (remainingTicks < 0) {
                return false;
            }
            remainingTicks--;
            return remainingTicks <= 0;
        }

        void dispose() {
            trailController.clear();
            wrapper.dispose();
            disposed = true;
        }

        @Override
        public AnimationEventState getPrimaryEventState() {
            return animationState;
        }

        @Override
        public List<EntityAnimationController.OverlayState> getOverlayStates() {
            return Collections.emptyList();
        }

        @Override
        public OverlayEventCursorCache getOverlayCursorCache() {
            return overlayCursorCache;
        }

        @Override
        public WeaponTrailController getTrailController() {
            return trailController;
        }

        @Override
        public double getBaseX() {
            return posX;
        }

        @Override
        public double getBaseY() {
            return posY;
        }

        @Override
        public double getBaseZ() {
            return posZ;
        }

        @Override
        public float getHeadYaw() {
            return yaw;
        }

        @Override
        public float getBodyYaw() {
            return yaw;
        }

        @Override
        public boolean isEventTargetExpired() {
            return disposed;
        }
    }
}
