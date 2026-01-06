package org.mybad.minecraft.render.entity.events;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser.ParticleParams;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser.ParticleTargetMode;
import org.mybad.minecraft.render.BedrockModelHandle;

public final class AnimationEventMathUtil {
    private AnimationEventMathUtil() {
    }

    public static double[] resolveEventPosition(EntityLivingBase entity, BedrockModelHandle wrapper, String locatorName,
                                                float positionYaw, float partialTicks) {
        if (entity == null) {
            return resolveEventPosition(null, wrapper, locatorName, positionYaw, 0.0, 0.0, 0.0);
        }
        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        return resolveEventPosition(entity, wrapper, locatorName, positionYaw, baseX, baseY, baseZ);
    }

    public static double[] resolveEventPositionNow(EntityLivingBase entity, BedrockModelHandle wrapper, String locatorName,
                                                   float positionYaw) {
        if (entity == null) {
            return resolveEventPosition(null, wrapper, locatorName, positionYaw, 0.0, 0.0, 0.0);
        }
        return resolveEventPosition(entity, wrapper, locatorName, positionYaw, entity.posX, entity.posY, entity.posZ);
    }

    public static double[] resolveEventPosition(EntityLivingBase entity, BedrockModelHandle wrapper, String locatorName,
                                                float positionYaw, double baseX, double baseY, double baseZ) {
        if (locatorName == null || locatorName.isEmpty()) {
            return new double[]{baseX, baseY, baseZ};
        }
        if (wrapper == null) {
            return new double[]{baseX, baseY, baseZ};
        }
        float[] local = wrapper.resolveLocatorPosition(locatorName);
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

    public static float resolveBodyYaw(EntityLivingBase entity, float partialTicks) {
        if (entity == null) {
            return 0.0f;
        }
        return interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
    }

    public static float resolveBodyYaw(EntityLivingBase entity) {
        if (entity == null) {
            return 0.0f;
        }
        return entity.renderYawOffset;
    }

    public static float resolveHeadYaw(EntityLivingBase entity) {
        if (entity == null) {
            return 0.0f;
        }
        return entity.rotationYawHead;
    }

    public static float resolveHeadYaw(EntityLivingBase entity, float partialTicks) {
        if (entity == null) {
            return 0.0f;
        }
        return interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
    }

    public static float resolveEmitterYaw(EntityLivingBase entity, float partialTicks, ParticleParams params) {
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

    public static float resolveEmitterYaw(EntityLivingBase entity, ParticleTargetMode mode, float yawOffset, float fallbackYaw) {
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

    public static float interpolateRotation(float prev, float current, float partialTicks) {
        float diff = current - prev;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return prev + partialTicks * diff;
    }
}
