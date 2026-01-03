package org.mybad.minecraft.particle.runtime;

import org.mybad.bedrockparticle.molangcompiler.api.MolangEnvironment;
import org.mybad.bedrockparticle.molangcompiler.api.MolangEnvironmentBuilder;
import org.mybad.bedrockparticle.molangcompiler.api.MolangExpression;
import org.mybad.bedrockparticle.pinwheel.particle.ParticleData;
import org.mybad.bedrockparticle.pinwheel.particle.ParticleInstance;
import org.mybad.bedrockparticle.pinwheel.particle.ParticleContext;
import org.mybad.bedrockparticle.pinwheel.particle.component.EmitterInitializationComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleAppearanceBillboardComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleAppearanceLightingComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleAppearanceTintingComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleExpireInBlocksComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleExpireNotInBlocksComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleInitialSpeedComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleInitialSpinComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleLifetimeExpressionComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleLifetimeEventComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleKillPlaneComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleMotionCollisionComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleMotionDynamicComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleMotionParametricComponent;
import org.mybad.bedrockparticle.pinwheel.particle.event.ParticleEvent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.mybad.minecraft.particle.molang.ParticleCurveEvaluator;
import org.mybad.minecraft.particle.molang.ParticleMolangBindings;
import org.mybad.minecraft.particle.molang.ParticleMolangContext;
import org.mybad.minecraft.particle.render.ParticleRenderer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Random;

public class ActiveParticle implements ParticleInstance, ParticleContext {
    private final BedrockParticleSystem system;
    private final ParticleData data;
    private final Map<String, ParticleData.Curve> curves;
    private final Map<String, Float> curveValues;
    private final ParticleRenderer renderer;
    private final ParticleAppearanceBillboardComponent billboard;
    private final ParticleAppearanceTintingComponent tint;
    private final ParticleAppearanceLightingComponent lighting;
    private final ResourceLocation texture;
    private final BedrockParticleSystem.BlendMode blendMode;
    private final ParticleInitialSpeedComponent speed;
    private final ParticleInitialSpinComponent initialSpin;
    private final ParticleMotionDynamicComponent motionDynamic;
    private final ParticleMotionParametricComponent motionParametric;
    private final ParticleMotionCollisionComponent motionCollision;
    private final ParticleLifetimeEventComponent lifetimeEvents;
    private final ParticleKillPlaneComponent killPlane;
    private final ParticleExpireInBlocksComponent expireInBlocks;
    private final ParticleExpireNotInBlocksComponent expireNotInBlocks;
    private final EmitterInitializationComponent particleInitialization;
    private final ParticleLifetimeExpressionComponent lifetimeComponent;
    private final MolangExpression lifetimeExpiration;
    private float lifetime;
    private final ParticleMolangContext molangContext;
    private final MolangEnvironment environment;
    private final ActiveEmitter emitter;
    private final boolean localPosition;
    private final boolean localRotation;
    private final boolean localVelocity;
        private final Block[] expireInBlockIds;
        private final Block[] expireNotInBlockIds;
        private final BlockPos.MutableBlockPos blockPos;
        private final Random eventRandom;
        private int lifetimeEventIndex;

        private double x;
        private double y;
        private double z;
        private double prevX;
        private double prevY;
        private double prevZ;
        private double vx;
        private double vy;
        private double vz;
        private double dirX;
        private double dirY;
        private double dirZ;
        private double ax;
        private double ay;
        private double az;
        private float age;
        private float roll;
        private float prevRoll;
        private float rollVelocity;
        private float rollAcceleration;
        private float collisionRadius;
        private float collisionDrag;
        private float collisionRestitution;
        private boolean expireOnContact;

