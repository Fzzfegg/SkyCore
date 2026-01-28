package org.mybad.packtool;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convenience runner for IDE usage.
 * <p>
 * Reads parameters from {@code packtool-dev.properties} located at the project root
 * (or module root) and executes {@link org.mybad.packtool.ResourcePackCompiler} without requiring
 * command-line arguments.
 */
public final class DevPackToolRunner {

    private static final String CONFIG_FILE_NAME = "packtool-dev.properties";

    private DevPackToolRunner() {}

    public static void main(String[] args) throws Exception {
        Path configPath = locateConfigFile();
        if (configPath == null) {
            System.err.println("[PackTool] 未找到 " + CONFIG_FILE_NAME + "，请参照 packtool-dev.properties.example 创建。");
            return;
        }
        Map<String, String> map = loadConfig(configPath);
        org.mybad.packtool.ResourcePackCompiler.Config config = org.mybad.packtool.ResourcePackCompiler.Config.fromMap(map);
        org.mybad.packtool.ResourcePackCompiler.runWithConfig(config);
    }

    private static Map<String, String> loadConfig(Path path) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                    continue;
                }
                int index = trimmed.indexOf('=');
                if (index < 0) {
                    index = trimmed.indexOf(':');
                }
                if (index < 0) {
                    continue;
                }
                String key = trimmed.substring(0, index).trim();
                String value = trimmed.substring(index + 1).trim();
                map.put(key, value);
            }
        }
        return map;
    }

    private static Path locateConfigFile() {
        Path direct = Paths.get(CONFIG_FILE_NAME).toAbsolutePath().normalize();
        if (Files.exists(direct)) {
            return direct;
        }
        Path moduleFallback = Paths.get("skycore-packtool").resolve(CONFIG_FILE_NAME).toAbsolutePath().normalize();
        if (Files.exists(moduleFallback)) {
            return moduleFallback;
        }
        return null;
    }
}
