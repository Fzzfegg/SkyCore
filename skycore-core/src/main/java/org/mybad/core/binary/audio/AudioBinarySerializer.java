package org.mybad.core.binary.audio;

import org.mybad.core.binary.BinaryDataReader;
import org.mybad.core.binary.BinaryDataWriter;

import java.io.IOException;

/**
 * Serializer for audio payloads (e.g., OGG).
 */
public final class AudioBinarySerializer {
    private static final int VERSION = 1;

    public int getVersion() {
        return VERSION;
    }

    public void write(BinaryDataWriter writer, byte[] audioBytes) throws IOException {
        writer.writeBytes(audioBytes);
    }

    public byte[] read(BinaryDataReader reader) throws IOException {
        return reader.readBytes();
    }
}
