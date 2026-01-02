package org.mybad.minecraft.render;

import org.mybad.core.data.ModelBone;
import org.mybad.core.data.ModelCube;

import java.util.List;

final class QuadGeneration {
    private QuadGeneration() {
    }

    static void generateAll(List<ModelBone> bones, int textureWidth, int textureHeight) {
        if (bones == null) {
            return;
        }
        for (ModelBone bone : bones) {
            generateForBone(bone, textureWidth, textureHeight);
        }
    }

    static void clearAll(List<ModelBone> bones) {
        if (bones == null) {
            return;
        }
        for (ModelBone bone : bones) {
            clearForBone(bone);
        }
    }

    private static void generateForBone(ModelBone bone, int textureWidth, int textureHeight) {
        for (ModelCube cube : bone.getCubes()) {
            if (!cube.hasQuads()) {
                cube.generateQuads(textureWidth, textureHeight);
            }
        }
        for (ModelBone child : bone.getChildren()) {
            generateForBone(child, textureWidth, textureHeight);
        }
    }

    private static void clearForBone(ModelBone bone) {
        for (ModelCube cube : bone.getCubes()) {
            cube.getQuads().clear();
        }
        for (ModelBone child : bone.getChildren()) {
            clearForBone(child);
        }
    }
}
