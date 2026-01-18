package org.mybad.minecraft.render.geometry;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.data.ModelQuad;
import org.mybad.minecraft.render.transform.BedrockModelTransforms;
import org.mybad.minecraft.render.transform.MatrixStack;

import java.nio.FloatBuffer;
import java.util.Map;

final class SharedGeometryBuilder {
    private static final float NORMAL_OFFSET_EPS = 0.0002f;

    private final Model model;
    private final int textureWidth;
    private final int textureHeight;
    private final Map<ModelBone, Integer> boneIndexMap;

    SharedGeometryBuilder(Model model, int textureWidth, int textureHeight, Map<ModelBone, Integer> boneIndexMap) {
        this.model = model;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.boneIndexMap = boneIndexMap;
    }

    SharedGeometry build() {
        int quadCount = countQuads();
        if (quadCount <= 0) {
            return null;
        }
        int vertexCount = quadCount * 6;
        FloatBuffer buffer = SkinnedMesh.allocateInputBuffer(vertexCount);
        int vertexIndex = 0;

        int quadCounter = 0;
        for (ModelBone bone : model.getBones()) {
            if (bone.isNeverRender()) {
                continue;
            }
            Integer boneIndex = boneIndexMap.get(bone);
            if (boneIndex == null) {
                continue;
            }
            for (ModelCube cube : bone.getCubes()) {
                if (!cube.hasQuads()) {
                    cube.generateQuads(textureWidth, textureHeight);
                }
                MatrixStack cubeRotation = BedrockModelTransforms.buildCubeRotationStack(cube);
                for (ModelQuad quad : cube.getQuads()) {
                    int pattern = quadCounter % 8;
                    float magnitude = NORMAL_OFFSET_EPS * (1 + (pattern >> 1));
                    float bias = ((pattern & 1) == 0 ? 1f : -1f) * magnitude;
                    quadCounter++;
                    int[] order = new int[]{0, 1, 2, 2, 3, 0};
                    for (int idx : order) {
                        float[] pos = new float[]{quad.vertices[idx].x, quad.vertices[idx].y, quad.vertices[idx].z};
                        float[] normal = new float[]{quad.normalX, quad.normalY, quad.normalZ};
                        if (cubeRotation != null) {
                            cubeRotation.transform(pos);
                            cubeRotation.transformNormal(normal);
                        }
                        float lenSq = normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2];
                        if (lenSq > 1.0e-8f) {
                            pos[0] += normal[0] * bias;
                            pos[1] += normal[1] * bias;
                            pos[2] += normal[2] * bias;
                        }

                        buffer.put(pos[0]).put(pos[1]).put(pos[2]);
                        buffer.put(quad.vertices[idx].u).put(quad.vertices[idx].v);
                        buffer.put(normal[0]).put(normal[1]).put(normal[2]);

                        buffer.put(boneIndex.floatValue()).put(0f).put(0f).put(0f);
                        buffer.put(1f).put(0f).put(0f).put(0f);
                        buffer.put((float) vertexIndex);
                        vertexIndex++;
                    }
                }
            }
        }

        buffer.flip();
        return new SharedGeometry(buffer, vertexIndex);
    }

    private int countQuads() {
        int count = 0;
        for (ModelBone bone : model.getBones()) {
            count += countQuadsRecursive(bone);
        }
        return count;
    }

    private int countQuadsRecursive(ModelBone bone) {
        int count = 0;
        for (ModelCube cube : bone.getCubes()) {
            if (!cube.hasQuads()) {
                cube.generateQuads(textureWidth, textureHeight);
            }
            count += cube.getQuads().size();
        }
        for (ModelBone child : bone.getChildren()) {
            count += countQuadsRecursive(child);
        }
        return count;
    }
}
