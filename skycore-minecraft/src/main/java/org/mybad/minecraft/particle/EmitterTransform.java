package org.mybad.minecraft.particle;

/**
 * Mutable emitter transform snapshot.
 */
public final class EmitterTransform {
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float scale = 1.0f;
    public final float[] basisX = new float[]{1.0f, 0.0f, 0.0f};
    public final float[] basisY = new float[]{0.0f, 1.0f, 0.0f};
    public final float[] basisZ = new float[]{0.0f, 0.0f, 1.0f};
}
