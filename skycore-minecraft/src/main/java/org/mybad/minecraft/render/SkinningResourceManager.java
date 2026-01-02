package org.mybad.minecraft.render;

final class SkinningResourceManager {
    private final ModelGeometryBuilder geometryBuilder;
    private final GeometryCache geometryCache;
    private final GeometryCache.Key geometryKey;
    private SharedGeometry sharedGeometry;
    private SkinnedMesh skinnedMesh;
    private boolean shaderAcquired;
    private boolean gpuSkinningReady;

    SkinningResourceManager(ModelGeometryBuilder geometryBuilder, GeometryCache geometryCache, GeometryCache.Key geometryKey) {
        this.geometryBuilder = geometryBuilder;
        this.geometryCache = geometryCache;
        this.geometryKey = geometryKey;
    }

    boolean ensureGpuSkinningReady(int boneCount) {
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
            releaseShader();
            return false;
        }
        skinnedMesh = new SkinnedMesh(sharedGeometry, boneCount);
        gpuSkinningReady = skinnedMesh != null;
        if (!gpuSkinningReady) {
            releaseSharedGeometry();
            releaseShader();
        }
        return gpuSkinningReady;
    }

    SkinnedMesh getSkinnedMesh() {
        return skinnedMesh;
    }

    void dispose() {
        if (skinnedMesh != null) {
            skinnedMesh.destroy();
            skinnedMesh = null;
        }
        releaseSharedGeometry();
        releaseShader();
        gpuSkinningReady = false;
    }

    private void releaseSharedGeometry() {
        if (sharedGeometry == null) {
            return;
        }
        geometryCache.release(geometryKey, sharedGeometry);
        sharedGeometry = null;
    }

    private void releaseShader() {
        if (shaderAcquired) {
            GpuSkinningShader.release();
            shaderAcquired = false;
        }
    }
}
