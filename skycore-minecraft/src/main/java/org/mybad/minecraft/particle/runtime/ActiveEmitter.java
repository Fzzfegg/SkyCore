package org.mybad.minecraft.particle.runtime;

import org.mybad.bedrockparticle.molang.api.MolangEnvironment;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.particle.ParticleData;
import org.mybad.bedrockparticle.particle.ParticleContext;
import org.mybad.bedrockparticle.particle.component.EmitterInitializationComponent;
import org.mybad.bedrockparticle.particle.component.EmitterLifetimeExpressionComponent;
import org.mybad.bedrockparticle.particle.component.EmitterLifetimeLoopingComponent;
import org.mybad.bedrockparticle.particle.component.EmitterLifetimeOnceComponent;
import org.mybad.bedrockparticle.particle.component.EmitterLocalSpaceComponent;
import org.mybad.bedrockparticle.particle.component.EmitterRateInstantComponent;
import org.mybad.bedrockparticle.particle.component.EmitterRateSteadyComponent;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceBillboardComponent;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceTintingComponent;
import org.mybad.bedrockparticle.particle.component.ParticleEmitterShape;
import org.mybad.bedrockparticle.particle.component.ParticleInitialSpeedComponent;
import org.mybad.bedrockparticle.particle.component.ParticleLifetimeExpressionComponent;
import org.mybad.bedrockparticle.particle.component.ParticleLifetimeEventComponent;
import org.mybad.bedrockparticle.particle.event.ParticleEvent;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.particle.molang.ParticleCurveEvaluator;
import org.mybad.minecraft.particle.molang.ParticleMolangBindings;
import org.mybad.minecraft.particle.molang.ParticleMolangContext;
import org.mybad.minecraft.particle.transform.EmitterTransform;
import org.mybad.minecraft.particle.transform.EmitterTransformProvider;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class ActiveEmitter implements ParticleContext {
    private final BedrockParticleSystem system;
    private final ParticleData data;
    private final Map<String, ParticleData.Curve> curves;
    private final Map<String, Float> curveValues;
    private final ParticleMolangContext molangContext;
    private final MolangEnvironment environment;
    private final ParticleAppearanceBillboardComponent billboard;
    private final ParticleAppearanceTintingComponent tint;
    private final ParticleInitialSpeedComponent speed;
    private final ParticleEmitterShape emitterShape;
    private final ParticleLifetimeExpressionComponent particleLifetimeComponent;
    private final EmitterLocalSpaceComponent localSpace;
    private final EmitterRateInstantComponent rateInstant;
    private final EmitterRateSteadyComponent rateSteady;
    private final EmitterLifetimeOnceComponent lifetimeOnce;
    private final EmitterLifetimeLoopingComponent lifetimeLooping;
    private final EmitterLifetimeExpressionComponent lifetimeExpression;
    private final ParticleLifetimeEventComponent lifetimeEvents;
    private final EmitterInitializationComponent emitterInitialization;
    private final ResourceLocation texture;
    private final ResourceLocation emissiveTexture;
    private final float emissiveStrength;
    private final ResourceLocation blendTexture;
    private final BedrockParticleSystem.BlendMode blendMode;
    private final float blendR;
    private final float blendG;
    private final float blendB;
    private final float blendA;
    private final int overrideCount;
    private final EmitterTransformProvider transformProvider;
    private final EmitterTransform currentTransform;
    private final EmitterShapeSpawner spawner;
    private final float[] basisX;
    private final float[] basisY;
    private final float[] basisZ;
    private final float[] lastBasisX;
    private final float[] lastBasisY;
    private final float[] lastBasisZ;
    private float scale;
    private final Random eventRandom;
    private final boolean locatorBound;
    private final ArrayDeque<ActiveParticle> particlePool;
    private final int particlePoolLimit;
    private int lifetimeEventIndex;
    private boolean expirationEventsFired;

        private double x;
        private double y;
        private double z;
        private double lastX;
        private double lastY;
        private double lastZ;
        private float yaw;
        private float lastYaw;
        private float deltaYawRad;
        private double deltaX;
        private double deltaY;
        private double deltaZ;
        private float age;
        private float lifetime;
        private float activeTimeEval;
        private float sleepTimeEval;
        private float sleepRemaining;
        private int maxParticlesEval;
        private float steadyRemainder;
        private boolean instantEmitted;
        private boolean expired;
        private int activeParticles;
        private boolean spawnedAny;

        ActiveEmitter(BedrockParticleSystem system, ParticleData data, EmitterTransformProvider provider, int overrideCount) {
            this.system = system;
            this.data = data;
            this.transformProvider = provider != null ? provider
                : new BedrockParticleSystem.StaticTransformProvider(0.0, 0.0, 0.0, 0.0f);
            this.locatorBound = provider != null && provider.isLocatorBound();
            this.currentTransform = new EmitterTransform();
            this.transformProvider.fill(currentTransform, 0.0f);
            this.x = currentTransform.x;
            this.y = currentTransform.y;
            this.z = currentTransform.z;
            this.yaw = currentTransform.yaw;
            this.basisX = new float[]{currentTransform.basisX[0], currentTransform.basisX[1], currentTransform.basisX[2]};
            this.basisY = new float[]{currentTransform.basisY[0], currentTransform.basisY[1], currentTransform.basisY[2]};
            this.basisZ = new float[]{currentTransform.basisZ[0], currentTransform.basisZ[1], currentTransform.basisZ[2]};
            this.lastBasisX = new float[]{basisX[0], basisX[1], basisX[2]};
            this.lastBasisY = new float[]{basisY[0], basisY[1], basisY[2]};
            this.lastBasisZ = new float[]{basisZ[0], basisZ[1], basisZ[2]};
            this.lastX = this.x;
            this.lastY = this.y;
            this.lastZ = this.z;
            this.lastYaw = this.yaw;
            this.scale = currentTransform.scale;
            this.deltaX = 0.0;
            this.deltaY = 0.0;
            this.deltaZ = 0.0;
            this.deltaYawRad = 0.0f;
            this.overrideCount = overrideCount;
            this.curves = ParticleMolangBindings.buildCurveDefinitions(data);
            this.curveValues = this.curves.isEmpty() ? Collections.emptyMap() : new HashMap<>(this.curves.size());
            this.molangContext = new ParticleMolangContext();
            this.molangContext.curves = this.curveValues;
            for (int i = 1; i <= 16; i++) {
                this.molangContext.setRandom(i, (float) Math.random());
            }
            this.molangContext.random = this.molangContext.getRandom(1);
            for (int i = 1; i <= 16; i++) {
                this.molangContext.setEmitterRandom(i, this.molangContext.getRandom(i));
            }
            this.molangContext.entityScale = this.scale;
            this.environment = ParticleMolangBindings.createRuntime(this.molangContext, this.curves);

            this.billboard = BedrockParticleSystem.getComponent(data, "particle_appearance_billboard");
            this.tint = BedrockParticleSystem.getComponent(data, "particle_appearance_tinting");
            this.speed = BedrockParticleSystem.getComponent(data, "particle_initial_speed");
            this.emitterShape = resolveEmitterShape();
            this.particleLifetimeComponent = BedrockParticleSystem.getComponent(data, "particle_lifetime_expression");
            this.localSpace = BedrockParticleSystem.getComponent(data, "emitter_local_space");
            this.rateInstant = BedrockParticleSystem.getComponent(data, "emitter_rate_instant");
            this.rateSteady = BedrockParticleSystem.getComponent(data, "emitter_rate_steady");
            this.lifetimeOnce = BedrockParticleSystem.getComponent(data, "emitter_lifetime_once");
            this.lifetimeLooping = BedrockParticleSystem.getComponent(data, "emitter_lifetime_looping");
            this.lifetimeExpression = BedrockParticleSystem.getComponent(data, "emitter_lifetime_expression");
            this.lifetimeEvents = BedrockParticleSystem.getComponent(data, "emitter_lifetime_events");
            this.emitterInitialization = BedrockParticleSystem.getComponent(data, "emitter_initialization");
            this.texture = system.toMinecraft(data);
            this.emissiveTexture = resolveEmissiveTexture(system, data);
            this.emissiveStrength = resolveEmissiveStrength(data);
            this.blendTexture = resolveBlendTexture(system, data);
            this.blendMode = resolveBlendMode(data);
            float[] blendColor = resolveBlendColor(data);
            if (blendColor != null && blendColor.length >= 4) {
                this.blendR = clamp01(blendColor[0]);
                this.blendG = clamp01(blendColor[1]);
                this.blendB = clamp01(blendColor[2]);
                this.blendA = clamp01(blendColor[3]);
            } else {
                this.blendR = 1.0f;
                this.blendG = 1.0f;
                this.blendB = 1.0f;
                this.blendA = 0.0f;
            }
            this.age = 0.0f;
            this.lifetime = Float.MAX_VALUE;
            this.instantEmitted = false;
            this.expired = false;
            this.activeParticles = 0;
            this.spawnedAny = false;
            this.spawner = new EmitterShapeSpawner(this);
            this.eventRandom = new Random();
            this.particlePool = new ArrayDeque<>();
            int limit = BedrockParticleSystem.DEFAULT_PARTICLE_POOL_LIMIT;
            if (overrideCount > 0) {
                limit = Math.min(limit, overrideCount);
            }
            this.particlePoolLimit = Math.max(0, limit);
            this.lifetimeEventIndex = 0;
            this.expirationEventsFired = false;
            this.steadyRemainder = 0.0f;

            evaluateLifetimeOnCreate();
            updateContext(0.0f);
            if (emitterInitialization != null && emitterInitialization.creationExpression() != null) {
                environment.safeResolve(emitterInitialization.creationExpression());
            }
            if (lifetimeEvents != null) {
                fireEvents(lifetimeEvents.creationEvent());
            }
        }

        void emitInitialParticles(int room) {
            if (expired) {
                return;
            }
            if (lifetimeExpression != null) {
                return;
            }
            if (!isActive()) {
                return;
            }
            if (rateInstant != null) {
                int count = resolveInstantCount();
                emitParticles(count, room);
                instantEmitted = true;
            } else if (rateSteady == null) {
                int count = overrideCount > 0 ? overrideCount : 1;
                emitParticles(count, room);
                instantEmitted = true;
                expire();
            }
        }

        boolean tick() {
            updateTransform(BedrockParticleSystem.TICK_SECONDS);
            updateContext(age);
            if (emitterInitialization != null && emitterInitialization.tickExpression() != null) {
                environment.safeResolve(emitterInitialization.tickExpression());
            }
            if (!expired) {
                tickLifetimeEvents();
                applyLifetimeLogic();
                updateContext(age);
                if (!expired && isActive()) {
                    if (rateInstant != null) {
                        if (!instantEmitted) {
                            emitParticles(resolveInstantCount(), system.getRemainingParticleRoom());
                            instantEmitted = true;
                        }
                    } else if (rateSteady != null) {
                        emitSteady();
                    }
                }
            }
            age += BedrockParticleSystem.TICK_SECONDS;
            updateContext(age);
            return !expired || activeParticles > 0;
        }

        void render(float partialTicks) {
            if (emitterInitialization == null || emitterInitialization.renderExpression() == null) {
                return;
            }
            float renderAge = this.age + partialTicks * BedrockParticleSystem.TICK_SECONDS;
            updateContext(renderAge);
            environment.safeResolve(emitterInitialization.renderExpression());
        }

        private void emitSteady() {
            int maxCount = getMaxParticles() - activeParticles;
            if (maxCount <= 0) {
                return;
            }
            float spawnRate = environment.safeResolve(rateSteady.spawnRate());
            if (spawnRate <= 0.0f) {
                return;
            }
            int room = system.getRemainingParticleRoom();
            if (room <= 0) {
                return;
            }
            steadyRemainder += spawnRate / 20.0f;
            int count = (int) steadyRemainder;
            if (count <= 0) {
                return;
            }
            int actual = Math.min(Math.min(count, maxCount), room);
            if (actual <= 0) {
                steadyRemainder -= count;
                return;
            }
            emitParticles(actual, room);
            steadyRemainder -= count;
        }

        private int resolveInstantCount() {
            if (overrideCount > 0) {
                return overrideCount;
            }
            float count = environment.safeResolve(rateInstant.particleCount());
            return Math.max(0, (int) count);
        }

        private int getMaxParticles() {
            if (rateSteady == null) {
                return Integer.MAX_VALUE;
            }
            if (maxParticlesEval <= 0) {
                float max = environment.safeResolve(rateSteady.maxParticles());
                maxParticlesEval = Math.max(1, (int) max);
            }
            if (overrideCount > 0) {
                return Math.min(maxParticlesEval, overrideCount);
            }
            return maxParticlesEval;
        }

        private void emitParticles(int count, int room) {
            if (count <= 0) {
                return;
            }
            if (room <= 0) {
                return;
            }
            int actual = Math.min(count, room);
            if (emitterShape != null && emitterShape != ParticleEmitterShape.EMPTY) {
                emitterShape.emitParticles(spawner, actual);
            } else {
                for (int i = 0; i < actual; i++) {
                    ActiveParticle particle = createParticle(x, y, z);
                    particle.applyInitialSpeed();
                    system.addParticle(particle);
                    onParticleSpawned();
                }
            }
        }

        private void applyLifetimeLogic() {
            if (lifetimeExpression != null) {
                boolean active = environment.safeResolve(lifetimeExpression.activation()) != 0;
                lifetime = active ? Float.MAX_VALUE : 0.0f;
                if (environment.safeResolve(lifetimeExpression.expiration()) != 0) {
                    expire();
                }
                return;
            }

            if (lifetimeLooping != null) {
                lifetime = activeTimeEval;
                if (!isActive()) {
                    if (sleepRemaining > 0.0f) {
                        sleepRemaining -= BedrockParticleSystem.TICK_SECONDS;
                    } else {
                        restartLoop();
                    }
                }
                return;
            }

            if (lifetimeOnce != null) {
                if (activeTimeEval <= 0.0f) {
                    activeTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeOnce.activeTime()));
                }
                lifetime = activeTimeEval;
                if (!isActive()) {
                    expire();
                }
                return;
            }
        }

        private void restartLoop() {
            age = 0.0f;
            activeTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeLooping.activeTime()));
            sleepTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeLooping.sleepTime()));
            sleepRemaining = sleepTimeEval;
            for (int i = 1; i <= 16; i++) {
                molangContext.setRandom(i, (float) Math.random());
            }
            molangContext.random = molangContext.getRandom(1);
            for (int i = 1; i <= 16; i++) {
                molangContext.setEmitterRandom(i, molangContext.getRandom(i));
            }
            instantEmitted = false;
            maxParticlesEval = 0;
            steadyRemainder = 0.0f;
            updateContext(0.0f);
            if (emitterInitialization != null && emitterInitialization.creationExpression() != null) {
                environment.safeResolve(emitterInitialization.creationExpression());
            }
        }

        private void evaluateLifetimeOnCreate() {
            if (lifetimeOnce != null) {
                activeTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeOnce.activeTime()));
                lifetime = activeTimeEval;
            } else if (lifetimeLooping != null) {
                activeTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeLooping.activeTime()));
                sleepTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeLooping.sleepTime()));
                sleepRemaining = sleepTimeEval;
                lifetime = activeTimeEval;
            }
        }

        private void updateContext(float ageSeconds) {
            molangContext.emitterAge = ageSeconds;
            molangContext.emitterLifetime = lifetime;
            molangContext.particleAge = ageSeconds;
            molangContext.particleLifetime = lifetime;
            molangContext.entityScale = scale;
            updateCurves();
        }

        private void runEvent(String name) {
            if (name == null || name.isEmpty()) {
                return;
            }
            Map<String, ParticleEvent> events = data.events();
            if (events == null || events.isEmpty()) {
                return;
            }
            ParticleEvent event = events.get(name);
            if (event == null) {
                return;
            }
            event.execute(this);
        }

        private void fireEvents(String[] events) {
            if (events == null || events.length == 0) {
                return;
            }
            for (String event : events) {
                runEvent(event);
            }
        }

        private void tickLifetimeEvents() {
            if (lifetimeEvents == null) {
                return;
            }
            ParticleLifetimeEventComponent.TimelineEvent[] timeline = lifetimeEvents.timelineEvents();
            if (timeline == null || lifetimeEventIndex >= timeline.length) {
                return;
            }
            float time = this.age;
            while (lifetimeEventIndex < timeline.length && time >= timeline[lifetimeEventIndex].time()) {
                ParticleLifetimeEventComponent.TimelineEvent evt = timeline[lifetimeEventIndex];
                fireEvents(evt.events());
                lifetimeEventIndex++;
            }
        }

        private void expire() {
            if (expired) {
                return;
            }
            expired = true;
            if (!expirationEventsFired && lifetimeEvents != null) {
                expirationEventsFired = true;
                fireEvents(lifetimeEvents.expirationEvent());
            }
        }

        private void updateCurves() {
            if (curves.isEmpty()) {
                return;
            }
            for (Map.Entry<String, ParticleData.Curve> entry : curves.entrySet()) {
                float value = ParticleCurveEvaluator.evaluateCurve(environment, entry.getValue());
                curveValues.put(entry.getKey(), value);
            }
        }

        private void updateTransform(float deltaSeconds) {
            lastX = x;
            lastY = y;
            lastZ = z;
            lastYaw = yaw;
            copyBasis(basisX, lastBasisX);
            copyBasis(basisY, lastBasisY);
            copyBasis(basisZ, lastBasisZ);
            transformProvider.fill(currentTransform, deltaSeconds);
            x = currentTransform.x;
            y = currentTransform.y;
            z = currentTransform.z;
            yaw = currentTransform.yaw;
            scale = currentTransform.scale;
            copyBasis(currentTransform.basisX, basisX);
            copyBasis(currentTransform.basisY, basisY);
            copyBasis(currentTransform.basisZ, basisZ);
            deltaX = x - lastX;
            deltaY = y - lastY;
            deltaZ = z - lastZ;
            float deltaYawDeg = BedrockParticleSystem.wrapDegrees(yaw - lastYaw);
            deltaYawRad = (float) Math.toRadians(deltaYawDeg);
        }

        private void copyBasis(float[] src, float[] dst) {
            dst[0] = src[0];
            dst[1] = src[1];
            dst[2] = src[2];
        }

        private boolean isActive() {
            return age < lifetime;
        }

        void onParticleExpired() {
            if (activeParticles > 0) {
                activeParticles--;
            }
        }

        boolean hasSpawnedParticles() {
            return spawnedAny;
        }

        boolean isAlive() {
            return !expired || activeParticles > 0;
        }

        float getEmitterAge() {
            return molangContext.emitterAge;
        }

        float getEmitterLifetime() {
            return molangContext.emitterLifetime;
        }

        float getEmitterRandom1() {
            return molangContext.emitterRandom1;
        }

        float getEmitterRandom2() {
            return molangContext.emitterRandom2;
        }

        float getEmitterRandom3() {
            return molangContext.emitterRandom3;
        }

        float getEmitterRandom4() {
            return molangContext.emitterRandom4;
        }

        float getEmitterRandom(int index) {
            return molangContext.getEmitterRandom(index);
        }

        MolangEnvironment getEnvironment() {
            return environment;
        }

        @Override
        public void particleEffect(String effect, ParticleEvent.ParticleSpawnType type) {
            if (effect == null || effect.isEmpty()) {
                return;
            }
            if (type == ParticleEvent.ParticleSpawnType.EMITTER_BOUND) {
                system.spawnEffectFromEmitter(effect, this, true);
                return;
            }
            system.spawnEffectFromEmitter(effect, this, false);
        }

        @Override
        public void soundEffect(String sound) {
            system.playSoundAt(sound, this.x, this.y, this.z);
        }

        @Override
        public void expression(MolangExpression expression) {
            if (expression != null) {
                environment.safeResolve(expression);
            }
        }

        @Override
        public void log(String message) {
            system.logMessage(message);
        }

        @Override
        public Random getRandom() {
            return eventRandom;
        }

        boolean isLocalPosition() {
            return localSpace != null && localSpace.position();
        }

        boolean isLocalRotation() {
            return localSpace != null && localSpace.rotation();
        }

        boolean isLocalVelocity() {
            return localSpace != null && localSpace.velocity();
        }

        boolean shouldRotateAcceleration() {
            return localSpace != null && (localSpace.rotation() || localSpace.velocity());
        }

        double getDeltaX() {
            return deltaX;
        }

        double getDeltaY() {
            return deltaY;
        }

        double getDeltaZ() {
            return deltaZ;
        }

        float getDeltaYawRad() {
            return deltaYawRad;
        }

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        double getZ() {
            return z;
        }

        float getYaw() {
            return yaw;
        }

        public float[] getBasisX() {
            return basisX;
        }

        public float[] getBasisY() {
            return basisY;
        }

        public float[] getBasisZ() {
            return basisZ;
        }

        float getScale() {
            return scale;
        }

        double rotateLocalX(double x, double y, double z) {
            return basisX[0] * x + basisY[0] * y + basisZ[0] * z;
        }

        double rotateLocalY(double x, double y, double z) {
            return basisX[1] * x + basisY[1] * y + basisZ[1] * z;
        }

        double rotateLocalZ(double x, double y, double z) {
            return basisX[2] * x + basisY[2] * y + basisZ[2] * z;
        }

        private double rotateFromLastX(double x, double y, double z) {
            return lastBasisX[0] * x + lastBasisX[1] * y + lastBasisX[2] * z;
        }

        private double rotateFromLastY(double x, double y, double z) {
            return lastBasisY[0] * x + lastBasisY[1] * y + lastBasisY[2] * z;
        }

        private double rotateFromLastZ(double x, double y, double z) {
            return lastBasisZ[0] * x + lastBasisZ[1] * y + lastBasisZ[2] * z;
        }

        void applyDeltaRotation(ActiveParticle particle) {
            double ox = particle.getX() - x;
            double oy = particle.getY() - y;
            double oz = particle.getZ() - z;
            double lx = rotateFromLastX(ox, oy, oz);
            double ly = rotateFromLastY(ox, oy, oz);
            double lz = rotateFromLastZ(ox, oy, oz);
            double rx = rotateLocalX(lx, ly, lz);
            double ry = rotateLocalY(lx, ly, lz);
            double rz = rotateLocalZ(lx, ly, lz);
            particle.setPosition(x + rx, y + ry, z + rz);
        }

        void applyDeltaRotationToVelocity(ActiveParticle particle) {
            double lx = rotateFromLastX(particle.getVx(), particle.getVy(), particle.getVz());
            double ly = rotateFromLastY(particle.getVx(), particle.getVy(), particle.getVz());
            double lz = rotateFromLastZ(particle.getVx(), particle.getVy(), particle.getVz());
            double rx = rotateLocalX(lx, ly, lz);
            double ry = rotateLocalY(lx, ly, lz);
            double rz = rotateLocalZ(lx, ly, lz);
            particle.setVelocity(rx, ry, rz);
        }

        private ActiveParticle obtainParticle(double px, double py, double pz) {
            ActiveParticle particle = particlePool.pollFirst();
            if (particle != null) {
                system.onPoolBorrow();
                particle.reset(px, py, pz);
                return particle;
            }
            return new ActiveParticle(
                system,
                data,
                this,
                px, py, pz,
                billboard,
                tint,
                speed,
                texture,
                emissiveTexture,
                emissiveStrength,
                blendTexture,
                blendMode,
                blendR,
                blendG,
                blendB,
                blendA,
                particleLifetimeComponent
            );
        }

        void recycleParticle(ActiveParticle particle) {
            if (particle == null) {
                return;
            }
            if (particlePoolLimit <= 0) {
                return;
            }
            if (particlePool.size() >= particlePoolLimit) {
                return;
            }
            if (!system.canPoolMore()) {
                return;
            }
            particlePool.addFirst(particle);
            system.onPoolReturn();
        }

        int clearPool() {
            int count = particlePool.size();
            particlePool.clear();
            return count;
        }

        ActiveParticle createParticle(double px, double py, double pz) {
            return obtainParticle(px, py, pz);
        }

        private ParticleEmitterShape resolveEmitterShape() {
            ParticleEmitterShape shape = BedrockParticleSystem.getComponent(data, "emitter_shape_disc");
            if (shape != null) {
                return shape;
            }
            shape = BedrockParticleSystem.getComponent(data, "emitter_shape_box");
            if (shape != null) {
                return shape;
            }
            shape = BedrockParticleSystem.getComponent(data, "emitter_shape_sphere");
            if (shape != null) {
                return shape;
            }
            shape = BedrockParticleSystem.getComponent(data, "emitter_shape_point");
            if (shape != null) {
                return shape;
            }
            shape = BedrockParticleSystem.getComponent(data, "emitter_shape_entity_aabb");
            if (shape != null) {
                return shape;
            }
            shape = BedrockParticleSystem.getComponent(data, "emitter_shape_custom");
            if (shape != null) {
                return shape;
            }
            if (data != null && data.components() != null) {
                for (org.mybad.bedrockparticle.particle.component.ParticleComponent component : data.components().values()) {
                    if (component instanceof ParticleEmitterShape) {
                        return (ParticleEmitterShape) component;
                    }
                }
            }
            return ParticleEmitterShape.EMPTY;
        }

        BedrockParticleSystem getSystem() {
            return system;
        }

        Random getEventRandom() {
            return eventRandom;
        }

        void onParticleSpawned() {
            activeParticles++;
            spawnedAny = true;
        }

        private static ResourceLocation resolveEmissiveTexture(BedrockParticleSystem system, ParticleData data) {
            if (data == null || data.description() == null) {
                return null;
            }
            return system.toMinecraft(data.description().getEmissiveTexture());
        }

        private static float resolveEmissiveStrength(ParticleData data) {
            if (data == null || data.description() == null) {
                return 0.0f;
            }
            float strength = data.description().getEmissiveStrength();
            if (strength < 0.0f) {
                return 0.0f;
            }
            return strength;
        }

        private static ResourceLocation resolveBlendTexture(BedrockParticleSystem system, ParticleData data) {
            if (data == null || data.description() == null) {
                return null;
            }
            return system.toMinecraft(data.description().getBlendTexture());
        }

        private static BedrockParticleSystem.BlendMode resolveBlendMode(ParticleData data) {
            if (data == null || data.description() == null) {
                return BedrockParticleSystem.BlendMode.ALPHA;
            }
            String mode = data.description().getBlendMode();
            if (mode == null || mode.isEmpty()) {
                return BedrockParticleSystem.BlendMode.ALPHA;
            }
            String lower = mode.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("add")) {
                return BedrockParticleSystem.BlendMode.ADD;
            }
            return BedrockParticleSystem.BlendMode.ALPHA;
        }

        private static float[] resolveBlendColor(ParticleData data) {
            if (data == null || data.description() == null) {
                return null;
            }
            return data.description().getBlendColor();
        }

        private static float clamp01(float value) {
            if (Float.isNaN(value)) {
                return 0.0f;
            }
            if (value < 0.0f) {
                return 0.0f;
            }
            if (value > 1.0f) {
                return 1.0f;
            }
            return value;
        }
    }
