package org.mybad.minecraft.particle.runtime;

import org.mybad.bedrockparticle.molang.api.MolangEnvironment;
import org.mybad.bedrockparticle.particle.ParticleInstance;
import org.mybad.bedrockparticle.particle.component.ParticleEmitterShape;

import java.util.Random;

final class EmitterShapeSpawner implements ParticleEmitterShape.Spawner {
    private final ActiveEmitter emitter;

    EmitterShapeSpawner(ActiveEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public ParticleInstance createParticle() {
        return emitter.createParticle(emitter.getX(), emitter.getY(), emitter.getZ());
    }

    @Override
    public void spawnParticle(ParticleInstance instance) {
        if (!(instance instanceof ActiveParticle)) {
            return;
        }
        ActiveParticle particle = (ActiveParticle) instance;
        particle.applyInitialSpeed();
        particle.syncPrev();
        emitter.getSystem().addParticle(particle);
        emitter.onParticleSpawned();
    }

    @Override
    public org.mybad.bedrockparticle.particle.ParticleSourceObject getEntity() {
        return null;
    }

    @Override
    public MolangEnvironment getEnvironment() {
        return emitter.getEnvironment();
    }

    @Override
    public Random getRandom() {
        return emitter.getEventRandom();
    }

    @Override
    public void setPosition(ParticleInstance instance, double x, double y, double z) {
        if (!(instance instanceof ActiveParticle)) {
            return;
        }
        ActiveParticle particle = (ActiveParticle) instance;
        boolean localPos = emitter.isLocalPosition();
        boolean localRot = emitter.isLocalRotation();
        float scale = emitter.getScale();
        x *= scale;
        y *= scale;
        z *= scale;
        double rx = (localPos || localRot) ? emitter.rotateLocalX(x, y, z) : x;
        double ry = (localPos || localRot) ? emitter.rotateLocalY(x, y, z) : y;
        double rz = (localPos || localRot) ? emitter.rotateLocalZ(x, y, z) : z;
        particle.setPosition(emitter.getX() + rx, emitter.getY() + ry, emitter.getZ() + rz);
    }

    @Override
    public void setVelocity(ParticleInstance instance, double dx, double dy, double dz) {
        if (!(instance instanceof ActiveParticle)) {
            return;
        }
        boolean localVel = emitter.isLocalVelocity() || emitter.isLocalRotation();
        float scale = emitter.getScale();
        dx *= scale;
        dy *= scale;
        dz *= scale;
        double rx = localVel ? emitter.rotateLocalX(dx, dy, dz) : dx;
        double ry = localVel ? emitter.rotateLocalY(dx, dy, dz) : dy;
        double rz = localVel ? emitter.rotateLocalZ(dx, dy, dz) : dz;
        double vx = rx + emitter.getDeltaX();
        double vy = ry + emitter.getDeltaY();
        double vz = rz + emitter.getDeltaZ();
        ActiveParticle particle = (ActiveParticle) instance;
        particle.setVelocity(vx, vy, vz);
    }
}
