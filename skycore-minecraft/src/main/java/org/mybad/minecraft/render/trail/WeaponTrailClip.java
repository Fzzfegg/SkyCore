package org.mybad.minecraft.render.trail;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ResourceLocation;
import org.mybad.minecraft.SkyCoreMod;
import org.mybad.minecraft.render.BedrockModelHandle;
import org.mybad.minecraft.render.entity.events.AnimationEventArgsParser;
import org.mybad.minecraft.render.transform.LocatorTransform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Runtime sword trail instance bound to a single entity.
 */
public final class WeaponTrailClip {
    private final String id;
    private final ArrayDeque<TrailSample> samples = new ArrayDeque<>();
    private String locatorStart;
    private String locatorEnd;
    private float[] offsetStart;
    private float[] offsetEnd;
    private ResourceLocation texture;
    private TrailBlendMode blendMode = TrailBlendMode.ADD;
    private float lifetime = 0.25f;
    private float sampleInterval = 0.02f;
    private int maxSamples = 32;
    private float colorR = 1.0f;
    private float colorG = 1.0f;
    private float colorB = 1.0f;
    private float alpha = 1.0f;
    private float uvSpeed = 1.0f;
    private float uvOffset = 0.0f;
    private float sampleAccumulator = 0.0f;
    private boolean emitting = false;
    private float width = 0.3f;
    private boolean solidColor = false;
    private TrailAxis axis = TrailAxis.Z;
    private final LocatorTransform locatorTransform = new LocatorTransform();
    private final float[] cachedBasis = new float[3];
    private boolean basisValid = false;

    public WeaponTrailClip(AnimationEventArgsParser.TrailParams params) {
        this.id = params != null && params.id != null ? params.id : "trail";
        applyParams(params);
    }

    public void applyParams(AnimationEventArgsParser.TrailParams params) {
        if (params == null) {
            return;
        }
        if (params.locatorStart != null && !params.locatorStart.isEmpty()) {
            this.locatorStart = params.locatorStart;
        }
        if (params.locatorEnd != null && !params.locatorEnd.isEmpty()) {
            this.locatorEnd = params.locatorEnd;
        }
        if (params.offsetStart != null) {
            this.offsetStart = params.offsetStart.clone();
        }
        if (params.offsetEnd != null) {
            this.offsetEnd = params.offsetEnd.clone();
        }
        if (params.width > 0f) {
            this.width = params.width;
        }
        if (params.texture != null) {
            this.texture = params.texture;
        }
        this.solidColor = params.solidColor || params.texture == null;
        if (params.axis != null) {
            this.axis = params.axis;
        }
        this.lifetime = clampPositive(params.lifetime, 0.05f, 5.0f);
        this.sampleInterval = clampPositive(params.sampleInterval, 0.005f, 0.2f);
        this.maxSamples = params.maxSamples > 2 ? Math.min(params.maxSamples, 128) : 8;
        this.blendMode = params.blendMode != null ? params.blendMode : TrailBlendMode.ADD;
        this.colorR = clamp01(params.colorR);
        this.colorG = clamp01(params.colorG);
        this.colorB = clamp01(params.colorB);
        this.alpha = clamp01(params.alpha);
        this.uvSpeed = params.uvSpeed;
        this.sampleAccumulator = this.sampleInterval;
        this.uvOffset = 0.0f;
        this.samples.clear();
    }

    private float clampPositive(float value, float min, float max) {
        if (Float.isNaN(value) || value <= 0f) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }

