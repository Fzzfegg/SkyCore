package org.mybad.minecraft.network.skycore;

public final class SkycorePacketId {
    private SkycorePacketId() {}

    public static final int HELLO = 1;
    public static final int CONFIG_INDEX = 2;
    public static final int CONFIG_FILE = 3;
    public static final int CONFIG_FILE_REMOVED = 4;
    public static final int RENDER_SETTINGS = 5;

    public static final int FORCE_ANIMATION = 16;
    public static final int SET_MODEL_ATTRIBUTES = 18;

    public static final int PRELOAD_HINT = 24;
    public static final int SPAWN_PARTICLE = 32;

    public static final int DEBUG_MESSAGE = 64;
    public static final int DEBUG_FLAGS = 65;
}
