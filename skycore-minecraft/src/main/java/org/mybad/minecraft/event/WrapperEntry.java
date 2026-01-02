package org.mybad.minecraft.event;

import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelWrapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

final class WrapperEntry {
    final BedrockModelWrapper wrapper;
    final EntityAnimationController controller;
    final UUID entityUuid;
    final String mappingName;
    long lastSeenTick;
    List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
    final OverlayCursorMap overlayCursors = new OverlayCursorMap();
    final EntityRenderState renderState = new EntityRenderState();

    WrapperEntry(BedrockModelWrapper wrapper, EntityAnimationController controller, UUID entityUuid, String mappingName, long lastSeenTick) {
        this.wrapper = wrapper;
        this.controller = controller;
        this.entityUuid = entityUuid;
        this.mappingName = mappingName;
        this.lastSeenTick = lastSeenTick;
    }
}
