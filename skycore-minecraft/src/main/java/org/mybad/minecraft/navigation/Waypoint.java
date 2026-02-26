package org.mybad.minecraft.navigation;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.mybad.skycoreproto.SkyCoreProto;

import javax.annotation.Nullable;

final class Waypoint {

    enum Status {
        ACTIVE,
        COMPLETED,
        FAILED;

        static Status fromProto(SkyCoreProto.WaypointTarget.Status proto) {
            if (proto == null) {
                return ACTIVE;
            }
            switch (proto) {
                case COMPLETED:
                    return COMPLETED;
                case FAILED:
                    return FAILED;
                case ACTIVE:
                default:
                    return ACTIVE;
            }
        }
    }

    private final String id;
    private final int order;
    private final Vec3d position;
    private final String styleId;
    private final Status status;
    private final java.util.List<String> presetLabels;
    private final String presetResource;
    private final float presetMaxDistance;
    private final float presetClearDistance;
    private final String npcId;
    private final float npcSearchRadius;
    private final boolean useNpcPosition;

    Waypoint(String id,
             int order,
             Vec3d position,
             String styleId,
             Status status,
             java.util.List<String> presetLabels,
             String presetResource,
             float presetMaxDistance,
             float presetClearDistance,
             String npcId,
             float npcSearchRadius,
             boolean useNpcPosition) {
        this.id = id;
        this.order = order;
        this.position = position;
        this.styleId = styleId;
        this.status = status;
        this.presetLabels = presetLabels == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(presetLabels);
        this.presetResource = presetResource;
        this.presetMaxDistance = presetMaxDistance;
        this.presetClearDistance = presetClearDistance;
        this.npcId = npcId != null && !npcId.isEmpty() ? npcId : null;
        this.npcSearchRadius = npcSearchRadius > 0f ? npcSearchRadius : 0f;
        this.useNpcPosition = useNpcPosition && this.npcId != null;
    }

    static @Nullable Waypoint fromProto(SkyCoreProto.WaypointTarget proto) {
        if (proto == null || proto.getId().isEmpty()) {
            return null;
        }
        String style = proto.getStyle().isEmpty() ? "default" : proto.getStyle();
        Vec3d pos = new Vec3d(proto.getX(), proto.getY(), proto.getZ());
        int safeOrder = proto.getOrder();
        safeOrder = MathHelper.clamp(safeOrder, Integer.MIN_VALUE, Integer.MAX_VALUE);
        Status status = Status.fromProto(proto.getStatus());
        java.util.List<String> labels = java.util.Collections.emptyList();
        String resource = "";
        float maxDistance = 0f;
        float clearDistance = 0f;
        String npcId = null;
        float npcRadius = 0f;
        boolean useNpcPosition = false;
        if (proto.hasPreset()) {
            labels = new java.util.ArrayList<>(proto.getPreset().getLabelsList());
            resource = proto.getPreset().getResource();
            maxDistance = proto.getPreset().getMaxDistance();
            clearDistance = proto.getPreset().getClearDistance();
            npcId = proto.getPreset().getNpcId();
            npcRadius = proto.getPreset().getNpcSearchRadius();
            useNpcPosition = proto.getPreset().getUseNpcPosition();
        }
        return new Waypoint(proto.getId(), safeOrder, pos, style, status, labels, resource, maxDistance, clearDistance, npcId, npcRadius, useNpcPosition);
    }

    String getId() {
        return id;
    }

    int getOrder() {
        return order;
    }

    Vec3d getPosition() {
        return position;
    }

    String getStyleId() {
        return styleId;
    }

    Status getStatus() {
        return status;
    }

    boolean isActive() {
        return status == Status.ACTIVE;
    }

    double distanceTo(Vec3d other) {
        return position.distanceTo(other);
    }

    java.util.List<String> getPresetLabels() {
        return presetLabels;
    }

    String getPresetResource() {
        return presetResource;
    }

    float getPresetMaxDistance() {
        return presetMaxDistance;
    }

    float getPresetClearDistance() {
        return presetClearDistance;
    }

    boolean hasPresetLabels() {
        return !presetLabels.isEmpty();
    }

    boolean shouldAutoClear(double distance) {
        return presetClearDistance > 0f && distance <= presetClearDistance;
    }

    boolean hasNpcBinding() {
        return npcId != null && !npcId.isEmpty();
    }

    String getNpcId() {
        return npcId;
    }

    float getNpcSearchRadius() {
        return npcSearchRadius;
    }

    boolean shouldUseNpcPosition() {
        return useNpcPosition;
    }
}
