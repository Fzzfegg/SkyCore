package org.mybad.minecraft.render.entity;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelHandle;

import java.util.HashSet;
import java.util.Set;

final class OverlayEventDispatcher {
    private static final float EVENT_EPS = 1.0e-4f;

    void dispatch(EntityLivingBase entity, EntityWrapperEntry entry, BedrockModelHandle wrapper,
                  float partialTicks, AnimationEventDispatcher dispatcher) {
        if (entry.overlayStates == null || entry.overlayStates.isEmpty()) {
            entry.overlayCursors.clear();
            return;
        }
        Set<Animation> active = new HashSet<>();
        for (EntityAnimationController.OverlayState state : entry.overlayStates) {
            if (state == null || state.animation == null) {
                continue;
            }
            Animation animation = state.animation;
            active.add(animation);
            OverlayEventCursor cursor = entry.overlayCursors.getOrCreate(animation);
            float currentTime = state.time;
            if (!cursor.valid) {
                cursor.lastTime = currentTime;
                cursor.lastLoop = 0;
                cursor.valid = true;
                continue;
            }
            boolean looped = animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < cursor.lastTime;
            dispatcher.dispatchEventsForAnimation(entity, wrapper, animation, cursor.lastTime, currentTime, looped, partialTicks);
            cursor.lastTime = currentTime;
        }
        entry.overlayCursors.prune(active);
    }
}