    public String getId() {
        return id;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public TrailBlendMode getBlendMode() {
        return blendMode;
    }

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public float getBaseAlpha() {
        return alpha;
    }

    public float getUvOffset() {
        return uvOffset;
    }

    public float getUvSpeed() {
        return uvSpeed;
    }

    public Deque<TrailSample> getSamples() {
        return samples;
    }

    public boolean isSolidColor() {
        return solidColor;
    }

    public void startEmission() {
        this.emitting = true;
        this.sampleAccumulator = 0f;
    }

    public void stopEmission() {
        this.emitting = false;
    }

    public boolean isAlive() {
        return emitting || samples.size() >= 2;
    }

    public boolean hasRenderableGeometry() {
        return samples.size() >= 2;
    }

    public float computeAlpha(TrailSample sample) {
        if (sample == null) {
            return 0f;
        }
        float life = 1.0f - (sample.age / lifetime);
        if (life < 0f) {
            life = 0f;
        }
        return alpha * life;
    }

    public void update(EntityLivingBase entity,
                       BedrockModelHandle wrapper,
                       float partialTicks,
                       float deltaSeconds,
                       float headYaw) {
        ageSamples(deltaSeconds);
        uvOffset += uvSpeed * deltaSeconds;
        if (uvOffset > 4096f) {
            uvOffset -= 4096f;
        } else if (uvOffset < -4096f) {
            uvOffset += 4096f;
        }
        if (!emitting || entity == null || wrapper == null) {
            return;
        }
        sampleAccumulator += deltaSeconds;
        while (sampleAccumulator >= sampleInterval) {
            if (!sample(entity, wrapper, partialTicks, headYaw)) {
                break;
            }
            sampleAccumulator -= sampleInterval;
        }
    }

    private void ageSamples(float deltaSeconds) {
        if (deltaSeconds <= 0f || samples.isEmpty()) {
            return;
        }
        Iterator<TrailSample> iterator = samples.iterator();
        while (iterator.hasNext()) {
            TrailSample sample = iterator.next();
            sample.age += deltaSeconds;
            if (sample.age >= lifetime) {
                iterator.remove();
            }
        }
    }

    private boolean sample(EntityLivingBase entity,
                           BedrockModelHandle wrapper,
                           float partialTicks,
                           float headYaw) {
        basisValid = false;
        double[] startPos = resolvePosition(entity, wrapper, locatorStart, offsetStart, headYaw, partialTicks, true);
        boolean captureEndBasis = (locatorStart == null || locatorStart.isEmpty());
        double[] endPos = resolvePosition(entity, wrapper, locatorEnd, offsetEnd, headYaw, partialTicks, captureEndBasis);
        Vec3d pointA = startPos != null ? new Vec3d(startPos[0], startPos[1], startPos[2]) : null;
        Vec3d pointB = endPos != null ? new Vec3d(endPos[0], endPos[1], endPos[2]) : null;
        if (pointA == null && pointB == null) {
            SkyCoreMod.LOGGER.info("[SkyTrail] {} skip sample - no locator/offset", id);
            return false;
        }
        if (pointB == null) {
            Vec3d[] edges = buildAutoWidthPoints(pointA);
            if (edges == null) {
                SkyCoreMod.LOGGER.info("[SkyTrail] {} auto-width failed (start)", id);
                return false;
            }
            pointA = edges[0];
            pointB = edges[1];
        } else if (pointA == null) {
            Vec3d[] edges = buildAutoWidthPoints(pointB);
            if (edges == null) {
                SkyCoreMod.LOGGER.info("[SkyTrail] {} auto-width failed (end)", id);
                return false;
            }
            pointA = edges[0];
            pointB = edges[1];
        }
        TrailSample sample = new TrailSample(pointA, pointB);
        samples.addLast(sample);
        while (samples.size() > maxSamples) {
            samples.removeFirst();
        }
        SkyCoreMod.LOGGER.info("[SkyTrail] sample {} start={} end={}", id, sample.start, sample.end);
        basisValid = false;
        return true;
    }

    private double[] resolvePosition(EntityLivingBase entity,
                                     BedrockModelHandle wrapper,
                                     String locator,
                                     float[] offset,
                                     float headYaw,
                                     float partialTicks,
                                     boolean captureBasis) {
        if (locator != null && !locator.isEmpty() && wrapper != null) {
            boolean transformOk = wrapper.resolveLocatorTransform(locator, locatorTransform);
            if (transformOk) {
                double[] world = transformLocatorToWorld(entity, headYaw, partialTicks, wrapper.getModelScale());
                if (captureBasis) {
                    cacheBasis(wrapper.getModelScale(), headYaw);
                }
                if (world != null) {
                    SkyCoreMod.LOGGER.info("[SkyTrail] locator {} world=({}, {}, {}) local=({}, {}, {}) axis={}",
                        locator,
                        String.format("%.3f", world[0]),
                        String.format("%.3f", world[1]),
                        String.format("%.3f", world[2]),
                        String.format("%.3f", locatorTransform.position[0]),
                        String.format("%.3f", locatorTransform.position[1]),
                        String.format("%.3f", locatorTransform.position[2]),
                        axis);
                    return world;
                }
            } else {
                SkyCoreMod.LOGGER.info("[SkyTrail] {} locator {} resolve failed", id, locator);
            }
        }
        if (entity == null || offset == null || offset.length < 3) {
            return null;
        }
        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        float yawRad = (float) Math.toRadians(180.0F - headYaw);
        float cos = (float) Math.cos(yawRad);
        float sin = (float) Math.sin(yawRad);
        double rx = offset[0] * cos + offset[2] * sin;
        double rz = -offset[0] * sin + offset[2] * cos;
        return new double[]{baseX + rx, baseY + offset[1], baseZ + rz};
    }

    private double[] transformLocatorToWorld(EntityLivingBase entity,
                                             float headYaw,
                                             float partialTicks,
                                             float scale) {
        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        float lx = locatorTransform.position[0] * scale;
        float ly = locatorTransform.position[1] * scale;
        float lz = locatorTransform.position[2] * scale;
        float yawRad = (float) Math.toRadians(180.0F - headYaw);
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(yawRad);
        float rx = lx * cos + lz * sin;
        float rz = -lx * sin + lz * cos;
        return new double[]{baseX + rx, baseY + ly, baseZ + rz};
    }

    private Vec3d[] buildAutoWidthPoints(Vec3d center) {
        if (!basisValid || center == null || width <= 0f) {
            return null;
        }
        Vec3d axis = new Vec3d(cachedBasis[0], cachedBasis[1], cachedBasis[2]);
        if (axis.lengthSquared() < 1.0e-4) {
            return null;
        }
        Vec3d dir = axis.normalize().scale(width * 0.5f);
        return new Vec3d[]{center.add(dir), center.subtract(dir)};
    }

    private void cacheBasis(float modelScale, float headYaw) {
        float[] source;
        switch (axis) {
            case Y:
                source = locatorTransform.basisY;
                break;
            case X:
                source = locatorTransform.basisX;
                break;
            case Z:
            default:
                source = locatorTransform.basisZ;
                break;
        }
        float yawRad = (float) Math.toRadians(180.0F - headYaw);
        float cos = MathHelper.cos(yawRad);
        float sin = MathHelper.sin(yawRad);
        float x = source[0];
        float y = source[1];
        float z = source[2];
        cachedBasis[0] = (x * cos + z * sin) * modelScale;
        cachedBasis[1] = y * modelScale;
        cachedBasis[2] = (-x * sin + z * cos) * modelScale;
        basisValid = true;
    }

    public static final class TrailSample {
        public final Vec3d start;
        public final Vec3d end;
        float age = 0f;

        private TrailSample(Vec3d start, Vec3d end) {
            this.start = start;
            this.end = end;
        }
    }
}
