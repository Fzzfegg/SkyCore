package org.mybad.core.binary;

import java.security.GeneralSecurityException;

/**
 * Abstraction for encrypting/decrypting payload bytes inside the archive.
 */
public interface BinaryPayloadCipher {

    BinaryPayloadCipher NO_OP = new NoOpBinaryCipher();

    BinaryCipherResult encrypt(byte[] plain) throws GeneralSecurityException;

    byte[] decrypt(byte[] payload, byte[] iv, byte[] mac) throws GeneralSecurityException;

    /** Additional flag bits (excluding {@link BinaryResourceFlags#ENCRYPTED}) describing the algorithm. */
    int algorithmFlags();

    int ivLength();

    int macLength();

    final class BinaryCipherResult {
        private final byte[] payload;
        private final byte[] iv;
        private final byte[] mac;

        public BinaryCipherResult(byte[] payload, byte[] iv, byte[] mac) {
            this.payload = payload;
            this.iv = iv;
            this.mac = mac;
        }

        public byte[] payload() {
            return payload;
        }

        public byte[] iv() {
            return iv;
        }

        public byte[] mac() {
            return mac;
        }
    }
}
