package org.mybad.packtool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.bedrockparticle.particle.ParticleParser;
import org.mybad.bedrockparticle.particle.io.ParticleBinarySerializer;
import org.mybad.core.animation.Animation;
import org.mybad.core.binary.BinaryDataWriter;
import org.mybad.core.binary.BinaryKeyDeriver;
import org.mybad.core.binary.BinaryPayloadCipher;
import org.mybad.core.binary.BinaryResourceIO;
import org.mybad.core.binary.BinaryResourceType;
import org.mybad.core.binary.animation.AnimationSetBinarySerializer;
import org.mybad.core.binary.audio.AudioBinarySerializer;
import org.mybad.core.binary.model.ModelBinarySerializer;
import org.mybad.core.binary.texture.TextureBinarySerializer;
import org.mybad.core.data.Model;
import org.mybad.core.parsing.AnimationParser;
import org.mybad.core.parsing.ModelParser;
import org.mybad.core.resource.PathObfuscator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CLI tool to compile JSON resources into SkyCore binary archives (.skm/.ska/.skp).
 */
public final class ResourcePackCompiler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private enum ResourceKind {
        MODEL(".skm", BinaryResourceType.MODEL),
        ANIMATION(".ska", BinaryResourceType.ANIMATION),
        PARTICLE(".skp", BinaryResourceType.PARTICLE),
        TEXTURE(".skt", BinaryResourceType.TEXTURE),
        AUDIO(".sko", BinaryResourceType.AUDIO);

        final String extension;
        final BinaryResourceType type;

