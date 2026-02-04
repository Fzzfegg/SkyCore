package org.mybad.minecraft.render.entity.events;

import org.mybad.minecraft.particle.transform.EmitterTransform;
import org.mybad.minecraft.particle.transform.EmitterTransformProvider;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser.ParticleParams;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser.ParticleTargetMode;
import org.mybad.minecraft.render.transform.LocatorTransform;

/**
 * Emits particles for block-anchored models (e.g., skulls) respecting locator transforms and scaling.
 */
final class BlockAnimationEventTransformProvider implements EmitterTransformProvider {
    private final BedrockModelHandle wrapper;
    private final AnimationEventTarget target;
    private final String locatorName;
    private final double baseX;
    private final double baseY;
    private final double baseZ;
    private final float positionYaw;
    private final float emitterYaw;
    private final LocatorTransform locatorTransform = new LocatorTransform();
    private final float modelScale;

    BlockAnimationEventTransformProvider(AnimationEventTarget target,
                                         BedrockModelHandle wrapper,
                                         String locatorName,
                                         float positionYaw,
                                         ParticleParams params) {
        this.wrapper = wrapper;
        this.target = target;
        this.locatorName = locatorName;
        this.baseX = target != null ? target.getBaseX() : 0.0;
        this.baseY = target != null ? target.getBaseY() : 0.0;
        this.baseZ = target != null ? target.getBaseZ() : 0.0;
        this.positionYaw = positionYaw;
        this.modelScale = 1.0f;
        ParticleTargetMode mode = params != null ? params.mode : ParticleTargetMode.LOOK;
        float yawOffset = params != null ? params.yawOffset : 0.0f;
        this.emitterYaw = AnimationEventMathUtil.resolveEmitterYaw(target, mode, yawOffset, positionYaw);
    }

    @Override
    public void fill(EmitterTransform transform, float deltaSeconds) {
        if (wrapper != null && locatorName != null && wrapper.resolveLocatorTransform(locatorName, locatorTransform)) {
            applyLocatorTransform(transform);
            return;
        }
        setIdentityBasis(transform);
        transform.x = baseX;
        transform.y = baseY;
        transform.z = baseZ;
        transform.yaw = 180.0F - emitterYaw;
        transform.scale = 1.0f;
    }

    @Override
    public boolean isLocatorBound() {
        return locatorName != null && !locatorName.isEmpty();
    }

    @Override
    public boolean shouldExpireEmitter() {
        return target != null && target.isEventTargetExpired();
    }

    private void applyLocatorTransform(EmitterTransform transform) {
        float scale = wrapper != null ? wrapper.getModelScale() : 1.0f;
        float lx = locatorTransform.position[0] * scale;
        float ly = locatorTransform.position[1] * scale;
        float lz = locatorTransform.position[2] * scale;
        float yawRad = (float) Math.toRadians(180.0F - positionYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);
        float rx = lx * cos + lz * sin;
        float rz = -lx * sin + lz * cos;
        transform.x = baseX + rx;
        transform.y = baseY + ly;
        transform.z = baseZ + rz;
        transform.yaw = 180.0F - emitterYaw;
        applyYawToBasis(locatorTransform, cos, sin, transform);
        flipLocatorBasisY(transform);
        transform.scale = 1.0f;
    }

    private void applyYawToBasis(LocatorTransform source, float cos, float sin, EmitterTransform transform) {
        rotateBasis(source.basisX, cos, sin, transform.basisX);
        rotateBasis(source.basisY, cos, sin, transform.basisY);
        rotateBasis(source.basisZ, cos, sin, transform.basisZ);
    }

    private void rotateBasis(float[] axis, float cos, float sin, float[] out) {
        float x = axis[0];
        float y = axis[1];
        float z = axis[2];
        out[0] = x * cos + z * sin;
        out[1] = y;
        out[2] = -x * sin + z * cos;
    }

    private void flipLocatorBasisY(EmitterTransform transform) {
        transform.basisY[0] = -transform.basisY[0];
        transform.basisY[1] = -transform.basisY[1];
        transform.basisY[2] = -transform.basisY[2];
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
