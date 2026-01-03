package org.mybad.minecraft.particle.render.gpu;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL30;
import org.mybad.minecraft.render.GLDeletionQueue;

import java.nio.FloatBuffer;

/**
 * 粒子 SSBO 管理（粒子 + 发射器）。
 */
final class ParticleSsboBuffer {
    static final int PARTICLE_BINDING = 0;
    static final int EMITTER_BINDING = 1;

    private int particleSsbo = -1;
    private int emitterSsbo = -1;
    private int particleCapacityBytes = 0;
    private int emitterCapacityBytes = 0;

    void ensureCreated() {
        if (particleSsbo != -1) {
            return;
        }
        particleSsbo = GL15.glGenBuffers();
        emitterSsbo = GL15.glGenBuffers();
    }

    void uploadParticles(FloatBuffer data, int bytes) {
        if (particleSsbo == -1) {
            return;
        }
        if (bytes <= 0) {
            return;
        }
        int capacity = ensureCapacity(bytes, true);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, particleSsbo);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, capacity, GL15.GL_STREAM_DRAW);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, data);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    void uploadEmitters(FloatBuffer data, int bytes) {
        if (emitterSsbo == -1) {
            return;
        }
        if (bytes <= 0) {
            return;
        }
        int capacity = ensureCapacity(bytes, false);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, emitterSsbo);
        GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, capacity, GL15.GL_STREAM_DRAW);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, data);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    void bind() {
        if (particleSsbo == -1 || emitterSsbo == -1) {
            return;
        }
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, PARTICLE_BINDING, particleSsbo);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, EMITTER_BINDING, emitterSsbo);
    }

    void unbind() {
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, PARTICLE_BINDING, 0);
        GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, EMITTER_BINDING, 0);
    }

    void destroy() {
        if (particleSsbo != -1) {
            GLDeletionQueue.enqueueBuffer(particleSsbo);
            particleSsbo = -1;
        }
        if (emitterSsbo != -1) {
            GLDeletionQueue.enqueueBuffer(emitterSsbo);
            emitterSsbo = -1;
        }
        particleCapacityBytes = 0;
        emitterCapacityBytes = 0;
    }

    private int ensureCapacity(int bytes, boolean particle) {
        int current = particle ? particleCapacityBytes : emitterCapacityBytes;
        if (bytes > current) {
            int newCapacity = Math.max(bytes, current * 2);
            if (particle) {
                particleCapacityBytes = newCapacity;
            } else {
                emitterCapacityBytes = newCapacity;
            }
            return newCapacity;
        }
        return current;
    }
}
