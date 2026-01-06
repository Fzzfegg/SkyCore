package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
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

        Animation forced = forcedAnimations.get(mappingName);
        entry.overlayStates = AnimationStateApplier.apply(entity, entry, forced);

        renderPipeline.render(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
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

    public boolean setForcedAnimation(String mappingName, Animation animation) {
        return forcedAnimations.set(mappingName, animation, wrapperCache.entries());
    }

    public void clearForcedAnimation(String mappingName) {
        forcedAnimations.clear(mappingName);
    }

    public void clearAllForcedAnimations() {
        forcedAnimations.clearAll();
    }


}
