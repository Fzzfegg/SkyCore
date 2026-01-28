package org.mybad.core.binary;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper for reading primitive types from a byte array.
 */
public final class BinaryDataReader {
    private final DataInputStream input;

    public BinaryDataReader(byte[] data) {
        this.input = new DataInputStream(new ByteArrayInputStream(data));
    }

    public boolean readBoolean() throws IOException {
        return input.readBoolean();
    }

    public byte readByte() throws IOException {
        return input.readByte();
    }

    public short readShort() throws IOException {
        return input.readShort();
    }

    public int readInt() throws IOException {
        return input.readInt();
    }

    public long readLong() throws IOException {
        return input.readLong();
    }

    public float readFloat() throws IOException {
        return input.readFloat();
    }

    public double readDouble() throws IOException {
        return input.readDouble();
    }

    public int readVarInt() throws IOException {
        int shift = 0;
        int result = 0;
        while (shift < 32) {
            int b = input.readUnsignedByte();
            result |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return decodeZigZag32(result);
            }
            shift += 7;
        }
        throw new IOException("Malformed varint");
    }

    public long readVarLong() throws IOException {
        int shift = 0;
        long result = 0;
        while (shift < 64) {
            int b = input.readUnsignedByte();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return decodeZigZag64(result);
            }
            shift += 7;
        }
        throw new IOException("Malformed varlong");
    }

    public String readString() throws IOException {
        int length = readVarInt();
        if (length <= 0) {
            return "";
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public float[] readFloatArray() throws IOException {
        int length = readVarInt();
        if (length <= 0) {
            return new float[0];
        }
        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            values[i] = readFloat();
        }
        return values;
    }

    public byte[] readBytes() throws IOException {
        int length = readVarInt();
        if (length <= 0) {
            return new byte[0];
        }
        byte[] data = new byte[length];
        input.readFully(data);
        return data;
    }

    private static int decodeZigZag32(int value) {
        return (value >>> 1) ^ -(value & 1);
    }

    private static long decodeZigZag64(long value) {
        return (value >>> 1) ^ -(value & 1L);
    }
}
