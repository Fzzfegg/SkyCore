package org.mybad.minecraft.particle.render.gpu;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.mybad.minecraft.render.GLDeletionQueue;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * 单位四边形 VAO/VBO/EBO，用于 instanced 粒子渲染。
 */
final class ParticleQuadMesh {
    private int vaoId = -1;
    private int vboId = -1;
    private int eboId = -1;

    void ensureCreated() {
        if (vaoId != -1) {
            return;
        }
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        eboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);

        float[] vertices = new float[]{
            // corner.x, corner.y, uv.x, uv.y
            -0.5f, -0.5f, 0.0f, 1.0f,
             0.5f, -0.5f, 1.0f, 1.0f,
             0.5f,  0.5f, 1.0f, 0.0f,
            -0.5f,  0.5f, 0.0f, 0.0f
        };

        int[] indices = new int[]{
            0, 1, 2,
            0, 2, 3
        };

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW);

        int stride = 4 * Float.BYTES;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, stride, 0);

        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 2L * Float.BYTES);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    void bind() {
        if (vaoId == -1) {
            return;
        }
        GL30.glBindVertexArray(vaoId);
    }

    void unbind() {
        GL30.glBindVertexArray(0);
    }

    void destroy() {
        if (vboId != -1) {
            GLDeletionQueue.enqueueBuffer(vboId);
            vboId = -1;
        }
        if (eboId != -1) {
            GLDeletionQueue.enqueueBuffer(eboId);
            eboId = -1;
        }
        if (vaoId != -1) {
            GLDeletionQueue.enqueueVertexArray(vaoId);
            vaoId = -1;
        }
    }
}
