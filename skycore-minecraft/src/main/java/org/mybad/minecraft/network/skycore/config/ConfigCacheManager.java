package org.mybad.minecraft.network.skycore.config;

import org.mybad.skycoreproto.SkyCoreProto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ConfigCacheManager {
    private final Path root;
    private final Path indexFile;
    private final Path renderSettingsFile;

    public ConfigCacheManager(Path gameDir) {
        this.root = gameDir.resolve("skycore_cache");
        this.indexFile = root.resolve("config_index.pb");
        this.renderSettingsFile = root.resolve("render_settings.pb");
    }

    public void saveIndex(SkyCoreProto.ConfigIndex index) {
        try {
            ensureRoot();
            Files.write(indexFile, index.toByteArray());
        } catch (IOException ignored) {
        } 
    }

    public SkyCoreProto.ConfigIndex loadIndex() {
        try {
            if (Files.exists(indexFile)) {
                byte[] bytes = Files.readAllBytes(indexFile);
                return SkyCoreProto.ConfigIndex.parseFrom(bytes);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public void saveMappingFile(SkyCoreProto.MappingFile file) {
        try {
            ensureRoot();
            Files.write(resolveFile(file.getFileName()), file.toByteArray());
        } catch (IOException ignored) {
        }
    }

    public void deleteMappingFile(String fileName) {
        try {
            Files.deleteIfExists(resolveFile(fileName));
        } catch (IOException ignored) {
        }
    }

    public List<SkyCoreProto.MappingFile> loadAllFiles() {
        List<SkyCoreProto.MappingFile> list = new ArrayList<>();
        if (!Files.exists(root)) {
            return list;
        }
        try {
            Files.list(root).forEach(path -> {
                String name = path.getFileName().toString();
                if (Files.isRegularFile(path)
                        && name.endsWith(".pb")
                        && !path.equals(indexFile)
                        && !path.equals(renderSettingsFile)) {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        list.add(SkyCoreProto.MappingFile.parseFrom(bytes));
                    } catch (IOException ignored) {
                    }
                }
            });
        } catch (IOException ignored) {
        }
        return list;
    }

    private Path resolveFile(String fileName) {
        String safe = fileName.replaceAll("[/\\\\]", "_");
        return root.resolve(safe + ".pb");
    }

    public void saveRenderSettings(SkyCoreProto.RenderSettings settings) {
        try {
            ensureRoot();
            Files.write(renderSettingsFile, settings.toByteArray());
        } catch (IOException ignored) {
        }
    }

    public SkyCoreProto.RenderSettings loadRenderSettings() {
        try {
            if (Files.exists(renderSettingsFile)) {
                return SkyCoreProto.RenderSettings.parseFrom(Files.readAllBytes(renderSettingsFile));
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private void ensureRoot() throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
    }
}
