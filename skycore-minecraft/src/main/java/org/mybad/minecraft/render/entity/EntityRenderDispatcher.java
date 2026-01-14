package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.resource.ResourceCacheManager;

@SideOnly(Side.CLIENT)
public final class EntityRenderDispatcher {
    private final EntityWrapperCache wrapperCache;
    private final ForcedAnimationCache forcedAnimations;
    private final AnimationEventDispatcher eventDispatcher;
    private final EntityRenderPipeline renderPipeline;

    public EntityRenderDispatcher(ResourceCacheManager cacheManager) {
        this.wrapperCache = new EntityWrapperCache(cacheManager);
        this.forcedAnimations = new ForcedAnimationCache();
        this.eventDispatcher = new AnimationEventDispatcher();
        this.renderPipeline = new EntityRenderPipeline(eventDispatcher);
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

        event.setCanceled(true);

        EntityWrapperEntry entry = wrapperCache.getOrCreate(entity, mappingName, mappingResult.mapping);
        if (entry == null || entry.wrapper == null) {
            return;
        }

        long tick = entity.world != null ? entity.world.getTotalWorldTime() : Long.MIN_VALUE;
        if (tick != Long.MIN_VALUE && entry.lastAnimationTick != tick) {
            tickEntry(entity, entry, tick);
        }

        renderPipeline.render(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
    }

    public void onClientTick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null || mc.isGamePaused()) {
            return;
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
    }

    public void invalidateWrapper(String entityName) {
        wrapperCache.invalidateByName(entityName);
    }

    public void cleanupEntityWrappers() {
        wrapperCache.cleanupDead();
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

    public String findMappingByUuid(java.util.UUID uuid) {
        return wrapperCache.findMappingNameByUuid(uuid);
    }

    public void forEachWrapper(java.util.function.BiConsumer<EntityLivingBase, EntityWrapperEntry> consumer) {
        if (consumer == null) {
            return;
        }
        wrapperCache.forEach(consumer);
    }

    private void tickEntry(EntityLivingBase entity, EntityWrapperEntry entry, long currentTick) {
        if (entry == null || entry.wrapper == null) {
            return;
        }
        if (entry.lastAnimationTick == currentTick) {
            return;
        }
        Animation forced = forcedAnimations.get(entity.getUniqueID());
        entry.overlayStates = AnimationStateApplier.apply(entity, entry, forced);
        if (forced != null) {
            AnimationPlayer player = entry.wrapper.getActiveAnimationPlayer();
            if (player == null || player.isFinished()) {
                forcedAnimations.clear(entity.getUniqueID());
            }
        }
        entry.wrapper.updateAnimations();
        entry.lastAnimationTick = currentTick;
    }

}
