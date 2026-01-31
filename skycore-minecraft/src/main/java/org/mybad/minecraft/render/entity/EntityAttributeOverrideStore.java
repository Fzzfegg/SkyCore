package org.mybad.minecraft.render.entity;

import org.mybad.skycoreproto.SkyCoreProto;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class EntityAttributeOverrideStore {
    private static final class Entry {
        EntityAttributeOverride override;
        long version;
    }

    private final Map<UUID, Entry> overrides = new ConcurrentHashMap<>();
    private long versionCounter = 0L;

    long applyOverride(SkyCoreProto.EntityAttributeOverride proto) {
        UUID uuid = parseUuid(proto.getEntityUuid());
        if (uuid == null) {
            return versionCounter;
        }
        if (proto.getReset()) {
            overrides.remove(uuid);
            return ++versionCounter;
        }
        Entry entry = overrides.computeIfAbsent(uuid, key -> new Entry());
        entry.override = merge(entry.override, proto);
        entry.version = ++versionCounter;
        return entry.version;
    }

    EntityAttributeOverride get(UUID uuid) {
        Entry entry = overrides.get(uuid);
        return entry != null ? entry.override : null;
    }

    long getVersion(UUID uuid) {
        Entry entry = overrides.get(uuid);
        return entry != null ? entry.version : Long.MIN_VALUE;
    }

    void clear(UUID uuid) {
        overrides.remove(uuid);
    }

    void clearAll() {
        overrides.clear();
    }

    private EntityAttributeOverride merge(EntityAttributeOverride base, SkyCoreProto.EntityAttributeOverride proto) {
        Float scale = base != null ? base.scale : null;
        if (proto.hasScale()) {
            scale = proto.getScale();
        }

        Float emissiveStrength = base != null ? base.emissiveStrength : null;
        if (proto.hasEmissiveStrength()) {
            emissiveStrength = proto.getEmissiveStrength();
        }

        Float bloomStrength = base != null ? base.bloomStrength : null;
        if (proto.hasBloomStrength()) {
            bloomStrength = proto.getBloomStrength();
        }

        Float primaryFadeSeconds = base != null ? base.primaryFadeSeconds : null;
        if (proto.hasPrimaryFadeSeconds()) {
            primaryFadeSeconds = proto.getPrimaryFadeSeconds();
        }

        float[] bloomOffset = base != null ? base.bloomOffset : null;
        if (proto.getBloomOffsetCount() >= 3) {
            bloomOffset = toArray(proto.getBloomOffsetList());
        }

        Integer bloomPasses = base != null ? base.bloomPasses : null;
        if (proto.hasBloomPasses()) {
            bloomPasses = proto.getBloomPasses();
        }

        Float bloomScaleStep = base != null ? base.bloomScaleStep : null;
        if (proto.hasBloomScaleStep()) {
            bloomScaleStep = proto.getBloomScaleStep();
        }

        Float bloomDownscale = base != null ? base.bloomDownscale : null;
        if (proto.hasBloomDownscale()) {
            bloomDownscale = proto.getBloomDownscale();
        }
        return new EntityAttributeOverride(
            scale,
            emissiveStrength,
            bloomStrength,
            primaryFadeSeconds,
            bloomOffset,
            bloomPasses,
            bloomScaleStep,
            bloomDownscale
        );
    }

    private static float[] toArray(java.util.List<Float> list) {
        float[] arr = new float[3];
        for (int i = 0; i < 3 && i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
