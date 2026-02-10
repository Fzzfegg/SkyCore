package org.mybad.minecraft.render.world;

import net.minecraft.client.Minecraft;
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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages standalone world actors that are not bound to real entities.
 */
public final class WorldActorManager {

    private final ResourceCacheManager cacheManager;
    private final Map<String, WorldActorEntry> actors = new ConcurrentHashMap<>();

    public WorldActorManager(ResourceCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void spawnActor(SkyCoreProto.WorldActorCommand packet) {
        if (packet == null || packet.getId().isEmpty()) {
            return;
        }
        String mappingName = packet.getMappingName();
        if (mappingName == null || mappingName.isEmpty()) {
            return;
        }
        EntityModelMapping mapping = SkyCoreConfig.getInstance().getMapping(mappingName);
        if (mapping == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 世界实体映射不存在: {}", mappingName);
            return;
        }
        BedrockModelHandle handle = ModelHandleFactory.create(cacheManager, mapping);
        if (handle == null) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 世界实体模型构建失败: {}", mappingName);
            return;
        }
        applyMappingProperties(handle, mapping);
        if (packet.hasScale()) {
            handle.setModelScale(packet.getScale());
        }
        if (packet.hasAnimation() && mapping.getAnimation() != null && !mapping.getAnimation().isEmpty()) {
            Animation clip = cacheManager.loadAnimation(mapping.getAnimation(), packet.getAnimation());
            SkyCoreMod.LOGGER.info("[SkyCore] 世界实体动画 mapping={} clip={} loaded={}",
                mapping.getAnimation(),
                packet.getAnimation(),
                clip != null);
            if (clip != null) {
                handle.setAnimation(clip);
                handle.restartAnimation();
            }
        }
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float yaw = packet.getYaw();
        int lifetime = packet.hasLifetimeTicks() ? packet.getLifetimeTicks() : -1;
        WorldActorEntry entry = new WorldActorEntry(packet.getId(), handle, x, y, z, yaw, lifetime);
        WorldActorEntry previous = actors.put(packet.getId(), entry);
        if (previous != null) {
            previous.dispose();
        }
        SkyCoreMod.LOGGER.info("[SkyCore] 已创建世界实体 {} -> {} (pos={}, {}, {}, lifetime={})",
            packet.getId(),
            mappingName,
            x,
            y,
            z,
            lifetime);
    }

    public void removeActor(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        WorldActorEntry entry = actors.remove(id);
        if (entry != null) {
            entry.dispose();
        }
    }

    public void clear() {
        actors.values().forEach(WorldActorEntry::dispose);
        actors.clear();
    }

    public void tick() {
        if (actors.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, WorldActorEntry>> iterator = actors.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WorldActorEntry> entry = iterator.next();
            if (entry.getValue().tick()) {
                entry.getValue().dispose();
                iterator.remove();
            }
        }
    }

    public void render(float partialTicks,
                       AnimationEventDispatcher dispatcher,
                       WeaponTrailRenderer trailRenderer) {
        if (actors.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getRenderManager() == null) {
            return;
        }
        double cameraX = mc.getRenderManager().viewerPosX;
        double cameraY = mc.getRenderManager().viewerPosY;
        double cameraZ = mc.getRenderManager().viewerPosZ;

        for (WorldActorEntry entry : actors.values()) {
            entry.wrapper.updateAnimations();
            double renderX = entry.posX - cameraX;
            double renderY = entry.posY - cameraY;
            double renderZ = entry.posZ - cameraZ;
            entry.wrapper.renderBlock(renderX, renderY, renderZ, entry.yaw, partialTicks);
            if (dispatcher != null) {
                dispatcher.dispatchAnimationEvents(null, entry, entry, entry.wrapper, partialTicks);
            }
            if (trailRenderer != null) {
                entry.trailController.update(null, entry.wrapper, partialTicks, entry.yaw);
                entry.trailController.forEachRenderable(trailRenderer::queueClip);
            }
        }
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
    }

    private static final class WorldActorEntry implements AnimationEventContext, AnimationEventTarget {
        final String id;
        final BedrockModelHandle wrapper;
        final double posX;
        final double posY;
        final double posZ;
        final float yaw;
        int remainingTicks;
        final AnimationEventState animationState = new AnimationEventState();
        final OverlayEventCursorCache overlayCursorCache = new OverlayEventCursorCache();
        final WeaponTrailController trailController = new WeaponTrailController();
        boolean disposed;

        WorldActorEntry(String id,
                        BedrockModelHandle wrapper,
                        double posX,
                        double posY,
                        double posZ,
                        float yaw,
                        int lifetimeTicks) {
            this.id = id;
            this.wrapper = wrapper;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.yaw = yaw;
            this.remainingTicks = lifetimeTicks <= 0 ? -1 : lifetimeTicks;
        }

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
        public java.util.List<EntityAnimationController.OverlayState> getOverlayStates() {
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
