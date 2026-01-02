package org.mybad.minecraft.event;

import org.mybad.core.animation.Animation;

final class EntityRenderState {
    float lastPrimaryTime;
    int lastPrimaryLoop;
    Animation lastPrimaryAnimation;
    boolean primaryValid;
}
