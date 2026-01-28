package org.mybad.minecraft.resource;

import org.mybad.minecraft.SkyCoreMod;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small utility for consistent resource load logging.
 */
final class ResourceLoadReporter {
    private static final long LOG_COOLDOWN_MS = 25000L;
    private static final Map<String, Long> LAST_LOG = new ConcurrentHashMap<>();

    private final String component;

    ResourceLoadReporter(String component) {
        this.component = component;
    }

    void missing(String path) {
        if (!shouldLog("missing", path)) {
            return;
        }
        SkyCoreMod.LOGGER.warn("[SkyCore][" + component + "] 无法加载资源: {}", path);
    }

    void missing(String path, Exception e) {
        if (e == null) {
            missing(path);
            return;
        }
        if (!shouldLog("missing", path)) {
            return;
        }
        SkyCoreMod.LOGGER.warn("[SkyCore][" + component + "] 无法加载资源: {} - {}", path, e.getMessage());
    }

    void parseFailed(String path, Exception e) {
        parseFailed(path, null, e);
    }

    void parseFailed(String logicalPath, Path binaryPath, Exception e) {
        if (!shouldLog("parse", logicalPath)) {
            return;
        }
        StringBuilder builder = new StringBuilder("[SkyCore][")
            .append(component)
            .append("] 解析失败: ")
            .append(logicalPath);
        if (binaryPath != null) {
            builder.append(" (binary=")
                .append(binaryPath.toString().replace('\\', '/'))
                .append(')');
        }
        String error = e != null ? e.getMessage() : "unknown";
        SkyCoreMod.LOGGER.warn(builder.append(" - ").append(error).toString());
    }

    void warn(String message, Object... args) {
        if (!shouldLog("warn", message)) {
            return;
        }
        SkyCoreMod.LOGGER.warn("[SkyCore][" + component + "] " + message, args);
    }

    private boolean shouldLog(String type, String identifier) {
        if (identifier == null) {
            return true;
        }
        String key = component + "|" + type + "|" + identifier;
        long now = System.currentTimeMillis();
        Long last = LAST_LOG.get(key);
        if (last != null && now - last < LOG_COOLDOWN_MS) {
            return false;
        }
        LAST_LOG.put(key, now);
        return true;
    }
}
