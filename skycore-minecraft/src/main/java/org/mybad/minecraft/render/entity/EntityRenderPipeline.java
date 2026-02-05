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
    private final EntityAttachmentManager attachmentManager;
    private final EntityHeadBarManager headBarManager;

    EntityRenderPipeline(AnimationEventDispatcher eventDispatcher,
                         BiConsumer<EntityLivingBase, EntityWrapperEntry> preRenderCallback,
                         WeaponTrailRenderer trailRenderer,
                         EntityAttachmentManager attachmentManager,
                         EntityHeadBarManager headBarManager) {
        this.eventDispatcher = eventDispatcher;
        this.preRenderCallback = preRenderCallback;
        this.trailRenderer = trailRenderer;
        this.attachmentManager = attachmentManager;
        this.headBarManager = headBarManager;
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
        updateLastKnownPose(entity, entry, partialTicks, entityYaw);
        entry.lastPackedLight = entity.getBrightnessForRender();

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

        if (attachmentManager != null) {
            attachmentManager.renderAttachments(entity, x, y, z, entityYaw, partialTicks, eventDispatcher, trailRenderer);
        }
        if (headBarManager != null) {
            headBarManager.queueHeadBar(entity, entry, x, y, z, partialTicks);
        }
    }

    private void updateLastKnownPose(EntityLivingBase entity,
                                     EntityWrapperEntry entry,
                                     float partialTicks,
                                     float headYaw) {
        if (entity == null || entry == null) {
            return;
        }
        double interpX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double interpY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double interpZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        entry.lastWorldX = interpX;
        entry.lastWorldY = interpY;
        entry.lastWorldZ = interpZ;
        float bodyYaw = AnimationEventMathUtil.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
        entry.lastBodyYaw = Float.isNaN(bodyYaw) ? headYaw : bodyYaw;
    }
}
