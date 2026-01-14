package org.mybad.minecraft.render.entity;

import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventContext;
import org.mybad.minecraft.render.entity.events.AnimationEventState;
import org.mybad.minecraft.render.entity.events.OverlayEventCursorCache;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class EntityWrapperEntry implements AnimationEventContext {
    public final BedrockModelHandle wrapper;
    public final EntityAnimationController controller;
    public final UUID entityUuid;
    public final String mappingName;
    public long lastSeenTick;
    public long lastAnimationTick = Long.MIN_VALUE;
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
    @Override
    public AnimationEventState getPrimaryEventState() {
        return renderState;
    }

    @Override
    public List<EntityAnimationController.OverlayState> getOverlayStates() {
        return overlayStates;
    }

    @Override
    public OverlayEventCursorCache getOverlayCursorCache() {
        return overlayCursors;
    }
}
