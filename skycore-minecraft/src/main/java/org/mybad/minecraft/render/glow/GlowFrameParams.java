package org.mybad.minecraft.render.glow;

/**
 * Describes bloom parameters accumulated for the current frame.
 */
public final class GlowFrameParams {
    private float strength = 1.0f;
    private float tintR = 1.0f;
    private float tintG = 1.0f;
    private float tintB = 1.0f;
    private boolean useTint;

    public float getStrength() {
        return strength;
    }

    public void setStrength(float strength) {
        this.strength = strength;
    }

    public boolean isUseTint() {
        return useTint;
    }

    public void setTint(float r, float g, float b) {
        this.tintR = r;
        this.tintG = g;
        this.tintB = b;
        this.useTint = true;
    }

    public float getTintR() {
        return tintR;
    }

    public float getTintG() {
        return tintG;
    }

    public float getTintB() {
        return tintB;
    }

    public void resetTint() {
        this.tintR = this.tintG = this.tintB = 1.0f;
        this.useTint = false;
    }

}
