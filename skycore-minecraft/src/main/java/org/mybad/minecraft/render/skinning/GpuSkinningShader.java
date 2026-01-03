package org.mybad.minecraft.render.skinning;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.GLDeletionQueue;

/**
 * GPU 蒙皮顶点着色器（SSBO 输出）。
 */
public final class GpuSkinningShader {
    public static final int JOINT_MATS_BINDING = 0;
    public static final int VERTEX_BUFFER_BINDING = 3;

    private static int programId = -1;
    private static int refCount = 0;

    private GpuSkinningShader() {
    }

    public static void acquire() {
        if (!GpuSkinningSupport.isGpuSkinningAvailable()) {
            return;
        }
        refCount++;
        ensureProgram();
    }

    public static void release() {
        if (refCount <= 0) {
            return;
        }
        refCount--;
        if (refCount == 0) {
            enqueueDeleteProgram();
        }
    }

    public static void releaseAll() {
        refCount = 0;
        enqueueDeleteProgram();
    }

    public static void use() {
        if (!GpuSkinningSupport.isGpuSkinningAvailable()) {
            return;
        }
        ensureProgram();
        if (programId != -1) {
            GL20.glUseProgram(programId);
        }
    }

    public static void stop() {
        GL20.glUseProgram(0);
    }

    private static void ensureProgram() {
        if (programId != -1) {
            return;
        }

        String shaderCode = shaderSource();
        int vertexShaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShaderId, shaderCode);
        GL20.glCompileShader(vertexShaderId);
        if (GL20.glGetShaderi(vertexShaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            SkyCoreMod.LOGGER.error("[SkyCore] GPU skinning vertex shader compile failed: {}", GL20.glGetShaderInfoLog(vertexShaderId, Short.MAX_VALUE));
            throw new RuntimeException("GPU skinning vertex shader compilation failed.");
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShaderId);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertexShaderId);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            SkyCoreMod.LOGGER.error("[SkyCore] GPU skinning shader link failed: {}", GL20.glGetProgramInfoLog(program, Short.MAX_VALUE));
            throw new RuntimeException("GPU skinning shader link failed.");
        }
        programId = program;
    }

    private static void enqueueDeleteProgram() {
        int program = programId;
        programId = -1;
        if (program != -1) {
            GLDeletionQueue.enqueueProgram(program);
        }
    }

    private static String shaderSource() {
        return "#version 430\n"
            + "\n"
            + "layout (location = 0) in vec3 v_pos;\n"
            + "layout (location = 1) in vec2 v_tex;\n"
            + "layout (location = 2) in vec3 v_normal;\n"
            + "layout (location = 3) in vec4 v_joint;\n"
            + "layout (location = 4) in vec4 v_weight;\n"
            + "layout (location = 5) in float v_index;\n"
            + "\n"
            + "layout (std430, binding = 0) buffer JointMatsBuffer {\n"
            + "    readonly mat4 joint_mats[];\n"
            + "};\n"
            + "\n"
            + "layout (std430, binding = 3) buffer VertexBuffer {\n"
            + "    writeonly float v_buffer[];\n"
            + "};\n"
            + "\n"
            + "void main() {\n"
            + "    uint j0 = uint(v_joint.x);\n"
            + "    uint j1 = uint(v_joint.y);\n"
            + "    uint j2 = uint(v_joint.z);\n"
            + "    uint j3 = uint(v_joint.w);\n"
            + "\n"
            + "    mat4 skinMatrix = v_weight.x * joint_mats[j0]\n"
            + "        + v_weight.y * joint_mats[j1]\n"
            + "        + v_weight.z * joint_mats[j2]\n"
            + "        + v_weight.w * joint_mats[j3];\n"
            + "\n"
            + "    vec3 outPos = (skinMatrix * vec4(v_pos, 1.0)).xyz;\n"
            + "    vec3 outNormal = mat3(skinMatrix) * v_normal;\n"
            + "    vec2 outTex = v_tex;\n"
            + "\n"
            + "    uint v_offset = uint(v_index) * 8u;\n"
            + "    v_buffer[v_offset + 0u] = outPos.x;\n"
            + "    v_buffer[v_offset + 1u] = outPos.y;\n"
            + "    v_buffer[v_offset + 2u] = outPos.z;\n"
            + "    v_buffer[v_offset + 3u] = outNormal.x;\n"
            + "    v_buffer[v_offset + 4u] = outNormal.y;\n"
            + "    v_buffer[v_offset + 5u] = outNormal.z;\n"
            + "    v_buffer[v_offset + 6u] = outTex.x;\n"
            + "    v_buffer[v_offset + 7u] = outTex.y;\n"
            + "\n"
            + "    gl_Position = vec4(outPos, 1.0);\n"
            + "}";
    }
}
