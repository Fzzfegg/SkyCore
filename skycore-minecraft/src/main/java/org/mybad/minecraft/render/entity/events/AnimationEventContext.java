package org.mybad.minecraft.render.entity.events;

import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.trail.WeaponTrailController;

import java.util.List;

/**
 * Shared view of animation events state so multiple renderers can dispatch particles/sounds.
 */
public interface AnimationEventContext {
    AnimationEventState getPrimaryEventState();

    List<EntityAnimationController.OverlayState> getOverlayStates();

    OverlayEventCursorCache getOverlayCursorCache();

    default WeaponTrailController getTrailController() {
        return null;
    }
}
