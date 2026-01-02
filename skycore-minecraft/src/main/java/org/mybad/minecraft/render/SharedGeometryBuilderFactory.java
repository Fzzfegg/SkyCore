package org.mybad.minecraft.render;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;

import java.util.Map;

final class SharedGeometryBuilderFactory {
    private SharedGeometryBuilderFactory() {
    }

    static SharedGeometryBuilder create(Model model, int textureWidth, int textureHeight, Map<ModelBone, Integer> boneIndexMap) {
        return new SharedGeometryBuilder(model, textureWidth, textureHeight, boneIndexMap);
    }
}
