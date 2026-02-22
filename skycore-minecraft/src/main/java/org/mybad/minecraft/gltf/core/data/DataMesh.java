package org.mybad.minecraft.gltf.core.data;

import org.mybad.minecraft.gltf.core.gl.SkinningSupport;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class DataMesh {
    public String material;
    public boolean skin;

    protected List<Float> geoList = new ArrayList<>();
    protected int geoCount;
    protected ByteBuffer geoBuffer;
    protected IntBuffer elementBuffer;
    protected int elementCount;
    public int unit;
    public int glDrawingMode = GL11.GL_TRIANGLES;
    private int displayList = -1;
    private int ssboVao = -1;
    private int vertexCount = 0;
    private boolean compiled = false;
    private boolean compiling = false;
    private boolean initSkinning = false;

    // BUFFER OBJECT
    private int pos_vbo = -1;
    private int tex_vbo = -1;
    private int normal_vbo = -1;
    private int vbo = -1;
    private int ebo = -1;
    private int ssbo = -1;
    private boolean gpuSkinningEnabled = true;
    private float[] cpuBasePositions;
    private float[] cpuBaseNormals;
    private float[] cpuBaseTexCoords;
    private int[] cpuBaseJoints;
    private float[] cpuBaseWeights;
    private float[] cpuDeformedData;
    private FloatBuffer cpuDeformedBuffer;
    private static final int DEFORMED_VERTEX_FLOATS = 8;

    public void render() {
         if (!this.compiled) {
            try {
                compileVAO(1);
                return;
            } catch (Throwable t) {
                System.out.println("[DataMesh] Exception during VAO compilation: " + t.getMessage());
                t.printStackTrace();
            }
        }
        // 如果需要 可加入纹理处理内容

        this.callVAO();
        
//        if(ObjModelRenderer.glowTxtureMode) {
//            if(!ObjModelRenderer.customItemRenderer.bindTextureGlow(ObjModelRenderer.glowType, ObjModelRenderer.glowPath)) {
//                return;
//            }
//            float x = OpenGlHelper.lastBrightnessX;
//            float y = OpenGlHelper.lastBrightnessY;
//            ObjModelRenderer.glowTxtureMode=false;
//            GlStateManager.depthMask(false);
//            //GlStateManager.enableBlend();
//            GlStateManager.depthFunc(GL11.GL_EQUAL);
//            GlStateManager.disableLighting();
//            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
//            callVAO();
//            GlStateManager.enableLighting();
//            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
//            GlStateManager.depthFunc(GL11.GL_LEQUAL);
//            //GlStateManager.disableBlend();
//            GlStateManager.depthMask(true);
//            ObjModelRenderer.glowTxtureMode=true;
//            ObjModelRenderer.customItemRenderer.bindTexture(ObjModelRenderer.glowType, ObjModelRenderer.glowPath);
//
//        }
    }

    private void compileVAO(float scale) {
        if (this.compiling) {
            return;
        }

        this.compiling = true;
        this.ssboVao = GL30.glGenVertexArrays();
        this.displayList = GL30.glGenVertexArrays();

        if (this.unit == 3) {
            final List<Float> geoList = this.geoList;
            this.vertexCount = geoList.size() / this.unit;

            FloatBuffer pos_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
            FloatBuffer tex_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
            FloatBuffer normal_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);

//            IntBuffer joint_intBuffer = BufferUtils.createIntBuffer(vertexCount * 4);
//            FloatBuffer weight_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 4);

            for (int i = 0, size = geoList.size(); i + 8 <= size; i += 8) {
                pos_floatBuffer.put(geoList.get(i));
                pos_floatBuffer.put(geoList.get(i + 1));
                pos_floatBuffer.put(geoList.get(i + 2));
                tex_floatBuffer.put(geoList.get(i + 3));
                tex_floatBuffer.put(geoList.get(i + 4));
                normal_floatBuffer.put(geoList.get(i + 5));
                normal_floatBuffer.put(geoList.get(i + 6));
                normal_floatBuffer.put(geoList.get(i + 7));
            }
            pos_floatBuffer.flip();
            tex_floatBuffer.flip();
            normal_floatBuffer.flip();

            GL30.glBindVertexArray(this.displayList);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            this.pos_vbo = GL15.glGenBuffers();
            this.tex_vbo = GL15.glGenBuffers();
            this.normal_vbo = GL15.glGenBuffers();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pos_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, pos_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tex_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tex_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normal_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normal_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            this.compiled = true;
            this.compiling = false;

        } else {
            this.vbo = GL15.glGenBuffers();
            this.ebo = GL15.glGenBuffers();
            this.geoBuffer.flip();
            this.elementBuffer.flip();
            this.gpuSkinningEnabled = SkinningSupport.isGpuSkinningAvailable();
            if (!this.gpuSkinningEnabled) {
                captureCpuSkinningSourceData();
            }
            GL30.glBindVertexArray(this.displayList);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.geoBuffer, GL15.GL_STATIC_DRAW);
            int step = 17 * Float.BYTES;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, step, 0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, step, 3 * Float.BYTES);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, step, 5 * Float.BYTES);
            // in fact, it is u_int:
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, step, 8 * Float.BYTES);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, step, 12 * Float.BYTES);
            // in fact, it is u_int:
            GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, step, 16 * Float.BYTES);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer, GL15.GL_STATIC_DRAW);
            this.ssbo = GL15.glGenBuffers();
            if (this.gpuSkinningEnabled) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssbo);
                GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, this.geoBuffer, GL15.GL_DYNAMIC_COPY);
                int glError = GL11.glGetError();
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
                if (glError != GL11.GL_NO_ERROR) {
                    SkinningSupport.disableGpuSkinning(
                        "SSBO allocation failed for mesh '" + this.material + "' (GL error " + glError + ")");
                    this.gpuSkinningEnabled = false;
                    captureCpuSkinningSourceData();
                    GL15.glDeleteBuffers(this.ssbo);
                    this.ssbo = GL15.glGenBuffers();
                }
            }
            if (!this.gpuSkinningEnabled) {
                if (this.cpuBasePositions == null) {
                    captureCpuSkinningSourceData();
                }
                int cpuBufferBytes = getCpuDeformBufferSizeBytes();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.ssbo);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, cpuBufferBytes, GL15.GL_DYNAMIC_DRAW);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
            
            GL30.glBindVertexArray(this.ssboVao);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.ssbo);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 8 * Float.BYTES, 0);
            GL11.glNormalPointer(GL11.GL_FLOAT, 8 * Float.BYTES, 3 * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 8 * Float.BYTES, 6 * Float.BYTES);
            
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);

            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);

            this.skin = true;
            this.compiled = true;
            this.compiling = false;
        }

        //内存优化
        if(this.geoList != null) {
            this.geoList.clear();
            this.geoList = null;
        }
        if(this.geoBuffer != null) {
            if(((sun.nio.ch.DirectBuffer)this.geoBuffer).cleaner() != null) {
                ((sun.nio.ch.DirectBuffer)this.geoBuffer).cleaner().clean();
            }
        }
        if(this.elementBuffer!=null) {
            if(((sun.nio.ch.DirectBuffer)this.elementBuffer).cleaner() != null) {
                ((sun.nio.ch.DirectBuffer)this.elementBuffer).cleaner().clean();
            }
        }
    }

    private void applyCpuSkinning(Matrix4f[] jointMatrices) {
        if (jointMatrices == null || this.cpuBasePositions == null || this.cpuBaseJoints == null) {
            return;
        }
        final int vertexTotal = this.geoCount;
        if (vertexTotal <= 0) {
            return;
        }
        if (this.cpuDeformedData == null || this.cpuDeformedData.length < vertexTotal * 8) {
            this.cpuDeformedData = new float[vertexTotal * 8];
        }
        if (this.cpuDeformedBuffer == null || this.cpuDeformedBuffer.capacity() < vertexTotal * 8) {
            this.cpuDeformedBuffer = BufferUtils.createFloatBuffer(vertexTotal * 8);
        }

        Vector3f tmpPos = new Vector3f();
        Vector3f tmpNormal = new Vector3f();
        for (int i = 0; i < vertexTotal; i++) {
            int basePos = i * 3;
            float srcX = this.cpuBasePositions[basePos];
            float srcY = this.cpuBasePositions[basePos + 1];
            float srcZ = this.cpuBasePositions[basePos + 2];

            float srcNx = this.cpuBaseNormals[basePos];
            float srcNy = this.cpuBaseNormals[basePos + 1];
            float srcNz = this.cpuBaseNormals[basePos + 2];

            float outX = 0f;
            float outY = 0f;
            float outZ = 0f;
            float outNx = 0f;
            float outNy = 0f;
            float outNz = 0f;
            float weightSum = 0f;

            int jointBase = i * 4;
            for (int j = 0; j < 4; j++) {
                float weight = this.cpuBaseWeights[jointBase + j];
                if (weight <= 0f) {
                    continue;
                }
                int jointIndex = this.cpuBaseJoints[jointBase + j];
                if (jointIndex < 0 || jointIndex >= jointMatrices.length) {
                    continue;
                }
                Matrix4f mat = jointMatrices[jointIndex];
                mat.transformPosition(srcX, srcY, srcZ, tmpPos);
                outX += tmpPos.x * weight;
                outY += tmpPos.y * weight;
                outZ += tmpPos.z * weight;

                mat.transformDirection(srcNx, srcNy, srcNz, tmpNormal);
                outNx += tmpNormal.x * weight;
                outNy += tmpNormal.y * weight;
                outNz += tmpNormal.z * weight;
                weightSum += weight;
            }

            if (weightSum == 0f) {
                outX = srcX;
                outY = srcY;
                outZ = srcZ;
                outNx = srcNx;
                outNy = srcNy;
                outNz = srcNz;
            }

            float normalLen = (float)Math.sqrt(outNx * outNx + outNy * outNy + outNz * outNz);
            if (normalLen > 0f) {
                outNx /= normalLen;
                outNy /= normalLen;
                outNz /= normalLen;
            }

            int outBase = i * 8;
            this.cpuDeformedData[outBase] = outX;
            this.cpuDeformedData[outBase + 1] = outY;
            this.cpuDeformedData[outBase + 2] = outZ;
            this.cpuDeformedData[outBase + 3] = outNx;
            this.cpuDeformedData[outBase + 4] = outNy;
            this.cpuDeformedData[outBase + 5] = outNz;
            this.cpuDeformedData[outBase + 6] = this.cpuBaseTexCoords[i * 2];
            this.cpuDeformedData[outBase + 7] = this.cpuBaseTexCoords[i * 2 + 1];
        }

        this.cpuDeformedBuffer.clear();
        this.cpuDeformedBuffer.put(this.cpuDeformedData);
        this.cpuDeformedBuffer.flip();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.ssbo);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, this.cpuDeformedBuffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        this.initSkinning = true;
    }

    private void captureCpuSkinningSourceData() {
        if (this.geoBuffer == null || this.geoCount <= 0) {
            return;
        }
        ByteBuffer source = this.geoBuffer.duplicate();
        source.clear();
        int vertexTotal = this.geoCount;
        this.cpuBasePositions = new float[vertexTotal * 3];
        this.cpuBaseNormals = new float[vertexTotal * 3];
        this.cpuBaseTexCoords = new float[vertexTotal * 2];
        this.cpuBaseJoints = new int[vertexTotal * 4];
        this.cpuBaseWeights = new float[vertexTotal * 4];
        for (int i = 0; i < vertexTotal; i++) {
            this.cpuBasePositions[i * 3] = source.getFloat();
            this.cpuBasePositions[i * 3 + 1] = source.getFloat();
            this.cpuBasePositions[i * 3 + 2] = source.getFloat();
            this.cpuBaseTexCoords[i * 2] = source.getFloat();
            this.cpuBaseTexCoords[i * 2 + 1] = source.getFloat();
            this.cpuBaseNormals[i * 3] = source.getFloat();
            this.cpuBaseNormals[i * 3 + 1] = source.getFloat();
            this.cpuBaseNormals[i * 3 + 2] = source.getFloat();
            this.cpuBaseJoints[i * 4] = source.getInt();
            this.cpuBaseJoints[i * 4 + 1] = source.getInt();
            this.cpuBaseJoints[i * 4 + 2] = source.getInt();
            this.cpuBaseJoints[i * 4 + 3] = source.getInt();
            this.cpuBaseWeights[i * 4] = source.getFloat();
            this.cpuBaseWeights[i * 4 + 1] = source.getFloat();
            this.cpuBaseWeights[i * 4 + 2] = source.getFloat();
            this.cpuBaseWeights[i * 4 + 3] = source.getFloat();
            source.getInt(); // Skip vertex counter used by GPU shader.
        }
        this.cpuDeformedData = new float[vertexTotal * 8];
        this.cpuDeformedBuffer = BufferUtils.createFloatBuffer(vertexTotal * 8);
    }

    private int getCpuDeformBufferSizeBytes() {
        return this.geoCount * DEFORMED_VERTEX_FLOATS * Float.BYTES;
    }

    public void callSkinning(Matrix4f[] jointMatrices) {
        if (!this.compiled) {
            return;
        }
        if (this.skin) {
            if (this.ebo == -1 || this.elementCount <= 0) {
                return;
            }
            if (this.gpuSkinningEnabled && SkinningSupport.isGpuSkinningAvailable()) {
                GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, this.ssbo);
                GL30.glBindVertexArray(this.displayList);
                if (this.ebo != -1) {
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
                }
                GL11.glDrawElements(this.glDrawingMode, this.elementCount, GL11.GL_UNSIGNED_INT, 0);
                GL30.glBindVertexArray(0);
                if (this.ebo != -1) {
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
                }
                GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
                this.initSkinning = true;
            } else {
                applyCpuSkinning(jointMatrices);
            }
        }
    }

    private void callVAO() {
        if (!this.compiled) {
            return;
        }
        if (this.skin) {
            if (!this.initSkinning) {
                return;
            }
            if (this.ebo == -1 || this.elementCount <= 0) {
                return;
            }
            GL30.glBindVertexArray(this.ssboVao);
            if (this.ebo != -1) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
            }
            GL11.glDrawElements(this.glDrawingMode, this.elementCount, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);
            if (this.ebo != -1) {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            }
            if (this.gpuSkinningEnabled && SkinningSupport.isGpuSkinningAvailable()) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            } else {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            }
        } else {
            GL30.glBindVertexArray(this.displayList);
            GL11.glDrawArrays(this.glDrawingMode, 0, this.vertexCount);
            GL30.glBindVertexArray(0);
        }
    }

    public void delete() {
        // It will be auto clean.
        GL30.glDeleteVertexArrays(this.displayList);
        GL30.glDeleteVertexArrays(this.ssboVao);
        if (this.pos_vbo != -1) {
            GL15.glDeleteBuffers(this.pos_vbo);
        }
        if (this.tex_vbo != -1) {
            GL15.glDeleteBuffers(this.tex_vbo);
        }
        if (this.normal_vbo != -1) {
            GL15.glDeleteBuffers(this.normal_vbo);
        }
        if (this.vbo != -1) {
            GL15.glDeleteBuffers(this.vbo);
        }
        if (this.ebo != -1) {
            GL15.glDeleteBuffers(this.ebo);
        }
        if (this.ssbo != -1) {
            GL15.glDeleteBuffers(this.ssbo);
        }
    }
}
