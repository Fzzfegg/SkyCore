package org.mybad.minecraft.animation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import org.mybad.core.animation.Animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chameleon-style animation controller.
 * Selects a primary action and manages secondary overlays (attack/spawn/etc).
 */
public class EntityAnimationController {
    public static final class OverlayState {
        public final Animation animation;
        public final float time;
        public final float weight;

        public OverlayState(Animation animation, float time, float weight) {
            this.animation = animation;
            this.time = time;
            this.weight = weight;
        }
    }

    public static final class Frame {
        public final Animation primary;
        public final List<OverlayState> overlays;

        public Frame(Animation primary, List<OverlayState> overlays) {
            this.primary = primary;
            this.overlays = overlays;
        }
    }

    private static final float DEFAULT_FADE_IN = 0.08f;
    private static final float DEFAULT_FADE_OUT = 0.12f;
    private static final float ATTACK_FADE_IN = 0.06f;
    private static final float ATTACK_FADE_OUT = 0.10f;
    private static final float SPAWN_FADE_IN = 0.12f;
    private static final float SPAWN_FADE_OUT = 0.15f;

    private final Map<String, Animation> actions;
    private String currentAction;
    private double prevX = Double.NaN;
    private double prevZ = Double.NaN;
    private double prevMY;
    private boolean wasOnGround = true;
    private boolean spawnPlayed = false;
    private long lastUpdateTime = 0L;
    private int lastTick = -1;

    private final List<OverlayAction> overlays = new ArrayList<>();

    private static final class OverlayAction {
        final String name;
        final Animation animation;
        float time;
        final float fadeIn;
        final float fadeOut;
        boolean finished;

        OverlayAction(String name, Animation animation, float fadeIn, float fadeOut) {
            this.name = name;
            this.animation = animation;
            this.fadeIn = fadeIn;
            this.fadeOut = fadeOut;
        }
    }

    public EntityAnimationController(Map<String, Animation> actions) {
        this.actions = new HashMap<>();
        if (actions != null) {
            for (Map.Entry<String, Animation> entry : actions.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String key = normalize(entry.getKey());
                this.actions.put(key, entry.getValue());
            }
        }
    }

    public Frame update(EntityLivingBase entity) {
        if (entity == null || actions.isEmpty()) {
            return null;
        }

        float deltaTime = computeDeltaTime();
        int tick = entity.ticksExisted;
        boolean newTick = tick != lastTick;
        Animation primaryAnim = null;
        if (newTick) {
            String nextPrimary = selectAction(entity);
            if (nextPrimary != null && !nextPrimary.equals(currentAction)) {
                currentAction = nextPrimary;
                primaryAnim = actions.get(nextPrimary);
            }
            triggerSecondaryActions(entity);
            lastTick = tick;
        }
        List<OverlayState> overlayStates = updateOverlays(deltaTime);

        if (primaryAnim == null && overlayStates.isEmpty()) {
            return null;
        }
        return new Frame(primaryAnim, overlayStates);
    }

    public String getCurrentAction() {
        return currentAction;
    }

    private float computeDeltaTime() {
        long now = System.currentTimeMillis();
        float delta;
        if (lastUpdateTime == 0L) {
            delta = 0f;
        } else {
            delta = (now - lastUpdateTime) / 1000.0f;
            if (delta > 0.1f) {
                delta = 0.1f;
            } else if (delta < 0f) {
                delta = 0f;
            }
        }
        lastUpdateTime = now;
        return delta;
    }

    private void triggerSecondaryActions(EntityLivingBase entity) {
        if (!spawnPlayed && entity.ticksExisted <= 1) {
            if (actions.containsKey("spawn")) {
                startOverlay("spawn", SPAWN_FADE_IN, SPAWN_FADE_OUT);
                spawnPlayed = true;
            }
        }

        if (entity.isSwingInProgress && entity.swingProgress == 0F) {
            startOverlay("attack", ATTACK_FADE_IN, ATTACK_FADE_OUT);
        }

        if (entity.hurtTime == entity.maxHurtTime - 1) {
            startOverlay("hurt", DEFAULT_FADE_IN, DEFAULT_FADE_OUT);
        }

        if (!entity.onGround && wasOnGround && Math.abs(entity.motionY) > 0.2F) {
            startOverlay("jump", DEFAULT_FADE_IN, DEFAULT_FADE_OUT);
            wasOnGround = false;
        }

        if (entity.onGround && !wasOnGround && prevMY < -0.5) {
            startOverlay("land", DEFAULT_FADE_IN, DEFAULT_FADE_OUT);
        }
    }

