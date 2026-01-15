package org.mybad.minecraft.render.trail;

/**
 * Blending mode for sword trails.
 */
public enum TrailBlendMode {
    ALPHA,
    ADD;

    public static TrailBlendMode fromString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return ADD;
        }
        String value = raw.trim().toLowerCase();
        if ("alpha".equals(value) || "normal".equals(value)) {
            return ALPHA;
        }
        if ("add".equals(value) || "additive".equals(value)) {
            return ADD;
        }
        return ADD;
    }
}
