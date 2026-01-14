package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.animation.EntityAnimationController;

import java.util.Collections;
import java.util.List;

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
                    entry.setLastPrimaryAnimation(frame.primary);
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
        return overlayStates;
    }
}
