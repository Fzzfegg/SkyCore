package org.mybad.minecraft.render;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.Model;
import org.mybad.minecraft.animation.EntityAnimationController;

import java.util.List;

final class ModelAnimationController {
    private final AnimationBridge bridge;

    ModelAnimationController(AnimationBridge bridge) {
        this.bridge = bridge != null ? bridge : new AnimationBridge(null);
    }

    void updateAndApply(Model model) {
        bridge.updateAndApply(model);
    }

    boolean update() {
        return bridge.update();
    }

    boolean apply(Model model) {
        return bridge.apply(model);
    }

    void setAnimation(Animation animation) {
        bridge.setAnimation(animation);
    }

    void restartAnimation() {
        bridge.restartAnimation();
    }

    AnimationPlayer getAnimationPlayer() {
        return bridge.getAnimationPlayer();
    }

    AnimationPlayer getActiveAnimationPlayer() {
        return bridge.getActiveAnimationPlayer();
    }

    void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        bridge.setOverlayStates(states);
    }

    void clearOverlayStates() {
        bridge.clearOverlayStates();
    }

    void setPrimaryFadeDuration(float seconds) {
        bridge.setPrimaryFadeDuration(seconds);
    }

    void dispose() {
        bridge.dispose();
    }
}
