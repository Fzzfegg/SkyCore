package org.mybad.minecraft.render.entity;

import org.mybad.core.animation.Animation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ForcedAnimationCache {
    private final Map<UUID, Animation> forcedAnimations = new ConcurrentHashMap<>();

    Animation get(UUID entityUuid) {
        if (entityUuid == null) {
            return null;
        }
        return forcedAnimations.get(entityUuid);
    }

    boolean set(UUID entityUuid, Animation animation, Iterable<EntityWrapperEntry> entries) {
        if (entityUuid == null || animation == null) {
            return false;
        }
        forcedAnimations.put(entityUuid, animation);
        if (entries != null) {
            for (EntityWrapperEntry entry : entries) {
                if (entry != null && entityUuid.equals(entry.entityUuid)) {
                    entry.wrapper.setAnimation(animation);
                    entry.wrapper.restartAnimation();
                    entry.wrapper.clearOverlayStates();
                }
            }
        }
        return true;
    }

    void clear(UUID entityUuid) {
        if (entityUuid == null) {
            return;
        }
        forcedAnimations.remove(entityUuid);
    }

    void clearAll() {
        forcedAnimations.clear();
    }
}
