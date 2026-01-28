package org.mybad.core.binary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves cipher implementations using flag bits stored in the header.
 */
public final class BinaryPayloadCipherRegistry implements BinaryResourceIO.CipherResolver {
    private final Map<Integer, BinaryPayloadCipher> registry = new ConcurrentHashMap<>();

    public BinaryPayloadCipherRegistry register(BinaryPayloadCipher cipher) {
        if (cipher == null) {
            return this;
        }
        registry.put(cipher.algorithmFlags(), cipher);
        return this;
    }

    public BinaryPayloadCipher resolve(int flags) {
        int algoBits = flags & BinaryResourceFlags.ALGO_MASK;
        BinaryPayloadCipher cipher = registry.get(algoBits);
        if (cipher != null) {
            return cipher;
        }
        if (algoBits == BinaryResourceFlags.ALGO_NONE) {
            return BinaryPayloadCipher.NO_OP;
        }
        // fallback to no-op but caller should treat as error
        return BinaryPayloadCipher.NO_OP;
    }

    public static BinaryPayloadCipherRegistry withDefaults() {
        BinaryPayloadCipherRegistry registry = new BinaryPayloadCipherRegistry();
        registry.register(BinaryPayloadCipher.NO_OP);
        return registry;
    }

    public synchronized void setActiveCipher(BinaryPayloadCipher cipher) {
        registry.clear();
        registry.put(BinaryPayloadCipher.NO_OP.algorithmFlags(), BinaryPayloadCipher.NO_OP);
        if (cipher != null && cipher != BinaryPayloadCipher.NO_OP) {
            registry.put(cipher.algorithmFlags(), cipher);
        }
    }
}
