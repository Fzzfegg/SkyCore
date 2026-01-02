package org.mybad.minecraft.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.resource.ResourceLoader;


@SideOnly(Side.CLIENT)
final class EntityRenderDispatcher {
    private final EntityWrapperCache wrapperCache;
    private final ForcedAnimationRegistry forcedAnimations;
    private final AnimationEventDispatcher eventDispatcher;
    private final EntityRenderPipeline renderPipeline;

    EntityRenderDispatcher(ResourceLoader resourceLoader) {
        this.wrapperCache = new EntityWrapperCache(resourceLoader);
        this.forcedAnimations = new ForcedAnimationRegistry();
        this.eventDispatcher = new AnimationEventDispatcher();
        this.renderPipeline = new EntityRenderPipeline(eventDispatcher);
    }

    void onRenderLivingPre(RenderLivingEvent.Pre<?> event) {
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

        WrapperEntry entry = wrapperCache.getOrCreate(entity, mappingName, mappingResult.mapping);
        if (entry == null || entry.wrapper == null) {
            return;
        }

        Animation forced = forcedAnimations.get(mappingName);
        entry.overlayStates = AnimationStateApplier.apply(entity, entry, forced);

        renderPipeline.render(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
    }

    void clearCache() {
        wrapperCache.clear();
        clearAllForcedAnimations();
    }

    void invalidateWrapper(String entityName) {
        wrapperCache.invalidateByName(entityName);
    }

    void cleanupEntityWrappers() {
        wrapperCache.cleanupDead();
    }

    boolean setForcedAnimation(String mappingName, Animation animation) {
        return forcedAnimations.set(mappingName, animation, wrapperCache.entries());
    }

    void clearForcedAnimation(String mappingName) {
        forcedAnimations.clear(mappingName);
    }

    void clearAllForcedAnimations() {
        forcedAnimations.clearAll();
    }


}
