package org.mybad.minecraft.render.entity.events;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundCategory;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.audio.DirectSoundPlayer;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.trail.WeaponTrailController;

import java.util.List;

/**
 * Dispatches particle and sound events from animations.
 */
public final class AnimationEventDispatcher {
    private static final float EVENT_EPS = 1.0e-4f;
    private final OverlayEventDispatcher overlayDispatcher = new OverlayEventDispatcher();

    public void dispatchAnimationEvents(EntityLivingBase entity,
                                        AnimationEventContext context,
                                        AnimationEventTarget target,
                                        BedrockModelHandle wrapper,
                                        float partialTicks) {
        if (context == null || wrapper == null) {
            return;
        }
        AnimationEventState renderState = context.getPrimaryEventState();
        if (renderState == null) {
            return;
        }
        AnimationPlayer primaryPlayer = wrapper.getActiveAnimationPlayer();
        if (primaryPlayer != null && primaryPlayer.getAnimation() != null) {
            Animation animation = primaryPlayer.getAnimation();
            float currentTime = primaryPlayer.getState().getCurrentTime();
            int loopCount = primaryPlayer.getState().getLoopCount();
            if (!renderState.primaryValid || renderState.lastPrimaryAnimation != animation) {
                renderState.lastPrimaryAnimation = animation;
                renderState.lastPrimaryTime = currentTime;
                renderState.lastPrimaryLoop = loopCount;
                renderState.primaryValid = true;
                if (currentTime >= -EVENT_EPS) {
                    dispatchEventsForAnimation(entity, context, target, wrapper, animation,
                        0f, currentTime, false, partialTicks);
                }
            } else {
                boolean looped = loopCount != renderState.lastPrimaryLoop ||
                    (animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < renderState.lastPrimaryTime);
                float prevTime = renderState.lastPrimaryTime;
                boolean wrapAtEnd = !looped
                    && Math.abs(prevTime - animation.getLength()) <= EVENT_EPS
                    && Math.abs(currentTime - 0f) <= EVENT_EPS;
                if (wrapAtEnd) {
                    dispatchEventsForAnimation(entity, context, target, wrapper, animation,
                        prevTime, animation.getLength(), false, partialTicks);
                    dispatchEventsForAnimation(entity, context, target, wrapper, animation,
                        0f, currentTime, false, partialTicks);
                } else {
                    dispatchEventsForAnimation(entity, context, target, wrapper, animation,
                        prevTime, currentTime, looped, partialTicks);
                }
                renderState.lastPrimaryTime = currentTime;
                renderState.lastPrimaryLoop = loopCount;
            }
        } else {
            renderState.primaryValid = false;
        }

        overlayDispatcher.dispatch(entity, context, target, wrapper, partialTicks, this);
    }

    void dispatchEventsForAnimation(EntityLivingBase entity,
                                    AnimationEventContext context,
                                    AnimationEventTarget target,
                                    BedrockModelHandle wrapper,
                                    Animation animation,
                                    float prevTime,
                                    float currentTime,
                                    boolean looped,
                                    float partialTicks) {
        if (animation == null) {
            return;
        }
        if (!looped && currentTime + EVENT_EPS < prevTime - EVENT_EPS) {
            if (!animation.getSoundEvents().isEmpty()) {
                dispatchEventList(entity, context, target, wrapper, animation, animation.getSoundEvents(),
                    prevTime, animation.getLength(), false, partialTicks);
                dispatchEventList(entity, context, target, wrapper, animation, animation.getSoundEvents(),
                    0f, currentTime, false, partialTicks);
            }
            if (!animation.getParticleEvents().isEmpty()) {
                dispatchEventList(entity, context, target, wrapper, animation, animation.getParticleEvents(),
                    prevTime, animation.getLength(), false, partialTicks);
                dispatchEventList(entity, context, target, wrapper, animation, animation.getParticleEvents(),
                    0f, currentTime, false, partialTicks);
            }
            return;
        }
        if (!animation.getSoundEvents().isEmpty()) {
            dispatchEventList(entity, context, target, wrapper, animation, animation.getSoundEvents(),
                prevTime, currentTime, looped, partialTicks);
        }
        if (!animation.getParticleEvents().isEmpty()) {
            dispatchEventList(entity, context, target, wrapper, animation, animation.getParticleEvents(),
                prevTime, currentTime, looped, partialTicks);
        }
    }

    private void dispatchEventList(EntityLivingBase entity,
                                   AnimationEventContext context,
                                   AnimationEventTarget target,
                                   BedrockModelHandle wrapper,
                                   Animation animation,
                                   List<Animation.Event> events,
                                   float prevTime,
                                   float currentTime,
                                   boolean looped,
                                   float partialTicks) {
        if (events == null || events.isEmpty()) {
            return;
        }
        if (!looped) {
            for (Animation.Event event : events) {
                if (event == null) {
                    continue;
                }
                float t = event.getTimestamp();
                if (t >= prevTime - EVENT_EPS && t <= currentTime + EVENT_EPS) {
                    fireEvent(entity, context, target, wrapper, event, partialTicks);
                }
            }
            return;
        }

        float length = animation.getLength();
        for (Animation.Event event : events) {
            if (event == null) {
                continue;
            }
            float t = event.getTimestamp();
            if (t > prevTime + EVENT_EPS && t <= length + EVENT_EPS) {
                fireEvent(entity, context, target, wrapper, event, partialTicks);
            } else if (t >= -EVENT_EPS && t <= currentTime + EVENT_EPS) {
                fireEvent(entity, context, target, wrapper, event, partialTicks);
            }
        }
    }

