package org.mybad.minecraft.render.entity.events;

import net.minecraft.entity.EntityLivingBase;
import org.mybad.core.animation.Animation;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelHandle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OverlayEventDispatcher {
    private static final float EVENT_EPS = 1.0e-4f;

    void dispatch(EntityLivingBase entity, AnimationEventContext context, AnimationEventTarget target, BedrockModelHandle wrapper,
                  float partialTicks, AnimationEventDispatcher dispatcher) {
        if (context == null) {
            return;
        }
        List<EntityAnimationController.OverlayState> overlayStates = context.getOverlayStates();
        OverlayEventCursorCache cursorCache = context.getOverlayCursorCache();
        if (overlayStates == null || overlayStates.isEmpty()) {
            cursorCache.clear();
            return;
        }
        Set<Animation> active = new HashSet<>();
        for (EntityAnimationController.OverlayState state : overlayStates) {
            if (state == null || state.animation == null) {
                continue;
            }
            Animation animation = state.animation;
            active.add(animation);
            OverlayEventCursor cursor = cursorCache.getOrCreate(animation);
            float currentTime = state.time;
            if (!cursor.valid) {
                if (currentTime >= -EVENT_EPS) {
                    dispatcher.dispatchEventsForAnimation(entity, context, target, wrapper, animation, 0f, currentTime, false, partialTicks);
                }
                cursor.lastTime = currentTime;
                cursor.lastLoop = 0;
                cursor.valid = true;
                continue;
            }
            boolean looped = animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < cursor.lastTime;
            dispatcher.dispatchEventsForAnimation(entity, context, target, wrapper, animation, cursor.lastTime, currentTime, looped, partialTicks);
            cursor.lastTime = currentTime;
        }
        cursorCache.prune(active);
    }
}
