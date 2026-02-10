package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.render.trail.WeaponTrailRenderer;
import org.mybad.minecraft.resource.ResourceCacheManager;
import org.mybad.minecraft.render.world.WorldActorManager;
import org.mybad.skycoreproto.SkyCoreProto;

@SideOnly(Side.CLIENT)
public final class EntityRenderDispatcher {
    private final EntityWrapperCache wrapperCache;
    private final ForcedAnimationCache forcedAnimations;
    private final AnimationEventDispatcher eventDispatcher;
    private final EntityAttachmentManager attachmentManager;
    private final WorldActorManager worldActorManager;
    private final EntityAttributeOverrideStore overrideStore;
    private final EntityHeadBarManager headBarManager;
    private final EntityRenderPipeline renderPipeline;
    private final LingeringEntityManager lingeringManager;
    private long renderFrameCounter = 0L;
    private long currentRenderFrameId = 0L;
    private boolean renderFrameActive = false;

    public EntityRenderDispatcher(ResourceCacheManager cacheManager, WeaponTrailRenderer trailRenderer) {
        this.wrapperCache = new EntityWrapperCache(cacheManager);
        this.forcedAnimations = new ForcedAnimationCache();
        this.eventDispatcher = new AnimationEventDispatcher();
        this.attachmentManager = new EntityAttachmentManager(cacheManager);
        this.worldActorManager = new WorldActorManager(cacheManager);
        this.headBarManager = new EntityHeadBarManager();
        this.overrideStore = new EntityAttributeOverrideStore();
        this.lingeringManager = new LingeringEntityManager();
        this.renderPipeline = new EntityRenderPipeline(
            eventDispatcher,
            this::handleForcedAnimationFrame,
            trailRenderer,
            attachmentManager,
            headBarManager
        );
    }

