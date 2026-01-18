package org.mybad.minecraft.render.entity.events;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser.ParticleTargetMode;
import org.mybad.minecraft.particle.transform.EmitterTransform;
import org.mybad.minecraft.particle.transform.EmitterTransformProvider;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.transform.LocatorTransform;

public final class AnimationEventTransformProvider implements EmitterTransformProvider {
    private final EntityLivingBase entity;
    private final BedrockModelHandle wrapper;
    private final String locatorName;
    private final double initialX;
    private final double initialY;
    private final double initialZ;
    private final float initialEmitterYaw;
    private final float initialPositionYaw;
    private final ParticleTargetMode mode;
    private final float yawOffset;
    private final LocatorTransform locatorTransform;
    private final float modelScale;
    private boolean usedInitial;
    private boolean missingLocatorWarned;

    AnimationEventTransformProvider(EntityLivingBase entity,
                                    BedrockModelHandle wrapper,
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
        this.locatorTransform = new LocatorTransform();
        this.modelScale = wrapper != null ? wrapper.getModelScale() : 1.0f;
        this.usedInitial = false;
        this.missingLocatorWarned = false;
    }

    @Override
    public void fill(EmitterTransform transform, float deltaSeconds) {
        float positionYaw = AnimationEventMathUtil.resolveHeadYaw(entity);
        float emitterYaw = AnimationEventMathUtil.resolveEmitterYaw(entity, mode, yawOffset, initialEmitterYaw);
        if (wrapper != null && locatorName != null && wrapper.resolveLocatorTransform(locatorName, locatorTransform)) {
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
        } else if (wrapper != null && locatorName != null && !missingLocatorWarned) {
            missingLocatorWarned = true;
            SkyCoreMod.LOGGER.warn("[SkyCore] 粒子事件定位点 '{}' 无法解析（实体：{}）。将回退到世界坐标/默认姿态。",
                locatorName,
                entity != null ? entity.getName() : "unknown");
        }
        if (!usedInitial) {
            transform.x = initialX;
            transform.y = initialY;
            transform.z = initialZ;
            transform.yaw = 180.0F - initialEmitterYaw;
            setIdentityBasis(transform);
            transform.scale = modelScale;
            usedInitial = true;
            return;
        }
        if (entity == null) {
            setIdentityBasis(transform);
            transform.scale = modelScale;
            return;
        }
        double[] pos = AnimationEventMathUtil.resolveEventPositionNow(entity, wrapper, locatorName, positionYaw);
        transform.x = pos[0];
        transform.y = pos[1];
        transform.z = pos[2];
        transform.yaw = 180.0F - emitterYaw;
        setIdentityBasis(transform);
        transform.scale = modelScale;
    }

    @Override
    public boolean isLocatorBound() {
        return locatorName != null && !locatorName.isEmpty();
    }

    private void applyYawToBasis(LocatorTransform source, float cos, float sin,
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