        ActiveParticle(BedrockParticleSystem system, ParticleData data,
                               ActiveEmitter emitter,
                               double x,
                               double y,
                               double z,
                               ParticleAppearanceBillboardComponent billboard,
                               ParticleAppearanceTintingComponent tint,
                               ParticleInitialSpeedComponent speed,
                               ResourceLocation texture,
                               ParticleLifetimeExpressionComponent lifetimeComponent) {
            this.system = system;
            this.data = data;
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.speed = speed;
            this.initialSpin = BedrockParticleSystem.getComponent(data, "particle_initial_spin");
            this.motionDynamic = BedrockParticleSystem.getComponent(data, "particle_motion_dynamic");
            this.motionParametric = BedrockParticleSystem.getComponent(data, "particle_motion_parametric");
            this.motionCollision = BedrockParticleSystem.getComponent(data, "particle_motion_collision");
            this.lifetimeEvents = BedrockParticleSystem.getComponent(data, "particle_lifetime_events");
            this.killPlane = BedrockParticleSystem.getComponent(data, "particle_kill_plane");
            this.expireInBlocks = BedrockParticleSystem.getComponent(data, "particle_expire_if_in_blocks");
            this.expireNotInBlocks = BedrockParticleSystem.getComponent(data, "particle_expire_if_not_in_blocks");
            this.particleInitialization = BedrockParticleSystem.getComponent(data, "particle_initialization");
            ParticleAppearanceLightingComponent lighting = BedrockParticleSystem.getComponent(data, "particle_appearance_lighting");
            this.billboard = billboard;
            this.tint = tint;
            this.lighting = lighting;
            this.texture = texture;
            this.age = 0.0f;
            this.emitter = emitter;
            this.localPosition = emitter != null && emitter.isLocalPosition();
            this.localRotation = emitter != null && emitter.isLocalRotation();
            this.localVelocity = emitter != null && emitter.isLocalVelocity();
            BedrockParticleSystem.BlendMode blendMode = system.resolveBlendMode(data);
            this.blendMode = blendMode;
            this.expireInBlockIds = system.resolveBlocks(expireInBlocks);
            this.expireNotInBlockIds = system.resolveBlocks(expireNotInBlocks);
            this.blockPos = new BlockPos.MutableBlockPos();
            this.eventRandom = new Random();
            this.lifetimeEventIndex = 0;
            this.molangContext = new ParticleMolangContext();
            this.curves = ParticleMolangBindings.buildCurveDefinitions(data);
            this.curveValues = this.curves.isEmpty() ? Collections.emptyMap() : new HashMap<>(this.curves.size());
            this.molangContext.curves = this.curveValues;
            for (int i = 1; i <= 16; i++) {
                this.molangContext.setRandom(i, (float) Math.random());
            }
            this.molangContext.random = this.molangContext.getRandom(1);
            this.molangContext.entityScale = emitter != null ? emitter.getScale() : 1.0f;
            this.environment = ParticleMolangBindings.createRuntime(this.molangContext, this.curves);
            if (emitter != null) {
                MolangEnvironmentBuilder<? extends MolangEnvironment> builder = this.environment.edit();
                builder.copy(emitter.getEnvironment());
                ParticleMolangBindings.bindCommonVariables(builder, this.molangContext);
                ParticleMolangBindings.bindCurves(builder, this.molangContext, this.curves);
            }
            this.lifetimeComponent = lifetimeComponent;
            this.lifetimeExpiration = lifetimeComponent != null ? lifetimeComponent.expirationExpression() : null;
            this.renderer = new ParticleRenderer(billboard, tint, lighting, texture, blendMode);
            reset(x, y, z);
        }

