package org.mybad.minecraft.render.geometry;

import org.mybad.core.data.Model;
import org.mybad.core.data.ModelBone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BoneIndexResolver {
    private BoneIndexResolver() {
    }

    static Map<ModelBone, Integer> resolveBoneIndexMap(Model model) {
        Map<ModelBone, Integer> map = new HashMap<>();
        List<ModelBone> bones = model.getBones();
        for (int i = 0; i < bones.size(); i++) {
            map.put(bones.get(i), i);
        }
        return map;
    }

    static List<ModelBone> resolveRootBones(List<ModelBone> bones) {
        List<ModelBone> roots = new ArrayList<>();
        for (ModelBone bone : bones) {
            if (bone.getParent() == null) {
                roots.add(bone);
            }
        }
        return roots;
    }
}
