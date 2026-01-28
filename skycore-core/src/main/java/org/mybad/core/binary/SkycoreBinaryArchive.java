package org.mybad.core.binary;

/**
 * Holder for parsed binary archive.
 */
public final class SkycoreBinaryArchive {
    private final BinaryResourceHeader header;
    private final byte[] payload;

    public SkycoreBinaryArchive(BinaryResourceHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public BinaryResourceHeader getHeader() {
        return header;
    }

    public byte[] getPayload() {
        return payload;
    }
}
