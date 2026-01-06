package org.mybad.minecraft.render.entity;

import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventState;
import org.mybad.minecraft.render.entity.events.OverlayEventCursorCache;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class EntityWrapperEntry {
    public final BedrockModelHandle wrapper;
    public final EntityAnimationController controller;
    public final UUID entityUuid;
    public final String mappingName;
    public long lastSeenTick;
    public List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
    public final OverlayEventCursorCache overlayCursors = new OverlayEventCursorCache();
    public final AnimationEventState renderState = new AnimationEventState();

    EntityWrapperEntry(BedrockModelHandle wrapper, EntityAnimationController controller, UUID entityUuid, String mappingName, long lastSeenTick) {
        this.wrapper = wrapper;
        this.controller = controller;
        this.entityUuid = entityUuid;
        this.mappingName = mappingName;
        this.lastSeenTick = lastSeenTick;
    }
}
