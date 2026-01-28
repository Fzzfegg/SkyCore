package org.mybad.core.binary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Supported resource kinds for SkyCore binary archives.
 */
public enum BinaryResourceType {
    MODEL("SKM\0"),
    ANIMATION("SKA\0"),
    PARTICLE("SKP\0"),
    TEXTURE("SKT\0"),
    AUDIO("SKO\0"),
    UNKNOWN("SKX\0");

    private final int magic;

    BinaryResourceType(String magicText) {
        byte[] bytes = magicText.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length != 4) {
            throw new IllegalArgumentException("Magic text must be 4 bytes");
        }
        this.magic = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public int getMagic() {
        return magic;
    }

    public static BinaryResourceType fromMagic(int magic) {
        for (BinaryResourceType type : values()) {
            if (type.magic == magic) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
