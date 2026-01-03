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
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssboOutput);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, outputBytes, GL15.GL_DYNAMIC_COPY);
        int glError = GL11.glGetError();
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        if (glError != GL11.GL_NO_ERROR) {
            GpuSkinningSupport.disableGpuSkinning("SSBO allocation failed (GL error " + glError + ")");
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
