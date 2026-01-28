package org.mybad.core.binary;

/**
 * Fixed-size header that prefixes every SkyCore binary archive.
 */
public final class BinaryResourceHeader {
    public static final int HEADER_SIZE = 12; // bytes

    private final BinaryResourceType type;
    private final int version;
    private final int flags;
    private final int originalSize;

    public BinaryResourceHeader(BinaryResourceType type, int version, int flags, int originalSize) {
        this.type = type == null ? BinaryResourceType.UNKNOWN : type;
        this.version = version & 0xFFFF;
        this.flags = flags & 0xFFFF;
        this.originalSize = Math.max(0, originalSize);
    }

    public BinaryResourceType getType() {
        return type;
    }

    public int getVersion() {
        return version;
    }

    public int getFlags() {
        return flags;
    }

    public int getOriginalSize() {
        return originalSize;
    }

    public boolean isCompressed() {
        return (flags & BinaryResourceFlags.COMPRESSED) != 0;
    }

    public boolean isEncrypted() {
        return (flags & BinaryResourceFlags.ENCRYPTED) != 0;
    }
}
