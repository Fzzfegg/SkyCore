package org.mybad.minecraft.event;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.render.BedrockModelWrapper;
import org.mybad.minecraft.resource.ResourceLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SideOnly(Side.CLIENT)
final class EntityRenderDispatcher {
    private final EntityWrapperCache wrapperCache;
    private final ForcedAnimationRegistry forcedAnimations;
    private final AnimationEventDispatcher eventDispatcher;

    EntityRenderDispatcher(ResourceLoader resourceLoader) {
        this.wrapperCache = new EntityWrapperCache(resourceLoader);
        this.forcedAnimations = new ForcedAnimationRegistry();
        this.eventDispatcher = new AnimationEventDispatcher();
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

        renderEntity(entity, entry, event.getX(), event.getY(), event.getZ(), event.getPartialRenderTick());
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

    private void renderEntity(EntityLivingBase entity, WrapperEntry entry,
                              double x, double y, double z, float partialTicks) {
        BedrockModelWrapper wrapper = entry.wrapper;
        float entityYaw = AnimationEventMath.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
        eventDispatcher.dispatchAnimationEvents(entity, entry, wrapper, partialTicks);

        if (NameTagRenderer.shouldRenderNameTag(entity)) {
            NameTagRenderer.render(entity, x, y, z);
        }
    }

    static final class EventCursor {
        float lastTime;
        int lastLoop;
        boolean valid;
    }

    static final class WrapperEntry {
        final BedrockModelWrapper wrapper;
        final EntityAnimationController controller;
        final UUID entityUuid;
        final String mappingName;
        long lastSeenTick;
        List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
        final Map<Animation, EventCursor> overlayCursors = new HashMap<>();
        Animation lastPrimaryAnimation;
        float lastPrimaryTime;
        int lastPrimaryLoop;
        boolean primaryValid;

        WrapperEntry(BedrockModelWrapper wrapper, EntityAnimationController controller, UUID entityUuid, String mappingName, long lastSeenTick) {
            this.wrapper = wrapper;
            this.controller = controller;
            this.entityUuid = entityUuid;
            this.mappingName = mappingName;
            this.lastSeenTick = lastSeenTick;
        }
    }
}
