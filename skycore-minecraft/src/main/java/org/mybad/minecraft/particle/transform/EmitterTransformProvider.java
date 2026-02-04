package org.mybad.minecraft.particle.transform;

/**
 * Supplies emitter transform values each tick.
 */
public interface EmitterTransformProvider {
    void fill(EmitterTransform transform, float deltaSeconds);

    default boolean isLocatorBound() {
        return false;
    }

    default boolean shouldExpireEmitter() {
        return false;
    }
}
