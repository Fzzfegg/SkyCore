package org.mybad.minecraft.render;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import java.nio.FloatBuffer;

/**
 * GPU 蒙皮网格：输入 VBO + 输出 SSBO + VAO。
 */
public final class SkinnedMesh {
    private static final int INPUT_STRIDE_FLOATS = 17;
    private static final int OUTPUT_STRIDE_FLOATS = 8;

    private final int vertexCount;
    private final int vboInput;
    private final int vaoInput;
    private final int ssboOutput;
    private final int vaoOutput;
    private final int jointSsbo;
    private boolean initialized;

    public SkinnedMesh(FloatBuffer inputBuffer, int vertexCount, int jointCount) {
        this.vertexCount = vertexCount;

        this.vboInput = GL15.glGenBuffers();
        this.vaoInput = GL30.glGenVertexArrays();
        this.ssboOutput = GL15.glGenBuffers();
        this.vaoOutput = GL30.glGenVertexArrays();
        this.jointSsbo = GL15.glGenBuffers();

        initInputVao(inputBuffer);
        initOutputVao();
        initJointBuffer(jointCount);
        this.initialized = true;
    }

    private void initInputVao(FloatBuffer inputBuffer) {
        GL30.glBindVertexArray(vaoInput);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboInput);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, inputBuffer, GL15.GL_STATIC_DRAW);

        int stride = INPUT_STRIDE_FLOATS * Float.BYTES;
        int offset = 0;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, offset);
        offset += 3 * Float.BYTES;
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, offset);
        offset += 2 * Float.BYTES;
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, stride, offset);
        offset += 3 * Float.BYTES;
        GL20.glEnableVertexAttribArray(3);
        GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, offset);
        offset += 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(4);
        GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, offset);
        offset += 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(5);
        GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, stride, offset);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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
        GL30.glBindVertexArray(vaoInput);
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
}
