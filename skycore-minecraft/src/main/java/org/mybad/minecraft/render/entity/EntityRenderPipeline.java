package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.EntityNameTagRenderer;

final class EntityRenderPipeline {
    private final AnimationEventDispatcher eventDispatcher;

    EntityRenderPipeline(AnimationEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    void render(EntityLivingBase entity, EntityWrapperEntry entry,
                double x, double y, double z, float partialTicks) {
        if (entry == null || entry.wrapper == null) {
            return;
        }
        BedrockModelHandle wrapper = entry.wrapper;
        float entityYaw = AnimationEventMath.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
        eventDispatcher.dispatchAnimationEvents(entity, entry, wrapper, partialTicks);

        if (EntityNameTagRenderer.shouldRenderNameTag(entity)) {
            EntityNameTagRenderer.render(entity, x, y, z);
        }
    }
}
