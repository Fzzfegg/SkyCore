package org.mybad.minecraft.network.skycore.config;

import org.mybad.minecraft.config.EntityModelMapping;
import org.mybad.skycoreproto.SkyCoreProto;

public final class ProtoMappingConverter {
    private ProtoMappingConverter() {}

    public static EntityModelMapping toEntityModelMapping(SkyCoreProto.EntityMapping proto) {
        EntityModelMapping mapping = new EntityModelMapping();
        mapping.setName(proto.getName());
        mapping.setModel(proto.getModel());
        mapping.setAnimation(nullIfEmpty(proto.getAnimation()));
        mapping.setTexture(nullIfEmpty(proto.getTexture()));
        mapping.setEmissive(nullIfEmpty(proto.getEmissive()));
        mapping.setEmissiveStrength(proto.getEmissiveStrength());
        mapping.setBloom(nullIfEmpty(proto.getBloom()));
        mapping.setBlendTexture(nullIfEmpty(proto.getBlendTexture()));
        mapping.setBlendMode(nullIfEmpty(proto.getBlendMode()));
        if (!proto.getBlendColorList().isEmpty()) {
            mapping.setBlendColor(toArray(proto.getBlendColorList()));
        }
        mapping.setEnableCull(proto.getEnableCull());
        mapping.setModelScale(proto.getScale());
        mapping.setPrimaryFadeSeconds(proto.getPrimaryFadeSeconds());
        mapping.setRenderHurtTint(proto.getRenderHurtTint());
        if (!proto.getHurtTintList().isEmpty()) {
            mapping.setHurtTint(toArray(proto.getHurtTintList()));
        }
        mapping.setRenderShadow(proto.getRenderShadow());
        return mapping;
    }

    private static float[] toArray(java.util.List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static String nullIfEmpty(String str) {
        return str == null || str.isEmpty() ? null : str;
    }
}
