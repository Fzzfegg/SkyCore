package org.mybad.core.binary;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * AES-CTR implementation with 16-byte IV and no auth tag.
 */
public final class AesCtrBinaryCipher implements BinaryPayloadCipher {
    private static final String TRANSFORMATION = "AES/CTR/NoPadding";
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKeySpec keySpec;

    public AesCtrBinaryCipher(byte[] keyBytes) {
        Objects.requireNonNull(keyBytes, "keyBytes");
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public BinaryCipherResult encrypt(byte[] plain) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        byte[] iv = new byte[16];
        RNG.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        byte[] encrypted = cipher.doFinal(plain);
        return new BinaryCipherResult(encrypted, iv, null);
    }

    @Override
    public byte[] decrypt(byte[] payload, byte[] iv, byte[] mac) throws GeneralSecurityException {
        if (iv == null || iv.length != 16) {
            throw new GeneralSecurityException("Invalid IV for AES-CTR");
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(payload);
    }

    @Override
    public int algorithmFlags() {
        return BinaryResourceFlags.ALGO_AES_CTR;
    }

    @Override
    public int ivLength() {
        return 16;
    }

    @Override
    public int macLength() {
        return 0;
    }
}
