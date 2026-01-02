package org.mybad.minecraft.event;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import org.mybad.core.animation.Animation;
import org.mybad.core.animation.AnimationPlayer;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.animation.EntityAnimationController;
import org.mybad.minecraft.particle.BedrockParticleSystem;
import org.mybad.minecraft.particle.EmitterTransform;
import org.mybad.minecraft.particle.EmitterTransformProvider;
import org.mybad.minecraft.render.BedrockModelWrapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dispatches particle and sound events from animations.
 */
final class AnimationEventDispatcher {
    private static final float EVENT_EPS = 1.0e-4f;

    void dispatchAnimationEvents(EntityLivingBase entity, EntityRenderDispatcher.WrapperEntry entry,
                                 BedrockModelWrapper wrapper, float entityYaw, float partialTicks) {
        AnimationPlayer primaryPlayer = wrapper.getActiveAnimationPlayer();
        if (primaryPlayer != null && primaryPlayer.getAnimation() != null) {
            Animation animation = primaryPlayer.getAnimation();
            float currentTime = primaryPlayer.getState().getCurrentTime();
            int loopCount = primaryPlayer.getState().getLoopCount();
            if (!entry.primaryValid || entry.lastPrimaryAnimation != animation) {
                entry.lastPrimaryAnimation = animation;
                entry.lastPrimaryTime = currentTime;
                entry.lastPrimaryLoop = loopCount;
                entry.primaryValid = true;
            } else {
                boolean looped = loopCount != entry.lastPrimaryLoop ||
                    (animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < entry.lastPrimaryTime);
                dispatchEventsForAnimation(entity, wrapper, animation, entry.lastPrimaryTime, currentTime, looped, entityYaw, partialTicks);
                entry.lastPrimaryTime = currentTime;
                entry.lastPrimaryLoop = loopCount;
            }
        } else {
            entry.primaryValid = false;
        }

        if (entry.overlayStates == null || entry.overlayStates.isEmpty()) {
            entry.overlayCursors.clear();
            return;
        }

        Set<Animation> active = new HashSet<>();
        for (EntityAnimationController.OverlayState state : entry.overlayStates) {
            if (state == null || state.animation == null) {
                continue;
            }
            Animation animation = state.animation;
            active.add(animation);
            EntityRenderDispatcher.EventCursor cursor = entry.overlayCursors.get(animation);
            if (cursor == null) {
                cursor = new EntityRenderDispatcher.EventCursor();
                entry.overlayCursors.put(animation, cursor);
            }
            float currentTime = state.time;
            if (!cursor.valid) {
                cursor.lastTime = currentTime;
                cursor.lastLoop = 0;
                cursor.valid = true;
                continue;
            }
            boolean looped = animation.getLoopMode() == Animation.LoopMode.LOOP && currentTime + EVENT_EPS < cursor.lastTime;
            dispatchEventsForAnimation(entity, wrapper, animation, cursor.lastTime, currentTime, looped, entityYaw, partialTicks);
            cursor.lastTime = currentTime;
        }

        entry.overlayCursors.keySet().removeIf(anim -> !active.contains(anim));
    }

    private void dispatchEventsForAnimation(EntityLivingBase entity, BedrockModelWrapper wrapper, Animation animation,
                                            float prevTime, float currentTime, boolean looped,
                                            float entityYaw, float partialTicks) {
        if (animation == null) {
            return;
        }
        if (!looped && currentTime + EVENT_EPS < prevTime) {
            return;
        }

        if (!animation.getSoundEvents().isEmpty()) {
            dispatchEventList(entity, wrapper, animation, animation.getSoundEvents(), prevTime, currentTime, looped, entityYaw, partialTicks);
        }
        if (!animation.getParticleEvents().isEmpty()) {
            dispatchEventList(entity, wrapper, animation, animation.getParticleEvents(), prevTime, currentTime, looped, entityYaw, partialTicks);
        }
    }

