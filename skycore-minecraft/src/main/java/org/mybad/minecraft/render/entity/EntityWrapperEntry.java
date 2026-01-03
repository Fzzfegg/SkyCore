package org.mybad.minecraft.render.entity;

import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelHandle;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class EntityWrapperEntry {
    final BedrockModelHandle wrapper;
    final EntityAnimationController controller;
    final UUID entityUuid;
    final String mappingName;
    long lastSeenTick;
    List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
    final OverlayEventCursorCache overlayCursors = new OverlayEventCursorCache();
    final AnimationEventState renderState = new AnimationEventState();

    EntityWrapperEntry(BedrockModelHandle wrapper, EntityAnimationController controller, UUID entityUuid, String mappingName, long lastSeenTick) {
        this.wrapper = wrapper;
        this.controller = controller;
        this.entityUuid = entityUuid;
        this.mappingName = mappingName;
        this.lastSeenTick = lastSeenTick;
    }
}
