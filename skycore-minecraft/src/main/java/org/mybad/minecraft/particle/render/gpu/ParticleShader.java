package org.mybad.minecraft.particle.render.gpu;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.GLDeletionQueue;

import java.nio.FloatBuffer;

/**
 * 粒子 GPU 渲染着色器（SSBO + instancing）。
 */
final class ParticleShader {
    private final boolean lit;
    private int programId = -1;

    private int uViewProj = -1;
    private int uCameraPos = -1;
    private int uCameraRight = -1;
    private int uCameraUp = -1;
    private int uCameraOffset = -1;
    private int uFogColor = -1;
    private int uFogStart = -1;
    private int uFogEnd = -1;
    private int uFogEnabled = -1;
    private int uTexture = -1;
    private int uLightmap = -1;
    private int uInstanceOffset = -1;

    ParticleShader(boolean lit) {
        this.lit = lit;
    }

    void ensureProgram() {
        if (programId != -1) {
            return;
        }
        int vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertexSource());
        int fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource());

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShaderId);
        GL20.glAttachShader(program, fragmentShaderId);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertexShaderId);
        GL20.glDeleteShader(fragmentShaderId);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            SkyCoreMod.LOGGER.error("[SkyCore] Particle shader link failed: {}", GL20.glGetProgramInfoLog(program, Short.MAX_VALUE));
            throw new RuntimeException("Particle shader link failed.");
        }

        programId = program;
        uViewProj = GL20.glGetUniformLocation(programId, "u_viewProj");
        uCameraPos = GL20.glGetUniformLocation(programId, "u_cameraPos");
        uCameraRight = GL20.glGetUniformLocation(programId, "u_cameraRight");
        uCameraUp = GL20.glGetUniformLocation(programId, "u_cameraUp");
        uCameraOffset = GL20.glGetUniformLocation(programId, "u_cameraOffset");
        uFogColor = GL20.glGetUniformLocation(programId, "u_fogColor");
        uFogStart = GL20.glGetUniformLocation(programId, "u_fogStart");
        uFogEnd = GL20.glGetUniformLocation(programId, "u_fogEnd");
        uFogEnabled = GL20.glGetUniformLocation(programId, "u_fogEnabled");
        uTexture = GL20.glGetUniformLocation(programId, "u_texture");
        uLightmap = GL20.glGetUniformLocation(programId, "u_lightmap");
        uInstanceOffset = GL20.glGetUniformLocation(programId, "u_instanceOffset");

        GL20.glUseProgram(programId);
        if (uTexture != -1) {
            GL20.glUniform1i(uTexture, 0);
        }
        if (uLightmap != -1) {
            GL20.glUniform1i(uLightmap, 1);
        }
        if (uInstanceOffset != -1) {
            GL20.glUniform1i(uInstanceOffset, 0);
        }
        GL20.glUseProgram(0);
    }

    void use() {
        ensureProgram();
        if (programId != -1) {
            GL20.glUseProgram(programId);
        }
    }

    void stop() {
        GL20.glUseProgram(0);
    }

    void destroy() {
        if (programId != -1) {
            GLDeletionQueue.enqueueProgram(programId);
            programId = -1;
        }
    }

    void setViewProj(FloatBuffer matrix) {
        if (uViewProj != -1) {
            GL20.glUniformMatrix4(uViewProj, false, matrix);
        }
    }

    void setCamera(float x, float y, float z, float rightX, float rightY, float rightZ, float upX, float upY, float upZ) {
        if (uCameraPos != -1) {
            GL20.glUniform3f(uCameraPos, x, y, z);
        }
        if (uCameraRight != -1) {
            GL20.glUniform3f(uCameraRight, rightX, rightY, rightZ);
        }
        if (uCameraUp != -1) {
            GL20.glUniform3f(uCameraUp, upX, upY, upZ);
        }
    }

    void setCameraOffset(float x, float y, float z) {
        if (uCameraOffset != -1) {
            GL20.glUniform3f(uCameraOffset, x, y, z);
        }
    }

    void setFog(float r, float g, float b, float start, float end, boolean enabled) {
        if (uFogColor != -1) {
            GL20.glUniform3f(uFogColor, r, g, b);
        }
        if (uFogStart != -1) {
            GL20.glUniform1f(uFogStart, start);
        }
        if (uFogEnd != -1) {
            GL20.glUniform1f(uFogEnd, end);
        }
        if (uFogEnabled != -1) {
            GL20.glUniform1i(uFogEnabled, enabled ? 1 : 0);
        }
    }

    void setInstanceOffset(int offset) {
        if (uInstanceOffset != -1) {
            GL20.glUniform1i(uInstanceOffset, offset);
        }
    }

    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);
        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            SkyCoreMod.LOGGER.error("[SkyCore] Particle shader compile failed: {}", GL20.glGetShaderInfoLog(shaderId, Short.MAX_VALUE));
            throw new RuntimeException("Particle shader compilation failed.");
        }
        return shaderId;
    }

    private String vertexSource() {
        return "#version 430\n"
            + "\n"
            + "layout(location = 0) in vec2 a_corner;\n"
            + "layout(location = 1) in vec2 a_uv;\n"
            + "\n"
            + "struct ParticleData {\n"
            + "    vec4 pos_roll;\n"
            + "    vec4 size_mode;\n"
            + "    vec4 color;\n"
            + "    vec4 uv;\n"
            + "    vec4 dir;\n"
            + "    vec4 extra;\n"
            + "};\n"
            + "\n"
            + "struct EmitterData {\n"
            + "    vec4 basisX;\n"
            + "    vec4 basisY;\n"
            + "    vec4 basisZ;\n"
            + "    vec4 padding;\n"
            + "};\n"
            + "\n"
            + "layout(std430, binding = 0) buffer ParticleBuffer {\n"
            + "    ParticleData particles[];\n"
            + "};\n"
            + "\n"
            + "layout(std430, binding = 1) buffer EmitterBuffer {\n"
            + "    EmitterData emitters[];\n"
            + "};\n"
            + "\n"
            + "uniform mat4 u_viewProj;\n"
            + "uniform vec3 u_cameraPos;\n"
            + "uniform vec3 u_cameraRight;\n"
            + "uniform vec3 u_cameraUp;\n"
            + "uniform vec3 u_cameraOffset;\n"
            + "uniform int u_instanceOffset;\n"
            + "\n"
            + "out vec2 v_uv;\n"
            + "out vec4 v_color;\n"
            + "out vec2 v_lightUV;\n"
            + "out float v_fogDist;\n"
            + "\n"
            + "const int MODE_ROTATE_XYZ = 0;\n"
            + "const int MODE_ROTATE_Y = 1;\n"
            + "const int MODE_LOOK_AT_XYZ = 2;\n"
            + "const int MODE_LOOK_AT_Y = 3;\n"
            + "const int MODE_DIRECTION_X = 4;\n"
            + "const int MODE_DIRECTION_Y = 5;\n"
            + "const int MODE_DIRECTION_Z = 6;\n"
            + "const int MODE_LOOKAT_DIRECTION = 7;\n"
            + "const int MODE_EMITTER_XY = 8;\n"
            + "const int MODE_EMITTER_XZ = 9;\n"
            + "const int MODE_EMITTER_YZ = 10;\n"
            + "\n"
            + "vec3 safeNormalize(vec3 v) {\n"
            + "    float len = length(v);\n"
            + "    if (len < 1e-6) {\n"
            + "        return vec3(0.0, 0.0, 0.0);\n"
            + "    }\n"
            + "    return v / len;\n"
            + "}\n"
            + "\n"
            + "mat3 rotX(float a) {\n"
            + "    float c = cos(a);\n"
            + "    float s = sin(a);\n"
            + "    return mat3(\n"
            + "        1.0, 0.0, 0.0,\n"
            + "        0.0, c, -s,\n"
            + "        0.0, s, c\n"
            + "    );\n"
            + "}\n"
            + "\n"
            + "mat3 rotY(float a) {\n"
            + "    float c = cos(a);\n"
            + "    float s = sin(a);\n"
            + "    return mat3(\n"
            + "        c, 0.0, s,\n"
            + "        0.0, 1.0, 0.0,\n"
            + "        -s, 0.0, c\n"
            + "    );\n"
            + "}\n"
            + "\n"
            + "mat3 rotZ(float a) {\n"
            + "    float c = cos(a);\n"
            + "    float s = sin(a);\n"
            + "    return mat3(\n"
            + "        c, -s, 0.0,\n"
            + "        s, c, 0.0,\n"
            + "        0.0, 0.0, 1.0\n"
            + "    );\n"
            + "}\n"
            + "\n"
            + "void buildFacingFromNormal(vec3 normal, out vec3 right, out vec3 up) {\n"
            + "    vec3 worldUp = vec3(0.0, 1.0, 0.0);\n"
            + "    if (abs(dot(normal, worldUp)) > 0.99) {\n"
            + "        worldUp = vec3(1.0, 0.0, 0.0);\n"
            + "    }\n"
            + "    right = safeNormalize(cross(worldUp, normal));\n"
            + "    up = safeNormalize(cross(normal, right));\n"
            + "}\n"
            + "\n"
            + "float getYaw(vec3 dir) {\n"
            + "    return -atan(-dir.x, dir.z);\n"
            + "}\n"
            + "\n"
            + "float getPitch(vec3 dir) {\n"
            + "    return -atan(dir.y, length(vec2(dir.x, dir.z)));\n"
            + "}\n"
            + "\n"
            + "void main() {\n"
            + "    ParticleData p = particles[gl_InstanceID + u_instanceOffset];\n"
            + "    vec3 worldPos = p.pos_roll.xyz;\n"
            + "    vec3 pos = worldPos - u_cameraOffset;\n"
            + "    float rollRad = radians(p.pos_roll.w);\n"
            + "    vec2 size = p.size_mode.xy;\n"
            + "    int mode = int(p.size_mode.z + 0.5);\n"
            + "    vec3 dir = p.dir.xyz;\n"
            + "\n"
            + "    vec3 right = vec3(1.0, 0.0, 0.0);\n"
            + "    vec3 up = vec3(0.0, 1.0, 0.0);\n"
            + "    vec3 normal = vec3(0.0, 0.0, 1.0);\n"
            + "    vec3 camRight = safeNormalize(u_cameraRight);\n"
            + "    vec3 camUp = safeNormalize(u_cameraUp);\n"
            + "    vec3 camForward = safeNormalize(cross(camRight, camUp));\n"
            + "\n"
            + "    if (mode == MODE_ROTATE_XYZ) {\n"
            + "        right = camRight;\n"
            + "        up = camUp;\n"
            + "        normal = camForward;\n"
            + "    } else if (mode == MODE_ROTATE_Y) {\n"
            + "        vec3 fwd = vec3(camForward.x, 0.0, camForward.z);\n"
            + "        normal = safeNormalize(fwd);\n"
            + "        if (length(normal) < 1e-6) {\n"
            + "            normal = vec3(0.0, 0.0, 1.0);\n"
            + "        }\n"
            + "        right = safeNormalize(vec3(camRight.x, 0.0, camRight.z));\n"
            + "        if (length(right) < 1e-6) {\n"
            + "            right = safeNormalize(cross(vec3(0.0, 1.0, 0.0), normal));\n"
            + "        }\n"
            + "        up = vec3(0.0, 1.0, 0.0);\n"
            + "    } else if (mode == MODE_LOOK_AT_XYZ) {\n"
            + "        normal = safeNormalize(u_cameraPos - worldPos);\n"
            + "        buildFacingFromNormal(normal, right, up);\n"
            + "    } else if (mode == MODE_LOOK_AT_Y) {\n"
            + "        vec3 toCam = vec3(u_cameraPos.x - worldPos.x, 0.0, u_cameraPos.z - worldPos.z);\n"
            + "        normal = safeNormalize(toCam);\n"
            + "        buildFacingFromNormal(normal, right, up);\n"
            + "    } else if (mode == MODE_DIRECTION_X || mode == MODE_DIRECTION_Y || mode == MODE_DIRECTION_Z) {\n"
            + "        vec3 ndir = safeNormalize(dir);\n"
            + "        float yaw = getYaw(ndir);\n"
            + "        float pitch = getPitch(ndir);\n"
            + "        mat3 rot = rotY(yaw) * rotX(pitch);\n"
            + "        if (mode == MODE_DIRECTION_X) {\n"
            + "            rot = rot * rotY(radians(90.0)) * rotZ(radians(90.0));\n"
            + "        } else if (mode == MODE_DIRECTION_Y) {\n"
            + "            rot = rotY(yaw) * rotX(pitch + radians(90.0)) * rotZ(radians(90.0));\n"
            + "        } else {\n"
            + "            rot = rot * rotZ(radians(90.0));\n"
            + "        }\n"
            + "        right = rot * vec3(1.0, 0.0, 0.0);\n"
            + "        up = rot * vec3(0.0, 1.0, 0.0);\n"
            + "        normal = rot * vec3(0.0, 0.0, 1.0);\n"
            + "    } else if (mode == MODE_LOOKAT_DIRECTION) {\n"
            + "        vec3 ndir = safeNormalize(dir);\n"
            + "        float yaw = getYaw(ndir);\n"
            + "        float pitch = getPitch(ndir);\n"
            + "        mat3 rot = rotY(yaw) * rotX(pitch + radians(90.0));\n"
            + "        vec3 baseZ = rot * vec3(0.0, 0.0, 1.0);\n"
            + "        vec3 camDir = safeNormalize(u_cameraPos - worldPos);\n"
            + "        vec3 proj = camDir - ndir * dot(camDir, ndir);\n"
            + "        float projLen = length(proj);\n"
            + "        if (projLen > 1e-6) {\n"
            + "            proj = proj / projLen;\n"
            + "            float dotp = clamp(dot(proj, baseZ), -1.0, 1.0);\n"
            + "            float angle = acos(dotp);\n"
            + "            float signVal = dot(cross(proj, baseZ), ndir);\n"
            + "            float finalRot = signVal < 0.0 ? angle : -angle;\n"
            + "            rot = rot * rotY(finalRot);\n"
            + "        }\n"
            + "        rot = rot * rotZ(radians(90.0));\n"
            + "        right = rot * vec3(1.0, 0.0, 0.0);\n"
            + "        up = rot * vec3(0.0, 1.0, 0.0);\n"
            + "        normal = rot * vec3(0.0, 0.0, 1.0);\n"
            + "    } else if (mode == MODE_EMITTER_XY || mode == MODE_EMITTER_XZ || mode == MODE_EMITTER_YZ) {\n"
            + "        int emitterIndex = int(p.dir.w + 0.5);\n"
            + "        EmitterData e = emitters[emitterIndex];\n"
            + "        vec3 ex = e.basisX.xyz;\n"
            + "        vec3 ey = e.basisY.xyz;\n"
            + "        vec3 ez = e.basisZ.xyz;\n"
            + "        if (mode == MODE_EMITTER_XZ) {\n"
            + "            right = ex;\n"
            + "            up = ez;\n"
            + "            normal = -ey;\n"
            + "        } else if (mode == MODE_EMITTER_YZ) {\n"
            + "            right = -ez;\n"
            + "            up = ey;\n"
            + "            normal = ex;\n"
            + "        } else {\n"
            + "            right = ex;\n"
            + "            up = ey;\n"
            + "            normal = ez;\n"
            + "        }\n"
            + "        right = safeNormalize(right);\n"
            + "        up = safeNormalize(up);\n"
            + "        normal = safeNormalize(normal);\n"
            + "    }\n"
            + "\n"
            + "    float c = cos(rollRad);\n"
            + "    float s = sin(rollRad);\n"
            + "    vec3 r2 = right * c + up * s;\n"
            + "    vec3 u2 = -right * s + up * c;\n"
            + "    right = r2;\n"
            + "    up = u2;\n"
            + "\n"
            + "    vec3 finalPos = pos + right * (a_corner.x * size.x) + up * (a_corner.y * size.y);\n"
            + "    gl_Position = u_viewProj * vec4(finalPos, 1.0);\n"
            + "\n"
            + "    vec2 uv = vec2(a_uv.x, 1.0 - a_uv.y);\n"
            + "    v_uv = mix(p.uv.xy, p.uv.zw, uv);\n"
            + "    v_color = p.color;\n"
            + "    v_lightUV = p.extra.xy;\n"
            + "    v_fogDist = length(u_cameraPos - worldPos);\n"
            + "}\n";
    }

    private String fragmentSource() {
        StringBuilder sb = new StringBuilder();
        sb.append("#version 430\n\n");
        sb.append("in vec2 v_uv;\n");
        sb.append("in vec4 v_color;\n");
        sb.append("in vec2 v_lightUV;\n");
        sb.append("in float v_fogDist;\n");
        sb.append("\n");
        sb.append("uniform sampler2D u_texture;\n");
        if (lit) {
            sb.append("uniform sampler2D u_lightmap;\n");
        }
        sb.append("uniform vec3 u_fogColor;\n");
        sb.append("uniform float u_fogStart;\n");
        sb.append("uniform float u_fogEnd;\n");
        sb.append("uniform int u_fogEnabled;\n");
        sb.append("\n");
        sb.append("out vec4 fragColor;\n\n");
        sb.append("void main() {\n");
        sb.append("    vec4 tex = texture(u_texture, v_uv);\n");
        sb.append("    vec4 color = tex * v_color;\n");
        if (lit) {
            sb.append("    vec4 light = texture(u_lightmap, v_lightUV);\n");
            sb.append("    color.rgb *= light.rgb;\n");
        }
        sb.append("    if (u_fogEnabled != 0) {\n");
        sb.append("        float denom = max(0.0001, u_fogEnd - u_fogStart);\n");
        sb.append("        float fogFactor = clamp((u_fogEnd - v_fogDist) / denom, 0.0, 1.0);\n");
        sb.append("        color.rgb = mix(u_fogColor, color.rgb, fogFactor);\n");
        sb.append("    }\n");
        sb.append("    fragColor = color;\n");
        sb.append("}\n");
        return sb.toString();
    }
}
