package org.mybad.minecraft.particle;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangEnvironmentBuilder;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.MolangRuntime;
import gg.moonflower.pinwheel.particle.ParticleData;
import gg.moonflower.pinwheel.particle.ParticleInstance;
import gg.moonflower.pinwheel.particle.ParticleContext;
import gg.moonflower.pinwheel.particle.component.EmitterRateInstantComponent;
import gg.moonflower.pinwheel.particle.component.EmitterRateSteadyComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLocalSpaceComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLifetimeOnceComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLifetimeLoopingComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLifetimeExpressionComponent;
import gg.moonflower.pinwheel.particle.component.EmitterInitializationComponent;
import gg.moonflower.pinwheel.particle.component.ParticleEmitterShape;
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceBillboardComponent;
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceTintingComponent;
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceLightingComponent;
import gg.moonflower.pinwheel.particle.component.ParticleInitialSpinComponent;
import gg.moonflower.pinwheel.particle.component.ParticleInitialSpeedComponent;
import gg.moonflower.pinwheel.particle.component.ParticleLifetimeExpressionComponent;
import gg.moonflower.pinwheel.particle.component.ParticleLifetimeEventComponent;
import gg.moonflower.pinwheel.particle.component.ParticleKillPlaneComponent;
import gg.moonflower.pinwheel.particle.component.ParticleExpireInBlocksComponent;
import gg.moonflower.pinwheel.particle.component.ParticleExpireNotInBlocksComponent;
import gg.moonflower.pinwheel.particle.component.ParticleMotionDynamicComponent;
import gg.moonflower.pinwheel.particle.component.ParticleMotionParametricComponent;
import gg.moonflower.pinwheel.particle.component.ParticleMotionCollisionComponent;
import gg.moonflower.pinwheel.particle.event.ParticleEvent;
import gg.moonflower.pollen.particle.render.QuadRenderProperties;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.resource.ResourceLoader;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class BedrockParticleDebugSystem {

    private static final float TICK_SECONDS = 1.0f / 20.0f;
    private static final int MAX_DEBUG_PARTICLES = 2000;
    private static final FloatBuffer ORIENTATION_BUFFER = BufferUtils.createFloatBuffer(16);

    private final ResourceLoader resourceLoader;
    private final List<ActiveParticle> particles;
    private final List<ActiveEmitter> emitters;
    private final List<ActiveParticle> pendingParticles;
    private final List<ActiveEmitter> pendingEmitters;
    private final Random random;
    private boolean ticking;

    public BedrockParticleDebugSystem(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.particles = new ArrayList<>();
        this.emitters = new ArrayList<>();
        this.pendingParticles = new ArrayList<>();
        this.pendingEmitters = new ArrayList<>();
        this.random = new Random();
        this.ticking = false;
    }

    public boolean spawn(String particlePath, double x, double y, double z, int overrideCount) {
        return spawnInternal(particlePath, new StaticTransformProvider(x, y, z, 0.0f), overrideCount);
    }

    public boolean spawn(String particlePath, EmitterTransformProvider provider, int overrideCount) {
        return spawnInternal(particlePath, provider, overrideCount);
    }

    private boolean spawnInternal(String particlePath, EmitterTransformProvider provider, int overrideCount) {
        ParticleData data = resourceLoader.loadParticle(particlePath);
        if (data == null) {
            return false;
        }
        int room = getRemainingParticleRoom();
        if (room <= 0) {
            return false;
        }
        ActiveEmitter emitter = new ActiveEmitter(data, provider, overrideCount);
        emitter.emitInitialParticles(room);
        if (emitter.isAlive()) {
            addEmitter(emitter);
        }
        return emitter.hasSpawnedParticles() || emitter.isAlive();
    }

    private void addEmitter(ActiveEmitter emitter) {
        if (emitter == null) {
            return;
        }
        if (ticking) {
            pendingEmitters.add(emitter);
        } else {
            emitters.add(emitter);
        }
    }

    private void addParticle(ActiveParticle particle) {
        if (particle == null) {
            return;
        }
        if (ticking) {
            pendingParticles.add(particle);
        } else {
            particles.add(particle);
        }
    }

    private void flushPending() {
        if (!pendingEmitters.isEmpty()) {
            emitters.addAll(pendingEmitters);
            pendingEmitters.clear();
        }
        if (!pendingParticles.isEmpty()) {
            particles.addAll(pendingParticles);
            pendingParticles.clear();
        }
    }

    private int getRemainingParticleRoom() {
        int used = particles.size() + pendingParticles.size();
        return MAX_DEBUG_PARTICLES - used;
    }

    public void clear() {
        particles.clear();
        emitters.clear();
        pendingParticles.clear();
        pendingEmitters.clear();
    }

    public int getActiveCount() {
        return particles.size() + pendingParticles.size();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().world == null) {
            particles.clear();
            emitters.clear();
            pendingParticles.clear();
            pendingEmitters.clear();
            return;
        }
        ticking = true;
        if (!emitters.isEmpty()) {
            Iterator<ActiveEmitter> emitterIterator = emitters.iterator();
            while (emitterIterator.hasNext()) {
                ActiveEmitter emitter = emitterIterator.next();
                if (!emitter.tick()) {
                    emitterIterator.remove();
                }
            }
        }
        Iterator<ActiveParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            ActiveParticle particle = iterator.next();
            if (!particle.tick()) {
                particle.onExpired();
                iterator.remove();
            }
        }
        ticking = false;
        flushPending();
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (particles.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager() == null) {
            return;
        }
        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        float partialTicks = event.getPartialTicks();

        if (!emitters.isEmpty()) {
            for (ActiveEmitter emitter : emitters) {
                emitter.render(partialTicks);
            }
        }

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);

        for (ActiveParticle particle : particles) {
            particle.render(mc, camX, camY, camZ, partialTicks);
        }

        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }

    private ResourceLocation toMinecraft(ParticleData data) {
        if (data == null || data.description() == null || data.description().getTexture() == null) {
            return new ResourceLocation("missingno");
        }
        org.mybad.bedrockparticle.ResourceLocation tex = data.description().getTexture();
        return new ResourceLocation(tex.getNamespace(), tex.getPath());
    }

    @SuppressWarnings("unchecked")
    private static <T> T getComponent(ParticleData data, String name) {
        if (data == null) {
            return null;
        }
        Map<String, gg.moonflower.pinwheel.particle.component.ParticleComponent> components = data.components();
        if (components == null || components.isEmpty()) {
            return null;
        }
        gg.moonflower.pinwheel.particle.component.ParticleComponent component = components.get("minecraft:" + name);
        if (component == null) {
            component = components.get(name);
        }
        if (component == null) {
            return null;
        }
        try {
            return (T) component;
        } catch (ClassCastException ex) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 粒子组件类型不匹配: {}", name);
            return null;
        }
    }

    private final class ActiveParticle implements ParticleInstance, ParticleContext {
        private final ParticleData data;
        private final Map<String, ParticleData.Curve> curves;
        private final Map<String, Float> curveValues;
        private final QuadRenderProperties renderProps;
        private final ParticleAppearanceBillboardComponent billboard;
        private final ParticleAppearanceTintingComponent tint;
        private final ParticleAppearanceLightingComponent lighting;
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
        private final MolangExpression lifetimeExpiration;
        private final ResourceLocation texture;
        private final float lifetime;
        private final ParticleMolangContext molangContext;
        private final MolangEnvironment environment;
        private final ActiveEmitter emitter;
        private final boolean localPosition;
        private final boolean localRotation;
        private final boolean localVelocity;
        private final BlendMode blendMode;
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
        private final float[] tempDir;
        private final float[] tempAxisX;
        private final float[] tempAxisY;
        private final float[] tempAxisZ;
        private final Quaternionf tempQuat;
        private final Vector3f tempVecX;
        private final Vector3f tempVecY;
        private final Vector3f tempVecZ;
        private final Vector3f tempVecA;
        private final Vector3f tempVecB;

        private ActiveParticle(ParticleData data,
                               ActiveEmitter emitter,
                               double x,
                               double y,
                               double z,
                               ParticleAppearanceBillboardComponent billboard,
                               ParticleAppearanceTintingComponent tint,
                               ParticleInitialSpeedComponent speed,
                               ResourceLocation texture,
                               ParticleLifetimeExpressionComponent lifetimeComponent) {
            this.data = data;
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.billboard = billboard;
            this.tint = tint;
            this.lighting = getComponent(data, "particle_appearance_lighting");
            this.speed = speed;
            this.initialSpin = getComponent(data, "particle_initial_spin");
            this.motionDynamic = getComponent(data, "particle_motion_dynamic");
            this.motionParametric = getComponent(data, "particle_motion_parametric");
            this.motionCollision = getComponent(data, "particle_motion_collision");
            this.lifetimeEvents = getComponent(data, "particle_lifetime_events");
            this.killPlane = getComponent(data, "particle_kill_plane");
            this.expireInBlocks = getComponent(data, "particle_expire_if_in_blocks");
            this.expireNotInBlocks = getComponent(data, "particle_expire_if_not_in_blocks");
            this.particleInitialization = getComponent(data, "particle_initialization");
            this.texture = texture;
            this.renderProps = new QuadRenderProperties();
            this.age = 0.0f;
            this.emitter = emitter;
            this.localPosition = emitter != null && emitter.isLocalPosition();
            this.localRotation = emitter != null && emitter.isLocalRotation();
            this.localVelocity = emitter != null && emitter.isLocalVelocity();
            this.blendMode = resolveBlendMode(data);
            this.expireInBlockIds = resolveBlocks(expireInBlocks);
            this.expireNotInBlockIds = resolveBlocks(expireNotInBlocks);
            this.blockPos = new BlockPos.MutableBlockPos();
            this.eventRandom = new Random();
            this.lifetimeEventIndex = 0;
            this.molangContext = new ParticleMolangContext();
            this.curves = buildCurveDefinitions(data);
            this.curveValues = this.curves.isEmpty() ? Collections.emptyMap() : new HashMap<>(this.curves.size());
            this.molangContext.curves = this.curveValues;
            for (int i = 1; i <= 16; i++) {
                this.molangContext.setRandom(i, (float) Math.random());
            }
            this.molangContext.random = this.molangContext.getRandom(1);
            this.molangContext.entityScale = emitter != null ? emitter.getScale() : 1.0f;
            this.environment = createRuntime(this.molangContext, this.curves);
            if (emitter != null) {
                MolangEnvironmentBuilder<? extends MolangEnvironment> builder = this.environment.edit();
                builder.copy(emitter.environment);
                bindCommonVariables(builder, this.molangContext);
                bindCurves(builder, this.molangContext, this.curves);
            }
            this.lifetime = resolveLifetime(lifetimeComponent);
            this.lifetimeExpiration = lifetimeComponent != null ? lifetimeComponent.expirationExpression() : null;
            this.molangContext.particleLifetime = this.lifetime;
            this.tempDir = new float[3];
            this.tempAxisX = new float[3];
            this.tempAxisY = new float[3];
            this.tempAxisZ = new float[3];
            this.tempQuat = new Quaternionf();
            this.tempVecX = new Vector3f();
            this.tempVecY = new Vector3f();
            this.tempVecZ = new Vector3f();
            this.tempVecA = new Vector3f();
            this.tempVecB = new Vector3f();
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

        private void applyInitialSpeed() {
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

        private boolean tick() {
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
            this.age += TICK_SECONDS;
            updateContext(this.age);
            return this.age < this.lifetime;
        }

        private void render(Minecraft mc, double camX, double camY, double camZ, float partialTicks) {
            float renderAge = this.age + partialTicks * TICK_SECONDS;
            updateContext(renderAge);
            if (particleInitialization != null && particleInitialization.renderExpression() != null) {
                environment.safeResolve(particleInitialization.renderExpression());
            }
            float width = 1.0f;
            float height = 1.0f;
            if (billboard != null && billboard.size() != null && billboard.size().length >= 2) {
                width = environment.safeResolve(billboard.size()[0]);
                height = environment.safeResolve(billboard.size()[1]);
            }
            if (emitter != null) {
                float scale = emitter.getScale();
                if (scale != 1.0f) {
                    width *= scale;
                    height *= scale;
                }
            }
            if (width <= 0.0f || height <= 0.0f) {
                return;
            }
            renderProps.setWidth(width);
            renderProps.setHeight(height);
            renderProps.setUV(0.0f, 0.0f, 1.0f, 1.0f);
            if (billboard != null) {
                billboard.textureSetter().setUV(this, environment, renderProps);
            }
            double px = this.prevX + (this.x - this.prevX) * partialTicks;
            double py = this.prevY + (this.y - this.prevY) * partialTicks;
            double pz = this.prevZ + (this.z - this.prevZ) * partialTicks;
            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            float a = 1.0f;
            if (tint != null) {
                r = clamp01(tint.red().get(this, environment));
                g = clamp01(tint.green().get(this, environment));
                b = clamp01(tint.blue().get(this, environment));
                a = clamp01(tint.alpha().get(this, environment));
            }
            renderProps.setColor(r, g, b, a);

            applyBlendMode();
            GlStateManager.pushMatrix();
            GlStateManager.translate(px - camX, py - camY, pz - camZ);
            GlStateManager.translate(0.0, 0.01, 0.0);
            float yaw = mc.getRenderManager().playerViewY;
            float pitch = mc.getRenderManager().playerViewX;
            boolean oriented = false;
            boolean directionMode = false;
            if (billboard != null && billboard.cameraMode() != null) {
                switch (billboard.cameraMode()) {
                    case ROTATE_Y:
                        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                        oriented = true;
                        break;
                    case ROTATE_XYZ:
                        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
                        oriented = true;
                        break;
                    case LOOK_AT_XYZ:
                        oriented = applyLookAt(camX, camY, camZ, px, py, pz, false);
                        break;
                    case LOOK_AT_Y:
                        oriented = applyLookAt(camX, camY, camZ, px, py, pz, true);
                        break;
                    case EMITTER_TRANSFORM_XY:
                    case EMITTER_TRANSFORM_XZ:
                    case EMITTER_TRANSFORM_YZ:
                        oriented = applyEmitterTransform(billboard.cameraMode());
                        break;
                    case LOOKAT_DIRECTION:
                    case DIRECTION_X:
                    case DIRECTION_Y:
                    case DIRECTION_Z:
                        directionMode = true;
                        oriented = applyDirectionFacing(billboard.cameraMode(), camX, camY, camZ, px, py, pz);
                        break;
                    default:
                        break;
                }
            }
            if (!oriented) {
                if (directionMode) {
                    GlStateManager.popMatrix();
                    resetBlendMode();
                    return;
                }
                GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            }
            int prevLightX = (int) OpenGlHelper.lastBrightnessX;
            int prevLightY = (int) OpenGlHelper.lastBrightnessY;
            int lightX;
            int lightY;
            if (lighting != null) {
                int packed = resolvePackedLight(px, py, pz);
                lightX = packed & 0xFFFF;
                lightY = (packed >> 16) & 0xFFFF;
                renderProps.setPackedLight(packed);
            } else {
                lightX = 240;
                lightY = 240;
                renderProps.setPackedLight((lightY << 16) | lightX);
            }
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lightX, (float) lightY);
            float renderRoll = this.prevRoll + (this.roll - this.prevRoll) * partialTicks;
            if (renderRoll != 0.0f) {
                GlStateManager.rotate(renderRoll, 0.0F, 0.0F, 1.0F);
            }
            mc.getTextureManager().bindTexture(texture);

            float halfW = renderProps.getWidth() * 0.5f;
            float halfH = renderProps.getHeight() * 0.5f;
            float u0 = renderProps.getUMin();
            float v0 = renderProps.getVMin();
            float u1 = renderProps.getUMax();
            float v1 = renderProps.getVMax();

            int cr = toColor(renderProps.getRed());
            int cg = toColor(renderProps.getGreen());
            int cb = toColor(renderProps.getBlue());
            int ca = toColor(renderProps.getAlpha());

            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(-halfW, -halfH, 0.0).tex(u0, v1).color(cr, cg, cb, ca).endVertex();
            buffer.pos(halfW, -halfH, 0.0).tex(u1, v1).color(cr, cg, cb, ca).endVertex();
            buffer.pos(halfW, halfH, 0.0).tex(u1, v0).color(cr, cg, cb, ca).endVertex();
            buffer.pos(-halfW, halfH, 0.0).tex(u0, v0).color(cr, cg, cb, ca).endVertex();
            Tessellator.getInstance().draw();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) prevLightX, (float) prevLightY);

            GlStateManager.popMatrix();
            resetBlendMode();
        }

        private boolean applyLookAt(double camX, double camY, double camZ, double px, double py, double pz, boolean yOnly) {
            float dx = (float) (camX - px);
            float dy = (float) (camY - py);
            float dz = (float) (camZ - pz);
            if (yOnly) {
                dy = 0.0f;
            }
            tempDir[0] = dx;
            tempDir[1] = dy;
            tempDir[2] = dz;
            if (tempDir[0] == 0.0f && tempDir[1] == 0.0f && tempDir[2] == 0.0f) {
                return false;
            }
            return applyDirectionAsNormal(tempDir);
        }

        private boolean applyDirectionFacing(ParticleAppearanceBillboardComponent.FaceCameraMode mode,
                                             double camX, double camY, double camZ,
                                             double px, double py, double pz) {
            if (!resolveFacingDirection(tempDir)) {
                return false;
            }
            float dx = tempDir[0];
            float dy = tempDir[1];
            float dz = tempDir[2];
            float lenSq = dx * dx + dy * dy + dz * dz;
            if (lenSq <= 1.0e-6f) {
                return false;
            }
            switch (mode) {
                case DIRECTION_X:
                    return applyDirectionX(dx, dy, dz);
                case DIRECTION_Y:
                    return applyDirectionY(dx, dy, dz);
                case DIRECTION_Z:
                    return applyDirectionZ(dx, dy, dz);
                case LOOKAT_DIRECTION:
                default:
                    return applyLookAtDirection(dx, dy, dz, camX, camY, camZ, px, py, pz);
            }
        }

        private boolean applyDirectionX(float dx, float dy, float dz) {
            float yawDeg = getDirectionYawDeg(dx, dy, dz);
            float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
            tempQuat.identity();
            tempQuat.rotateY((float) Math.toRadians(yawDeg));
            tempQuat.rotateX((float) Math.toRadians(pitchDeg));
            tempQuat.rotateY((float) Math.toRadians(90.0f));
            tempQuat.rotateZ((float) Math.toRadians(90.0f));
            return applyRotationFromQuaternion();
        }

        private boolean applyDirectionY(float dx, float dy, float dz) {
            float yawDeg = getDirectionYawDeg(dx, dy, dz);
            float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
            tempQuat.identity();
            tempQuat.rotateY((float) Math.toRadians(yawDeg));
            tempQuat.rotateX((float) Math.toRadians(pitchDeg + 90.0f));
            tempQuat.rotateZ((float) Math.toRadians(90.0f));
            return applyRotationFromQuaternion();
        }

        private boolean applyDirectionZ(float dx, float dy, float dz) {
            float yawDeg = getDirectionYawDeg(dx, dy, dz);
            float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
            tempQuat.identity();
            tempQuat.rotateY((float) Math.toRadians(yawDeg));
            tempQuat.rotateX((float) Math.toRadians(pitchDeg));
            tempQuat.rotateZ((float) Math.toRadians(90.0f));
            return applyRotationFromQuaternion();
        }

        private boolean applyLookAtDirection(float dx, float dy, float dz,
                                             double camX, double camY, double camZ,
                                             double px, double py, double pz) {
            // Blockbuster/Snowstorm-like: align to direction, then rotate around local Y to face camera.
            float yawDeg = getDirectionYawDeg(dx, dy, dz);
            float pitchDeg = getDirectionPitchDeg(dx, dy, dz);
            tempQuat.identity();
            tempQuat.rotateY((float) Math.toRadians(yawDeg));
            tempQuat.rotateX((float) Math.toRadians(pitchDeg + 90.0f));

            // rotated normal
            tempVecZ.set(0.0f, 0.0f, 1.0f);
            tempQuat.transform(tempVecZ);

            // camera direction projected onto plane (direction is the plane normal)
            tempVecA.set((float) (camX - px), (float) (camY - py), (float) (camZ - pz));
            tempVecB.set(dx, dy, dz);
            if (tempVecB.lengthSquared() <= 1.0e-6f) {
                return false;
            }
            tempVecB.normalize();
            float dot = tempVecA.dot(tempVecB);
            tempVecA.sub(tempVecB.x * dot, tempVecB.y * dot, tempVecB.z * dot);
            if (tempVecA.lengthSquared() <= 1.0e-6f) {
                return false;
            }
            tempVecA.normalize();

            tempVecX.set(tempVecA).cross(tempVecZ);
            float angle = tempVecA.angle(tempVecZ);
            float sign = tempVecX.dot(tempVecB);
            float finalRot = (float) -Math.copySign(angle, sign);
            tempQuat.rotateY(finalRot);
            tempQuat.rotateZ((float) Math.toRadians(90.0f));

            return applyRotationFromQuaternion();
        }

        private float getDirectionYawDeg(float dx, float dy, float dz) {
            double yaw = Math.atan2(-dx, dz);
            return (float) -Math.toDegrees(yaw);
        }

        private float getDirectionPitchDeg(float dx, float dy, float dz) {
            double pitch = Math.atan2(dy, Math.sqrt(dx * dx + dz * dz));
            return (float) -Math.toDegrees(pitch);
        }

        private boolean applyRotationFromQuaternion() {
            tempVecX.set(1.0f, 0.0f, 0.0f);
            tempVecY.set(0.0f, 1.0f, 0.0f);
            tempVecZ.set(0.0f, 0.0f, 1.0f);
            tempQuat.transform(tempVecX);
            tempQuat.transform(tempVecY);
            tempQuat.transform(tempVecZ);
            tempAxisX[0] = tempVecX.x;
            tempAxisX[1] = tempVecX.y;
            tempAxisX[2] = tempVecX.z;
            tempAxisY[0] = tempVecY.x;
            tempAxisY[1] = tempVecY.y;
            tempAxisY[2] = tempVecY.z;
            tempAxisZ[0] = tempVecZ.x;
            tempAxisZ[1] = tempVecZ.y;
            tempAxisZ[2] = tempVecZ.z;
            normalize(tempAxisX);
            normalize(tempAxisY);
            normalize(tempAxisZ);
            multMatrix(tempAxisX, tempAxisY, tempAxisZ);
            return true;
        }

        private boolean applyEmitterTransform(ParticleAppearanceBillboardComponent.FaceCameraMode mode) {
            if (emitter == null) {
                return false;
            }
            float[] ex = emitter.getBasisX();
            float[] ey = emitter.getBasisY();
            float[] ez = emitter.getBasisZ();
            if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_XZ) {
                tempAxisX[0] = ex[0];
                tempAxisX[1] = ex[1];
                tempAxisX[2] = ex[2];
                tempAxisY[0] = -ez[0];
                tempAxisY[1] = -ez[1];
                tempAxisY[2] = -ez[2];
                tempAxisZ[0] = -ey[0];
                tempAxisZ[1] = -ey[1];
                tempAxisZ[2] = -ey[2];
            } else if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_YZ) {
                tempAxisX[0] = -ez[0];
                tempAxisX[1] = -ez[1];
                tempAxisX[2] = -ez[2];
                tempAxisY[0] = ey[0];
                tempAxisY[1] = ey[1];
                tempAxisY[2] = ey[2];
                tempAxisZ[0] = ex[0];
                tempAxisZ[1] = ex[1];
                tempAxisZ[2] = ex[2];
            } else if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_XY) {
                tempAxisX[0] = ex[0];
                tempAxisX[1] = ex[1];
                tempAxisX[2] = ex[2];
                tempAxisY[0] = ey[0];
                tempAxisY[1] = ey[1];
                tempAxisY[2] = ey[2];
                tempAxisZ[0] = ez[0];
                tempAxisZ[1] = ez[1];
                tempAxisZ[2] = ez[2];
            } else {
                tempAxisX[0] = ex[0];
                tempAxisX[1] = ex[1];
                tempAxisX[2] = ex[2];
                tempAxisY[0] = ey[0];
                tempAxisY[1] = ey[1];
                tempAxisY[2] = ey[2];
                tempAxisZ[0] = ez[0];
                tempAxisZ[1] = ez[1];
                tempAxisZ[2] = ez[2];
            }
            normalize(tempAxisX);
            normalize(tempAxisY);
            normalize(tempAxisZ);
            multMatrix(tempAxisX, tempAxisY, tempAxisZ);
            return true;
        }

        private boolean resolveFacingDirection(float[] out) {
            if (billboard != null && billboard.customDirection() != null) {
                MolangExpression[] custom = billboard.customDirection();
                out[0] = environment.safeResolve(custom[0]);
                out[1] = environment.safeResolve(custom[1]);
                out[2] = environment.safeResolve(custom[2]);
            } else {
                float speed = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
                float threshold = billboard != null ? billboard.minSpeedThreshold() : 0.0f;
                if (speed <= threshold) {
                    return false;
                }
                out[0] = (float) vx;
                out[1] = (float) vy;
                out[2] = (float) vz;
            }
            if (out[0] == 0.0f && out[1] == 0.0f && out[2] == 0.0f) {
                return false;
            }
            normalize(out);
            return true;
        }

        private boolean applyDirectionAsNormal(float[] direction) {
            tempAxisZ[0] = direction[0];
            tempAxisZ[1] = direction[1];
            tempAxisZ[2] = direction[2];
            float[] up = tempAxisY;
            up[0] = 0.0f;
            up[1] = 1.0f;
            up[2] = 0.0f;
            if (Math.abs(dot(tempAxisZ, up)) > 0.99f) {
                up[0] = 1.0f;
                up[1] = 0.0f;
                up[2] = 0.0f;
            }
            cross(up, tempAxisZ, tempAxisX);
            normalize(tempAxisX);
            cross(tempAxisZ, tempAxisX, tempAxisY);
            normalize(tempAxisY);
            normalize(tempAxisZ);
            multMatrix(tempAxisX, tempAxisY, tempAxisZ);
            return true;
        }

        private boolean applyDirectionAsAxis(float[] direction, int axis) {
            float[] up = tempAxisZ;
            up[0] = 0.0f;
            up[1] = 1.0f;
            up[2] = 0.0f;
            if (axis == 1 && Math.abs(dot(direction, up)) > 0.99f) {
                up[0] = 1.0f;
                up[1] = 0.0f;
                up[2] = 0.0f;
            }
            if (axis == 0) {
                tempAxisX[0] = direction[0];
                tempAxisX[1] = direction[1];
                tempAxisX[2] = direction[2];
                normalize(tempAxisX);
                cross(up, tempAxisX, tempAxisY);
                normalize(tempAxisY);
                cross(tempAxisX, tempAxisY, tempAxisZ);
            } else if (axis == 1) {
                tempAxisY[0] = direction[0];
                tempAxisY[1] = direction[1];
                tempAxisY[2] = direction[2];
                normalize(tempAxisY);
                cross(up, tempAxisY, tempAxisX);
                normalize(tempAxisX);
                cross(tempAxisY, tempAxisX, tempAxisZ);
            } else {
                return applyDirectionAsNormal(direction);
            }
            normalize(tempAxisZ);
            multMatrix(tempAxisX, tempAxisY, tempAxisZ);
            return true;
        }

        private int toColor(float value) {
            return Math.min(255, Math.max(0, (int) (value * 255.0f)));
        }

        private float clamp01(float value) {
            if (value < 0.0f) {
                return 0.0f;
            }
            if (value > 1.0f) {
                return 1.0f;
            }
            return value;
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

        private int resolvePackedLight(double px, double py, double pz) {
            if (lighting == null) {
                return 0;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.world == null) {
                return 0;
            }
            blockPos.setPos((int) Math.floor(px), (int) Math.floor(py), (int) Math.floor(pz));
            int packed = mc.world.getCombinedLight(blockPos, 0);
            return packed;
        }

        private void applyBlendMode() {
            if (blendMode == BlendMode.OPAQUE) {
                GlStateManager.disableBlend();
                return;
            }
            GlStateManager.enableBlend();
            if (blendMode == BlendMode.ADD) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
        }

        private void resetBlendMode() {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
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
                    spawnEffectAt(effect, this.x, this.y, this.z, false);
                }
                return;
            }
            spawnEffectAt(effect, this.x, this.y, this.z, false);
        }

        @Override
        public void soundEffect(String sound) {
            playSoundAt(sound, this.x, this.y, this.z);
        }

        @Override
        public void expression(MolangExpression expression) {
            if (expression != null) {
                environment.safeResolve(expression);
            }
        }

        @Override
        public void log(String message) {
            logMessage(message);
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
                float value = evaluateCurve(environment, entry.getValue());
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

        private void onExpired() {
            if (lifetimeEvents != null) {
                fireEvents(lifetimeEvents.expirationEvent());
            }
            if (emitter != null) {
                emitter.onParticleExpired();
            }
        }

        private void syncPrev() {
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.prevRoll = this.roll;
        }

        private void setPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private void setVelocity(double vx, double vy, double vz) {
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

    private final class ActiveEmitter implements ParticleContext {
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

        private ActiveEmitter(ParticleData data, EmitterTransformProvider provider, int overrideCount) {
            this.data = data;
            this.transformProvider = provider != null ? provider : new StaticTransformProvider(0.0, 0.0, 0.0, 0.0f);
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
            this.curves = buildCurveDefinitions(data);
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
            this.environment = createRuntime(this.molangContext, this.curves);

            this.billboard = getComponent(data, "particle_appearance_billboard");
            this.tint = getComponent(data, "particle_appearance_tinting");
            this.speed = getComponent(data, "particle_initial_speed");
            this.emitterShape = resolveEmitterShape();
            this.particleLifetimeComponent = getComponent(data, "particle_lifetime_expression");
            this.localSpace = getComponent(data, "emitter_local_space");
            this.rateInstant = getComponent(data, "emitter_rate_instant");
            this.rateSteady = getComponent(data, "emitter_rate_steady");
            this.lifetimeOnce = getComponent(data, "emitter_lifetime_once");
            this.lifetimeLooping = getComponent(data, "emitter_lifetime_looping");
            this.lifetimeExpression = getComponent(data, "emitter_lifetime_expression");
            this.lifetimeEvents = getComponent(data, "emitter_lifetime_events");
            this.emitterInitialization = getComponent(data, "emitter_initialization");
            this.texture = toMinecraft(data);
            this.age = 0.0f;
            this.lifetime = Float.MAX_VALUE;
            this.instantEmitted = false;
            this.expired = false;
            this.activeParticles = 0;
            this.spawnedAny = false;
            this.spawner = new EmitterShapeSpawner();
            this.eventRandom = new Random();
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

        private void emitInitialParticles(int room) {
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

        private boolean tick() {
            if (expired) {
                return activeParticles > 0;
            }
            updateTransform(TICK_SECONDS);
            updateContext(age);
            if (emitterInitialization != null && emitterInitialization.tickExpression() != null) {
                environment.safeResolve(emitterInitialization.tickExpression());
            }
            tickLifetimeEvents();
            applyLifetimeLogic();
            updateContext(age);
            if (!expired && isActive()) {
                if (rateInstant != null) {
                    if (!instantEmitted) {
                        emitParticles(resolveInstantCount(), getRemainingParticleRoom());
                        instantEmitted = true;
                    }
                } else if (rateSteady != null) {
                    emitSteady();
                }
            }
            age += TICK_SECONDS;
            updateContext(age);
            return !expired || activeParticles > 0;
        }

        private void render(float partialTicks) {
            if (emitterInitialization == null || emitterInitialization.renderExpression() == null) {
                return;
            }
            float renderAge = this.age + partialTicks * TICK_SECONDS;
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
            int room = getRemainingParticleRoom();
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
                    addParticle(particle);
                    activeParticles++;
                    spawnedAny = true;
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
                        sleepRemaining -= TICK_SECONDS;
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
                float value = evaluateCurve(environment, entry.getValue());
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
            float deltaYawDeg = wrapDegrees(yaw - lastYaw);
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

        private void onParticleExpired() {
            if (activeParticles > 0) {
                activeParticles--;
            }
        }

        private boolean hasSpawnedParticles() {
            return spawnedAny;
        }

        private boolean isAlive() {
            return !expired || activeParticles > 0;
        }

        private float getEmitterAge() {
            return molangContext.emitterAge;
        }

        private float getEmitterLifetime() {
            return molangContext.emitterLifetime;
        }

        private float getEmitterRandom1() {
            return molangContext.emitterRandom1;
        }

        private float getEmitterRandom2() {
            return molangContext.emitterRandom2;
        }

        private float getEmitterRandom3() {
            return molangContext.emitterRandom3;
        }

        private float getEmitterRandom4() {
            return molangContext.emitterRandom4;
        }

        private float getEmitterRandom(int index) {
            return molangContext.getEmitterRandom(index);
        }

        @Override
        public void particleEffect(String effect, ParticleEvent.ParticleSpawnType type) {
            if (effect == null || effect.isEmpty()) {
                return;
            }
            if (type == ParticleEvent.ParticleSpawnType.EMITTER_BOUND) {
                spawnEffectFromEmitter(effect, this, true);
                return;
            }
            spawnEffectFromEmitter(effect, this, false);
        }

        @Override
        public void soundEffect(String sound) {
            playSoundAt(sound, this.x, this.y, this.z);
        }

        @Override
        public void expression(MolangExpression expression) {
            if (expression != null) {
                environment.safeResolve(expression);
            }
        }

        @Override
        public void log(String message) {
            logMessage(message);
        }

        @Override
        public Random getRandom() {
            return eventRandom;
        }

        private boolean isLocalPosition() {
            return localSpace != null && localSpace.position();
        }

        private boolean isLocalRotation() {
            return localSpace != null && localSpace.rotation();
        }

        private boolean isLocalVelocity() {
            return (localSpace != null && localSpace.velocity()) || locatorBound;
        }

        private double getDeltaX() {
            return deltaX;
        }

        private double getDeltaY() {
            return deltaY;
        }

        private double getDeltaZ() {
            return deltaZ;
        }

        private float getDeltaYawRad() {
            return deltaYawRad;
        }

        private double getX() {
            return x;
        }

        private double getY() {
            return y;
        }

        private double getZ() {
            return z;
        }

        private float getYaw() {
            return yaw;
        }

        private float[] getBasisX() {
            return basisX;
        }

        private float[] getBasisY() {
            return basisY;
        }

        private float[] getBasisZ() {
            return basisZ;
        }

        private float getScale() {
            return scale;
        }

        private double rotateLocalX(double x, double y, double z) {
            return basisX[0] * x + basisY[0] * y + basisZ[0] * z;
        }

        private double rotateLocalY(double x, double y, double z) {
            return basisX[1] * x + basisY[1] * y + basisZ[1] * z;
        }

        private double rotateLocalZ(double x, double y, double z) {
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

        private void applyDeltaRotation(ActiveParticle particle) {
            double ox = particle.x - x;
            double oy = particle.y - y;
            double oz = particle.z - z;
            double lx = rotateFromLastX(ox, oy, oz);
            double ly = rotateFromLastY(ox, oy, oz);
            double lz = rotateFromLastZ(ox, oy, oz);
            double rx = rotateLocalX(lx, ly, lz);
            double ry = rotateLocalY(lx, ly, lz);
            double rz = rotateLocalZ(lx, ly, lz);
            particle.x = x + rx;
            particle.y = y + ry;
            particle.z = z + rz;
        }

        private void applyDeltaRotationToVelocity(ActiveParticle particle) {
            double lx = rotateFromLastX(particle.vx, particle.vy, particle.vz);
            double ly = rotateFromLastY(particle.vx, particle.vy, particle.vz);
            double lz = rotateFromLastZ(particle.vx, particle.vy, particle.vz);
            double rx = rotateLocalX(lx, ly, lz);
            double ry = rotateLocalY(lx, ly, lz);
            double rz = rotateLocalZ(lx, ly, lz);
            particle.setVelocity(rx, ry, rz);
        }

        private ActiveParticle createParticle(double px, double py, double pz) {
            return new ActiveParticle(
                data,
                this,
                px, py, pz,
                billboard,
                tint,
                speed,
                texture,
                particleLifetimeComponent
            );
        }

        private ParticleEmitterShape resolveEmitterShape() {
            ParticleEmitterShape shape = getComponent(data, "emitter_shape_disc");
            if (shape != null) {
                return shape;
            }
            shape = getComponent(data, "emitter_shape_box");
            if (shape != null) {
                return shape;
            }
            shape = getComponent(data, "emitter_shape_sphere");
            if (shape != null) {
                return shape;
            }
            shape = getComponent(data, "emitter_shape_point");
            if (shape != null) {
                return shape;
            }
            shape = getComponent(data, "emitter_shape_entity_aabb");
            if (shape != null) {
                return shape;
            }
            shape = getComponent(data, "emitter_shape_custom");
            if (shape != null) {
                return shape;
            }
            if (data != null && data.components() != null) {
                for (gg.moonflower.pinwheel.particle.component.ParticleComponent component : data.components().values()) {
                    if (component instanceof ParticleEmitterShape) {
                        return (ParticleEmitterShape) component;
                    }
                }
            }
            return ParticleEmitterShape.EMPTY;
        }

        private final class EmitterShapeSpawner implements ParticleEmitterShape.Spawner {
            @Override
            public ParticleInstance createParticle() {
                return ActiveEmitter.this.createParticle(x, y, z);
            }

            @Override
            public void spawnParticle(ParticleInstance instance) {
                if (!(instance instanceof ActiveParticle)) {
                    return;
                }
                ActiveParticle particle = (ActiveParticle) instance;
                particle.applyInitialSpeed();
                particle.syncPrev();
                addParticle(particle);
                activeParticles++;
                spawnedAny = true;
            }

            @Override
            public gg.moonflower.pinwheel.particle.ParticleSourceObject getEntity() {
                return null;
            }

            @Override
            public MolangEnvironment getEnvironment() {
                return environment;
            }

            @Override
            public Random getRandom() {
                return random;
            }

            @Override
            public void setPosition(ParticleInstance instance, double x, double y, double z) {
                if (!(instance instanceof ActiveParticle)) {
                    return;
                }
                ActiveParticle particle = (ActiveParticle) instance;
                double rx = rotateLocalX(x, y, z);
                double ry = rotateLocalY(x, y, z);
                double rz = rotateLocalZ(x, y, z);
                particle.setPosition(ActiveEmitter.this.x + rx, ActiveEmitter.this.y + ry, ActiveEmitter.this.z + rz);
            }

            @Override
            public void setVelocity(ParticleInstance instance, double dx, double dy, double dz) {
                if (!(instance instanceof ActiveParticle)) {
                    return;
                }
                boolean localVel = ActiveEmitter.this.isLocalVelocity();
                double rx = localVel ? rotateLocalX(dx, dy, dz) : dx;
                double ry = localVel ? rotateLocalY(dx, dy, dz) : dy;
                double rz = localVel ? rotateLocalZ(dx, dy, dz) : dz;
                double vx = rx + ActiveEmitter.this.deltaX;
                double vy = ry + ActiveEmitter.this.deltaY;
                double vz = rz + ActiveEmitter.this.deltaZ;
                ActiveParticle particle = (ActiveParticle) instance;
                particle.setVelocity(vx, vy, vz);
            }
        }
    }

    private static final class ParticleMolangContext {
        private float particleAge;
        private float particleLifetime;
        private float emitterAge;
        private float emitterLifetime;
        private float random;
        private float random1;
        private float random2;
        private float random3;
        private float random4;
        private final float[] randomExtra = new float[12];
        private float emitterRandom1;
        private float emitterRandom2;
        private float emitterRandom3;
        private float emitterRandom4;
        private final float[] emitterRandomExtra = new float[12];
        private float entityScale;
        private Map<String, Float> curves;

        private float getCurveValue(String name) {
            if (curves == null) {
                return 0.0f;
            }
            Float value = curves.get(name);
            return value != null ? value : 0.0f;
        }

        private float getRandom(int index) {
            switch (index) {
                case 1:
                    return random1;
                case 2:
                    return random2;
                case 3:
                    return random3;
                case 4:
                    return random4;
                default:
                    if (index >= 5 && index <= 16) {
                        return randomExtra[index - 5];
                    }
                    return 0.0f;
            }
        }

        private void setRandom(int index, float value) {
            switch (index) {
                case 1:
                    random1 = value;
                    return;
                case 2:
                    random2 = value;
                    return;
                case 3:
                    random3 = value;
                    return;
                case 4:
                    random4 = value;
                    return;
                default:
                    if (index >= 5 && index <= 16) {
                        randomExtra[index - 5] = value;
                    }
            }
        }

        private float getEmitterRandom(int index) {
            switch (index) {
                case 1:
                    return emitterRandom1;
                case 2:
                    return emitterRandom2;
                case 3:
                    return emitterRandom3;
                case 4:
                    return emitterRandom4;
                default:
                    if (index >= 5 && index <= 16) {
                        return emitterRandomExtra[index - 5];
                    }
                    return 0.0f;
            }
        }

        private void setEmitterRandom(int index, float value) {
            switch (index) {
                case 1:
                    emitterRandom1 = value;
                    return;
                case 2:
                    emitterRandom2 = value;
                    return;
                case 3:
                    emitterRandom3 = value;
                    return;
                case 4:
                    emitterRandom4 = value;
                    return;
                default:
                    if (index >= 5 && index <= 16) {
                        emitterRandomExtra[index - 5] = value;
                    }
            }
        }
    }

    private static MolangRuntime createRuntime(ParticleMolangContext context, Map<String, ParticleData.Curve> curves) {
        MolangRuntime.Builder builder = MolangRuntime.runtime();
        bindCommonVariables(builder, context);
        bindCurves(builder, context, curves);
        return builder.create();
    }

    private static void bindCommonVariables(MolangEnvironmentBuilder<?> builder, ParticleMolangContext context) {
        builder.setVariable("particle_age", MolangExpression.of(() -> context.particleAge));
        builder.setVariable("particle_lifetime", MolangExpression.of(() -> context.particleLifetime));
        builder.setVariable("emitter_age", MolangExpression.of(() -> context.emitterAge));
        builder.setVariable("emitter_lifetime", MolangExpression.of(() -> context.emitterLifetime));
        builder.setVariable("random", MolangExpression.of(() -> context.random));
        builder.setVariable("entity_scale", MolangExpression.of(() -> context.entityScale));

        builder.setQuery("particle_age", MolangExpression.of(() -> context.particleAge));
        builder.setQuery("particle_lifetime", MolangExpression.of(() -> context.particleLifetime));
        builder.setQuery("emitter_age", MolangExpression.of(() -> context.emitterAge));
        builder.setQuery("emitter_lifetime", MolangExpression.of(() -> context.emitterLifetime));
        builder.setQuery("random", MolangExpression.of(() -> context.random));
        builder.setQuery("entity_scale", MolangExpression.of(() -> context.entityScale));
        builder.setQuery("age", MolangExpression.of(() -> context.particleAge));
        builder.setQuery("life_time", MolangExpression.of(() -> context.particleLifetime));

        for (int i = 1; i <= 16; i++) {
            final int index = i;
            builder.setVariable("random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setVariable("particle_random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setVariable("emitter_random_" + i, MolangExpression.of(() -> context.getEmitterRandom(index)));

            builder.setQuery("random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setQuery("particle_random_" + i, MolangExpression.of(() -> context.getRandom(index)));
            builder.setQuery("emitter_random_" + i, MolangExpression.of(() -> context.getEmitterRandom(index)));
        }
    }

    private static void bindCurves(MolangEnvironmentBuilder<?> builder, ParticleMolangContext context, Map<String, ParticleData.Curve> curves) {
        if (curves == null || curves.isEmpty()) {
            return;
        }
        for (String name : curves.keySet()) {
            final String key = name;
            builder.setVariable(key, MolangExpression.of(() -> context.getCurveValue(key)));
        }
    }

    private static Map<String, ParticleData.Curve> buildCurveDefinitions(ParticleData data) {
        if (data == null || data.curves() == null || data.curves().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ParticleData.Curve> curves = new LinkedHashMap<>();
        for (Map.Entry<String, ParticleData.Curve> entry : data.curves().entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String name = key;
            int dot = key.indexOf('.');
            if (dot >= 0 && dot + 1 < key.length()) {
                name = key.substring(dot + 1);
            }
            curves.put(name, entry.getValue());
        }
        return curves;
    }

    private static float evaluateCurve(MolangEnvironment environment, ParticleData.Curve curve) {
        if (curve == null) {
            return 0.0f;
        }
        ParticleData.CurveNode[] nodes = curve.nodes();
        if (nodes == null || nodes.length == 0) {
            return 0.0f;
        }
        float horizontalRange = environment.safeResolve(curve.horizontalRange());
        if (horizontalRange == 0.0f) {
            return 1.0f;
        }
        float input = environment.safeResolve(curve.input()) / horizontalRange;
        int index = getCurveIndex(curve, input);
        if (index < 0) {
            return input;
        }
        switch (curve.type()) {
            case LINEAR:
                return evalLinear(environment, nodes, index, input);
            case BEZIER:
                return evalBezier(environment, nodes, input);
            case BEZIER_CHAIN:
                return evalBezierChain(environment, nodes, index, input);
            case CATMULL_ROM:
                return evalCatmullRom(environment, nodes, index, input);
            default:
                return input;
        }
    }

    private static int getCurveIndex(ParticleData.Curve curve, float input) {
        ParticleData.CurveNode[] nodes = curve.nodes();
        int offset = curve.type() == ParticleData.CurveType.CATMULL_ROM ? 1 : 0;
        int best = offset;
        for (int i = offset; i < nodes.length - offset * 2; i++) {
            if (nodes[i].getTime() > input) {
                break;
            }
            best = i;
        }
        return best;
    }

    private static float evalLinear(MolangEnvironment environment, ParticleData.CurveNode[] nodes, int index, float input) {
        if (nodes.length == 1) {
            return environment.safeResolve(nodes[0].getValue());
        }
        ParticleData.CurveNode current = nodes[index];
        ParticleData.CurveNode next = index + 1 >= nodes.length ? current : nodes[index + 1];
        float a = environment.safeResolve(current.getValue());
        float b = environment.safeResolve(next.getValue());
        float denom = (next.getTime() - current.getTime());
        float progress = denom == 0.0f ? 0.0f : (input - current.getTime()) / denom;
        return lerp(progress, a, b);
    }

    private static float evalBezier(MolangEnvironment environment, ParticleData.CurveNode[] nodes, float input) {
        if (nodes.length < 4) {
            return input;
        }
        float a = environment.safeResolve(nodes[0].getValue());
        float b = environment.safeResolve(nodes[1].getValue());
        float c = environment.safeResolve(nodes[2].getValue());
        float d = environment.safeResolve(nodes[3].getValue());
        return bezier(a, b, c, d, input);
    }

    private static float evalBezierChain(MolangEnvironment environment, ParticleData.CurveNode[] nodes, int index, float input) {
        if (!(nodes[index] instanceof ParticleData.BezierChainCurveNode)) {
            return input;
        }
        ParticleData.BezierChainCurveNode current = (ParticleData.BezierChainCurveNode) nodes[index];
        if (index + 1 >= nodes.length || !(nodes[index + 1] instanceof ParticleData.BezierChainCurveNode)) {
            return environment.safeResolve(current.getRightValue());
        }
        ParticleData.BezierChainCurveNode next = (ParticleData.BezierChainCurveNode) nodes[index + 1];
        float step = (next.getTime() - current.getTime()) / 3.0f;
        float a = environment.safeResolve(current.getRightValue());
        float b = a + step * environment.safeResolve(current.getRightSlope());
        float d = environment.safeResolve(next.getLeftValue());
        float c = d - step * environment.safeResolve(next.getLeftSlope());
        float denom = (next.getTime() - current.getTime());
        float progress = denom == 0.0f ? 0.0f : (input - current.getTime()) / denom;
        return bezier(a, b, c, d, progress);
    }

    private static float evalCatmullRom(MolangEnvironment environment, ParticleData.CurveNode[] nodes, int index, float input) {
        if (nodes.length < 4 || index - 1 < 0 || index + 2 >= nodes.length) {
            return input;
        }
        ParticleData.CurveNode last = nodes[index - 1];
        ParticleData.CurveNode from = nodes[index];
        ParticleData.CurveNode to = nodes[index + 1];
        ParticleData.CurveNode after = nodes[index + 2];
        float a = environment.safeResolve(last.getValue());
        float b = environment.safeResolve(from.getValue());
        float c = environment.safeResolve(to.getValue());
        float d = environment.safeResolve(after.getValue());
        float denom = (to.getTime() - from.getTime());
        float progress = denom == 0.0f ? 0.0f : (input - from.getTime()) / denom;
        return catmullRom(a, b, c, d, clamp01(progress));
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    private static float bezier(float p0, float p1, float p2, float p3, float t) {
        float inv = 1.0f - t;
        return inv * inv * inv * p0 + 3 * inv * inv * t * p1 + 3 * inv * t * t * p2 + t * t * t * p3;
    }

    private static float catmullRom(float p0, float p1, float p2, float p3, float t) {
        return 0.5f * ((2 * p1)
            + (-p0 + p2) * t
            + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
            + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static float wrapDegrees(float degrees) {
        float result = degrees % 360.0f;
        if (result >= 180.0f) {
            result -= 360.0f;
        }
        if (result < -180.0f) {
            result += 360.0f;
        }
        return result;
    }

    private static void normalize(float[] vec) {
        float len = (float) Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
        if (len > 0.0f) {
            vec[0] /= len;
            vec[1] /= len;
            vec[2] /= len;
        }
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static void cross(float[] a, float[] b, float[] out) {
        out[0] = a[1] * b[2] - a[2] * b[1];
        out[1] = a[2] * b[0] - a[0] * b[2];
        out[2] = a[0] * b[1] - a[1] * b[0];
    }

    private static void multMatrix(float[] xAxis, float[] yAxis, float[] zAxis) {
        ORIENTATION_BUFFER.clear();
        ORIENTATION_BUFFER.put(xAxis[0]).put(xAxis[1]).put(xAxis[2]).put(0.0f);
        ORIENTATION_BUFFER.put(yAxis[0]).put(yAxis[1]).put(yAxis[2]).put(0.0f);
        ORIENTATION_BUFFER.put(zAxis[0]).put(zAxis[1]).put(zAxis[2]).put(0.0f);
        ORIENTATION_BUFFER.put(0.0f).put(0.0f).put(0.0f).put(1.0f);
        ORIENTATION_BUFFER.flip();
        GL11.glMultMatrix(ORIENTATION_BUFFER);
    }

    private enum BlendMode {
        ALPHA,
        ADD,
        OPAQUE
    }

    private BlendMode resolveBlendMode(ParticleData data) {
        if (data == null || data.description() == null) {
            return BlendMode.ALPHA;
        }
        String material = data.description().getMaterial();
        if (material == null || material.isEmpty()) {
            return BlendMode.ALPHA;
        }
        String lower = material.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("add")) {
            return BlendMode.ADD;
        }
        if (lower.contains("opaque")) {
            return BlendMode.OPAQUE;
        }
        return BlendMode.ALPHA;
    }

    private Block[] resolveBlocks(ParticleExpireInBlocksComponent component) {
        if (component == null) {
            return null;
        }
        return resolveBlocks(component.blocks());
    }

    private Block[] resolveBlocks(ParticleExpireNotInBlocksComponent component) {
        if (component == null) {
            return null;
        }
        return resolveBlocks(component.blocks());
    }

    private Block[] resolveBlocks(String[] blockIds) {
        if (blockIds == null || blockIds.length == 0) {
            return null;
        }
        List<Block> blocks = new ArrayList<>(blockIds.length);
        for (String name : blockIds) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            try {
                ResourceLocation id = new ResourceLocation(name);
                Block block = Block.REGISTRY.getObject(id);
                if (block != null) {
                    blocks.add(block);
                } else {
                    SkyCoreMod.LOGGER.warn("[SkyCore] 未找到方块: {}", name);
                }
            } catch (Exception ex) {
                SkyCoreMod.LOGGER.warn("[SkyCore] 方块ID无效: {}", name);
            }
        }
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.toArray(new Block[0]);
    }

    private void spawnEffectAt(String effect, double x, double y, double z, boolean bound) {
        String path = normalizeParticlePath(effect);
        if (path == null) {
            return;
        }
        EmitterTransformProvider provider = new SnapshotTransformProvider(x, y, z, 0.0f, null, null, null, 1.0f);
        spawnInternal(path, provider, 0);
    }

    private void spawnEffectFromEmitter(String effect, ActiveEmitter emitter, boolean bound) {
        if (emitter == null) {
            return;
        }
        String path = normalizeParticlePath(effect);
        if (path == null) {
            return;
        }
        EmitterTransformProvider provider;
        if (bound) {
            provider = new BoundTransformProvider(emitter);
        } else {
            provider = new SnapshotTransformProvider(
                emitter.getX(), emitter.getY(), emitter.getZ(), emitter.getYaw(),
                emitter.getBasisX(), emitter.getBasisY(), emitter.getBasisZ(), emitter.getScale());
        }
        spawnInternal(path, provider, 0);
    }

    private String normalizeParticlePath(String effect) {
        if (effect == null) {
            return null;
        }
        String trimmed = effect.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int colon = trimmed.indexOf(':');
        String namespace = null;
        String rel = trimmed;
        if (colon > 0) {
            namespace = trimmed.substring(0, colon);
            rel = trimmed.substring(colon + 1);
        }
        if (!rel.startsWith("particles/")) {
            rel = "particles/" + rel;
        }
        if (!rel.endsWith(".json")) {
            rel = rel + ".json";
        }
        return namespace != null ? namespace + ":" + rel : rel;
    }

    private void playSoundAt(String sound, double x, double y, double z) {
        if (sound == null || sound.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.world == null) {
            return;
        }
        ResourceLocation id;
        try {
            id = new ResourceLocation(sound);
        } catch (Exception ex) {
            SkyCoreMod.LOGGER.warn("[SkyCore] 无效的声音ID: {}", sound);
            return;
        }
        SoundEvent event = SoundEvent.REGISTRY.getObject(id);
        if (event == null) {
            event = new SoundEvent(id);
        }
        mc.world.playSound(x, y, z, event, SoundCategory.NEUTRAL, 1.0f, 1.0f, false);
    }

    private void logMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.ingameGUI != null) {
            mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString("[Particle] " + message));
            return;
        }
        SkyCoreMod.LOGGER.info("[Particle] {}", message);
    }

    public interface EmitterTransformProvider {
        void fill(EmitterTransform transform, float deltaSeconds);

        default boolean isLocatorBound() {
            return false;
        }
    }

    public static final class EmitterTransform {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float scale = 1.0f;
        public final float[] basisX = new float[]{1.0f, 0.0f, 0.0f};
        public final float[] basisY = new float[]{0.0f, 1.0f, 0.0f};
        public final float[] basisZ = new float[]{0.0f, 0.0f, 1.0f};
    }

    public static final class StaticTransformProvider implements EmitterTransformProvider {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;

        private StaticTransformProvider(double x, double y, double z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }

        @Override
        public void fill(EmitterTransform transform, float deltaSeconds) {
            transform.x = x;
            transform.y = y;
            transform.z = z;
            transform.yaw = yaw;
            transform.scale = 1.0f;
            setIdentityBasis(transform);
        }
    }

    private static final class SnapshotTransformProvider implements EmitterTransformProvider {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float[] basisX;
        private final float[] basisY;
        private final float[] basisZ;
        private final float scale;

        private SnapshotTransformProvider(double x, double y, double z, float yaw,
                                          float[] basisX, float[] basisY, float[] basisZ, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.scale = scale;
            this.basisX = new float[]{1.0f, 0.0f, 0.0f};
            this.basisY = new float[]{0.0f, 1.0f, 0.0f};
            this.basisZ = new float[]{0.0f, 0.0f, 1.0f};
            if (basisX != null && basisY != null && basisZ != null) {
                copyBasis(basisX, this.basisX);
                copyBasis(basisY, this.basisY);
                copyBasis(basisZ, this.basisZ);
            }
        }

        @Override
        public void fill(EmitterTransform transform, float deltaSeconds) {
            transform.x = x;
            transform.y = y;
            transform.z = z;
            transform.yaw = yaw;
            transform.scale = scale;
            copyBasis(basisX, transform.basisX);
            copyBasis(basisY, transform.basisY);
            copyBasis(basisZ, transform.basisZ);
        }
    }

    private final class BoundTransformProvider implements EmitterTransformProvider {
        private final ActiveEmitter emitter;

        private BoundTransformProvider(ActiveEmitter emitter) {
            this.emitter = emitter;
        }

        @Override
        public void fill(EmitterTransform transform, float deltaSeconds) {
            transform.x = emitter.getX();
            transform.y = emitter.getY();
            transform.z = emitter.getZ();
            transform.yaw = emitter.getYaw();
            transform.scale = emitter.getScale();
            copyBasis(emitter.getBasisX(), transform.basisX);
            copyBasis(emitter.getBasisY(), transform.basisY);
            copyBasis(emitter.getBasisZ(), transform.basisZ);
        }
    }

    private static void setIdentityBasis(EmitterTransform transform) {
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

    private static void copyBasis(float[] src, float[] dst) {
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
    }
}
