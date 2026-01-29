package org.mybad.minecraft.render.entity;

import org.mybad.skycoreproto.SkyCoreProto;

public final class HeadBarConfigStore {

    private static volatile SkyCoreProto.HeadBarConfig config;

    private HeadBarConfigStore() {
    }

    public static SkyCoreProto.HeadBarConfig getConfig() {
        return config;
    }

    public static void update(SkyCoreProto.HeadBarConfig newConfig) {
        config = newConfig;
    }

    public static void clear() {
        config = null;
    }
}
