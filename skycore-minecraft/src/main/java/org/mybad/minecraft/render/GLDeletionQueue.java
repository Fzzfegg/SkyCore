package org.mybad.minecraft.render;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * OpenGL 资源延迟删除队列。
 * 在渲染线程 flush，避免在非 GL 线程直接删除资源。
 */
public final class GLDeletionQueue {
    private static final Deque<Integer> buffers = new ArrayDeque<>();
    private static final Deque<Integer> vertexArrays = new ArrayDeque<>();
    private static final Deque<Integer> programs = new ArrayDeque<>();

    private GLDeletionQueue() {
    }

    public static void enqueueBuffer(int id) {
        if (id <= 0) {
            return;
        }
        synchronized (buffers) {
            buffers.addLast(id);
        }
    }

    public static void enqueueVertexArray(int id) {
        if (id <= 0) {
            return;
        }
        synchronized (vertexArrays) {
            vertexArrays.addLast(id);
        }
    }

    public static void enqueueProgram(int id) {
        if (id <= 0) {
            return;
        }
        synchronized (programs) {
            programs.addLast(id);
        }
    }

    public static void flush() {
        flushBuffers();
        flushVertexArrays();
        flushPrograms();
    }

    private static void flushBuffers() {
        synchronized (buffers) {
            while (!buffers.isEmpty()) {
                GL15.glDeleteBuffers(buffers.pollFirst());
            }
        }
    }

    private static void flushVertexArrays() {
        synchronized (vertexArrays) {
            while (!vertexArrays.isEmpty()) {
                GL30.glDeleteVertexArrays(vertexArrays.pollFirst());
            }
        }
    }

    private static void flushPrograms() {
        synchronized (programs) {
            while (!programs.isEmpty()) {
                GL20.glDeleteProgram(programs.pollFirst());
            }
        }
    }
}
