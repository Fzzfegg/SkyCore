package org.mybad.minecraft.render;

import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.core.data.Model;
import org.mybad.minecraft.animation.EntityAnimationController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles animation playback, overlays, and cross-fading.
 */
final class AnimationBridge {
    private AnimationPlayer animationPlayer;
    private AnimationPlayer activePlayer;
    private AnimationPlayer previousPlayer;
    private float primaryFadeTime;
    private float primaryFadeDuration = 0.12f;
    private final Map<Animation, AnimationPlayer> overlayPlayers = new HashMap<>();
    private List<EntityAnimationController.OverlayState> overlayStates = Collections.emptyList();
    private long lastUpdateTime = System.currentTimeMillis();

    AnimationBridge(Animation animation) {
        if (animation != null) {
            animationPlayer = new AnimationPlayer(animation);
            animationPlayer.play();
            activePlayer = animationPlayer;
        }
    }

    void setPrimaryFadeDuration(float seconds) {
        if (Float.isNaN(seconds) || seconds < 0f) {
            return;
        }
        this.primaryFadeDuration = seconds;
    }

    void setAnimation(Animation animation) {
        overlayPlayers.clear();
        overlayStates = Collections.emptyList();
        if (animation != null) {
            if (animationPlayer == null || animationPlayer.getAnimation() != animation) {
                animationPlayer = new AnimationPlayer(animation);
                animationPlayer.play();
            }
        } else {
            animationPlayer = null;
        }
        beginPrimaryTransition(animationPlayer);
    }

    void restartAnimation() {
        if (animationPlayer != null) {
            animationPlayer.restart();
        }
    }

    AnimationPlayer getAnimationPlayer() {
        return animationPlayer;
    }

    AnimationPlayer getActiveAnimationPlayer() {
        return activePlayer != null ? activePlayer : animationPlayer;
    }

    void setOverlayStates(List<EntityAnimationController.OverlayState> states) {
        if (states == null || states.isEmpty()) {
            overlayStates = Collections.emptyList();
            return;
        }
        overlayStates = new ArrayList<>(states);
    }

    void clearOverlayStates() {
        overlayStates = Collections.emptyList();
    }

    void dispose() {
        overlayPlayers.clear();
        overlayStates = Collections.emptyList();
        activePlayer = null;
        previousPlayer = null;
    }

    boolean updateAndApply(Model model) {
        update();
        return apply(model);
    }

    boolean update() {
        updateAnimation();
        return hasActiveAnimation();
    }

    boolean apply(Model model) {
        if (model == null) {
            return false;
        }
        if (!hasActiveAnimation()) {
            model.resetToBindPose();
            return false;
        }
        model.resetToBindPose();
        AnimationPlayer player = activePlayer;
        if (player != null) {
            player.apply(model);
        }
        float previousWeight = getPrimaryFadeWeight();
        if (previousPlayer != null && previousWeight > 0f) {
            previousPlayer.apply(model, previousWeight);
        }
        applyOverlays(model);
        return true;
    }

    private void updateAnimation() {
        if (animationPlayer == null && activePlayer == null && previousPlayer == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0F;
        lastUpdateTime = currentTime;

        if (deltaTime > 0.1F) {
            deltaTime = 0.1F;
        }

        AnimationPlayer desiredPlayer = null;
        if (animationPlayer != null) {
            animationPlayer.update(deltaTime);
            desiredPlayer = animationPlayer;
        }

        if (desiredPlayer != activePlayer) {
            beginPrimaryTransition(desiredPlayer);
        }

        if (previousPlayer != null) {
            previousPlayer.update(deltaTime);
            primaryFadeTime += deltaTime;
            if (primaryFadeTime >= primaryFadeDuration) {
                previousPlayer = null;
            }
        }
    }

    private void applyOverlays(Model model) {
        if (overlayStates.isEmpty()) {
            return;
        }
        for (EntityAnimationController.OverlayState state : overlayStates) {
            if (state == null || state.weight <= 0f || state.animation == null) {
                continue;
            }
            AnimationPlayer player = overlayPlayers.get(state.animation);
            if (player == null) {
                player = new AnimationPlayer(state.animation);
                player.play();
                overlayPlayers.put(state.animation, player);
            }
            player.setCurrentTime(state.time);
            player.apply(model, state.weight);
        }
    }

    private void beginPrimaryTransition(AnimationPlayer next) {
        if (next == activePlayer) {
            return;
        }
        if (primaryFadeDuration > 0f) {
            previousPlayer = activePlayer;
            primaryFadeTime = 0f;
        } else {
            previousPlayer = null;
            primaryFadeTime = primaryFadeDuration;
        }
        activePlayer = next;
    }

    private float getPrimaryFadeWeight() {
        if (previousPlayer == null || primaryFadeDuration <= 0f) {
            return 0f;
        }
        float t = primaryFadeTime / primaryFadeDuration;
        if (t >= 1f) {
            return 0f;
        }
        return 1f - t;
    }

    private boolean hasActiveAnimation() {
        return activePlayer != null || previousPlayer != null || !overlayStates.isEmpty();
    }
}
