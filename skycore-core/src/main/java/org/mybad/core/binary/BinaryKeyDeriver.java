package org.mybad.core.binary;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Deterministic key derivation used by both packtool and runtime.
 */
public final class BinaryKeyDeriver {

    private BinaryKeyDeriver() {
    }

    /**
     * Derives a fixed-length key from an arbitrary seed.
     *
     * @param seed    arbitrary seed bytes (e.g. config hex)
     * @param keySize desired key size (16/24/32 etc.)
     * @return derived key
     */
    public static byte[] derive(byte[] seed, int keySize) {
        return derive(seed, keySize, (byte[]) null);
    }

    /**
     * Derives a fixed-length key from an arbitrary seed and optional context strings.
     *
     * Context allows future extensions (e.g. namespace or profile-specific salts) while
     * ensuring packtool and client stay in sync.
     */
    public static byte[] derive(byte[] seed, int keySize, String... context) {
        byte[][] extra = null;
        if (context != null && context.length > 0) {
            extra = new byte[context.length][];
            for (int i = 0; i < context.length; i++) {
                extra[i] = context[i] == null ? new byte[0] : context[i].getBytes(StandardCharsets.UTF_8);
            }
        }
        return derive(seed, keySize, extra);
    }

    private static byte[] derive(byte[] seed, int keySize, byte[]... context) {
        if (seed == null) {
            throw new IllegalArgumentException("seed must not be null");
        }
        if (keySize <= 0) {
            throw new IllegalArgumentException("keySize must be positive");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] material = new byte[0];
            byte[] result = new byte[keySize];
            int offset = 0;
            int counter = 0;
            while (offset < keySize) {
                digest.reset();
                digest.update(seed);
                if (context != null) {
                    for (byte[] extra : context) {
                        if (extra != null && extra.length > 0) {
                            digest.update(extra);
                        }
                    }
                }
                digest.update(material);
                digest.update((byte) counter++);
                material = digest.digest();
                int remaining = Math.min(material.length, keySize - offset);
                System.arraycopy(material, 0, result, offset, remaining);
                offset += remaining;
            }
            Arrays.fill(material, (byte) 0);
            return result;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not supported", ex);
        }
    }
}
