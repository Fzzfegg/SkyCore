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
    }

    void tick() {
        if (attachments.isEmpty()) {
            return;
        }
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
        handle.setRenderHurtTint(mapping.isRenderHurtTint());
        handle.setHurtTint(mapping.getHurtTint());
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
}
