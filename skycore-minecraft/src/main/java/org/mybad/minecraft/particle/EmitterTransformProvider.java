package org.mybad.minecraft.particle;

/**
 * Supplies emitter transform values each tick.
 */
public interface EmitterTransformProvider {
    void fill(EmitterTransform transform, float deltaSeconds);

    default boolean isLocatorBound() {
        return false;
    }
}
