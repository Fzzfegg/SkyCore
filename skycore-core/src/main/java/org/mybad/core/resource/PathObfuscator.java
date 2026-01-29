package org.mybad.core.resource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Deterministically maps logical namespace:path identifiers to obfuscated physical paths.
 */
public final class PathObfuscator {

    public static final String DEFAULT_NAMESPACE = "skycore";
    private static final String VERSION_PREFIX = "v1/";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int DIR_TOKEN_LENGTH = 12;
    private static final int FILE_TOKEN_LENGTH = 16;
    private static final char[] BASE32 = "abcdefghijklmnopqrstuvwxyz234567".toCharArray();
    private static final byte[] KEY_BYTES = new byte[] {
        (byte) 0x62, (byte) 0xA1, (byte) 0xF0, (byte) 0x1C,
        (byte) 0x54, (byte) 0x3D, (byte) 0x9B, (byte) 0x2E,
        (byte) 0xC7, (byte) 0x10, (byte) 0x4A, (byte) 0x85,
        (byte) 0x28, (byte) 0x7E, (byte) 0xF3, (byte) 0xD4,
        (byte) 0x99, (byte) 0xB2, (byte) 0x6F, (byte) 0x01,
        (byte) 0x73, (byte) 0xE4, (byte) 0xC2, (byte) 0x5D,
        (byte) 0x8A, (byte) 0x11, (byte) 0x6C, (byte) 0x97,
        (byte) 0x3A, (byte) 0xDF, (byte) 0x42, (byte) 0xB5
    };
    private static final ThreadLocal<Mac> MAC = ThreadLocal.withInitial(PathObfuscator::createMac);
    private static volatile BiConsumer<String, String> mappingListener;

    private PathObfuscator() {
    }

    public enum Mode {
        DEV,
        PROD
    }

    public static void setMappingListener(BiConsumer<String, String> listener) {
        mappingListener = listener;
    }

    public static String toPhysical(String logicalPath, Mode mode) {
        Objects.requireNonNull(mode, "mode");
        LogicalResource resource = parse(logicalPath);
        if (mode == Mode.DEV) {
            String devPath = resource.namespace + "/" + resource.relativePath;
            publish(resource.logicalString(), devPath);
            return devPath;
        }
        String physical = obfuscate(resource);
        publish(resource.logicalString(), physical);
        return physical;
    }

    public static String canonicalLogical(String logicalPath) {
        return parse(logicalPath).logicalString();
    }

    private static void publish(String logical, String physical) {
        BiConsumer<String, String> listener = mappingListener;
        if (listener != null) {
            listener.accept(logical, physical);
        }
    }

    private static LogicalResource parse(String logicalPath) {
        if (logicalPath == null) {
            throw new IllegalArgumentException("logicalPath is null");
        }
        String trimmed = logicalPath.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("logicalPath is empty");
        }
        int colon = trimmed.indexOf(':');
        String namespace;
        String remainder;
        if (colon > 0) {
            namespace = canonicalizeNamespace(trimmed.substring(0, colon));
            remainder = trimmed.substring(colon + 1);
        } else if (colon == 0) {
            namespace = DEFAULT_NAMESPACE;
            remainder = trimmed.substring(1);
        } else {
            namespace = DEFAULT_NAMESPACE;
            remainder = trimmed;
        }
        if (namespace.isEmpty()) {
            namespace = DEFAULT_NAMESPACE;
        }
        String normalized = normalizeRelative(remainder);
        List<String> segments = split(normalized);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("logicalPath has no segments: " + logicalPath);
        }
        String canonicalRelative = join(segments);
        String fileSegment = segments.get(segments.size() - 1);
        String extension = "";
        int dot = fileSegment.lastIndexOf('.');
        if (dot >= 0) {
            extension = fileSegment.substring(dot);
        }
        return new LogicalResource(namespace, canonicalRelative, segments, extension);
    }

    private static String normalizeRelative(String input) {
        if (input == null) {
            throw new IllegalArgumentException("relative path is null");
        }
        StringBuilder builder = new StringBuilder(input.length());
        boolean prevSlash = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '\\' || ch == '/') {
                if (!prevSlash) {
                    builder.append('/');
                    prevSlash = true;
                }
            } else {
                builder.append(ch);
                prevSlash = false;
            }
        }
        String normalized = builder.toString();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("relative path is empty");
        }
        return normalized;
    }

    private static List<String> split(String path) {
        String[] rawSegments = path.split("/");
        Deque<String> normalized = new ArrayDeque<>(rawSegments.length);
        for (String segment : rawSegments) {
            if (segment == null || segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (!normalized.isEmpty()) {
                    normalized.removeLast();
                }
                continue;
            }
            normalized.add(canonicalizeSegment(segment));
        }
        return Collections.unmodifiableList(new ArrayList<>(normalized));
    }

    private static String obfuscate(LogicalResource resource) {
        List<String> segments = resource.segments;
        List<String> out = new ArrayList<>(segments.size() + 2);
        out.add("obf");
        out.add(hashToken(resource.namespace, DIR_TOKEN_LENGTH));

        StringBuilder prefix = new StringBuilder(resource.namespace);
        for (int i = 0; i < segments.size(); i++) {
            prefix.append('/').append(segments.get(i));
            boolean last = (i == segments.size() - 1);
            int length = last ? FILE_TOKEN_LENGTH : DIR_TOKEN_LENGTH;
            String token = hashToken(prefix.toString(), length);
            if (last && !resource.extension.isEmpty()) {
                token = token + resource.extension;
            }
            out.add(token);
        }
        return join(out);
    }

    private static String join(List<String> segments) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(segments.get(i));
        }
        return builder.toString();
    }

    private static String hashToken(String value, int length) {
        byte[] data = (VERSION_PREFIX + value).getBytes(StandardCharsets.UTF_8);
        Mac mac = MAC.get();
        mac.reset();
        byte[] digest = mac.doFinal(data);
        String encoded = base32(digest);
        if (encoded.length() < length) {
            throw new IllegalStateException("Encoded digest too short for " + value);
        }
        return encoded.substring(0, length);
    }

    private static String canonicalizeNamespace(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) {
            return DEFAULT_NAMESPACE;
        }
        return namespace.trim().toLowerCase(Locale.ROOT);
    }

    private static String canonicalizeSegment(String segment) {
        if (segment == null) {
            return "";
        }
        int dot = segment.lastIndexOf('.');
        if (dot < 0) {
            return segment.toLowerCase(Locale.ROOT);
        }
        String name = segment.substring(0, dot).toLowerCase(Locale.ROOT);
        String extension = segment.substring(dot);
        return name + extension;
    }

    private static String base32(byte[] data) {
        StringBuilder builder = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte datum : data) {
            buffer = (buffer << 8) | (datum & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                builder.append(BASE32[index]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            builder.append(BASE32[index]);
        }
        return builder.toString();
    }

    private static Mac createMac() {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(KEY_BYTES, HMAC_ALGORITHM));
            return mac;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to init HMAC", ex);
        }
    }

    private static final class LogicalResource {
        private final String namespace;
        private final String relativePath;
        private final List<String> segments;
        private final String extension;

        private LogicalResource(String namespace, String relativePath, List<String> segments, String extension) {
            this.namespace = namespace;
            this.relativePath = relativePath;
            this.segments = segments;
            this.extension = extension;
        }

        private String logicalString() {
            return namespace + ":" + relativePath;
        }
    }
}
