package org.mybad.minecraft.event;

import org.mybad.core.animation.Animation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

final class OverlayCursorMap {
    private final Map<Animation, EventCursor> cursors = new HashMap<>();

    EventCursor getOrCreate(Animation animation) {
        EventCursor cursor = cursors.get(animation);
        if (cursor == null) {
            cursor = new EventCursor();
            cursors.put(animation, cursor);
        }
        return cursor;
    }

    void prune(Set<Animation> active) {
        cursors.keySet().removeIf(anim -> !active.contains(anim));
    }

    Collection<EventCursor> values() {
        return cursors.values();
    }

    void clear() {
        cursors.clear();
    }
}
