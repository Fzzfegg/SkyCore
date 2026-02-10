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
        if (proto.getRenderBoxWidth() > 0f) {
            mapping.setRenderBoxWidth(proto.getRenderBoxWidth());
        }
        if (proto.getRenderBoxHeight() > 0f) {
            mapping.setRenderBoxHeight(proto.getRenderBoxHeight());
        }
        if (proto.getRenderBoxDepth() > 0f) {
            mapping.setRenderBoxDepth(proto.getRenderBoxDepth());
        }
        if (proto.getBloomColorCount() > 0) {
            mapping.setBloomColor(toIntArray(proto.getBloomColorList()));
        }
        if (proto.getBloomStrength() > 0f) {
            mapping.setBloomStrength(proto.getBloomStrength());
        }
        if (proto.getBloomPasses() > 0) {
            mapping.setBloomPasses(proto.getBloomPasses());
        }
        if (proto.getBloomScaleStep() > 0f) {
            mapping.setBloomScaleStep(proto.getBloomScaleStep());
        }
        if (proto.getBloomDownscale() > 0f) {
            mapping.setBloomDownscale(proto.getBloomDownscale());
        }
        if (proto.getBloomOffsetCount() >= 3) {
            mapping.setBloomOffset(toArray(proto.getBloomOffsetList()));
        }
        mapping.setOffsetX(proto.getOffsetX());
        mapping.setOffsetY(proto.getOffsetY());
        mapping.setOffsetZ(proto.getOffsetZ());
        mapping.setOffsetMode(proto.getOffsetMode());
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

    private static int[] toIntArray(java.util.List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