        ResourceKind(String extension, BinaryResourceType type) {
            this.extension = extension;
            this.type = type;
        }
    }

    private final ModelParser modelParser = new ModelParser();
    private final ModelBinarySerializer modelSerializer = new ModelBinarySerializer();
    private final AnimationParser animationParser = new AnimationParser();
    private final AnimationSetBinarySerializer animationSetSerializer = new AnimationSetBinarySerializer();
    private final ParticleBinarySerializer particleSerializer = new ParticleBinarySerializer();
    private final TextureBinarySerializer textureSerializer = new TextureBinarySerializer();
    private final AudioBinarySerializer audioSerializer = new AudioBinarySerializer();

    private final BinaryPayloadCipher cipher;
    private final boolean encryptedOutput;
    private final PathObfuscator.Mode pathMode;
    private final Path inputRoot;
    private final Path outputRoot;

    private ResourcePackCompiler(Path inputRoot,
                                 Path outputRoot,
                                 BinaryPayloadCipher cipher,
                                 boolean encryptedOutput,
                                 PathObfuscator.Mode pathMode) {
        this.inputRoot = inputRoot;
        this.outputRoot = outputRoot;
        this.cipher = cipher == null ? BinaryPayloadCipher.NO_OP : cipher;
        this.encryptedOutput = encryptedOutput;
        this.pathMode = pathMode == null ? PathObfuscator.Mode.DEV : pathMode;
    }

    public static void main(String[] args) throws Exception {
        runWithConfig(Config.fromArgs(args));
    }

    public static void runWithConfig(Config config) throws Exception {
        if (!config.valid()) {
            System.exit(1);
        }
        Files.createDirectories(config.output);
        ResourcePackCompiler compiler = new ResourcePackCompiler(
            config.input,
            config.output,
            config.cipher,
            config.encryptedOutput,
            config.pathMode
        );
        compiler.compile();
    }

    private void compile() throws IOException {
        List<ManifestEntry> manifest = new ArrayList<>();
        Files.walkFileTree(inputRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                try {
                    processFile(file, manifest);
                } catch (Exception ex) {
                    System.err.println("[PackCompiler] Failed to process " + file + " - " + ex.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        writeManifest(manifest);
        System.out.println("[PackCompiler] Done. Generated " + manifest.size() + " resources.");
    }

    private void processFile(Path file, List<ManifestEntry> manifest) throws IOException, GeneralSecurityException {
        ResourceKind kind = detectKind(file);
        if (kind == null) {
            return;
        }
        byte[] payload = serialize(kind, file);
        if (payload == null) {
            return;
        }
        int version = serializerVersion(kind);
        byte[] archive = BinaryResourceIO.write(kind.type, version, 0, payload, cipher);
        Path relative = inputRoot.relativize(file);
        String relativeNormalized = normalizeRelative(relative.toString());
        String binaryRelative = replaceExtension(relativeNormalized, kind.extension);
        String logicalBinaryPath = toLogicalPath(binaryRelative);
        String physicalRelative = PathObfuscator.toPhysical(logicalBinaryPath, pathMode);
        if (encryptedOutput) {
            physicalRelative = physicalRelative + ".enc";
        }
        Path target = outputRoot.resolve(physicalRelative);
        Files.createDirectories(target.getParent());
        Files.write(target, archive);
        manifest.add(ManifestEntry.from(
            logicalBinaryPath,
            relativeNormalized,
            outputRoot.relativize(target).toString(),
            kind.type.name(),
            archive
        ));
        System.out.println("[PackCompiler] " + relativeNormalized + " -> " + physicalRelative);
    }

    private byte[] serialize(ResourceKind kind, Path file) throws IOException {
        BinaryDataWriter writer = new BinaryDataWriter();
        try {
            switch (kind) {
                case MODEL: {
                    byte[] raw = Files.readAllBytes(file);
                    String json = new String(raw, StandardCharsets.UTF_8);
                    Model model = modelParser.parse(json);
                    modelSerializer.write(writer, model);
                    break;
                }
                case ANIMATION: {
                    byte[] raw = Files.readAllBytes(file);
                    String json = new String(raw, StandardCharsets.UTF_8);
                    Map<String, AnimationParser.AnimationData> dataMap = animationParser.parseAll(json);
                    Map<String, Animation> animations = animationParser.toAnimations(dataMap);
                    animationSetSerializer.write(writer, animations);
                    break;
                }
                case PARTICLE: {
                    byte[] raw = Files.readAllBytes(file);
                    String json = new String(raw, StandardCharsets.UTF_8);
                    ParticleData particle = ParticleParser.parseParticle(json);
                    particleSerializer.write(writer, particle);
                    break;
                }
                case TEXTURE: {
                    byte[] bytes = Files.readAllBytes(file);
                    textureSerializer.write(writer, bytes);
                    break;
                }
                case AUDIO: {
                    byte[] bytes = Files.readAllBytes(file);
                    audioSerializer.write(writer, bytes);
                    break;
                }
                default:
                    return null;
            }
        } catch (Exception ex) {
            throw new IOException("Serialize failed for " + file.getFileName() + ": " + ex.getMessage(), ex);
        }
        try {
            return writer.toByteArray();
        } catch (IOException e) {
            throw new IOException("Failed to finalize payload for " + file.getFileName(), e);
        }
    }

    private ResourceKind detectKind(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".geo.json")) {
            return ResourceKind.MODEL;
        }
        if (name.endsWith(".animation.json") || name.endsWith(".anim.json")) {
            return ResourceKind.ANIMATION;
        }
        if (name.endsWith(".json")) {
            // attempt to detect particle by directory hint
            String normalized = file.toString().replace('\\', '/');
            if (normalized.contains("/particles/")) {
                return ResourceKind.PARTICLE;
            }
        }
        if (name.endsWith(".png")) {
            return ResourceKind.TEXTURE;
        }
        if (name.endsWith(".ogg")) {
            return ResourceKind.AUDIO;
        }
        return null;
    }

    private String replaceExtension(String path, String newExt) {
        String normalized = path.replace('\\', '/');
        String lower = normalized.toLowerCase(Locale.ROOT);
        String[] suffixes = {".geo.json", ".animation.json", ".anim.json", ".json", ".png", ".ogg"};
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                return normalized.substring(0, normalized.length() - suffix.length()) + newExt;
            }
        }
        return normalized + newExt;
    }

    private String normalizeRelative(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim();
        if (trimmed.startsWith("./")) {
            trimmed = trimmed.substring(2);
        }
        return trimmed.replace('\\', '/');
    }

    private String toLogicalPath(String namespacedRelative) {
        if (namespacedRelative == null || namespacedRelative.isEmpty()) {
            throw new IllegalArgumentException("Relative path is empty");
        }
        String normalized = namespacedRelative;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("assets/")) {
            normalized = normalized.substring("assets/".length());
        }
        int slash = normalized.indexOf('/');
        String namespace = slash > 0 ? normalized.substring(0, slash) : PathObfuscator.DEFAULT_NAMESPACE;
        String path = slash > 0 ? normalized.substring(slash + 1) : normalized;
        if (path.isEmpty()) {
            throw new IllegalArgumentException("Logical path missing resource segment: " + namespacedRelative);
        }
        return namespace + ":" + path;
    }

    private int serializerVersion(ResourceKind kind) {
        switch (kind) {
            case MODEL:
                return modelSerializer.getVersion();
            case ANIMATION:
                return animationSetSerializer.getVersion();
            case PARTICLE:
                return particleSerializer.getVersion();
            case TEXTURE:
                return textureSerializer.getVersion();
            case AUDIO:
                return audioSerializer.getVersion();
            default:
                return 0;
        }
    }

    private void writeManifest(List<ManifestEntry> manifest) throws IOException {
        manifest.sort(Comparator.comparing(entry -> entry.output));
        Path manifestFile = outputRoot.resolve("skycore-pack-manifest.json");
        byte[] json = GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8);
        Files.write(manifestFile, json);
    }

    private static final class ManifestEntry {
        String logical;
        String source;
        String output;
        String type;
        long size;
        String sha256;

        static ManifestEntry from(String logical, String source, String output, String type, byte[] data) {
            ManifestEntry entry = new ManifestEntry();
            entry.logical = normalize(logical);
            entry.source = normalize(source);
            entry.output = normalize(output);
            entry.type = type;
            entry.size = data.length;
            entry.sha256 = digest(data);
            return entry;
        }

        private static String normalize(String text) {
            return text == null ? "" : text.replace('\\', '/');
        }

        private static String digest(byte[] data) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(data);
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception ex) {
                return "";
            }
        }
    }

    public static final class Config {
        Path input;
        Path output;
        BinaryPayloadCipher cipher = BinaryPayloadCipher.NO_OP;
        boolean encryptedOutput = false;
        PathObfuscator.Mode pathMode = PathObfuscator.Mode.DEV;
        int keySize = 16;

        static Config fromArgs(String[] args) throws IOException {
            return fromMap(parseArgs(args));
        }

        public static Config fromMap(Map<String, String> kv) throws IOException {
            Config config = new Config();
            if (kv.containsKey("input")) {
                config.input = Paths.get(kv.get("input")).toAbsolutePath().normalize();
            }
            if (kv.containsKey("output")) {
                config.output = Paths.get(kv.get("output")).toAbsolutePath().normalize();
            }
            String mode = kv.getOrDefault("mode", "dev").toLowerCase(Locale.ROOT);
            config.pathMode = "prod".equals(mode) ? PathObfuscator.Mode.PROD : PathObfuscator.Mode.DEV;
            config.encryptedOutput = config.pathMode == PathObfuscator.Mode.PROD
                || Boolean.parseBoolean(kv.getOrDefault("encrypt", "false"));
            String cipherName = kv.getOrDefault("cipher", "aes-ctr").toLowerCase(Locale.ROOT);
            if (!config.encryptedOutput) {
                config.cipher = BinaryPayloadCipher.NO_OP;
            } else {
                byte[] seed = loadKey(kv);
                int requestedSize = parseInt(kv, "keySize",
                    parseInt(kv, "key-size", seed.length));
                config.keySize = normalizeKeySize(requestedSize, seed.length);
                byte[] key = BinaryKeyDeriver.derive(seed, config.keySize);
                if ("aes-gcm".equals(cipherName)) {
                    config.cipher = new org.mybad.core.binary.AesGcmBinaryCipher(key);
                } else {
                    config.cipher = new org.mybad.core.binary.AesCtrBinaryCipher(key);
                }
            }
            return config;
        }

        boolean valid() {
            System.out.println(input);
            System.out.println(output);
            return input != null && Files.isDirectory(input) && output != null;
        }
        
        private static Map<String, String> parseArgs(String[] args) {
            if (args == null || args.length == 0) {
                return Collections.emptyMap();
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (String arg : args) {
                if (arg == null) {
                    continue;
                }
                String trimmed = arg.trim();
                if (!trimmed.startsWith("--")) {
                    continue;
                }
                String[] parts = trimmed.substring(2).split("=", 2);
                String key = parts[0];
                String value = parts.length > 1 ? parts[1] : "true";
                map.put(key, value);
            }
            return map;
        }

        private static int parseInt(Map<String, String> map, String key, int defaultValue) {
            if (map.containsKey(key)) {
                try {
                    return Integer.parseInt(map.get(key).trim());
                } catch (NumberFormatException ignored) {
                }
            }
            return defaultValue;
        }

        private static int normalizeKeySize(int requested, int seedLength) {
            switch (requested) {
                case 16:
                case 24:
                case 32:
                    return requested;
                default:
                    switch (seedLength) {
                        case 24:
                        case 32:
                            return seedLength;
                        case 16:
                        default:
                            return 16;
                    }
            }
        }

        private static byte[] loadKey(Map<String, String> args) throws IOException {
            if (args.containsKey("key")) {
                return hexToBytes(args.get("key"));
            }
            if (args.containsKey("keyFile")) {
                byte[] raw = Files.readAllBytes(Paths.get(args.get("keyFile")).toAbsolutePath());
                String text = new String(raw, StandardCharsets.UTF_8).trim();
                return hexToBytes(text);
            }
            throw new IllegalArgumentException("Encryption enabled but no key provided.");
        }

        private static byte[] hexToBytes(String hex) {
            if (hex == null) {
                throw new IllegalArgumentException("Key hex is empty");
            }
            String normalized = hex.replaceAll("[^0-9a-fA-F]", "");
            if (normalized.length() % 2 != 0) {
                throw new IllegalArgumentException("Invalid hex length");
            }
            byte[] result = new byte[normalized.length() / 2];
            for (int i = 0; i < normalized.length(); i += 2) {
                result[i / 2] = (byte) Integer.parseInt(normalized.substring(i, i + 2), 16);
            }
            return result;
        }
    }
}
