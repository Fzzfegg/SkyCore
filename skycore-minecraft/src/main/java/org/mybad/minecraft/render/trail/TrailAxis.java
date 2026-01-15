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
            case "right":
                return X;
            case "y":
            case "up":
                return Y;
            case "z":
            case "forward":
            default:
                return Z;
        }
    }
}
