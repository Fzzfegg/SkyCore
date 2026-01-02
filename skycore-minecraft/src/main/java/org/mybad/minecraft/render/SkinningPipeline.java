package org.mybad.minecraft.render;

import org.lwjgl.BufferUtils;
import org.mybad.core.data.Model;

import java.nio.FloatBuffer;

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
    private final BoneMatrixUpdater boneMatrixUpdater;

    SkinningPipeline(Model model, ModelGeometryBuilder geometryBuilder, GeometryCache geometryCache, GeometryCache.Key geometryKey) {
        this.model = model;
        this.geometryBuilder = geometryBuilder;
        this.geometryCache = geometryCache;
        this.geometryKey = geometryKey;
        this.boneMatrixUpdater = new BoneMatrixUpdater(model, geometryBuilder.getBoneIndexMap(), geometryBuilder.getRootBones());
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
        boneMatrixUpdater.update(boneMatrices, boneMatrixBuffer);
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

    private void releaseSharedGeometry() {
        if (sharedGeometry == null) {
            return;
        }
        geometryCache.release(geometryKey, sharedGeometry);
        sharedGeometry = null;
    }
}
