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
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceBillboardComponent;
import gg.moonflower.pinwheel.particle.component.ParticleAppearanceTintingComponent;
import gg.moonflower.pinwheel.particle.component.ParticleInitialSpeedComponent;
import gg.moonflower.pinwheel.particle.component.ParticleLifetimeExpressionComponent;
import gg.moonflower.pollen.particle.render.QuadRenderProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
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
    private <T> T getComponent(ParticleData data, String name) {
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
        private float age;

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
        }

        private void randomizeVelocity(Random random) {
            if (speed == null) {
                return;
            }
            MolangExpression[] speeds = speed.speed();
            if (speeds == null || speeds.length < 3) {
                return;
            }
            updateContext(this.age);
            this.vx = environment.safeResolve(speeds[0]);
            this.vy = environment.safeResolve(speeds[1]);
            this.vz = environment.safeResolve(speeds[2]);
            if (Math.abs(vx) + Math.abs(vy) + Math.abs(vz) > 0.0) {
                return;
            }
            this.vx = (random.nextDouble() - 0.5) * 0.02;
            this.vy = random.nextDouble() * 0.02;
            this.vz = (random.nextDouble() - 0.5) * 0.02;
        }

        private float resolveLifetime(ParticleLifetimeExpressionComponent lifetimeComponent) {
            if (lifetimeComponent == null) {
                return 1.0f;
            }
            float value = environment.safeResolve(lifetimeComponent.maxLifetime());
            return value > 0.0f ? value : 1.0f;
        }

        private boolean tick() {
            if (emitter != null) {
                if (localPosition) {
                    this.x += emitter.getDeltaX();
                    this.y += emitter.getDeltaY();
                    this.z += emitter.getDeltaZ();
                }
                if (localRotation) {
                    rotateAroundEmitter(emitter.getX(), emitter.getZ(), emitter.getDeltaYawRad());
                }
                if (localVelocity) {
                    rotateVelocity(emitter.getDeltaYawRad());
                }
            }
            this.prevX = this.x;
            this.prevY = this.y;
            this.prevZ = this.z;
            this.x += this.vx;
            this.y += this.vy;
            this.z += this.vz;
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
            GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
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
            for (int i = 0; i < actual; i++) {
                ActiveParticle particle = new ActiveParticle(
                    data,
                    this,
                    x, y, z,
                    billboard,
                    tint,
                    speed,
                    texture,
                    particleLifetimeComponent
                );
                particle.randomizeVelocity(random);
                particles.add(particle);
                activeParticles++;
                spawnedAny = true;
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
            transformProvider.fill(currentTransform, deltaSeconds);
            x = currentTransform.x;
            y = currentTransform.y;
            z = currentTransform.z;
            yaw = currentTransform.yaw;
            deltaX = x - lastX;
            deltaY = y - lastY;
            deltaZ = z - lastZ;
            float deltaYawDeg = wrapDegrees(yaw - lastYaw);
            deltaYawRad = (float) Math.toRadians(deltaYawDeg);
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

        private double getZ() {
            return z;
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

    public interface EmitterTransformProvider {
        void fill(EmitterTransform transform, float deltaSeconds);
    }

    public static final class EmitterTransform {
        public double x;
        public double y;
        public double z;
        public float yaw;
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
        }
    }
}
