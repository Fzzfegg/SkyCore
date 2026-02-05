package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.animation.EntityAnimationController;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class AnimationStateApplier {
    private AnimationStateApplier() {
    }

    static List<EntityAnimationController.OverlayState> apply(EntityLivingBase entity,
                                                              EntityWrapperEntry entry,
                                                              Animation forced) {
        if (entry == null || entry.wrapper == null) {
            return Collections.emptyList();
        }
        List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
        if (forced != null) {
            entry.wrapper.setAnimation(forced);
            entry.wrapper.clearOverlayStates();
            return overlayStates;
        }
        if (entry.controller != null) {
            EntityAnimationController.Frame frame = entry.controller.update(entity);
            if (frame != null) {
                boolean override = false;
                if (frame.primary != null) {
                    entry.wrapper.setAnimation(frame.primary);
                    override = frame.primary.isOverridePreviousAnimation();
                    if (override) {
                        entry.wrapper.clearOverlayStates();
                    }
                }
                if (!override) {
                    entry.wrapper.setOverlayStates(frame.overlays);
                    overlayStates = frame.overlays != null ? frame.overlays : Collections.emptyList();
                }
            } else {
                entry.wrapper.clearOverlayStates();
            }
        } else {
            entry.wrapper.clearOverlayStates();
        }
        updateLingeringState(entity, entry);
        return overlayStates;
    }

    private static void updateLingeringState(EntityLivingBase entity, EntityWrapperEntry entry) {
        if (entity == null || entry == null) {
            return;
        }
        boolean isDead = entity.getHealth() <= 0f || entity.isDead || entity.deathTime > 0;
        if (!isDead) {
            entry.lastKnownDead = false;
            return;
        }
        boolean flagged = false;
        if (entry.controller != null) {
            String currentAction = entry.controller.getCurrentAction();
            if (currentAction != null) {
                String normalized = currentAction.toLowerCase(Locale.ROOT);
                if (normalized.contains("death") || normalized.contains("dying")) {
                    flagged = true;
                }
            }
        }
        if (!flagged && entry.wrapper != null) {
            org.mybad.core.animation.AnimationPlayer player = entry.wrapper.getActiveAnimationPlayer();
            if (player != null) {
                org.mybad.core.animation.Animation animation = player.getAnimation();
                if (animation != null && animation.getName() != null) {
                    String animName = animation.getName().toLowerCase(Locale.ROOT);
                    flagged = animName.contains("death") || animName.contains("dying");
                }
            }
        }
        entry.lastKnownDead = flagged;
    }
}
