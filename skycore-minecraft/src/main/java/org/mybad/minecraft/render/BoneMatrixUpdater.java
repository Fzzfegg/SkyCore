package org.mybad.minecraft.render;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

final class BoneMatrixUpdater {
    private final Model model;
    private final Map<ModelBone, Integer> boneIndexMap;
    private final List<ModelBone> rootBones;
    private final MatrixStack boneMatrixStack = new MatrixStack();

    BoneMatrixUpdater(Model model, Map<ModelBone, Integer> boneIndexMap, List<ModelBone> rootBones) {
        this.model = model;
        this.boneIndexMap = boneIndexMap;
        this.rootBones = rootBones;
    }

    void update(float[] boneMatrices, FloatBuffer boneMatrixBuffer) {
        if (boneMatrices == null || boneMatrixBuffer == null) {
            return;
        }
        ConstraintApplier.apply(model);

        boneMatrixStack.loadIdentity();
        for (ModelBone bone : rootBones) {
            fillBoneMatricesRecursive(bone, boneMatrixStack, boneMatrices);
        }

        boneMatrixBuffer.clear();
        boneMatrixBuffer.put(boneMatrices, 0, boneMatrices.length);
        boneMatrixBuffer.flip();
    }

    private void fillBoneMatricesRecursive(ModelBone bone, MatrixStack stack, float[] boneMatrices) {
        stack.push();
        BedrockModelTransforms.applyBoneTransform(bone, stack);

        Integer index = boneIndexMap.get(bone);
        if (index != null) {
            float[] current = stack.getCurrentMatrix();
            System.arraycopy(current, 0, boneMatrices, index * 16, 16);
        }

        for (ModelBone child : bone.getChildren()) {
            fillBoneMatricesRecursive(child, stack, boneMatrices);
        }

        stack.pop();
    }
}
