package org.mybad.minecraft.particle.runtime;

import org.mybad.bedrockparticle.pinwheel.particle.ParticleData;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleExpireInBlocksComponent;
import org.mybad.bedrockparticle.pinwheel.particle.component.ParticleExpireNotInBlocksComponent;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.particle.transform.EmitterTransform;
import org.mybad.minecraft.particle.transform.EmitterTransformProvider;
import org.mybad.bedrockparticle.pinwheel.particle.BedrockResourceLocation;
import org.mybad.minecraft.resource.ResourceLoader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@SideOnly(Side.CLIENT)
public class BedrockParticleSystem {

    static final float TICK_SECONDS = 1.0f / 20.0f;
    static final int MAX_ACTIVE_PARTICLES = 2000;
    static final int DEFAULT_PARTICLE_POOL_LIMIT = 96;
    static final int MAX_POOLED_PARTICLES = 768;
    private static final FloatBuffer ORIENTATION_BUFFER = BufferUtils.createFloatBuffer(16);

    private final ResourceLoader resourceLoader;
    private final List<ActiveParticle> particles;
    private final List<ActiveEmitter> emitters;
    private final List<ActiveParticle> pendingParticles;
    private final List<ActiveEmitter> pendingEmitters;
    private final Random random;
    private int pooledParticles;
    private boolean ticking;

    public BedrockParticleSystem(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
        this.particles = new ArrayList<>();
        this.emitters = new ArrayList<>();
        this.pendingParticles = new ArrayList<>();
        this.pendingEmitters = new ArrayList<>();
        this.random = new Random();
        this.ticking = false;
    }

    Random getRandom() {
        return random;
    }

    void onPoolBorrow() {
        pooledParticles--;
        if (pooledParticles < 0) {
            pooledParticles = 0;
        }
    }

    void onPoolReturn() {
        pooledParticles++;
    }

    boolean canPoolMore() {
        return pooledParticles < MAX_POOLED_PARTICLES;
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
        ActiveEmitter emitter = new ActiveEmitter(this, data, provider, overrideCount);
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

    void addParticle(ActiveParticle particle) {
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

    int getRemainingParticleRoom() {
        int used = particles.size() + pendingParticles.size();
        return MAX_ACTIVE_PARTICLES - used;
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
                    pooledParticles -= emitter.clearPool();
                    if (pooledParticles < 0) {
                        pooledParticles = 0;
                    }
                }
            }
        }
        Iterator<ActiveParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            ActiveParticle particle = iterator.next();
            if (!particle.tick()) {
                particle.onExpired();
                particle.recycle();
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

    ResourceLocation toMinecraft(ParticleData data) {
        if (data == null || data.description() == null || data.description().getTexture() == null) {
            return new ResourceLocation("missingno");
        }
        BedrockResourceLocation tex = data.description().getTexture();
        return new ResourceLocation(tex.getNamespace(), tex.getPath());
    }

    @SuppressWarnings("unchecked")
    static <T> T getComponent(ParticleData data, String name) {
        if (data == null) {
            return null;
        }
        Map<String, org.mybad.bedrockparticle.pinwheel.particle.component.ParticleComponent> components = data.components();
        if (components == null || components.isEmpty()) {
            return null;
        }
        org.mybad.bedrockparticle.pinwheel.particle.component.ParticleComponent component = components.get("minecraft:" + name);
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

    static float wrapDegrees(float degrees) {
        float result = degrees % 360.0f;
        if (result >= 180.0f) {
            result -= 360.0f;
        }
        if (result < -180.0f) {
            result += 360.0f;
        }
        return result;
    }

    public static void normalize(float[] vec) {
        float len = (float) Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
        if (len > 0.0f) {
            vec[0] /= len;
            vec[1] /= len;
            vec[2] /= len;
        }
    }

    public static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    public static void cross(float[] a, float[] b, float[] out) {
        out[0] = a[1] * b[2] - a[2] * b[1];
        out[1] = a[2] * b[0] - a[0] * b[2];
        out[2] = a[0] * b[1] - a[1] * b[0];
    }

    public static void multMatrix(float[] xAxis, float[] yAxis, float[] zAxis) {
        ORIENTATION_BUFFER.clear();
        ORIENTATION_BUFFER.put(xAxis[0]).put(xAxis[1]).put(xAxis[2]).put(0.0f);
        ORIENTATION_BUFFER.put(yAxis[0]).put(yAxis[1]).put(yAxis[2]).put(0.0f);
        ORIENTATION_BUFFER.put(zAxis[0]).put(zAxis[1]).put(zAxis[2]).put(0.0f);
        ORIENTATION_BUFFER.put(0.0f).put(0.0f).put(0.0f).put(1.0f);
        ORIENTATION_BUFFER.flip();
        GL11.glMultMatrix(ORIENTATION_BUFFER);
    }

    public enum BlendMode {
        ALPHA,
        ADD,
        OPAQUE
    }

    BlendMode resolveBlendMode(ParticleData data) {
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

    Block[] resolveBlocks(ParticleExpireInBlocksComponent component) {
        if (component == null) {
            return null;
        }
        return resolveBlocks(component.blocks());
    }

    Block[] resolveBlocks(ParticleExpireNotInBlocksComponent component) {
        if (component == null) {
            return null;
        }
        return resolveBlocks(component.blocks());
    }

    Block[] resolveBlocks(String[] blockIds) {
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

    void spawnEffectAt(String effect, double x, double y, double z, boolean bound) {
        String path = normalizeParticlePath(effect);
        if (path == null) {
            return;
        }
        EmitterTransformProvider provider = new SnapshotTransformProvider(x, y, z, 0.0f, null, null, null, 1.0f);
        spawnInternal(path, provider, 0);
    }

    void spawnEffectFromEmitter(String effect, ActiveEmitter emitter, boolean bound) {
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

    String normalizeParticlePath(String effect) {
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

    void playSoundAt(String sound, double x, double y, double z) {
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

    void logMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (SkyCoreMod.LOGGER.isDebugEnabled()) {
            SkyCoreMod.LOGGER.debug("[Particle] {}", message);
        }
    }

    public static final class StaticTransformProvider implements EmitterTransformProvider {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;

        StaticTransformProvider(double x, double y, double z, float yaw) {
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

    static void copyBasis(float[] src, float[] dst) {
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
    }
}
