package org.mybad.minecraft.render.trail;

/**
 * Trail events actions.
 */
public enum TrailAction {
    START,
    STOP;

    public static TrailAction fromString(String raw) {
        if (raw == null || raw.isEmpty()) {
            return START;
        }
        String value = raw.trim().toLowerCase();
        if ("stop".equals(value)) {
            return STOP;
        }
        return START;
    }
}
