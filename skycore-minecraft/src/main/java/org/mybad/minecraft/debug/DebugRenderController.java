package org.mybad.minecraft.debug;

import org.mybad.skycoreproto.SkyCoreProto;

/**
 * 维护客户端调试渲染开关状态
 */
public final class DebugRenderController {
    private static boolean showEntityBoxes;
    private static boolean showSkullAnchors;

    private DebugRenderController() {
    }

    public static void apply(SkyCoreProto.DebugRenderFlags flags) {
        if (flags == null) {
            return;
        }
        if (flags.hasShowEntityBoxes()) {
            showEntityBoxes = flags.getShowEntityBoxes();
        }
        if (flags.hasShowSkullAnchors()) {
            showSkullAnchors = flags.getShowSkullAnchors();
        }
    }

    public static boolean shouldDrawEntityBoxes() {
        return showEntityBoxes;
    }

    public static boolean shouldDrawSkullAnchors() {
        return showSkullAnchors;
    }

    public static boolean isActive() {
        return showEntityBoxes || showSkullAnchors;
    }

    public static void clear() {
        showEntityBoxes = false;
        showSkullAnchors = false;
    }
}