        void reset(double x, double y, double z) {
            MolangEnvironmentBuilder<? extends MolangEnvironment> builder = this.environment.edit();
            builder.clearVariable();
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.vx = 0.0;
            this.vy = 0.0;
            this.vz = 0.0;
            this.dirX = 0.0;
            this.dirY = 0.0;
            this.dirZ = 0.0;
            this.ax = 0.0;
            this.ay = 0.0;
            this.az = 0.0;
            this.age = 0.0f;
            this.roll = 0.0f;
            this.prevRoll = 0.0f;
            this.rollVelocity = 0.0f;
            this.rollAcceleration = 0.0f;
            this.lifetimeEventIndex = 0;

            for (int i = 1; i <= 16; i++) {
                this.molangContext.setRandom(i, (float) Math.random());
            }
            this.molangContext.random = this.molangContext.getRandom(1);
            this.eventRandom.setSeed(system.getRandom().nextLong());
            if (!this.curveValues.isEmpty()) {
                this.curveValues.clear();
            }

            if (emitter != null) {
                builder.copy(emitter.getEnvironment());
            }
            ParticleMolangBindings.bindCommonVariables(builder, this.molangContext);
            ParticleMolangBindings.bindCurves(builder, this.molangContext, this.curves);

            this.lifetime = resolveLifetime(lifetimeComponent);
            this.molangContext.particleLifetime = this.lifetime;

            updateContext(0.0f);
            if (particleInitialization != null && particleInitialization.creationExpression() != null) {
                environment.safeResolve(particleInitialization.creationExpression());
            }
            if (lifetimeEvents != null) {
                fireEvents(lifetimeEvents.creationEvent());
            }
            if (initialSpin != null) {
                this.roll = environment.safeResolve(initialSpin.rotation());
                this.rollVelocity = environment.safeResolve(initialSpin.rotationRate()) / 20.0f;
            }
            if (motionCollision != null) {
                this.collisionRadius = motionCollision.collisionRadius();
                this.collisionDrag = motionCollision.collisionDrag();
                this.collisionRestitution = motionCollision.coefficientOfRestitution();
                this.expireOnContact = motionCollision.expireOnContact();
            } else {
                this.collisionRadius = 0.0f;
                this.collisionDrag = 0.0f;
                this.collisionRestitution = 0.0f;
                this.expireOnContact = false;
            }
        }

        void applyInitialSpeed() {
            if (speed == null) {
                return;
            }
            MolangExpression[] speeds = speed.speed();
            if (speeds == null || speeds.length < 3) {
                return;
            }
            updateContext(this.age);
            double sx = environment.safeResolve(speeds[0]) / 20.0;
            double sy = environment.safeResolve(speeds[1]) / 20.0;
            double sz = environment.safeResolve(speeds[2]) / 20.0;
            double vx = this.dirX * sx;
            double vy = this.dirY * sy;
            double vz = this.dirZ * sz;
            setVelocity(vx, vy, vz);
        }

        private float resolveLifetime(ParticleLifetimeExpressionComponent lifetimeComponent) {
            if (lifetimeComponent == null) {
                return 1.0f;
            }
            float value = environment.safeResolve(lifetimeComponent.maxLifetime());
            return value > 0.0f ? value : 1.0f;
        }

