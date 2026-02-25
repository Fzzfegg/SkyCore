package org.mybad.minecraft.navigation;

import org.mybad.minecraft.SkyCoreMod;

import javax.annotation.Nullable;
import java.util.*;

final class WaypointManager {

    private static final Comparator<Waypoint> ORDERING = Comparator
        .comparingInt(Waypoint::getOrder)
        .thenComparing(Waypoint::getId);

    private final Map<String, Waypoint> waypoints = new HashMap<>();
    private String currentTrackedId;

    synchronized void upsert(Waypoint waypoint) {
        waypoints.put(waypoint.getId(), waypoint);
        SkyCoreMod.LOGGER.debug("[Waypoint] upsert {} order={} style={} status={}",
            waypoint.getId(), waypoint.getOrder(), waypoint.getStyleId(), waypoint.getStatus());
        refreshTrackingLocked();
    }

    synchronized void remove(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        Waypoint removed = waypoints.remove(id);
        if (removed != null) {
            SkyCoreMod.LOGGER.debug("[Waypoint] remove {}", id);
        }
        if (removed != null && id.equals(currentTrackedId)) {
            currentTrackedId = null;
        }
        refreshTrackingLocked();
    }

    synchronized void clear() {
        if (!waypoints.isEmpty()) {
            SkyCoreMod.LOGGER.debug("[Waypoint] clearing {} entries", waypoints.size());
        }
        waypoints.clear();
        currentTrackedId = null;
    }

    synchronized List<Waypoint> getOrderedWaypoints() {
        List<Waypoint> list = new ArrayList<>(waypoints.values());
        list.sort(ORDERING);
        return list;
    }

    synchronized Set<String> snapshotIds() {
        return new HashSet<>(waypoints.keySet());
    }

    synchronized @Nullable Waypoint getCurrentTracked() {
        if (currentTrackedId == null) {
            return null;
        }
        return waypoints.get(currentTrackedId);
    }

    private void refreshTrackingLocked() {
        if (currentTrackedId != null) {
            Waypoint current = waypoints.get(currentTrackedId);
            if (current != null && current.isActive()) {
                return;
            }
        }
        currentTrackedId = waypoints.values().stream()
            .filter(Waypoint::isActive)
            .sorted(ORDERING)
            .map(Waypoint::getId)
            .findFirst()
            .orElse(null);
    }
}
