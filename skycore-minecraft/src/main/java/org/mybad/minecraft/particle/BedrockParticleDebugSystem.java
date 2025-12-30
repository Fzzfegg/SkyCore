package org.mybad.minecraft.particle;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.BindingMolangEnvironment;
import gg.moonflower.pinwheel.particle.ParticleData;
import gg.moonflower.pinwheel.particle.ParticleInstance;
import gg.moonflower.pinwheel.particle.component.EmitterRateInstantComponent;
import gg.moonflower.pinwheel.particle.component.EmitterRateSteadyComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLocalSpaceComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLifetimeOnceComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLifetimeLoopingComponent;
import gg.moonflower.pinwheel.particle.component.EmitterLifetimeExpressionComponent;
import gg.moonflower.pinwheel.particle.component.ParticleEmitterShape;
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceBillboardComponent;
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceTintingComponent;
import gg.moonflower.pinwheel.particle.component.ParticleInitialSpinComponent;
import gg.moonflower.pinwheel.particle.component.ParticleInitialSpeedComponent;
import gg.moonflower.pinwheel.particle.component.ParticleLifetimeExpressionComponent;
import gg.moonflower.pinwheel.particle.component.ParticleMotionDynamicComponent;
import gg.moonflower.pinwheel.particle.component.ParticleMotionParametricComponent;
import gg.moonflower.pinwheel.particle.component.ParticleMotionCollisionComponent;
import gg.moonflower.pollen.particle.render.QuadRenderProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.resource.ResourceLoader;
import org.mybad.core.legacy.expression.molang.reference.ExpressionBindingContext;
import org.mybad.core.legacy.expression.molang.reference.ReferenceType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
    private final Random random;

    public BedrockParticleDebugSystem(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.particles = new ArrayList<>();
        this.emitters = new ArrayList<>();
        this.random = new Random();
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
        int room = MAX_DEBUG_PARTICLES - particles.size();
        if (room <= 0) {
            return false;
        }
        ActiveEmitter emitter = new ActiveEmitter(data, provider, overrideCount);
        emitter.emitInitialParticles(room);
        if (emitter.isAlive()) {
            emitters.add(emitter);
        }
        return emitter.hasSpawnedParticles() || emitter.isAlive();
    }

    public void clear() {
        particles.clear();
        emitters.clear();
    }

    public int getActiveCount() {
        return particles.size();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().world == null) {
            particles.clear();
            return;
        }
        if (particles.isEmpty()) {
            // continue to tick emitters even if no particles yet
        }
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

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();

        for (ActiveParticle particle : particles) {
            particle.render(mc, camX, camY, camZ, partialTicks);
        }

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

    private static final class ActiveParticle implements ParticleInstance {
        private final Map<String, ParticleData.Curve> curves;
        private final Map<String, Float> curveValues;
        private final QuadRenderProperties renderProps;
        private final ParticleAppearanceBillboardComponent billboard;
        private final ParticleAppearanceTintingComponent tint;
        private final ParticleInitialSpeedComponent speed;
        private final ParticleInitialSpinComponent initialSpin;
        private final ParticleMotionDynamicComponent motionDynamic;
        private final ParticleMotionParametricComponent motionParametric;
        private final ParticleMotionCollisionComponent motionCollision;
        private final ResourceLocation texture;
        private final float lifetime;
        private final ParticleMolangContext molangContext;
        private final ParticleMolangEnvironment environment;
        private final ActiveEmitter emitter;
        private final boolean localPosition;
        private final boolean localRotation;
        private final boolean localVelocity;

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
            this.x = x;
            this.y = y;
            this.z = z;
            this.prevX = x;
            this.prevY = y;
            this.prevZ = z;
            this.billboard = billboard;
            this.tint = tint;
            this.speed = speed;
            this.initialSpin = getComponent(data, "particle_initial_spin");
            this.motionDynamic = getComponent(data, "particle_motion_dynamic");
            this.motionParametric = getComponent(data, "particle_motion_parametric");
            this.motionCollision = getComponent(data, "particle_motion_collision");
            this.texture = texture;
            this.renderProps = new QuadRenderProperties();
            this.age = 0.0f;
            this.emitter = emitter;
            this.localPosition = emitter != null && emitter.isLocalPosition();
            this.localRotation = emitter != null && emitter.isLocalRotation();
            this.localVelocity = emitter != null && emitter.isLocalVelocity();
            this.molangContext = new ParticleMolangContext();
            this.curves = buildCurveDefinitions(data);
            this.curveValues = this.curves.isEmpty() ? Collections.emptyMap() : new HashMap<>(this.curves.size());
            this.molangContext.curves = this.curveValues;
            this.molangContext.random = (float) Math.random();
            this.molangContext.random1 = (float) Math.random();
            this.molangContext.random2 = (float) Math.random();
            this.molangContext.random3 = (float) Math.random();
            this.molangContext.random4 = (float) Math.random();
            this.molangContext.entityScale = 1.0f;
            ExpressionBindingContext bindings = createBindingContext(this.curves);
            this.environment = new ParticleMolangEnvironment(molangContext, bindings);
            this.lifetime = resolveLifetime(lifetimeComponent);
            this.molangContext.particleLifetime = this.lifetime;
            this.tempDir = new float[3];
            this.tempAxisX = new float[3];
            this.tempAxisY = new float[3];
            this.tempAxisZ = new float[3];
            updateContext(0.0f);
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
            if (motionCollision != null && isCollisionEnabled()) {
                boolean collideX = isColliding(nextX, this.y, this.z, collisionRadius);
                if (collideX) {
                    this.vx = -this.vx * collisionRestitution;
                    nextX = this.x;
                }
                boolean collideY = isColliding(this.x, nextY, this.z, collisionRadius);
                if (collideY) {
                    this.vy = -this.vy * collisionRestitution;
                    nextY = this.y;
                }
                boolean collideZ = isColliding(this.x, this.y, nextZ, collisionRadius);
                if (collideZ) {
                    this.vz = -this.vz * collisionRestitution;
                    nextZ = this.z;
                }
                if (collideX || collideY || collideZ) {
                    if (collisionDrag > 0.0f) {
                        float drag = Math.max(0.0f, 1.0f - collisionDrag);
                        this.vx *= drag;
                        this.vy *= drag;
                        this.vz *= drag;
                    }
                    if (expireOnContact) {
                        return false;
                    }
                }
            }
            this.x = nextX;
            this.y = nextY;
            this.z = nextZ;
            this.age += TICK_SECONDS;
            updateContext(this.age);
            return this.age < this.lifetime;
        }

        private void render(Minecraft mc, double camX, double camY, double camZ, float partialTicks) {
            float renderAge = this.age + partialTicks * TICK_SECONDS;
            updateContext(renderAge);
            float width = 1.0f;
            float height = 1.0f;
            if (billboard != null && billboard.size() != null && billboard.size().length >= 2) {
                width = environment.safeResolve(billboard.size()[0]);
                height = environment.safeResolve(billboard.size()[1]);
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
            if (tint != null) {
                float r = clamp01(tint.red().get(this, environment));
                float g = clamp01(tint.green().get(this, environment));
                float b = clamp01(tint.blue().get(this, environment));
                float a = clamp01(tint.alpha().get(this, environment));
                renderProps.setColor(r, g, b, a);
            } else {
                renderProps.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }

            double px = this.prevX + (this.x - this.prevX) * partialTicks;
            double py = this.prevY + (this.y - this.prevY) * partialTicks;
            double pz = this.prevZ + (this.z - this.prevZ) * partialTicks;

            GlStateManager.pushMatrix();
            GlStateManager.translate(px - camX, py - camY, pz - camZ);
            float yaw = mc.getRenderManager().playerViewY;
            float pitch = mc.getRenderManager().playerViewX;
            boolean oriented = false;
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
                        oriented = applyDirectionFacing(billboard.cameraMode());
                        break;
                    default:
                        break;
                }
            }
            if (!oriented) {
                GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            }
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

            GlStateManager.popMatrix();
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

        private boolean applyDirectionFacing(ParticleAppearanceBillboardComponent.FaceCameraMode mode) {
            if (!resolveFacingDirection(tempDir)) {
                return false;
            }
            switch (mode) {
                case DIRECTION_X:
                    return applyDirectionAsAxis(tempDir, 0);
                case DIRECTION_Y:
                    return applyDirectionAsAxis(tempDir, 1);
                case DIRECTION_Z:
                case LOOKAT_DIRECTION:
                default:
                    return applyDirectionAsNormal(tempDir);
            }
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
                tempAxisY[0] = ez[0];
                tempAxisY[1] = ez[1];
                tempAxisY[2] = ez[2];
                cross(tempAxisX, tempAxisY, tempAxisZ);
            } else if (mode == ParticleAppearanceBillboardComponent.FaceCameraMode.EMITTER_TRANSFORM_YZ) {
                tempAxisX[0] = ey[0];
                tempAxisX[1] = ey[1];
                tempAxisX[2] = ey[2];
                tempAxisY[0] = ez[0];
                tempAxisY[1] = ez[1];
                tempAxisY[2] = ez[2];
                cross(tempAxisX, tempAxisY, tempAxisZ);
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

        private static int toColor(float value) {
            return Math.min(255, Math.max(0, (int) (value * 255.0f)));
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

        private void updateContext(float ageSeconds) {
            this.molangContext.particleAge = ageSeconds;
            if (emitter != null) {
                this.molangContext.emitterAge = emitter.getEmitterAge();
                this.molangContext.emitterLifetime = emitter.getEmitterLifetime();
                this.molangContext.emitterRandom1 = emitter.getEmitterRandom1();
                this.molangContext.emitterRandom2 = emitter.getEmitterRandom2();
                this.molangContext.emitterRandom3 = emitter.getEmitterRandom3();
                this.molangContext.emitterRandom4 = emitter.getEmitterRandom4();
            } else {
                this.molangContext.emitterAge = ageSeconds;
                this.molangContext.emitterLifetime = this.lifetime;
                this.molangContext.emitterRandom1 = this.molangContext.random1;
                this.molangContext.emitterRandom2 = this.molangContext.random2;
                this.molangContext.emitterRandom3 = this.molangContext.random3;
                this.molangContext.emitterRandom4 = this.molangContext.random4;
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
            double drag = environment.safeResolve(motionDynamic.linearDragCoefficient()) / 400.0;
            this.ax = ax - drag * this.vx;
            this.ay = ay - drag * this.vy;
            this.az = az - drag * this.vz;
            float rotAcc = (float) (environment.safeResolve(motionDynamic.rotationAcceleration()) / 400.0);
            float rotDrag = (float) (environment.safeResolve(motionDynamic.rotationDragCoefficient()) / 400.0);
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
                double rx = emitter.rotateLocalX(lx, ly, lz);
                double ry = emitter.rotateLocalY(lx, ly, lz);
                double rz = emitter.rotateLocalZ(lx, ly, lz);
                setPosition(emitter.getX() + rx, emitter.getY() + ry, emitter.getZ() + rz);
            }
            MolangExpression[] dir = motionParametric.direction();
            if (dir != null) {
                double lx = environment.safeResolve(dir[0]);
                double ly = environment.safeResolve(dir[1]);
                double lz = environment.safeResolve(dir[2]);
                double dx = emitter != null ? emitter.rotateLocalX(lx, ly, lz) : lx;
                double dy = emitter != null ? emitter.rotateLocalY(lx, ly, lz) : ly;
                double dz = emitter != null ? emitter.rotateLocalZ(lx, ly, lz) : lz;
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
            double rx = ox * cos - oz * sin;
            double rz = ox * sin + oz * cos;
            this.x = emitterX + rx;
            this.z = emitterZ + rz;
        }

        private void rotateVelocity(float yawDeltaRad) {
            if (yawDeltaRad == 0.0f) {
                return;
            }
            double cos = Math.cos(yawDeltaRad);
            double sin = Math.sin(yawDeltaRad);
            double rvx = this.vx * cos - this.vz * sin;
            double rvz = this.vx * sin + this.vz * cos;
            this.vx = rvx;
            this.vz = rvz;
        }

        private void onExpired() {
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

    private final class ActiveEmitter {
        private final ParticleData data;
        private final Map<String, ParticleData.Curve> curves;
        private final Map<String, Float> curveValues;
        private final ParticleMolangContext molangContext;
        private final ParticleMolangEnvironment environment;
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
        private boolean instantEmitted;
        private boolean expired;
        private int activeParticles;
        private boolean spawnedAny;

        private ActiveEmitter(ParticleData data, EmitterTransformProvider provider, int overrideCount) {
            this.data = data;
            this.transformProvider = provider != null ? provider : new StaticTransformProvider(0.0, 0.0, 0.0, 0.0f);
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
            this.deltaX = 0.0;
            this.deltaY = 0.0;
            this.deltaZ = 0.0;
            this.deltaYawRad = 0.0f;
            this.overrideCount = overrideCount;
            this.curves = buildCurveDefinitions(data);
            this.curveValues = this.curves.isEmpty() ? Collections.emptyMap() : new HashMap<>(this.curves.size());
            this.molangContext = new ParticleMolangContext();
            this.molangContext.curves = this.curveValues;
            this.molangContext.random1 = (float) Math.random();
            this.molangContext.random2 = (float) Math.random();
            this.molangContext.random3 = (float) Math.random();
            this.molangContext.random4 = (float) Math.random();
            this.molangContext.random = this.molangContext.random1;
            this.molangContext.emitterRandom1 = this.molangContext.random1;
            this.molangContext.emitterRandom2 = this.molangContext.random2;
            this.molangContext.emitterRandom3 = this.molangContext.random3;
            this.molangContext.emitterRandom4 = this.molangContext.random4;
            this.molangContext.entityScale = 1.0f;
            ExpressionBindingContext bindings = createBindingContext(this.curves);
            this.environment = new ParticleMolangEnvironment(molangContext, bindings);

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
            this.texture = toMinecraft(data);
            this.age = 0.0f;
            this.lifetime = Float.MAX_VALUE;
            this.instantEmitted = false;
            this.expired = false;
            this.activeParticles = 0;
            this.spawnedAny = false;
            this.spawner = new EmitterShapeSpawner();

            evaluateLifetimeOnCreate();
            updateContext(0.0f);
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
                expired = true;
            }
        }

        private boolean tick() {
            if (expired) {
                return activeParticles > 0;
            }
            updateTransform(TICK_SECONDS);
            updateContext(age);
            applyLifetimeLogic();
            updateContext(age);
            if (!expired && isActive()) {
                if (rateInstant != null) {
                    if (!instantEmitted) {
                        emitParticles(resolveInstantCount(), MAX_DEBUG_PARTICLES - particles.size());
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

        private void emitSteady() {
            int maxCount = getMaxParticles() - activeParticles;
            if (maxCount <= 0) {
                return;
            }
            float spawnRate = environment.safeResolve(rateSteady.spawnRate());
            int perTick = (int) (spawnRate / 20.0f);
            if (perTick <= 0) {
                return;
            }
            int room = MAX_DEBUG_PARTICLES - particles.size();
            int count = Math.min(Math.min(perTick, maxCount), room);
            emitParticles(count, room);
        }

        private int resolveInstantCount() {
            if (overrideCount > 0) {
                return overrideCount;
            }
            float count = environment.safeResolve(rateInstant.particleCount());
            return Math.max(1, Math.round(count));
        }

        private int getMaxParticles() {
            if (rateSteady == null) {
                return Integer.MAX_VALUE;
            }
            if (maxParticlesEval <= 0) {
                float max = environment.safeResolve(rateSteady.maxParticles());
                maxParticlesEval = Math.max(1, Math.round(max));
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
                    particles.add(particle);
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
                    expired = true;
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
                    expired = true;
                }
                return;
            }
        }

        private void restartLoop() {
            age = 0.0f;
            activeTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeLooping.activeTime()));
            sleepTimeEval = Math.max(0.0f, environment.safeResolve(lifetimeLooping.sleepTime()));
            sleepRemaining = sleepTimeEval;
            molangContext.random1 = (float) Math.random();
            molangContext.random2 = (float) Math.random();
            molangContext.random3 = (float) Math.random();
            molangContext.random4 = (float) Math.random();
            molangContext.random = molangContext.random1;
            molangContext.emitterRandom1 = molangContext.random1;
            molangContext.emitterRandom2 = molangContext.random2;
            molangContext.emitterRandom3 = molangContext.random3;
            molangContext.emitterRandom4 = molangContext.random4;
            instantEmitted = false;
            maxParticlesEval = 0;
            updateContext(0.0f);
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

        private boolean isLocalPosition() {
            return localSpace != null && localSpace.position();
        }

        private boolean isLocalRotation() {
            return localSpace != null && localSpace.rotation();
        }

        private boolean isLocalVelocity() {
            return localSpace != null && localSpace.velocity();
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
                particles.add(particle);
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
                double rx = rotateLocalX(dx, dy, dz);
                double ry = rotateLocalY(dx, dy, dz);
                double rz = rotateLocalZ(dx, dy, dz);
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
        private float emitterRandom1;
        private float emitterRandom2;
        private float emitterRandom3;
        private float emitterRandom4;
        private float entityScale;
        private Map<String, Float> curves;

        private float getCurveValue(String name) {
            if (curves == null) {
                return 0.0f;
            }
            Float value = curves.get(name);
            return value != null ? value : 0.0f;
        }
    }

    private static final class ParticleMolangEnvironment implements BindingMolangEnvironment {
        private final ParticleMolangContext context;
        private final ExpressionBindingContext bindings;
        private final Map<org.mybad.core.legacy.expression.molang.ast.MolangExpression, org.mybad.core.legacy.expression.molang.ast.MolangExpression> cache;

        private ParticleMolangEnvironment(ParticleMolangContext context, ExpressionBindingContext bindings) {
            this.context = context;
            this.bindings = bindings;
            this.cache = new IdentityHashMap<>();
        }

        @Override
        public org.mybad.core.legacy.expression.molang.ast.MolangExpression bind(
            org.mybad.core.legacy.expression.molang.ast.MolangExpression expression
        ) {
            if (expression == null) {
                return null;
            }
            org.mybad.core.legacy.expression.molang.ast.MolangExpression bound = cache.get(expression);
            if (bound == null) {
                bound = expression.bind(bindings, context);
                cache.put(expression, bound);
            }
            return bound;
        }
    }

    private static ExpressionBindingContext buildMolangBindings() {
        ExpressionBindingContext context = ExpressionBindingContext.create();
        // variable.*
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "particle_age", ParticleMolangContext.class, ctx -> ctx.particleAge);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "particle_lifetime", ParticleMolangContext.class, ctx -> ctx.particleLifetime);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "emitter_age", ParticleMolangContext.class, ctx -> ctx.emitterAge);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "emitter_lifetime", ParticleMolangContext.class, ctx -> ctx.emitterLifetime);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "random", ParticleMolangContext.class, ctx -> ctx.random);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "random_1", ParticleMolangContext.class, ctx -> ctx.random1);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "random_2", ParticleMolangContext.class, ctx -> ctx.random2);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "random_3", ParticleMolangContext.class, ctx -> ctx.random3);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "random_4", ParticleMolangContext.class, ctx -> ctx.random4);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "particle_random_1", ParticleMolangContext.class, ctx -> ctx.random1);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "particle_random_2", ParticleMolangContext.class, ctx -> ctx.random2);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "particle_random_3", ParticleMolangContext.class, ctx -> ctx.random3);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "particle_random_4", ParticleMolangContext.class, ctx -> ctx.random4);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "emitter_random_1", ParticleMolangContext.class, ctx -> ctx.emitterRandom1);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "emitter_random_2", ParticleMolangContext.class, ctx -> ctx.emitterRandom2);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "emitter_random_3", ParticleMolangContext.class, ctx -> ctx.emitterRandom3);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "emitter_random_4", ParticleMolangContext.class, ctx -> ctx.emitterRandom4);
        context.registerDirectReferenceResolver(ReferenceType.VARIABLE, "entity_scale", ParticleMolangContext.class, ctx -> ctx.entityScale);
        // query.* (common aliases)
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "particle_age", ParticleMolangContext.class, ctx -> ctx.particleAge);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "particle_lifetime", ParticleMolangContext.class, ctx -> ctx.particleLifetime);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "emitter_age", ParticleMolangContext.class, ctx -> ctx.emitterAge);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "emitter_lifetime", ParticleMolangContext.class, ctx -> ctx.emitterLifetime);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "random", ParticleMolangContext.class, ctx -> ctx.random);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "random_1", ParticleMolangContext.class, ctx -> ctx.random1);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "random_2", ParticleMolangContext.class, ctx -> ctx.random2);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "random_3", ParticleMolangContext.class, ctx -> ctx.random3);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "random_4", ParticleMolangContext.class, ctx -> ctx.random4);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "particle_random_1", ParticleMolangContext.class, ctx -> ctx.random1);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "particle_random_2", ParticleMolangContext.class, ctx -> ctx.random2);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "particle_random_3", ParticleMolangContext.class, ctx -> ctx.random3);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "particle_random_4", ParticleMolangContext.class, ctx -> ctx.random4);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "emitter_random_1", ParticleMolangContext.class, ctx -> ctx.emitterRandom1);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "emitter_random_2", ParticleMolangContext.class, ctx -> ctx.emitterRandom2);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "emitter_random_3", ParticleMolangContext.class, ctx -> ctx.emitterRandom3);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "emitter_random_4", ParticleMolangContext.class, ctx -> ctx.emitterRandom4);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "entity_scale", ParticleMolangContext.class, ctx -> ctx.entityScale);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "age", ParticleMolangContext.class, ctx -> ctx.particleAge);
        context.registerDirectReferenceResolver(ReferenceType.QUERY, "life_time", ParticleMolangContext.class, ctx -> ctx.particleLifetime);
        return context;
    }

    private static ExpressionBindingContext createBindingContext(Map<String, ParticleData.Curve> curves) {
        ExpressionBindingContext context = buildMolangBindings();
        if (curves == null || curves.isEmpty()) {
            return context;
        }
        for (String name : curves.keySet()) {
            final String key = name;
            context.registerDirectReferenceResolver(ReferenceType.VARIABLE, key, ParticleMolangContext.class, ctx -> ctx.getCurveValue(key));
        }
        return context;
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
        float step = input - current.getTime() + next.getTime() / 3.0f;
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

    public interface EmitterTransformProvider {
        void fill(EmitterTransform transform, float deltaSeconds);
    }

    public static final class EmitterTransform {
        public double x;
        public double y;
        public double z;
        public float yaw;
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
            setIdentityBasis(transform);
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
}
