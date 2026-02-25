package org.mybad.minecraft.navigation;

import net.minecraft.client.Minecraft;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.skycoreproto.SkyCoreProto;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class WaypointStyleRegistry {

    private final Map<String, WaypointStyleDefinition> styles = new HashMap<>();
    private final WaypointStyleDefinition defaultStyle = WaypointStyleDefinition.defaultStyle();
    private final Path cacheFile;

    WaypointStyleRegistry() {
        Path root = Minecraft.getMinecraft().gameDir.toPath().resolve("skycore_cache");
        this.cacheFile = root.resolve("navigation_styles.pb");
        styles.put(defaultStyle.getId(), defaultStyle);
        reloadFromDisk();
    }

    synchronized void reloadFromDisk() {
        if (!Files.exists(cacheFile)) {
            styles.clear();
            styles.put(defaultStyle.getId(), defaultStyle);
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(cacheFile);
            SkyCoreProto.NavigationStyleConfig config = SkyCoreProto.NavigationStyleConfig.parseFrom(bytes);
            replaceFromProto(config);
        } catch (IOException ex) {
            SkyCoreMod.LOGGER.warn("[Waypoint] 样式缓存解析失败: {}", ex.getMessage());
        }
    }

    synchronized void applyRemoteConfig(@Nullable SkyCoreProto.NavigationStyleConfig config) {
        if (config == null) {
            return;
        }
        persist(config);
        replaceFromProto(config);
    }

    synchronized WaypointStyleDefinition resolve(String styleId) {
        if (styleId == null || styleId.isEmpty()) {
            return defaultStyle;
        }
        return styles.getOrDefault(styleId, defaultStyle);
    }

    private void replaceFromProto(SkyCoreProto.NavigationStyleConfig config) {
        Map<String, WaypointStyleDefinition> updated = new HashMap<>();
        updated.put(defaultStyle.getId(), defaultStyle);
        for (SkyCoreProto.NavigationStyle styleProto : config.getStylesList()) {
            WaypointStyleDefinition def = WaypointStyleDefinition.fromProto(styleProto);
            if (def != null) {
                updated.put(def.getId(), def);
            }
        }
        styles.clear();
        styles.putAll(updated);
    }

    private void persist(SkyCoreProto.NavigationStyleConfig config) {
        try {
            Files.createDirectories(cacheFile.getParent());
            Files.write(cacheFile, config.toByteArray());
        } catch (IOException ex) {
            SkyCoreMod.LOGGER.warn("[Waypoint] 无法写入样式缓存: {}", ex.getMessage());
        }
    }
}
