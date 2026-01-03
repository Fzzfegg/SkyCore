package org.mybad.minecraft.render.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundCategory;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;
import org.mybad.minecraft.render.BedrockModelHandle;

import java.util.List;

/**
 * Dispatches particle and sound events from animations.
 */
final class AnimationEventDispatcher {
    private static final float EVENT_EPS = 1.0e-4f;
    private final OverlayEventDispatcher overlayDispatcher = new OverlayEventDispatcher();

    void dispatchAnimationEvents(EntityLivingBase entity, EntityWrapperEntry entry,
                                 BedrockModelHandle wrapper, float partialTicks) {
        AnimationPlayer primaryPlayer = wrapper.getActiveAnimationPlayer();
        if (primaryPlayer != null && primaryPlayer.getAnimation() != null) {
            Animation animation = primaryPlayer.getAnimation();
            float currentTime = primaryPlayer.getState().getCurrentTime();
            int loopCount = primaryPlayer.getState().getLoopCount();
            AnimationEventState renderState = entry.renderState;
            if (!renderState.primaryValid || renderState.lastPrimaryAnimation != animation) {
                renderState.lastPrimaryAnimation = animation;
                renderState.lastPrimaryTime = currentTime;
                renderState.lastPrimaryLoop = loopCount;
                renderState.primaryValid = true;
            } else {
                boolean looped = loopCount != renderState.lastPrimaryLoop ||
                    (animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < renderState.lastPrimaryTime);
                dispatchEventsForAnimation(entity, wrapper, animation, renderState.lastPrimaryTime, currentTime, looped, partialTicks);
                renderState.lastPrimaryTime = currentTime;
                renderState.lastPrimaryLoop = loopCount;
            }
        } else {
            entry.renderState.primaryValid = false;
        }

        overlayDispatcher.dispatch(entity, entry, wrapper, partialTicks, this);
    }

    void dispatchEventsForAnimation(EntityLivingBase entity, BedrockModelHandle wrapper, Animation animation,
                                    float prevTime, float currentTime, boolean looped,
                                    float partialTicks) {
        if (animation == null) {
            return;
        }
        if (!looped && currentTime + EVENT_EPS < prevTime) {
            return;
        }

        if (!animation.getSoundEvents().isEmpty()) {
            dispatchEventList(entity, wrapper, animation, animation.getSoundEvents(), prevTime, currentTime, looped, partialTicks);
        }
        if (!animation.getParticleEvents().isEmpty()) {
            dispatchEventList(entity, wrapper, animation, animation.getParticleEvents(), prevTime, currentTime, looped, partialTicks);
        }
    }

    private void dispatchEventList(EntityLivingBase entity, BedrockModelHandle wrapper, Animation animation,
                                   List<Animation.Event> events,
                                   float prevTime, float currentTime, boolean looped,
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
                if (t > prevTime + EVENT_EPS && t <= currentTime + EVENT_EPS) {
                    fireEvent(entity, wrapper, event, partialTicks);
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
                fireEvent(entity, wrapper, event, partialTicks);
            } else if (t >= -EVENT_EPS && t <= currentTime + EVENT_EPS) {
                fireEvent(entity, wrapper, event, partialTicks);
            }
        }
    }

    private void fireEvent(EntityLivingBase entity, BedrockModelHandle wrapper, Animation.Event event,
                           float partialTicks) {
        if (event.getType() == Animation.Event.Type.PARTICLE) {
            spawnParticleEffect(event.getEffect(), entity, wrapper, event.getLocator(), partialTicks);
        } else {
            float positionYaw = AnimationEventMathUtil.resolveHeadYaw(entity, partialTicks);
            double[] pos = AnimationEventMathUtil.resolveEventPosition(entity, wrapper, event.getLocator(), positionYaw, partialTicks);
            playSoundEffect(event.getEffect(), pos[0], pos[1], pos[2]);
        }
    }

    private void spawnParticleEffect(String effect,
                                     EntityLivingBase entity,
                                     BedrockModelHandle wrapper,
                                     String locatorName,
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
        float positionYaw = AnimationEventMathUtil.resolveHeadYaw(entity, partialTicks);
        float emitterYaw = AnimationEventMathUtil.resolveEmitterYaw(entity, partialTicks, params);
        double[] initialPos = AnimationEventMathUtil.resolveEventPosition(entity, wrapper, locatorName, positionYaw, partialTicks);
        if (entity == null) {
            double[] pos = initialPos != null ? initialPos : new double[]{0.0, 0.0, 0.0};
            system.spawn(params.path, pos[0], pos[1], pos[2], params.count);
            return;
        }
        double ix = initialPos != null && initialPos.length > 0 ? initialPos[0] : entity.posX;
        double iy = initialPos != null && initialPos.length > 1 ? initialPos[1] : entity.posY;
        double iz = initialPos != null && initialPos.length > 2 ? initialPos[2] : entity.posZ;
        system.spawn(params.path, new AnimationEventTransformProvider(entity, wrapper, locatorName, ix, iy, iz, emitterYaw,
            positionYaw, params.mode, params.yawOffset), params.count);
    }

    private void playSoundEffect(String effect, double x, double y, double z) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        AnimationEventArgsParser.SoundParams params = AnimationEventArgsParser.parseSound(effect);
        if (params == null || params.soundEvent == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        mc.world.playSound(x, y, z, params.soundEvent, SoundCategory.NEUTRAL, params.volume, params.pitch, false);
    }
}
