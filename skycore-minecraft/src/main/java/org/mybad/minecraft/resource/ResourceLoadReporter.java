package org.mybad.minecraft.resource;

import org.mybad.minecraft.SkyCoreMod;

/**
 * Small utility for consistent resource load logging.
 */
final class ResourceLoadReporter {
    private final String component;

    ResourceLoadReporter(String component) {
        this.component = component;
    }

    void missing(String path) {
        SkyCoreMod.LOGGER.warn("[SkyCore][" + component + "] 无法加载资源: {}", path);
    }

    void missing(String path, Exception e) {
        if (e == null) {
            missing(path);
            return;
        }
        SkyCoreMod.LOGGER.warn("[SkyCore][" + component + "] 无法加载资源: {} - {}", path, e.getMessage());
    }

    void parseFailed(String path, Exception e) {
        SkyCoreMod.LOGGER.error("[SkyCore][" + component + "] 解析失败: {} - {}", path, e.getMessage());
    }

    void warn(String message, Object... args) {
        SkyCoreMod.LOGGER.warn("[SkyCore][" + component + "] " + message, args);
    }
}
