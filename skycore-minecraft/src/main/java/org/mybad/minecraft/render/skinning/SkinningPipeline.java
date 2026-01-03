package org.mybad.minecraft.render.skinning;

import org.lwjgl.BufferUtils;
import org.mybad.core.data.Model;
import org.mybad.minecraft.render.geometry.GeometryCache;
import org.mybad.minecraft.render.geometry.ModelGeometryBuilder;
import org.mybad.minecraft.render.geometry.SkinnedMesh;

import java.nio.FloatBuffer;

/**
 * Handles GPU skinning setup and bone matrix updates.
 */
public final class SkinningPipeline {
    private final Model model;
    private final ModelGeometryBuilder geometryBuilder;
    private final SkinningResourceManager resourceManager;
    private FloatBuffer boneMatrixBuffer;
    private float[] boneMatrices;
    private final BoneMatrixUpdater boneMatrixUpdater;

    public SkinningPipeline(Model model, ModelGeometryBuilder geometryBuilder, GeometryCache geometryCache, GeometryCache.Key geometryKey) {
        this.model = model;
        this.geometryBuilder = geometryBuilder;
        this.resourceManager = new SkinningResourceManager(geometryBuilder, geometryCache, geometryKey);
        this.boneMatrixUpdater = new BoneMatrixUpdater(model, geometryBuilder.getBoneIndexMap(), geometryBuilder.getRootBones());
        initBoneMatrices();
    }

    public boolean ensureGpuSkinningReady() {
        return resourceManager.ensureGpuSkinningReady(model.getBones().size());
    }

    public void updateBoneMatrices() {
        boneMatrixUpdater.update(boneMatrices, boneMatrixBuffer);
    }

    public void runSkinningPass() {
        SkinnedMesh skinnedMesh = resourceManager.getSkinnedMesh();
        if (skinnedMesh == null) {
            return;
        }
        skinnedMesh.updateJointMatrices(boneMatrixBuffer);
        skinnedMesh.runSkinningPass();
    }

    public void draw() {
        SkinnedMesh skinnedMesh = resourceManager.getSkinnedMesh();
        if (skinnedMesh != null) {
            skinnedMesh.draw();
        }
    }

    public void dispose() {
        resourceManager.dispose();
    }

    private void initBoneMatrices() {
        int boneCount = model.getBones().size();
        if (boneCount <= 0) {
            return;
        }
        boneMatrices = new float[boneCount * 16];
        boneMatrixBuffer = BufferUtils.createFloatBuffer(boneCount * 16);
    }

}
