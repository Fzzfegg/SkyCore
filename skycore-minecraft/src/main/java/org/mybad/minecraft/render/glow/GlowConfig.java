package org.mybad.minecraft.render.glow;

import org.mybad.minecraft.config.EntityModelMapping;

/**
 * Per-entity glow/bloom settings derived from model mapping.
 */
public final class GlowConfig {
    private final String name;
    private final int[] color;
    private final float strength;
    private final float radius;
    private final int priority;

    public GlowConfig(String name, int[] color, float strength, float radius, int priority) {
        this.name = name;
        this.color = color;
        this.strength = strength;
        this.radius = radius;
        this.priority = priority;
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
                mapping.getBloomStrength(),
                mapping.getBloomRadius(),
                mapping.getBloomPriority()
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

    public float getRadius() {
        return radius;
    }

    public int getPriority() {
        return priority;
    }
}

