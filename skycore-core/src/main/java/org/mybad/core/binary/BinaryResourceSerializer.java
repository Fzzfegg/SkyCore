package org.mybad.core.binary;

import java.io.IOException;

/**
 * Defines how a specific resource converts to/from binary payload bytes.
 */
public interface BinaryResourceSerializer<T> {
    BinaryResourceType getType();

    int getVersion();

    void write(BinaryDataWriter writer, T value) throws IOException;

    T read(BinaryDataReader reader) throws IOException;
}
