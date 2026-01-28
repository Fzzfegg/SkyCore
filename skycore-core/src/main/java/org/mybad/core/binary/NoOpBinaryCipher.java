package org.mybad.core.binary;

import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Default cipher that performs no encryption.
 */
final class NoOpBinaryCipher implements BinaryPayloadCipher {

    @Override
    public BinaryCipherResult encrypt(byte[] plain) {
        byte[] copy = plain == null ? new byte[0] : Arrays.copyOf(plain, plain.length);
        return new BinaryCipherResult(copy, null, null);
    }

    @Override
    public byte[] decrypt(byte[] payload, byte[] iv, byte[] mac) {
        return payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
    }

    @Override
    public int algorithmFlags() {
        return BinaryResourceFlags.ALGO_NONE;
    }

    @Override
    public int ivLength() {
        return 0;
    }

    @Override
    public int macLength() {
        return 0;
    }
}