    private List<OverlayState> updateOverlays(float deltaTime) {
        List<OverlayState> result = new ArrayList<>();
        if (overlays.isEmpty()) {
            return result;
        }
        Iterator<OverlayAction> it = overlays.iterator();
        while (it.hasNext()) {
            OverlayAction overlay = it.next();
            if (overlay.finished) {
                it.remove();
                continue;
            }
            float speed = overlay.animation != null ? overlay.animation.getSpeed() : 1f;
            float length = overlay.animation != null ? overlay.animation.getLength() : 0f;
            overlay.time += deltaTime * speed;
            float weight = computeFadeWeight(overlay, length);
            if (length > 0f && overlay.time >= length) {
                overlay.finished = true;
            }
            if (!overlay.finished && weight > 0f) {
                result.add(new OverlayState(overlay.animation, overlay.time, weight));
            }
        }
        return result;
    }

    private float computeFadeWeight(OverlayAction overlay, float length) {
        if (length <= 0f) {
            return 0f;
        }
        if (overlay.time < overlay.fadeIn && overlay.fadeIn > 0f) {
            return overlay.time / overlay.fadeIn;
        }
        float remaining = length - overlay.time;
        if (remaining < overlay.fadeOut && overlay.fadeOut > 0f) {
            return Math.max(0f, remaining / overlay.fadeOut);
        }
        return 1f;
    }

    private void startOverlay(String name, float fadeIn, float fadeOut) {
        String key = normalize(name);
        Animation anim = actions.get(key);
        if (anim == null) {
            return;
        }
        for (OverlayAction existing : overlays) {
            if (existing.name.equals(key) && !existing.finished) {
                existing.time = 0f;
                existing.finished = false;
                return;
            }
        }
        overlays.add(new OverlayAction(key, anim, fadeIn, fadeOut));
    }

    private String selectAction(EntityLivingBase entity) {
        if (Double.isNaN(prevX)) {
            prevX = entity.posX;
            prevZ = entity.posZ;
            prevMY = entity.motionY;
            wasOnGround = entity.onGround;
        }

        double dx = entity.posX - prevX;
        double dz = entity.posZ - prevZ;
        boolean creativeFlying = entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying;
        boolean wet = entity.isInWater();
        float threshold = creativeFlying ? 0.1F : (wet ? 0.025F : 0.01F);
        boolean moves = Math.abs(dx) > threshold || Math.abs(dz) > threshold;

        String action;

        if (entity.getHealth() <= 0) {
            action = pick("dying", "death");
        } else if (entity.isPlayerSleeping()) {
            action = pick("sleeping", "sleep");
        } else if (wet) {
            action = moves ? pick("swimming", "swim", "running", "walking", "walk") : pick("swimming_idle", "swim_idle", "idle");
        } else if (entity.isRiding()) {
            Entity riding = entity.getRidingEntity();
            if (riding != null) {
                moves = Math.abs(riding.posX - prevX) > threshold || Math.abs(riding.posZ - prevZ) > threshold;
                prevX = riding.posX;
                prevZ = riding.posZ;
            }
            action = moves ? pick("riding", "ride", "running", "walking", "walk") : pick("riding_idle", "ride_idle", "idle");
        } else if (creativeFlying || entity.isElytraFlying()) {
            action = moves ? pick("flying", "fly", "running", "walking", "walk") : pick("flying_idle", "fly_idle", "idle");
        } else {
            if (entity.isSneaking()) {
                action = moves ? pick("crouching", "crouch", "running", "walking", "walk") : pick("crouching_idle", "crouch_idle", "idle");
            } else if (!entity.onGround && entity.motionY < 0 && entity.fallDistance > 1.25F) {
                action = pick("falling", "fall");
            } else if (entity.isSprinting()) {
                action = pick("sprinting", "run", "running", "walking", "walk");
            } else {
                action = moves ? pick("running", "walking", "walk") : pick("idle");
            }
        }

        prevX = entity.posX;
        prevZ = entity.posZ;
        prevMY = entity.motionY;
        wasOnGround = entity.onGround;

        if (action == null) {
            action = pick("idle", "running", "walking", "walk");
        }
        return action;
    }

    private String pick(String... keys) {
        for (String key : keys) {
            String normalized = normalize(key);
            if (actions.containsKey(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
