package org.mybad.minecraft.event;

import org.mybad.core.animation.Animation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ForcedAnimationRegistry {
    private final Map<String, Animation> forcedAnimations = new ConcurrentHashMap<>();

    Animation get(String mappingName) {
        if (mappingName == null || mappingName.isEmpty()) {
            return null;
        }
        return forcedAnimations.get(mappingName);
    }

    boolean set(String mappingName, Animation animation, Iterable<WrapperEntry> entries) {
        if (mappingName == null || mappingName.isEmpty() || animation == null) {
            return false;
        }
        forcedAnimations.put(mappingName, animation);
        if (entries != null) {
            for (WrapperEntry entry : entries) {
                if (entry != null && mappingName.equals(entry.mappingName)) {
                    entry.wrapper.setAnimation(animation);
                    entry.wrapper.clearOverlayStates();
                }
            }
        }
        return true;
    }

    void clear(String mappingName) {
        if (mappingName == null || mappingName.isEmpty()) {
            return;
        }
        forcedAnimations.remove(mappingName);
    }

    void clearAll() {
        forcedAnimations.clear();
    }
}
