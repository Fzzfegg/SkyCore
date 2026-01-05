package org.mybad.minecraft.resource;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Virtual resource pack that serves files from SkyCore root.
 * Layout: <root>/<namespace>/<path>
 * Also generates sounds.json based on .ogg files under <root>/<namespace>/sounds/.
 */
public final class ConfigResourcePack implements IResourcePack {
    private final Path root;
    private final String packName;

    public ConfigResourcePack(Path root) {
        this.root = root;
        this.packName = "SkyCore Config Pack";
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if ("sounds.json".equals(location.getPath())) {
            String json = buildSoundsJson(location.getNamespace());
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }
        Path file = resolvePath(location);
        if (file == null || !Files.isRegularFile(file)) {
            throw new FileNotFoundException(location.toString());
        }
        return Files.newInputStream(file);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if ("sounds.json".equals(location.getPath())) {
            return hasSounds(location.getNamespace());
        }
        Path file = resolvePath(location);
        return file != null && Files.isRegularFile(file);
    }

    @Override
    public Set<String> getResourceDomains() {
        if (!Files.isDirectory(root)) {
            return Collections.emptySet();
        }
        Set<String> domains = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    domains.add(child.getFileName().toString());
                }
            }
        } catch (IOException ignored) {
        }
        return domains;
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String metadataSectionName) {
        return null;
    }

    @Nullable
    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public String getPackName() {
        return packName;
    }

    private Path resolvePath(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        Path base = root.resolve(location.getNamespace());
        return base.resolve(location.getPath().replace('/', File.separatorChar));
    }

    private boolean hasSounds(String namespace) {
        Path soundsDir = root.resolve(namespace).resolve("sounds");
        if (!Files.isDirectory(soundsDir)) {
            return false;
        }
        try {
            return Files.walk(soundsDir).anyMatch(p -> Files.isRegularFile(p) && p.toString().toLowerCase(Locale.ROOT).endsWith(".ogg"));
        } catch (IOException e) {
            return false;
        }
    }

    private String buildSoundsJson(String namespace) {
        Map<String, List<String>> soundMap = new LinkedHashMap<>();
        Path soundsDir = root.resolve(namespace).resolve("sounds");
        if (Files.isDirectory(soundsDir)) {
            try {
                Files.walk(soundsDir).forEach(p -> {
                    if (!Files.isRegularFile(p)) {
                        return;
                    }
                    String path = p.toString().toLowerCase(Locale.ROOT);
                    if (!path.endsWith(".ogg")) {
                        return;
                    }
                    String rel = soundsDir.relativize(p).toString().replace(File.separatorChar, '/');
                    String id = rel.substring(0, rel.length() - 4); // strip .ogg
                    soundMap.computeIfAbsent(id, k -> new ArrayList<>()).add(id);
                });
            } catch (IOException ignored) {
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : soundMap.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\n  \"").append(entry.getKey()).append("\": {\n    \"sounds\": [");
            List<String> sounds = entry.getValue();
            for (int i = 0; i < sounds.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append("\"").append(sounds.get(i)).append("\"");
            }
            sb.append("]\n  }");
        }
        if (!soundMap.isEmpty()) {
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
