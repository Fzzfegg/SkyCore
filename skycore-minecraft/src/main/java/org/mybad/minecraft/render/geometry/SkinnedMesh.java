package org.mybad.minecraft.render.geometry;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.mybad.minecraft.render.GLDeletionQueue;
import org.mybad.minecraft.render.skinning.GpuSkinningShader;
import org.mybad.minecraft.render.skinning.GpuSkinningSupport;

import java.nio.FloatBuffer;

/**
 * GPU 蒙皮网格：共享输入几何体 + 输出 SSBO + VAO。
 */
public final class SkinnedMesh implements AutoCloseable {
    private static final int INPUT_STRIDE_FLOATS = 17;
    private static final int OUTPUT_STRIDE_FLOATS = 8;

    private final int vertexCount;
    private final SharedGeometry geometry;
    private final int ssboOutput;
    private final int vaoOutput;
    private final int jointSsbo;
    private boolean initialized;
    private boolean destroyed;

    public SkinnedMesh(SharedGeometry geometry, int jointCount) {
        this.geometry = geometry;
        this.vertexCount = geometry.getVertexCount();

        this.ssboOutput = GL15.glGenBuffers();
        this.vaoOutput = GL30.glGenVertexArrays();
        this.jointSsbo = GL15.glGenBuffers();

        initOutputVao();
        initJointBuffer(jointCount);
        this.initialized = true;
    }

    private void initOutputVao() {
        int outputBytes = vertexCount * OUTPUT_STRIDE_FLOATS * Float.BYTES;
        // Some mods leave stale GL errors in the queue; clear them before probing our own allocation.
        clearGlErrors();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboOutput);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, outputBytes, GL15.GL_DYNAMIC_COPY);
        int firstError = GL11.glGetError();
        int secondError = GL11.glGetError();
        int allocatedBytes = GL15.glGetBufferParameteri(GL43.GL_SHADER_STORAGE_BUFFER, GL15.GL_BUFFER_SIZE);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        boolean outOfMemory = firstError == GL11.GL_OUT_OF_MEMORY || secondError == GL11.GL_OUT_OF_MEMORY;
        boolean undersized = allocatedBytes < outputBytes;
        if (outOfMemory || undersized) {
            GpuSkinningSupport.disableGpuSkinning(
                "SSBO allocation failed (GL error " + firstError + ", allocated=" + allocatedBytes + ", expected=" + outputBytes + ")"
            );
        } else if (firstError != GL11.GL_NO_ERROR || secondError != GL11.GL_NO_ERROR) {
            // Ignore non-fatal GL noise from other renderers; do not globally disable skinning.
            org.mybad.minecraft.SkyCoreMod.LOGGER.debug(
                "[SkyCore] Ignored non-fatal GL error after SSBO upload: first={}, second={}, allocated={}, expected={}",
                firstError,
                secondError,
                allocatedBytes,
                outputBytes
            );
        }

        GL30.glBindVertexArray(vaoOutput);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ssboOutput);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, OUTPUT_STRIDE_FLOATS * Float.BYTES, 0);
        GL11.glNormalPointer(GL11.GL_FLOAT, OUTPUT_STRIDE_FLOATS * Float.BYTES, 3 * Float.BYTES);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, OUTPUT_STRIDE_FLOATS * Float.BYTES, 6 * Float.BYTES);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    }

    private static void clearGlErrors() {
        for (int i = 0; i < 32; i++) {
            if (GL11.glGetError() == GL11.GL_NO_ERROR) {
                return;
            }
        }
    }

    private void initJointBuffer(int jointCount) {
        int jointBytes = Math.max(1, jointCount) * 16 * Float.BYTES;
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointSsbo);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, jointBytes, GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void updateJointMatrices(FloatBuffer matrices) {
        if (!initialized) {
            return;
        }
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, jointSsbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, matrices);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void runSkinningPass() {
        if (!initialized) {
            return;
        }
        if (!GpuSkinningSupport.isGpuSkinningAvailable()) {
            return;
        }

        GpuSkinningShader.use();
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GpuSkinningShader.JOINT_MATS_BINDING, jointSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GpuSkinningShader.VERTEX_BUFFER_BINDING, ssboOutput);

        GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBindVertexArray(geometry.getVaoInput());
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL30.glBindVertexArray(0);
        GL11.glDisable(GL30.GL_RASTERIZER_DISCARD);

        GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);

        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GpuSkinningShader.JOINT_MATS_BINDING, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, GpuSkinningShader.VERTEX_BUFFER_BINDING, 0);
        GpuSkinningShader.stop();
    }

    public void draw() {
        if (!initialized) {
            return;
        }
        GL30.glBindVertexArray(vaoOutput);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public static FloatBuffer allocateInputBuffer(int vertexCount) {
        return BufferUtils.createFloatBuffer(vertexCount * INPUT_STRIDE_FLOATS);
    }

    public static int getInputStrideFloats() {
        return INPUT_STRIDE_FLOATS;
    }

    public static int getOutputStrideFloats() {
        return OUTPUT_STRIDE_FLOATS;
    }

    public void destroy() {
        close();
    }

    @Override
    public void close() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        initialized = false;

        GLDeletionQueue.enqueueBuffer(ssboOutput);
        GLDeletionQueue.enqueueBuffer(jointSsbo);
        GLDeletionQueue.enqueueVertexArray(vaoOutput);
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
