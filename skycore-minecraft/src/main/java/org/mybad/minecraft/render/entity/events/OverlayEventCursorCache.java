package org.mybad.minecraft.render.entity.events;

import org.mybad.core.animation.Animation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class OverlayEventCursorCache {
    private final Map<Animation, OverlayEventCursor> cursors = new HashMap<>();

    OverlayEventCursor getOrCreate(Animation animation) {
        OverlayEventCursor cursor = cursors.get(animation);
        if (cursor == null) {
            cursor = new OverlayEventCursor();
            cursors.put(animation, cursor);
        }
        return cursor;
    }

    void prune(Set<Animation> active) {
        cursors.keySet().removeIf(anim -> !active.contains(anim));
    }

    Collection<OverlayEventCursor> values() {
        return cursors.values();
    }

    void clear() {
        cursors.clear();
    }
}
