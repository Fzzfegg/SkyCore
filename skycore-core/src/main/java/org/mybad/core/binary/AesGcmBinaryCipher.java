package org.mybad.core.binary;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * AES-GCM implementation with 12-byte IV and 16-byte auth tag.
 */
public final class AesGcmBinaryCipher implements BinaryPayloadCipher {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final SecureRandom RNG = new SecureRandom();
    private static final int TAG_BITS = 128;

    private final SecretKeySpec keySpec;

    public AesGcmBinaryCipher(byte[] keyBytes) {
        Objects.requireNonNull(keyBytes, "keyBytes");
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public BinaryCipherResult encrypt(byte[] plain) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv = new byte[12];
        RNG.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
        byte[] cipherText = cipher.doFinal(plain);
        return new BinaryCipherResult(cipherText, iv, null);
    }

    @Override
    public byte[] decrypt(byte[] payload, byte[] iv, byte[] mac) throws GeneralSecurityException {
        if (iv == null || iv.length != 12) {
            throw new GeneralSecurityException("Invalid IV for AES-GCM");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
        return cipher.doFinal(payload);
    }

    @Override
    public int algorithmFlags() {
        return BinaryResourceFlags.ALGO_AES_GCM;
    }

    @Override
    public int ivLength() {
        return 12;
    }

    @Override
    public int macLength() {
        // Auth tag is embedded into ciphertext for GCM
        return 0;
    }
}
