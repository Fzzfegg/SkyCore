package org.mybad.core.particle.curve;

/**
 * 曲线 - 定义粒子属性随时间的变化曲线
 * 用于颜色、透明度、缩放等参数的渐进式变化
 */
public class Curve {

    private String curveId;
    private CurveType curveType;

    private float[] timePoints;      // 时间点（0-1）
    private float[] valuePoints;     // 对应的值

    // 预设曲线
    public enum CurveType {
        LINEAR,           // 线性
        EASE_IN,          // 缓入
        EASE_OUT,         // 缓出
        EASE_IN_OUT,      // 缓入缓出
        STEP,             // 阶跃
        CUSTOM            // 自定义
    }

    public Curve(String curveId) {
        this(curveId, CurveType.LINEAR);
    }

    public Curve(String curveId, CurveType curveType) {
        this.curveId = curveId;
        this.curveType = curveType;
        this.timePoints = new float[]{0.0f, 1.0f};
        this.valuePoints = new float[]{0.0f, 1.0f};

        // 初始化预设曲线
        initializePreset();
    }

    /**
     * 初始化预设曲线
     */
    private void initializePreset() {
        switch (curveType) {
            case EASE_IN:
                // 二次方缓入
                valuePoints = new float[]{0.0f, 0.25f, 0.5f, 1.0f};
                timePoints = new float[]{0.0f, 0.33f, 0.66f, 1.0f};
                break;
            case EASE_OUT:
                // 二次方缓出
                valuePoints = new float[]{0.0f, 0.5f, 0.75f, 1.0f};
                timePoints = new float[]{0.0f, 0.34f, 0.67f, 1.0f};
                break;
            case EASE_IN_OUT:
                // 缓入缓出
                valuePoints = new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
                timePoints = new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
                break;
            case STEP:
                // 阶跃（保持初始值，在最后跳到最终值）
                valuePoints = new float[]{0.0f, 0.0f, 1.0f};
                timePoints = new float[]{0.0f, 0.99f, 1.0f};
                break;
            case LINEAR:
            default:
                // 线性已在构造函数中设置
                break;
        }
    }

    /**
     * 评估曲线在给定时间的值
     */
    public float evaluate(float time) {
        // 限制时间在0-1范围内
        time = Math.max(0, Math.min(1, time));

        // 找到对应的区间
        for (int i = 0; i < timePoints.length - 1; i++) {
            float t0 = timePoints[i];
            float t1 = timePoints[i + 1];

            if (time >= t0 && time <= t1) {
                float v0 = valuePoints[i];
                float v1 = valuePoints[i + 1];

                // 线性插值
                float t = (time - t0) / (t1 - t0);
                return v0 + (v1 - v0) * t;
            }
        }

        return valuePoints[valuePoints.length - 1];
    }

    /**
     * 设置自定义曲线点
     */
    public void setPoints(float[] times, float[] values) {
        if (times.length != values.length || times.length < 2) {
            throw new IllegalArgumentException("Invalid curve points");
        }

        this.timePoints = times;
        this.valuePoints = values;
        this.curveType = CurveType.CUSTOM;
    }

    /**
     * 添加曲线点
     */
    public void addPoint(float time, float value) {
        // 找到插入位置
        int insertIndex = 0;
        for (int i = 0; i < timePoints.length; i++) {
            if (timePoints[i] > time) {
                insertIndex = i;
                break;
            }
            insertIndex = i + 1;
        }

        // 扩展数组
        float[] newTimes = new float[timePoints.length + 1];
        float[] newValues = new float[valuePoints.length + 1];

        System.arraycopy(timePoints, 0, newTimes, 0, insertIndex);
        System.arraycopy(valuePoints, 0, newValues, 0, insertIndex);

        newTimes[insertIndex] = time;
        newValues[insertIndex] = value;

        System.arraycopy(timePoints, insertIndex, newTimes, insertIndex + 1, timePoints.length - insertIndex);
        System.arraycopy(valuePoints, insertIndex, newValues, insertIndex + 1, valuePoints.length - insertIndex);

        this.timePoints = newTimes;
        this.valuePoints = newValues;
        this.curveType = CurveType.CUSTOM;
    }

    // Getters
    public String getCurveId() { return curveId; }
    public CurveType getCurveType() { return curveType; }
    public float[] getTimePoints() { return timePoints; }
    public float[] getValuePoints() { return valuePoints; }

    @Override
    public String toString() {
        return String.format("Curve [%s, Type: %s, Points: %d]",
                curveId, curveType, timePoints.length);
    }
}
