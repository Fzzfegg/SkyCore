package org.mybad.minecraft.audio;

import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.resource.ResourcePackRegistrar;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class SoundExistenceCache {
    private static final Set<ResourceLocation> EXISTING = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean ready;

    private SoundExistenceCache() {}

    public static void rescan(Path gameDir) {
        EXISTING.clear();
        ready = false;
        Path packRoot = ResourcePackRegistrar.getPackRoot(gameDir != null ? gameDir.toFile() : null);
        if (packRoot == null || !Files.isDirectory(packRoot)) {
            ready = true;
            return;
        }
        int total = 0;
        try (DirectoryStream<Path> domains = Files.newDirectoryStream(packRoot)) {
            for (Path namespaceDir : domains) {
                if (!Files.isDirectory(namespaceDir)) {
                    continue;
                }
                String namespace = namespaceDir.getFileName().toString();
                Path soundsDir = namespaceDir.resolve("sounds");
                if (!Files.isDirectory(soundsDir)) {
                    continue;
                }
                Files.walk(soundsDir).forEach(p -> {
                    if (!Files.isRegularFile(p)) {
                        return;
                    }
                    String lower = p.toString().toLowerCase(Locale.ROOT);
                    if (!lower.endsWith(".ogg")) {
                        return;
                    }
                    String rel = soundsDir.relativize(p).toString().replace('\\', '/');
                    String idPath = rel.substring(0, rel.length() - 4);
                    EXISTING.add(new ResourceLocation(namespace, idPath));
                });
            }
        } catch (IOException ignored) {
        }
        total = EXISTING.size();
        ready = true;
        SkyCoreMod.LOGGER.info("[SkyCore] 声音存在表加载完成: total={}", total);
    }

    public static boolean canPlay(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        if (!ready) {
            return false;
        }
        return EXISTING.contains(id);
    }
}
