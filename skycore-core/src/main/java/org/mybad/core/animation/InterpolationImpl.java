package org.mybad.core.animation;

/**
 * 27种标准缓动函数实现
 * 基于Robert Penner的缓动函数
 */
public class InterpolationImpl {

    /**
     * 线性插值
     */
    public static class Linear implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t;
        }

        @Override
        public String getName() {
            return "linear";
        }
    }

    /**
     * Catmull-Rom（平滑）插值标记。
     * 实际的曲线计算在动画播放器中完成。
     */
    public static class CatmullRom implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t;
        }

        @Override
        public String getName() {
            return "catmullrom";
        }
    }

    /**
     * Step（步进）插值。
     * 始终保持上一帧的值。
     */
    public static class Step implements Interpolation {
        @Override
        public float interpolate(float t) {
            return 0f;
        }

        @Override
        public String getName() {
            return "step";
        }
    }

    /**
     * Bezier 插值标记。
     * 实际的曲线计算在动画播放器中完成。
     */
    public static class Bezier implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t;
        }

        @Override
        public String getName() {
            return "bezier";
        }
    }

    // === Quadratic (2nd power) ===

    public static class QuadIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t * t;
        }

        @Override
        public String getName() {
            return "quad.in";
        }
    }

    public static class QuadOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            return 1 - (1 - t) * (1 - t);
        }

        @Override
        public String getName() {
            return "quad.out";
        }
    }

    public static class QuadInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t < 0.5f) {
                return 2 * t * t;
            } else {
                return 1 - 2 * (1 - t) * (1 - t);
            }
        }

        @Override
        public String getName() {
            return "quad.inout";
        }
    }

    // === Cubic (3rd power) ===

    public static class CubicIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t * t * t;
        }

        @Override
        public String getName() {
            return "cubic.in";
        }
    }

    public static class CubicOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            return 1 - (1 - t) * (1 - t) * (1 - t);
        }

        @Override
        public String getName() {
            return "cubic.out";
        }
    }

    public static class CubicInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t < 0.5f) {
                return 4 * t * t * t;
            } else {
                return 1 - 4 * (1 - t) * (1 - t) * (1 - t);
            }
        }

        @Override
        public String getName() {
            return "cubic.inout";
        }
    }

    // === Quartic (4th power) ===

    public static class QuartIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t * t * t * t;
        }

        @Override
        public String getName() {
            return "quart.in";
        }
    }

    public static class QuartOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            return 1 - (1 - t) * (1 - t) * (1 - t) * (1 - t);
        }

        @Override
        public String getName() {
            return "quart.out";
        }
    }

    public static class QuartInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t < 0.5f) {
                return 8 * t * t * t * t;
            } else {
                return 1 - 8 * (1 - t) * (1 - t) * (1 - t) * (1 - t);
            }
        }

        @Override
        public String getName() {
            return "quart.inout";
        }
    }

    // === Quintic (5th power) ===

    public static class QuintIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t * t * t * t * t;
        }

        @Override
        public String getName() {
            return "quint.in";
        }
    }

    public static class QuintOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            return 1 - (1 - t) * (1 - t) * (1 - t) * (1 - t) * (1 - t);
        }

        @Override
        public String getName() {
            return "quint.out";
        }
    }

    public static class QuintInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t < 0.5f) {
                return 16 * t * t * t * t * t;
            } else {
                return 1 - 16 * (1 - t) * (1 - t) * (1 - t) * (1 - t) * (1 - t);
            }
        }

        @Override
        public String getName() {
            return "quint.inout";
        }
    }

    // === Sine ===

    public static class SineIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return 1 - (float) Math.cos((t * Math.PI) / 2);
        }

        @Override
        public String getName() {
            return "sine.in";
        }
    }

    public static class SineOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return (float) Math.sin((t * Math.PI) / 2);
        }

        @Override
        public String getName() {
            return "sine.out";
        }
    }

    public static class SineInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return -(float) (Math.cos(Math.PI * t) - 1) / 2;
        }

        @Override
        public String getName() {
            return "sine.inout";
        }
    }

    // === Exponential ===

    public static class ExpoIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10);
        }

        @Override
        public String getName() {
            return "expo.in";
        }
    }

    public static class ExpoOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            return t == 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
        }

        @Override
        public String getName() {
            return "expo.out";
        }
    }

    public static class ExpoInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t == 0) {
                return 0;
            } else if (t == 1) {
                return 1;
            } else if (t < 0.5f) {
                return (float) Math.pow(2, 20 * t - 10) / 2;
            } else {
                return (2 - (float) Math.pow(2, -20 * t + 10)) / 2;
            }
        }

        @Override
        public String getName() {
            return "expo.inout";
        }
    }

    // === Circular ===

    public static class CircIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return 1 - (float) Math.sqrt(1 - t * t);
        }

        @Override
        public String getName() {
            return "circ.in";
        }
    }

    public static class CircOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return (float) Math.sqrt(1 - (t - 1) * (t - 1));
        }

        @Override
        public String getName() {
            return "circ.out";
        }
    }

    public static class CircInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            if (t < 0.5f) {
                return (1 - (float) Math.sqrt(1 - 4 * t * t)) / 2;
            } else {
                return ((float) Math.sqrt(1 - 4 * (t - 1) * (t - 1)) + 1) / 2;
            }
        }

        @Override
        public String getName() {
            return "circ.inout";
        }
    }

    // === Back ===

    private static final float C1 = 1.70158f;
    private static final float C2 = C1 + 1;
    private static final float C3 = C1 + 1;
    private static final float C4 = (2 * (float) Math.PI) / 3;
    private static final float C5 = (2 * (float) Math.PI) / 4.5f;

    public static class BackIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return C2 * t * t * t - C1 * t * t;
        }

        @Override
        public String getName() {
            return "back.in";
        }
    }

    public static class BackOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            return 1 + C2 * (t - 1) * (t - 1) * (t - 1) + C1 * (t - 1) * (t - 1);
        }

        @Override
        public String getName() {
            return "back.out";
        }
    }

    public static class BackInOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t <= 0) return 0;
            if (t >= 1) return 1;
            float c2 = C3 * 1.525f;
            if (t < 0.5f) {
                return ((2 * t) * (2 * t) * (c2 + 1) - 2 * t * c2) / 2;
            } else {
                return ((2 * t - 2) * (2 * t - 2) * (c2 + 1) + 2 * (t * 2 - 1) * c2) / 2 + 1;
            }
        }

        @Override
        public String getName() {
            return "back.inout";
        }
    }

    // === Elastic ===

    public static class ElasticIn implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t == 0) {
                return 0;
            } else if (t == 1) {
                return 1;
            } else {
                return -(float) Math.pow(2, 10 * t - 10) * (float) Math.sin((t * 10 - 10.75f) * C5);
            }
        }

        @Override
        public String getName() {
            return "elastic.in";
        }
    }

    public static class ElasticOut implements Interpolation {
        @Override
        public float interpolate(float t) {
            if (t == 0) {
                return 0;
            } else if (t == 1) {
                return 1;
            } else {
                return (float) Math.pow(2, -10 * t) * (float) Math.sin((t * 10 - 0.75f) * C5) + 1;
            }
        }

        @Override
        public String getName() {
            return "elastic.out";
        }
    }

    // === 工厂方法 ===

    /**
     * 根据名称获取插值实例
     */
    public static Interpolation getInstance(String name) {
        if (name == null || name.isEmpty()) {
            return new Linear();
        }

        switch (name.toLowerCase()) {
            case "linear":
                return new Linear();
            case "catmullrom":
                return new CatmullRom();
            case "step":
                return new Step();
            case "bezier":
                return new Bezier();

            case "quad.in":
                return new QuadIn();
            case "quad.out":
                return new QuadOut();
            case "quad.inout":
                return new QuadInOut();

            case "cubic.in":
                return new CubicIn();
            case "cubic.out":
                return new CubicOut();
            case "cubic.inout":
                return new CubicInOut();

            case "quart.in":
                return new QuartIn();
            case "quart.out":
                return new QuartOut();
            case "quart.inout":
                return new QuartInOut();

            case "quint.in":
                return new QuintIn();
            case "quint.out":
                return new QuintOut();
            case "quint.inout":
                return new QuintInOut();

            case "sine.in":
                return new SineIn();
            case "sine.out":
                return new SineOut();
            case "sine.inout":
                return new SineInOut();

            case "expo.in":
                return new ExpoIn();
            case "expo.out":
                return new ExpoOut();
            case "expo.inout":
                return new ExpoInOut();

            case "circ.in":
                return new CircIn();
            case "circ.out":
                return new CircOut();
            case "circ.inout":
                return new CircInOut();

            case "back.in":
                return new BackIn();
            case "back.out":
                return new BackOut();
            case "back.inout":
                return new BackInOut();

            case "elastic.in":
                return new ElasticIn();
            case "elastic.out":
                return new ElasticOut();

            default:
                return new Linear();
        }
    }

    /**
     * 获取所有支持的插值模式名称
     */
    public static String[] getSupportedModes() {
        return new String[]{
            "linear",
            "catmullrom",
            "quad.in", "quad.out", "quad.inout",
            "cubic.in", "cubic.out", "cubic.inout",
            "quart.in", "quart.out", "quart.inout",
            "quint.in", "quint.out", "quint.inout",
            "sine.in", "sine.out", "sine.inout",
            "expo.in", "expo.out", "expo.inout",
            "circ.in", "circ.out", "circ.inout",
            "back.in", "back.out", "back.inout",
            "elastic.in", "elastic.out"
        };
    }

    /**
     * 获取插值模式总数
     */
    public static int getTotalModes() {
        return getSupportedModes().length;
    }
}
