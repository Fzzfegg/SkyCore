package org.mybad.minecraft.render.geometry;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.mybad.minecraft.render.GLDeletionQueue;

import java.nio.FloatBuffer;

public final class SharedGeometry {
    private final int vertexCount;
    private final int vboInput;
    private final int vaoInput;
    private int refCount = 1;
    private boolean destroyed;

    public SharedGeometry(FloatBuffer inputBuffer, int vertexCount) {
        this.vertexCount = vertexCount;
        this.vboInput = GL15.glGenBuffers();
        this.vaoInput = GL30.glGenVertexArrays();
        initInputVao(inputBuffer);
    }

    private void initInputVao(FloatBuffer inputBuffer) {
        GL30.glBindVertexArray(vaoInput);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboInput);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, inputBuffer, GL15.GL_STATIC_DRAW);

        int stride = SkinnedMesh.getInputStrideFloats() * Float.BYTES;
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

    public void retain() {
        if (destroyed) {
            return;
        }
        refCount++;
    }

    public boolean release() {
        if (destroyed) {
            return true;
        }
        refCount--;
        if (refCount <= 0) {
            destroyed = true;
            GLDeletionQueue.enqueueBuffer(vboInput);
            GLDeletionQueue.enqueueVertexArray(vaoInput);
            return true;
        }
        return false;
    }

    public void forceDestroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        refCount = 0;
        GLDeletionQueue.enqueueBuffer(vboInput);
        GLDeletionQueue.enqueueVertexArray(vaoInput);
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getVaoInput() {
        return vaoInput;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
