package org.mybad.minecraft.particle.render.gpu;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

final class BufferUtilsHelper {
    private static final FloatBuffer EMPTY_FLOAT_BUFFER = BufferUtils.createFloatBuffer(0);

    private BufferUtilsHelper() {
    }

    static FloatBuffer createFloatBuffer(int capacity) {
        if (capacity <= 0) {
            return BufferUtils.createFloatBuffer(0);
        }
        return BufferUtils.createFloatBuffer(capacity);
    }

    static FloatBuffer emptyFloatBuffer() {
        return EMPTY_FLOAT_BUFFER;
    }
}
