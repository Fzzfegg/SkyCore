package org.mybad.minecraft.render;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;
import org.mybad.core.data.ModelQuad;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds shared geometry and quad data for Bedrock models.
 */
final class ModelGeometryBuilder {
    private final Model model;
    private final int textureWidth;
    private final int textureHeight;
    private boolean quadsGenerated;
    private final Map<ModelBone, Integer> boneIndexMap;
    private final List<ModelBone> rootBones;

    ModelGeometryBuilder(Model model, int textureWidth, int textureHeight) {
        this.model = model;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.quadsGenerated = false;
        this.boneIndexMap = buildBoneIndexMap(model);
        this.rootBones = collectRootBones(model.getBones());
    }

    Map<ModelBone, Integer> getBoneIndexMap() {
        return boneIndexMap;
    }

    List<ModelBone> getRootBones() {
        return rootBones;
    }

    void generateAllQuads() {
        if (quadsGenerated) {
            return;
        }
        for (ModelBone bone : model.getBones()) {
            generateQuadsForBone(bone);
        }
        quadsGenerated = true;
    }

    void regenerateQuads() {
        for (ModelBone bone : model.getBones()) {
            clearQuadsForBone(bone);
        }
        quadsGenerated = false;
        generateAllQuads();
    }

    SharedGeometry buildSharedGeometry() {
        int quadCount = countQuads();
        if (quadCount <= 0) {
            return null;
        }
        int vertexCount = quadCount * 6;
        FloatBuffer buffer = SkinnedMesh.allocateInputBuffer(vertexCount);
        int vertexIndex = 0;

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
                    int[] order = new int[]{0, 1, 2, 2, 3, 0};
                    for (int idx : order) {
                        float[] pos = new float[]{quad.vertices[idx].x, quad.vertices[idx].y, quad.vertices[idx].z};
                        float[] normal = new float[]{quad.normalX, quad.normalY, quad.normalZ};
                        if (cubeRotation != null) {
                            cubeRotation.transform(pos);
                            cubeRotation.transformNormal(normal);
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

    private void generateQuadsForBone(ModelBone bone) {
        for (ModelCube cube : bone.getCubes()) {
            if (!cube.hasQuads()) {
                cube.generateQuads(textureWidth, textureHeight);
            }
        }
        for (ModelBone child : bone.getChildren()) {
            generateQuadsForBone(child);
        }
    }

    private void clearQuadsForBone(ModelBone bone) {
        for (ModelCube cube : bone.getCubes()) {
            cube.getQuads().clear();
        }
        for (ModelBone child : bone.getChildren()) {
            clearQuadsForBone(child);
        }
    }

    private static Map<ModelBone, Integer> buildBoneIndexMap(Model model) {
        Map<ModelBone, Integer> map = new HashMap<>();
        List<ModelBone> bones = model.getBones();
        for (int i = 0; i < bones.size(); i++) {
            map.put(bones.get(i), i);
        }
        return map;
    }

    private static List<ModelBone> collectRootBones(List<ModelBone> bones) {
        List<ModelBone> roots = new ArrayList<>();
        for (ModelBone bone : bones) {
            if (bone.getParent() == null) {
                roots.add(bone);
            }
        }
        return roots;
    }
}
