package org.mybad.minecraft.render.trail;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.ResourceLocation;
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
    private TrailAxis axis = TrailAxis.Z;
    private final LocatorTransform locatorTransformStart = new LocatorTransform();
    private final LocatorTransform locatorTransformEnd = new LocatorTransform();
    private final float[] cachedBasis = new float[3];
    private boolean basisValid = false;
    private final Vec3d[] rawPoints = new Vec3d[4];
    private int rawCount = 0;
    private final Vec3d[] rawStartPoints = new Vec3d[4];
    private final Vec3d[] rawEndPoints = new Vec3d[4];
    private int rawDualCount = 0;
    private int segmentDetail = 4;
    private boolean stretchUv = false;

    public WeaponTrailClip(AnimationEventArgsParser.TrailParams params) {
        this.id = params != null && params.id != null ? params.id : "trail";
        applyParams(params);
    }

    public void applyParams(AnimationEventArgsParser.TrailParams params) {
        if (params == null) {
            return;
        }
        if (params.locatorStart != null && !params.locatorStart.isEmpty()) {
            this.locatorStart = params.locatorStart.trim();
        }
        if (params.locatorEnd != null) {
            String trimmedEnd = params.locatorEnd.trim();
            this.locatorEnd = trimmedEnd.isEmpty() ? null : trimmedEnd;
        } else {
            this.locatorEnd = null;
        }
        if (params.width > 0f) {
            this.width = params.width;
        }
        if (params.texture != null) {
            this.texture = params.texture;
        }
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
        this.rawCount = 0;
        this.rawDualCount = 0;
        for (int i = 0; i < rawPoints.length; i++) {
            rawPoints[i] = null;
        }
        for (int i = 0; i < rawStartPoints.length; i++) {
            rawStartPoints[i] = null;
            rawEndPoints[i] = null;
        }
        this.segmentDetail = Math.max(1, params.segments);
        this.stretchUv = params.stretchUv;
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
    
    public boolean isStretchUv() {
        return stretchUv;
    }

    public Deque<TrailSample> getSamples() {
        return samples;
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
        double[] startPos = resolvePosition(entity, wrapper, locatorStart, locatorTransformStart, headYaw, partialTicks, true);
        if (startPos == null) {
            return false;
        }
        Vec3d startVec = new Vec3d(startPos[0], startPos[1], startPos[2]);
        if (hasDualLocator()) {
            double[] endPos = resolvePosition(entity, wrapper, locatorEnd, locatorTransformEnd, headYaw, partialTicks, false);
            if (endPos != null) {
                Vec3d endVec = new Vec3d(endPos[0], endPos[1], endPos[2]);
                addDualPoint(startVec, endVec);
                basisValid = false;
                return true;
            }
        }
        addRawPoint(startPos[0], startPos[1], startPos[2]);
        basisValid = false;
        return true;
    }

    private void addRawPoint(double x, double y, double z) {
        Vec3d point = new Vec3d(x, y, z);
        if (rawCount < rawPoints.length) {
            rawPoints[rawCount++] = point;
        } else {
            System.arraycopy(rawPoints, 1, rawPoints, 0, rawPoints.length - 1);
            rawPoints[rawPoints.length - 1] = point;
        }
        if (rawCount >= 4) {
            emitInterpolatedSamples();
        } else {
            emitLinearSample(point);
        }
    }

    private void addDualPoint(Vec3d startEdge, Vec3d endEdge) {
        if (startEdge == null || endEdge == null) {
            return;
        }
        if (rawDualCount < rawStartPoints.length) {
            rawStartPoints[rawDualCount] = startEdge;
            rawEndPoints[rawDualCount] = endEdge;
            rawDualCount++;
        } else {
            System.arraycopy(rawStartPoints, 1, rawStartPoints, 0, rawStartPoints.length - 1);
            System.arraycopy(rawEndPoints, 1, rawEndPoints, 0, rawEndPoints.length - 1);
            rawStartPoints[rawStartPoints.length - 1] = startEdge;
            rawEndPoints[rawEndPoints.length - 1] = endEdge;
            rawDualCount = rawStartPoints.length;
        }
        if (rawDualCount >= 4) {
            emitDualInterpolatedSamples();
        } else {
            addSample(startEdge, endEdge);
        }
    }

    private void emitLinearSample(Vec3d center) {
        Vec3d[] edges = buildAutoWidthPoints(center);
        if (edges == null) {
            return;
        }
        addSample(edges[0], edges[1]);
    }

    private void emitInterpolatedSamples() {
        Vec3d p0 = rawPoints[0];
        Vec3d p1 = rawPoints[1];
        Vec3d p2 = rawPoints[2];
        Vec3d p3 = rawPoints[3];
        if (p0 == null || p1 == null || p2 == null || p3 == null) {
            return;
        }
        int steps = Math.max(1, segmentDetail);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double x = catmullRom(p0.x, p1.x, p2.x, p3.x, t);
            double y = catmullRom(p0.y, p1.y, p2.y, p3.y, t);
            double z = catmullRom(p0.z, p1.z, p2.z, p3.z, t);
            Vec3d center = new Vec3d(x, y, z);
            Vec3d[] edges = buildAutoWidthPoints(center);
            if (edges == null) {
                continue;
            }
            addSample(edges[0], edges[1]);
        }
    }

    private void emitDualInterpolatedSamples() {
        Vec3d s0 = rawStartPoints[0];
        Vec3d s1 = rawStartPoints[1];
        Vec3d s2 = rawStartPoints[2];
        Vec3d s3 = rawStartPoints[3];
        Vec3d e0 = rawEndPoints[0];
        Vec3d e1 = rawEndPoints[1];
        Vec3d e2 = rawEndPoints[2];
        Vec3d e3 = rawEndPoints[3];
        if (s0 == null || s1 == null || s2 == null || s3 == null ||
            e0 == null || e1 == null || e2 == null || e3 == null) {
            return;
        }
        int steps = Math.max(1, segmentDetail);
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double sx = catmullRom(s0.x, s1.x, s2.x, s3.x, t);
            double sy = catmullRom(s0.y, s1.y, s2.y, s3.y, t);
            double sz = catmullRom(s0.z, s1.z, s2.z, s3.z, t);
            double ex = catmullRom(e0.x, e1.x, e2.x, e3.x, t);
            double ey = catmullRom(e0.y, e1.y, e2.y, e3.y, t);
            double ez = catmullRom(e0.z, e1.z, e2.z, e3.z, t);
            addSample(new Vec3d(sx, sy, sz), new Vec3d(ex, ey, ez));
        }
    }

    private double catmullRom(double p0, double p1, double p2, double p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * ((2.0 * p1) +
            (-p0 + p2) * t +
            (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 +
            (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private void addSample(Vec3d start, Vec3d end) {
        TrailSample sample = new TrailSample(start, end);
        samples.addLast(sample);
        while (samples.size() > maxSamples) {
            samples.removeFirst();
        }
    }

    private double[] resolvePosition(EntityLivingBase entity,
                                     BedrockModelHandle wrapper,
                                     String locator,
                                     LocatorTransform transform,
                                     float headYaw,
                                     float partialTicks,
                                     boolean captureBasis) {
        if (locator != null && !locator.isEmpty() && wrapper != null && transform != null) {
            boolean transformOk = wrapper.resolveLocatorTransform(locator, transform);
            if (transformOk) {
                double[] world = transformLocatorToWorld(entity, headYaw, partialTicks, wrapper.getModelScale(), transform);
                if (captureBasis) {
                    cacheBasis(transform, wrapper.getModelScale(), headYaw);
                }
                return world;
            }
        }
        return null;
    }

    private double[] transformLocatorToWorld(EntityLivingBase entity,
                                             float headYaw,
                                             float partialTicks,
                                             float scale,
                                             LocatorTransform transform) {
        double baseX = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
        double baseY = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks;
        double baseZ = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
        float lx = transform.position[0] * scale;
        float ly = transform.position[1] * scale;
        float lz = transform.position[2] * scale;
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

    private void cacheBasis(LocatorTransform transform, float modelScale, float headYaw) {
        float[] source;
        switch (axis) {
            case Y:
                source = transform.basisY;
                break;
            case X:
                source = transform.basisX;
                break;
            case Z:
            default:
                source = transform.basisZ;
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

    private boolean hasDualLocator() {
        return locatorEnd != null && !locatorEnd.isEmpty();
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
