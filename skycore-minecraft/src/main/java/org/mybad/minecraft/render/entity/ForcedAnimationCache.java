package org.mybad.minecraft.render.entity;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ForcedAnimationCache {
    private final Map<UUID, Animation> forcedAnimations = new ConcurrentHashMap<>();
    private final Map<UUID, Animation> previousAnimations = new ConcurrentHashMap<>();

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
                    Animation previous = entry.getLastPrimaryAnimation();
                    if (previous == null) {
                        AnimationPlayer currentPlayer = entry.wrapper.getActiveAnimationPlayer();
                        if (currentPlayer != null) {
                            previous = currentPlayer.getAnimation();
                        }
                    }
                    if (previous != null) {
                        previousAnimations.put(entityUuid, previous);
                    }
                    entry.wrapper.setAnimation(animation);
                    entry.wrapper.restartAnimation();
                    entry.wrapper.clearOverlayStates();
                }
            }
        }
        return true;
    }

    Animation remove(UUID entityUuid) {
        if (entityUuid == null) {
            return null;
        }
        forcedAnimations.remove(entityUuid);
        return previousAnimations.remove(entityUuid);
    }

    void clearAll() {
        forcedAnimations.clear();
        previousAnimations.clear();
    }
}
