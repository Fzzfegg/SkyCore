package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.EntityNameTagRenderer;
import org.mybad.minecraft.render.entity.events.AnimationEventDispatcher;
import org.mybad.minecraft.render.entity.events.AnimationEventMathUtil;
import org.mybad.minecraft.render.trail.WeaponTrailRenderer;

import java.util.function.BiConsumer;

final class EntityRenderPipeline {
    private final AnimationEventDispatcher eventDispatcher;
    private final BiConsumer<EntityLivingBase, EntityWrapperEntry> preRenderCallback;
    private final WeaponTrailRenderer trailRenderer;

    EntityRenderPipeline(AnimationEventDispatcher eventDispatcher,
                         BiConsumer<EntityLivingBase, EntityWrapperEntry> preRenderCallback,
                         WeaponTrailRenderer trailRenderer) {
        this.eventDispatcher = eventDispatcher;
        this.preRenderCallback = preRenderCallback;
        this.trailRenderer = trailRenderer;
    }

    void render(EntityLivingBase entity, EntityWrapperEntry entry,
                double x, double y, double z, float partialTicks) {
        if (entry == null || entry.wrapper == null) {
            return;
        }
        BedrockModelHandle wrapper = entry.wrapper;
        // 每一帧渲染前推进动画，保持动画刷新率与渲染帧率一致
        wrapper.updateAnimations();
        if (preRenderCallback != null) {
            preRenderCallback.accept(entity, entry);
        }
        float entityYaw = AnimationEventMathUtil.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);

        wrapper.render(entity, x, y, z, entityYaw, partialTicks);
        eventDispatcher.dispatchAnimationEvents(entity, entry, null, wrapper, partialTicks);
        if (entry.trailController != null) {
            entry.trailController.update(entity, wrapper, partialTicks);
            if (trailRenderer != null) {
                entry.trailController.forEachRenderable(trailRenderer::queueClip);
            }
        }

        if (EntityNameTagRenderer.shouldRenderNameTag(entity)) {
            EntityNameTagRenderer.render(entity, x, y, z);
        }
    }
}
