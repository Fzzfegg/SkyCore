package org.mybad.core.binary.texture;

import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;

import java.io.IOException;

/**
 * Serializer for texture payloads.
 * Stores the original image bytes (PNG) verbatim.
 */
public final class TextureBinarySerializer {
    private static final int VERSION = 1;

    public int getVersion() {
        return VERSION;
    }

    public void write(BinaryDataWriter writer, byte[] imageBytes) throws IOException {
        writer.writeBytes(imageBytes);
    }

    public byte[] read(BinaryDataReader reader) throws IOException {
        return reader.readBytes();
    }
}
