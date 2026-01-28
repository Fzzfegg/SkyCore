package org.mybad.core.binary;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper for writing primitive types into a growable byte buffer.
 */
public final class BinaryDataWriter {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final DataOutputStream output = new DataOutputStream(buffer);

    public void writeBoolean(boolean value) throws IOException {
        output.writeBoolean(value);
    }

    public void writeByte(int value) throws IOException {
        output.writeByte(value);
    }

    public void writeShort(int value) throws IOException {
        output.writeShort(value);
    }

    public void writeInt(int value) throws IOException {
        output.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
        output.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
        output.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
        output.writeDouble(value);
    }

    public void writeVarInt(int value) throws IOException {
        int v = encodeZigZag32(value);
        while ((v & ~0x7F) != 0) {
            output.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        output.writeByte(v);
    }

    public void writeVarLong(long value) throws IOException {
        long v = encodeZigZag64(value);
        while ((v & ~0x7FL) != 0) {
            output.writeByte((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        output.writeByte((int) v);
    }

    public void writeString(String text) throws IOException {
        if (text == null) {
            writeVarInt(0);
            return;
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        output.write(bytes);
    }

    public void writeFloatArray(float[] values) throws IOException {
        if (values == null) {
            writeVarInt(0);
            return;
        }
        writeVarInt(values.length);
        for (float v : values) {
            writeFloat(v);
        }
    }

    public void writeBytes(byte[] data) throws IOException {
        if (data == null) {
            writeVarInt(0);
            return;
        }
        writeVarInt(data.length);
        output.write(data);
    }

    public byte[] toByteArray() throws IOException {
        output.flush();
        return buffer.toByteArray();
    }

    public int size() {
        return buffer.size();
    }

    private static int encodeZigZag32(int value) {
        return (value << 1) ^ (value >> 31);
    }

    private static long encodeZigZag64(long value) {
        return (value << 1) ^ (value >> 63);
    }
}
