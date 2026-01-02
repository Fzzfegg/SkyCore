package org.mybad.minecraft.render.entity;

import org.mybad.core.animation.Animation;

final class AnimationEventState {
    float lastPrimaryTime;
    int lastPrimaryLoop;
    Animation lastPrimaryAnimation;
    boolean primaryValid;
}
