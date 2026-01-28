package org.mybad.core.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Helper for writing and reading SkyCore binary archives.
 */
public final class BinaryResourceIO {
    private BinaryResourceIO() {}

    public interface CipherResolver {
        BinaryPayloadCipher resolve(int flags);
    }

    public static byte[] write(BinaryResourceType type,
                               int version,
                               int flags,
                               byte[] payload,
                               BinaryPayloadCipher cipher) throws IOException, GeneralSecurityException {
        BinaryPayloadCipher effectiveCipher = cipher == null ? BinaryPayloadCipher.NO_OP : cipher;
        byte[] plainPayload = payload == null ? new byte[0] : payload;
        BinaryPayloadCipher.BinaryCipherResult result = effectiveCipher.encrypt(plainPayload);
        int headerFlags = flags & 0xFFFF;
        int algoFlags = effectiveCipher.algorithmFlags();
        if (algoFlags != BinaryResourceFlags.ALGO_NONE) {
            headerFlags |= BinaryResourceFlags.ENCRYPTED;
            headerFlags |= algoFlags;
        }
        if (effectiveCipher.macLength() > 0) {
            headerFlags |= BinaryResourceFlags.AUTH_TAG;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(buffer);
        output.writeInt(type.getMagic());
        output.writeShort(version & 0xFFFF);
        output.writeShort(headerFlags & 0xFFFF);
        int originalSize = plainPayload.length;
        output.writeInt(originalSize);
        byte[] encryptedPayload = result.payload() == null ? new byte[0] : result.payload();
        output.write(encryptedPayload);
        if (result.iv() != null) {
            output.write(result.iv());
        }
        if (result.mac() != null) {
            output.write(result.mac());
        }
        output.flush();
        return buffer.toByteArray();
    }

    public static SkycoreBinaryArchive read(byte[] data,
                                            CipherResolver resolver) throws IOException, GeneralSecurityException {
        if (data == null || data.length < BinaryResourceHeader.HEADER_SIZE) {
            throw new IOException("Corrupted binary archive");
        }
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        int magic = input.readInt();
        BinaryResourceType type = BinaryResourceType.fromMagic(magic);
        int version = Short.toUnsignedInt(input.readShort());
        int flags = Short.toUnsignedInt(input.readShort());
        int originalSize = input.readInt();
        BinaryResourceHeader header = new BinaryResourceHeader(type, version, flags, originalSize);

        int headerSize = BinaryResourceHeader.HEADER_SIZE;
        int remaining = data.length - headerSize;
        BinaryPayloadCipher cipher = resolver == null ? BinaryPayloadCipher.NO_OP : resolver.resolve(flags);
        int ivLength = cipher.ivLength();
        int macLength = cipher.macLength();
        int payloadLength = remaining - ivLength - macLength;
        if (payloadLength < 0) {
            throw new IOException("Invalid archive sections");
        }
        byte[] payload = new byte[payloadLength];
        input.readFully(payload);
        byte[] iv = ivLength > 0 ? new byte[ivLength] : null;
        if (iv != null) {
            input.readFully(iv);
        }
        byte[] mac = macLength > 0 ? new byte[macLength] : null;
        if (mac != null) {
            input.readFully(mac);
        }
        byte[] plain = cipher.decrypt(payload, iv, mac);
        return new SkycoreBinaryArchive(header, plain);
    }

    public static SkycoreBinaryArchive read(byte[] data) throws IOException, GeneralSecurityException {
        return read(data, flags -> BinaryPayloadCipher.NO_OP);
    }

    public static byte[] slicePayload(byte[] data) {
        if (data == null || data.length < BinaryResourceHeader.HEADER_SIZE) {
            return new byte[0];
        }
        return Arrays.copyOfRange(data, BinaryResourceHeader.HEADER_SIZE, data.length);
    }
}
