package org.mybad.minecraft.gltf.client.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteAnimationController {

    private static final Map<UUID, RemoteAnimationState> ACTIVE = new ConcurrentHashMap<>();

    private RemoteAnimationController() {
    }

    public static void store(RemoteAnimationState state) {
        if (state == null) {
            return;
        }
        ACTIVE.put(state.getSubjectId(), state);
    }

    public static RemoteAnimationState getState(UUID subjectId) {
        if (subjectId == null) {
            return null;
        }
        RemoteAnimationState state = ACTIVE.get(subjectId);
        if (state == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (state.isExpired(now)) {
            ACTIVE.remove(subjectId);
            return null;
        }
        return state;
    }

    public static void clear(UUID subjectId) {
        if (subjectId != null) {
            ACTIVE.remove(subjectId);
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
    }
}