    public void onRenderLivingPre(RenderLivingEvent.Pre<?> event) {
        EntityLivingBase entity = event.getEntity();
        if (entity == null) {
            return;
        }

        EntityMappingResolver.MappingResult mappingResult = EntityMappingResolver.resolve(entity);
        if (mappingResult == null) {
            return;
        }
        String mappingName = mappingResult.mappingName;

        EntityWrapperEntry entry = wrapperCache.getOrCreate(entity, mappingName, mappingResult.mapping);
        if (entry == null || entry.wrapper == null) {
            return;
        }
        event.setCanceled(true);

        long tick = entity.world != null ? entity.world.getTotalWorldTime() : Long.MIN_VALUE;
        if (tick != Long.MIN_VALUE && entry.lastAnimationTick != tick) {
            tickEntry(entity, entry, tick);
        }

        applyOverridesIfNeeded(entity, entry);
        renderPipeline.render(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
        entry.lastRenderFrameId = currentRenderFrameId;
    }

    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.isGamePaused()) {
            return;
        }
        for (Entity entity : mc.world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isDead) {
                continue;
            }
            EntityMappingResolver.MappingResult mappingResult = EntityMappingResolver.resolve(living);
            if (mappingResult == null) {
                continue;
            }
            wrapperCache.getOrCreate(living, mappingResult.mappingName, mappingResult.mapping);
        }
        wrapperCache.forEach((entity, entry) -> {
            if (entity == null || entry == null) {
                return;
            }
            long tick = entity.world != null ? entity.world.getTotalWorldTime() : Long.MIN_VALUE;
            if (tick != Long.MIN_VALUE) {
                tickEntry(entity, entry, tick);
            }
        });
        attachmentManager.tick();
        worldActorManager.tick();
        lingeringManager.tick();
    }

    public boolean isSkyCoreEntity(EntityLivingBase entity) {
        return EntityMappingResolver.resolve(entity) != null;
    }

    public boolean isSkyCoreEntity(net.minecraft.entity.Entity entity) {
        if (!(entity instanceof EntityLivingBase)) {
            return false;
        }
        return isSkyCoreEntity((EntityLivingBase) entity);
    }

    public void clearCache() {
        wrapperCache.clear();
        clearAllForcedAnimations();
        attachmentManager.clear();
        worldActorManager.clear();
        lingeringManager.clear();
        headBarManager.reload();
        overrideStore.clearAll();
    }

    public void invalidateWrapper(String entityName) {
        wrapperCache.invalidateByName(entityName);
    }

    public void cleanupEntityWrappers() {
        wrapperCache.cleanupDead(lingeringManager);
    }

    public boolean setForcedAnimation(java.util.UUID entityUuid, Animation animation) {
        return forcedAnimations.set(entityUuid, animation, wrapperCache.entries());
    }

    public void clearForcedAnimation(java.util.UUID entityUuid) {
        forcedAnimations.clear(entityUuid);
    }

    public void clearAllForcedAnimations() {
        forcedAnimations.clearAll();
    }

    public EntityAttachmentManager getAttachmentManager() {
        return attachmentManager;
    }

    public WorldActorManager getWorldActorManager() {
        return worldActorManager;
    }

    public EntityHeadBarManager getHeadBarManager() {
        return headBarManager;
    }

    public AnimationEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public LingeringEntityManager getLingeringManager() {
        return lingeringManager;
    }

    public void renderLingeringEntities(float partialTicks) {
        lingeringManager.render(partialTicks, eventDispatcher);
    }

    public String findMappingByUuid(java.util.UUID uuid) {
        return wrapperCache.findMappingNameByUuid(uuid);
    }

    public void forEachWrapper(java.util.function.BiConsumer<EntityLivingBase, EntityWrapperEntry> consumer) {
        if (consumer == null) {
            return;
        }
        wrapperCache.forEach(consumer);
    }

    public void applyAttributeOverrides(java.util.List<SkyCoreProto.EntityAttributeOverride> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }
        overrides.forEach(overrideStore::applyOverride);
    }

    public void beginRenderFrame() {
        if (renderFrameActive) {
            return;
        }
        renderFrameActive = true;
        currentRenderFrameId = ++renderFrameCounter;
    }

    public void finishRenderFrame() {
        if (!renderFrameActive) {
            return;
        }
        renderFrameActive = false;
        final long frameId = currentRenderFrameId;
        wrapperCache.forEach((entity, entry) -> {
            if (entry == null || entry.wrapper == null) {
                return;
            }
            if (entry.lastRenderFrameId != frameId) {
                entry.wrapper.updateAnimations();
                entry.lastRenderFrameId = frameId;
            }
        });
    }

    private void handleForcedAnimationFrame(EntityLivingBase entity, EntityWrapperEntry entry) {
        releaseForcedAnimation(entity, entry);
    }

    private void tickEntry(EntityLivingBase entity, EntityWrapperEntry entry, long currentTick) {
        if (entry == null || entry.wrapper == null) {
            return;
        }
        if (entry.lastAnimationTick == currentTick) {
            return;
        }
        if (releaseForcedAnimation(entity, entry)) {
            entry.lastAnimationTick = currentTick;
            return;
        }
        java.util.UUID uuid = entity.getUniqueID();
        Animation forced = forcedAnimations.get(uuid);
        entry.overlayStates = AnimationStateApplier.apply(entity, entry, forced);
        entry.lastAnimationTick = currentTick;
    }

    private boolean releaseForcedAnimation(EntityLivingBase entity, EntityWrapperEntry entry) {
        if (entity == null || entry == null || entry.wrapper == null) {
            return false;
        }
        java.util.UUID uuid = entity.getUniqueID();
        Animation forced = forcedAnimations.get(uuid);
        if (forced == null) {
            return false;
        }
        if (forced.getLoopMode() == Animation.LoopMode.HOLD_ON_LAST_FRAME) {
            return false;
        }
        AnimationPlayer player = entry.wrapper.getActiveAnimationPlayer();
        if (player != null && !player.isFinished()) {
            return false;
        }
        forcedAnimations.clear(uuid);
        if (entry.controller != null) {
            entry.controller.forcePrimaryRefresh();
        }
        entry.overlayStates = AnimationStateApplier.apply(entity, entry, null);
        entry.wrapper.updateAnimations();
        if (entity.world != null) {
            entry.lastAnimationTick = entity.world.getTotalWorldTime();
        }
        return true;
    }

    private void applyOverridesIfNeeded(EntityLivingBase entity, EntityWrapperEntry entry) {
        if (entity == null || entry == null || entry.wrapper == null) {
            return;
        }
        java.util.UUID uuid = entity.getUniqueID();
        if (uuid == null) {
            return;
        }
        long targetVersion = overrideStore.getVersion(uuid);
        if (targetVersion == entry.appliedOverrideVersion) {
            return;
        }
        if (targetVersion == Long.MIN_VALUE) {
            EntityModelMapping mapping = entry.mapping;
            if (mapping != null) {
                applyOverride(entry, null);
                entry.appliedOverrideVersion = targetVersion;
            }
            return;
        }
        EntityAttributeOverride override = overrideStore.get(uuid);
        applyOverride(entry, override);
        entry.appliedOverrideVersion = targetVersion;
    }

    private void applyOverride(EntityWrapperEntry entry, EntityAttributeOverride override) {
        EntityModelMapping mapping = entry.mapping;
        BedrockModelHandle handle = entry.wrapper;
        if (mapping == null || handle == null) {
            return;
        }
        float scale = override != null && override.scale != null ? override.scale : mapping.getModelScale();
        handle.setModelScale(scale);
        handle.setModelOffset(mapping.getOffsetX(), mapping.getOffsetY(), mapping.getOffsetZ(), mapping.getOffsetMode());

        float emissive = override != null && override.emissiveStrength != null ? override.emissiveStrength : mapping.getEmissiveStrength();
        handle.setEmissiveStrength(emissive);

        float bloomStrength = override != null && override.bloomStrength != null ? override.bloomStrength : mapping.getBloomStrength();
        handle.setBloomStrength(bloomStrength);

        float fadeSeconds = override != null && override.primaryFadeSeconds != null ? override.primaryFadeSeconds : mapping.getPrimaryFadeSeconds();
        handle.setPrimaryFadeDuration(fadeSeconds);

        float[] bloomOffset = override != null && override.bloomOffset != null ? copyBloomOffset(override.bloomOffset) : mapping.getBloomOffset();
        handle.setBloomOffset(bloomOffset);

        int bloomPasses = override != null && override.bloomPasses != null ? override.bloomPasses : mapping.getBloomPasses();
        handle.setBloomPasses(bloomPasses);

        float bloomScaleStep = override != null && override.bloomScaleStep != null ? override.bloomScaleStep : mapping.getBloomScaleStep();
        handle.setBloomScaleStep(bloomScaleStep);

        float bloomDownscale = override != null && override.bloomDownscale != null ? override.bloomDownscale : mapping.getBloomDownscale();
        handle.setBloomDownscale(bloomDownscale);
    }

    private float[] copyBloomOffset(float[] source) {
        if (source == null || source.length < 3) {
            return null;
        }
        return new float[]{source[0], source[1], source[2]};
    }

}