        boolean tick() {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.prevRoll = this.roll;
            if (emitter != null) {
                if (localPosition) {
                    this.x += emitter.getDeltaX();
                    this.y += emitter.getDeltaY();
                    this.z += emitter.getDeltaZ();
                }
                if (localRotation) {
                    emitter.applyDeltaRotation(this);
                }
                if (localVelocity) {
                    emitter.applyDeltaRotationToVelocity(this);
                }
            }
            updateContext(this.age);
            if (particleInitialization != null && particleInitialization.tickExpression() != null) {
                environment.safeResolve(particleInitialization.tickExpression());
            }
            if (lifetimeExpiration != null && environment.safeResolve(lifetimeExpiration) != 0.0f) {
                return false;
            }
            tickLifetimeEvents();
            applyParametricMotion();
            applyDynamicMotion();
            this.vx += this.ax;
            this.vy += this.ay;
            this.vz += this.az;
            this.rollVelocity += this.rollAcceleration;
            this.roll += this.rollVelocity;
            double nextX = this.x + this.vx;
            double nextY = this.y + this.vy;
            double nextZ = this.z + this.vz;
            boolean collided = false;
            boolean collideX = false;
            boolean collideY = false;
            boolean collideZ = false;
            if (motionCollision != null && isCollisionEnabled() && collisionRadius > 0.0f) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.world != null) {
                    double dx = this.vx;
                    double dy = this.vy;
                    double dz = this.vz;
                    AxisAlignedBB aabb = new AxisAlignedBB(
                        this.x - collisionRadius, this.y - collisionRadius, this.z - collisionRadius,
                        this.x + collisionRadius, this.y + collisionRadius, this.z + collisionRadius
                    );
                    List<AxisAlignedBB> boxes = mc.world.getCollisionBoxes(null, aabb.expand(dx, dy, dz));
                    double originalDx = dx;
                    double originalDy = dy;
                    double originalDz = dz;
                    for (AxisAlignedBB box : boxes) {
                        dy = box.calculateYOffset(aabb, dy);
                    }
                    aabb = aabb.offset(0.0, dy, 0.0);
                    for (AxisAlignedBB box : boxes) {
                        dz = box.calculateZOffset(aabb, dz);
                    }
                    aabb = aabb.offset(0.0, 0.0, dz);
                    for (AxisAlignedBB box : boxes) {
                        dx = box.calculateXOffset(aabb, dx);
                    }
                    aabb = aabb.offset(dx, 0.0, 0.0);

                    collideX = dx != originalDx;
                    collideY = dy != originalDy;
                    collideZ = dz != originalDz;
                    collided = collideX || collideY || collideZ;
                    nextX = this.x + dx;
                    nextY = this.y + dy;
                    nextZ = this.z + dz;
                    this.vx = dx;
                    this.vy = dy;
                    this.vz = dz;
                }
            }
            if (collided) {
                double speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
                if (speed > 0.0) {
                    double newSpeed = Math.max(0.0, speed - collisionDrag);
                    double scale = newSpeed / speed;
                    this.vx *= scale;
                    this.vy *= scale;
                    this.vz *= scale;
                }
                if (collideY) {
                    this.vy = -this.vy * collisionRestitution;
                }
                fireCollisionEvents();
                if (expireOnContact) {
                    return false;
                }
            }
            if (killPlane != null && isKillPlaneCrossed(this.prevX, this.prevY, this.prevZ, nextX, nextY, nextZ)) {
                return false;
            }
            this.x = nextX;
            this.y = nextY;
            this.z = nextZ;
            if (shouldExpireInBlocks() || shouldExpireNotInBlocks()) {
                return false;
            }
            this.age += BedrockParticleSystem.TICK_SECONDS;
            updateContext(this.age);
            return this.age < this.lifetime;
        }

        void render(Minecraft mc, double camX, double camY, double camZ, float partialTicks) {
            prepareRender(partialTicks);
            renderer.render(this, mc, camX, camY, camZ, partialTicks);
        }

        public void prepareRender(float partialTicks) {
            float renderAge = this.age + partialTicks * BedrockParticleSystem.TICK_SECONDS;
            updateContext(renderAge);
            if (particleInitialization != null && particleInitialization.renderExpression() != null) {
                environment.safeResolve(particleInitialization.renderExpression());
            }
        }

        @Override
        public float getParticleAge() {
            return this.age;
        }

        @Override
        public float getParticleLifetime() {
            return this.lifetime;
        }

        @Override
        public MolangEnvironment getEnvironment() {
            return environment;
        }

        public ParticleAppearanceBillboardComponent getBillboard() {
            return billboard;
        }

        public ParticleAppearanceTintingComponent getTint() {
            return tint;
        }

        public ParticleAppearanceLightingComponent getLighting() {
            return lighting;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public BedrockParticleSystem.BlendMode getBlendMode() {
            return blendMode;
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

        private void fireCollisionEvents() {
            if (motionCollision == null) {
                return;
            }
            fireEvents(motionCollision.events());
        }

        private boolean isKillPlaneCrossed(double oldX, double oldY, double oldZ,
                                            double newX, double newY, double newZ) {
            if (killPlane == null) {
                return false;
            }
            double baseX = emitter != null ? emitter.getX() : 0.0;
            double baseY = emitter != null ? emitter.getY() : 0.0;
            double baseZ = emitter != null ? emitter.getZ() : 0.0;
            return killPlane.solve(oldX - baseX, oldY - baseY, oldZ - baseZ,
                newX - baseX, newY - baseY, newZ - baseZ);
        }

        private boolean shouldExpireInBlocks() {
            if (expireInBlockIds == null || expireInBlockIds.length == 0) {
                return false;
            }
            Block block = getCurrentBlock();
            if (block == null) {
                return false;
            }
            for (Block test : expireInBlockIds) {
                if (block == test) {
                    return true;
                }
            }
            return false;
        }

        private boolean shouldExpireNotInBlocks() {
            if (expireNotInBlockIds == null || expireNotInBlockIds.length == 0) {
                return false;
            }
            Block block = getCurrentBlock();
            if (block == null) {
                return false;
            }
            for (Block test : expireNotInBlockIds) {
                if (block == test) {
                    return false;
                }
            }
            return true;
        }

        private Block getCurrentBlock() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.world == null) {
                return null;
            }
            blockPos.setPos((int) Math.floor(this.x), (int) Math.floor(this.y), (int) Math.floor(this.z));
            return mc.world.getBlockState(blockPos).getBlock();
        }

        public int resolvePackedLight(double px, double py, double pz) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.world == null) {
                return 0;
            }
            blockPos.setPos((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
            int packed = mc.world.getCombinedLight(blockPos, 0);
            return packed;
        }


        @Override
        public void particleEffect(String effect, ParticleEvent.ParticleSpawnType type) {
            if (effect == null || effect.isEmpty()) {
                return;
            }
            if (type == ParticleEvent.ParticleSpawnType.EMITTER || type == ParticleEvent.ParticleSpawnType.EMITTER_BOUND) {
                if (emitter != null) {
                    emitter.particleEffect(effect, ParticleEvent.ParticleSpawnType.PARTICLE);
                } else {
                    system.spawnEffectAt(effect, this.x, this.y, this.z, false);
                }
                return;
            }
            system.spawnEffectAt(effect, this.x, this.y, this.z, false);
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

        private void updateContext(float ageSeconds) {
            this.molangContext.particleAge = ageSeconds;
            if (emitter != null) {
                this.molangContext.emitterAge = emitter.getEmitterAge();
                this.molangContext.emitterLifetime = emitter.getEmitterLifetime();
                for (int i = 1; i <= 16; i++) {
                    this.molangContext.setEmitterRandom(i, emitter.getEmitterRandom(i));
                }
                this.molangContext.entityScale = emitter.getScale();
            } else {
                this.molangContext.emitterAge = ageSeconds;
                this.molangContext.emitterLifetime = this.lifetime;
                for (int i = 1; i <= 16; i++) {
                    this.molangContext.setEmitterRandom(i, this.molangContext.getRandom(i));
                }
                this.molangContext.entityScale = 1.0f;
            }
            updateCurves();
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

        private boolean isCollisionEnabled() {
            if (motionCollision == null) {
                return false;
            }
            return environment.safeResolve(motionCollision.enabled()) != 0.0f;
        }

        private boolean isColliding(double cx, double cy, double cz, float radius) {
            if (radius <= 0.0f) {
                return false;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.world == null) {
                return false;
            }
            AxisAlignedBB aabb = new AxisAlignedBB(
                cx - radius, cy - radius, cz - radius,
                cx + radius, cy + radius, cz + radius
            );
            return !mc.world.getCollisionBoxes(null, aabb).isEmpty();
        }

        private void applyDynamicMotion() {
            if (motionDynamic == null) {
                this.ax = 0.0;
                this.ay = 0.0;
                this.az = 0.0;
                this.rollAcceleration = 0.0f;
                return;
            }
            MolangExpression[] accel = motionDynamic.linearAcceleration();
            double ax = environment.safeResolve(accel[0]) / 400.0;
            double ay = environment.safeResolve(accel[1]) / 400.0;
            double az = environment.safeResolve(accel[2]) / 400.0;
            if (emitter != null && localRotation) {
                double rax = emitter.rotateLocalX(ax, ay, az);
                double ray = emitter.rotateLocalY(ax, ay, az);
                double raz = emitter.rotateLocalZ(ax, ay, az);
                ax = rax;
                ay = ray;
                az = raz;
            }
            double drag = environment.safeResolve(motionDynamic.linearDragCoefficient()) / 20.0;
            this.ax = ax - drag * this.vx;
            this.ay = ay - drag * this.vy;
            this.az = az - drag * this.vz;
            float rotAcc = (float) (environment.safeResolve(motionDynamic.rotationAcceleration()) / 400.0);
            float rotDrag = (float) (environment.safeResolve(motionDynamic.rotationDragCoefficient()) / 20.0);
            this.rollAcceleration = rotAcc - rotDrag * this.rollVelocity;
        }

        private void applyParametricMotion() {
            if (motionParametric == null) {
                return;
            }
            MolangExpression[] relative = motionParametric.relativePosition();
            if (relative != null && emitter != null) {
                double lx = environment.safeResolve(relative[0]);
                double ly = environment.safeResolve(relative[1]);
                double lz = environment.safeResolve(relative[2]);
                double rx = localPosition ? emitter.rotateLocalX(lx, ly, lz) : lx;
                double ry = localPosition ? emitter.rotateLocalY(lx, ly, lz) : ly;
                double rz = localPosition ? emitter.rotateLocalZ(lx, ly, lz) : lz;
                setPosition(emitter.getX() + rx, emitter.getY() + ry, emitter.getZ() + rz);
            }
            MolangExpression[] dir = motionParametric.direction();
            if (dir != null) {
                double lx = environment.safeResolve(dir[0]);
                double ly = environment.safeResolve(dir[1]);
                double lz = environment.safeResolve(dir[2]);
                double dx = (emitter != null && localVelocity) ? emitter.rotateLocalX(lx, ly, lz) : lx;
                double dy = (emitter != null && localVelocity) ? emitter.rotateLocalY(lx, ly, lz) : ly;
                double dz = (emitter != null && localVelocity) ? emitter.rotateLocalZ(lx, ly, lz) : lz;
                setDirection(dx, dy, dz);
            }
            this.roll = environment.safeResolve(motionParametric.rotation());
        }

        private void rotateAroundEmitter(double emitterX, double emitterZ, float yawDeltaRad) {
            if (yawDeltaRad == 0.0f) {
                return;
            }
            double ox = this.x - emitterX;
            double oz = this.z - emitterZ;
            double cos = Math.cos(yawDeltaRad);
            double sin = Math.sin(yawDeltaRad);
            double rx = ox * cos + oz * sin;
            double rz = -ox * sin + oz * cos;
            this.x = emitterX + rx;
            this.z = emitterZ + rz;
        }

        private void rotateVelocity(float yawDeltaRad) {
            if (yawDeltaRad == 0.0f) {
                return;
            }
            double cos = Math.cos(yawDeltaRad);
            double sin = Math.sin(yawDeltaRad);
            double rvx = this.vx * cos + this.vz * sin;
            double rvz = -this.vx * sin + this.vz * cos;
            this.vx = rvx;
            this.vz = rvz;
        }

        void onExpired() {
            if (lifetimeEvents != null) {
                fireEvents(lifetimeEvents.expirationEvent());
            }
            if (emitter != null) {
                emitter.onParticleExpired();
            }
        }

        void recycle() {
            if (emitter != null) {
                emitter.recycleParticle(this);
            }
        }

        void syncPrev() {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.prevRoll = this.roll;
        }

        void setPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getPrevX() {
            return prevX;
        }

        public double getY() {
            return y;
        }

        public double getPrevY() {
            return prevY;
        }

        public double getZ() {
            return z;
        }

        public double getPrevZ() {
            return prevZ;
        }

        public double getVx() {
            return vx;
        }

        public double getVy() {
            return vy;
        }

        public double getVz() {
            return vz;
        }

        public float getRoll() {
            return roll;
        }

        public float getPrevRoll() {
            return prevRoll;
        }

        public ActiveEmitter getEmitter() {
            return emitter;
        }

        void setVelocity(double vx, double vy, double vz) {
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            setDirection(vx, vy, vz);
        }

        private void setDirection(double dx, double dy, double dz) {
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 0.0) {
                this.dirX = dx / length;
                this.dirY = dy / length;
                this.dirZ = dz / length;
            } else {
                this.dirX = 0.0;
                this.dirY = 0.0;
                this.dirZ = 0.0;
            }
            double speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy + this.vz * this.vz);
            this.vx = this.dirX * speed;
            this.vy = this.dirY * speed;
            this.vz = this.dirZ * speed;
        }
    }
