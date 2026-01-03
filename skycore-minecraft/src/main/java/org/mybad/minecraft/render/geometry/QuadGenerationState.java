package org.mybad.minecraft.render.geometry;

final class QuadGenerationState {
    private boolean generated;

    boolean isGenerated() {
        return generated;
    }

    void markGenerated() {
        generated = true;
    }

    void clear() {
        generated = false;
    }
}
