package org.mybad.minecraft.render.glow;

import org.mybad.minecraft.config.EntityModelMapping;

/**
 * Per-entity glow/bloom settings derived from model mapping.
 */
public final class GlowConfig {
    private final String name;
    private final int[] color;
    private final float strength;

    public GlowConfig(String name, int[] color, float strength) {
        this.name = name;
        this.color = color;
        this.strength = strength;
    }

    public static GlowConfig fromMapping(EntityModelMapping mapping) {
        if (mapping == null) {
            return null;
        }
        int[] color = mapping.getBloomColor();
        if (color == null || color.length < 4) {
            color = new int[]{255, 255, 255, 255};
        }
        return new GlowConfig(
                mapping.getName(),
                color,
                mapping.getBloomStrength()
        );
    }

    public String getName() {
        return name;
    }

    public int[] getColor() {
        return color;
    }

    public float getStrength() {
        return strength;
    }
}
