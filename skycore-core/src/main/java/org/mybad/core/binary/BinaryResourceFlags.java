package org.mybad.core.binary;

/**
 * Bit flags stored in the 16-bit header field.
 */
public final class BinaryResourceFlags {
    private BinaryResourceFlags() {}

    /** Payload has been compressed (algorithm negotiated elsewhere). */
    public static final int COMPRESSED = 0x0001;
    /** Payload encrypted flag. */
    public static final int ENCRYPTED = 0x0002;
    /** Payload contains authentication tag (HMAC/GCM). */
    public static final int AUTH_TAG = 0x0004;

    /** Bits reserved for encryption algorithm identifier. */
    public static final int ALGO_MASK = 0x00F0;
    public static final int ALGO_NONE = 0x0000;
    public static final int ALGO_AES_CTR = 0x0010;
    public static final int ALGO_AES_GCM = 0x0020;
}
