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

    Waypoint(String id, int order, Vec3d position, String styleId, Status status) {
        this.id = id;
        this.order = order;
        this.position = position;
        this.styleId = styleId;
        this.status = status;
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
        return new Waypoint(proto.getId(), safeOrder, pos, style, status);
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
}
