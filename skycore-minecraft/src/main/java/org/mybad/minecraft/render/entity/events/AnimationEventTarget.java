package org.mybad.minecraft.render.entity.events;

/**
 * Provides positional context for animation events when there is no backing entity.
 */
public interface AnimationEventTarget {
    double getBaseX();

    double getBaseY();

    double getBaseZ();

    float getHeadYaw();

    float getBodyYaw();
}
