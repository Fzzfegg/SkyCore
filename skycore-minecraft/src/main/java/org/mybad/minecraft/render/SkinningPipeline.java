package org.mybad.minecraft.render;

import org.lwjgl.BufferUtils;
import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

/**
 * Handles GPU skinning setup and bone matrix updates.
 */
final class SkinningPipeline {
    private final Model model;
    private final ModelGeometryBuilder geometryBuilder;
    private final GeometryCache geometryCache;
    private final GeometryCache.Key geometryKey;
    private SharedGeometry sharedGeometry;
    private SkinnedMesh skinnedMesh;
    private boolean shaderAcquired;
    private boolean gpuSkinningReady;
    private FloatBuffer boneMatrixBuffer;
    private float[] boneMatrices;
    private final MatrixStack boneMatrixStack = new MatrixStack();

    SkinningPipeline(Model model, ModelGeometryBuilder geometryBuilder, GeometryCache geometryCache, GeometryCache.Key geometryKey) {
        this.model = model;
        this.geometryBuilder = geometryBuilder;
        this.geometryCache = geometryCache;
        this.geometryKey = geometryKey;
        initBoneMatrices();
    }

    boolean ensureGpuSkinningReady() {
        if (gpuSkinningReady) {
            return true;
        }
        if (!GpuSkinningSupport.isGpuSkinningAvailable()) {
            return false;
        }
        if (!shaderAcquired) {
            GpuSkinningShader.acquire();
            shaderAcquired = true;
        }
        if (sharedGeometry == null || sharedGeometry.isDestroyed()) {
            sharedGeometry = geometryCache.acquire(geometryKey, geometryBuilder::buildSharedGeometry);
        }
        if (sharedGeometry == null || sharedGeometry.isDestroyed()) {
            if (shaderAcquired) {
                GpuSkinningShader.release();
                shaderAcquired = false;
            }
            return false;
        }
        skinnedMesh = new SkinnedMesh(sharedGeometry, model.getBones().size());
        gpuSkinningReady = skinnedMesh != null;
        if (!gpuSkinningReady) {
            releaseSharedGeometry();
            if (shaderAcquired) {
                GpuSkinningShader.release();
                shaderAcquired = false;
            }
        }
        return gpuSkinningReady;
    }

    void updateBoneMatrices() {
        if (boneMatrices == null || boneMatrixBuffer == null) {
            return;
        }

        ConstraintApplier.apply(model);

        boneMatrixStack.loadIdentity();
        List<ModelBone> rootBones = geometryBuilder.getRootBones();
        for (ModelBone bone : rootBones) {
            fillBoneMatricesRecursive(bone, boneMatrixStack);
        }

        boneMatrixBuffer.clear();
        boneMatrixBuffer.put(boneMatrices, 0, boneMatrices.length);
        boneMatrixBuffer.flip();
    }

    void runSkinningPass() {
        if (skinnedMesh == null) {
            return;
        }
        skinnedMesh.updateJointMatrices(boneMatrixBuffer);
        skinnedMesh.runSkinningPass();
    }

    void draw() {
        if (skinnedMesh != null) {
            skinnedMesh.draw();
        }
    }

    void dispose() {
        if (skinnedMesh != null) {
            skinnedMesh.destroy();
            skinnedMesh = null;
        }
        releaseSharedGeometry();
        if (shaderAcquired) {
            GpuSkinningShader.release();
            shaderAcquired = false;
        }
        gpuSkinningReady = false;
    }

    private void initBoneMatrices() {
        int boneCount = model.getBones().size();
        if (boneCount <= 0) {
            return;
        }
        boneMatrices = new float[boneCount * 16];
        boneMatrixBuffer = BufferUtils.createFloatBuffer(boneCount * 16);
    }

    private void fillBoneMatricesRecursive(ModelBone bone, MatrixStack stack) {
        stack.push();
        BedrockModelTransforms.applyBoneTransform(bone, stack);

        Map<ModelBone, Integer> boneIndexMap = geometryBuilder.getBoneIndexMap();
        Integer index = boneIndexMap.get(bone);
        if (index != null) {
            float[] current = stack.getCurrentMatrix();
            System.arraycopy(current, 0, boneMatrices, index * 16, 16);
        }

        for (ModelBone child : bone.getChildren()) {
            fillBoneMatricesRecursive(child, stack);
        }

        stack.pop();
    }

    private void releaseSharedGeometry() {
        if (sharedGeometry == null) {
            return;
        }
        geometryCache.release(geometryKey, sharedGeometry);
        sharedGeometry = null;
    }
}
