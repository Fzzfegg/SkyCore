package org.mybad.minecraft.particle;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.pinwheel.particle.ParticleData;

final class ParticleCurveEvaluator {
    private ParticleCurveEvaluator() {
    }

    static float evaluateCurve(MolangEnvironment environment, ParticleData.Curve curve) {
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
}