    private void fireEvent(EntityLivingBase entity,
                           AnimationEventContext context,
                           AnimationEventTarget target,
                           BedrockModelHandle wrapper,
                           Animation.Event event,
                           float partialTicks) {
        if (event == null) {
            return;
        }
        float positionYaw = resolveHeadYaw(entity, target, partialTicks);
        if (event.getType() == Animation.Event.Type.PARTICLE) {
            spawnParticleEffect(event.getEffect(), entity, target, wrapper, event.getLocator(),
                positionYaw, partialTicks);
            return;
        }
        if (event.getType() == Animation.Event.Type.TRAIL) {
            triggerTrailEvent(context, event);
            return;
        }
        double[] pos = resolveEventPosition(entity, target, wrapper, event.getLocator(), positionYaw, partialTicks);
        if (pos == null) {
            return;
        }
        playSoundEffect(event.getEffect(), pos[0], pos[1], pos[2]);
    }

    private void spawnParticleEffect(String effect,
                                     EntityLivingBase entity,
                                     AnimationEventTarget target,
                                     BedrockModelHandle wrapper,
                                     String locatorName,
                                     float positionYaw,
                                     float partialTicks) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        AnimationEventArgsParser.ParticleParams params = AnimationEventArgsParser.parseParticle(effect);
        if (params == null || params.path == null || params.path.isEmpty()) {
            return;
        }
        BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
        if (system == null) {
            return;
        }
        double[] initialPos = resolveEventPosition(entity, target, wrapper, locatorName, positionYaw, partialTicks);
        if (entity == null) {
            if (target != null) {
                system.spawn(params.path,
                    new BlockAnimationEventTransformProvider(target, wrapper, locatorName, positionYaw, params),
                    params.count);
                return;
            }
            double px = initialPos != null && initialPos.length > 0 ? initialPos[0] : getBaseX(target);
            double py = initialPos != null && initialPos.length > 1 ? initialPos[1] : getBaseY(target);
            double pz = initialPos != null && initialPos.length > 2 ? initialPos[2] : getBaseZ(target);
            system.spawn(params.path, px, py, pz, params.count);
            return;
        }
        float emitterYaw = AnimationEventMathUtil.resolveEmitterYaw(entity, partialTicks, params);
        double ix = initialPos != null && initialPos.length > 0 ? initialPos[0] : entity.posX;
        double iy = initialPos != null && initialPos.length > 1 ? initialPos[1] : entity.posY;
        double iz = initialPos != null && initialPos.length > 2 ? initialPos[2] : entity.posZ;
        system.spawn(params.path, new AnimationEventTransformProvider(entity, wrapper, locatorName, ix, iy, iz, emitterYaw,
            positionYaw, params.mode, params.yawOffset, params.expireOnDeath), params.count);
    }

    private void triggerTrailEvent(AnimationEventContext context, Animation.Event event) {
        if (context == null || event == null) {
            return;
        }
        WeaponTrailController controller = context.getTrailController();
        if (controller == null) {
            return;
        }
        AnimationEventArgsParser.TrailParams params = AnimationEventArgsParser.parseTrail(event.getEffect());
        if (params == null) {
            return;
        }
        if ((params.locatorStart == null || params.locatorStart.isEmpty()) && event.getLocator() != null) {
            params.locatorStart = event.getLocator();
        }
        controller.handle(params);
    }

    private double[] resolveEventPosition(EntityLivingBase entity,
                                          AnimationEventTarget target,
                                          BedrockModelHandle wrapper,
                                          String locatorName,
                                          float positionYaw,
                                          float partialTicks) {
        if (entity != null) {
            return AnimationEventMathUtil.resolveEventPosition(entity, wrapper, locatorName, positionYaw, partialTicks);
        }
        double baseX = getBaseX(target);
        double baseY = getBaseY(target);
        double baseZ = getBaseZ(target);
        return AnimationEventMathUtil.resolveEventPosition(null, wrapper, locatorName, positionYaw, baseX, baseY, baseZ);
    }

    private float resolveHeadYaw(EntityLivingBase entity, AnimationEventTarget target, float partialTicks) {
        if (entity != null) {
            return AnimationEventMathUtil.resolveHeadYaw(entity, partialTicks);
        }
        return target != null ? target.getHeadYaw() : 0.0f;
    }

    private double getBaseX(AnimationEventTarget target) {
        return target != null ? target.getBaseX() : 0.0;
    }

    private double getBaseY(AnimationEventTarget target) {
        return target != null ? target.getBaseY() : 0.0;
    }

    private double getBaseZ(AnimationEventTarget target) {
        return target != null ? target.getBaseZ() : 0.0;
    }

    private void playSoundEffect(String effect, double x, double y, double z) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        AnimationEventArgsParser.SoundParams params = AnimationEventArgsParser.parseSound(effect);
        if (params == null || params.soundId == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        DirectSoundPlayer.play(params.soundId, SoundCategory.NEUTRAL, params.volume, params.pitch, x, y, z);
    }
}
