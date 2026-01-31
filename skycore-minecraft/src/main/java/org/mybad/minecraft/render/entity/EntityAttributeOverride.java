package org.mybad.minecraft.render.entity;

final class EntityAttributeOverride {
    final Float scale;
    final Float emissiveStrength;
    final Float bloomStrength;
    final Float primaryFadeSeconds;
    final float[] bloomOffset;
    final Integer bloomPasses;
    final Float bloomScaleStep;
    final Float bloomDownscale;

    EntityAttributeOverride(Float scale,
                            Float emissiveStrength,
                            Float bloomStrength,
                            Float primaryFadeSeconds,
                            float[] bloomOffset,
                            Integer bloomPasses,
                            Float bloomScaleStep,
                            Float bloomDownscale) {
        this.scale = scale;
        this.emissiveStrength = emissiveStrength;
        this.bloomStrength = bloomStrength;
        this.primaryFadeSeconds = primaryFadeSeconds;
        this.bloomOffset = bloomOffset;
        this.bloomPasses = bloomPasses;
        this.bloomScaleStep = bloomScaleStep;
        this.bloomDownscale = bloomDownscale;
    }

    EntityAttributeOverride merge(EntityAttributeOverride other) {
        if (other == null) {
            return this;
        }
        return new EntityAttributeOverride(
            other.scale != null ? other.scale : this.scale,
            other.emissiveStrength != null ? other.emissiveStrength : this.emissiveStrength,
            other.bloomStrength != null ? other.bloomStrength : this.bloomStrength,
            other.primaryFadeSeconds != null ? other.primaryFadeSeconds : this.primaryFadeSeconds,
            other.bloomOffset != null ? other.bloomOffset : this.bloomOffset,
            other.bloomPasses != null ? other.bloomPasses : this.bloomPasses,
            other.bloomScaleStep != null ? other.bloomScaleStep : this.bloomScaleStep,
            other.bloomDownscale != null ? other.bloomDownscale : this.bloomDownscale
        );
    }
}
