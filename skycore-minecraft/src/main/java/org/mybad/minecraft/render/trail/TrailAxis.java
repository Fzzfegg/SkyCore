package org.mybad.minecraft.render.trail;

public enum TrailAxis {
    X,
    Y,
    Z;

    public static TrailAxis fromString(String raw) {
        if (raw == null) {
            return Z;
        }
        String v = raw.trim().toLowerCase();
        switch (v) {
            case "x":
                return X;
            case "y":
                return Y;
            case "z":
            default:
                return Z;
        }
    }
}
