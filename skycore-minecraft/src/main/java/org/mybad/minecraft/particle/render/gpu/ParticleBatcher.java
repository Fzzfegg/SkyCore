package org.mybad.minecraft.particle.render.gpu;

import org.mybad.bedrockparticle.molang.api.MolangEnvironment;
import org.mybad.bedrockparticle.molang.api.MolangExpression;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceBillboardComponent;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceLightingComponent;
import org.mybad.bedrockparticle.particle.component.ParticleAppearanceTintingComponent;
import org.mybad.bedrockparticle.particle.render.QuadRenderProperties;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.particle.runtime.ActiveEmitter;
import org.mybad.minecraft.particle.runtime.ActiveParticle;
import org.mybad.minecraft.particle.runtime.BedrockParticleSystem;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ParticleBatcher {
    static final int FLOATS_PER_PARTICLE = 24;
    static final int FLOATS_PER_EMITTER = 16;

    private final QuadRenderProperties renderProps = new QuadRenderProperties();

    Result build(List<ActiveParticle> particles, float partialTicks) {
        if (particles.isEmpty()) {
            return Result.empty();
        }

        Map<BatchKey, BatchBucket> buckets = new LinkedHashMap<>();
        Map<BatchKey, BatchBucket> emissiveBuckets = new LinkedHashMap<>();
        Map<BatchKey, BatchBucket> blendBuckets = new LinkedHashMap<>();
        Map<ActiveEmitter, Integer> emitterIndex = new IdentityHashMap<>();

        int totalParticles = 0;
        int emissiveParticles = 0;
        int blendParticles = 0;

        for (ActiveParticle particle : particles) {
            particle.prepareRender(partialTicks);
            ParticleAppearanceBillboardComponent billboard = particle.getBillboard();
            if (billboard == null) {
                continue;
            }

            float width = 1.0f;
            float height = 1.0f;
            MolangExpression[] size = billboard.size();
            MolangEnvironment env = particle.getEnvironment();
            if (size != null && size.length >= 2) {
                width = env.safeResolve(size[0]);
                height = env.safeResolve(size[1]);
            }
            width *= 2.0f;
            height *= 2.0f;
            float scale = particle.getEmitterScale();
            width *= scale;
            height *= scale;
            if (width <= 0.0f || height <= 0.0f) {
                continue;
            }

            renderProps.setUV(0.0f, 0.0f, 1.0f, 1.0f);
            billboard.textureSetter().setUV(particle, env, renderProps);

            float r = 1.0f;
            float g = 1.0f;
            float b = 1.0f;
            float a = 1.0f;
            ParticleAppearanceTintingComponent tint = particle.getTint();
            if (tint != null) {
                r = clamp01(tint.red().get(particle, env));
                g = clamp01(tint.green().get(particle, env));
                b = clamp01(tint.blue().get(particle, env));
                a = clamp01(tint.alpha().get(particle, env));
            }

            ParticleAppearanceLightingComponent lighting = particle.getLighting();
            boolean lit = lighting != null;

            float roll = particle.getPrevRoll() + (particle.getRoll() - particle.getPrevRoll()) * partialTicks;

            float[] dir = resolveFacingDirection(particle, billboard, env);
            if (dir == null) {
                if (isDirectionMode(billboard.cameraMode())) {
                    continue;
                }
                dir = new float[]{0.0f, 0.0f, 0.0f};
            }

            int camMode = encodeCameraMode(billboard.cameraMode());
            ResourceLocation texture = particle.getTexture();
            BedrockParticleSystem.BlendMode blendMode = particle.getBlendMode();
            boolean bloom = particle.isBloom();
            ResourceLocation emissiveTexture = particle.getEmissiveTexture();
            float emissiveStrength = particle.getEmissiveStrength();
            boolean hasEmissive = emissiveTexture != null && emissiveStrength > 0.0f;
            ResourceLocation blendTexture = particle.getBlendTexture();
            BedrockParticleSystem.BlendMode blendModeOverlay = particle.getBlendModeOverlay();
            float blendR = particle.getBlendR();
            float blendG = particle.getBlendG();
            float blendB = particle.getBlendB();
            float blendA = particle.getBlendA();

            BatchKey key = new BatchKey(texture, texture, blendMode, camMode, lit, bloom, bloom ? particle.getBloomStrength() : 0f);
            BatchBucket bucket = buckets.get(key);
            if (bucket == null) {
                bucket = new BatchBucket();
                buckets.put(key, bucket);
            }

            int emitterIdx = 0;
            ActiveEmitter emitter = particle.getEmitter();
            if (emitter != null) {
                Integer existing = emitterIndex.get(emitter);
                if (existing == null) {
                    emitterIdx = emitterIndex.size() + 1; // 0 reserved for identity
                    emitterIndex.put(emitter, emitterIdx);
                } else {
                    emitterIdx = existing;
                }
            }

            double px = particle.getPrevX() + (particle.getX() - particle.getPrevX()) * partialTicks;
            double py = particle.getPrevY() + (particle.getY() - particle.getPrevY()) * partialTicks;
            double pz = particle.getPrevZ() + (particle.getZ() - particle.getPrevZ()) * partialTicks;
            py += 0.01;

            float lightU = 0.0f;
            float lightV = 0.0f;
            if (lit) {
                int packed = particle.resolvePackedLight(px, py, pz);
                int lx = packed & 0xFFFF;
                int ly = (packed >> 16) & 0xFFFF;
                lightU = (lx + 8.0f) / 256.0f;
                lightV = (ly + 8.0f) / 256.0f;
            }

            bucket.add((float) px, (float) py, (float) pz, roll,
                width, height, camMode, 0.0f,
                r, g, b, a,
                renderProps.getUMin(), renderProps.getVMin(), renderProps.getUMax(), renderProps.getVMax(),
                dir[0], dir[1], dir[2], (float) emitterIdx,
                lightU, lightV, emissiveStrength, 0.0f);

            totalParticles++;

            if (hasEmissive) {
                BatchKey emissiveKey = new BatchKey(emissiveTexture, texture, BedrockParticleSystem.BlendMode.ADD, camMode, false, false, 0f);
                BatchBucket emissiveBucket = emissiveBuckets.get(emissiveKey);
                if (emissiveBucket == null) {
                    emissiveBucket = new BatchBucket();
                    emissiveBuckets.put(emissiveKey, emissiveBucket);
                }
                emissiveBucket.add((float) px, (float) py, (float) pz, roll,
                    width, height, camMode, 0.0f,
                    r, g, b, a,
                    renderProps.getUMin(), renderProps.getVMin(), renderProps.getUMax(), renderProps.getVMax(),
                    dir[0], dir[1], dir[2], (float) emitterIdx,
                    lightU, lightV, emissiveStrength, 0.0f);
                emissiveParticles++;
            }

            if (blendTexture != null && blendA > 0.0f) {
                BatchKey blendKey = new BatchKey(blendTexture, blendTexture, blendModeOverlay != null ? blendModeOverlay : BedrockParticleSystem.BlendMode.ALPHA, camMode, false, false, 0f);
                BatchBucket blendBucket = blendBuckets.get(blendKey);
                if (blendBucket == null) {
                    blendBucket = new BatchBucket();
                    blendBuckets.put(blendKey, blendBucket);
                }
                blendBucket.add((float) px, (float) py, (float) pz, roll,
                    width, height, camMode, 0.0f,
                    blendR, blendG, blendB, blendA,
                    renderProps.getUMin(), renderProps.getVMin(), renderProps.getUMax(), renderProps.getVMax(),
                    dir[0], dir[1], dir[2], (float) emitterIdx,
                    lightU, lightV, 0.0f, 0.0f);
                blendParticles++;
            }
        }

        if (totalParticles == 0) {
            return Result.empty();
        }

        int emitterCount = emitterIndex.size() + 1;
        float[] emitterData = new float[emitterCount * FLOATS_PER_EMITTER];
        // identity basis at index 0
        emitterData[0] = 1.0f;
        emitterData[5] = 1.0f;
        emitterData[10] = 1.0f;

        for (Map.Entry<ActiveEmitter, Integer> entry : emitterIndex.entrySet()) {
            int idx = entry.getValue();
            ActiveEmitter emitter = entry.getKey();
            float[] ex = emitter.getBasisX();
            float[] ey = emitter.getBasisY();
            float[] ez = emitter.getBasisZ();
            int base = idx * FLOATS_PER_EMITTER;
            emitterData[base + 0] = ex[0];
            emitterData[base + 1] = ex[1];
            emitterData[base + 2] = ex[2];
            emitterData[base + 4] = ey[0];
            emitterData[base + 5] = ey[1];
            emitterData[base + 6] = ey[2];
            emitterData[base + 8] = ez[0];
            emitterData[base + 9] = ez[1];
            emitterData[base + 10] = ez[2];
        }
        FloatBuffer emitterBuffer = BufferUtilsHelper.createFloatBuffer(emitterData.length);
        emitterBuffer.put(emitterData).flip();

        FloatBuffer particleBuffer = BufferUtilsHelper.createFloatBuffer(totalParticles * FLOATS_PER_PARTICLE);
        List<Batch> batches = new ArrayList<>(buckets.size());
        int offset = 0;
        for (Map.Entry<BatchKey, BatchBucket> entry : buckets.entrySet()) {
            BatchBucket bucket = entry.getValue();
            int count = bucket.size() / FLOATS_PER_PARTICLE;
            if (count <= 0) {
                continue;
            }
            particleBuffer.put(bucket.data, 0, bucket.size());
            batches.add(new Batch(entry.getKey(), offset, count));
            offset += count;
        }
        particleBuffer.flip();

        FloatBuffer emissiveBuffer = BufferUtilsHelper.emptyFloatBuffer();
        List<Batch> emissiveBatches = new ArrayList<>();
        if (emissiveParticles > 0) {
            emissiveBuffer = BufferUtilsHelper.createFloatBuffer(emissiveParticles * FLOATS_PER_PARTICLE);
            int emissiveOffset = 0;
            for (Map.Entry<BatchKey, BatchBucket> entry : emissiveBuckets.entrySet()) {
                BatchBucket bucket = entry.getValue();
                int count = bucket.size() / FLOATS_PER_PARTICLE;
                if (count <= 0) {
                    continue;
                }
                emissiveBuffer.put(bucket.data, 0, bucket.size());
                emissiveBatches.add(new Batch(entry.getKey(), emissiveOffset, count));
                emissiveOffset += count;
            }
            emissiveBuffer.flip();
        }

        FloatBuffer blendBuffer = BufferUtilsHelper.emptyFloatBuffer();
        List<Batch> blendBatches = new ArrayList<>();
        if (blendParticles > 0) {
            blendBuffer = BufferUtilsHelper.createFloatBuffer(blendParticles * FLOATS_PER_PARTICLE);
            int blendOffset = 0;
            for (Map.Entry<BatchKey, BatchBucket> entry : blendBuckets.entrySet()) {
                BatchBucket bucket = entry.getValue();
                int count = bucket.size() / FLOATS_PER_PARTICLE;
                if (count <= 0) {
                    continue;
                }
                blendBuffer.put(bucket.data, 0, bucket.size());
                blendBatches.add(new Batch(entry.getKey(), blendOffset, count));
                blendOffset += count;
            }
            blendBuffer.flip();
        }

        return new Result(particleBuffer, emitterBuffer, batches, totalParticles, emitterCount,
            emissiveBuffer, emissiveBatches, emissiveParticles,
            blendBuffer, blendBatches, blendParticles);
    }

    private static boolean isDirectionMode(ParticleAppearanceBillboardComponent.FaceCameraMode mode) {
        return mode == ParticleAppearanceBillboardComponent.FaceCameraMode.DIRECTION_X
            || mode == ParticleAppearanceBillboardComponent.FaceCameraMode.DIRECTION_Y
            || mode == ParticleAppearanceBillboardComponent.FaceCameraMode.DIRECTION_Z
            || mode == ParticleAppearanceBillboardComponent.FaceCameraMode.LOOKAT_DIRECTION;
    }

    private static int encodeCameraMode(ParticleAppearanceBillboardComponent.FaceCameraMode mode) {
        if (mode == null) {
            return 0;
        }
        switch (mode) {
            case ROTATE_Y:
                return 1;
            case LOOK_AT_XYZ:
                return 2;
            case LOOK_AT_Y:
                return 3;
            case DIRECTION_X:
                return 4;
            case DIRECTION_Y:
                return 5;
            case DIRECTION_Z:
                return 6;
            case LOOKAT_DIRECTION:
                return 7;
            case EMITTER_TRANSFORM_XY:
                return 8;
            case EMITTER_TRANSFORM_XZ:
                return 9;
            case EMITTER_TRANSFORM_YZ:
                return 10;
            case ROTATE_XYZ:
            default:
                return 0;
        }
    }

    private static float[] resolveFacingDirection(ActiveParticle particle,
                                                  ParticleAppearanceBillboardComponent billboard,
                                                  MolangEnvironment env) {
        if (billboard.customDirection() != null) {
            float dx = env.safeResolve(billboard.customDirection()[0]);
            float dy = env.safeResolve(billboard.customDirection()[1]);
            float dz = env.safeResolve(billboard.customDirection()[2]);
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len <= 1.0e-6f) {
                return null;
            }
            return new float[]{dx / len, dy / len, dz / len};
        }
        double vx = particle.getVx();
        double vy = particle.getVy();
        double vz = particle.getVz();
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed <= billboard.minSpeedThreshold()) {
            return null;
        }
        if (speed <= 1.0e-6) {
            return null;
        }
        return new float[]{(float) (vx / speed), (float) (vy / speed), (float) (vz / speed)};
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

    static final class BatchKey {
        final ResourceLocation texture;
        final ResourceLocation baseTexture;
        final BedrockParticleSystem.BlendMode blendMode;
        final int cameraMode;
        final boolean lit;
        final boolean bloom;
        final float bloomStrength;

        BatchKey(ResourceLocation texture,
                 ResourceLocation baseTexture,
                 BedrockParticleSystem.BlendMode blendMode,
                 int cameraMode,
                 boolean lit,
                 boolean bloom,
                 float bloomStrength) {
            this.texture = texture;
            this.baseTexture = baseTexture;
            this.blendMode = blendMode;
            this.cameraMode = cameraMode;
            this.lit = lit;
            this.bloom = bloom;
            this.bloomStrength = bloom ? Math.max(0f, bloomStrength) : 0f;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof BatchKey)) {
                return false;
            }
            BatchKey other = (BatchKey) obj;
            return cameraMode == other.cameraMode
                && lit == other.lit
                && bloom == other.bloom
                && Float.floatToIntBits(bloomStrength) == Float.floatToIntBits(other.bloomStrength)
                && blendMode == other.blendMode
                && texture.equals(other.texture)
                && baseTexture.equals(other.baseTexture);
        }

        @Override
        public int hashCode() {
            int result = texture.hashCode();
            result = 31 * result + baseTexture.hashCode();
            result = 31 * result + blendMode.hashCode();
            result = 31 * result + cameraMode;
            result = 31 * result + (lit ? 1 : 0);
            result = 31 * result + (bloom ? 1 : 0);
            result = 31 * result + Float.floatToIntBits(bloomStrength);
            return result;
        }
    }

    static final class Batch {
        final BatchKey key;
        final int offset;
        final int count;

        Batch(BatchKey key, int offset, int count) {
            this.key = key;
            this.offset = offset;
            this.count = count;
        }
    }

    static final class Result {
        final FloatBuffer particleBuffer;
        final FloatBuffer emitterBuffer;
        final List<Batch> batches;
        final int particleCount;
        final int emitterCount;
        final FloatBuffer emissiveParticleBuffer;
        final List<Batch> emissiveBatches;
        final int emissiveCount;
        final FloatBuffer blendParticleBuffer;
        final List<Batch> blendBatches;
        final int blendCount;

        Result(FloatBuffer particleBuffer,
               FloatBuffer emitterBuffer,
               List<Batch> batches,
               int particleCount,
               int emitterCount,
               FloatBuffer emissiveParticleBuffer,
               List<Batch> emissiveBatches,
               int emissiveCount,
               FloatBuffer blendParticleBuffer,
               List<Batch> blendBatches,
               int blendCount) {
            this.particleBuffer = particleBuffer;
            this.emitterBuffer = emitterBuffer;
            this.batches = batches;
            this.particleCount = particleCount;
            this.emitterCount = emitterCount;
            this.emissiveParticleBuffer = emissiveParticleBuffer;
            this.emissiveBatches = emissiveBatches;
            this.emissiveCount = emissiveCount;
            this.blendParticleBuffer = blendParticleBuffer;
            this.blendBatches = blendBatches;
            this.blendCount = blendCount;
        }

        static Result empty() {
            return new Result(BufferUtilsHelper.emptyFloatBuffer(), BufferUtilsHelper.emptyFloatBuffer(), new ArrayList<>(), 0, 0,
                BufferUtilsHelper.emptyFloatBuffer(), new ArrayList<>(), 0,
                BufferUtilsHelper.emptyFloatBuffer(), new ArrayList<>(), 0);
        }
    }

    private static final class BatchBucket {
        private float[] data = new float[256];
        private int size = 0;

        void add(float... values) {
            ensureCapacity(size + values.length);
            System.arraycopy(values, 0, data, size, values.length);
            size += values.length;
        }

        int size() {
            return size;
        }

        void ensureCapacity(int capacity) {
            if (capacity <= data.length) {
                return;
            }
            int next = Math.max(capacity, data.length * 2);
            float[] nextData = new float[next];
            System.arraycopy(data, 0, nextData, 0, size);
            data = nextData;
        }
    }
}
