package org.mybad.minecraft.render.geometry;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;

import java.util.List;
import java.util.Map;

/**
 * Builds shared geometry and quad data for Bedrock models.
 */
public final class ModelGeometryBuilder {
    private final Model model;
    private final int textureWidth;
    private final int textureHeight;
    private final QuadGenerationState quadState;
    private final Map<ModelBone, Integer> boneIndexMap;
    private final List<ModelBone> rootBones;
    private final SharedGeometryBuilder sharedGeometryBuilder;

    public ModelGeometryBuilder(Model model, int textureWidth, int textureHeight) {
        this.model = model;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.quadState = new QuadGenerationState();
        this.boneIndexMap = BoneIndexResolver.resolveBoneIndexMap(model);
        this.rootBones = BoneIndexResolver.resolveRootBones(model.getBones());
        this.sharedGeometryBuilder = SharedGeometryBuilderFactory.create(model, textureWidth, textureHeight, boneIndexMap);
    }

    public Map<ModelBone, Integer> getBoneIndexMap() {
        return boneIndexMap;
    }

    public List<ModelBone> getRootBones() {
        return rootBones;
    }

    public void generateAllQuads() {
        if (quadState.isGenerated()) {
            return;
        }
        QuadGeneration.generateAll(model.getBones(), textureWidth, textureHeight);
        quadState.markGenerated();
    }

    public void regenerateQuads() {
        QuadGeneration.clearAll(model.getBones());
        quadState.clear();
        generateAllQuads();
    }

    public SharedGeometry buildSharedGeometry() {
        return sharedGeometryBuilder.build();
    }

    // bone index/root collection extracted to BoneIndexResolver
}
