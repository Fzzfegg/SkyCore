package org.mybad.minecraft.event;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.minecraft.render.BedrockModelWrapper;

final class EntityRenderPipeline {
    private final AnimationEventDispatcher eventDispatcher;

    EntityRenderPipeline(AnimationEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    void render(EntityLivingBase entity, WrapperEntry entry,
                double x, double y, double z, float partialTicks) {
        if (entry == null || entry.wrapper == null) {
            return;
        }
        BedrockModelWrapper wrapper = entry.wrapper;
        float entityYaw = AnimationEventMath.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
        eventDispatcher.dispatchAnimationEvents(entity, entry, wrapper, partialTicks);

        if (NameTagRenderer.shouldRenderNameTag(entity)) {
            NameTagRenderer.render(entity, x, y, z);
        }
    }
}