    private void dispatchEventList(EntityLivingBase entity, BedrockModelWrapper wrapper, Animation animation,
                                   List<Animation.Event> events,
                                   float prevTime, float currentTime, boolean looped,
                                   float entityYaw, float partialTicks) {
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
                    fireEvent(entity, wrapper, event, entityYaw, partialTicks);
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
                fireEvent(entity, wrapper, event, entityYaw, partialTicks);
            } else if (t >= -EVENT_EPS && t <= currentTime + EVENT_EPS) {
                fireEvent(entity, wrapper, event, entityYaw, partialTicks);
            }
        }
    }

    private void fireEvent(EntityLivingBase entity, BedrockModelWrapper wrapper, Animation.Event event,
                           float entityYaw, float partialTicks) {
        if (event.getType() == Animation.Event.Type.PARTICLE) {
            spawnParticleEffect(event.getEffect(), entity, wrapper, event.getLocator(), partialTicks);
        } else {
            float positionYaw = resolveHeadYaw(entity, partialTicks);
            double[] pos = resolveEventPosition(entity, wrapper, event.getLocator(), positionYaw, partialTicks);
            playSoundEffect(event.getEffect(), pos[0], pos[1], pos[2]);
        }
    }

    private void spawnParticleEffect(String effect,
                                     EntityLivingBase entity,
                                     BedrockModelWrapper wrapper,
                                     String locatorName,
                                     float partialTicks) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        ParticleParams params = parseParticleParams(effect);
        if (params == null || params.path == null || params.path.isEmpty()) {
            return;
        }
        BedrockParticleSystem system = SkyCoreMod.getParticleSystem();
        if (system == null) {
            return;
        }
        float positionYaw = resolveHeadYaw(entity, partialTicks);
        float emitterYaw = resolveEmitterYaw(entity, partialTicks, params);
        double[] initialPos = resolveEventPosition(entity, wrapper, locatorName, positionYaw, partialTicks);
        if (entity == null) {
            double[] pos = initialPos != null ? initialPos : new double[]{0.0, 0.0, 0.0};
            system.spawn(params.path, pos[0], pos[1], pos[2], params.count);
            return;
        }
        double ix = initialPos != null && initialPos.length > 0 ? initialPos[0] : entity.posX;
        double iy = initialPos != null && initialPos.length > 1 ? initialPos[1] : entity.posY;
        double iz = initialPos != null && initialPos.length > 2 ? initialPos[2] : entity.posZ;
        system.spawn(params.path, new EventTransformProvider(entity, wrapper, locatorName, ix, iy, iz, emitterYaw,
            positionYaw, params.mode, params.yawOffset), params.count);
    }

    private ParticleParams parseParticleParams(String effect) {
        String path = null;
        int count = 0;
        ParticleTargetMode mode = ParticleTargetMode.LOOK;
        float yawOffset = 0.0f;
        String[] parts = effect.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                if (path == null) {
                    path = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("effect".equals(key) || "particle".equals(key) || "path".equals(key)) {
                path = value;
            } else if ("count".equals(key) || "num".equals(key) || "amount".equals(key)) {
                try {
                    count = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            } else if ("mode".equals(key)) {
                mode = ParticleTargetMode.parse(value);
            } else if ("yaw".equals(key)) {
                try {
                    yawOffset = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (path == null) {
            path = effect.trim();
        }
        ParticleParams params = new ParticleParams();
        params.path = path;
        params.count = count;
        params.mode = mode;
        params.yawOffset = yawOffset;
        return params;
    }

    private double[] resolveEventPosition(EntityLivingBase entity, BedrockModelWrapper wrapper, String locatorName,
                                          float positionYaw, float partialTicks) {
        if (entity == null) {
            return resolveEventPosition(null, wrapper, locatorName, positionYaw, 0.0, 0.0, 0.0);
        }
        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        return resolveEventPosition(entity, wrapper, locatorName, positionYaw, baseX, baseY, baseZ);
    }

    private double[] resolveEventPositionNow(EntityLivingBase entity, BedrockModelWrapper wrapper, String locatorName,
                                             float positionYaw) {
        if (entity == null) {
            return resolveEventPosition(null, wrapper, locatorName, positionYaw, 0.0, 0.0, 0.0);
        }
        return resolveEventPosition(entity, wrapper, locatorName, positionYaw, entity.posX, entity.posY, entity.posZ);
    }

    private double[] resolveEventPosition(EntityLivingBase entity, BedrockModelWrapper wrapper, String locatorName,
                                          float positionYaw, double baseX, double baseY, double baseZ) {
        if (locatorName == null || locatorName.isEmpty()) {
            return new double[]{baseX, baseY, baseZ};
        }
        if (wrapper == null) {
            return new double[]{baseX, baseY, baseZ};
        }
        float[] local = wrapper.getLocatorPosition(locatorName);
        if (local == null) {
            return new double[]{baseX, baseY, baseZ};
        }
        float scale = wrapper.getModelScale();
        float lx = local[0] * scale;
        float ly = local[1] * scale;
        float lz = local[2] * scale;

        float yawRad = (float) Math.toRadians(180.0F - positionYaw);
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(yawRad);
        float rx = lx * cos + lz * sin;
        float rz = -lx * sin + lz * cos;
        return new double[]{baseX + rx, baseY + ly, baseZ + rz};
    }

    private final class EventTransformProvider implements EmitterTransformProvider {
        private final EntityLivingBase entity;
        private final BedrockModelWrapper wrapper;
        private final String locatorName;
        private final double initialX;
        private final double initialY;
        private final double initialZ;
        private final float initialEmitterYaw;
        private final float initialPositionYaw;
        private final ParticleTargetMode mode;
        private final float yawOffset;
        private final BedrockModelWrapper.LocatorTransform locatorTransform;
        private boolean usedInitial;

        private EventTransformProvider(EntityLivingBase entity,
                                       BedrockModelWrapper wrapper,
                                       String locatorName,
                                       double initialX,
                                       double initialY,
                                       double initialZ,
                                       float initialEmitterYaw,
                                       float initialPositionYaw,
                                       ParticleTargetMode mode,
                                       float yawOffset) {
            this.entity = entity;
            this.wrapper = wrapper;
            this.locatorName = locatorName;
            this.initialX = initialX;
            this.initialY = initialY;
            this.initialZ = initialZ;
            this.initialEmitterYaw = initialEmitterYaw;
            this.initialPositionYaw = initialPositionYaw;
            this.mode = mode != null ? mode : ParticleTargetMode.LOOK;
            this.yawOffset = yawOffset;
            this.locatorTransform = new BedrockModelWrapper.LocatorTransform();
            this.usedInitial = false;
        }

        @Override
        public void fill(EmitterTransform transform, float deltaSeconds) {
            float positionYaw = resolveHeadYaw(entity);
            float emitterYaw = resolveEmitterYaw(entity, mode, yawOffset, initialEmitterYaw);
            if (wrapper != null && locatorName != null && wrapper.getLocatorTransform(locatorName, locatorTransform)) {
                float scale = wrapper.getModelScale();
                float lx = locatorTransform.position[0] * scale;
                float ly = locatorTransform.position[1] * scale;
                float lz = locatorTransform.position[2] * scale;
                float yawRad = (float) Math.toRadians(180.0F - positionYaw);
                float cos = MathHelper.cos(yawRad);
                float sin = MathHelper.sin(yawRad);
                float rx = lx * cos + lz * sin;
                float rz = -lx * sin + lz * cos;
                double baseX;
                double baseY;
                double baseZ;
                if (usedInitial) {
                    baseX = entity != null ? entity.posX : initialX;
                    baseY = entity != null ? entity.posY : initialY;
                    baseZ = entity != null ? entity.posZ : initialZ;
                } else {
                    float initYawRad = (float) Math.toRadians(180.0F - initialPositionYaw);
                    float initCos = MathHelper.cos(initYawRad);
                    float initSin = MathHelper.sin(initYawRad);
                    float initRx = lx * initCos + lz * initSin;
                    float initRz = -lx * initSin + lz * initCos;
                    baseX = initialX - initRx;
                    baseY = initialY - ly;
                    baseZ = initialZ - initRz;
                }
                transform.x = baseX + rx;
                transform.y = baseY + ly;
                transform.z = baseZ + rz;
                transform.yaw = 180.0F - emitterYaw;
                applyYawToBasis(locatorTransform, cos, sin, transform);
                flipLocatorBasisY(transform);
                float sx = locatorTransform.scale[0] * scale;
                float sy = locatorTransform.scale[1] * scale;
                float sz = locatorTransform.scale[2] * scale;
                float uniformScale = (Math.abs(sx) + Math.abs(sy) + Math.abs(sz)) / 3.0f;
                transform.scale = uniformScale <= 0.0f ? 1.0f : uniformScale;
                usedInitial = true;
                return;
            }
            if (!usedInitial) {
                transform.x = initialX;
                transform.y = initialY;
                transform.z = initialZ;
                transform.yaw = 180.0F - initialEmitterYaw;
                setIdentityBasis(transform);
                transform.scale = 1.0f;
                usedInitial = true;
                return;
            }
            if (entity == null) {
                setIdentityBasis(transform);
                transform.scale = 1.0f;
                return;
            }
            double[] pos = resolveEventPositionNow(entity, wrapper, locatorName, positionYaw);
            transform.x = pos[0];
            transform.y = pos[1];
            transform.z = pos[2];
            transform.yaw = 180.0F - emitterYaw;
            setIdentityBasis(transform);
            transform.scale = 1.0f;
        }

        @Override
        public boolean isLocatorBound() {
            return locatorName != null && !locatorName.isEmpty();
        }

        private void applyYawToBasis(BedrockModelWrapper.LocatorTransform source, float cos, float sin,
                                     EmitterTransform transform) {
            rotateBasis(source.basisX, cos, sin, transform.basisX);
            rotateBasis(source.basisY, cos, sin, transform.basisY);
            rotateBasis(source.basisZ, cos, sin, transform.basisZ);
        }

        private void flipLocatorBasisY(EmitterTransform transform) {
            transform.basisY[0] = -transform.basisY[0];
            transform.basisY[1] = -transform.basisY[1];
            transform.basisY[2] = -transform.basisY[2];
        }

        private void rotateBasis(float[] axis, float cos, float sin, float[] out) {
            float x = axis[0];
            float y = axis[1];
            float z = axis[2];
            out[0] = x * cos + z * sin;
            out[1] = y;
            out[2] = -x * sin + z * cos;
        }

        private void setIdentityBasis(EmitterTransform transform) {
            transform.basisX[0] = 1.0f;
            transform.basisX[1] = 0.0f;
            transform.basisX[2] = 0.0f;
            transform.basisY[0] = 0.0f;
            transform.basisY[1] = 1.0f;
            transform.basisY[2] = 0.0f;
            transform.basisZ[0] = 0.0f;
            transform.basisZ[1] = 0.0f;
            transform.basisZ[2] = 1.0f;
        }
    }

    private enum ParticleTargetMode {
        LOOK,
        BODY,
        WORLD;

        private static ParticleTargetMode parse(String raw) {
            if (raw == null || raw.isEmpty()) {
                return LOOK;
            }
            String value = raw.trim().toLowerCase();
            if ("body".equals(value)) {
                return BODY;
            }
            if ("world".equals(value)) {
                return WORLD;
            }
            return LOOK;
        }
    }

    private float resolveBodyYaw(EntityLivingBase entity, float partialTicks) {
        if (entity == null) {
            return 0.0f;
        }
        return interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
    }

    private float resolveBodyYaw(EntityLivingBase entity) {
        if (entity == null) {
            return 0.0f;
        }
        return entity.renderYawOffset;
    }

    private float resolveHeadYaw(EntityLivingBase entity) {
        if (entity == null) {
            return 0.0f;
        }
        return entity.rotationYawHead;
    }

    private float resolveHeadYaw(EntityLivingBase entity, float partialTicks) {
        if (entity == null) {
            return 0.0f;
        }
        return interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
    }

    private float resolveEmitterYaw(EntityLivingBase entity, float partialTicks, ParticleParams params) {
        if (params == null) {
            return resolveHeadYaw(entity, partialTicks);
        }
        switch (params.mode) {
            case BODY:
                return resolveBodyYaw(entity, partialTicks) + params.yawOffset;
            case WORLD:
                return params.yawOffset;
            case LOOK:
            default:
                return resolveHeadYaw(entity, partialTicks) + params.yawOffset;
        }
    }

    private float resolveEmitterYaw(EntityLivingBase entity, ParticleTargetMode mode, float yawOffset, float fallbackYaw) {
        if (entity == null) {
            return fallbackYaw;
        }
        if (mode == ParticleTargetMode.BODY) {
            return entity.renderYawOffset + yawOffset;
        }
        if (mode == ParticleTargetMode.WORLD) {
            return yawOffset;
        }
        return entity.rotationYawHead + yawOffset;
    }

    private void playSoundEffect(String effect, double x, double y, double z) {
        if (effect == null || effect.isEmpty()) {
            return;
        }
        SoundParams params = parseSoundParams(effect);
        if (params == null || params.soundEvent == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return;
        }
        mc.world.playSound(x, y, z, params.soundEvent, SoundCategory.NEUTRAL, params.volume, params.pitch, false);
    }

    private SoundParams parseSoundParams(String effect) {
        String soundId = null;
        float pitch = 1.0f;
        float volume = 1.0f;
        String[] parts = effect.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq < 0) {
                if (soundId == null) {
                    soundId = trimmed;
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (value.isEmpty()) {
                continue;
            }
            if ("sound".equals(key)) {
                soundId = value;
            } else if ("pitch".equals(key)) {
                try {
                    pitch = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
            } else if ("volume".equals(key)) {
                try {
                    volume = Float.parseFloat(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (soundId == null || soundId.isEmpty()) {
            soundId = effect;
        }
        soundId = soundId.trim();
        if (soundId.endsWith(".ogg")) {
            soundId = soundId.substring(0, soundId.length() - 4);
        }
        ResourceLocation soundLocation = parseSoundLocation(soundId);
        SoundEvent soundEvent = SoundEvent.REGISTRY.getObject(soundLocation);
        if (soundEvent == null) {
            soundEvent = new SoundEvent(soundLocation);
        }
        if (volume > 1.0f) {
            volume = volume / 100.0f;
        }
        SoundParams params = new SoundParams();
        params.soundEvent = soundEvent;
        params.volume = Math.max(0f, volume);
        params.pitch = Math.max(0f, pitch);
        return params;
    }

    private ResourceLocation parseSoundLocation(String soundId) {
        if (soundId.contains(":")) {
            return new ResourceLocation(soundId);
        }
        return new ResourceLocation(SkyCoreMod.MOD_ID, soundId);
    }

    private float interpolateRotation(float prev, float current, float partialTicks) {
        float diff = current - prev;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return prev + partialTicks * diff;
    }

    private static final class ParticleParams {
        private String path;
        private int count;
        private ParticleTargetMode mode;
        private float yawOffset;
    }

    private static final class SoundParams {
        private SoundEvent soundEvent;
        private float volume;
        private float pitch;
    }
}
